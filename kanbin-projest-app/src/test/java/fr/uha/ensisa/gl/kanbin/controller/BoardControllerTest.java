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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final String BOARD_NAME = "Test Board";

    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = new Board(TEST_BOARD_ID, BOARD_NAME);
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    @Test
    void board_shouldReturnBoardView() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        when(issueRepo.findAll()).thenReturn(List.of());
        ModelAndView mv = sut.board();
        assertEquals("board", mv.getViewName());
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

    // --- TESTS ÉDITION (Edit/Update) ---
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

        String view = sut.updateColumn(colId, newTitle, null);
        assertEquals("redirect:/board", view);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(boardCaptor.capture());
        Column updatedCol = boardCaptor.getValue().getColumns().stream()
                .filter(c -> c.getId() == colId).findFirst().orElseThrow();
        assertEquals(newTitle, updatedCol.getTitle());
    }

    @Test
    void updateColumn_fixedColumn_shouldNotChangeTitle() {
        long fixedColId = 777L;
        Column fixedCol = new Column(fixedColId, "fix", "Original Title");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        String view = sut.updateColumn(fixedColId, "Hacked Title", null);
        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void board_shouldAssignOrphanIssuesToDefaultColumn() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
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


    // --- TESTS POUR LES SOUS-COLONNES ---

    @Test
    void addColumn_shouldCreateParentWithTwoSubColumns() {
        when(boardRepo.findAll()).thenReturn(List.of());
        when(boardRepo.save(any(Board.class))).thenAnswer(i -> {
            Board b = i.getArgument(0); b.setId(1L); return b;
        });

        sut.addColumn("ColonnePrincipale", "double");

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(boardRepo).addColumn(eq(1L), columnCaptor.capture());

        Column createdCol = columnCaptor.getValue();

        assertEquals("ColonnePrincipale", createdCol.getTitle());
        assertEquals(2, createdCol.getSubColumns().size());
        assertEquals("À Faire", createdCol.getSubColumns().get(0).getTitle());
        assertEquals("En Cours", createdCol.getSubColumns().get(1).getTitle());
    }

    @Test
    void board_withMixedColumns_shouldMapAllKeys() {
        Board b = new Board(1L, BOARD_NAME);

        // Cas  : Colonne AVEC sous-colonnes
        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column("sub1", "Sub 1"));
        parent.addSubColumn(new Column("sub2", "Sub 2"));
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
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column fixed = new Column(100L, "fixed", "Backlog (Fixed)");
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");
        board.addColumn(fixed); board.addColumn(colA); board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(102L, 1);

        assertEquals("OK", response);

        // L'ordre doit avoir changé : [Fixed, B, A]
        assertEquals(fixed, board.getColumns().get(0));
        assertEquals(colB, board.getColumns().get(1));
        assertEquals(colA, board.getColumns().get(2));

        // La sauvegarde doit avoir été appelée
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveToIndexZero_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(new Column(100L, "fixed", "Fixed"));
        board.addColumn(new Column(101L, "col-a", "Col A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(101L, 0);

        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveFixedColumn_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed); board.addColumn(new Column(101L, "col-a", "Col A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Action : Essayer de déplacer la colonne fixe (ID 100) vers la fin (index 1)
        String response = sut.reorderColumn(100L, 1);
        assertEquals("ERROR: Invalid move", response);

        // L'ordre ne doit pas changer
        assertEquals(fixed, board.getColumns().getFirst());
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveToIndexGreaterThanSize_shouldAppendToEnd() {
        // 1. SETUP : Board [Fixed, A, B]
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column fixed = new Column(100L, "fixed", "Fixed");
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");
        board.addColumn(fixed); board.addColumn(colA); board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(101L, 10);

        assertEquals("OK", response);

        List<Column> result = board.getColumns();
        assertEquals(3, result.size());

        // L'ordre doit être : [Fixed, B, A]
        assertEquals(fixed, result.get(0));
        assertEquals(colB, result.get(1));
        assertEquals(colA, result.get(2));
        verify(boardRepo).save(board);
    }

    @Test
    public void removeColumn_shouldRemoveEmptyColumn() {
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);

        Board board = new Board(BOARD_NAME);
        boardRepo.save(board);
        Column col = boardRepo.addColumn(board.getId(), new Column("TODO", "A faire"));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.removeColumn(col.getId(), redirectAttributes);

        assertEquals("redirect:/board", view);
        assertTrue(boardRepo.findById(board.getId()).orElseThrow().getColumns().isEmpty());
    }

    @Test
    public void removeColumn_shouldNotRemoveNonEmptyColumn_andSetErrorMessage() {
        BoardRepoMem boardRepo = new BoardRepoMem();
        IssueRepoMem issueRepo = new IssueRepoMem();
        BoardController controller = new BoardController(boardRepo, issueRepo);

        Board board = new Board(BOARD_NAME);
        boardRepo.save(board);

        Column col = new Column("IN_PROGRESS", "En cours");
        col = boardRepo.addColumn(board.getId(), col);
        long columnId = col.getId();

        Issue issue = new Issue();
        issue.setTitle("Une story");
        issue.setColumnKey(col.getKey());

        issueRepo.persist(issue);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.removeColumn(columnId, redirectAttributes);

        assertEquals("redirect:/board", view);

        Board reloaded = boardRepo.findById(board.getId()).orElseThrow();
        assertFalse(reloaded.getColumns().isEmpty(),
                "La colonne ne doit pas être supprimée si elle contient des issues");

        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"),
                "Un message d'erreur doit être ajouté en flash");
        assertEquals("Impossible de supprimer une colonne non vide",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void board_whenRepoEmpty_shouldCreateBoardWithSimpleBacklog() {
        when(boardRepo.findAll()).thenReturn(List.of());
        when(boardRepo.save(any(Board.class))).thenAnswer(i -> i.getArgument(0));

        sut.board();

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());

        Board createdBoard = captor.getValue();
        assertEquals(BOARD_NAME, createdBoard.getName());
        assertEquals("Backlog", createdBoard.getColumns().getFirst().getTitle());
    }


// --- TESTS DRAG & DROP STORIES ---

    @Test
    void moveIssueDnD_shouldReturn400_whenIssueNotFound() {
        // Cas : ID inexistant
        when(issueRepo.find(999L)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(999L, "any-col");

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Story introuvable.", response.getBody().get("message"));
        verify(issueRepo, never()).persist(any());
    }

    @Test
    void moveIssueDnD_shouldReturn400_whenTargetColumnNotFound() {
        // Cas : Colonne cible inexistante
        long issueId = 1L;
        Issue issue = new Issue(issueId, "Story");
        when(issueRepo.find(issueId)).thenReturn(issue);

        // Board vide ou sans cette colonne
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "unknown-key");

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Colonne cible introuvable.", response.getBody().get("message"));
        verify(issueRepo, never()).persist(any());
    }

    @Test
    void moveIssueDnD_shouldMove_whenNoWipLimit() {
        // Cas nominal : Pas de limite
        long issueId = 1L;
        String targetKey = "col-free";
        Issue issue = new Issue(issueId, "Story", "old-col");

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(new Column(targetKey, "Free Column")); // WipLimit null par défaut

        when(issueRepo.find(issueId)).thenReturn(issue);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(targetKey, issue.getColumnKey());
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldFail_whenWipLimitReached_inSimpleColumn() {
        // Cas : Limite atteinte dans une colonne simple
        long issueId = 1L;
        String targetKey = "col-limited";

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column col = new Column(targetKey, "Limited");
        col.setWipLimit(1); // Limite = 1
        board.addColumn(col);

        Issue issueToMove = new Issue(issueId, "Mover", "old-col");
        Issue existingIssue = new Issue(2L, "Blocker", targetKey);

        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        when(issueRepo.findAll()).thenReturn(List.of(existingIssue, issueToMove));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(((String)response.getBody().get("message")).contains("Limite atteinte"));


        assertEquals("old-col", issueToMove.getColumnKey());
        verify(issueRepo, never()).persist(issueToMove);
    }

    @Test
    void moveIssueDnD_shouldFail_whenWipLimitReached_inParentColumn() {

        long issueId = 1L;

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        parent.setWipLimit(1); // Le parent limite à 1

        Column sub1 = new Column("sub-1", "Sub 1");
        Column sub2 = new Column("sub-2", "Sub 2");
        parent.addSubColumn(sub1);
        parent.addSubColumn(sub2);
        board.addColumn(parent);

        Issue issueToMove = new Issue(issueId, "Mover", "outside");
        Issue existingInSub2 = new Issue(2L, "Blocker", "sub-2");

        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of(existingInSub2, issueToMove));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "sub-1");

        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"), "Devrait échouer car la limite du parent est atteinte");
        assertTrue(((String)response.getBody().get("message")).contains("Parent"));
    }

    @Test
    void moveIssueDnD_shouldAllowMove_ifAlreadyInLogicalColumn() {

        long issueId = 1L;

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        parent.setWipLimit(1); // Limite stricte à 1

        Column subTodo = new Column("todo", "Todo");
        Column subWip = new Column("wip", "Wip");
        parent.addSubColumn(subTodo);
        parent.addSubColumn(subWip);
        board.addColumn(parent);

        Issue issue = new Issue(issueId, "Mover", "todo");

        when(issueRepo.find(issueId)).thenReturn(issue);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of(issue)); // Seule issue présente

        // Action : Déplacer de Todo vers Wip
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "wip");

        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"), "Devrait réussir car l'issue est déjà comptée dans la limite du parent");
        assertEquals("wip", issue.getColumnKey());
    }

    @Test
    void updateColumn_nonExistingColumn_shouldNotSaveBoard() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        String view = sut.updateColumn(999L, "New Title", null);

        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_nonExistingColumn_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");
        board.addColumn(colA);
        board.addColumn(colB);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(999L, 1);

        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void reorderColumn_moveColumnIntoFixedSlot_shouldFail_CORRECTED() {

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column movable = new Column(101L, "col-a", "Col A");
        Column fixedSlot = new Column(100L, "fixed", "Fixed");
        fixedSlot.setFixed(true);

        board.addColumn(movable);
        board.addColumn(fixedSlot);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(101L, 1);


        assertEquals("OK", response);
        verify(boardRepo).save(any());
    }

    @Test
    void collectLogicalColumnKeys_orphanColumn_shouldUseItsOwnKey() throws Exception {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column orphan = new Column("orphan", "Orphan");

        java.lang.reflect.Method m = BoardController.class
                .getDeclaredMethod("collectLogicalColumnKeys", Board.class, Column.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) m.invoke(sut, board, orphan);

        assertEquals(List.of("orphan"), keys);
    }

    @Test
    void countIssuesInColumn_whenNoLogicalKeys_shouldReturnZero() throws Exception {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column column = new Column(null, "NoKey");

        java.lang.reflect.Method m = BoardController.class
                .getDeclaredMethod("countIssuesInColumn", Board.class, Column.class);
        m.setAccessible(true);

        long count = (long) m.invoke(sut, board, column);
        assertEquals(0L, count);
    }

    @Test
    void resolveWipLimit_shouldReturnTargetLimit_whenColumnNotInBoard() throws Exception {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column orphan = new Column("wip-orphan", "Orphan WIP");
        orphan.setWipLimit(5);

        java.lang.reflect.Method m = BoardController.class
                .getDeclaredMethod("resolveWipLimit", Board.class, Column.class);
        m.setAccessible(true);

        Integer limit = (Integer) m.invoke(sut, board, orphan);
        assertEquals(5, limit);
    }

    @Test
    void board_withSubColumnWithoutKey_shouldIgnoreNullKeysInWipComputation() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent2", "Parent 2");
        Column subWithKey = new Column("p2-todo", "Todo");
        Column subWithoutKey = new Column(null, "No Key");

        parent.addSubColumn(subWithKey);
        parent.addSubColumn(subWithoutKey);
        b.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of());

        sut.board();
    }

    @Test
    void board_shouldHandleIssuesWithUnknownColumnKeys() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        b.addColumn(new Column("default", "Default"));

        Issue ghostIssue = new Issue(1L, "Ghost", "ghost-key");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(ghostIssue));

        sut.board();

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepo).persist(captor.capture());
        assertEquals("default", captor.getValue().getColumnKey());
    }

    @Test
    void moveIssueDnD_shouldReturnFailure_whenWipLimitReached() {
        long issueId = 60L;
        String targetKey = "col-limited";
        Issue issue = new Issue(issueId, "Story", "col-origin");

        Board b = new Board(1L, BOARD_NAME);
        Column limitedCol = new Column(targetKey, "Limited");
        limitedCol.setWipLimit(1);
        b.addColumn(limitedCol);
        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        Issue existing = new Issue(99L, "Blocker", targetKey);
        when(issueRepo.findAll()).thenReturn(List.of(existing, issue));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(((String)response.getBody().get("message")).contains("Limite atteinte"));

        verify(issueRepo, never()).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldAllowMove_whenWipLimitNotReached() {
        long issueId = 61L;
        String targetKey = "col-limited-open";
        Issue issue = new Issue(issueId, "Story", "col-origin");

        Board b = new Board(1L, BOARD_NAME);
        Column limitedCol = new Column(targetKey, "Limited But Empty");
        limitedCol.setWipLimit(5);
        b.addColumn(limitedCol);
        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        when(issueRepo.findAll()).thenReturn(List.of(issue));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        verify(issueRepo).persist(issue);
    }

    @Test
    void reorderColumn_shouldFail_whenMovingFixedColumn() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed);
        board.addColumn(new Column(101L, "col-a", "A"));

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String result = sut.reorderColumn(100L, 1); // Essai de bouger la fixe
        assertEquals("ERROR: Invalid move", result);
    }

    @Test
    void reorderColumn_shouldFail_whenTargetIndexInvalid() {
        String result = sut.reorderColumn(101L, -5);
        assertEquals("ERROR: Invalid move", result);
    }

    @Test
    void updateColumn_shouldRedirect_whenColumnNotFound() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        String result = sut.updateColumn(9999L, "New Title", 0);

        assertEquals("redirect:/board", result);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void removeColumn_shouldRedirect_whenColumnNotFound() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String result = sut.removeColumn(9999L, redirectAttributes);

        assertEquals("redirect:/board", result);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void removeColumn_whenColumnNotFound_shouldRedirect() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = sut.removeColumn(9999L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void removeColumn_shouldIgnoreSubColumnsWithNullKeys() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column(100L, "parent", "Parent");
        Column corruptSub = new Column();
        corruptSub.setTitle("Corrupt");
        parent.addSubColumn(corruptSub);

        board.addColumn(parent);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(100L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void removeColumn_shouldRemoveParentColumn_andCheckSubKeys() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column(200L, "parent", "Parent");
        parent.addSubColumn(new Column("sub1", "Sub 1"));
        parent.addSubColumn(new Column("sub2", "Sub 2"));
        board.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(200L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
        assertTrue(board.getColumns().isEmpty());
    }

    @Test
    void removeColumn_shouldNotRemoveParentColumn_whenSubColumnNotEmpty() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column(200L, "parent", "Parent");
        Column sub = new Column("sub-1", "Sub 1");
        parent.addSubColumn(sub);
        board.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issue = new Issue(1L, "Story", "sub-1");
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(200L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    void removeColumn_shouldRemoveParentColumn_whenEmpty() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column(200L, "parent", "Parent");
        parent.addSubColumn(new Column("sub-1", "Sub 1"));
        board.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(200L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void removeColumn_shouldHandleParentColumnWithSubColumns() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column(200L, "parent", "Parent");
        Column sub1 = new Column("sub1", "Sub 1");
        parent.addSubColumn(sub1);
        board.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(200L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void removeColumn_shouldIgnoreNullKeys_SafetyCheck() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column corruptCol = new Column();
        corruptCol.setId(300L);
        corruptCol.setTitle("Corrupt");
        Column corruptSub = new Column();
        corruptSub.setTitle("Sub Corrupt");
        corruptCol.addSubColumn(corruptSub);
        board.addColumn(corruptCol);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(300L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void moveIssueDnD_shouldIgnoreWipLimit_whenLimitIsZero() {
        long issueId = 10L;
        String targetKey = "col-unlimited";
        Board b = new Board(1L, BOARD_NAME);
        Column unlimitedCol = new Column(targetKey, "Unlimited");
        unlimitedCol.setWipLimit(0);
        b.addColumn(unlimitedCol);

        Issue issue = new Issue(issueId, "Story", "old");
        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, targetKey);
        verify(issueRepo).persist(issue);
    }

    @Test
    void board_shouldHandleIssuesWithNullOrUnknownKeys() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        b.addColumn(new Column("default", "Default"));
        Issue validIssue = new Issue(1L, "Valid", "default");
        Issue nullKeyIssue = new Issue(2L, "Null Key", null);
        Issue unknownKeyIssue = new Issue(3L, "Unknown", "ghost-key");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(validIssue, nullKeyIssue, unknownKeyIssue));

        ModelAndView mv = sut.board();
        @SuppressWarnings("unchecked")
        Map<String, List<Issue>> map = (Map<String, List<Issue>>) mv.getModel().get("issuesByColumn");
        assertEquals(3, map.get("default").size());
    }

    @Test
    void updateColumn_shouldResetWipLimit_whenValueIsZeroOrNegative() {
        long colId = 100L;
        Column col = new Column(colId, "col", "Title");
        col.setWipLimit(5);
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(col);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        sut.updateColumn(colId, "Title", 0);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        assertEquals(0, captor.getValue().getColumns().getFirst().getWipLimit());
    }

    @Test
    void homeRedirect_shouldReturnRedirectToBoard() {
        String view = sut.homeRedirect();
        assertEquals("redirect:/board", view);
    }

    @Test
    void addColumn_doubleType_shouldCreateCorrectHierarchy() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        sut.addColumn("Dev", "double");

        ArgumentCaptor<Column> captor = ArgumentCaptor.forClass(Column.class);
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), captor.capture());
        Column parent = captor.getValue();
        assertEquals(2, parent.getSubColumns().size());
    }

    @Test
    void updateColumn_withPositiveWipLimit_shouldSaveThatLimit() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        sut.updateColumn(colId, "Title", 5);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        assertEquals(5, captor.getValue().getColumns().getFirst().getWipLimit());
    }

    @Test
    void updateColumn_withNegativeWipLimit_shouldSaveZero() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        sut.updateColumn(colId, "Title", -1);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        assertEquals(0, captor.getValue().getColumns().getFirst().getWipLimit());
    }

    @Test
    void updateColumn_withZeroWipLimit_shouldSaveZero() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        sut.updateColumn(colId, "Title", 0);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        assertEquals(0, captor.getValue().getColumns().getFirst().getWipLimit());
    }

    @Test
    void moveIssueDnD_shouldTraverseEmptySubColumnsWithoutError() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column simpleCol = new Column("simple", "Simple");
        Column targetCol = new Column("target", "Target");
        board.addColumn(simpleCol);
        board.addColumn(targetCol);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Mover", "simple");
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, "target");
        verify(issueRepo).persist(issue);
        assertEquals("target", issue.getColumnKey());
    }

    @Test
    void collectLogicalKeys_shouldHandleNullMainColumnKey() throws Exception {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column corruptMain = new Column();
        corruptMain.setId(999L);

        java.lang.reflect.Method m = BoardController.class
                .getDeclaredMethod("collectLogicalColumnKeys", Board.class, Column.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) m.invoke(sut, board, corruptMain);
        assertTrue(keys.isEmpty());
    }

    @Test
    void removeColumn_shouldIgnoreIssuesWithNullOrDifferentKeys() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column colToRemove = new Column(100L, "remove-me", "Remove Me");
        board.addColumn(colToRemove);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issueNull = new Issue(1L, "Null Key", null);
        Issue issueOther = new Issue(2L, "Other Key", "safe-col");
        when(issueRepo.findAll()).thenReturn(List.of(issueNull, issueOther));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(100L, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void moveIssueDnD_shouldIterateOverNonMatchingSubColumns() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        Column sub1 = new Column("sub-1", "Sub 1");
        Column sub2 = new Column("sub-2", "Sub 2");
        parent.addSubColumn(sub1);
        parent.addSubColumn(sub2);
        board.addColumn(parent);

        long issueId = 50L;
        Issue issue = new Issue(issueId, "Mover", "start");
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, "sub-2");
        verify(issueRepo).persist(issue);
        assertEquals("sub-2", issue.getColumnKey());
    }

    @Test
    void removeColumn_shouldIgnoreIssuesWithNullKey() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column col = new Column(100L, "remove-me", "Remove Me");
        board.addColumn(col);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        Issue nullKeyIssue = new Issue();
        nullKeyIssue.setId(1L);
        nullKeyIssue.setTitle("Ghost Issue");
        when(issueRepo.findAll()).thenReturn(List.of(nullKeyIssue));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.removeColumn(100L, redirectAttributes);
        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void moveIssueDnD_shouldUseTargetColTitle_whenNoMainColumn() {
        long issueId = 10L;
        String targetKey = "simple-limited";
        Board b = new Board(1L, BOARD_NAME);
        Column simpleLimited = new Column(targetKey, "Simple Limited");
        simpleLimited.setWipLimit(1);
        b.addColumn(simpleLimited);

        Issue issue = new Issue(issueId, "Mover", "start");
        Issue blocker = new Issue(20L, "Blocker", targetKey);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);
        when(issueRepo.findAll()).thenReturn(List.of(blocker, issue));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);
        assertNotNull(response.getBody());
        String msg = (String) response.getBody().get("message");
        assertTrue(msg.contains("Simple Limited"));
    }

    @Test
    void moveIssueDnD_shouldSkipColumnsWithNoSubColumns() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column simple = new Column("simple", "Simple");
        Column target = new Column("target", "Target");
        board.addColumn(simple);
        board.addColumn(target);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Story", "simple");
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, "target");
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldSearchInSubColumnsAndContinueIfNotFound() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column("sub-a", "Sub A"));
        parent.addSubColumn(new Column("sub-b", "Sub B"));
        Column target = new Column("target", "Target");
        board.addColumn(parent);
        board.addColumn(target);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Voyageur", "sub-a");
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, "target");
        verify(issueRepo).persist(issue);
        assertEquals("target", issue.getColumnKey());
    }

    @Test
    void collectLogicalColumnKeys_shouldCollectSubColumnKeys_whenMainColumnExists() throws Exception {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        Column sub1 = new Column("sub-1", "Sub 1");
        parent.addSubColumn(sub1);

        java.lang.reflect.Method m = BoardController.class
                .getDeclaredMethod("collectLogicalColumnKeys", Board.class, Column.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) m.invoke(sut, board, sub1);
        assertTrue(keys.contains("sub-1"));
        assertEquals(1, keys.size());
    }

    @Test
    void moveIssueDnD_fullLoopCoverage_searchTraversal() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column simple = new Column("simple", "Simple");
        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column("sub-1", "Sub 1"));
        parent.addSubColumn(new Column("sub-2", "Sub 2"));
        Column target = new Column("target", "Target");
        board.addColumn(simple);
        board.addColumn(parent);
        board.addColumn(target);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Traveler", "start");
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);

        sut.moveIssueDnD(issueId, "target");
        verify(issueRepo).persist(issue);
        assertEquals("target", issue.getColumnKey());
    }

    @Test
    void moveIssueDnD_shouldHandleIssueWithNullKey() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column target = new Column("target", "Target");
        target.setWipLimit(1);
        board.addColumn(target);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Null Key Issue", null);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "target");

        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("target", issue.getColumnKey());
    }

    @Test
    void removeColumn_shouldIgnoreIssuesWithNullKey_inCheck() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column col = new Column(100L, "col", "Col");
        board.addColumn(col);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        Issue issueNull = new Issue(1L, "Ghost", null);
        when(issueRepo.findAll()).thenReturn(List.of(issueNull));

        RedirectAttributes attr = new RedirectAttributesModelMap();
        String view = sut.removeColumn(100L, attr);

        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
    }

    @Test
    void updateColumn_shouldHandleNegativeAndZeroLimits() {
        long colId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        // Test 0
        sut.updateColumn(colId, "Title", 0);
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        assertEquals(0, captor.getValue().getColumns().getFirst().getWipLimit());

        // Test Négatif
        sut.updateColumn(colId, "Title", -5);
        // (Mockito note: on capture la 2ème invocation)
        verify(boardRepo, times(2)).save(captor.capture());
        assertEquals(0, captor.getValue().getColumns().getFirst().getWipLimit());
    }

    @Test
    void board_shouldHandleIssueWithNullKey_inDisplay() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        b.addColumn(new Column("default", "Def"));
        Issue issueNull = new Issue(1L, "No Key", null);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(issueNull));

        sut.board();
        assertNotNull(issueNull.getColumnKey());
    }

    @Test
    void moveIssueDnD_shouldHandleIssueWithNullKey_inWipCalculation() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column target = new Column("target", "Target");
        target.setWipLimit(1);
        board.addColumn(target);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Moving Story", "start");
        Issue nullKeyIssue = new Issue(2L, "Corrupt", null);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);
        when(issueRepo.findAll()).thenReturn(List.of(nullKeyIssue, issue));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "target");
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("target", issue.getColumnKey());
    }

    @Test
    void board_shouldReassignIssueWithNullKey() {
        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        b.addColumn(new Column("default", "Def"));
        Issue issueNull = new Issue(1L, "No Key", null);
        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(issueNull));

        sut.board();

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepo).persist(captor.capture());
        assertNotNull(captor.getValue().getColumnKey());
    }

    @Test
    void coverage_moveIssueDnD_wipLimitError_SimpleColumn_CheckMessage() {
        long issueId = 10L;
        String targetKey = "simple-limit";
        String colTitle = "Simple Limit Col";

        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column c = new Column(targetKey, colTitle);
        c.setWipLimit(1);
        b.addColumn(c);

        Issue issueToMove = new Issue(issueId, "Move Me", "origin");
        Issue blocker = new Issue(20L, "Blocker", targetKey);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(issueRepo.findAll()).thenReturn(List.of(blocker, issueToMove));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        String msg = (String) body.get("message");
        assertTrue(msg.contains("Limite atteinte (1)"));
        assertTrue(msg.contains(colTitle));
    }

    @Test
    void coverage_moveIssueDnD_wipLimitError_SubColumn_CheckParentTitle() {
        long issueId = 11L;
        String parentTitle = "Parent Col";

        Board b = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", parentTitle);
        parent.setWipLimit(1);

        Column sub1 = new Column("sub1", "Sub 1");
        Column sub2 = new Column("sub2", "Sub 2");
        parent.addSubColumn(sub1);
        parent.addSubColumn(sub2);
        b.addColumn(parent);

        Issue issueToMove = new Issue(issueId, "Move Me", "origin");
        Issue blocker = new Issue(21L, "Blocker", "sub2");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(issueRepo.findAll()).thenReturn(List.of(blocker, issueToMove));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "sub1");

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));

        String msg = (String) body.get("message");
        assertTrue(msg.contains(parentTitle));
    }

    @Test
    void moveIssueDnD_shouldSetClosedAt_whenMovedToClosed() {
        long issueId = 1L;
        String closedKey = "closed";
        Issue issue = new Issue(issueId, "Task");

        // Setup : colonne closed existe
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(new Column("closed", "Closed"));

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);
        // Simulation que l'issue existe déjà pour éviter d'autres erreurs logiques
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        // Action : Déplacement vers closed
        sut.moveIssueDnD(issueId, closedKey);

        // Vérification
        assertNotNull(issue.getClosedAt(), "La date de fermeture doit être définie");
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldClearClosedAt_whenMovedOutOfClosed() {
        long issueId = 1L;
        Issue issue = new Issue(issueId, "Task");
        issue.setColumnKey("closed");
        issue.setClosedAt(LocalDateTime.now()); // Déjà fermé

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(new Column("todo", "To Do")); // Colonne destination

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.find(issueId)).thenReturn(issue);
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        // Action : Déplacement hors de closed
        sut.moveIssueDnD(issueId, "todo");

        // Vérification
        assertNull(issue.getClosedAt(), "La date de fermeture doit être effacée (null)");
        assertEquals("todo", issue.getColumnKey());
    }

    @Test
    void board_shouldSortClosedIssuesByDateDescending() {
        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        board.addColumn(new Column("closed", "Closed"));

        // Issue 1: Fermée hier (Vieux)
        Issue oldIssue = new Issue(1L, "Old", "closed");
        oldIssue.setClosedAt(LocalDateTime.now().minusDays(1));

        // Issue 2: Fermée maintenant (Récent)
        Issue recentIssue = new Issue(2L, "Recent", "closed");
        recentIssue.setClosedAt(LocalDateTime.now());

        when(boardRepo.findAll()).thenReturn(List.of(board));
        // Note: l'ordre retourné par le repo n'est pas trié
        when(issueRepo.findAll()).thenReturn(List.of(oldIssue, recentIssue));

        ModelAndView mv = sut.board();

        @SuppressWarnings("unchecked")
        Map<String, List<Issue>> issuesByCol = (Map<String, List<Issue>>) mv.getModel().get("issuesByColumn");
        List<Issue> closedList = issuesByCol.get("closed");

        // Vérification du tri
        assertEquals(2, closedList.size());
        assertEquals(recentIssue, closedList.get(0), "La story la plus récente doit être en premier (haut de colonne)");
        assertEquals(oldIssue, closedList.get(1), "La story la plus ancienne doit être en dessous");
    }
}