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

    @GetMapping("/board")
    public ModelAndView board() {
        // prendre le premier board existant, sinon en créer un
        Board b = getOrCreateDefaultBoard();
        var mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());

        // préparer les issues groupées par colonne
        List<Column> columns = b.getColumns();
        Map<String, List<Issue>> issuesByColumn = new LinkedHashMap<>();
        for (Column c : columns) {
            issuesByColumn.put(c.getKey(), new ArrayList<>());
        }

        String defaultKey = columns.isEmpty() ? null : columns.get(0).getKey();

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
    @RequestMapping(value="/")
    public String home(){
        return "redirect:/hello";
    }

    @RequestMapping(value="/hello")
    public ModelAndView hello(@RequestParam(required=false, defaultValue="World") String name) {
        ModelAndView ret = new ModelAndView("home");

        ret.addObject("name", name);
        return ret;
    }

    @PostMapping("/board/add-column")
    public String addColumn(@RequestParam("title") String title) {

        Board board = getOrCreateDefaultBoard();

        String key = title.toLowerCase().replaceAll("\\s+", "-");

        Column newColumn = new Column(key, title);

        boards.addColumn(board.getId(), newColumn);

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
        List<Column> columns = board.getColumns();
        if (columns.isEmpty()) {
            return "redirect:/board";
        }

        Issue issue = issues.find(issueId);
        if (issue == null) {
            return "redirect:/board";
        }

        String currentKey = issue.getColumnKey();
        int currentIndex = 0;

        if (currentKey != null) {
            for (int i = 0; i < columns.size(); i++) {
                if (currentKey.equals(columns.get(i).getKey())) {
                    currentIndex = i;
                    break;
                }
            }
        }

        if ("prev".equals(direction) && currentIndex > 0) {
            currentIndex--;
        } else if ("next".equals(direction) && currentIndex < columns.size() - 1) {
            currentIndex++;
        } else {
            // déjà au bord, rien à faire
            return "redirect:/board";
        }

        issue.setColumnKey(columns.get(currentIndex).getKey());
        issues.persist(issue);

        return "redirect:/board";
    }
}