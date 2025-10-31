package vcs.repository;

import vcs.repository.classes.Commit;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.dao.impl.CommitDao;


public class VcsAllInOne {
    public static void main(String[] args) {
        DatabaseContext dbContext = new DatabaseContext();
        CommitDao commitDao = new CommitDao(dbContext);

        Commit commit = commitDao.getById(1);
        System.out.println(commit);
    }
}
