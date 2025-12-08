package fr.uha.ensisa.gl.kanbin.controller;

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
        return boards.findAll().stream().findFirst()
                .orElseGet(() -> boards.save(new Board("Default")));
    }

    @GetMapping("/")
    public String homeRedirect() {
        return "redirect:/board";
    }

    @GetMapping("/board")
    public ModelAndView board() {
        Board b = getOrCreateDefaultBoard();
        var mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());

        // On prépare les boîtes pour les sous-colonnes
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

        // On range les issues
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
                // Feature #55 : Les stories récentes en haut (addFirst)
                issuesByColumn.get(key).addFirst(issue);
            }
        }

        mv.addObject("issuesByColumn", issuesByColumn);
        return mv;
    }

    @PostMapping("/board/add-column")
    public String addColumn(@RequestParam("title") String title) {
        Board board = getOrCreateDefaultBoard();

        String mainKey = title.toLowerCase().replaceAll("\\s+", "-");
        Column mainColumn = new Column(mainKey, title);

        Column subTodo = new Column(mainKey + "-todo", "À Faire");
        Column subWip  = new Column(mainKey + "-wip",  "En Cours");

        mainColumn.addSubColumn(subTodo);
        mainColumn.addSubColumn(subWip);

        boards.addColumn(board.getId(), mainColumn);

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

        // --- SECURITÉ : COLONNE FIXE ---
        if (column.isFixed()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Action interdite : Cette colonne système ne peut pas être supprimée.");
            return "redirect:/board";
        }

        // --- SECURITÉ : COLONNE NON VIDE ---
        List<String> keys = new ArrayList<>();
        if (column.getKey() != null) keys.add(column.getKey());
        for (Column sub : column.getSubColumns()) {
            if (sub.getKey() != null) keys.add(sub.getKey());
        }

        boolean hasIssues = issues.findAll().stream()
                .anyMatch(i -> i.getColumnKey() != null && keys.contains(i.getColumnKey()));

        if (hasIssues) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Impossible de supprimer une colonne non vide"
            );
            return "redirect:/board";
        }

        board.removeColumn(columnId);
        boards.save(board);
        return "redirect:/board";
    }


    @PostMapping("/board/move-issue")
    public String moveIssue(@RequestParam("issueId") long issueId,
                            @RequestParam("direction") String direction) {
        Board board = getOrCreateDefaultBoard();
        List<Column> flatList = new ArrayList<>();
        for (Column mainCol : board.getColumns()) {
            if (!mainCol.getSubColumns().isEmpty()) {
                flatList.addAll(mainCol.getSubColumns());
            } else {
                flatList.add(mainCol);
            }
        }

        if (flatList.isEmpty()) return "redirect:/board";

        Issue issue = issues.find(issueId);
        if (issue == null) return "redirect:/board";

        int currentIndex = -1;
        for (int i = 0; i < flatList.size(); i++) {
            if (flatList.get(i).getKey().equals(issue.getColumnKey())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            int newIndex = currentIndex;
            if ("prev".equals(direction) && currentIndex > 0) {
                newIndex--;
            } else if ("next".equals(direction) && currentIndex < flatList.size() - 1) {
                newIndex++;
            }
            if (newIndex != currentIndex) {
                issue.setColumnKey(flatList.get(newIndex).getKey());
                issues.persist(issue);
            }
        }
        return "redirect:/board";
    }

    @GetMapping("/board/columns/{id}/edit")
    public ModelAndView editColumnForm(@PathVariable long id) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst();

        if (colOpt.isEmpty()) return new ModelAndView("redirect:/board");

        // --- SECURITÉ : PAS D'ÉDITION SUR COLONNE FIXE ---
        if (colOpt.get().isFixed()) {
            return new ModelAndView("redirect:/board");
        }

        ModelAndView mv = new ModelAndView("edit-column");
        mv.addObject("column", colOpt.get());
        return mv;
    }

    @PostMapping("/board/columns/{id}")
    public String updateColumn(@PathVariable long id,
                               @RequestParam("title") String title) {
        Board board = getOrCreateDefaultBoard();

        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst();

        // --- SECURITÉ : PAS DE RENOMMAGE SUR COLONNE FIXE ---
        if (colOpt.isPresent() && !colOpt.get().isFixed()) {
            colOpt.get().setTitle(title);
            boards.save(board);
        }

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
        if (newIndex < 0) return false;

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

        // --- SECURITÉ : COLONNE FIXE IMMOBILE ---
        if (columnToMove.isFixed()) {
            return false;
        }

        // --- SECURITÉ : NE PAS VOLER LA PLACE D'UNE COLONNE FIXE ---
        // Empêche de se mettre en position 0 si la pos 0 est une colonne fixe
        if (newIndex < columns.size() && columns.get(newIndex).isFixed()) {
            return false;
        }

        columns.remove(oldIndex);
        if (newIndex >= columns.size()) {
            columns.add(columnToMove);
        } else {
            columns.add(newIndex, columnToMove);
        }
        return true;
    }
}