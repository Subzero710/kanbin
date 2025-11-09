package fr.uha.ensisa.gl.kanbin.projest.repo.mem;

import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.RepoFactory;
import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;

public final class RepoFactoryMem implements RepoFactory {
    private final IssueRepo issueRepo = new IssueRepoMem();
    private final BoardRepo boardRepo = new BoardRepoMem();

    @Override
    public IssueRepo getIssueRepo() {
        return this.issueRepo;
    }
    @Override
    public BoardRepo getBoardRepo(){return this.boardRepo;}
}