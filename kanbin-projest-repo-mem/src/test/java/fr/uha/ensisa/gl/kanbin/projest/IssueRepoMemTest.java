package fr.uha.ensisa.gl.kanbin.projest;

import fr.uha.ensisa.gl.kanbin.projest.repo.mem.IssueRepoMem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class IssueRepoMemTest {

    @Test
    public void createRetreiveFirstIssue(){
        Issue i = new Issue("an issue title");
        long id = i.getId();
        assumeTrue(id > 0, "Issue should be given an ID at creation time");
        IssueRepoMem sut = new IssueRepoMem();
        sut.persist(i);
        Issue retreive = sut.find(id);
        assertNotNull(retreive);
        assertEquals(id, retreive.getId());
        assertEquals(i.getTitle(), retreive.getTitle());
    }
}
