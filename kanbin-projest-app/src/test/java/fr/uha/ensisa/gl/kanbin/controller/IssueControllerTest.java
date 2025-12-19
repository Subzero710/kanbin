package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueRepo issueRepo;
    @Mock
    private BoardRepo boardRepo;
    @Mock
    private RedirectAttributes redirectAttributes;

    private IssueController sut;

    @BeforeEach
    void setUp() {
        sut = new IssueController(issueRepo, boardRepo);
    }
    static class IssueControllerForcedBoard extends IssueController {
        private final Board forced;

        IssueControllerForcedBoard(IssueRepo issueRepo, BoardRepo boardRepo, Board forced) {
            super(issueRepo, boardRepo);
            this.forced = forced;
        }

        @Override
        protected Board getOrCreateDefaultBoard() {
            return forced;
        }
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
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().get("issue") instanceof Issue);
        assertNotNull(mv.getModel().get("rows"));
    }

    @Test
    void createIssue_shouldPersistAndRedirect() {
        Issue issue = new Issue();
        issue.setTitle("Test");
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
        LocalDateTime closedDate = LocalDateTime.now();
        Issue existing = new Issue(id, "Old");
        existing.setColumnKey("todo");
        existing.setRowKey("urgent");
        existing.setClosedAt(closedDate);

        when(issueRepo.find(id)).thenReturn(existing);

        Issue update = new Issue();
        update.setTitle("New");

        String view = sut.updateIssue(id, update, redirectAttributes);

        verify(issueRepo).persist(update);
        assertEquals("todo", update.getColumnKey());
        assertEquals("urgent", update.getRowKey());
        assertEquals(closedDate, update.getClosedAt());
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

    @Test
    void createIssue_NoRowKey_ShouldAssignDefaultRow() {
        Board mockBoard = new Board("Default Board");
        fr.uha.ensisa.gl.kanbin.projest.model.Row defaultRow = new fr.uha.ensisa.gl.kanbin.projest.model.Row("row_def", "Default Row");
        mockBoard.getRows().add(defaultRow);

        when(boardRepo.findAll()).thenReturn(List.of(mockBoard));

        Issue issue = new Issue();
        issue.setTitle("Ticket sans ligne");
        issue.setRowKey(null);

        sut.createIssue(issue, redirectAttributes);
        assertEquals("row_def", issue.getRowKey());
    }

    @Test
    void createIssue_ExistingId_ShouldPreserveOldKeys() {
        long id = 50L;
        Issue oldIssue = new Issue(id, "Vieux Titre");
        oldIssue.setColumnKey("old_col");
        oldIssue.setRowKey("old_row");
        when(issueRepo.find(id)).thenReturn(oldIssue);

        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));


        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setTitle("Nouveau Titre");
        formIssue.setColumnKey(null);
        formIssue.setRowKey("");

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("old_col", formIssue.getColumnKey());
        assertEquals("old_row", formIssue.getRowKey());
        verify(issueRepo).persist(formIssue);
    }

    @Test
    void deleteIssue_UnknownId_ShouldNotCrash() {
        long id = 999L;
        when(issueRepo.find(id)).thenReturn(null); // L'ID n'existe pas
        String view = sut.deleteIssue(id, redirectAttributes);
        verify(issueRepo, never()).remove(anyLong());
        assertEquals("redirect:/board", view);
    }

    @Test
    void createIssue_WithExplicitRowKey_ShouldNotUseDefault() {
        Issue issue = new Issue();
        issue.setTitle("Tache avec ligne forcée");
        issue.setRowKey("row_custom"); // L'utilisateur choisit sa ligne
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));
        sut.createIssue(issue, redirectAttributes);
        assertEquals("row_custom", issue.getRowKey());
    }

    @Test
    void createIssue_ExistingId_WithNewKeys_ShouldNotUseOldKeys() {
        long id = 5L;
        Issue oldIssue = new Issue(id, "Old Title");
        oldIssue.setColumnKey("col_old");
        oldIssue.setRowKey("row_old");
        when(issueRepo.find(id)).thenReturn(oldIssue);
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setColumnKey("col_new");
        formIssue.setRowKey("row_new");

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("col_new", formIssue.getColumnKey());
        assertEquals("row_new", formIssue.getRowKey());
    }

    @Test
    void createIssue_WithIdButNotFound_ShouldIgnoreOldValues() {

        long id = 77L;
        Issue formIssue = new Issue();
        formIssue.setId(id);

        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));
        when(issueRepo.find(id)).thenReturn(null);
        sut.createIssue(formIssue, redirectAttributes);
        verify(issueRepo).persist(formIssue);
    }

    @Test
    void updateIssue_WithExplicitKeys_ShouldKeepThem() {
        long id = 10L;
        Issue existing = new Issue(id, "Old");
        existing.setColumnKey("col_old");
        existing.setRowKey("row_old");
        when(issueRepo.find(id)).thenReturn(existing);

        Issue update = new Issue();
        update.setColumnKey("col_new");
        update.setRowKey("row_new");
        sut.updateIssue(id, update, redirectAttributes);
        assertEquals("col_new", update.getColumnKey());
        assertEquals("row_new", update.getRowKey());
    }

    @Test
    void updateIssue_UnknownId_ShouldJustPersist() {

        long id = 999L;
        when(issueRepo.find(id)).thenReturn(null);
        Issue update = new Issue();
        update.setTitle("Forced Update");
        sut.updateIssue(id, update, redirectAttributes);
        verify(issueRepo).persist(update);
    }

    @Test
    void getOrCreateDefaultBoard_shouldCreateNewBoard_whenRepoEmpty_andSaveReturnsNull() {
        when(boardRepo.findAll()).thenReturn(Collections.emptyList());
        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertNotNull(mv.getModel().get("rows"));

        // Capture du board créé pour vérifier colonnes + row système
        org.mockito.ArgumentCaptor<Board> captor = org.mockito.ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());
        Board created = captor.getValue();

        assertEquals("Default", created.getName());
        assertTrue(created.getColumns().stream().anyMatch(c -> "backlog".equals(c.getKey()) && c.isFixed()));
        assertTrue(created.getColumns().stream().anyMatch(c -> "closed".equals(c.getKey()) && c.isFixed()));
        assertFalse(created.getRows().isEmpty());
        assertEquals("default", created.getRows().get(0).getKey());
        assertTrue(created.getRows().get(0).isFixed());
    }

    @Test
    void getOrCreateDefaultBoard_shouldCreateNewBoard_whenRepoEmpty_andSaveReturnsNonNull() {
        when(boardRepo.findAll()).thenReturn(Collections.emptyList());
        when(boardRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0)); // saved != null
        ModelAndView mv = sut.newIssue();

        assertEquals("create-issue", mv.getViewName());
        verify(boardRepo).save(org.mockito.ArgumentMatchers.any(Board.class));
    }

    @Test
    void getOrCreateDefaultBoard_shouldPreferBoardNamedDefault_whenPresent() {
        Board other = new Board("Other");
        other.addRow(new Row("r1", "Row 1"));

        Board def = new Board("Default");
        def.addRow(new Row("rDef", "Default Row"));

        when(boardRepo.findAll()).thenReturn(List.of(other, def));

        ModelAndView mv = sut.newIssue();
        List<Row> rows = (List<Row>) mv.getModel().get("rows");

        assertEquals(1, rows.size());
        assertEquals("rDef", rows.get(0).getKey());
        verify(boardRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOrCreateDefaultBoard_shouldFallbackToFirstBoard_whenNoDefaultPresent() {
        Board first = new Board("First");
        first.addRow(new Row("rFirst", "First Row"));

        Board second = new Board("Second");
        second.addRow(new Row("rSecond", "Second Row"));

        when(boardRepo.findAll()).thenReturn(List.of(first, second));

        ModelAndView mv = sut.newIssue();
        List<Row> rows = (List<Row>) mv.getModel().get("rows");

        assertEquals(1, rows.size());
        assertEquals("rFirst", rows.get(0).getKey());
        verify(boardRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOrCreateDefaultBoard_shouldAddDefaultRow_andSave_whenExistingBoardHasNoRows() {
        Board def = new Board("Default");
        when(boardRepo.findAll()).thenReturn(List.of(def));
        when(boardRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        ModelAndView mv = sut.newIssue();
        List<Row> rows = (List<Row>) mv.getModel().get("rows");

        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        assertEquals("default", rows.get(0).getKey());
        assertTrue(rows.get(0).isFixed());
        verify(boardRepo).save(def);
    }
    @Test
    void getOrCreateDefaultBoard_shouldCreateNewBoard_whenFindAllReturnsNull() {
        when(boardRepo.findAll()).thenReturn(null);
        when(boardRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        ModelAndView mv = sut.newIssue();
        assertEquals("create-issue", mv.getViewName());
        assertNotNull(mv.getModel().get("rows"));
        verify(boardRepo).save(org.mockito.ArgumentMatchers.any(Board.class));
    }

    @Test
    void getOrCreateDefaultBoard_shouldHandleNullRowsGetter_andSave() {
        Board def = org.mockito.Mockito.spy(new Board("Default"));
        org.mockito.Mockito.doReturn(null)
                .doCallRealMethod()
                .when(def).getRows();

        when(boardRepo.findAll()).thenReturn(List.of(def));
        when(boardRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        ModelAndView mv = sut.newIssue();
        List<Row> rows = (List<Row>) mv.getModel().get("rows");

        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        assertEquals("default", rows.get(0).getKey());
        verify(boardRepo).save(def);
    }

    @Test
    void createIssue_ExistingId_ShouldRestoreOldColumnKey_WhenColumnKeyEmpty() {
        long id = 123L;
        Issue oldIssue = new Issue(id, "Old");
        oldIssue.setColumnKey("old_col");
        oldIssue.setRowKey("old_row");
        when(issueRepo.find(id)).thenReturn(oldIssue);
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setColumnKey("");
        formIssue.setRowKey("row_new");

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("old_col", formIssue.getColumnKey());
        assertEquals("row_new", formIssue.getRowKey());
        verify(issueRepo).persist(formIssue);
    }

    @Test
    void createIssue_ExistingId_ShouldRestoreOldRowKey_WhenRowKeyNull() {
        long id = 124L;
        Issue oldIssue = new Issue(id, "Old");
        oldIssue.setColumnKey("old_col");
        oldIssue.setRowKey("old_row");
        when(issueRepo.find(id)).thenReturn(oldIssue);
        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        Issue formIssue = new Issue();
        formIssue.setId(id);
        formIssue.setColumnKey("col_new");
        formIssue.setRowKey(null);

        sut.createIssue(formIssue, redirectAttributes);

        assertEquals("col_new", formIssue.getColumnKey());
        assertEquals("old_row", formIssue.getRowKey());
        verify(issueRepo).persist(formIssue);
    }
    @Test
    void newIssue_whenBoardNull_shouldNotAddRows() {
        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, null);

        ModelAndView mv = ctrl.newIssue();

        assertEquals("create-issue", mv.getViewName());
        assertTrue(mv.getModel().get("issue") instanceof Issue);
        assertFalse(mv.getModel().containsKey("rows")); // couvre if (board != null) -> false
    }

    @Test
    void editIssue_whenBoardNull_shouldNotAddRows() {
        long id = 1L;
        when(issueRepo.find(id)).thenReturn(new Issue(id, "To Edit"));

        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, null);

        ModelAndView mv = ctrl.editIssue(id);

        assertEquals("edit-issue", mv.getViewName());
        assertFalse(mv.getModel().containsKey("rows")); // couvre if (board != null) -> false
    }

    @Test
    void createIssue_whenBoardNull_andRowKeyMissing_shouldNotSetDefaultRow() {
        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, null);

        Issue issue = new Issue();
        issue.setTitle("X");
        issue.setRowKey(null);

        ctrl.createIssue(issue, redirectAttributes);

        assertNull(issue.getRowKey()); // couvre (board != null && ...) -> false via board == null
        verify(issueRepo).persist(issue);
    }

    @Test
    void createIssue_whenRowKeyEmpty_shouldAssignDefaultRow() {
        Board board = new Board("Default");
        board.addRow(new Row("row_def", "Default Row"));

        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, board);

        Issue issue = new Issue();
        issue.setTitle("X");
        issue.setRowKey(""); // force (rowKey == null) false, (isEmpty) true

        ctrl.createIssue(issue, redirectAttributes);

        assertEquals("row_def", issue.getRowKey()); // couvre issue.getRowKey().isEmpty()
        verify(issueRepo).persist(issue);
    }

    @Test
    void createIssue_whenBoardRowsEmpty_shouldNotSetDefaultRow() {
        Board board = new Board("Default"); // getRows() non-null mais vide

        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, board);

        Issue issue = new Issue();
        issue.setTitle("X");
        issue.setRowKey(null);

        ctrl.createIssue(issue, redirectAttributes);

        assertNull(issue.getRowKey()); // couvre board.getRows()!=null true, !isEmpty false
        verify(issueRepo).persist(issue);
    }

    @Test
    void createIssue_whenBoardRowsNull_shouldNotSetDefaultRow() {
        Board board = spy(new Board("Default"));
        doReturn(null).when(board).getRows(); // force board.getRows() == null

        IssueController ctrl = new IssueControllerForcedBoard(issueRepo, boardRepo, board);

        Issue issue = new Issue();
        issue.setTitle("X");
        issue.setRowKey(null);

        ctrl.createIssue(issue, redirectAttributes);

        assertNull(issue.getRowKey()); // couvre board.getRows()==null => inner if false (short-circuit)
        verify(issueRepo).persist(issue);
    }

    @Test
    void createIssue_withIdZero_shouldNotLookUpOldIssue() {
        Issue issue = new Issue();
        issue.setId(0L);
        issue.setTitle("New Issue");

        when(boardRepo.findAll()).thenReturn(List.of(new Board("Default")));

        sut.createIssue(issue, redirectAttributes);
        verify(issueRepo, never()).find(0L);
        verify(issueRepo).persist(issue);
    }

}