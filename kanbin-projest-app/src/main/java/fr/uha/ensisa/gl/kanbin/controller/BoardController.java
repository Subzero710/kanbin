package fr.uha.ensisa.gl.kanbin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;

@Controller
public class BoardController {
    private final BoardRepo boards;

    public BoardController(BoardRepo boards) {
        this.boards = boards;
    }

    @GetMapping("/board")
    public ModelAndView board() {
        // prendre le premier board existant, sinon en créer un
        Board b = boards.findAll().stream().findFirst()
                .orElseGet(() -> boards.save(new Board("Default")));
        var mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());
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

        Board board = boards.findAll().stream().findFirst()
                .orElseGet(() -> boards.save(new Board("Default")));


        String key = title.toLowerCase().replaceAll("\\s+", "-");


        Column newColumn = new Column(key, title);

        boards.addColumn(board.getId(), newColumn);

        return "redirect:/board";
    }

    @PostMapping("/board/remove-column")
    public String removeColumn(@RequestParam("columnId") long columnId) {
        Board board = boards.findAll().stream().findFirst()
                .orElseGet(() -> boards.save(new Board("Default")));
        board.removeColumn(columnId);
        boards.save(board);
        return "redirect:/board";
    }
}