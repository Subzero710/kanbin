package fr.uha.ensisa.gl.kanbin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class BoardController {

    private final BoardRepo boards;
    private final IssueRepo issues;

    public BoardController(BoardRepo boards, IssueRepo issues) {
        this.boards = boards;
        this.issues = issues;
    }

    private Board getOrCreateDefaultBoard() {
        String boardName = "Test Board";

        return boards.findAll().stream()
                .filter(b -> boardName.equals(b.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Board newBoard = new Board(boardName);
                    Column backlog = new Column("backlog", "Backlog");
                    backlog.setFixed(true);
                    Column closed = new Column("closed", "Closed");
                    closed.setFixed(true);
                    newBoard.addColumn(backlog);
                    newBoard.addColumn(closed);
                    return boards.save(newBoard);
                });

    }

    @GetMapping("/")
    public String homeRedirect() {
        return "redirect:/board";
    }

    @GetMapping("/board")
    public ModelAndView board() {
        Board b = getOrCreateDefaultBoard();
        ModelAndView mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());

        // Prépare la structure issues par colonne (sub-colonnes incluses)
        Map<String, List<Issue>> issuesByColumn = new LinkedHashMap<>();
        String defaultKey = null;

        for (Column mainCol : b.getColumns()) {
            if (!mainCol.getSubColumns().isEmpty()) {
                for (Column sub : mainCol.getSubColumns()) {
                    issuesByColumn.put(sub.getKey(), new ArrayList<>());
                    if (defaultKey == null) defaultKey = sub.getKey();
                }
            } else {
                issuesByColumn.put(mainCol.getKey(), new ArrayList<>());
                if (defaultKey == null) defaultKey = mainCol.getKey();
            }
        }

        for (Issue issue : issues.findAll()) {
            String key = issue.getColumnKey();
            if (key == null || !issuesByColumn.containsKey(key)) {
                key = defaultKey;
                if (key != null) {
                    issue.setColumnKey(key);
                    issues.persist(issue);
                }
            }
            if (key != null) {
                issuesByColumn.get(key).addFirst(issue);
            }
        }

        mv.addObject("issuesByColumn", issuesByColumn);

        // Prépare les informations WIP pour chaque colonne principale
        Map<Long, Long> wipUsageByColumnId = new HashMap<>();
        for (Column mainCol : b.getColumns()) {
            long count = countIssuesInColumn(b, mainCol);
            wipUsageByColumnId.put(mainCol.getId(), count);
        }
        mv.addObject("wipUsageByColumnId", wipUsageByColumnId);

        return mv;
    }

    @PostMapping("/board/add-column")
    public String addColumn(@RequestParam("title") String title,
                            @RequestParam(value = "type", defaultValue = "simple") String type) {

        Board board = getOrCreateDefaultBoard();
        String key = title.toLowerCase().trim().replaceAll("\\s+", "-");
        Column newColumn = new Column(key, title);

        if ("double".equalsIgnoreCase(type)) {
            Column subTodo = new Column(key + "-todo", "À Faire");
            Column subWip  = new Column(key + "-wip",  "En Cours");
            newColumn.addSubColumn(subTodo);
            newColumn.addSubColumn(subWip);
        }

        boards.addColumn(board.getId(), newColumn);
        return "redirect:/board";
    }

    @PostMapping("/board/remove-column")
    public String removeColumn(@RequestParam("columnId") long columnId,
                               RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();

        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == columnId)
                .findFirst();

        if (colOpt.isEmpty()) {
            return "redirect:/board";
        }

        Column column = colOpt.get();

        // SECURITÉ : COLONNE FIXE
        if (column.isFixed()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Action interdite : Cette colonne système ne peut pas être supprimée.");
            return "redirect:/board";
        }

        // SECURITÉ : COLONNE NON VIDE
        List<String> keys = new ArrayList<>();
        if (column.getKey() != null) keys.add(column.getKey());
        for (Column sub : column.getSubColumns()) {
            if (sub.getKey() != null) keys.add(sub.getKey());
        }

        boolean hasIssues = issues.findAll().stream()
                .anyMatch(i -> i.getColumnKey() != null && keys.contains(i.getColumnKey()));

        if (hasIssues) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Impossible de supprimer une colonne non vide");
            return "redirect:/board";
        }

        board.removeColumn(columnId);
        boards.save(board);
        return "redirect:/board";
    }


    @GetMapping("/board/columns/{id}/edit")
    public ModelAndView editColumnForm(@PathVariable long id) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id).findFirst();

        if (colOpt.isEmpty()) return new ModelAndView("redirect:/board");

        if (colOpt.get().isFixed()) {
            return new ModelAndView("redirect:/board");
        }

        ModelAndView mv = new ModelAndView("edit-column");
        mv.addObject("column", colOpt.get());
        return mv;
    }

    @PostMapping("/board/columns/{id}")
    public String updateColumn(@PathVariable long id,
                               @RequestParam("title") String title,
                               @RequestParam(value = "wipLimit", required = false) Integer wipLimit) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst();

        // Colonne introuvable → on ne fait rien
        if (colOpt.isEmpty()) {
            return "redirect:/board";
        }

        Column column = colOpt.get();

        // Colonne fixe (Backlog, etc.) → on ne la modifie pas
        if (column.isFixed()) {
            return "redirect:/board";
        }

        // On met à jour le titre
        column.setTitle(title);

        // wipLimit nul ou <= 0 => pas de limite (stocké comme 0)
        int finalLimit = (wipLimit != null && wipLimit > 0) ? wipLimit : 0;
        column.setWipLimit(finalLimit);

        boards.save(board);
        return "redirect:/board";
    }

    @PostMapping("/board/reorder-column")
    @ResponseBody
    public String reorderColumn(@RequestParam("columnId") long columnId,
                                @RequestParam("newIndex") int newIndex) {
        Board board = getOrCreateDefaultBoard();
        boolean success = moveColumnInternal(board, columnId, newIndex);
        if (success) {
            boards.save(board);
            return "OK";
        } else {
            return "ERROR: Invalid move";
        }
    }

    private boolean moveColumnInternal(Board board, long columnId, int newIndex) {
        if (newIndex <= 0) return false;

        List<Column> columns = board.getColumns();
        int oldIndex = -1;
        Column columnToMove = null;

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getId() == columnId) {
                columnToMove = columns.get(i);
                oldIndex = i;
                break;
            }
        }

        if (columnToMove == null) return false;
        if (columnToMove.isFixed()) return false;
        columns.remove(oldIndex);
        if (newIndex >= columns.size()) {
            columns.add(columnToMove);
        } else {
            columns.add(newIndex, columnToMove);
        }
        return true;
    }

    /**
     * Colonne principale logique pour une colonne donnée (elle-même ou son parent).
     */
    private Column findMainColumn(Board board, Column column) {
        for (Column main : board.getColumns()) {
            if (main == column) {
                return main;
            }
            if (main.getSubColumns().contains(column)) {
                return main;
            }
        }
        return null;
    }

    /**
     * Clés de colonnes (sub-colonnes) qui appartiennent à la même colonne logique.
     */
    private List<String> collectLogicalColumnKeys(Board board, Column column) {
        List<String> keys = new ArrayList<>();

        Column main = findMainColumn(board, column);
        if (main != null) {
            if (!main.getSubColumns().isEmpty()) {
                for (Column sub : main.getSubColumns()) {
                    if (sub.getKey() != null) {
                        keys.add(sub.getKey());
                    }
                }
            } else if (main.getKey() != null) {
                keys.add(main.getKey());
            }
        } else if (column.getKey() != null) {
            // Colonne orpheline (ancien board)
            keys.add(column.getKey());
        }

        return keys;
    }

    /**
     * Nombre de stories présentes dans une colonne logique (principale + sous-colonnes).
     */
    private long countIssuesInColumn(Board board, Column anyColumn) {
        List<String> keys = collectLogicalColumnKeys(board, anyColumn);
        if (keys.isEmpty()) {
            return 0L;
        }

        long count = 0L;
        for (Issue i : issues.findAll()) {
            String key = i.getColumnKey();
            if (key != null && keys.contains(key)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Limite WIP associée à la colonne logique.
     * Elle est stockée sur la colonne principale du board.
     */
    private Integer resolveWipLimit(Board board, Column targetCol) {
        Column main = findMainColumn(board, targetCol);
        if (main != null) {
            return main.getWipLimit();
        }
        return targetCol.getWipLimit();
    }

    /**
     * Indique si l'issue est déjà dans cette colonne logique (pour ne pas la compter deux fois).
     */
    private boolean isIssueAlreadyInLogicalColumn(Board board, Column targetCol, Issue issue) {
        List<String> keys = collectLogicalColumnKeys(board, targetCol);
        String currentKey = issue.getColumnKey();
        return currentKey != null && keys.contains(currentKey);
    }

    @PostMapping("/board/move-issue-dnd")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveIssueDnD(@RequestParam("issueId") long issueId,
                                                            @RequestParam("targetColumnKey") String targetColumnKey) {

        Map<String, Object> response = new HashMap<>();
        Board board = getOrCreateDefaultBoard();
        Issue issue = issues.find(issueId);

        if (issue == null) {
            response.put("success", false);
            response.put("message", "Story introuvable.");
            return ResponseEntity.badRequest().body(response);
        }

        // Trouver la colonne cible (parcours du board pour la trouver par sa clé)
        Column targetCol = null;
        for (Column col : board.getColumns()) {
            if (targetColumnKey.equals(col.getKey())) { targetCol = col; break; }
            for (Column sub : col.getSubColumns()) {
                if (targetColumnKey.equals(sub.getKey())) { targetCol = sub; break; }
            }
        }

        if (targetCol == null) {
            response.put("success", false);
            response.put("message", "Colonne cible introuvable.");
            return ResponseEntity.badRequest().body(response);
        }

        int wipLimit = resolveWipLimit(board, targetCol);
        if (wipLimit > 0) {
            long currentCount = countIssuesInColumn(board, targetCol);
            boolean alreadyInTargetColumn = isIssueAlreadyInLogicalColumn(board, targetCol, issue);

            long afterMove = currentCount + (alreadyInTargetColumn ? 0L : 1L);

            if (afterMove > wipLimit) {
                Column main = findMainColumn(board, targetCol);
                String colTitle = (main != null ? main.getTitle() : targetCol.getTitle());

                response.put("success", false);
                response.put("message", "Limite atteinte (" + wipLimit + ") pour la colonne \"" + colTitle + "\".");
                // On retourne OK (200) mais avec success=false pour gestion JS
                return ResponseEntity.ok(response);
            }
        }

        // Succès
        issue.setColumnKey(targetColumnKey);
        issues.persist(issue);

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}