package vcs.repository.factories;

import vcs.repository.dao.GeneralDao;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.dao.impl.BranchDao;
import vcs.repository.dao.impl.CommitDao;
import vcs.repository.dao.impl.RepositoryDao;
import vcs.repository.dao.impl.UserDao;

public class DaoFactory {
    private final DatabaseContext dbContext;

    public DaoFactory(DatabaseContext dbContext) {
        this.dbContext = dbContext;
    }

    public RepositoryDao getRepositoryDao() {
        return new RepositoryDao(dbContext);
    }
    public BranchDao getBranchDao() {
        return new BranchDao(dbContext);
    }
    public CommitDao getCommitDao() {
        return new CommitDao(dbContext);
    }
    public UserDao getUserDao() {
        return new UserDao(dbContext);
    }
}
