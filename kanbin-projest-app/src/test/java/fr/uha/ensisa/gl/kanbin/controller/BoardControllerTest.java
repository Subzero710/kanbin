package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

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
        MockitoAnnotations.openMocks(this);
        testBoard = new Board(TEST_BOARD_ID, "Test Board");
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    // --- TESTS COMMUNS ---

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

        sut.addColumn(newColumnTitle);
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
        sut.addColumn("Ma Colonne");
        verify(boardRepo).save(any(Board.class));
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), any(Column.class));
    }

    @Test
    void homeRedirect_shouldRedirectToBoard() {
        String viewName = sut.homeRedirect();
        assertEquals("redirect:/board", viewName);
    }

    @Test
    void postRemoveColumn_shouldSaveBoardWithoutThatColumn() {
        long columnToRemoveId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        assertTrue(testBoard.getColumns().stream().anyMatch(c -> c.getId() == columnToRemoveId));

        String viewName = sut.removeColumn(columnToRemoveId);
        assertEquals("redirect:/board", viewName);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo, times(1)).save(captor.capture());
        Board saved = captor.getValue();
        assertTrue(saved.getColumns().stream().noneMatch(c -> c.getId() == columnToRemoveId));
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
    void moveIssue_prev_shouldMoveIssueToPreviousColumnAndPersist() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column first = new Column(101L, "col-1", "First");
        Column second = new Column(102L, "col-2", "Second");
        board.addColumn(first);
        board.addColumn(second);

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

    // --- TESTS POUR LE RENOMMAGE (VOTRE FEATURE) ---

    @Test
    void editColumnForm_shouldShowEditView_whenColumnExists() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        ModelAndView mv = sut.editColumnForm(colId);
        assertEquals("edit-column", mv.getViewName());
        Column c = (Column) mv.getModel().get("column");
        assertEquals(colId, c.getId());
    }

    @Test
    void editColumnForm_shouldRedirect_whenColumnDoesNotExist() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        ModelAndView mv = sut.editColumnForm(999L);
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
        Board savedBoard = boardCaptor.getValue();
        Column updatedCol = savedBoard.getColumns().stream()
                .filter(c -> c.getId() == colId).findFirst().get();
        assertEquals(newTitle, updatedCol.getTitle());
    }

    // --- TESTS POUR LES SOUS-COLONNES (VENANT DE DEVELOP) ---

    @Test
    void addColumn_shouldCreateParentWithTwoSubColumns() {
        when(boardRepo.findAll()).thenReturn(List.of());
        when(boardRepo.save(any(Board.class))).thenAnswer(i -> {
            Board b = i.getArgument(0); b.setId(1L); return b;
        });

        sut.addColumn("ColonnePrincipale");

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(boardRepo).addColumn(eq(1L), columnCaptor.capture());

        Column createdCol = columnCaptor.getValue();

        assertEquals("ColonnePrincipale", createdCol.getTitle());
        assertEquals(2, createdCol.getSubColumns().size());
        assertEquals("À Faire", createdCol.getSubColumns().get(0).getTitle());
        assertEquals("En Cours", createdCol.getSubColumns().get(1).getTitle());

    }

    @Test
    void moveIssue_nextFromEndOfParent1_shouldJumpToStartOfParent2() {
        Board b = new Board(1L, "Board");

        // Simulation des colonnes avec sous-colonnes
        Column parent1 = new Column("p1", "Parent 1");
        parent1.addSubColumn(new Column("p1-todo", "Todo"));
        parent1.addSubColumn(new Column("p1-wip", "Wip"));

        Column parent2 = new Column("p2", "Parent 2");
        parent2.addSubColumn(new Column("p2-todo", "Todo"));

        b.addColumn(parent1);
        b.addColumn(parent2);

        when(boardRepo.findAll()).thenReturn(List.of(b));

        Issue issue = new Issue(1L, "Task 1");
        issue.setColumnKey("p1-wip");
        // On utilise bien issueRepo (et pas issues)
        when(issueRepo.find(99L)).thenReturn(issue);

        sut.moveIssue(99L, "next");

        // On vérifie sur issueRepo
        verify(issueRepo).persist(issue);
        assertEquals("p2-todo", issue.getColumnKey());
    }

    @Test
    void board_withMixedColumns_shouldMapAllKeys() {

        Board b = new Board(1L, "Mixte");

        // Cas  : Colonne AVEC sous-colonnes
        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column("sub1", "Sub 1"));
        parent.addSubColumn(new Column("sub2", "Sub 2")); // Couvre le cas où defaultKey n'est plus null
        b.addColumn(parent);

        // Cas  : Colonne SANS sous-colonnes
        Column simple = new Column("simple", "Simple");
        b.addColumn(simple);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of());

        sut.board();

    }
}