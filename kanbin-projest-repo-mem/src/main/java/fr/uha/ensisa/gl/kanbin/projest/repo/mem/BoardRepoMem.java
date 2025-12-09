package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.model.*;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BoardRepoMem implements BoardRepo {

    private final Map<Long, Board> store = new ConcurrentHashMap<>();
    private final AtomicLong boardSeq = new AtomicLong(1);
    private final AtomicLong colSeq   = new AtomicLong(1);

    public void seed() {
        if (store.isEmpty()) {
            Board b = new Board("Default");

            // Colonne Backlog (Système)
            Column backlog = new Column("backlog", "Backlog");

            // --- VERROUILLAGE ICI ---
            backlog.setFixed(true);

            Column backlogTodo = new Column("backlog-todo", "À Faire");
            Column backlogWip  = new Column("backlog-wip",  "En Cours");

            backlog.addSubColumn(backlogTodo);
            backlog.addSubColumn(backlogWip);
            b.addColumn(backlog);

            // On passe par save() pour avoir les mêmes règles d'IDs
            save(b);
        }
    }

    @Override
    public Board save(Board board) {
        if (board.getId() == 0) {
            board.setId(boardSeq.getAndIncrement());
        }
        // Assigner des ids aux colonnes sans id
        for (Column c : board.getColumns()) {
            if (c.getId() == 0) c.setId(colSeq.getAndIncrement());
        }
        store.put(board.getId(), board);
        return board;
    }

    @Override
    public Optional<Board> findById(long id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public List<Board> findAll() { return new ArrayList<>(store.values()); }

    @Override
    public boolean deleteById(long id) { return store.remove(id) != null; }

    @Override
    public Column addColumn(long boardId, Column column) {
        Board b = store.get(boardId);
        if (b == null) throw new NoSuchElementException("board " + boardId + " not found");
        if (column.getId() == 0) column.setId(colSeq.getAndIncrement());
        b.addColumn(column);
        this.save(b);
        return column;
    }

    @Override
    public boolean removeColumn(long boardId, long columnId) {
        Board b = store.get(boardId);
        if (b == null) return false;
        boolean removed = b.removeColumn(columnId);
        if (removed) {
            this.save(b);
        }
        return removed;
    }
}