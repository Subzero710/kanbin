package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;

public class IssueRepoMemTest {

    @Test
    public void createRetreiveFirstIssue(){
        Issue i = new Issue();
        i.setTitle("an issue title");
        IssueRepoMem sut = new IssueRepoMem();
        long initialCount = sut.count();

        sut.persist(i);

        // On vérifie que l'ID a bien été généré (qu'il n'est plus 0)
        assertNotEquals(0, i.getId(), "L'ID doit être généré lors du persist");

        long newId = i.getId();
        Issue retreive = sut.find(newId);
        assertNotNull(retreive);
        assertEquals(newId, retreive.getId());
        assertEquals(i.getTitle(), retreive.getTitle());
        assertEquals(initialCount + 1, sut.count());
    }

    @Test
    public void persist_shouldNotChangeId_whenIdIsAlreadySet() {
        IssueRepoMem sut = new IssueRepoMem();
        Issue i = new Issue(100L, "Existing ID");
        sut.persist(i);
        assertEquals(100L, i.getId());
        assertNotNull(sut.find(100L));
    }

    @Test
    public void testRemoveIssue() {
        IssueRepoMem sut = new IssueRepoMem();
        Issue i = new Issue();
        i.setTitle("Story à supprimer");
        sut.persist(i);
        long id = i.getId();
        assertNotNull(sut.find(id));
        sut.remove(id);
        assertNull(sut.find(id));
    }

    @Test
    public void testFindAll() {
        IssueRepoMem repo = new IssueRepoMem();

        Issue i1 = new Issue(1L, "Story 1");
        Issue i2 = new Issue(2L, "Story 2");
        repo.persist(i1);
        repo.persist(i2);
        Collection<Issue> allIssues = repo.findAll();

        assertEquals(2, allIssues.size());
        assertTrue(allIssues.contains(i1));
        assertTrue(allIssues.contains(i2));
    }
}