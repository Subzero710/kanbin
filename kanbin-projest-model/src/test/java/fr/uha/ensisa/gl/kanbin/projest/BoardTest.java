package fr.uha.ensisa.gl.kanbin.projest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;


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
        Column c = mock(Column.class);
        Board b = new Board();
        Column returned = b.addColumn(c);
        assertSame(c, returned);
        assertEquals(1, b.getColumns().size());
        assertSame(c, b.getColumns().get(0));
    }

    @Test
    public void removeColumn_byId_successAndFailure() {
        Column c1 = mock(Column.class);
        Column c2 = mock(Column.class);
        when(c1.getId()).thenReturn(1L);
        when(c2.getId()).thenReturn(2L);

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
        Column c = mock(Column.class);
        b.getColumns().add(c);
        assertEquals(1, b.getColumns().size());
        assertSame(c, b.getColumns().get(0));

        // verify that clearing the returned list affects the board
        b.getColumns().clear();
        assertTrue(b.getColumns().isEmpty());
    }
}


