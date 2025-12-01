package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Collection;

@Controller
public class IssueController {

    private final IssueRepo issueRepo;

    @Autowired
    public IssueController(IssueRepo issueRepo) {
        this.issueRepo = issueRepo;
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

    @PostMapping("/issues/{id}/delete")
    public String deleteIssue(@PathVariable long id, RedirectAttributes redirectAttributes) {
        issueRepo.remove(id);
        redirectAttributes.addFlashAttribute("message", "La story ID " + id + " a été supprimée avec succès.");
        return "redirect:/issues";
    }

    @GetMapping("/issues/{id}/edit")
    public ModelAndView editIssueForm(@PathVariable long id) {
        Issue issue = issueRepo.find(id);
        if (issue == null) {
            return new ModelAndView("redirect:/issues");
        }
        ModelAndView mv = new ModelAndView("edit-issue");
        mv.addObject("issue", issue);
        return mv;
    }

    @PostMapping("/issues/{id}")
    public String updateIssue(@PathVariable long id, Issue issue, RedirectAttributes redirectAttributes) {
        issue.setId(id);
        Issue existing = issueRepo.find(id);
        if (existing != null) {
            issue.setColumnKey(existing.getColumnKey());
        }

        issueRepo.persist(issue);
        redirectAttributes.addFlashAttribute("message", "La story '" + issue.getTitle() + "' a été mise à jour.");
        return "redirect:/issues";
    }
}