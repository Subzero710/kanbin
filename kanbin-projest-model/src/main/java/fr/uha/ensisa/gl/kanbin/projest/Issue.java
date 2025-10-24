package fr.uha.ensisa.gl.kanbin.projest;

import java.util.random.RandomGenerator;

public class Issue {

    private final long id;
    private String title;

    public Issue(String title) {
        this.id = RandomGenerator.getDefault().nextInt();
        this.title = title;
    }

    public Object getTitle() {
        return this.title;
    }

    public long getId(){
        return this.id;
    }

}
