package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import static org.mockito.ArgumentMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardRepo boardRepo;
    @Mock
    private IssueRepo issueRepo;
    @Mock
    private RedirectAttributes redirectAttributes;

    private BoardController sut;

    @BeforeEach
    void setUp() {
        sut = new BoardController(boardRepo, issueRepo);
    }

    @Test
    void root_shouldRedirectToBoard() {
        // Correction nom méthode : homeRedirect -> root
        ModelAndView mv = sut.root();
        assertEquals("redirect:/board", mv.getViewName());
    }

    @Test
    void showBoard_shouldDisplayBoardWithRowsAndCols() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("todo", "Todo"));
        b.addRow(new Row("urgent", "Urgent"));

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of());

        // Correction nom méthode : board -> showBoard
        ModelAndView mv = sut.showBoard();

        assertEquals("board", mv.getViewName());
        assertSame(b, mv.getModel().get("board"));
        assertNotNull(mv.getModel().get("rows")); // Vérifie que les rows sont passées
        assertNotNull(mv.getModel().get("issuesByRowAndCol"));
    }

    @Test
    void addColumn_shouldAddColumnAndRedirect() {
        Board b = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(b));

        String view = sut.addColumn("New Col", "simple", 0, redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(boardRepo).addColumn(eq(b.getId()), any(Column.class));
    }

    @Test
    void addRow_shouldAddRowAndRedirect() {
        Board b = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(b));

        String view = sut.addRow("New Swimlane");

        assertEquals("redirect:/board", view);
        // Vérifie qu'on a bien ajouté la row
        assertFalse(b.getRows().isEmpty());
        verify(boardRepo).save(b);
    }

    @Test
    void moveIssueDnD_shouldUpdateColumnAndRow() {
        Board b = new Board("Test Board");
        Column todo = new Column("todo", "Todo");
        b.addColumn(todo);

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Task");
        issue.setColumnKey("old");
        issue.setRowKey("oldRow");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        // Appel avec 3 arguments (ID, Col, Row)
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(issueId, "todo", "newRow");

        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("todo", issue.getColumnKey());
        assertEquals("newRow", issue.getRowKey());
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldKeepRowIfNull() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("done", "Done"));

        long issueId = 1L;
        Issue issue = new Issue(issueId, "Task");
        issue.setRowKey("urgent"); // Ligne existante

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(issueId)).thenReturn(issue);

        // Appel avec Row = null (ne doit pas changer la ligne)
        sut.moveIssueDnD(issueId, "done", null);

        assertEquals("done", issue.getColumnKey());
        assertEquals("urgent", issue.getRowKey()); // N'a pas changé
    }

    // Test WIP Limit
    @Test
    void moveIssueDnD_shouldFailIfWipLimitReached() {
        Board b = new Board("Test Board");
        Column wipCol = new Column("wip", "WIP");
        wipCol.setWipLimit(1);
        b.addColumn(wipCol);

        // Déjà une issue dedans
        Issue blocker = new Issue(2L, "Blocker");
        blocker.setColumnKey("wip");
        blocker.setRowKey("default"); // Important pour le comptage

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.findAll()).thenReturn(List.of(blocker));

        Issue newIssue = new Issue(1L, "Mover");
        newIssue.setRowKey("default");
        when(issueRepo.find(1L)).thenReturn(newIssue);

        // Tentative de déplacer dans une colonne pleine
        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(1L, "wip", "default");

        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("maximum"));
    }

    @Test
    void getOrCreateDefaultBoard_ShouldInitializeDefaults_WhenNoBoardExists() {
        // Arrange : Le repo est vide
        when(boardRepo.findAll()).thenReturn(List.of());
        // On intercepte la sauvegarde pour vérifier ce qui a été créé
        when(boardRepo.save(any(Board.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        // On appelle showBoard qui appelle indirectement getOrCreateDefaultBoard
        sut.showBoard();

        // Assert
        org.mockito.ArgumentCaptor<Board> captor = org.mockito.ArgumentCaptor.forClass(Board.class);
        verify(boardRepo).save(captor.capture());

        Board savedBoard = captor.getValue();
        // Vérifie les lignes rouges : Colonnes fixes et Row par défaut
        assertTrue(savedBoard.getColumns().stream().anyMatch(c -> c.getKey().equals("backlog") && c.isFixed()));
        assertTrue(savedBoard.getColumns().stream().anyMatch(c -> c.getKey().equals("closed") && c.isFixed()));
        assertFalse(savedBoard.getRows().isEmpty());
        assertEquals("default", savedBoard.getRows().get(0).getKey());
    }

    @Test
    void showBoard_ShouldSortBacklogAndClosedColumns() {
        // Arrange
        Board board = new Board("Test Board");
        board.addRow(new fr.uha.ensisa.gl.kanbin.projest.model.Row("def", "Def"));

        fr.uha.ensisa.gl.kanbin.projest.model.Column backlog = new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog");
        fr.uha.ensisa.gl.kanbin.projest.model.Column closed = new fr.uha.ensisa.gl.kanbin.projest.model.Column("closed", "Closed");
        board.addColumn(backlog);
        board.addColumn(closed);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Issues pour le Backlog (Tri ID décroissant)
        Issue i1 = new Issue(1L, "Old"); i1.setColumnKey("backlog"); i1.setRowKey("def");
        Issue i2 = new Issue(2L, "New"); i2.setColumnKey("backlog"); i2.setRowKey("def");

        // Issues pour Closed (Tri Date décroissante)
        Issue c1 = new Issue(10L, "Closed Yesterday");
        c1.setColumnKey("closed"); c1.setRowKey("def");
        c1.setClosedAt(java.time.LocalDateTime.now().minusDays(1));

        Issue c2 = new Issue(11L, "Closed Today");
        c2.setColumnKey("closed"); c2.setRowKey("def");
        c2.setClosedAt(java.time.LocalDateTime.now());

        when(issueRepo.findAll()).thenReturn(List.of(i1, i2, c1, c2));

        // Act
        ModelAndView mv = sut.showBoard();
        Map<String, Map<String, List<Issue>>> data = (Map) mv.getModel().get("issuesByRowAndCol");

        // Assert
        List<Issue> backlogList = data.get("def").get("backlog");
        assertEquals(2L, backlogList.get(0).getId()); // Le plus récent (ID 2) en premier

        List<Issue> closedList = data.get("def").get("closed");
        assertEquals(11L, closedList.get(0).getId()); // Le fermé aujourd'hui en premier
    }

    @Test
    void removeColumn_ShouldFail_IfNotEmpty() {
        // Arrange
        Board board = new Board("Test Board");
        fr.uha.ensisa.gl.kanbin.projest.model.Column col = new fr.uha.ensisa.gl.kanbin.projest.model.Column("wip", "WIP");
        col.setId(5L);
        board.addColumn(col);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // On simule une issue dans cette colonne via countIssuesInColumn -> findAll
        Issue issue = new Issue(1L, "Task");
        issue.setColumnKey("wip");
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        // Act
        String view = sut.removeColumn(5L, redirectAttributes);

        // Assert
        verify(boardRepo, never()).removeColumn(anyLong(), anyLong());
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("contient des tâches"));
    }

    @Test
    void removeColumn_ShouldFail_IfNotFound() {
        // Arrange
        Board board = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Act : ID inconnu 999
        String view = sut.removeColumn(999L, redirectAttributes);

        // Assert
        assertEquals("redirect:/board", view);
    }

    @Test
    void reorderColumn_ShouldFail_InvalidIndex() {
        Board board = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Act : Index négatif
        String res = sut.reorderColumn(1L, -1);

        // Assert
        assertEquals("ERROR: Invalid move", res);
    }

    @Test
    void reorderColumn_ShouldFail_FixedColumn() {
        Board board = new Board("Test Board");
        fr.uha.ensisa.gl.kanbin.projest.model.Column backlog = new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog");
        backlog.setFixed(true);
        backlog.setId(10L);
        board.addColumn(backlog);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Act : Essayer de bouger le backlog
        String res = sut.reorderColumn(10L, 2);

        // Assert
        assertEquals("ERROR: Invalid move", res);
    }

    @Test
    void addColumn_ShouldFail_WhenNameAlreadyExists() {
        // --- ARRANGE (Préparation) ---
        // 1. On crée un board fictif
        Board board = new Board("Test Board");
        board.setId(1L);

        // 2. On lui ajoute DÉJÀ une colonne nommée "Dev"
        // (Attention à la casse : ton code semble sensible aux majuscules/minuscules selon le screenshot)
        fr.uha.ensisa.gl.kanbin.projest.model.Column existingCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev", "Dev");
        board.addColumn(existingCol);

        // 3. On dit au Mock de renvoyer ce board
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // --- ACT (Action) ---
        // On essaie d'ajouter une colonne avec le MÊME titre "Dev"
        String viewName = sut.addColumn("Dev", "simple", 0, redirectAttributes);

        // --- ASSERT (Vérification) ---
        // 1. Vérifie qu'on est redirigé vers le board
        assertEquals("redirect:/board", viewName);

        // 2. Vérifie qu'un message d'erreur est bien ajouté (Cible la zone rouge de ton screenshot)
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("déjà pris"));

        // 3. CRUCIAL : Vérifie que boardRepo.addColumn(...) N'A JAMAIS ÉTÉ APPELÉ
        verify(boardRepo, never()).addColumn(anyLong(), any());
    }

    @Test
    void addColumn_ShouldCreateSubColumns_WhenTypeIsDouble() {
        // --- ARRANGE ---
        Board board = new Board("Test Board");
        board.setId(10L); // On donne un ID pour le verify plus tard
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // --- ACT ---
        // On ajoute une colonne "Feature" de type "double"
        String viewName = sut.addColumn("Feature", "double", 5, redirectAttributes);

        // --- ASSERT ---
        // 1. On vérifie la redirection
        assertEquals("redirect:/board", viewName);

        // 2. On CAPTURE la colonne qui a été envoyée au repo pour l'inspecter
        org.mockito.ArgumentCaptor<fr.uha.ensisa.gl.kanbin.projest.model.Column> columnCaptor =
                org.mockito.ArgumentCaptor.forClass(fr.uha.ensisa.gl.kanbin.projest.model.Column.class);

        // On vérifie que la méthode a été appelée sur le bon Board (ID 10) avec notre colonne
        verify(boardRepo).addColumn(eq(10L), columnCaptor.capture());

        // 3. Analyse de la colonne capturée (Cible le bloc "if double" de ton code)
        fr.uha.ensisa.gl.kanbin.projest.model.Column createdCol = columnCaptor.getValue();

        assertEquals("double", createdCol.getType()); // Vérifie le type
        assertEquals(2, createdCol.getSubColumns().size()); // Vérifie qu'il y a 2 enfants

        String subCol1Key = createdCol.getSubColumns().get(0).getKey();
        String subCol2Key = createdCol.getSubColumns().get(1).getKey();

        assertTrue(subCol1Key.endsWith("-todo"));
        assertTrue(subCol2Key.endsWith("-wip"));
    }

    @Test
    void removeColumn_ShouldCountIssuesInSubColumns() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // 1. Création d'une colonne "Double" (Parent)
        fr.uha.ensisa.gl.kanbin.projest.model.Column parentCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev", "Dev");
        parentCol.setId(10L);
        parentCol.setType("double"); // Important pour la logique

        // 2. Ajout des sous-colonnes (simulation de ce que fait addColumn)
        fr.uha.ensisa.gl.kanbin.projest.model.Column subTodo = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev-todo", "Dev Todo");
        fr.uha.ensisa.gl.kanbin.projest.model.Column subWip = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev-wip", "Dev Wip");
        parentCol.addSubColumn(subTodo);
        parentCol.addSubColumn(subWip);

        realBoard.addColumn(parentCol);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // 3. On place une Issue dans la sous-colonne "dev-wip"
        Issue hiddenIssue = new Issue(100L, "Hidden Task");
        hiddenIssue.setColumnKey("dev-wip"); // C'est une clé enfant !

        // countIssuesInColumn appelle findAll()
        when(issueRepo.findAll()).thenReturn(List.of(hiddenIssue));

        // --- ACT ---
        // On essaie de supprimer le PARENT (ID 10)
        String view = sut.removeColumn(10L, redirectAttributes);

        // --- ASSERT ---
        // Ça doit échouer car il y a une tâche dans un enfant
        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("contient des tâches"));

    }

    @Test
    void showBoard_ShouldSortIssuesCorrectly() {
        // --- ARRANGE ---
        // 1. Vrai Board avec les colonnes nécessaires
        Board realBoard = new Board("Test Board");
        realBoard.addRow(new fr.uha.ensisa.gl.kanbin.projest.model.Row("def", "Def"));
        realBoard.addColumn(new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog"));
        realBoard.addColumn(new fr.uha.ensisa.gl.kanbin.projest.model.Column("closed", "Closed"));

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // 2. Issues pour le Backlog (ID 10 et ID 20) -> On veut 20 avant 10
        Issue i1 = new Issue(10L, "Vieux");
        i1.setColumnKey("backlog"); i1.setRowKey("def");

        Issue i2 = new Issue(20L, "Recent");
        i2.setColumnKey("backlog"); i2.setRowKey("def");

        // 3. Issues pour Closed -> On veut la date la plus récente en premier
        Issue c1 = new Issue(100L, "Fermé Hier");
        c1.setColumnKey("closed"); c1.setRowKey("def");
        c1.setClosedAt(java.time.LocalDateTime.now().minusDays(1)); // Hier

        Issue c2 = new Issue(200L, "Fermé Auj");
        c2.setColumnKey("closed"); c2.setRowKey("def");
        c2.setClosedAt(java.time.LocalDateTime.now()); // Aujourd'hui

        when(issueRepo.findAll()).thenReturn(List.of(i1, i2, c1, c2));

        // --- ACT ---
        ModelAndView mv = sut.showBoard();

        // --- ASSERT ---
        // On récupère la grosse Map compliquée du modèle
        java.util.Map<String, java.util.Map<String, List<Issue>>> data =
                (java.util.Map) mv.getModel().get("issuesByRowAndCol");

        // Vérif Backlog : ID 20 doit être avant ID 10
        List<Issue> backlogList = data.get("def").get("backlog");
        assertEquals(20L, backlogList.get(0).getId());
        assertEquals(10L, backlogList.get(1).getId());

        // Vérif Closed : Date récente (c2) avant Date ancienne (c1)
        List<Issue> closedList = data.get("def").get("closed");
        assertEquals(200L, closedList.get(0).getId());
    }

    @Test
    void showBoard_ShouldFixOrphanIssues() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        // Ligne par défaut et Colonne par défaut (backlog)
        realBoard.addRow(new fr.uha.ensisa.gl.kanbin.projest.model.Row("def-row", "Default"));
        realBoard.addColumn(new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog"));

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // Une issue cassée (null / null)
        Issue orphan = new Issue(99L, "Lost Issue");
        orphan.setRowKey(null);
        orphan.setColumnKey(null);

        when(issueRepo.findAll()).thenReturn(List.of(orphan));

        // --- ACT ---
        sut.showBoard();

        // --- ASSERT ---
        // 1. Vérifie qu'on a réparé l'objet en mémoire
        assertEquals("def-row", orphan.getRowKey());
        assertEquals("backlog", orphan.getColumnKey());

        // 2. Vérifie qu'on a sauvegardé la correction en base
        verify(issueRepo).persist(orphan);
    }

    @Test
    void updateColumn_ShouldFail_IfNameAlreadyExists() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // Colonne 1 : Celle qu'on veut modifier (ID 10)
        fr.uha.ensisa.gl.kanbin.projest.model.Column colToEdit = new fr.uha.ensisa.gl.kanbin.projest.model.Column("todo", "Todo");
        colToEdit.setId(10L);
        realBoard.addColumn(colToEdit);

        // Colonne 2 : Celle qui porte le nom conflictuel (ID 20)
        fr.uha.ensisa.gl.kanbin.projest.model.Column otherCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("done", "Done");
        otherCol.setId(20L);
        realBoard.addColumn(otherCol);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // --- ACT ---
        // On essaie de renommer la colonne 10 en "Done" (nom de la 20)
        String view = sut.updateColumn(10L, "Done", 5, redirectAttributes);

        // --- ASSERT ---
        // Redirection vers board
        assertEquals("redirect:/board", view);

        // Message d'erreur présent
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("déjà utilisé"));

        // Pas de sauvegarde
        verify(boardRepo, never()).save(any());
    }

    @Test
    void showBoard_ShouldDisplaySubColumns_WhenTypeIsDouble() {
        // ARRANGE
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        realBoard.addRow(new fr.uha.ensisa.gl.kanbin.projest.model.Row("def", "Default"));

        // Création d'une colonne "Double"
        fr.uha.ensisa.gl.kanbin.projest.model.Column dev = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev", "Dev");
        dev.setType("double");

        // Ajout des sous-colonnes (C'est ça qui va déclencher la boucle rouge)
        fr.uha.ensisa.gl.kanbin.projest.model.Column subTodo = new fr.uha.ensisa.gl.kanbin.projest.model.Column("dev-todo", "Dev Todo");
        dev.addSubColumn(subTodo);
        realBoard.addColumn(dev);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        when(issueRepo.findAll()).thenReturn(List.of()); // Pas besoin d'issues pour ce test

        // ACT
        ModelAndView mv = sut.showBoard();

        // ASSERT
        // On vérifie juste que ça n'a pas planté et que la map est construite
        java.util.Map<String, Object> data = (java.util.Map) mv.getModel().get("issuesByRowAndCol");
        assertNotNull(data);
        // Le code est passé dans le "else { for (Column sub : ...)}"
    }

    @Test
    void removeColumn_ShouldFail_IfFixedOrNotEmpty() {
        // ARRANGE
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // Cas 1 : Colonne Fixe (ex: Backlog)
        fr.uha.ensisa.gl.kanbin.projest.model.Column fixedCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog");
        fixedCol.setId(10L);
        fixedCol.setFixed(true);
        realBoard.addColumn(fixedCol);

        // Cas 2 : Colonne Non Vide
        fr.uha.ensisa.gl.kanbin.projest.model.Column busyCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("todo", "Todo");
        busyCol.setId(20L);
        realBoard.addColumn(busyCol);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // Pour le Cas 2, on simule une tâche dedans
        Issue task = new Issue(1L, "Tache");
        task.setColumnKey("todo");
        // Attention : countIssuesInColumn utilise findAll
        when(issueRepo.findAll()).thenReturn(List.of(task));

        // ACT & ASSERT 1 : Suppression Fixe
        String view1 = sut.removeColumn(10L, redirectAttributes);
        assertEquals("redirect:/board", view1);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("interdite"));

        // ACT & ASSERT 2 : Suppression Non Vide
        String view2 = sut.removeColumn(20L, redirectAttributes);
        assertEquals("redirect:/board", view2);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("contient des tâches"));

        // Vérifie qu'aucune suppression n'a été faite
        verify(boardRepo, never()).removeColumn(anyLong(), anyLong());
    }

    @Test
    void updateColumn_ShouldFail_IfNameConflict() {
        // ARRANGE
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // Col A (ID 10)
        fr.uha.ensisa.gl.kanbin.projest.model.Column colA = new fr.uha.ensisa.gl.kanbin.projest.model.Column("cola", "Col A");
        colA.setId(10L);
        realBoard.addColumn(colA);

        // Col B (ID 20)
        fr.uha.ensisa.gl.kanbin.projest.model.Column colB = new fr.uha.ensisa.gl.kanbin.projest.model.Column("colb", "Col B");
        colB.setId(20L);
        realBoard.addColumn(colB);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // ACT : On essaie de renommer A (10) avec le nom de B ("Col B")
        String view = sut.updateColumn(10L, "Col B", 0, redirectAttributes);

        // ASSERT
        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("déjà utilisé"));
    }

    @Test
    void moveCard_ShouldReturnOK() {
        // C'est un test tout bête pour couvrir la ligne "return OK"
        String result = sut.moveCard(1L, "row", "col");
        assertEquals("OK", result);
    }

    @Test
    void reorderColumn_ShouldFail_IfInvalid() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        // Une colonne fixe (ne peut pas bouger)
        fr.uha.ensisa.gl.kanbin.projest.model.Column fixed = new fr.uha.ensisa.gl.kanbin.projest.model.Column("fix", "Fix");
        fixed.setId(99L);
        fixed.setFixed(true);
        realBoard.addColumn(fixed);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // Cas 1 : Index négatif
        String res1 = sut.reorderColumn(99L, -1);
        assertEquals("ERROR: Invalid move", res1);

        // Cas 2 : Colonne fixe
        String res2 = sut.reorderColumn(99L, 2);
        assertEquals("ERROR: Invalid move", res2);
    }

    @Test
    void editColumnForm_ShouldReturnView_WhenColumnExistsAndNotFixed() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // Création d'une colonne normale (non fixe)
        fr.uha.ensisa.gl.kanbin.projest.model.Column col = new fr.uha.ensisa.gl.kanbin.projest.model.Column("edit-me", "To Edit");
        col.setId(50L);
        col.setFixed(false); // Elle peut être éditée
        realBoard.addColumn(col);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // --- ACT ---
        ModelAndView mv = sut.editColumnForm(50L);

        // --- ASSERT ---
        // Vérifie qu'on va sur la page d'édition
        assertEquals("edit-column", mv.getViewName());

        // Vérifie qu'on a bien passé la bonne colonne à la vue
        fr.uha.ensisa.gl.kanbin.projest.model.Column modelCol = (fr.uha.ensisa.gl.kanbin.projest.model.Column) mv.getModel().get("column");
        assertEquals(50L, modelCol.getId());
    }

    @Test
    void editColumnForm_ShouldRedirect_WhenColumnIsFixed() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");

        // Création d'une colonne fixe (ex: Backlog)
        fr.uha.ensisa.gl.kanbin.projest.model.Column fixedCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("fixed", "Fixed");
        fixedCol.setId(60L);
        fixedCol.setFixed(true); // INTERDIT d'éditer
        realBoard.addColumn(fixedCol);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // --- ACT ---
        ModelAndView mv = sut.editColumnForm(60L);

        // --- ASSERT ---
        // Doit rediriger vers le board
        assertEquals("redirect:/board", mv.getViewName()); // Attention : ton code renvoie un ModelAndView avec cette string, pas un String direct
    }
    @Test
    void editColumnForm_ShouldRedirect_WhenColumnNotFound() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // --- ACT ---
        // On demande l'ID 999 qui n'est pas dans le board
        ModelAndView mv = sut.editColumnForm(999L);

        // --- ASSERT ---
        assertEquals("redirect:/board", mv.getViewName());
    }

    @Test
    void addColumn_ShouldFail_IfNameMatchesExistingSubColumn() {
        // ARRANGE
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        // 1. On crée une structure existante : Parent -> Enfant
        fr.uha.ensisa.gl.kanbin.projest.model.Column parent = new fr.uha.ensisa.gl.kanbin.projest.model.Column("parent", "Parent");
        // L'enfant s'appelle "hidden-target"
        fr.uha.ensisa.gl.kanbin.projest.model.Column sub = new fr.uha.ensisa.gl.kanbin.projest.model.Column("hidden-target", "Hidden Target");
        parent.addSubColumn(sub);

        realBoard.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        // ACT
        // 2. On essaie d'ajouter une nouvelle colonne principale qui s'appelle "Hidden Target"
        // Le système doit voir que ce nom est déjà pris par la sous-colonne !
        String view = sut.addColumn("Hidden Target", "simple", 0, redirectAttributes);

        // ASSERT
        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("déjà pris"));
        verify(boardRepo, never()).addColumn(anyLong(), any());
    }

    @Test
    void addColumn_ShouldIgnoreExistingColumnsWithNullKeys() {
        // ARRANGE
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        fr.uha.ensisa.gl.kanbin.projest.model.Column corruptedCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column(null, "No Key Column");
        realBoard.addColumn(corruptedCol);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        String view = sut.addColumn("New Normal", "simple", 0, redirectAttributes);

        // ASSERT
        // Ça doit passer (redirect sans erreur)
        assertEquals("redirect:/board", view);
        // Et l'ajout doit bien se faire
        verify(boardRepo).addColumn(eq(1L), any());
    }

    @Test
    void removeColumn_ShouldSucceed_WhenColumnIsValidAndEmpty() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        fr.uha.ensisa.gl.kanbin.projest.model.Column colToRemove = new fr.uha.ensisa.gl.kanbin.projest.model.Column("toremove", "To Remove");
        colToRemove.setId(50L);
        colToRemove.setFixed(false); // Important
        realBoard.addColumn(colToRemove);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        when(issueRepo.findAll()).thenReturn(List.of());
        String view = sut.removeColumn(50L, redirectAttributes);

        // --- ASSERT ---
        // 1. Vérifie la redirection standard (sans erreur)
        assertEquals("redirect:/board", view);
        verify(boardRepo).removeColumn(eq(1L), eq(50L));
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), any());
    }

    @Test
    void removeColumn_ShouldCoverYellowChecks_WithSubColumnsAndOrphanIssues() {
        // --- ARRANGE ---
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        fr.uha.ensisa.gl.kanbin.projest.model.Column parent = new fr.uha.ensisa.gl.kanbin.projest.model.Column("parent", "Parent");
        parent.setId(10L);
        parent.setType("double");

        fr.uha.ensisa.gl.kanbin.projest.model.Column sub = new fr.uha.ensisa.gl.kanbin.projest.model.Column("sub", "Sub-Col");
        parent.addSubColumn(sub);
        realBoard.addColumn(parent);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        Issue orphan = new Issue(1L, "Orphan");
        orphan.setColumnKey(null);
        Issue other = new Issue(2L, "Other");
        other.setColumnKey("somewhere-else");
        when(issueRepo.findAll()).thenReturn(List.of(orphan, other));
        sut.removeColumn(10L, redirectAttributes);
        verify(boardRepo).removeColumn(1L, 10L);
    }

    @Test
    void updateColumn_ShouldRedirect_WhenColumnNotFound() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        String view = sut.updateColumn(999L, "New Title", 0, redirectAttributes);
        assertEquals("redirect:/board", view);
    }
    @Test
    void updateColumn_ShouldRedirect_WhenColumnIsFixed() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);
        fr.uha.ensisa.gl.kanbin.projest.model.Column fixedCol = new fr.uha.ensisa.gl.kanbin.projest.model.Column("backlog", "Backlog");
        fixedCol.setId(50L);
        fixedCol.setFixed(true);
        realBoard.addColumn(fixedCol);
        when(boardRepo.findAll()).thenReturn(List.of(realBoard));
        String view = sut.updateColumn(50L, "New Name", 0, redirectAttributes);
        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
    }
    @Test
    void removeRow_shouldRedirect_whenRowKeyNotFoundOrNull() {
        Board board = new Board("Default");
        board.addRow(new Row("r1", "Row 1"));
        board.addRow(new Row("r2", "Row 2"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        // rowKey inexistant
        String view1 = sut.removeRow("missing", redirectAttributes);
        assertEquals("redirect:/board", view1);

        // rowKey null (couvre le prédicat rowKey != null)
        String view2 = sut.removeRow(null, redirectAttributes);
        assertEquals("redirect:/board", view2);

        verify(boardRepo, never()).save(any());
        verify(issueRepo, never()).findAll();
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), any());
    }

    @Test
    void removeRow_shouldBlockFixedRow() {
        Board board = new Board("Default");
        Row fixed = new Row("sys", "System");
        fixed.setFixed(true);
        board.addRow(fixed);
        board.addRow(new Row("r2", "Row 2"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String view = sut.removeRow("sys", redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("ne peut pas être supprimée"));
        verify(boardRepo, never()).save(any());
        verify(issueRepo, never()).findAll();
    }

    @Test
    void removeRow_shouldBlockIfItWouldLeaveNoRows() {
        Board board = new Board("Default");
        board.addRow(new Row("r1", "Only Row")); // non fixed
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String view = sut.removeRow("r1", redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("au moins une swimlane"));
        verify(boardRepo, never()).save(any());
        verify(issueRepo, never()).findAll();
    }

    @Test
    void removeRow_shouldBlockIfRowContainsIssues() {
        Board board = new Board("Default");
        board.addRow(new Row("r1", "Row 1"));
        board.addRow(new Row("r2", "Row 2"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        Issue issue = new Issue(1L, "Task");
        issue.setRowKey("r1");
        when(issueRepo.findAll()).thenReturn(List.of(issue));

        String view = sut.removeRow("r1", redirectAttributes);

        assertEquals("redirect:/board", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("contient des tâches"));
        verify(boardRepo, never()).save(any());
    }

    @Test
    void removeRow_shouldRemoveAndSave_whenAllowed() {
        Board board = new Board("Default");
        Row r1 = new Row("r1", "Row 1");
        Row r2 = new Row("r2", "Row 2");
        board.addRow(r1);
        board.addRow(r2);
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of()); // aucune issue dans la row

        String view = sut.removeRow("r1", redirectAttributes);

        assertEquals("redirect:/board", view);
        assertEquals(1, board.getRows().size());
        assertSame(r2, board.getRows().get(0));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_shouldMoveColumnAndSave_whenValid() {
        Board board = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setId(1L);
        backlog.setFixed(true);

        Column a = new Column("a", "A");
        a.setId(2L);

        Column b = new Column("b", "B");
        b.setId(3L);

        Column c = new Column("c", "C");
        c.setId(4L);

        Column closed = new Column("closed", "Closed");
        closed.setId(5L);
        closed.setFixed(true);

        board.addColumn(backlog);
        board.addColumn(a);
        board.addColumn(b);
        board.addColumn(c);
        board.addColumn(closed);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Déplacer C juste après backlog (branche newIndex < size)
        String res = sut.reorderColumn(4L, 1);

        assertEquals("OK", res);
        assertEquals(List.of("backlog", "c", "a", "b", "closed"),
                board.getColumns().stream().map(Column::getKey).collect(java.util.stream.Collectors.toList()));
        verify(boardRepo).save(board);
    }

    @Test
    void reorderColumn_shouldMoveColumnToEnd_whenNewIndexBeyondSize() {
        Board board = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setId(1L);
        backlog.setFixed(true);

        Column a = new Column("a", "A");
        a.setId(2L);

        Column b = new Column("b", "B");
        b.setId(3L);

        Column closed = new Column("closed", "Closed");
        closed.setId(4L);
        closed.setFixed(true);

        board.addColumn(backlog);
        board.addColumn(a);
        board.addColumn(b);
        board.addColumn(closed);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        // Déplacer A à la fin (branche newIndex >= size)
        String res = sut.reorderColumn(2L, 99);

        assertEquals("OK", res);
        assertEquals(List.of("backlog", "b", "closed", "a"),
                board.getColumns().stream().map(Column::getKey).collect(java.util.stream.Collectors.toList()));
        verify(boardRepo).save(board);
    }

    @Test
    void moveIssueDnD_shouldAllowMoveBetweenSubColumnsWithoutBreakingWipLimit() {
        Board board = new Board("Default");
        board.addRow(new Row("default", "Default"));

        Column dev = new Column("dev", "Dev");
        dev.setType("double");
        dev.setWipLimit(1); // limite à 1 story sur tout le groupe dev

        Column subTodo = new Column("dev-todo", "Dev Todo");
        Column subDone = new Column("dev-done", "Dev Done");
        Column subNull = new Column(null, "Corrupted Sub"); // couvre (sub.getKey() != null)

        dev.addSubColumn(subTodo);
        dev.addSubColumn(subDone);
        dev.addSubColumn(subNull);
        board.addColumn(dev);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        long issueId = 42L;
        Issue issue = new Issue(issueId, "Task");
        issue.setColumnKey("dev-todo");
        issue.setRowKey("default");

        when(issueRepo.find(issueId)).thenReturn(issue);
        when(issueRepo.findAll()).thenReturn(List.of(issue)); // currentCount = 1
        ResponseEntity<Map<String, Object>> res = sut.moveIssueDnD(issueId, "dev-done", null);

        assertTrue((Boolean) res.getBody().get("success"));
        assertEquals("dev-done", issue.getColumnKey());
        assertEquals("default", issue.getRowKey()); // n'a pas change
        verify(issueRepo).persist(issue);
    }
    @Test
    void moveIssueDnD_shouldReturnBadRequest_whenIssueNotFound() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("todo", "Todo"));

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(123L)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(123L, "todo", null);

        assertEquals(400, response.getStatusCodeValue());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("introuvable"));
        verify(issueRepo, never()).persist(any());
    }

    @Test
    void moveIssueDnD_shouldReturnBadRequest_whenTargetColumnNotFound() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("todo", "Todo"));

        Issue issue = new Issue(1L, "Task");

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(1L)).thenReturn(issue);

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(1L, "missing", null);

        assertEquals(400, response.getStatusCodeValue());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Colonne cible"));
        verify(issueRepo, never()).persist(any());
    }

    @Test
    void moveIssueDnD_shouldSetClosedAt_whenMovingToClosed() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("closed", "Closed"));

        Issue issue = new Issue(1L, "Task");
        issue.setClosedAt(null);

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(1L)).thenReturn(issue);

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(1L, "closed", null);

        assertTrue((Boolean) response.getBody().get("success"));
        assertNotNull(issue.getClosedAt());
        assertEquals("closed", issue.getColumnKey());
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldClearClosedAt_whenMovingOutOfClosed() {
        Board b = new Board("Test Board");
        b.addColumn(new Column("backlog", "Backlog"));

        Issue issue = new Issue(1L, "Task");
        issue.setColumnKey("closed");
        issue.setClosedAt(java.time.LocalDateTime.now());

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(1L)).thenReturn(issue);

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(1L, "backlog", null);

        assertTrue((Boolean) response.getBody().get("success"));
        assertNull(issue.getClosedAt());
        assertEquals("backlog", issue.getColumnKey());
        verify(issueRepo).persist(issue);
    }

    @Test
    void moveIssueDnD_shouldPassWipLimit_whenSpaceAvailable_andIssueNotAlreadyInLogicalColumn() {
        Board b = new Board("Test Board");
        b.addRow(new Row("default", "Default"));

        Column dev = new Column("dev", "Dev");
        dev.setType("double");
        dev.setWipLimit(2);

        Column subTodo = new Column("dev-todo", "Todo");
        Column subWip = new Column("dev-wip", "Wip");
        dev.addSubColumn(subTodo);
        dev.addSubColumn(subWip);
        b.addColumn(dev);

        Issue alreadyThere = new Issue(10L, "Existing");
        alreadyThere.setRowKey("default");
        alreadyThere.setColumnKey("dev-todo");

        Issue moving = new Issue(11L, "Moving");
        moving.setRowKey("default");
        moving.setColumnKey("backlog"); // pas dans dev-* => alreadyInTargetColumn=false

        when(boardRepo.findAll()).thenReturn(List.of(b));
        when(issueRepo.find(11L)).thenReturn(moving);
        when(issueRepo.findAll()).thenReturn(List.of(alreadyThere, moving));

        ResponseEntity<Map<String, Object>> response = sut.moveIssueDnD(11L, "dev-wip", null);

        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("dev-wip", moving.getColumnKey());
        verify(issueRepo).persist(moving);
    }

    @Test
    void showBoard_ShouldSortClosedAndPlaceNullClosedAtLast() {
        Board board = new Board("Test Board");
        board.addRow(new Row("def", "Def"));
        board.addColumn(new Column("closed", "Closed"));

        when(boardRepo.findAll()).thenReturn(List.of(board));

        Issue nullClosedAt = new Issue(1L, "Null");
        nullClosedAt.setRowKey("def");
        nullClosedAt.setColumnKey("closed");
        nullClosedAt.setClosedAt(null);

        Issue old = new Issue(2L, "Old");
        old.setRowKey("def");
        old.setColumnKey("closed");
        old.setClosedAt(java.time.LocalDateTime.now().minusDays(2));

        Issue recent = new Issue(3L, "Recent");
        recent.setRowKey("def");
        recent.setColumnKey("closed");
        recent.setClosedAt(java.time.LocalDateTime.now().minusHours(1));

        // ordre volontairement "mauvais" pour forcer les branches null/non-null du comparator
        when(issueRepo.findAll()).thenReturn(List.of(nullClosedAt, old, recent));

        ModelAndView mv = sut.showBoard();
        Map<String, Map<String, List<Issue>>> data = (Map) mv.getModel().get("issuesByRowAndCol");
        List<Issue> closedList = data.get("def").get("closed");

        assertEquals(3L, closedList.get(0).getId());
        assertEquals(2L, closedList.get(1).getId());
        assertEquals(1L, closedList.get(2).getId()); // null en dernier
    }

    @Test
    void updateColumn_ShouldSave_WhenValidAndTrimTitle_AndSetPositiveWipLimit() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        Column colToEdit = new Column("todo", "Todo");
        colToEdit.setId(10L);
        realBoard.addColumn(colToEdit);

        Column other = new Column("done", "Done");
        other.setId(20L);
        realBoard.addColumn(other);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        String view = sut.updateColumn(10L, "  New Title  ", 3, redirectAttributes);

        assertEquals("redirect:/board", view);
        assertEquals("New Title", colToEdit.getTitle());
        assertEquals(3, colToEdit.getWipLimit());
        verify(boardRepo).save(realBoard);
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), any());
    }

    @Test
    void updateColumn_ShouldSave_WhenValidAndSetWipLimitToZero_WhenNullOrNonPositive() {
        Board realBoard = new Board("Test Board");
        realBoard.setId(1L);

        Column colToEdit = new Column("todo", "Todo");
        colToEdit.setId(10L);
        colToEdit.setWipLimit(5);
        realBoard.addColumn(colToEdit);

        when(boardRepo.findAll()).thenReturn(List.of(realBoard));

        String view1 = sut.updateColumn(10L, "X", null, redirectAttributes);
        assertEquals("redirect:/board", view1);
        assertEquals(0, colToEdit.getWipLimit());

        String view2 = sut.updateColumn(10L, "Y", 0, redirectAttributes);
        assertEquals("redirect:/board", view2);
        assertEquals(0, colToEdit.getWipLimit());

        verify(boardRepo, atLeastOnce()).save(realBoard);
    }

    @Test
    void resolveWipLimit_shouldReturnTargetLimit_whenColumnNotInBoard() throws Exception {
        Board b = new Board("B");
        Column in = new Column("in", "In");
        in.setWipLimit(7);
        b.addColumn(in);

        Column external = new Column("ext", "Ext");
        external.setWipLimit(13);

        java.lang.reflect.Method m = BoardController.class.getDeclaredMethod(
                "resolveWipLimit", Board.class, Column.class);
        m.setAccessible(true);

        int limit = (int) m.invoke(sut, b, external);
        assertEquals(13, limit);
    }

    @Test
    void collectLogicalColumnKeys_shouldCoverMainNullAndNullKeys() throws Exception {
        Board b = new Board("B");

        Column corruptedInBoard = new Column(null, "Corrupted");
        b.addColumn(corruptedInBoard);

        Column external = new Column("ext", "Ext");
        Column externalNullKey = new Column(null, "NoKey");

        java.lang.reflect.Method m = BoardController.class.getDeclaredMethod(
                "collectLogicalColumnKeys", Board.class, Column.class);
        m.setAccessible(true);

        List<String> keys1 = (List<String>) m.invoke(sut, b, corruptedInBoard);
        assertTrue(keys1.isEmpty());

        List<String> keys2 = (List<String>) m.invoke(sut, b, external);
        assertEquals(List.of("ext"), keys2);

        List<String> keys3 = (List<String>) m.invoke(sut, b, externalNullKey);
        assertTrue(keys3.isEmpty());
    }
    @Test
    void getOrCreateDefaultBoard_shouldCreateDefaults_whenFindAllReturnsNull_andSaveReturnsNull() {
        when(boardRepo.findAll()).thenReturn(null);
        when(boardRepo.save(any(Board.class))).thenReturn(null);

        ModelAndView mv = sut.showBoard();

        assertEquals("board", mv.getViewName());
        Board created = (Board) mv.getModel().get("board");
        assertNotNull(created);
        assertEquals("Default", created.getName());
        assertTrue(created.getColumns().stream().anyMatch(c -> "backlog".equals(c.getKey()) && c.isFixed()));
        assertTrue(created.getColumns().stream().anyMatch(c -> "closed".equals(c.getKey()) && c.isFixed()));
        assertFalse(created.getRows().isEmpty());
        assertEquals("default", created.getRows().get(0).getKey());

        verify(boardRepo).save(any(Board.class));
    }

    @Test
    void getOrCreateDefaultBoard_shouldPreferBoardNamedDefault_whenPresent() {
        Board other = new Board("Other");
        other.addRow(new Row("rOther", "Other Row"));
        other.addColumn(new Column("backlog", "Backlog"));

        Board def = new Board("Default");
        def.addRow(new Row("rDef", "Default Row"));
        def.addColumn(new Column("backlog", "Backlog"));

        when(boardRepo.findAll()).thenReturn(List.of(other, def));
        when(issueRepo.findAll()).thenReturn(List.of());

        ModelAndView mv = sut.showBoard();
        assertSame(def, mv.getModel().get("board"));
    }

    @Test
    void reorderColumn_shouldReturnError_whenColumnIdNotFound() {
        Board board = new Board("Default");
        Column backlog = new Column("backlog", "Backlog");
        backlog.setFixed(true);
        backlog.setId(1L);
        board.addColumn(backlog);
        board.addColumn(new Column("a", "A"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String res = sut.reorderColumn(999L, 1);
        assertEquals("ERROR: Invalid move", res);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void showBoard_shouldNotFixRowKey_whenBoardHasNoRows() {
        Board board = new Board("Default");
        board.addColumn(new Column("backlog", "Backlog"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        Issue orphan = new Issue(1L, "Orphan");
        orphan.setRowKey(null);
        orphan.setColumnKey(null);
        when(issueRepo.findAll()).thenReturn(List.of(orphan));

        ModelAndView mv = sut.showBoard();

        assertEquals("backlog", orphan.getColumnKey());
        assertNull(orphan.getRowKey());
        verify(issueRepo).persist(orphan);

        Map<String, Map<String, List<Issue>>> data = (Map) mv.getModel().get("issuesByRowAndCol");
        assertTrue(data.isEmpty());
    }

    @Test
    void showBoard_shouldIgnoreIssue_whenRowExistsButColumnKeyUnknown() {
        Board board = new Board("Default");
        board.addRow(new Row("r1", "Row 1"));
        board.addColumn(new Column("backlog", "Backlog"));
        when(boardRepo.findAll()).thenReturn(List.of(board));

        Issue ghost = new Issue(1L, "Ghost");
        ghost.setRowKey("r1");
        ghost.setColumnKey("ghost");
        when(issueRepo.findAll()).thenReturn(List.of(ghost));

        ModelAndView mv = sut.showBoard();

        Map<String, Map<String, List<Issue>>> data = (Map) mv.getModel().get("issuesByRowAndCol");
        assertNotNull(data.get("r1"));
        assertTrue(data.get("r1").get("backlog").isEmpty());
        verify(issueRepo, never()).persist(any());
    }

    @Test
    void showBoard_shouldCountZeroWip_whenColumnKeyIsNull() {
        Board board = new Board("Default");
        board.addRow(new Row("r1", "Row 1"));
        Column corrupted = new Column(null, "Corrupted");
        corrupted.setId(10L);
        board.addColumn(corrupted);

        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of(new Issue(1L, "X")));

        ModelAndView mv = sut.showBoard();

        Map<Long, Long> wip = (Map<Long, Long>) mv.getModel().get("wipUsageByColumnId");
        assertEquals(0L, wip.get(10L));
    }

    @Test
    void removeRow_shouldNotSave_whenRemoveRowByKeyReturnsFalse() {
        Board board = spy(new Board("Default"));
        board.addRow(new Row("r1", "Row 1"));
        board.addRow(new Row("r2", "Row 2"));
        when(boardRepo.findAll()).thenReturn(List.of(board));
        when(issueRepo.findAll()).thenReturn(List.of());

        doReturn(false).when(board).removeRowByKey("r1");

        String view = sut.removeRow("r1", redirectAttributes);
        assertEquals("redirect:/board", view);
        verify(boardRepo, never()).save(any());
    }

    @Test
    void updateColumn_shouldIgnoreNullTitlesInDuplicateCheck() {
        Board board = new Board("Default");
        board.setId(1L);

        Column toEdit = new Column("a", "A");
        toEdit.setId(10L);
        board.addColumn(toEdit);

        Column other = new Column("b", null);
        other.setId(20L);
        board.addColumn(other);

        when(boardRepo.findAll()).thenReturn(List.of(board));

        String view = sut.updateColumn(10L, "New", 0, redirectAttributes);
        assertEquals("redirect:/board", view);
        verify(boardRepo).save(board);
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), any());
    }

    @Test
    void addColumn_shouldIgnoreExistingSubColumnsWithNullKeys() {
        Board board = new Board("Default");
        board.setId(1L);

        Column parent = new Column("parent", "Parent");
        parent.addSubColumn(new Column(null, "Broken Sub"));
        board.addColumn(parent);
        when(boardRepo.findAll()).thenReturn(List.of(board));

        String view = sut.addColumn("New Col", "simple", 0, redirectAttributes);
        assertEquals("redirect:/board", view);
        verify(boardRepo).addColumn(eq(1L), any(Column.class));
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), any());
    }
}