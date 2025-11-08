package fr.uha.ensisa.gl.kanbin.projest.model;

public class Column {
    private final String key;
    private final String title;

    public Column(String key, String title) {
        this.key = key;
        this.title = title;
    }
    public String getKey() { return key; }
    public String getTitle() { return title; }
}