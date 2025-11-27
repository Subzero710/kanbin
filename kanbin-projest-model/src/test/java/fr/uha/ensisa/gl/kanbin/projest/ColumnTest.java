package fr.uha.ensisa.gl.kanbin.projest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        // Column n'override pas equals(), donc deux instances distinctes ne sont pas égales
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
        // Sans equals/hashCode personnalisés, les deux instances sont considérées différentes
        assertEquals(2, set.size());
    }

    @Test
    public void notEqualsNullOrOtherType() {
        Column c = new Column(1L, "key1", "ToDo");
        assertNotEquals(c, null);
        assertNotEquals(c, "some string");
    }
}