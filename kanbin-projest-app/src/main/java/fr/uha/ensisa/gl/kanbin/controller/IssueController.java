package fr.uha.ensisa.gl.kanbin.controller;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;

@Controller
public class IssueController {

    private final IssueRepo issueRepo;
    private final BoardRepo boardRepo;

    @Autowired
    public IssueController(IssueRepo issueRepo, BoardRepo boardRepo) {
        this.issueRepo = issueRepo;
        this.boardRepo = boardRepo;
    }

    // Helper pour récupérer le board (comme dans BoardController)
    private Board getOrCreateDefaultBoard() {
        final String preferredName = "Default";

        Collection<Board> all = boardRepo.findAll();
        Board board = null;

        if (all != null && !all.isEmpty()) {
            board = all.stream()
                    .filter(b -> preferredName.equals(b.getName()))
                    .findFirst()
                    .orElse(all.iterator().next());
        }

        if (board == null) {
            Board newBoard = new Board(preferredName);

            Column backlog = new Column("backlog", "Backlog");
            backlog.setFixed(true);
            Column closed = new Column("closed", "Closed");
            closed.setFixed(true);
            newBoard.addColumn(backlog);
            newBoard.addColumn(closed);

            Row defaultRow = new Row("default", "Non catégorisé");
            defaultRow.setFixed(true);
            newBoard.addRow(defaultRow);

            Board saved = boardRepo.save(newBoard);
            board = (saved != null ? saved : newBoard);
        }

        if (board.getRows() == null || board.getRows().isEmpty()) {
            Row defaultRow = new Row("default", "Non catégorisé");
            defaultRow.setFixed(true);
            board.addRow(defaultRow);
            boardRepo.save(board);
        }

        return board;
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
        Board board = getOrCreateDefaultBoard();
        ModelAndView modelAndView = new ModelAndView("create-issue");
        modelAndView.addObject("issue", new Issue());

        // On envoie la liste des lignes pour le selecteur dans la vue (si besoin)
        if (board != null) {
            modelAndView.addObject("rows", board.getRows());
        }
        return modelAndView;
    }

    @PostMapping("/issues")
    public String createIssue(Issue issue, RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();

        // 1) Si on édite une issue existante, restaurer d'abord ses clés
        if (issue.getId() > 0) {
            Issue oldIssue = issueRepo.find(issue.getId());
            if (oldIssue != null) {
                if (issue.getColumnKey() == null || issue.getColumnKey().isEmpty()) {
                    issue.setColumnKey(oldIssue.getColumnKey());
                }
                if (issue.getRowKey() == null || issue.getRowKey().isEmpty()) {
                    issue.setRowKey(oldIssue.getRowKey());
                }
            }
        }

        // 2) Ensuite seulement appliquer la row par défaut si toujours vide
        if (board != null && (issue.getRowKey() == null || issue.getRowKey().isEmpty())) {
            if (board.getRows() != null && !board.getRows().isEmpty()) {
                issue.setRowKey(board.getRows().get(0).getKey());
            }
        }

        issueRepo.persist(issue);
        redirectAttributes.addFlashAttribute("message", "Story créée avec succès");
        return "redirect:/board";
    }

    @GetMapping("/issues/{id}/edit")
    public ModelAndView editIssue(@PathVariable long id) {
        Issue issue = issueRepo.find(id);
        if (issue == null) {
            return new ModelAndView("redirect:/issues");
        }

        Board board = getOrCreateDefaultBoard();
        ModelAndView mv = new ModelAndView("edit-issue");
        mv.addObject("issue", issue);

        if (board != null) {
            mv.addObject("rows", board.getRows());
        }
        return mv;
    }

    @PostMapping("/issues/{id}")
    public String updateIssue(@PathVariable long id, Issue issue, RedirectAttributes redirectAttributes) {
        Issue existing = issueRepo.find(id);
        if (existing != null) {
            // On préserve la colonne si non modifiée
            if (issue.getColumnKey() == null) {
                issue.setColumnKey(existing.getColumnKey());
            }

            if (issue.getRowKey() == null || issue.getRowKey().isEmpty()) {
                issue.setRowKey(existing.getRowKey());
            }

            issue.setClosedAt(existing.getClosedAt());
        }

        issueRepo.persist(issue);
        redirectAttributes.addFlashAttribute("message", "Story mise à jour");
        return "redirect:/board";
    }

    @PostMapping("/issues/{id}/delete") // L'URL doit correspondre à ce que le test attend
    public String deleteIssue(@PathVariable long id, RedirectAttributes redirectAttributes) {
        fr.uha.ensisa.gl.kanbin.projest.model.Issue issue = issueRepo.find(id);
        if (issue != null) {
            issueRepo.remove(id);
            redirectAttributes.addFlashAttribute("message", "Story supprimée avec succès");
        }
        return "redirect:/board";
    }
}