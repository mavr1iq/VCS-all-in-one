package vcs.repository;

import vcs.repository.classes.Commit;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.dao.impl.CommitDao;

public class VcsAllInOne {
    public static void main(String[] args) {
        String dbUrl = "jdbc:postgresql://localhost:5433/vcs-all-in-one?user=postgres&password=kivar";
        DatabaseContext dbContext = new DatabaseContext(dbUrl);
        CommitDao commitDao = new CommitDao(dbContext);

        Commit commit = commitDao.getById(1);
        System.out.println(commit);
    }
}
