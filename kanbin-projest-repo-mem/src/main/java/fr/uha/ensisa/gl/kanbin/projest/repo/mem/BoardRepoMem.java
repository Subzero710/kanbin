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

            // Colonne Backlog (système)
            Column backlog = new Column("backlog", "Backlog");
            backlog.setFixed(true);

            // Colonne Closed (système)
            Column closed = new Column("closed", "Closed");
            closed.setFixed(true);

            b.addColumn(backlog);
            b.addColumn(closed);
            Row defaultRow = new Row("default", "Non catégorisé");
            defaultRow.setFixed(true);
            b.addRow(defaultRow);
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
        if (b == null) {
            throw new NoSuchElementException("board " + boardId + " not found");
        }

        if (column.getId() == 0) {
            column.setId(colSeq.getAndIncrement());
        }

        List<Column> cols = b.getColumns();

        // Les colonnes créées manuellement vont juste avant la colonne "closed" si elle existe
        if (!column.isFixed()) {
            int closedIndex = -1;
            for (int i = 0; i < cols.size(); i++) {
                Column c = cols.get(i);
                if ("closed".equals(c.getKey())) {
                    closedIndex = i;
                    break;
                }
            }
            if (closedIndex >= 0) {
                cols.add(closedIndex, column);
            } else {
                // fallback si, pour une raison quelconque, il n'y a pas (encore) de colonne closed
                cols.add(column);
            }
        } else {
            // colonnes fixes : on garde un comportement simple
            cols.add(column);
        }

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