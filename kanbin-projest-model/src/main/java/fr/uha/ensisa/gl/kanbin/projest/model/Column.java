package fr.uha.ensisa.gl.kanbin.projest.model;
import java.util.ArrayList;
import java.util.List;

public class Column {
    private long id;
    private String key;
    private String title;
    private int wipLimit;
    private Integer pos;
    private List<Column> subColumns = new ArrayList<>();

    public Column() {}
    public Column(long id, String key, String title) {
        this.id = id; this.key = key; this.title = title;
    }
    public Column(String key, String title) { this(0L, key, title); }
    public List<Column> getSubColumns() { return subColumns; }// 0l à revoir si problème

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getWipLimit() {return wipLimit;}
    public void setWipLimit(Integer wipLimit) {this.wipLimit = wipLimit;}
    public Integer getPos() { return pos; }
    public void setPos(Integer pos) { this.pos = pos; }
    public void addSubColumn(Column sub) {
        this.subColumns.add(sub);
    }
}