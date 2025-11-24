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

    // Le Test pour l'Issue #29
    @Test
    void addColumn_shouldCallAddColumnOnRepository() {
        String newColumnTitle = "Test Column #29";
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));



        sut.addColumn(newColumnTitle);


        verify(boardRepo, times(1)).addColumn(
                eq(TEST_BOARD_ID), // L'ID du Board doit être le bon
                any(Column.class)  // L'argument doit être une instance de Column
        );
    }
}