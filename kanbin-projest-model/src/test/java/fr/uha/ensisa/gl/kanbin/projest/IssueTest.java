package fr.uha.ensisa.gl.kanbin.projest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import org.junit.jupiter.api.Test;

public class IssueTest {

    @Test
    public void createIssue() {
        String title = "Test issue";
        Issue sut = new Issue();
        sut.setTitle(title);

        assertEquals(title, sut.getTitle());
    }

    @Test
    public void anIssueHasAStableId(){
        Issue sut = new Issue(42, "no title");
        long id = sut.getId();

        assertEquals(id, sut.getId());
        assertEquals(id, sut.getId());
        assertEquals(id, sut.getId());
    }

    @Test
    public void anIssueHasStableDetail(){
        String detail = "Ceci est un détail complet de l'issue";
        Issue sut = new Issue(1L, "Test Title", "todo", detail);
        
        assertEquals(detail, sut.getDetail());
        assertEquals(detail, sut.getDetail());
        assertEquals(detail, sut.getDetail());
    }

    @Test
    public void detailCanBeSetAfterCreation() {
        Issue sut = new Issue(1L, "Test Title");
        assertNull(sut.getDetail());
        
        String detail = "Nouveau détail";
        sut.setDetail(detail);
        
        assertEquals(detail, sut.getDetail());
    }

    @Test
    public void detailCanBeModified() {
        String initialDetail = "Détail initial";
        Issue sut = new Issue(1L, "Title", "todo", initialDetail);
        
        assertEquals(initialDetail, sut.getDetail());
        
        String updatedDetail = "Détail modifié";
        sut.setDetail(updatedDetail);
        
        assertEquals(updatedDetail, sut.getDetail());
        assertNotEquals(initialDetail, sut.getDetail());
    }
}