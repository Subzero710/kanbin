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

    protected Board getOrCreateDefaultBoard() {
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

        if (board != null) {
            modelAndView.addObject("rows", board.getRows());
        }
        return modelAndView;
    }

    @PostMapping("/issues")
    public String createIssue(Issue issue, RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();

        // 1. Restaurer clés existantes si ID fourni (Upsert)
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

        // 2. Assigner Row par défaut si nécessaire
        if (board != null && (issue.getRowKey() == null || issue.getRowKey().isEmpty())) {
            if (board.getRows() != null && !board.getRows().isEmpty()) {
                issue.setRowKey(board.getRows().getFirst().getKey());
            }
        }

        // 3. Initialiser la date si création directe dans Closed
        if ("closed".equals(issue.getColumnKey())) {
            issue.setClosedAt(java.time.LocalDateTime.now());
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
            // 1. Fusion (Merge) : Restauration des clés si le formulaire ne les envoie pas
            if (issue.getColumnKey() == null || issue.getColumnKey().isEmpty()) {
                issue.setColumnKey(existing.getColumnKey());
            }
            if (issue.getRowKey() == null || issue.getRowKey().isEmpty()) {
                issue.setRowKey(existing.getRowKey());
            }

            // 2. Gestion intelligente de la date (Machine à états)
            boolean wasClosed = "closed".equals(existing.getColumnKey());
            boolean isNowClosed = "closed".equals(issue.getColumnKey());

            if (isNowClosed && !wasClosed) {
                // Transition -> Entrée dans Closed : on date
                issue.setClosedAt(java.time.LocalDateTime.now());
            } else if (!isNowClosed && wasClosed) {
                // Transition -> Sortie de Closed : on efface la date
                issue.setClosedAt(null);
            } else {
                // Pas de transition (reste ouvert OU reste fermé) : on garde la date existante
                // Cela permet de préserver la date originale lors d'une simple modif de titre
                issue.setClosedAt(existing.getClosedAt());
            }
        } else {
            // Cas rare (Upsert avec ID inconnu) : on applique une règle simple
            if ("closed".equals(issue.getColumnKey()) && issue.getClosedAt() == null) {
                issue.setClosedAt(java.time.LocalDateTime.now());
            }
        }

        issueRepo.persist(issue);
        redirectAttributes.addFlashAttribute("message", "Story mise à jour");
        return "redirect:/board";
    }

    @PostMapping("/issues/{id}/delete")
    public String deleteIssue(@PathVariable long id, RedirectAttributes redirectAttributes) {
        Issue issue = issueRepo.find(id);
        if (issue != null) {
            issueRepo.remove(id);
            redirectAttributes.addFlashAttribute("message", "Story supprimée avec succès");
        }
        return "redirect:/board";
    }
}