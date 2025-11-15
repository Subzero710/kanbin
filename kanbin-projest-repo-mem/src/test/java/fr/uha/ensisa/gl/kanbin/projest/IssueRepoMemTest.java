package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class IssueRepoMemTest {

    @Test
    public void createRetreiveFirstIssue(){
        Issue i = new Issue();
        i.setTitle("an issue title");

        IssueRepoMem sut = new IssueRepoMem();

        sut.persist(i);
        long id = i.getId();

        Issue retrieve = sut.find(id);
        assertNotNull(retrieve);
        assertEquals(id, retrieve.getId());
        assertEquals(i.getTitle(), retrieve.getTitle());
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