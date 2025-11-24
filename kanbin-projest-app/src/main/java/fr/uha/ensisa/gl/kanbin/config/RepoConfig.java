package fr.uha.ensisa.gl.kanbin.config;

import fr.uha.ensisa.gl.kanbin.projest.repo.IssueRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo; // Cet import est OBLIGATOIRE
import fr.uha.ensisa.gl.kanbin.projest.repo.RepoFactory;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.RepoFactoryMem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepoConfig {
    @Bean
    public RepoFactory repoFactory() {
        return new RepoFactoryMem(); [cite_start]// Implémentation mémoire définie dans RepoFactoryMem [cite: 213]
    }

    @Bean
    public IssueRepo issueRepo(RepoFactory factory) {
        return factory.getIssueRepo();
    }

    @Bean
    public BoardRepo boardRepo(RepoFactory factory) {
        return factory.getBoardRepo();
    }
}