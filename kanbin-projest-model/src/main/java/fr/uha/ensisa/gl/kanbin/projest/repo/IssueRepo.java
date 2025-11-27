package fr.uha.ensisa.gl.kanbin.projest.repo;

import java.util.Collection;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;

public interface IssueRepo {
    void persist(Issue issue);
    void remove(long id);
    Issue find(long id);
    Collection<Issue> findAll();
    long count();
}