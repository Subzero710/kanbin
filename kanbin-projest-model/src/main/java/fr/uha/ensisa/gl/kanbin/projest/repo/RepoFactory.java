package fr.uha.ensisa.gl.kanbin.projest.repo;

public interface RepoFactory {
    public IssueRepo getIssueRepo();

    BoardRepo getBoardRepo();
}
