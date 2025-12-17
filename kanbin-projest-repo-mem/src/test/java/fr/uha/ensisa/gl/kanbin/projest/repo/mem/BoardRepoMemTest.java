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

    @Test
    void seed_shouldCreateDefaultBoardOnlyIfEmpty() {
        repo.seed();
        List<Board> boards = repo.findAll();
        assertEquals(1, boards.size());

        Board b = boards.getFirst();
        assertEquals("Default", b.getName());
        assertFalse(b.getColumns().isEmpty());

        Column backlog = b.getColumns().getFirst();
        assertEquals("backlog", backlog.getKey());
        assertTrue(backlog.isFixed());

        // Tue mutant 261
        Column closed = b.getColumns().stream()
                .filter(c -> "closed".equals(c.getKey()))
                .findFirst().orElseThrow();
        assertTrue(closed.isFixed());

        repo.seed();
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void save_shouldReturnTheSavedInstance() {
        Board b = new Board("Test Return");
        // Tue mutant 461
        Board saved = repo.save(b);
        assertNotNull(saved);
        assertEquals(b.getName(), saved.getName());
    }

    @Test
    void save_shouldGenerateIdForNewBoard() {
        Board b = new Board("New");
        repo.save(b);
        assertTrue(b.getId() > 0);
    }

    @Test
    void save_shouldGenerateIdsForNewColumns() {
        Board b = new Board("Cols");
        Column c = new Column(0, "c", "C");
        b.addColumn(c);
        repo.save(b);
        assertTrue(c.getId() > 0);
    }

    @Test
    void addColumn_shouldAddToExistingBoardAndGenerateId() {
        Board b = new Board("B");
        repo.save(b);

        Column c = new Column(0, "dev", "Dev");
        // L'ID sera généré par le save() interne à addColumn
        repo.addColumn(b.getId(), c);

        assertTrue(c.getId() > 0);
        assertEquals(1, repo.findById(b.getId()).get().getColumns().size());
    }

    @Test
    void addColumn_shouldReturnTheAddedColumn() {
        Board b = new Board("Ret");
        repo.save(b);
        Column c = new Column("k", "K");
        // Tue mutant 931
        Column ret = repo.addColumn(b.getId(), c);
        assertNotNull(ret);
        assertEquals("k", ret.getKey());
    }

    @Test
    void addNonFixedColumn_shouldInsertAtStart_whenClosedIsFirstElement() {
        // Tue mutant 811
        Board b = new Board("Spec");
        Column cl = new Column("closed", "Closed");
        cl.setFixed(true);
        b.addColumn(cl); // Index 0
        repo.save(b);

        Column todo = new Column("todo", "Todo");
        repo.addColumn(b.getId(), todo);

        List<Column> cols = repo.findById(b.getId()).get().getColumns();
        assertEquals("todo", cols.get(0).getKey());
        assertEquals("closed", cols.get(1).getKey());
    }

    @Test
    void addNonFixedColumn_isInsertedBeforeClosedIfPresent() {
        Board b = new Board("Def");
        Column bl = new Column("backlog", "BL"); bl.setFixed(true);
        Column cl = new Column("closed", "CL"); cl.setFixed(true);
        b.addColumn(bl); b.addColumn(cl);
        repo.save(b);

        Column wip = new Column("wip", "WIP");
        repo.addColumn(b.getId(), wip);

        List<Column> cols = repo.findById(b.getId()).get().getColumns();
        assertEquals("wip", cols.get(1).getKey());
        assertEquals("closed", cols.get(2).getKey());
    }

    @Test
    void addNonFixedColumn_appendsWhenClosedDoesNotExist() {
        Board b = new Board("NoClosed");
        Column bl = new Column("backlog", "BL"); bl.setFixed(true);
        b.addColumn(bl);
        repo.save(b);

        Column todo = new Column("todo", "Todo");
        repo.addColumn(b.getId(), todo);

        assertEquals("todo", repo.findById(b.getId()).get().getColumns().get(1).getKey());
    }

    @Test
    void addFixedColumn_keepsSimpleAppendBehaviour() {
        Board b = new Board("Fix");
        Column bl = new Column("backlog", "BL"); bl.setFixed(true);
        b.addColumn(bl);
        repo.save(b);

        Column cl = new Column("closed", "CL"); cl.setFixed(true);
        repo.addColumn(b.getId(), cl);

        assertEquals("closed", repo.findById(b.getId()).get().getColumns().get(1).getKey());
    }

    @Test
    void addColumn_shouldThrowExceptionIfBoardNotFound() {
        assertThrows(NoSuchElementException.class, () -> repo.addColumn(999, new Column("t", "T")));
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
        Board b = new Board("Test");
        repo.save(b);
        Column c = new Column(123L, "key", "Title");
        repo.addColumn(b.getId(), c);
        assertEquals(123L, c.getId());
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
        assertTrue(repo.findById(b.getId()).get().getColumns().isEmpty());
    }

    @Test
    void removeColumn_shouldReturnFalse_whenBoardNotFound() {
        assertFalse(repo.removeColumn(999L, 1L));
    }

    @Test
    void removeColumn_shouldReturnFalse_whenColumnNotFound() {
        Board b = new Board("B");
        repo.save(b);
        assertFalse(repo.removeColumn(b.getId(), 555L));
    }

    @Test
    void deleteById_shouldRemoveBoard() {
        Board b = new Board("Del");
        repo.save(b);
        assertTrue(repo.deleteById(b.getId()));
        assertTrue(repo.findById(b.getId()).isEmpty());
    }

    @Test
    void deleteById_shouldReturnFalse_whenBoardDoesNotExist() {
        assertFalse(repo.deleteById(999L));
    }

    @Test
    void findAll_shouldReturnAllBoards() {
        repo.save(new Board("B1"));
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void findById_shouldReturnEmpty() {
        assertTrue(repo.findById(99).isEmpty());
    }

    @Test
    void save_shouldUpdateExistingColumn() {
        Board b = new Board("Test");
        Column c = new Column(0, "c", "T");
        b.addColumn(c);
        repo.save(b);
        c.setTitle("Mod");
        repo.save(b);
        assertEquals("Mod", repo.findById(b.getId()).get().getColumns().get(0).getTitle());
    }

    @Test
    void save_shouldUpdateExistingReference() {
        Board b = new Board("Original");
        repo.save(b);
        b.setName("Modified");
        assertEquals("Modified", repo.findById(b.getId()).get().getName());
    }
}