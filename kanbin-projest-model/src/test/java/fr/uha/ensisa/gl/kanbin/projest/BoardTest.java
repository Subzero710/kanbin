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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        b.getColumns().clear();
        assertTrue(b.getColumns().isEmpty());
    }
}