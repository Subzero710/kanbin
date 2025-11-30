package fr.uha.ensisa.gl.kanbin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        // prendre le premier board existant, sinon en créer un
        Board b = getOrCreateDefaultBoard();
        var mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());

        // préparer les issues groupées par colonne

        Map<String, List<Issue>> issuesByColumn = new LinkedHashMap<>();
        String defaultKey = null;
        for (Column mainCol : b.getColumns()) {
            if (!mainCol.getSubColumns().isEmpty()) {
                for (Column sub : mainCol.getSubColumns()) {
                    issuesByColumn.put(sub.getKey(), new ArrayList<>());
                    // Si c'est la toute première, c'est la par défaut
                    if (defaultKey == null) defaultKey = sub.getKey();
                }
            } else {
                // Au cas où une colonne n'aurait pas de sous-colonnes (sécurité)
                issuesByColumn.put(mainCol.getKey(), new ArrayList<>());
                if (defaultKey == null) defaultKey = mainCol.getKey();
            }
        }

        // On range les issues dans les boîtes
        for (Issue issue : issues.findAll()) {
            String key = issue.getColumnKey();
            // Si l'issue est nouvelle (key null) ou sa colonne n'existe plus -> Hop, case départ !
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

        // 1. Créer la colonne Principale (Le conteneur)
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
        // 1. APLATIR LA LISTE : On met toutes les sous-colonnes à la suite
        // Résultat : [Dev-Todo, Dev-Wip, Test-Todo, Test-Wip...]
        List<Column> flatList = new ArrayList<>();
        for (Column mainCol : board.getColumns()) {
            if (!mainCol.getSubColumns().isEmpty()) {
                flatList.addAll(mainCol.getSubColumns());
            } else {
                flatList.add(mainCol);
            }
        }

        if (flatList.isEmpty()) return "redirect:/board";

        // 2. Trouver où est l'issue actuellement
        Issue issue = issues.find(issueId);
        if (issue == null) return "redirect:/board";

        int currentIndex = -1;
        for (int i = 0; i < flatList.size(); i++) {
            if (flatList.get(i).getKey().equals(issue.getColumnKey())) {
                currentIndex = i;
                break;
            }
        }

        // 3. Calculer la destination (+1 ou -1)
        if (currentIndex != -1) {
            int newIndex = currentIndex; // On garde une copie pour comparer après

            if ("prev".equals(direction) && currentIndex > 0) {
                newIndex--;
            } else if ("next".equals(direction) && currentIndex < flatList.size() - 1) {
                newIndex++;
            }

            // 4. Sauvegarder le déplacement UNIQUEMENT si ça a bougé
            // C'est cette condition qui va faire passer ton test au vert !
            if (newIndex != currentIndex) {
                issue.setColumnKey(flatList.get(newIndex).getKey());
                issues.persist(issue);
            }
        }

        return "redirect:/board";
    }
}