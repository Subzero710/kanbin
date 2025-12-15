package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueRepo issueRepo;
    @Mock
    private BoardRepo boardRepo; // Mock du BoardRepo ajouté
    @Mock
    private RedirectAttributes redirectAttributes;

    private IssueController sut;

    @BeforeEach
    void setUp() {
        // Nouveau constructeur avec 2 arguments
        sut = new IssueController(issueRepo, boardRepo);
    }

    @Test
    void listIssues_shouldReturnAllIssues() {
        when(issueRepo.findAll()).thenReturn(List.of(new Issue(1, "A"), new Issue(2, "B")));
        ModelAndView mv = sut.listIssues();
        assertEquals("list-issues", mv.getViewName());
        assertNotNull(mv.getModel().get("issues"));
    }

    @Test
    void newIssue_shouldReturnFormWithRows() {
        // On simule la présence d'un board pour avoir les rows
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().get("issue") instanceof Issue);
        // Vérifie qu'on passe bien les rows à la vue
        assertNotNull(mv.getModel().get("rows"));
    }

    @Test
    void createIssue_shouldPersistAndRedirect() {
        Issue issue = new Issue();
        issue.setTitle("Test");

        // Simulation board pour rowKey par défaut
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        String view = sut.createIssue(issue, redirectAttributes);

        verify(issueRepo).persist(issue);
        assertEquals("redirect:/board", view);
    }

    @Test
    void editIssue_whenFound_shouldReturnForm() {
        long id = 1L;
        when(issueRepo.find(id)).thenReturn(new Issue(id, "To Edit"));
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        // Correction du nom de méthode : editIssue au lieu de editIssueForm
        ModelAndView mv = sut.editIssue(id);

        assertEquals("edit-issue", mv.getViewName());
        assertEquals(id, ((Issue) mv.getModel().get("issue")).getId());
    }

    @Test
    void editIssue_whenNotFound_shouldRedirect() {
        long id = 99L;
        when(issueRepo.find(id)).thenReturn(null);

        ModelAndView mv = sut.editIssue(id);

        assertEquals("redirect:/issues", mv.getViewName());
    }

    @Test
    void updateIssue_shouldUpdateAndRedirect() {
        long id = 10L;
        Issue existing = new Issue(id, "Old");
        existing.setColumnKey("todo");
        existing.setRowKey("urgent"); // Simulation row existante

        when(issueRepo.find(id)).thenReturn(existing);

        Issue update = new Issue();
        update.setTitle("New");

        String view = sut.updateIssue(id, update, redirectAttributes);

        verify(issueRepo).persist(update);
        // Vérifie qu'on garde les anciennes clés si nulles
        assertEquals("todo", update.getColumnKey());
        assertEquals("urgent", update.getRowKey());
        assertEquals("redirect:/board", view);
    }

    @Test
    void deleteIssue_shouldRemoveAndRedirect() {
        long id = 5L;
        when(issueRepo.find(id)).thenReturn(new Issue(id, "To Delete"));

        String view = sut.deleteIssue(id, redirectAttributes);

        verify(issueRepo).remove(id);
        assertEquals("redirect:/board", view);
    }
}