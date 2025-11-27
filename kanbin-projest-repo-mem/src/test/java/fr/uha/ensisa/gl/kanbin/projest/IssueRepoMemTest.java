package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IssueRepoMemTest {

    @Test
    public void createRetreiveFirstIssue(){
        Issue i = new Issue();
        i.setTitle("an issue title");
        IssueRepoMem sut = new IssueRepoMem();
        long initialCount = sut.count();
        sut.persist(i);
        long newId = i.getId();
        Issue retreive = sut.find(newId);
        assertNotNull(retreive);
        assertEquals(newId, retreive.getId());
        assertEquals(i.getTitle(), retreive.getTitle());
        assertEquals(initialCount + 1, sut.count());
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
}