package fr.uha.ensisa.gl.kanbin.projest.model;

import java.util.ArrayList;
import java.util.List;

public class Row {
    private long id;
    private String key;
    private String title;
    private Integer pos;

    private boolean fixed = false;
    private final List<Row> subRows = new ArrayList<>();

    public Row() {}

    public Row(long id, String key, String title) {
        this.id = id; this.key = key; this.title = title;
    }

    public Row(String key, String title) { this(0L, key, title); }

    public List<Row> getSubRows() { return subRows; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getPos() { return pos; }
    public void setPos(Integer pos) { this.pos = pos; }

    public void addSubRow(Row sub) {
        this.subRows.add(sub);
    }

    public boolean isFixed() { return fixed; }
    public void setFixed(boolean fixed) { this.fixed = fixed; }
    public List<Row> getSubRowsOrSelf() {
        if (this.subRows.isEmpty()) {
            return List.of(this);
        }
        return this.subRows;
    }
}