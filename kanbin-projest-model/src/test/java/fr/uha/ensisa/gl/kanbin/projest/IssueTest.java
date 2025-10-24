package fr.uha.ensisa.gl.kanbin.projest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class IssueTest {

    @Test
    public void createIssue() {
        String title = "Test issue";
        Issue sut = new Issue("Test issue");
        assertEquals(title, sut.getTitle());
    }

    @Test
    public void twoTestsShouldHaveDifferentsIDs(){
        String t1 = "first title", t2 = "second title";
        Issue sut1 = new Issue(t1), sut2 = new Issue(t2);
        assertEquals(t1, sut1.getTitle());
        assertEquals(t2, sut2.getTitle());
        assertNotEquals(sut1.getId(), sut2.getId());
    }

    @Test
    public void anIssueHasAStableId(){
        Issue sut = new Issue("no title");
        long id = sut.getId();
        assertEquals(id, sut.getId());
        assertEquals(id, sut.getId());
        assertEquals(id, sut.getId());
    }
}
