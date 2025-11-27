package vcs.repository.adapters;

import vcs.repository.classes.*;

import java.io.File;
import java.util.List;

public interface VcsAdapter {

    void createRepository(Repository repository);
    Commit commit(Repository repo, User user, String message);
    boolean push(Repository repo, String remotePath);
    boolean pull(Repository repo, String remotePath);
    void fetch(Repository repo, String remotePath);
    List<String> list(Repository repo, String ref);
    List<Commit> getCommits(Repository repo);
    void applyPatch(Repository repo, File patchFile);
    List<Branch> getBranches(Repository repo);
    void createBranch(Repository repo, Branch branch);
    MergeResults merge(Repository repo, Branch sourceBranch, Branch targetBranch);
    void tag(Repository repo, String tagName, String message);
    void cloneRepository(Repository sourceRepo, String targetFolder);
    void checkout(Repository repo, Branch branch);
    public void update(Repository repo, String revision);
    String getGraphText(Repository repo);
}