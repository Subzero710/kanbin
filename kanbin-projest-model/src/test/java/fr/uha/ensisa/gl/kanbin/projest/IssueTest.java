package fr.uha.ensisa.gl.kanbin.projest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

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

    // TEST POUR LA LIMITE DE 30 CARACTÈRES
    @Test
    public void titleShouldBeTruncatedTo30Chars() {
        String longTitle = "Ceci est un titre vraiment très très long qui dépasse 30";

        String expected = "Ceci est un titre vraiment trè";

        Issue sut = new Issue();
        sut.setTitle(longTitle);

        assertEquals(30, sut.getTitle().length());
        assertEquals(expected, sut.getTitle());
    }

    @Test
    void setTitle_shouldHandleNull_gracefully() {
        Issue issue = new Issue();
        issue.setTitle(null);
        assertNull(issue.getTitle());
    }

    @Test
    void setTitle_shouldHandleEmptyString() {
        Issue issue = new Issue();
        issue.setTitle("");
        assertEquals("", issue.getTitle());
    }

    @Test
    void setDetail_shouldAcceptNull() {
        Issue issue = new Issue();
        issue.setDetail("Some detail");

        issue.setDetail(null);
        assertNull(issue.getDetail());
    }

    @Test
    void setColumnKey_shouldAcceptNull() {
        Issue issue = new Issue();
        issue.setColumnKey("todo");

        issue.setColumnKey(null);
        assertNull(issue.getColumnKey());
    }

    @Test
    void constructor_full_shouldInitializeAllFields() {
        Issue issue = new Issue(10L, "Title", "Key", "Detail");
        assertEquals(10L, issue.getId());
        assertEquals("Title", issue.getTitle());
        assertEquals("Key", issue.getColumnKey());
        assertEquals("Detail", issue.getDetail());
    }

    @Test
    void setTitle_shouldAcceptNull() {
        Issue i = new Issue();
        i.setTitle(null);
        assertNull(i.getTitle());
    }

    @Test
    public void testClosedAtField() {
        Issue issue = new Issue();
        assertNull(issue.getClosedAt(), "La date doit être nulle par défaut");

        LocalDateTime now = LocalDateTime.now();
        issue.setClosedAt(now);

        assertEquals(now, issue.getClosedAt(), "Le getter doit retourner la date définie");
    }
}