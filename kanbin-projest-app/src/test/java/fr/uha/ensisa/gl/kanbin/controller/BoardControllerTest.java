package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.BoardRepoMem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = sut.removeColumn(columnToRemoveId, redirectAttributes );
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

    // --- TESTS POUR LES SOUS-COLONNES ---

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

    // TESTS POUR LE REORDERING (DRAG & DROP)
    @Test
    void reorderColumn_validMove_shouldUpdateOrderAndSave() {
        // Setup : Un board avec 3 colonnes (0:Fixed, 1:A, 2:B)
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Backlog (Fixed)");
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");

        board.addColumn(fixed);
        board.addColumn(colA);
        board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Action : Déplacer "Col B" (index actuel 2) vers l'index 1 (devant A)
        String response = sut.reorderColumn(102L, 1);

        // Vérification
        assertEquals("OK", response);

        // L'ordre doit avoir changé : [Fixed, B, A]
        assertEquals(fixed, board.getColumns().get(0));
        assertEquals(colB, board.getColumns().get(1)); // B est passé devant
        assertEquals(colA, board.getColumns().get(2));

        // La sauvegarde doit avoir été appelée
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveToIndexZero_shouldFail() {
        // Setup standard
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        board.addColumn(new Column(100L, "fixed", "Fixed"));
        board.addColumn(new Column(101L, "col-a", "Col A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Action : Essayer de déplacer Col A tout au début (index 0)
        String response = sut.reorderColumn(101L, 0);

        // Vérification
        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any()); // Pas de sauvegarde
    }

    @Test
    void reorderColumn_moveFixedColumn_shouldFail() {
        // Setup standard
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        board.addColumn(fixed);
        board.addColumn(new Column(101L, "col-a", "Col A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Action : Essayer de déplacer la colonne fixe (ID 100) vers la fin (index 1)
        String response = sut.reorderColumn(100L, 1);

        // Vérification
        assertEquals("ERROR: Invalid move", response);

        // L'ordre ne doit pas changer
        assertEquals(fixed, board.getColumns().get(0));
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveToIndexGreaterThanSize_shouldAppendToEnd() {
        // 1. SETUP : Board [Fixed, A, B]
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");

        board.addColumn(fixed);
        board.addColumn(colA);
        board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        // 2. ACTION : Déplacer "Col A" (index 1) vers un index très grand (ex: 10)
        String response = sut.reorderColumn(101L, 10);

        // 3. VÉRIFICATION
        assertEquals("OK", response);

        List<Column> result = board.getColumns();
        assertEquals(3, result.size());

        // L'ordre doit être : [Fixed, B, A]
        assertEquals(fixed, result.get(0));
        assertEquals(colB, result.get(1));
        assertEquals(colA, result.get(2), "Col A doit être placée tout à la fin");

        verify(boardRepo).save(board);
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
        assertTrue(reloaded.getColumns().isEmpty(), "La colonne vide devrait être supprimée");

        assertFalse(redirectAttributes.getFlashAttributes().containsKey("errorMessage"),
                "Aucun message d'erreur ne doit être présent pour une colonne vide");
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
        long columnId = col.getId();

        // On crée une issue dans cette colonne
        Issue issue = new Issue();
        issue.setTitle("Une story");
        issue.setColumnKey(col.getKey());
        issueRepo.persist(issue);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        String view = controller.removeColumn(columnId, redirectAttributes);

        // Assert
        assertEquals("redirect:/board", view);

        Board reloaded = boardRepo.findById(board.getId()).orElseThrow();
        assertFalse(reloaded.getColumns().isEmpty(),
                "La colonne ne doit pas être supprimée si elle contient des issues");

        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"),
                "Un message d'erreur doit être ajouté en flash");
        assertEquals("Impossible de supprimer une colonne non vide",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }


}