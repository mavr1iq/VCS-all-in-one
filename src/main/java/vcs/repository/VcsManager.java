// Створіть новий пакет, наприклад, vcs
package vcs.repository;

import vcs.repository.adapters.VcsAdapter;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.MergeResult;
import vcs.repository.classes.Repository;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.factories.AdapterFactory;
import vcs.repository.factories.DaoFactory;

import java.util.List;


public class VcsManager {

    private final DaoFactory daoFactory;
    private final AdapterFactory adapterFactory;

    public VcsManager(DatabaseContext databaseContext) {
        this.daoFactory = new DaoFactory(databaseContext);
        this.adapterFactory = new AdapterFactory();
    }


    public void createRepository(int owner, String name, VcsType type, String url, String description) {
        Repository repo = new Repository();
        repo.setOwner_id(owner);
        repo.setName(name);
        repo.setType(type);
        repo.setUrl(url);
        repo.setDescription(description);
        daoFactory.getRepositoryDao().add(repo);

        System.out.println("Repository created: " + name);
    }

    public List<Commit> getCommits(int repoId) {
        Repository repo = findRepoOrFail(repoId);
        VcsAdapter adapter = adapterFactory.getAdapter(repo.getType());
        return adapter.getCommits(repo);
    }

    public MergeResult merge(int repoId, Branch source, Branch target) {
        Repository repo = findRepoOrFail(repoId);
        VcsAdapter adapter = adapterFactory.getAdapter(repo.getType());
        return adapter.merge(repo, source, target);
    }

    public List<Repository> getAllRepositories() {
        return daoFactory.getRepositoryDao().getAll();
    }

    private Repository findRepoOrFail(int repoId) {
        Repository repo = daoFactory.getRepositoryDao().getById(repoId);
        if (repo == null) {
            throw new IllegalArgumentException("Repository with id " + repoId + " not found.");
        }
        return repo;
    }
}