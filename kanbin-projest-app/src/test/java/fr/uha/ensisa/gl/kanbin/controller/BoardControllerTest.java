package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BoardControllerTest {

    @Mock
    private BoardRepo boardRepo;

    @Mock
    private IssueRepo issueRepo;

    @InjectMocks
    private BoardController sut;

    private final long TEST_BOARD_ID = 1L;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = new Board(TEST_BOARD_ID, "Test Board");
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    @Test
    void board_shouldReturnBoardView() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        when(issueRepo.findAll()).thenReturn(List.of());
        ModelAndView mv = sut.board();
        assertEquals("board", mv.getViewName());
    }

    @Test
    void addColumn_whenBoardExists_shouldAddColumnToIt() {
        String newColumnTitle = "Test Column #29";
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        sut.addColumn(newColumnTitle);
        verify(boardRepo, times(1)).addColumn(
                eq(TEST_BOARD_ID),
                any(Column.class)
        );
    }

    @Test
    void postRemoveColumn_shouldSaveBoardWithoutThatColumn() {
        long columnToRemoveId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = sut.removeColumn(columnToRemoveId, redirectAttributes);
        assertEquals("redirect:/board", viewName);

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo, times(1)).save(captor.capture());
        Board saved = captor.getValue();
        assertTrue(saved.getColumns().stream().noneMatch(c -> c.getId() == columnToRemoveId));
    }

    @Test
    void removeColumn_fixedColumn_shouldFail_andSetErrorMessage() {
        long fixedColId = 999L;
        Column fixedCol = new Column(fixedColId, "backlog", "Backlog");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);

        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = sut.removeColumn(fixedColId, redirectAttributes);

        assertEquals("redirect:/board", viewName);
        verify(boardRepo, never()).save(any());

        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
        assertEquals("Action interdite : Cette colonne système ne peut pas être supprimée.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void reorderColumn_validMove_shouldUpdateOrderAndSave() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Backlog");
        fixed.setFixed(true);
        Column colA = new Column(101L, "col-a", "Col A");
        Column colB = new Column(102L, "col-b", "Col B");

        board.addColumn(fixed);
        board.addColumn(colA);
        board.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(102L, 1);

        assertEquals("OK", response);
        // Utilisation de get(1) reste valide car List n'a pas de "getSecond" standard simple
        assertEquals(colB, board.getColumns().get(1));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_moveFixedColumn_shouldFail() {
        Board board = new Board(TEST_BOARD_ID, "Test Board");
        Column fixed = new Column(100L, "fixed", "Fixed");
        fixed.setFixed(true);
        board.addColumn(fixed);
        board.addColumn(new Column(101L, "col-a", "Col A"));

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String response = sut.reorderColumn(100L, 1);

        assertEquals("ERROR: Invalid move", response);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void editColumnForm_fixedColumn_shouldRedirect() {
        long fixedColId = 888L;
        Column fixedCol = new Column(fixedColId, "fix", "Fixed");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        ModelAndView mv = sut.editColumnForm(fixedColId);
        assertEquals("redirect:/board", mv.getViewName());
    }

    @Test
    void updateColumn_fixedColumn_shouldNotChangeTitle() {
        long fixedColId = 777L;
        Column fixedCol = new Column(fixedColId, "fix", "Original Title");
        fixedCol.setFixed(true);
        testBoard.addColumn(fixedCol);

        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        String view = sut.updateColumn(fixedColId, "Hacked Title");

        assertEquals("redirect:/board", view);
        assertEquals("Original Title", fixedCol.getTitle());
        verify(boardRepo, never()).save(any());
    }

    @Test
    void moveColumnInternal_negativeIndex_shouldReturnFalse() {
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        String response = sut.reorderColumn(100L, -5);
        assertEquals("ERROR: Invalid move", response);
    }
}