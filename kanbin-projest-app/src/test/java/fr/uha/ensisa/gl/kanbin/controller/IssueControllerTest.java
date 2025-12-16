package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueRepo issueRepo;
    @Mock
    private BoardRepo boardRepo; // Mock du BoardRepo ajouté
    @Mock
    private RedirectAttributes redirectAttributes;

    private IssueController sut;

    @BeforeEach
    void setUp() {
        // Nouveau constructeur avec 2 arguments
        sut = new IssueController(issueRepo, boardRepo);
    }

    @Test
    void listIssues_shouldReturnAllIssues() {
        when(issueRepo.findAll()).thenReturn(List.of(new Issue(1, "A"), new Issue(2, "B")));
        ModelAndView mv = sut.listIssues();
        assertEquals("list-issues", mv.getViewName());
        assertNotNull(mv.getModel().get("issues"));
    }

    @Test
    void newIssue_shouldReturnFormWithRows() {
        // On simule la présence d'un board pour avoir les rows
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().get("issue") instanceof Issue);
        // Vérifie qu'on passe bien les rows à la vue
        assertNotNull(mv.getModel().get("rows"));
    }

    @Test
    void createIssue_shouldPersistAndRedirect() {
        Issue issue = new Issue();
        issue.setTitle("Test");

        // Simulation board pour rowKey par défaut
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        String view = sut.createIssue(issue, redirectAttributes);

        verify(issueRepo).persist(issue);
        assertEquals("redirect:/board", view);
    }

    @Test
    void editIssue_whenFound_shouldReturnForm() {
        long id = 1L;
        when(issueRepo.find(id)).thenReturn(new Issue(id, "To Edit"));
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        // Correction du nom de méthode : editIssue au lieu de editIssueForm
        ModelAndView mv = sut.editIssue(id);

        assertEquals("edit-issue", mv.getViewName());
        assertEquals(id, ((Issue) mv.getModel().get("issue")).getId());
    }

    @Test
    void editIssue_whenNotFound_shouldRedirect() {
        long id = 99L;
        when(issueRepo.find(id)).thenReturn(null);

        ModelAndView mv = sut.editIssue(id);

        assertEquals("redirect:/issues", mv.getViewName());
    }

    @Test
    void updateIssue_shouldUpdateAndRedirect() {
        long id = 10L;
        Issue existing = new Issue(id, "Old");
        existing.setColumnKey("todo");
        existing.setRowKey("urgent"); // Simulation row existante

        when(issueRepo.find(id)).thenReturn(existing);

        Issue update = new Issue();
        update.setTitle("New");

        String view = sut.updateIssue(id, update, redirectAttributes);

        verify(issueRepo).persist(update);
        // Vérifie qu'on garde les anciennes clés si nulles
        assertEquals("todo", update.getColumnKey());
        assertEquals("urgent", update.getRowKey());
        assertEquals("redirect:/board", view);
    }

    @Test
    void deleteIssue_shouldRemoveAndRedirect() {
        long id = 5L;
        when(issueRepo.find(id)).thenReturn(new Issue(id, "To Delete"));

        String view = sut.deleteIssue(id, redirectAttributes);

        verify(issueRepo).remove(id);
        assertEquals("redirect:/board", view);
    }

    @Test
    void createIssue_NoRowKey_ShouldAssignDefaultRow() {
        Board mockBoard = new Board("Default Board");
        fr.uha.ensisa.gl.kanbin.projest.model.Row defaultRow = new fr.uha.ensisa.gl.kanbin.projest.model.Row("row_def", "Default Row");
        mockBoard.getRows().add(defaultRow);

        when(boardRepo.findAll()).thenReturn(List.of(mockBoard));

        Issue issue = new Issue();
        issue.setTitle("Ticket sans ligne");
        issue.setRowKey(null);

        sut.createIssue(issue, redirectAttributes);
        assertEquals("row_def", issue.getRowKey());
    }

    @Test
    void createIssue_ExistingId_ShouldPreserveOldKeys() {
        long id = 50L;
        Issue oldIssue = new Issue(id, "Vieux Titre");
        oldIssue.setColumnKey("old_col");
        oldIssue.setRowKey("old_row");
        when(issueRepo.find(id)).thenReturn(oldIssue);

        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));


        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setTitle("Nouveau Titre");
        formIssue.setColumnKey(null);
        formIssue.setRowKey("");

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("old_col", formIssue.getColumnKey());
        assertEquals("old_row", formIssue.getRowKey());
        verify(issueRepo).persist(formIssue);
    }

    @Test
    void deleteIssue_UnknownId_ShouldNotCrash() {
        long id = 999L;
        when(issueRepo.find(id)).thenReturn(null); // L'ID n'existe pas
        String view = sut.deleteIssue(id, redirectAttributes);
        verify(issueRepo, never()).remove(anyLong());
        assertEquals("redirect:/board", view);
    }

    @Test
    void createIssue_WithExplicitRowKey_ShouldNotUseDefault() {
        Issue issue = new Issue();
        issue.setTitle("Tache avec ligne forcée");
        issue.setRowKey("row_custom"); // L'utilisateur choisit sa ligne
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));
        sut.createIssue(issue, redirectAttributes);
        assertEquals("row_custom", issue.getRowKey());
    }

    @Test
    void createIssue_ExistingId_WithNewKeys_ShouldNotUseOldKeys() {
        long id = 5L;
        Issue oldIssue = new Issue(id, "Old Title");
        oldIssue.setColumnKey("col_old");
        oldIssue.setRowKey("row_old");
        when(issueRepo.find(id)).thenReturn(oldIssue);
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setColumnKey("col_new");
        formIssue.setRowKey("row_new");

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("col_new", formIssue.getColumnKey());
        assertEquals("row_new", formIssue.getRowKey());
    }

    @Test
    void createIssue_WithIdButNotFound_ShouldIgnoreOldValues() {

        long id = 77L;
        Issue formIssue = new Issue();
        formIssue.setId(id);

        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));
        when(issueRepo.find(id)).thenReturn(null);
        sut.createIssue(formIssue, redirectAttributes);
        verify(issueRepo).persist(formIssue);
    }

    @Test
    void updateIssue_WithExplicitKeys_ShouldKeepThem() {
        long id = 10L;
        Issue existing = new Issue(id, "Old");
        existing.setColumnKey("col_old");
        existing.setRowKey("row_old");
        when(issueRepo.find(id)).thenReturn(existing);

        Issue update = new Issue();
        update.setColumnKey("col_new");
        update.setRowKey("row_new");
        sut.updateIssue(id, update, redirectAttributes);
        assertEquals("col_new", update.getColumnKey());
        assertEquals("row_new", update.getRowKey());
    }

    @Test
    void updateIssue_UnknownId_ShouldJustPersist() {

        long id = 999L;
        when(issueRepo.find(id)).thenReturn(null);
        Issue update = new Issue();
        update.setTitle("Forced Update");
        sut.updateIssue(id, update, redirectAttributes);
        verify(issueRepo).persist(update);
    }





}