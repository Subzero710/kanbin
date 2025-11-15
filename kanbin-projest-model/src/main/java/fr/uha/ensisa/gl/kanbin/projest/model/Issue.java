package fr.uha.ensisa.gl.kanbin.projest.model;

public class Issue {

    private long id;
    private String title;

    public Issue() {
        this.id = 0;
        this.title = null;
    }

    public Issue(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getTitle() { return this.title; }
    public long getId(){ return this.id; }
    public void setTitle(String title) { this.title = title; }
    public void setId(long id) { this.id = id; }
}