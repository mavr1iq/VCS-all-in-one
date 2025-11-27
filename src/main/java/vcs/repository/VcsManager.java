package vcs.repository;

import vcs.repository.adapters.VcsAdapter;
import vcs.repository.classes.*;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.dao.impl.RepositoryDao;
import vcs.repository.factories.AdapterFactory;
import vcs.repository.factories.DaoFactory;

import java.io.File;
import java.util.List;

public class VcsManager {

    private final DaoFactory daoFactory;
    private final AdapterFactory adapterFactory;

    public VcsManager() {
        this.daoFactory = new DaoFactory(new DatabaseContext());

        this.adapterFactory = new AdapterFactory(this.daoFactory);
    }

    public void createRepository(String name, String path, VcsType type, int ownerId, String description) {
        RepositoryDao repoDao = daoFactory.getRepositoryDao();

        if (repoDao.getByPath(path) != null) {
            throw new RuntimeException("Repository already registered in DB at: " + path);
        }

        Repository repo = new Repository();
        repo.setName(name);
        repo.setUrl(path);
        repo.setType(type);
        repo.setOwner_id(ownerId);
        repo.setDescription(description);

        getAdapter(type).createRepository(repo);
    }


    public void cloneRepository(int sourceRepoId, String targetPath, int ownerId) {
        Repository sourceRepo = getRepoOrFail(sourceRepoId);

        Repository targetRepo = new Repository();
        targetRepo.setName(sourceRepo.getName() + "_clone");
        targetRepo.setUrl(targetPath);
        targetRepo.setType(sourceRepo.getType());
        targetRepo.setOwner_id(ownerId);

        getAdapter(sourceRepo.getType()).cloneRepository(sourceRepo, targetPath);
    }

    public List<Repository> getAllRepositories() {
        return daoFactory.getRepositoryDao().getAll();
    }

    public Repository getRepository(int id) {
        return getRepoOrFail(id);
    }

    public Commit commit(int repoId, int userId, String message) {
        Repository repo = getRepoOrFail(repoId);
        User user = daoFactory.getUserDao().getById(userId);
        if (user == null) throw new IllegalArgumentException("User not found: " + userId);

        return getAdapter(repo.getType()).commit(repo, user, message);
    }

    public void update(int repoId, String revision) {
        Repository repo = getRepoOrFail(repoId);
        getAdapter(repo.getType()).update(repo, revision);
    }

    public void checkout(int repoId, String branchName) {
        Repository repo = getRepoOrFail(repoId);
        Branch branch = new Branch();
        branch.setName(branchName);

        getAdapter(repo.getType()).checkout(repo, branch);
    }

    public boolean push(int repoId, String remotePath) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).push(repo, remotePath);
    }

    public boolean pull(int repoId, String remotePath) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).pull(repo, remotePath);
    }

    public void fetch(int repoId, String remotePath) {
        Repository repo = getRepoOrFail(repoId);
        getAdapter(repo.getType()).fetch(repo, remotePath);
    }

    public List<String> listFiles(int repoId, String ref) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).list(repo, ref);
    }

    public List<Commit> getLog(int repoId) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).getCommits(repo);
    }

    public Iterable<Commit> getCommitHistoryIterator(int startCommitId) {
        return daoFactory.getCommitDao().getAllPreviousCommitsById(startCommitId);
    }

    public List<Branch> getBranches(int repoId) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).getBranches(repo);
    }

    public void createBranch(int repoId, String branchName, int creatorId) {
        Repository repo = getRepoOrFail(repoId);
        Branch branch = new Branch();
        branch.setName(branchName);
        branch.setRepo_id(repoId);
        branch.setCreated_by(creatorId);

        getAdapter(repo.getType()).createBranch(repo, branch);
    }

    public MergeResults merge(int repoId, String sourceBranchName, String targetBranchName) {
        Repository repo = getRepoOrFail(repoId);

        Branch source = new Branch(); source.setName(sourceBranchName);
        Branch target = new Branch(); target.setName(targetBranchName);

        return getAdapter(repo.getType()).merge(repo, source, target);
    }

    public void createTag(int repoId, String tagName, String message) {
        Repository repo = getRepoOrFail(repoId);
        getAdapter(repo.getType()).tag(repo, tagName, message);
    }

    public void applyPatch(int repoId, File patchFile) {
        Repository repo = getRepoOrFail(repoId);
        getAdapter(repo.getType()).applyPatch(repo, patchFile);
    }

    private Repository getRepoOrFail(int repoId) {
        Repository repo = daoFactory.getRepositoryDao().getById(repoId);
        if (repo == null) {
            throw new IllegalArgumentException("Repository with ID " + repoId + " not found.");
        }
        return repo;
    }

    public String getRepoGraph(int repoId) {
        Repository repo = getRepoOrFail(repoId);
        return getAdapter(repo.getType()).getGraphText(repo);
    }

    private VcsAdapter getAdapter(VcsType type) {
        return adapterFactory.getAdapter(type);
    }
}