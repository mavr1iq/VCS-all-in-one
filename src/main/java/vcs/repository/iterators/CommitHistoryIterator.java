package vcs.repository.iterators;

import vcs.repository.classes.Commit;
import vcs.repository.dao.impl.CommitDao;

import java.util.Iterator;

public class CommitHistoryIterator implements Iterator<Commit> {

    private final CommitDao commitDao;
    private final Commit firstCommit;
    private int nextCommitId;
    private Commit currentCommit;

    public CommitHistoryIterator(int startId, CommitDao commitDao) {
        this.commitDao = commitDao;

        Commit commit = commitDao.getById(startId);
        this.firstCommit = commit;

        if (commit != null) {
            this.nextCommitId = commit.getParent_commit();
        } else {
            this.nextCommitId = 0;
        }
    }

    @Override
    public boolean hasNext() {
        return nextCommitId > 0;
    }

    @Override
    public Commit next() {
        if (hasNext()) {
            Commit commit = commitDao.getById(nextCommitId);
            nextCommitId = commit.getParent_commit();
            return commit;
        }
        return null;
    }

    public Commit currentItem() {
        return currentCommit;
    }

    public Commit first() {
        return firstCommit;
    }
}
