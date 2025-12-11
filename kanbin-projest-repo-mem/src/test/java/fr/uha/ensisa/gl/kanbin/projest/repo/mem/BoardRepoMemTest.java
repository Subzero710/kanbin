package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class BoardRepoMemTest {

    private BoardRepoMem repo;

    @BeforeEach
    void setUp() {
        repo = new BoardRepoMem();
    }

    @Test
    void seed_shouldCreateDefaultBoardOnlyIfEmpty() {
        repo.seed();
        List<Board> boards = repo.findAll();
        assertEquals(1, boards.size());
        assertEquals("Default", boards.getFirst().getName());
        assertFalse(boards.getFirst().getColumns().isEmpty(), "Le seed doit créer des colonnes par défaut");

        // --- VERIFICATION SUPPLEMENTAIRE ---
        Column backlog = boards.getFirst().getColumns().getFirst();
        assertEquals("backlog", backlog.getKey());
        assertTrue(backlog.isFixed(), "La colonne Backlog générée par le seed doit être fixe");
        // -----------------------------------

        // Deuxième appel ne doit rien faire
        repo.seed();
        assertEquals(1, repo.findAll().size());
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
    void addColumn_shouldThrowExceptionIfBoardNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                repo.addColumn(999L, new Column("test", "Test"))
        );
    }

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
    void deleteById_shouldRemoveBoard() {
        Board b = new Board("To Delete");
        repo.save(b);

        boolean deleted = repo.deleteById(b.getId());

        assertTrue(deleted);
        assertTrue(repo.findById(b.getId()).isEmpty());
    }

    @Test
    void addNonFixedColumn_isInsertedBeforeClosedIfPresent() {
        BoardRepoMem repo = new BoardRepoMem();
        Board board = new Board("Default");

        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        Column closed = new Column("closed", "Closed");
        closed.setFixed(true);

        board.addColumn(backlog);
        board.addColumn(closed);

        repo.save(board);

        Column inProgress = new Column("in-progress", "In progress");
        Column returned = repo.addColumn(board.getId(), inProgress);

        assertSame(inProgress, returned);

        List<Column> cols = repo.findById(board.getId())
                .orElseThrow()
                .getColumns();

        assertEquals(3, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("in-progress", cols.get(1).getKey());
        assertEquals("closed", cols.get(2).getKey());
    }

    @Test
    void addNonFixedColumn_appendsWhenClosedDoesNotExist() {
        BoardRepoMem repo = new BoardRepoMem();
        Board board = new Board("Default");

        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        board.addColumn(backlog);

        repo.save(board);

        Column todo = new Column("todo", "Todo");
        repo.addColumn(board.getId(), todo);

        List<Column> cols = repo.findById(board.getId())
                .orElseThrow()
                .getColumns();

        assertEquals(2, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("todo", cols.get(1).getKey());
    }

    @Test
    void addFixedColumn_keepsSimpleAppendBehaviour() {
        BoardRepoMem repo = new BoardRepoMem();
        Board board = new Board("Default");

        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        board.addColumn(backlog);

        repo.save(board);

        Column closed = new Column("closed", "Closed");
        closed.setFixed(true);

        repo.addColumn(board.getId(), closed);

        List<Column> cols = repo.findById(board.getId())
                .orElseThrow()
                .getColumns();
        assertEquals(2, cols.size());
        assertEquals("backlog", cols.get(0).getKey());
        assertEquals("closed", cols.get(1).getKey());
    }

    @Test
    public void testFindAll() {
        BoardRepoMem repo = new BoardRepoMem();
        repo.save(new Board("Board 1"));
        repo.save(new Board("Board 2"));

        Collection<Board> allBoards = repo.findAll();
        assertTrue(allBoards.size() >= 2);
    }
}