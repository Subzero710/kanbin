package fr.uha.ensisa.gl.kanbin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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
                // Au cas où une ancienne colonne traîne sans sous-colonne
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
                issuesByColumn.get(key).add(issue);
            }
        }

        mv.addObject("issuesByColumn", issuesByColumn);
        return mv;
    }

    @PostMapping("/board/add-column")
    public String addColumn(@RequestParam("title") String title) {
        Board board = getOrCreateDefaultBoard();

        // 1. Créer la colonne Principale
        String mainKey = title.toLowerCase().replaceAll("\\s+", "-");
        Column mainColumn = new Column(mainKey, title);

        // 2. Créer automatiquement les 2 sous-colonnes
        Column subTodo = new Column(mainKey + "-todo", "À Faire");
        Column subWip  = new Column(mainKey + "-wip",  "En Cours");

        // 3. Relier les enfants au parent
        mainColumn.addSubColumn(subTodo);
        mainColumn.addSubColumn(subWip);

        // 4. Ajouter le parent au board
        boards.addColumn(board.getId(), mainColumn);

        return "redirect:/board";
    }

    @PostMapping("/board/remove-column")
    public String removeColumn(@RequestParam("columnId") long columnId) {
        Board board = getOrCreateDefaultBoard();
        board.removeColumn(columnId);
        boards.save(board);
        return "redirect:/board";
    }

    @PostMapping("/board/move-issue")
    public String moveIssue(@RequestParam("issueId") long issueId,
                            @RequestParam("direction") String direction) {
        Board board = getOrCreateDefaultBoard();

        // 1. APLATIR LA LISTE pour naviguer linéairement entre sous-colonnes
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
        if (colOpt.isEmpty()) {
            return new ModelAndView("redirect:/board");
        }
        ModelAndView mv = new ModelAndView("edit-column");
        mv.addObject("column", colOpt.get());
        return mv;
    }

    @PostMapping("/board/columns/{id}")
    public String updateColumn(@PathVariable long id, @RequestParam("title") String title) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id)
                .findFirst();
        if (colOpt.isPresent()) {
            Column c = colOpt.get();
            c.setTitle(title);
            boards.save(board);
        }
        return "redirect:/board";
    }
}