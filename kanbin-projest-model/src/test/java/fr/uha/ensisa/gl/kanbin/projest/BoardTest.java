package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    @Test
    public void addColumn_shouldAddColumnToBoard() {
        Board board = new Board(1L, "Test Board");
        Column col = new Column(10L, "todo", "To Do");

        board.addColumn(col);

        assertEquals(1, board.getColumns().size());
        assertSame(col, board.getColumns().get(0));
    }

    @Test
    public void removeColumn_existingColumn_shouldReturnTrueAndRemoveIt() {
        Board board = new Board(1L, "Test Board");
        Column col1 = new Column(10L, "todo", "To Do");
        Column col2 = new Column(20L, "doing", "Doing");
        board.addColumn(col1);
        board.addColumn(col2);

        boolean removed = board.removeColumn(10L);

        assertTrue(removed);
        assertEquals(1, board.getColumns().size());
        assertSame(col2, board.getColumns().get(0));
    }

    @Test
    public void removeColumn_unknownId_shouldReturnFalseAndKeepColumns() {
        Board board = new Board(1L, "Test Board");
        Column col1 = new Column(10L, "todo", "To Do");
        board.addColumn(col1);

        boolean removed = board.removeColumn(999L);

        assertFalse(removed);
        assertEquals(1, board.getColumns().size());
        assertSame(col1, board.getColumns().get(0));
    }
}
