package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.BoardRepoMem;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BoardControllerTest {

    @Mock
    private BoardRepo boardRepo;

    @Mock
    private IssueRepo issueRepo;

    @InjectMocks
    private BoardController sut;

    private final long TEST_BOARD_ID = 1L;
    private final long TEST_ISSUE_ID = 42L;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = new Board(TEST_BOARD_ID, "Test Board");
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    @Test
    void board_shouldReturnBoardView() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        when(issueRepo.findAll()).thenReturn(List.of());
        ModelAndView mv = sut.board();
        assertEquals("board", mv.getViewName());
    }

    @Test
    void addColumn_whenBoardExists_shouldAddColumnToIt() {
        String newColumnTitle = "Test Column #29";
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        sut.addColumn(newColumnTitle, "simple");

        verify(boardRepo, times(1)).addColumn(
                eq(TEST_BOARD_ID),
                any(Column.class)
        );
    }

    @Test
    void addColumn_whenRepoIsEmpty_shouldCreateDefaultBoard() {
        when(boardRepo.findAll()).thenReturn(List.of());
        when(boardRepo.save(any(Board.class))).thenAnswer(invocation -> {
            Board b = invocation.getArgument(0);
            b.setId(TEST_BOARD_ID);
            return b;
        });
        sut.addColumn("Ma Colonne", "simple");
        verify(boardRepo).save(any(Board.class));
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), any(Column.class));
    }

    @Test
    void addColumn_shouldCreateParentWithTwoSubColumns() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        // Appel avec le type "double"
        sut.addColumn("ColonnePrincipale", "double");

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), columnCaptor.capture());

        Column createdCol = columnCaptor.getValue();
        assertEquals("ColonnePrincipale", createdCol.getTitle());
        assertEquals(2, createdCol.getSubColumns().size());
        assertEquals("À Faire", createdCol.getSubColumns().get(0).getTitle());
        assertEquals("En Cours", createdCol.getSubColumns().get(1).getTitle());
    }

    @Test
    void postRemoveColumn_shouldSaveBoardWithoutThatColumn() {
        long columnToRemoveId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = sut.removeColumn(columnToRemoveId, redirectAttributes);
        assertEquals("redirect:/board", viewName);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo, times(1)).save(captor.capture());
        Board saved = captor.getValue();
        assertTrue(saved.getColumns().stream().noneMatch(c -> c.getId() == columnToRemoveId));
    }

    @Test
    void removeColumn_fixedColumn_shouldFail_andSetErrorMessage() {
        long fixedColId = 999L;
        Column fixedCol = new Column(fixedColId, "backlog", "Backlog");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);

        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = sut.removeColumn(fixedColId, redirectAttributes);

        assertEquals("redirect:/board", viewName);
        verify(boardRepo, never()).save(any());

        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
        assertEquals("Action interdite : Cette colonne système ne peut pas être supprimée.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void editColumnForm_fixedColumn_shouldRedirect() {
        long fixedColId = 888L;
        Column fixedCol = new Column(fixedColId, "fix", "Fixed");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        ModelAndView mv = sut.editColumnForm(fixedColId);
        assertEquals("redirect:/board", mv.getViewName());
    }

    @Test
    void updateColumn_fixedColumn_shouldNotChangeTitle() {
        long fixedColId = 777L;
        Column fixedCol = new Column(fixedColId, "fix", "Original Title");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);

        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        String view = sut.updateColumn(fixedColId, "Hacked Title");

        assertEquals("redirect:/board", view);
        assertEquals("Original Title", fixedCol.getTitle());
        verify(boardRepo, never()).save(any());
    }

    @Test
    void moveIssue_next_shouldMoveIssueToNextColumnAndPersist() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column first = new Column(101L, "col-1", "First");
        Column second = new Column(102L, "col-2", "Second");
        board.addColumn(first);
        board.addColumn(second);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issue = new Issue(TEST_ISSUE_ID, "Test issue", first.getKey());
        when(issueRepo.find(TEST_ISSUE_ID)).thenReturn(issue);

        String view = sut.moveIssue(TEST_ISSUE_ID, "next");

        assertEquals("redirect:/board", view);
        assertEquals(second.getKey(), issue.getColumnKey());
        verify(issueRepo, times(1)).persist(issue);
    }

    @Test
    void board_shouldAssignOrphanIssuesToDefaultColumn() {
        Board b = new Board(TEST_BOARD_ID, "Default");
        Column defaultCol = new Column(10L, "todo", "To Do");
        b.addColumn(defaultCol);

        when(boardRepo.findAll()).thenReturn(List.of(b));

        Issue orphanIssue = new Issue(99L, "Orphan Issue", "lost-column");
        when(issueRepo.findAll()).thenReturn(List.of(orphanIssue));

        sut.board();

        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepo).persist(issueCaptor.capture());

        assertEquals("todo", issueCaptor.getValue().getColumnKey());
    }

    @Test
    void reorderColumn_validMove_shouldUpdateOrderAndSave() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Backlog");
        fixed.setFixed(true); // Sécurité
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");

        board.addColumn(fixed);
        board.addColumn(colA);
        board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(102L, 1);

        assertEquals("OK", response);
        // [Fixed, B, A] -> index 1 est B
        assertEquals(colB, board.getColumns().get(1));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveToIndexZero_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed);
        board.addColumn(new Column(101L, "col-a", "Col A"));

        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Action : Essayer de déplacer Col A tout au début (index 0)
        String response = sut.reorderColumn(101L, 0);

        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveFixedColumn_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed);
        board.addColumn(new Column(101L, "col-a", "Col A"));

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(100L, 1);

        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void moveColumnInternal_negativeIndex_shouldReturnFalse() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        String response = sut.reorderColumn(100L, -5);
        assertEquals("ERROR: Invalid move", response);
    }

    @Test
    public void removeColumn_shouldRemoveEmptyColumn() {
        // Arrange
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);

        Board board = new Board("Test board");
        boardRepo.save(board);

        Column col = new Column("TODO", "A faire");
        col = boardRepo.addColumn(board.getId(), col);
        long columnId = col.getId();

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        String view = controller.removeColumn(columnId, redirectAttributes);

        // Assert
        assertEquals("redirect:/board", view);
        Board reloaded = boardRepo.findById(board.getId()).orElseThrow();
        assertTrue(reloaded.getColumns().isEmpty());
    }

    @Test
    public void removeColumn_shouldNotRemoveNonEmptyColumn_andSetErrorMessage() {
        // Arrange
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);

        Board board = new Board("Test board");
        boardRepo.save(board);

        Column col = new Column("IN_PROGRESS", "En cours");
        col = boardRepo.addColumn(board.getId(), col);

        // On crée une issue dans cette colonne
        Issue issue = new Issue();
        issue.setTitle("Une story");
        issue.setColumnKey(col.getKey());
        issueRepo.persist(issue);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        String view = controller.removeColumn(col.getId(), redirectAttributes);

        // Assert
        assertEquals("redirect:/board", view);
        Board reloaded = boardRepo.findById(board.getId()).orElseThrow();
        assertFalse(reloaded.getColumns().isEmpty()); // N'a pas été supprimé
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
    }
}