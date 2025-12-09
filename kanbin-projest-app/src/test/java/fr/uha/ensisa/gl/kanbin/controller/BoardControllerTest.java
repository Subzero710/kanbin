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

    // --- TESTS BASIQUES ---

    @Test
    void board_shouldReturnBoardView() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        when(issueRepo.findAll()).thenReturn(List.of());
        ModelAndView mv = sut.board();
        assertEquals("board", mv.getViewName());
    }

    @Test
    void homeRedirect_shouldRedirectToBoard() {
        String viewName = sut.homeRedirect();
        assertEquals("redirect:/board", viewName);
    }

    // --- TESTS AJOUT COLONNE (Add Column) ---

    @Test
    void addColumn_whenBoardExists_shouldAddColumnToIt() {
        String newColumnTitle = "Test Column #29";
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        sut.addColumn(newColumnTitle, "simple");
        verify(boardRepo, times(1)).addColumn(eq(TEST_BOARD_ID), any(Column.class));
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
        sut.addColumn("ColonnePrincipale", "double");

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), columnCaptor.capture());
        Column createdCol = columnCaptor.getValue();
        assertEquals("ColonnePrincipale", createdCol.getTitle());
        assertEquals(2, createdCol.getSubColumns().size());
    }

    // --- TESTS SUPPRESSION (Remove Column) & SÉCURITÉ ---

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
    }

    // --- TESTS ÉDITION (Edit/Update) & SÉCURITÉ ---

    @Test
    void editColumnForm_shouldShowEditView_whenColumnExists() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        ModelAndView mv = sut.editColumnForm(colId);
        assertEquals("edit-column", mv.getViewName());
        assertEquals(colId, ((Column) mv.getModel().get("column")).getId());
    }

    @Test
    void editColumnForm_shouldRedirect_whenColumnDoesNotExist() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        ModelAndView mv = sut.editColumnForm(999L);
        assertEquals("redirect:/board", mv.getViewName());
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
    void updateColumn_shouldChangeTitleAndSaveBoard() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        String newTitle = "Renamed Column";

        String view = sut.updateColumn(colId, newTitle);
        assertEquals("redirect:/board", view);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(boardCaptor.capture());
        Column updatedCol = boardCaptor.getValue().getColumns().stream()
                .filter(c -> c.getId() == colId).findFirst().get();
        assertEquals(newTitle, updatedCol.getTitle());
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
        verify(boardRepo, never()).save(any());
    }

    // --- TESTS DÉPLACEMENT STORIES (Move Issue) ---

    @Test
    void moveIssue_next_shouldMoveIssueToNextColumnAndPersist() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column first = new Column(101L, "col-1", "First");
        Column second = new Column(102L, "col-2", "Second");
        board.addColumn(first); board.addColumn(second);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issue = new Issue(TEST_ISSUE_ID, "Test issue", first.getKey());
        when(issueRepo.find(TEST_ISSUE_ID)).thenReturn(issue);

        String view = sut.moveIssue(TEST_ISSUE_ID, "next");
        assertEquals("redirect:/board", view);
        assertEquals(second.getKey(), issue.getColumnKey());
        verify(issueRepo, times(1)).persist(issue);
    }

    @Test
    void moveIssue_prev_shouldMoveIssueToPreviousColumnAndPersist() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column first = new Column(101L, "col-1", "First");
        Column second = new Column(102L, "col-2", "Second");
        board.addColumn(first); board.addColumn(second);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issue = new Issue(TEST_ISSUE_ID, "Test issue", second.getKey());
        when(issueRepo.find(TEST_ISSUE_ID)).thenReturn(issue);

        String view = sut.moveIssue(TEST_ISSUE_ID, "prev");
        assertEquals("redirect:/board", view);
        assertEquals(first.getKey(), issue.getColumnKey());
        verify(issueRepo, times(1)).persist(issue);
    }

    @Test
    void moveIssue_prevOnFirstColumn_shouldNotPersistAndLeaveColumnUnchanged() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        Issue issue = new Issue(TEST_ISSUE_ID, "Test issue", "initial");
        when(issueRepo.find(TEST_ISSUE_ID)).thenReturn(issue);

        String view = sut.moveIssue(TEST_ISSUE_ID, "prev");
        assertEquals("redirect:/board", view);
        assertEquals("initial", issue.getColumnKey());
        verify(issueRepo, never()).persist(any(Issue.class));
    }

    @Test
    void moveIssue_nextFromEndOfParent1_shouldJumpToStartOfParent2() {
        Board b = new Board(1L, "Board");
        Column parent1 = new Column("p1", "Parent 1");
        parent1.addSubColumn(new Column("p1-todo", "Todo"));
        parent1.addSubColumn(new Column("p1-wip", "Wip"));
        Column parent2 = new Column("p2", "Parent 2");
        parent2.addSubColumn(new Column("p2-todo", "Todo"));
        b.addColumn(parent1); b.addColumn(parent2);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        Issue issue = new Issue(1L, "Task 1");
        issue.setColumnKey("p1-wip");
        when(issueRepo.find(99L)).thenReturn(issue);

        sut.moveIssue(99L, "next");

        verify(issueRepo).persist(issue);
        assertEquals("p2-todo", issue.getColumnKey());
    }

    // --- TESTS LOGIQUE GLOBALE ---

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
    void board_withMixedColumns_shouldMapAllKeys() {
        Board b = new Board(1L, "Mixte");
        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column("sub1", "Sub 1"));
        parent.addSubColumn(new Column("sub2", "Sub 2"));
        b.addColumn(parent);
        Column simple = new Column("simple", "Simple");
        b.addColumn(simple);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of());
        assertDoesNotThrow(() -> sut.board());
    }

    // --- TESTS REORDER (Drag & Drop) ---

    @Test
    void reorderColumn_validMove_shouldUpdateOrderAndSave() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Backlog");
        fixed.setFixed(true);
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");
        board.addColumn(fixed); board.addColumn(colA); board.addColumn(colB);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(102L, 1);
        assertEquals("OK", response);
        assertEquals(colB, board.getColumns().get(1));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveToIndexZero_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed); board.addColumn(new Column(101L, "col-a", "Col A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(101L, 0);
        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveToIndexGreaterThanSize_shouldAppendToEnd() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");
        board.addColumn(fixed); board.addColumn(colA); board.addColumn(colB);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(101L, 10); // Index hors limite

        assertEquals("OK", response);
        // Ordre attendu : Fixed, B, A
        List<Column> result = board.getColumns();
        assertEquals(colA, result.get(2));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveFixedColumn_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed); board.addColumn(new Column(101L, "col-a", "Col A"));
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

    // --- TESTS COMPLEXES (Avec Repo Memoire) ---

    @Test
    public void removeColumn_shouldRemoveEmptyColumn() {
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);
        Board board = new Board("Test board");
        boardRepo.save(board);
        Column col = boardRepo.addColumn(board.getId(), new Column("TODO", "A faire"));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.removeColumn(col.getId(), redirectAttributes);

        assertEquals("redirect:/board", view);
        assertTrue(boardRepo.findById(board.getId()).get().getColumns().isEmpty());
    }

    @Test
    public void removeColumn_shouldNotRemoveNonEmptyColumn_andSetErrorMessage() {
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);
        Board board = new Board("Test board");
        boardRepo.save(board);
        Column col = boardRepo.addColumn(board.getId(), new Column("IN_PROGRESS", "En cours"));
        Issue issue = new Issue(); issue.setTitle("Story"); issue.setColumnKey(col.getKey());
        issueRepo.persist(issue);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.removeColumn(col.getId(), redirectAttributes);

        assertEquals("redirect:/board", view);
        assertFalse(boardRepo.findById(board.getId()).get().getColumns().isEmpty());
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
    }
}