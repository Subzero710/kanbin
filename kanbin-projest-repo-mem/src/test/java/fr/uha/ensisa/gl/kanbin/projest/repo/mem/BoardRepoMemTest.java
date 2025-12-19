package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BoardRepoMemTest {

    private BoardRepoMem repo;

    @BeforeEach
    void setUp() {
        repo = new BoardRepoMem();
    }

    // --- INITIALISATION (SEED) ---

    @Test
    void seed_shouldCreateDefaultBoardOnlyIfEmpty() {
        repo.seed();
        List<Board> boards = repo.findAll();
        assertEquals(1, boards.size());

        Board b = boards.getFirst();
        assertEquals("Default", b.getName());
        assertFalse(b.getColumns().isEmpty(), "Le seed doit créer des colonnes par défaut");

        // Vérification Backlog (système)
        Column backlog = b.getColumns().getFirst();
        assertEquals("backlog", backlog.getKey());
        assertTrue(backlog.isFixed(), "La colonne Backlog doit être fixe");

        // Tue le mutant 261 (Vérification Closed Fixed)
        Column closed = b.getColumns().stream()
                .filter(c -> "closed".equals(c.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La colonne Closed devrait exister"));
        assertTrue(closed.isFixed(), "La colonne Closed doit être fixe");

        // Vérification idempotence
        repo.seed();
        assertEquals(1, repo.findAll().size());
    }

    // --- SAVE & RETOURS ---

    @Test
    void save_shouldReturnTheSavedInstance() {
        Board b = new Board("Test Return");
        // Tue le mutant 461
        Board saved = repo.save(b);
        assertNotNull(saved, "La méthode save doit retourner l'objet sauvegardé, pas null");
        assertEquals(b.getName(), saved.getName());
    }

    @Test
    void save_shouldGenerateIdForNewBoard() {
        Board b = new Board("New Board");
        assertEquals(0, b.getId());

        repo.save(b);

        assertTrue(b.getId() > 0, "L'ID du board doit être généré");
        Optional<Board> retrieved = repo.findById(b.getId());
        assertTrue(retrieved.isPresent());
    }

    @Test
    void save_shouldGenerateIdsForNewColumns() {
        Board b = new Board("Board with cols");
        Column c1 = new Column(0, "col1", "Col 1");
        b.addColumn(c1);

        repo.save(b);

        assertTrue(c1.getId() > 0, "L'ID de la colonne doit être généré lors du save du board");
    }

    @Test
    void save_shouldUpdateExistingColumn_withoutChangingId() {
        Board b = new Board("Test Board");
        Column c = new Column(0, "col", "Title");
        b.addColumn(c);
        repo.save(b);
        long id = c.getId();

        c.setTitle("New Title");
        repo.save(b);

        assertEquals(id, c.getId());
        assertEquals("New Title", repo.findById(b.getId())
                .orElseThrow()
                .getColumns().getFirst().getTitle());
    }

    @Test
    void save_shouldUpdateExistingReference() {
        Board b = new Board("Original");
        repo.save(b);
        b.setName("Modified");

        Board retrieved = repo.findById(b.getId()).orElseThrow();
        assertEquals("Modified", retrieved.getName());
    }

    @Test
    void save_existingColumn_shouldNotChangeId() {
        Board b = new Board("Test");
        Column c = new Column(0, "col", "Title");
        repo.save(b);
        long originalId = c.getId();

        c.setTitle("New Title");
        repo.save(b);

        assertEquals(originalId, c.getId());
    }

    @Test
    void save_shouldNotChangeId_whenUpdatingExistingColumn() {
        Board b = new Board("Test");
        Column c = new Column(0, "col", "Title");
        b.addColumn(c);
        repo.save(b);
        long originalId = c.getId();

        c.setTitle("Updated");
        repo.save(b);

        assertEquals(originalId, c.getId());
    }

    // --- AJOUT DE COLONNES (LOGIQUE D'ORDRE & RETOUR) ---

    @Test
    void addColumn_shouldAddToExistingBoardAndGenerateId() {
        Board b = new Board("My Board");
        repo.save(b);
        long boardId = b.getId();

        Column newCol = new Column(0, "dev", "Dev");
        repo.addColumn(boardId, newCol);

        Board reloaded = repo.findById(boardId).orElseThrow();
        assertEquals(1, reloaded.getColumns().size());
        assertTrue(newCol.getId() > 0, "L'ID de la nouvelle colonne doit être généré");
    }

    @Test
    void addColumn_shouldReturnTheAddedColumn_andNotNull() {
        Board b = new Board("Test Return");
        repo.save(b);
        Column c = new Column("new", "New");

        // Tue le mutant 931
        Column returned = repo.addColumn(b.getId(), c);

        assertNotNull(returned, "addColumn ne doit pas retourner null");
        assertEquals(c, returned);
        assertTrue(returned.getId() > 0);
    }

    @Test
    void addNonFixedColumn_isInsertedBeforeClosedIfPresent() {
        Board b = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        Column closed = new Column("closed", "Closed");
        closed.setFixed(true);

        b.addColumn(backlog);
        b.addColumn(closed);
        repo.save(b);

        Column inProgress = new Column("in-progress", "In progress");
        repo.addColumn(b.getId(), inProgress);

        List<Column> cols = repo.findById(b.getId()).orElseThrow().getColumns();

        // [Backlog, InProgress, Closed]
        assertEquals(3, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("in-progress", cols.get(1).getKey());
        assertEquals("closed", cols.get(2).getKey());
    }

    @Test
    void addNonFixedColumn_shouldInsertAtStart_whenClosedIsFirstElement() {
        // Tue le mutant 811
        Board b = new Board("Special Board");
        Column closed = new Column("closed", "Closed");
        closed.setFixed(true);
        b.addColumn(closed); // Index 0
        repo.save(b);

        Column todo = new Column("todo", "To Do");
        repo.addColumn(b.getId(), todo);

        List<Column> cols = repo.findById(b.getId()).orElseThrow().getColumns();

        assertEquals("todo", cols.getFirst().getKey(), "La colonne doit être insérée avant Closed (index 0)");
        assertEquals("closed", cols.get(1).getKey());
    }

    @Test
    void addNonFixedColumn_appendsWhenClosedDoesNotExist() {
        Board b = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        b.addColumn(backlog);
        repo.save(b);

        Column todo = new Column("todo", "Todo");
        repo.addColumn(b.getId(), todo);

        List<Column> cols = repo.findById(b.getId()).orElseThrow().getColumns();
        assertEquals(2, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("todo", cols.get(1).getKey());
    }

    @Test
    void addFixedColumn_keepsSimpleAppendBehaviour() {
        Board b = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        b.addColumn(backlog);
        repo.save(b);

        Column closed = new Column("closed", "Closed");
        closed.setFixed(true);
        repo.addColumn(b.getId(), closed);

        List<Column> cols = repo.findById(b.getId()).orElseThrow().getColumns();
        assertEquals(2, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("closed", cols.get(1).getKey());
    }

    @Test
    void addColumn_shouldThrowExceptionIfBoardNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                repo.addColumn(999L, new Column("test", "Test"))
        );
    }

    @Test
    void addColumn_shouldThrowException_whenColumnIsNull() {
        Board b = new Board("Test");
        repo.save(b);
        long id = b.getId();
        assertThrows(RuntimeException.class, () -> repo.addColumn(id, null));
    }

    @Test
    void addColumn_shouldAddExistingColumn_withoutChangingId() {
        Board b = new Board("Test Board");
        repo.save(b);

        Column c = new Column(123L, "key", "Title");
        repo.addColumn(b.getId(), c);

        assertEquals(123L, c.getId());
    }

    // --- SUPPRESSION & FINDERS ---

    @Test
    void removeColumn_shouldRemoveColumnAndReturnTrue() {
        Board b = new Board("My Board");
        Column c = new Column(0, "del", "Delete Me");
        b.addColumn(c);
        repo.save(b);
        long colId = c.getId();

        boolean result = repo.removeColumn(b.getId(), colId);

        assertTrue(result);
        Board reloaded = repo.findById(b.getId()).orElseThrow();
        assertTrue(reloaded.getColumns().isEmpty());
    }

    @Test
    void removeColumn_shouldReturnFalse_whenBoardDoesNotExist() {
        boolean result = repo.removeColumn(9999L, 1L);
        assertFalse(result, "Doit retourner false si le board n'existe pas");
    }

    @Test
    void removeColumn_shouldReturnFalse_whenColumnDoesNotExistInBoard() {
        Board b = new Board("Test");
        repo.save(b);

        boolean result = repo.removeColumn(b.getId(), 555L);
        assertFalse(result, "Doit retourner false si la colonne n'est pas trouvée");
    }

    @Test
    void deleteById_shouldRemoveBoard() {
        Board b = new Board("To Delete");
        repo.save(b);

        boolean deleted = repo.deleteById(b.getId());

        assertTrue(deleted);
        assertTrue(repo.findById(b.getId()).isEmpty());
    }

    @Test
    void deleteById_shouldReturnFalse_whenBoardDoesNotExist() {
        boolean result = repo.deleteById(9999L);
        assertFalse(result, "Doit retourner false si l'ID n'existe pas");
    }

    @Test
    public void testFindAll() {
        repo.save(new Board("Board 1"));
        repo.save(new Board("Board 2"));

        Collection<Board> allBoards = repo.findAll();
        assertTrue(allBoards.size() >= 2);
    }
}