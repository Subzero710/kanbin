package fr.uha.ensisa.gl.kanbin.projest.model;

import java.util.random.RandomGenerator;

public class Issue {

    private long id;
    private String title;
    private String columnKey;

    public Issue() {
        this.id = 0;
        this.title = null;
        this.columnKey = null;
    }

    public Issue(long id, String title) {
        this.id = id;
        this.title = title;
        this.columnKey = null;
    }

    public Issue(long id, String title, String columnKey) {
        this.id = id;
        this.title = title;
        this.columnKey = columnKey;
    }

    public String getTitle() { return this.title; }
    public long getId(){ return this.id; }
    public void setTitle(String title) { this.title = title; }
    public void setId(long id) { this.id = id; }
    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }
}