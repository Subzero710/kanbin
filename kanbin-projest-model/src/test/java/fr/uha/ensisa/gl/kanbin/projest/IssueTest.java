package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

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
        String title = "no title";
        Issue sut = new Issue(42, title);
        long id = sut.getId();
        assertEquals(id, sut.getId());
        assertEquals(title, sut.getTitle(), "Le constructeur doit définir le titre via setTitle");
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

    @Test
    void testIssueConstructorWithColumnKey() {
        Issue issue = new Issue(123L, "Ma Tache", "todo_column");

        assertEquals(123L, issue.getId());
        assertEquals("Ma Tache", issue.getTitle());
    }

    @Test
    void testIdSetter() {
        Issue issue = new Issue();
        issue.setId(500L);
        assertEquals(500L, issue.getId());
    }

    @Test
    void testRowKeyManagement() {
        Issue issue = new Issue();
        // Tue le mutant NO_COVERAGE sur getRowKey
        assertNull(issue.getRowKey());

        issue.setRowKey("row-123");
        assertEquals("row-123", issue.getRowKey());
    }

    @Test
    public void titleBoundaryTests() {
        Issue sut = new Issue();
        String thirtyChars = "1234567890" + "1234567890" + "1234567890";
        sut.setTitle(thirtyChars);
        assertEquals(30, sut.getTitle().length());
        assertEquals(thirtyChars, sut.getTitle());
        assertSame(thirtyChars, sut.getTitle(), "À 30, on ne doit pas avoir fait de substring");
        String thirtyOneChars = thirtyChars + "A";
        sut.setTitle(thirtyOneChars);
        assertEquals(30, sut.getTitle().length());
        assertEquals(thirtyChars, sut.getTitle());
        assertNotSame(thirtyOneChars, sut.getTitle(), "À 31, un nouvel objet String doit être créé");
    }


}