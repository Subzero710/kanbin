package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.repo.RepoFactory;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

public class RepoFactoryMem implements RepoFactory {
    public final IssueRepo issueRepo = new IssueRepoMem();

    @Override
    public IssueRepo getIssueRepo() {
        return this.issueRepo;
    }
}