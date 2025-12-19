package fr.uha.ensisa.gl.kanbin.projest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
        assertNotNull(c);
        assertNotEquals("some string", c);
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
        assertEquals("child", parent.getSubColumns().getFirst().getKey());
    }

    @Test
    public void testFixedPropertyDefault() {
        Column c = new Column("test", "Test");
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

    @Test
    void getSubColumnsOrSelf_shouldReturnSelf_whenNoSubColumns() {
        Column simpleColumn = new Column("simple", "Simple Column");
        List<Column> result = simpleColumn.getSubColumnsOrSelf();
        assertEquals(1, result.size());
        assertEquals(simpleColumn, result.getFirst());
    }

    @Test
    void getSubColumnsOrSelf_shouldReturnSubColumns_whenSubColumnsExist() {
        Column parentColumn = new Column("parent", "Parent Column");
        Column sub1 = new Column("sub1", "Sub 1");
        Column sub2 = new Column("sub2", "Sub 2");
        parentColumn.addSubColumn(sub1);
        parentColumn.addSubColumn(sub2);

        List<Column> result = parentColumn.getSubColumnsOrSelf();
        assertEquals(2, result.size());
        assertTrue(result.contains(sub1));
        assertTrue(result.contains(sub2));
        assertFalse(result.contains(parentColumn), "Ne doit pas contenir le parent");

    }
    @Test
    void testColumnType() {
        Column column = new Column("todo", "A Faire");

        column.setType("swimlane");
        assertEquals("swimlane", column.getType());
    }
    @Test
    public void testWipLimitManagement() {
        Column c = new Column();
        assertEquals(0, c.getWipLimit(), "La limite WIP par défaut doit être 0");
        c.setWipLimit(5);
        assertEquals(5, c.getWipLimit(), "Le getter doit retourner la valeur définie (5)");
        c.setWipLimit(10);
        assertEquals(10, c.getWipLimit());
    }

}