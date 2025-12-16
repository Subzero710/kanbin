package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RowTest {

    @Test
    void testRowCoverage() {

        Row row = new Row();

        row.setId(10L);
        assertEquals(10L, row.getId());

        row.setKey("backlog");
        assertEquals("backlog", row.getKey());

        row.setTitle("Backlog");
        assertEquals("Backlog", row.getTitle());

        row.setPos(1);
        assertEquals(1, row.getPos());

        row.setFixed(true);
        assertTrue(row.isFixed());
        Row subRow = new Row("sub", "Sub Row");


        assertEquals(1, row.getSubRowsOrSelf().size()); // Doit retourner lui-même

        row.addSubRow(subRow);
        assertFalse(row.getSubRows().isEmpty());
        assertEquals(subRow, row.getSubRows().get(0));
        assertEquals(1, row.getSubRowsOrSelf().size()); // Retourne la liste des enfants
    }
}
