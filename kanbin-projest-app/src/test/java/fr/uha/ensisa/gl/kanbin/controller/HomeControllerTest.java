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

public class HomeControllerTest {

    // 1. Déclarer les mocks
    @Mock
    private BoardRepo boardRepo;

    // 2. Injecter les mocks
    @InjectMocks
    private HomeController sut;

    // Données de base pour le test
    private final long TEST_BOARD_ID = 1L;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        // Initialiser Mockito pour créer les @Mock et injecter dans @InjectMocks
        MockitoAnnotations.openMocks(this);

        // Préparer un Board que le Repo est censé retourner
        testBoard = new Board(TEST_BOARD_ID, "Test Board");
        testBoard.addColumn(new Column(100L, "initial", "Initial Column"));
    }

    // Le Test pour l'Issue #29
    @Test
    void addColumn_shouldCallAddColumnOnRepository() {
        String newColumnTitle = "Test Column #29";
        // Quand le contrôleur appelle findAll(), on simule le retour de notre Board de test
        when(boardRepo.findAll()).thenReturn(List.of(testBoard));


        // EXÉCUTION DU TEST

        sut.addColumn(newColumnTitle);


        // VÉRIFICATION (ASSERTION)
        // ------------------
        // On vérifie que la méthode CRITIQUE (addColumn) du repository A BIEN ÉTÉ APPELÉE

        // Vérifie que boardRepo.addColumn() a été appelé UNE FOIS
        verify(boardRepo, times(1)).addColumn(
                eq(TEST_BOARD_ID), // L'ID du Board doit être le bon
                any(Column.class)  // L'argument doit être une instance de Column
        );
    }
}