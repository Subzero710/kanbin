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
        assertTrue(c.equals(c));
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
        // Arrange : Une colonne simple (pas de sous-colonnes)
        Column simpleColumn = new Column("simple", "Simple Column");

        // Act
        List<Column> result = simpleColumn.getSubColumnsOrSelf();

        // Assert
        assertEquals(1, result.size(), "Doit retourner une liste de taille 1");
        assertEquals(simpleColumn, result.get(0), "La liste doit contenir la colonne elle-même");
    }

    @Test
    void getSubColumnsOrSelf_shouldReturnSubColumns_whenSubColumnsExist() {
        // Une colonne parent avec 2 sous-colonnes
        Column parentColumn = new Column("parent", "Parent Column");
        Column sub1 = new Column("sub1", "Sub 1");
        Column sub2 = new Column("sub2", "Sub 2");

        parentColumn.addSubColumn(sub1);
        parentColumn.addSubColumn(sub2);
        List<Column> result = parentColumn.getSubColumnsOrSelf();

        assertEquals(2, result.size(), "Doit retourner les 2 sous-colonnes");
        assertTrue(result.contains(sub1));
        assertTrue(result.contains(sub2));
        assertFalse(result.contains(parentColumn), "Ne doit pas contenir le parent");
    }
}