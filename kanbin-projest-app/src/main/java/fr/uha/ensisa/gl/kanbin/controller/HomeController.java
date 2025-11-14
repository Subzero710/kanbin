package fr.uha.ensisa.gl.kanbin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {
    private final BoardRepo boards;

    public HomeController(BoardRepo boards) {
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
        // Adds an objet to be used in home.jsp
        ret.addObject("name", name);
        return ret;
    }

    @PostMapping("/board/add-column")
    public String addColumn(@RequestParam("title") String title) {
        // Trouver le Board actif (le premier créé)
        Board board = boards.findAll().stream().findFirst()
                .orElseGet(() -> boards.save(new Board("Default")));

        // 2. Créer l'objet Column.
        String key = title.toLowerCase().replaceAll("\\s+", "-");

        // Le constructeur (key, title) est utilisé. Le BoardRepoMem attribuera l'ID (id=0 initial)
        Column newColumn = new Column(key, title);

        // 3. Appeler la méthode métier du repository pour ajouter et sauvegarder
        // Le Dev 1 a inclus cette méthode directement dans son BoardRepoMem.
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
