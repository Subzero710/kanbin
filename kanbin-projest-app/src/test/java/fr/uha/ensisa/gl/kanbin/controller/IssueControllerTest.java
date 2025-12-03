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
import static org.mockito.ArgumentMatchers.anyString;
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
        verify(issueRepo).persist(newIssue);
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
        assertEquals("redirect:/issues", viewName);
    }

    @Test
    void deleteIssue_shouldRemoveAndRedirect() {
        long idToDelete = 123L;
        String viewName = sut.deleteIssue(idToDelete, redirectAttributes);
        verify(issueRepo).remove(idToDelete);
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
        assertEquals("redirect:/issues", viewName);
    }

    @Test
    void newIssue_shouldShowCreateForm() {
        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().containsKey("issue"));
    }

    @Test
    void editIssueForm_shouldShowEditView_whenIssueExists() {
        long id = 5L;
        Issue existing = new Issue(id, "Old Title");
        when(issueRepo.find(id)).thenReturn(existing);
        ModelAndView mv = sut.editIssueForm(id);
        assertEquals("edit-issue", mv.getViewName());
        assertEquals(existing, mv.getModel().get("issue"));
    }

    @Test
    void editIssueForm_shouldRedirect_whenIssueDoesNotExist() {
        long id = 99L;
        when(issueRepo.find(id)).thenReturn(null);
        ModelAndView mv = sut.editIssueForm(id);
        assertEquals("redirect:/issues", mv.getViewName());
    }

    @Test
    void updateIssue_shouldUpdateAndRedirect() {
        long id = 10L;
        Issue existing = new Issue(id, "Old Title");
        existing.setColumnKey("todo");
        when(issueRepo.find(id)).thenReturn(existing);
        Issue updatedData = new Issue();
        updatedData.setTitle("New Title");
        String viewName = sut.updateIssue(id, updatedData, redirectAttributes);
        assertEquals("redirect:/issues", viewName);
        verify(issueRepo).persist(updatedData);
        assertEquals(id, updatedData.getId());
        assertEquals("todo", updatedData.getColumnKey());
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
    }
    
    @Test
    void updateIssue_shouldPreserveColumnKey_whenUpdating() {
        long id = 15L;
        Issue existing = new Issue(id, "Title", "done");
        existing.setDetail("Detail");
        when(issueRepo.find(id)).thenReturn(existing);
        Issue updatedData = new Issue();
        updatedData.setTitle("Updated Title");
        updatedData.setDetail("Updated Detail");
        sut.updateIssue(id, updatedData, redirectAttributes);
        verify(issueRepo).persist(any(Issue.class));
        assertEquals("done", updatedData.getColumnKey());
    }
}