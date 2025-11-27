package vcs.repository;

import vcs.repository.adapters.GitAdapter;
import vcs.repository.adapters.MercurialAdapter;
import vcs.repository.adapters.SvnAdapter;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.Repository;
import vcs.repository.classes.User;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.dao.impl.CommitDao;
import vcs.repository.factories.DaoFactory;
import vcs.repository.iterators.CommitHistoryIterator;


public class VcsAllInOne {
    public static void main(String[] args) {
        DatabaseContext dbContext = new DatabaseContext();
        DaoFactory daoFactory = new DaoFactory(dbContext);

       //testGitAdapter(daoFactory);
       //testSvnAdapter(daoFactory);
        testMercurialAdapter(daoFactory);
    }

    private static void testGitAdapter(DaoFactory daoFactory) {
        GitAdapter gitAdapter = new GitAdapter(daoFactory);
        Repository repo = new Repository();
        Repository repo2 = new Repository();
        Repository repo3 = new Repository();

        User user = new User();
        Branch branch = new Branch();
        Branch branch1 = new Branch();

        repo.setName("PapaRoma")
                .setOwner_id(1)
                .setType(VcsType.GIT)
                .setUrl("F:\\Programming\\projects\\python\\additional\\parser");

        repo2.setName("parser")
                .setOwner_id(1)
                .setType(VcsType.GIT)
                .setUrl("F:\\Programming\\projects\\python\\additional\\parser2");

        repo3.setName("test")
                .setOwner_id(1)
                .setType(VcsType.GIT)
                .setUrl("F:\\Programming\\projects\\repos\\test");

        user.setUsername("mavr")
                .setEmail("kivar2006@gmail.com")
                .setId(1);

        branch.setRepo_id(40)
                .setCreated_by(1)
                .setName("test");

        branch1.setRepo_id(40)
                .setCreated_by(1)
                .setName("master");

//         gitAdapter.createRepository(repo2);
//         gitAdapter.commit(repo2, user, "change");
//         gitAdapter.push(repo2, repo.getUrl());
//         gitAdapter.pull(repo2, repo.getUrl());
//         System.out.println(gitAdapter.list(repo2, null));
//         System.out.println(gitAdapter.getCommits(repo));
//         System.out.println(gitAdapter.getBranches(repo));
//         gitAdapter.createBranch(repo, branch);
//         gitAdapter.merge(repo, branch, branch1);
//         gitAdapter.tag(repo, "test", "dadadada");
        gitAdapter.getBranches(repo3);
    }

    private static void testSvnAdapter(DaoFactory daoFactory) {
        SvnAdapter svnAdapter = new SvnAdapter(daoFactory);

        Repository svnRepo = new Repository();
        Repository svnRepo2 = new Repository();
        User user = new User();
        Branch trunk = new Branch();
        Branch testBranch = new Branch();

        // Налаштування SVN репозиторіїв
        svnRepo.setName("SVN_Test_Repo")
                .setOwner_id(1)
                .setType(VcsType.SVN)
                .setUrl("F:\\Programming\\projects\\svn_repos\\svn_test1\\repo_working");

        svnRepo2.setName("SVN_Test_Repo2")
                .setOwner_id(1)
                .setType(VcsType.SVN)
                .setUrl("F:\\Programming\\projects\\svn_repos\\svn_test2");

        user.setUsername("svn_user")
                .setEmail("svn@example.com")
                .setId(1);

        trunk.setRepo_id(41)
                .setCreated_by(1)
                .setName("trunk");

        testBranch.setRepo_id(41)
                .setCreated_by(1)
                .setName("feature-test");


        //svnAdapter.createRepository(svnRepo);

        //svnAdapter.checkout(svnRepo, trunk);
        //svnAdapter.commit(svnRepo, user, "Adding merge files2");

        //svnAdapter.update(svnRepo, "HEAD");

        //svnAdapter.push(svnRepo, svnRepo2.getUrl());

        //svnAdapter.pull(svnRepo, svnRepo2.getUrl());

        //System.out.println("Files: " + svnAdapter.list(svnRepo, null));

        //System.out.println("Commits: " + svnAdapter.getCommits(svnRepo));

        System.out.println("Branches: " + svnAdapter.getBranches(svnRepo));

        //svnAdapter.createBranch(svnRepo, testBranch);

        //svnAdapter.merge(svnRepo, testBranch, trunk);

        //svnAdapter.tag(svnRepo, "v1.0.0", "Release version 1.0.0");

        //svnAdapter.cloneRepository(svnRepo, "F:\\Programming\\projects\\svn_repos\\svn_clone");

        //java.io.File patchFile = new java.io.File("F:\\Programming\\projects\\patches\\test.patch");
        //svnAdapter.applyPatch(svnRepo, patchFile);
    }

    private static void testMercurialAdapter(DaoFactory daoFactory) {
        MercurialAdapter mercurialAdapter = new MercurialAdapter(daoFactory);
        Repository repo = new Repository();
        Repository repo2 = new Repository();
        User user = new User();
        Branch branch = new Branch();
        Branch branch1 = new Branch();

        repo.setName("PapaRoma")
                .setOwner_id(1)
                .setType(VcsType.MERCURIAL)
                .setUrl("F:\\Programming\\projects\\python\\additional\\mercurial");

        repo2.setName("parser")
                .setOwner_id(1)
                .setType(VcsType.MERCURIAL)
                .setUrl("F:\\Programming\\projects\\python\\additional\\mercurial1");

        user.setUsername("mavr")
                .setEmail("kivar2006@gmail.com")
                .setId(1);

        branch.setRepo_id(40)
                .setCreated_by(1)
                .setName("test");

        branch1.setRepo_id(40)
                .setCreated_by(1)
                .setName("default");

//        mercurialAdapter.createRepository(repo);
//        mercurialAdapter.commit(repo2, user, "adding test files");
//        mercurialAdapter.push(repo2, repo.getUrl());
//        mercurialAdapter.pull(repo2, repo.getUrl());
//        System.out.println(mercurialAdapter.list(repo2, null));
        //System.out.println(mercurialAdapter.getCommits(repo));
//        System.out.println(mercurialAdapter.getBranches(repo));
        //mercurialAdapter.createBranch(repo, branch);
        //mercurialAdapter.merge(repo, branch, branch1);
        //mercurialAdapter.tag(repo, "м1", "dadadada");
        //mercurialAdapter.checkout(repo, branch);
    }
}
