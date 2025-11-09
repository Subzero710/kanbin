package fr.uha.ensisa.gl.kanbin.projest.repo;

import fr.uha.ensisa.gl.kanbin.projest.model.Board;
import fr.uha.ensisa.gl.kanbin.projest.model.Column;
import java.util.List;
import java.util.Optional;

public interface BoardRepo {
    Board save(Board board);
    Optional<Board> findById(long id);
    List<Board> findAll();
    boolean deleteById(long id);

    Column addColumn(long boardId, Column column);
    boolean removeColumn(long boardId, long columnId);
}
