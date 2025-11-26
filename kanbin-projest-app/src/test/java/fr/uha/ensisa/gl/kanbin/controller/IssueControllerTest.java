package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class IssueControllerTest {

    @Mock
    private IssueRepo issueRepo;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private IssueController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listIssues_shouldReturnListViewWithIssues() {
        when(issueRepo.findAll()).thenReturn(List.of(
                new Issue(1L, "Story A"),
                new Issue(2L, "Story B")
        ));
        ModelAndView mv = sut.listIssues();
        assertEquals("list-issues", mv.getViewName());
        @SuppressWarnings("unchecked")
        Collection<Issue> issues = (Collection<Issue>) mv.getModel().get("issues");
        assertEquals(2, issues.size());
        verify(issueRepo).findAll();
    }

    @Test
    void createIssue_shouldPersistAndRedirect() {
        Issue newIssue = new Issue();
        newIssue.setTitle("Nouvelle Story");
        String viewName = sut.createIssue(newIssue, redirectAttributes);
        verify(issueRepo).persist(newIssue); // Vérifie la sauvegarde
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString()); // Vérifie le message de confirmation
        assertEquals("redirect:/issues", viewName); // Vérifie la redirection
    }

    @Test
    void deleteIssue_shouldRemoveAndRedirect() {
        long idToDelete = 123L;
        String viewName = sut.deleteIssue(idToDelete, redirectAttributes);
        verify(issueRepo).remove(idToDelete); // Vérifie l'appel de suppression
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
        assertEquals("redirect:/issues", viewName);
    }

    @Test
    void newIssue_shouldShowCreateForm() {
        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().containsKey("issue"));
    }
}