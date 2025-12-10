package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepoFactoryMemTest {

    @Test
    void testRepoFactoryInitializationAndGetters() {
        RepoFactoryMem factory = new RepoFactoryMem();
        assertNotNull(factory.getIssueRepo(),
                "La factory doit renvoyer un IssueRepo valide");
        assertNotNull(factory.getBoardRepo(),
                "La factory doit renvoyer un BoardRepo valide");
    }
}