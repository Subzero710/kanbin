package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith; // Pour JUnit 5
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension; // Pour l'extension Mockito
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

@ExtendWith(MockitoExtension.class) // Remplace le openMocks(this)
public class IssueControllerTest {

    @Mock
    private IssueRepo issueRepo;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private IssueController sut;

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
    void createIssue_withExistingIdAndNullKey_shouldRestoreOldKey() {
        long id = 50L;
        String originalKey = "col-doing";
        Issue oldIssue = new Issue(id, "Old Story");
        oldIssue.setColumnKey(originalKey);
        Issue newVersion = new Issue(id, "Updated Story");
        newVersion.setColumnKey(null);
        when(issueRepo.find(id)).thenReturn(oldIssue);
        sut.createIssue(newVersion, redirectAttributes);
        assertEquals(originalKey, newVersion.getColumnKey());
        verify(issueRepo).persist(newVersion);
    }

    @Test
    void createIssue_withExistingIdAndNewKey_shouldKeepNewKey() {
        long id = 51L;
        Issue oldIssue = new Issue(id, "Old Story");
        oldIssue.setColumnKey("col-todo");
        Issue newVersion = new Issue(id, "Moved Story");
        newVersion.setColumnKey("col-done");
        when(issueRepo.find(id)).thenReturn(oldIssue);
        sut.createIssue(newVersion, redirectAttributes);
        assertEquals("col-done", newVersion.getColumnKey());
        verify(issueRepo).persist(newVersion);
    }

    @Test
    void createIssue_whenNew_shouldAddCreationMessage() {
        Issue newIssue = new Issue();
        newIssue.setId(0L);
        newIssue.setTitle("Ma Nouvelle Story");
        sut.createIssue(newIssue, redirectAttributes);
        verify(redirectAttributes).addFlashAttribute(
                eq("message"),
                eq("La nouvelle story a été ajoutée.")
        );
    }

    // Le test pour la limite de 30 caractères
    @Test
    void createIssue_withLongTitle_shouldTruncateAndPersist() {
        Issue longIssue = new Issue();
        longIssue.setTitle("Un titre vraiment super long qui fait plus de trente caractères");

        sut.createIssue(longIssue, redirectAttributes);

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepo).persist(captor.capture());

        Issue capturedIssue = captor.getValue();
        assertEquals(30, capturedIssue.getTitle().length());
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