package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.util.List;

@Controller
public class HomeController {

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
    @GetMapping("/board")
    public ModelAndView board() {
        var mv = new ModelAndView("board");
        List<Column> columns = List.of(
                new Column("todo",    "To Do"),
                new Column("ongoing", "In Progress"),
                new Column("done",    "Done")
        );
        mv.addObject("columns", columns);
        return mv;
    }
}
