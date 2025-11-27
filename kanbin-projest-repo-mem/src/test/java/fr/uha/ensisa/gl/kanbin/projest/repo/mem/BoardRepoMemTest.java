package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertEquals("Default", boards.get(0).getName());
        assertFalse(boards.get(0).getColumns().isEmpty(), "Le seed doit créer des colonnes par défaut");

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

        repo.save(b); // Sauvegarde globale

        assertTrue(c1.getId() > 0, "L'ID de la colonne doit être généré lors du save du board");
    }

    @Test
    void addColumn_shouldAddToExistingBoardAndGenerateId() {
        // Setup board
        Board b = new Board("My Board");
        repo.save(b);
        long boardId = b.getId();

        // Action
        Column newCol = new Column(0, "dev", "Dev");
        repo.addColumn(boardId, newCol);

        // Verify
        Board reloaded = repo.findById(boardId).get();
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
        Board reloaded = repo.findById(b.getId()).get();
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
}