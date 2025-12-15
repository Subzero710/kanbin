package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardRepo boardRepo;
    @Mock
    private IssueRepo issueRepo;
    @Mock
    private RedirectAttributes redirectAttributes;

    private BoardController sut;

    @BeforeEach
    void setUp() {
        sut = new BoardController(boardRepo, issueRepo);
    }

    @Test
    void root_shouldRedirectToBoard() {
        // Correction nom méthode : homeRedirect -> root
        ModelAndView mv = sut.root();
        assertEquals("redirect:/board", mv.getViewName());
    }

    @Test
    void showBoard_shouldDisplayBoardWithRowsAndCols() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("todo", "Todo"));
        b.addRow(new Row("urgent", "Urgent"));

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of());

        // Correction nom méthode : board -> showBoard
        ModelAndView mv = sut.showBoard();

        assertEquals("board", mv.getViewName());
        assertSame(b, mv.getModel().get("board"));
        assertNotNull(mv.getModel().get("rows")); // Vérifie que les rows sont passées
        assertNotNull(mv.getModel().get("issuesByRowAndCol"));
    }

    @Test
    void addColumn_shouldAddColumnAndRedirect() {
        Board b = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(b));

        String view = sut.addColumn("New Col", "simple", 0, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).addColumn(eq(b.getId()), any(Column.class));
    }

    @Test
    void addRow_shouldAddRowAndRedirect() {
        Board b = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(b));

        String view = sut.addRow("New Swimlane");

        assertEquals("redirect:/board", view);
        // Vérifie qu'on a bien ajouté la row
        assertFalse(b.getRows().isEmpty());
        verify(boardRepo).save(b);
    }

    @Test
    void moveIssueDnD_shouldUpdateColumnAndRow() {
        Board b = new Board("Test Board");
        Column todo = new Column("todo", "Todo");
        b.addColumn(todo);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Task");
        issue.setColumnKey("old");
        issue.setRowKey("oldRow");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        // Appel avec 3 arguments (ID, Col, Row)
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "todo", "newRow");

        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("todo", issue.getColumnKey());
        assertEquals("newRow", issue.getRowKey());
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldKeepRowIfNull() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("done", "Done"));

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Task");
        issue.setRowKey("urgent"); // Ligne existante

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        // Appel avec Row = null (ne doit pas changer la ligne)
        sut.moveIssueDnD(issueId, "done", null);

        assertEquals("done", issue.getColumnKey());
        assertEquals("urgent", issue.getRowKey()); // N'a pas changé
    }

    // Test WIP Limit
    @Test
    void moveIssueDnD_shouldFailIfWipLimitReached() {
        Board b = new Board("Test Board");
        Column wipCol = new Column("wip", "WIP");
        wipCol.setWipLimit(1);
        b.addColumn(wipCol);

        // Déjà une issue dedans
        Issue blocker = new Issue(2L, "Blocker");
        blocker.setColumnKey("wip");
        blocker.setRowKey("default"); // Important pour le comptage

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(blocker));

        Issue newIssue = new Issue(1L, "Mover");
        newIssue.setRowKey("default");
        when(issueRepo.find(1L)).thenReturn(newIssue);

        // Tentative de déplacer dans une colonne pleine
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(1L, "wip", "default");

        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("maximum"));
    }
}