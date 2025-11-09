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
            b.setId(boardSeq.getAndIncrement());
            b.addColumn(new Column(colSeq.getAndIncrement(), "backlog", "Backlog"));
            b.addColumn(new Column(colSeq.getAndIncrement(), "todo",    "To do"));
            b.addColumn(new Column(colSeq.getAndIncrement(), "ongoing", "Ongoing"));
            b.addColumn(new Column(colSeq.getAndIncrement(), "toit",    "To IT"));
            b.addColumn(new Column(colSeq.getAndIncrement(), "init",    "In IT"));
            b.addColumn(new Column(colSeq.getAndIncrement(), "done",    "Done"));
            store.put(b.getId(), b);
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
        return column;
    }

    @Override
    public boolean removeColumn(long boardId, long columnId) {
        Board b = store.get(boardId);
        if (b == null) return false;
        return b.removeColumn(columnId);
    }
}