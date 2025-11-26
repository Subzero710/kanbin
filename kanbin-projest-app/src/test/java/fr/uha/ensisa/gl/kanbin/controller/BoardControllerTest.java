package fr.uha.ensisa.gl.kanbin.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;

public class BoardControllerTest {

    @Mock
    private BoardRepo boardRepo;

    @InjectMocks
    private BoardController sut;

    private final long TEST_BOARD_ID = 1L;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testBoard = new Board(TEST_BOARD_ID, "Test Board");
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    // TEST 1 : Cas où le Board existe déjà (Standard)
    @Test
    void addColumn_whenBoardExists_shouldAddColumnToIt() {
        // Préparation
        String newColumnTitle = "Test Column #29";
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));

        sut.addColumn(newColumnTitle);
        verify(boardRepo, times(1)).addColumn(
                eq(TEST_BOARD_ID),
                any(Column.class)
        );
    }

    // TEST 2 : Cas où le Board n'existe pas (Repo vide) -> Le cas "Lambda"
    @Test
    void addColumn_whenRepoIsEmpty_shouldCreateDefaultBoard() {
        // Préparation : Repo vide
        when(boardRepo.findAll()).thenReturn(List.of());

        // On doit mocker le save() pour qu'il retourne un board valide avec l'ID 1
        when(boardRepo.save(any(Board.class))).thenAnswer(invocation -> {
            Board b = invocation.getArgument(0);
            b.setId(TEST_BOARD_ID);
            return b;
        });
        sut.addColumn("Ma Colonne");
        verify(boardRepo).save(any(Board.class));
        verify(boardRepo).addColumn(eq(TEST_BOARD_ID), any(Column.class));
    }

    @Test
    void homeRedirect_shouldRedirectToBoard() {
        String viewName = sut.homeRedirect();
        assertEquals("redirect:/board", viewName);
    }

    @Test // remove a column
    void postRemoveColumn_shouldSaveBoardWithoutThatColumn() {
        long columnToRemoveId = 100L;
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));
        assertTrue(testBoard.getColumns().stream().anyMatch(c -> c.getId() == columnToRemoveId),
                "Pré-condition échouée : Le testBoard doit contenir la colonne à retirer (ID 100L)");

        String viewName = sut.removeColumn(columnToRemoveId);
        assertEquals("redirect:/board", viewName, "Doit rediriger vers /board");

        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepo, times(1)).save(captor.capture());
        Board saved = captor.getValue();
        assertTrue(saved.getColumns().stream().noneMatch(c -> c.getId() == columnToRemoveId),
                "La colonne avec id " + columnToRemoveId + " doit avoir été retirée avant la sauvegarde");
        assertEquals(TEST_BOARD_ID, saved.getId(), "L'ID du Board sauvegardé doit être le même");
    }
}