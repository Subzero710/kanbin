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

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                // Utilisation de addFirst() (Java 21) pour mettre les plus récents en haut
                issuesByColumn.get(key).addFirst(issue);
            }
        }

        mv.addObject("issuesByColumn", issuesByColumn);
        return mv;
    }

    @PostMapping("/board/add-column") // Note : J'ai gardé ton URL "/board/add-column"
    public String addColumn(@RequestParam("title") String title,
                            @RequestParam(value = "type", defaultValue = "simple") String type) {

        Board board = getOrCreateDefaultBoard();

        String key = title.toLowerCase().trim().replaceAll("\\s+", "-");
        Column newColumn = new Column(key, title);

        if ("double".equalsIgnoreCase(type)) {
            // Création des 2 sous-colonnes (comme avant)
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

        // On retrouve la colonne racine à supprimer
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == columnId)
                .findFirst();
        if (colOpt.isEmpty()) {
            return "redirect:/board";
        }
        Column column = colOpt.get();

        // Clés de la colonne + de ses sous-colonnes
        List<String> keys = new ArrayList<>();
        if (column.getKey() != null) {
            keys.add(column.getKey());
        }
        for (Column sub : column.getSubColumns()) {
            if (sub.getKey() != null) {
                keys.add(sub.getKey());
            }
        }

        // Vérifier s'il existe des Issue dans ces colonnes
        boolean hasIssues = issues.findAll().stream()
                .anyMatch(i -> i.getColumnKey() != null
                        && keys.contains(i.getColumnKey()));

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

        // On cherche la colonne principale par id
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst();

        if (colOpt.isEmpty()) {
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

        board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(c -> c.setTitle(title));

        // Important : persister le board après la modification
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
            boards.save(board); // On sauvegarde uniquement si le mouvement est valide
            return "OK";
        } else {
            return "ERROR: Invalid move";
        }
    }

    private boolean moveColumnInternal(Board board, long columnId, int newIndex) {
        // Interdit de placer en position 0 (Réservé)
        if (newIndex <= 0) {
            return false;
        }

        List<Column> columns = board.getColumns();
        int oldIndex = -1;
        Column columnToMove = null;

        // Recherche de la colonne
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getId() == columnId) {
                columnToMove = columns.get(i);
                oldIndex = i;
                break;
            }
        }
        if (columnToMove == null || oldIndex == 0) {
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