package vcs.repository.adapters;

import vcs.repository.classes.Repository;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.MergeResult;

import java.util.List;

public interface VcsAdapter {
    List<Commit> getCommits(Repository repo);
    List<Branch> getBranches(Repository repo);
    MergeResult merge(Repository repo, Branch source, Branch target);
}
