package fr.uha.ensisa.gl.kanbin.projest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
}