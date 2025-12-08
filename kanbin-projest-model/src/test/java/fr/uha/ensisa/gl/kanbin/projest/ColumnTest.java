package fr.uha.ensisa.gl.kanbin.projest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*; // Import statique complet pour assertTrue/False
import org.junit.jupiter.api.Test;

import fr.uha.ensisa.gl.kanbin.projest.model.Column;

public class ColumnTest {

    @Test
    public void equalsSameInstance() {
        Column c = new Column("key1", "ToDo");
        assertEquals(c, c);
    }

    @Test
    public void differentInstancesSamePropertiesAreNotEqualByDefault() {
        Column c1 = new Column(1L, "key1", "ToDo");
        Column c2 = new Column(1L, "key1", "ToDo");
        assertNotEquals(c1, c2);
    }

    @Test
    public void notEqualsDifferentId() {
        Column c1 = new Column(1L, "key1", "ToDo");
        Column c2 = new Column(2L, "key1", "ToDo");
        assertNotEquals(c1, c2);
    }

    @Test
    public void notEqualsDifferentTitle() {
        Column c1 = new Column(1L, "key1", "ToDo");
        Column c2 = new Column(1L, "key1", "Done");
        assertNotEquals(c1, c2);
    }

    @Test
    public void hashSetKeepsBothWhenNoEqualsOverride() {
        Column c1 = new Column(1L, "key1", "ToDo");
        Column c2 = new Column(1L, "key1", "ToDo");
        Set<Column> set = new HashSet<>();
        set.add(c1);
        set.add(c2);
        assertEquals(2, set.size());
    }

    @Test
    public void notEqualsNullOrOtherType() {
        Column c = new Column(1L, "key1", "ToDo");
        assertNotEquals(c, null);
        assertNotEquals(c, "some string");
    }

    @Test
    public void testGetterAndSetterAndEmptyConstructor() {
        Column c = new Column();
        c.setId(55L);
        c.setKey("backlog");
        c.setTitle("Backlog");
        c.setPos(1);

        assertEquals(55L, c.getId());
        assertEquals("backlog", c.getKey());
        assertEquals("Backlog", c.getTitle());
        assertEquals(1, c.getPos());
    }

    @Test
    public void testSubColumnsManagement() {
        Column parent = new Column("parent", "Parent");
        assertEquals(0, parent.getSubColumns().size());

        Column child = new Column("child", "Enfant");
        parent.addSubColumn(child);

        assertEquals(1, parent.getSubColumns().size());
        assertEquals("child", parent.getSubColumns().get(0).getKey());
    }

    // --- NOUVEAUX TESTS POUR LA COUVERTURE ---
    @Test
    public void testFixedPropertyDefault() {
        Column c = new Column("test", "Test");
        // Par défaut, une colonne ne doit pas être fixe
        assertFalse(c.isFixed());
    }

    @Test
    public void testSetFixed() {
        Column c = new Column("test", "Test");
        c.setFixed(true);
        assertTrue(c.isFixed());

        c.setFixed(false);
        assertFalse(c.isFixed());
    }
}