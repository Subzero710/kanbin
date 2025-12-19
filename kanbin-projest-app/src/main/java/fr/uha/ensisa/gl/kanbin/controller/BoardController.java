package fr.uha.ensisa.gl.kanbin.controller;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.model.Row;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class BoardController {

    private final BoardRepo boards;
    private final IssueRepo issues;

    public BoardController(BoardRepo boards, IssueRepo issues) {
        this.boards = boards;
        this.issues = issues;
    }

    private Board getOrCreateDefaultBoard() {
        final String preferredName = "Default";

        Collection<Board> all = boards.findAll();
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

            Board saved = boards.save(newBoard);
            board = (saved != null ? saved : newBoard);
        }

        return board;
    }

    @GetMapping("/")
    public ModelAndView root() {
        return new ModelAndView("redirect:/board");
    }

    @GetMapping("/board")
    public ModelAndView showBoard() {
        Board b = getOrCreateDefaultBoard();
        ModelAndView mv = new ModelAndView("board");
        mv.addObject("board", b);
        mv.addObject("columns", b.getColumns());
        mv.addObject("rows", b.getRows());

        Map<String, Map<String, List<Issue>>> issuesByRowAndCol = new LinkedHashMap<>();

        for (Row row : b.getRows()) {
            Map<String, List<Issue>> colMap = new LinkedHashMap<>();
            for (Column col : b.getColumns()) {
                if (col.getSubColumns().isEmpty()) {
                    colMap.put(col.getKey(), new ArrayList<>());
                } else {
                    for (Column sub : col.getSubColumns()) {
                        colMap.put(sub.getKey(), new ArrayList<>());
                    }
                }
            }
            issuesByRowAndCol.put(row.getKey(), colMap);
        }

        String defaultRowKey = b.getRows().isEmpty() ? null : b.getRows().getFirst().getKey();
        String defaultColKey = "backlog";

        for (Issue issue : issues.findAll()) {
            boolean changed = false;

            if (issue.getRowKey() == null && defaultRowKey != null) {
                issue.setRowKey(defaultRowKey);
                changed = true;
            }
            if (issue.getColumnKey() == null) {
                issue.setColumnKey(defaultColKey);
                changed = true;
            }

            if (changed) issues.persist(issue);

            if (issue.getRowKey() != null && issue.getColumnKey() != null) {
                Map<String, List<Issue>> rowMap = issuesByRowAndCol.get(issue.getRowKey());
                if (rowMap != null) {
                    List<Issue> list = rowMap.get(issue.getColumnKey());
                    if (list != null) {
                        list.add(issue);
                    }
                }
            }
        }

        for (Map<String, List<Issue>> colMap : issuesByRowAndCol.values()) {
            for (Map.Entry<String, List<Issue>> entry : colMap.entrySet()) {
                String colKey = entry.getKey();
                List<Issue> list = entry.getValue();

                if ("backlog".equals(colKey)) {
                    list.sort(Comparator.comparingLong(Issue::getId).reversed());
                } else if ("closed".equals(colKey)) {
                    list.sort((i1, i2) -> {
                        if (i1.getClosedAt() == null) return 1;
                        if (i2.getClosedAt() == null) return -1;
                        return i2.getClosedAt().compareTo(i1.getClosedAt());
                    });
                } else {
                    list.sort(Comparator.comparingLong(Issue::getId));
                }
            }
        }

        mv.addObject("issuesByRowAndCol", issuesByRowAndCol);

        Map<Long, Long> wipUsageByColumnId = new HashMap<>();
        for (Column mainCol : b.getColumns()) {
            long count = countIssuesInColumn(b, mainCol);
            wipUsageByColumnId.put(mainCol.getId(), count);
        }
        mv.addObject("wipUsageByColumnId", wipUsageByColumnId);

        return mv;
    }

    @PostMapping("/board/add-column")
    public String addColumn(
            @RequestParam("title") String title,
            @RequestParam(value = "type", defaultValue = "simple") String type,
            @RequestParam(value = "wipLimit", defaultValue = "0") int wipLimit,
            RedirectAttributes redirectAttributes) {

        Board board = getOrCreateDefaultBoard();
        String key = title.toLowerCase().trim().replaceAll("\\s+", "-");

        List<String> newKeys = new ArrayList<>();
        newKeys.add(key);
        if ("double".equalsIgnoreCase(type)) {
            newKeys.add(key + "-todo");
            newKeys.add(key + "-wip");
        }

        Set<String> existingKeys = new HashSet<>();
        for (Column col : board.getColumns()) {
            if (col.getKey() != null) existingKeys.add(col.getKey());
            for (Column sub : col.getSubColumns()) {
                if (sub.getKey() != null) existingKeys.add(sub.getKey());
            }
        }

        if (newKeys.stream().anyMatch(existingKeys::contains)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de créer la colonne : nom déjà pris.");
            return "redirect:/board";
        }

        Column newColumn = new Column(key, title);
        newColumn.setWipLimit(wipLimit);

        if ("double".equalsIgnoreCase(type)) {
            newColumn.setType("double");
            Column subTodo = new Column(key + "-todo", "À Faire");
            Column subWip = new Column(key + "-wip", "En Cours");
            newColumn.addSubColumn(subTodo);
            newColumn.addSubColumn(subWip);
        }

        boards.addColumn(board.getId(), newColumn);
        return "redirect:/board";
    }

    @PostMapping("/board/move-card")
    @ResponseBody
    public String moveCard(@RequestParam Long issueId,
                           @RequestParam String rowKey,
                           @RequestParam String colKey) {
        return "OK";
    }

    @PostMapping("/board/remove-column")
    public String removeColumn(@RequestParam("columnId") long columnId,
                               RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == columnId)
                .findFirst();

        if (colOpt.isEmpty()) return "redirect:/board";
        Column column = colOpt.get();

        if (column.isFixed()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Action interdite : Cette colonne système ne peut pas être supprimée.");
            return "redirect:/board";
        }

        long count = countIssuesInColumn(board, column);
        if (count > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de supprimer : la colonne contient des tâches.");
            return "redirect:/board";
        }

        boards.removeColumn(board.getId(), columnId);
        return "redirect:/board";
    }

    @GetMapping("/board/columns/{id}/edit")
    public ModelAndView editColumnForm(@PathVariable long id) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id).findFirst();

        if (colOpt.isEmpty() || colOpt.get().isFixed()) {
            return new ModelAndView("redirect:/board");
        }

        ModelAndView mv = new ModelAndView("edit-column");
        mv.addObject("column", colOpt.get());
        return mv;
    }

    @PostMapping("/board/columns/{id}")
    public String updateColumn(@PathVariable long id,
                               @RequestParam("title") String title,
                               @RequestParam(value = "wipLimit", required = false) Integer wipLimit,
                               RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();
        Optional<Column> colOpt = board.getColumns().stream()
                .filter(c -> c.getId() == id).findFirst();

        if (colOpt.isEmpty() || colOpt.get().isFixed()) {
            return "redirect:/board";
        }

        Column column = colOpt.get();
        String normalizedTitle = title.trim();

        boolean titleAlreadyUsed = board.getColumns().stream()
                .filter(c -> c.getId() != id)
                .anyMatch(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(normalizedTitle));

        if (titleAlreadyUsed) {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de renommer : ce nom est déjà utilisé.");
            return "redirect:/board";
        }

        column.setTitle(normalizedTitle);
        int finalLimit = (wipLimit != null && wipLimit > 0) ? wipLimit : 0;
        column.setWipLimit(finalLimit);

        boards.save(board);
        return "redirect:/board";
    }

    @PostMapping("/board/reorder-column")
    @ResponseBody
    public String reorderColumn(@RequestParam("columnId") long columnId,
                                @RequestParam("newIndex") int newIndex) {
        Board board = getOrCreateDefaultBoard();
        boolean success = moveColumnInternal(board, columnId, newIndex);
        if (success) {
            boards.save(board);
            return "OK";
        } else {
            return "ERROR: Invalid move";
        }
    }

    private boolean moveColumnInternal(Board board, long columnId, int newIndex) {
        if (newIndex <= 0) return false;

        List<Column> columns = board.getColumns();
        int oldIndex = -1;
        Column columnToMove = null;

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getId() == columnId) {
                columnToMove = columns.get(i);
                oldIndex = i;
                break;
            }
        }

        if (columnToMove == null || columnToMove.isFixed()) return false;

        columns.remove(oldIndex);
        if (newIndex >= columns.size()) {
            columns.add(columnToMove);
        } else {
            columns.add(newIndex, columnToMove);
        }
        return true;
    }

    @PostMapping("/board/add-row")
    public String addRow(@RequestParam("title") String title) {
        Board board = getOrCreateDefaultBoard();
        String key = title.toLowerCase().trim().replaceAll("\\s+", "-") + "-" + System.currentTimeMillis();
        Row newRow = new Row(key, title);
        board.addRow(newRow);
        boards.save(board);
        return "redirect:/board";
    }

    @PostMapping("/board/remove-row")
    public String removeRow(@RequestParam("rowKey") String rowKey,
                            RedirectAttributes redirectAttributes) {
        Board board = getOrCreateDefaultBoard();

        Optional<Row> rowOpt = board.getRows().stream()
                .filter(r -> rowKey != null && rowKey.equals(r.getKey()))
                .findFirst();

        if (rowOpt.isEmpty()) return "redirect:/board";
        Row row = rowOpt.get();

        if (row.isFixed()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Action interdite : cette swimlane système ne peut pas être supprimée.");
            return "redirect:/board";
        }

        if (board.getRows().size() <= 1) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Impossible de supprimer : il doit rester au moins une swimlane.");
            return "redirect:/board";
        }

        long count = issues.findAll().stream()
                .filter(i -> rowKey.equals(i.getRowKey()))
                .count();
        if (count > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Impossible de supprimer : la swimlane contient des tâches.");
            return "redirect:/board";
        }

        if (board.removeRowByKey(rowKey)) {
            boards.save(board);
        }
        return "redirect:/board";
    }

    @PostMapping("/board/move-issue-dnd")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveIssueDnD(
            @RequestParam("issueId") long issueId,
            @RequestParam("targetColumnKey") String targetColumnKey,
            @RequestParam(value = "targetRowKey", required = false) String targetRowKey) {

        Map<String, Object> response = new HashMap<>();
        Board board = getOrCreateDefaultBoard();
        Issue issue = issues.find(issueId);

        if (issue == null) {
            response.put("success", false);
            response.put("message", "Issue introuvable");
            return ResponseEntity.badRequest().body(response);
        }

        Column targetCol = findColumnByKey(board, targetColumnKey);
        if (targetCol == null) {
            response.put("success", false);
            response.put("message", "Colonne cible introuvable");
            return ResponseEntity.badRequest().body(response);
        }

        int wipLimit = resolveWipLimit(board, targetCol);
        if (wipLimit > 0) {
            long currentCount = countIssuesInColumn(board, targetCol);
            boolean alreadyInTargetColumn = isIssueAlreadyInLogicalColumn(board, targetCol, issue);

            long afterMove = currentCount + (alreadyInTargetColumn ? 0L : 1L);
            if (afterMove > wipLimit) {
                response.put("success", false);
                response.put("message", "Le nombre maximum de story pour cette colonne est atteint !");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // CORRECTION ICI : Gestion de la transition d'état (State Machine)
        boolean wasClosed = "closed".equals(issue.getColumnKey());
        boolean isNowClosed = "closed".equals(targetColumnKey);

        if (isNowClosed && !wasClosed) {
            // Entrée dans Closed
            issue.setClosedAt(LocalDateTime.now());
        } else if (!isNowClosed && wasClosed) {
            // Sortie de Closed
            issue.setClosedAt(null);
        }
        // Sinon (déplacement interne Closed->Closed), on préserve la date existante

        issue.setColumnKey(targetColumnKey);
        if (targetRowKey != null) {
            issue.setRowKey(targetRowKey);
        }

        issues.persist(issue);

        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    private Column findColumnByKey(Board board, String key) {
        for (Column col : board.getColumns()) {
            if (key.equals(col.getKey())) return col;
            for (Column sub : col.getSubColumns()) {
                if (key.equals(sub.getKey())) return sub;
            }
        }
        return null;
    }

    private Column findMainColumn(Board board, Column column) {
        for (Column main : board.getColumns()) {
            if (main == column) return main;
            if (main.getSubColumns().contains(column)) return main;
        }
        return null;
    }

    private List<String> collectLogicalColumnKeys(Board board, Column column) {
        List<String> keys = new ArrayList<>();
        Column main = findMainColumn(board, column);
        if (main != null) {
            if (!main.getSubColumns().isEmpty()) {
                for (Column sub : main.getSubColumns()) {
                    if (sub.getKey() != null) keys.add(sub.getKey());
                }
            } else if (main.getKey() != null) {
                keys.add(main.getKey());
            }
        } else if (column.getKey() != null) {
            keys.add(column.getKey());
        }
        return keys;
    }

    private long countIssuesInColumn(Board board, Column anyColumn) {
        List<String> keys = collectLogicalColumnKeys(board, anyColumn);
        if (keys.isEmpty()) return 0L;
        long count = 0L;
        for (Issue i : issues.findAll()) {
            String key = i.getColumnKey();
            if (key != null && keys.contains(key)) {
                count++;
            }
        }
        return count;
    }

    private int resolveWipLimit(Board board, Column targetCol) {
        Column main = findMainColumn(board, targetCol);
        if (main != null) return main.getWipLimit();
        return targetCol.getWipLimit();
    }

    private boolean isIssueAlreadyInLogicalColumn(Board board, Column targetCol, Issue issue) {
        List<String> keys = collectLogicalColumnKeys(board, targetCol);
        String currentKey = issue.getColumnKey();
        return currentKey != null && keys.contains(currentKey);
    }
}