package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
}