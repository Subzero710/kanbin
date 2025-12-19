package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepoFactoryMemTest {

    @Test
    void testRepoFactoryInitializationAndGetters() {
        RepoFactoryMem factory = new RepoFactoryMem();

        assertNotNull(factory.getIssueRepo(),
                "La factory doit renvoyer un IssueRepo valide");

        BoardRepo boardRepo = factory.getBoardRepo();
        assertNotNull(boardRepo,
                "La factory doit renvoyer un BoardRepo valide");

        assertFalse(boardRepo.findAll().isEmpty(),
                "Le BoardRepo doit contenir des données par défaut (seed appelé)");

        Board defaultBoard = boardRepo.findAll().getFirst();
        assertEquals("Default", defaultBoard.getName());

        assertFalse(defaultBoard.getRows().isEmpty(),
                "Le board doit contenir la swimlane par défaut 'Non catégorisé'");

        Row defaultRow = defaultBoard.getRows().get(0);
        assertEquals("default", defaultRow.getKey());
        assertTrue(defaultRow.isFixed(),
                "La swimlane par défaut doit être marquée comme FIXE");
    }
}