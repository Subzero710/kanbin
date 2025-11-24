package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import fr.uha.ensisa.gl.kanbin.projest.model.Issue;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

public class IssueRepoMem implements IssueRepo {

    private final AtomicLong currentId = new AtomicLong(1);

    private final Map<Long, Issue> store =
            Collections.synchronizedMap(new TreeMap<Long, Issue>());

    @Override
    public void persist(Issue issue) {
        if (issue.getId() == 0) {
            issue.setId(currentId.getAndIncrement());
        }
        store.put(issue.getId(), issue);
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Issue find(long id) {
        return store.get(id);
    }

    @Override
    public Collection<Issue> findAll() {
        return store.values();
    }

    @Override
    public long count() {
        return store.size();
    }
}
