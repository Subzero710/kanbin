package fr.uha.ensisa.gl.kanbin.projest.model;

import java.time.LocalDateTime;

public class Issue {

    private long id;
    private String title;
    private String columnKey;

    private String rowKey;

    private String detail;
    private LocalDateTime closedAt;

    public Issue() {
        this.id = 0;
        this.title = null;
        this.columnKey = null;
        this.rowKey = null;
    }

    public Issue(long id, String title) {
        this.id = id;
        setTitle(title);
        this.columnKey = null;
    }

    public Issue(long id, String title, String columnKey) {
        this.id = id;
        setTitle(title);
        this.columnKey = columnKey;
    }

    public Issue(long id, String title, String columnKey, String detail) {
        this.id = id;
        setTitle(title);
        this.columnKey = columnKey;
        this.detail = detail;
    }

    // --- GETTERS & SETTERS ---

    public long getId(){ return this.id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return this.title; }

    public void setTitle(String title) {
        if (title != null && title.length() > 30) {
            this.title = title.substring(0, 30);
        } else {
            this.title = title;
        }
    }

    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }

    public void setDetail(String detail) { this.detail = detail; }
    public String getDetail() { return this.detail; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }
}