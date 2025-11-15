package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collection;

@Controller
public class IssueController {

    private final IssueRepo issueRepo;

    @Autowired
    public IssueController(IssueRepo issueRepo) {
        this.issueRepo = issueRepo;
        if (issueRepo.count() == 0) {
            Issue i1 = new Issue();
            i1.setTitle("Story: Configurer l'environnement");
            issueRepo.persist(i1);

            Issue i2 = new Issue();
            i2.setTitle("Story: Ajouter les tests unitaires");
            issueRepo.persist(i2);
        }
    }

    @GetMapping("/")
    public String homeRedirect() {
        return "redirect:/issues";
    }

    @GetMapping("/issues")
    public ModelAndView listIssues() {
        Collection<Issue> issues = issueRepo.findAll();
        ModelAndView modelAndView = new ModelAndView("list-issues");
        modelAndView.addObject("issues", issues);
        return modelAndView;
    }

    @GetMapping("/issues/new")
    public ModelAndView newIssue() {
        ModelAndView modelAndView = new ModelAndView("create-issue");
        modelAndView.addObject("issue", new Issue());
        return modelAndView;
    }

    @PostMapping("/issues")
    public String createIssue(Issue issue, RedirectAttributes redirectAttributes) {
        issueRepo.persist(issue);
        redirectAttributes.addFlashAttribute("message", "La nouvelle story '" + issue.getTitle() + "' a été ajoutée avec succès !");
        return "redirect:/issues";
    }

    @GetMapping("/hello")
    public ModelAndView hello(@RequestParam(required = false, defaultValue = "Utilisateur") String name) {
        ModelAndView ret = new ModelAndView("home");
        ret.addObject("name", name);
        return ret;
    }
}