package vcs.repository.adapters;

import vcs.repository.classes.Repository;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.MergeResult;

import java.util.List;

public class MercurialAdapter implements VcsAdapter {
    @Override
    public List<Commit> getCommits(Repository repo) {

        return List.of();
    }

    @Override
    public List<Branch> getBranches(Repository repo) {

        return List.of();
    }

    @Override
    public MergeResult merge(Repository repo, Branch source, Branch target) {

        return new MergeResult(true, List.of());
    }
}
