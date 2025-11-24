package fr.uha.ensisa.gl.kanbin.projest.repo;

import java.util.Collection;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;

public interface IssueRepo {
    public void persist(Issue issue);
    public void remove(long id);
    Issue find(long id);
    public Issue find(long id);
    public Collection<Issue> findAll();
    public long count();
}
