package fr.uha.ensisa.gl.kanbin.projest.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private long id;
    private String name;
    private final List<Column> columns = new ArrayList<>();

    public Board() {}
    public Board(long id, String name) { this.id = id; this.name = name; }
    public Board(String name) { this(0L, name); }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Column> getColumns() { return columns; }

    public Column addColumn(Column c) { columns.add(c); return c; }
    public boolean removeColumn(long columnId) {
        return columns.removeIf(c -> c.getId() == columnId);
    }
    private final List<Row> rows = new ArrayList<>();
    public List<Row> getRows() {
        return rows;
    }

    public void addRow(Row row) {
        this.rows.add(row);
    }
}