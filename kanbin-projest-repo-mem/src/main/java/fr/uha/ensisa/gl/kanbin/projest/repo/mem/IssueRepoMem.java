package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

public class IssueRepoMem implements IssueRepo {
    private final Map<Long, Issue> store =
            Collections.synchronizedMap(new TreeMap<Long, Issue>());
    public void persist(Issue issue) {
        store.put(issue.getId(), issue);
    }
    public void remove(long id) {
        store.remove(id);
    }
    public Issue find(long id) {
        return store.get(id);
    }
    public Collection<Issue> findAll() {
        return store.values();
    }
    public long count() {
        return store.size();
    }
}
