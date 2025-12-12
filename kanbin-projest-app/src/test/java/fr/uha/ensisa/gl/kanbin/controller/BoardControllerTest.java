package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.BoardRepoMem;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;

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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

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
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        sut.addColumn(newColumnTitle, "simple", redirectAttributes);
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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        sut.addColumn("Ma Colonne", "simple", redirectAttributes);
        verify(boardRepo).save(any(Board.class));
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), any(Column.class));
    }

    @Test
    void addColumn_duplicateName_shouldNotCallRepoAndSetErrorMessage() {
        // Board existant avec une colonne "Dev"
        Column existing = new Column("dev", "Dev");
        testBoard.addColumn(existing);
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.addColumn("Dev", "simple", redirectAttributes);

        assertEquals("redirect:/board", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
        verify(boardRepo, never()).addColumn(anyLong(), any(Column.class));
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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.updateColumn(colId, newTitle, null, redirectAttributes);
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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = sut.updateColumn(fixedColId, "Hacked Title", null, redirectAttributes);
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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        sut.addColumn("ColonnePrincipale", "double", redirectAttributes);

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
        Issue existingIssue = new Issue(2L, "Blocker", targetKey); // Déjà dedans

        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        // Simulation : Il y a déjà 1 élément
        when(issueRepo.findAll()).thenReturn(List.of(existingIssue, issueToMove));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, targetKey);

        assertEquals(200, response.getStatusCodeValue()); // HTTP OK
        assertFalse((Boolean) response.getBody().get("success")); // Mais Logique KO
        assertTrue(((String)response.getBody().get("message")).contains("Limite atteinte"));

        // Vérif : Pas de changement
        assertEquals("old-col", issueToMove.getColumnKey());
        verify(issueRepo, never()).persist(issueToMove);
    }

    @Test
    void moveIssueDnD_shouldFail_whenWipLimitReached_inParentColumn() {
        // Cas complexe : Limite sur le PARENT, déplacement vers l'ENFANT
        // Couvre findMainColumn et resolveWipLimit
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
        Issue existingInSub2 = new Issue(2L, "Blocker", "sub-2"); // Une issue est déjà dans Sub2

        when(issueRepo.find(issueId)).thenReturn(issueToMove);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of(existingInSub2, issueToMove));

        // On essaie de déplacer vers Sub1 (qui est vide), MAIS le parent est plein à cause de Sub2
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "sub-1");

        assertFalse((Boolean) response.getBody().get("success"), "Devrait échouer car la limite du parent est atteinte");
        assertTrue(((String)response.getBody().get("message")).contains("Parent"));
    }

    @Test
    void moveIssueDnD_shouldAllowMove_ifAlreadyInLogicalColumn() {
        // Cas : Déplacement intra-colonne (ex: de Todo à Wip dans le même parent)
        // La limite ne doit pas bloquer car on ne rajoute pas +1 au total du parent
        long issueId = 1L;

        Board board = new Board(TEST_BOARD_ID, BOARD_NAME);
        Column parent = new Column("parent", "Parent");
        parent.setWipLimit(1); // Limite stricte à 1

        Column subTodo = new Column("todo", "Todo");
        Column subWip = new Column("wip", "Wip");
        parent.addSubColumn(subTodo);
        parent.addSubColumn(subWip);
        board.addColumn(parent);

        Issue issue = new Issue(issueId, "Mover", "todo"); // Déjà dans le parent (via Todo)

        when(issueRepo.find(issueId)).thenReturn(issue);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of(issue)); // Seule issue présente

        // Action : Déplacer de Todo vers Wip
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "wip");

        assertTrue((Boolean) response.getBody().get("success"), "Devrait réussir car l'issue est déjà comptée dans la limite du parent");
        assertEquals("wip", issue.getColumnKey());
    }
}