package fr.uha.ensisa.gl.kanbin.projest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;


public class BoardTest {

    @Test
    public void defaultConstructor_initialState() {
        Board b = new Board();
        assertEquals(0L, b.getId());
        assertNull(b.getName());
        assertNotNull(b.getColumns());
        assertTrue(b.getColumns().isEmpty());
    }

    @Test
    public void parameterizedConstructors_andAccessors() {
        Board b = new Board(5L, "MyBoard");
        assertEquals(5L, b.getId());
        assertEquals("MyBoard", b.getName());

        b.setId(7L);
        b.setName("Renamed");
        assertEquals(7L, b.getId());
        assertEquals("Renamed", b.getName());

        Board b2 = new Board("OnlyName");
        assertEquals(0L, b2.getId());
        assertEquals("OnlyName", b2.getName());
    }

    @Test
    public void addColumn_returnsSameAndStored() {
        Column c = new Column(1L, "key1", "ToDo");
        Board b = new Board();
        Column returned = b.addColumn(c);
        assertSame(c, returned);
        assertEquals(1, b.getColumns().size());
        assertSame(c, b.getColumns().get(0));
    }

    @Test
    public void removeColumn_byId_successAndFailure() {
        Column c1 = new Column(1L, "key1", "ToDo");
        Column c2 = new Column(2L, "key2", "Done");

        Board b = new Board();
        b.addColumn(c1);
        b.addColumn(c2);

        boolean removed = b.removeColumn(1L);
        assertTrue(removed);
        assertEquals(1, b.getColumns().size());
        assertSame(c2, b.getColumns().get(0));

        boolean removedNonExisting = b.removeColumn(42L);
        assertFalse(removedNonExisting);
        assertEquals(1, b.getColumns().size());
    }

    @Test
    public void getColumns_returnsMutableList() {
        Board b = new Board();
        Column c = new Column(1L, "key1", "ToDo");
        b.getColumns().add(c);
        assertEquals(1, b.getColumns().size());
        assertSame(c, b.getColumns().get(0));

        // verify that clearing the returned list affects the board
        b.getColumns().clear();
        assertTrue(b.getColumns().isEmpty());
    }

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
    @Test
    public void removeRowByKey_shouldRemoveMatchingRowAndReturnTrue() {
        Board board = new Board("Test Board");
        Row r1 = new Row("r1", "Row 1");
        Row r2 = new Row("r2", "Row 2");
        board.addRow(r1);
        board.addRow(r2);

        boolean removed = board.removeRowByKey("r1");

        assertTrue(removed);
        assertEquals(1, board.getRows().size());
        assertSame(r2, board.getRows().get(0));
    }

    @Test
    public void removeRowByKey_nullKey_shouldRemoveNothingAndReturnFalse() {
        Board board = new Board("Test Board");
        Row r1 = new Row("r1", "Row 1");
        board.addRow(r1);

        boolean removed = board.removeRowByKey(null);

        assertFalse(removed);
        assertEquals(1, board.getRows().size());
        assertSame(r1, board.getRows().get(0));
    }

}