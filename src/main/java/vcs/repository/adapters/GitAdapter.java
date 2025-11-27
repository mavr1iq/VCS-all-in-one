package vcs.repository.adapters;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import vcs.repository.classes.*;
import vcs.repository.factories.DaoFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GitAdapter implements VcsAdapter {
    DaoFactory daoFactory;

    public GitAdapter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    private Git openRepo(Repository repo) throws IOException {
        return Git.open(new File(repo.getUrl())); // repo.getUrl() повертає шлях до папки (напр. "C:/my_repos/repo1")
    }

    @Override
    public void createRepository(Repository repo) {
        File repoDir = new File(repo.getUrl());

        File directory = new File(repo.getUrl());
        if (directory.exists()) {
            if (directory.isDirectory() && new File(directory, ".git").exists()) {
                System.out.println("У папці '" + repo.getUrl() + "' вже існує ініціалізований Git-репозиторій");
                return;
            }
        } else {
            repoDir.mkdir();
            System.out.println("Папки за шляхом '" + repo.getUrl() + "' не існує, створюємо");
            createRepository(repo);
        }

        try {
            try (Git git = Git.init().setDirectory(repoDir).call()) {

                git.getRepository().getConfig().setString(
                        "receive", null, "denyCurrentBranch", "updateInstead"
                );
                git.getRepository().getConfig().save();

                System.out.println("Created a new repository at " + git.getRepository().getDirectory());
            }
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to initialize Git repository", e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        addToDb(repo);
    }

    // 1. COMMIT
    @Override
    public Commit commit(Repository repo, User user, String message) {
        try (Git git = openRepo(repo)) {

            // аналог 'git add .'
            git.add().addFilepattern(".").call();

            RevCommit rev = git.commit()
                    .setMessage(message)
                    .setAuthor(user.getUsername(), user.getEmail())
                    .call();

            addToDb(repo);

            String branchName = git.getRepository().getBranch();

            System.out.printf("Commit to %s%n", rev);
            Repository repository = daoFactory.getRepositoryDao().getByPath(repo.getUrl());
            Branch branch = daoFactory.getBranchDao().findByRepoIdAndName(repository.getRepo_id(), branchName);
            Commit commit = new Commit();
            commit.setBranch_Id(branch.getBranch_id()).setMessage(message).setAuthor_id(user.getId());

            if (rev.getParentCount() > 1) {
                String msg = rev.getParent(0).getFullMessage();
                Commit parentCommit = daoFactory.getCommitDao().getCommitByMessageAndBranchId(new Commit().setMessage(msg).setBranch_Id(branch.getBranch_id()));
                commit.setParent_commit(parentCommit.getCommit_Id());
            }

            Commit sameCommit = daoFactory.getCommitDao().getCommitByMessageAndBranchId(new Commit().setMessage(message).setBranch_Id(branch.getBranch_id()));

            if (sameCommit == null) {
                daoFactory.getCommitDao().add(commit);
            } else {
                System.out.println("Commit already exists");
            }

            return convertToDomainCommit(rev);
        } catch (Exception e) {
            throw new RuntimeException("Commit failed", e);
        }
    }

    // 2. CHECKOUT
    @Override
    public void checkout(Repository repo, Branch branch) {
        try (Git git = openRepo(repo)) {
            git.checkout().setName(branch.getName()).call();
            System.out.printf("Checkout to %s%n", branch.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Repository repo, String revision) {
        checkout(repo, new Branch().setRepo_id(repo.getRepo_id()).setName(revision).setCreated_by(repo.getOwner_id()));
    }

    @Override
    public String getGraphText(Repository repo) {
        File repoDir = new File(repo.getUrl());
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "git", "log", "--graph", "--all", "--decorate", "--oneline"
            );
            builder.directory(repoDir);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            return output;

        } catch (Exception e) {
            return "Error generating graph: " + e.getMessage();
        }
    }

    // 3. PUSH (P2P: Локально до іншої папки)
    @Override
    public boolean push(Repository repo, String remotePath) {
        File directory = new File(remotePath);
        if (directory.exists()) {
            if (directory.isDirectory() && !new File(directory, ".git").exists()) {
                throw new RuntimeException("У папці '" + remotePath + "' неіснує ініціалізованого Git-репозиторію");
            }
        } else {
            throw new RuntimeException("Папки за шляхом '" + repo.getUrl() + "' не існує");
        }

        try (Git git = openRepo(repo)) {
            // remotePath - це шлях до іншого локального репозиторію
            git.push()
                    .setRemote(remotePath)
                    .setPushAll()
                    .call();

            reset(remotePath);

            System.out.printf("Pushed to %s%n", remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. PULL (P2P: Локально з іншої папки)
    @Override
    public boolean pull(Repository repo, String remotePath) {
        File directory = new File(remotePath);
        if (directory.exists()) {
            if (directory.isDirectory() && !new File(directory, ".git").exists()) {
                throw new RuntimeException("У папці '" + remotePath + "' неіснує ініціалізованого Git-репозиторію");
            }
        } else {
            throw new RuntimeException("Папки за шляхом '" + repo.getUrl() + "' не існує");
        }

        try (Git git = openRepo(repo)) {
            String currentBranch = git.getRepository().getBranch();
            RefSpec spec = new RefSpec("refs/heads/" + currentBranch);
            git.fetch()
                    .setRemote(remotePath)
                    .setRefSpecs(spec)
                    .call();

            ObjectId fetchHead = git.getRepository().resolve("FETCH_HEAD");

            if (fetchHead == null) {
                System.out.println("Nothing to merge from " + remotePath);
                return true;
            }

            git.merge()
                    .include(fetchHead)
                    .call();

            System.out.printf("Pull from %s%n", remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. FETCH
    public void fetch(Repository repo, String remotePath) {
        try (Git git = openRepo(repo)) {
            git.fetch()
                    .setRemote(remotePath)
                    .call();

            System.out.printf("Fetch from %s%n", remotePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 6. LIST (Файли в робочій директорії)
    public List<String> list(Repository repo, String ref) {
        List<String> filePaths = new ArrayList<>();

        try (Git git = openRepo(repo)) {
            org.eclipse.jgit.lib.Repository jgitRepo = git.getRepository();

            String targetRef = (ref == null || ref.isEmpty()) ? "HEAD" : ref;

            ObjectId treeId = jgitRepo.resolve(targetRef + "^{tree}");

            if (treeId == null) {
                System.out.println("Ref not found: " + targetRef);
                return filePaths;
            }

            // аналог ls-tree
            try (TreeWalk treeWalk = new TreeWalk(jgitRepo)) {

                treeWalk.addTree(treeId);

                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    filePaths.add(treeWalk.getPathString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePaths;
    }

    // 7. LOG
    @Override
    public List<Commit> getCommits(Repository repo) {
        List<Commit> commits = new ArrayList<>();
        try (Git git = openRepo(repo)) {
            Iterable<RevCommit> logs = git.log().all().call();
            for (RevCommit rev : logs) {
                commits.add(convertToDomainCommit(rev));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commits;
    }

    // 8. PATCH
    public void applyPatch(Repository repo, File patchFile) {
        try (Git git = openRepo(repo);
             FileInputStream in = new FileInputStream(patchFile)) {
            git.apply().setPatch(in).call();
            System.out.printf("Applied patch to to %s%n", patchFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 9. BRANCH (Створення та список)
    @Override
    public List<Branch> getBranches(Repository repo) {
        List<Branch> branches = new ArrayList<>();
        try (Git git = openRepo(repo)) {
            List<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                System.out.println(ref.getName());
                Branch b = new Branch();
                b.setName(ref.getName().replace("refs/heads/", ""));
                branches.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return branches;
    }


    public void createBranch(Repository repo, Branch branch) {
        try (Git git = openRepo(repo)) {
            git.branchCreate().setName(branch.getName()).call();
            System.out.printf("Created branch %s%n", branch.getName());

            Repository repoDb = daoFactory.getRepositoryDao().getByName(repo.getName());
            Branch branchDb = daoFactory.getBranchDao().findByRepoIdAndName(repoDb.getRepo_id(), branch.getName());

            if(branchDb == null) {
                daoFactory.getBranchDao().add(branch);
            }
        } catch (Exception e) {
            System.out.println("Branch alredy exists");
            e.printStackTrace();
        }
    }

    // 10. MERGE
    @Override
    public MergeResults merge(Repository repo, Branch sourceBranch, Branch targetBranch) {
        try (Git git = openRepo(repo)) {
            // 1. Checkout на target
            git.checkout().setName(targetBranch.getName()).call();
            // 2. Merge source в target
            Ref sourceRef = git.getRepository().findRef(sourceBranch.getName());

            Set<String> conflicting = git.status().call().getConflicting();

            if (!conflicting.isEmpty()) {
                List<FileConflict> files = new ArrayList<>();
                System.out.println("Conflicting files: " + conflicting);

                for (String el: conflicting) {
                    files.add(new FileConflict(el, "Merge conflict"));
                }

                return new MergeResults(false, files);
            }

            org.eclipse.jgit.api.MergeResult result = git.merge().include(sourceRef).call();

            System.out.printf("Merge from %s to %s", sourceBranch, targetBranch);

            if (result.getConflicts() == null) {
                return new MergeResults(true, List.of());
            } else {
                System.out.println(result.getFailingPaths());
                return new MergeResults(false, List.of(new FileConflict("unknown", "Merge conflict")));
            }
        } catch (Exception e) {
            return new MergeResults(false, List.of());
        }
    }

    // 11. TAG
    public void tag(Repository repo, String tagName, String message) {
        try (Git git = openRepo(repo)) {
            git.tag().setName(tagName).setMessage(message).call();

            System.out.printf("Created tag %s to %s", tagName, repo.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 12. CLONE
    @Override
    public void cloneRepository(Repository sourceRepo, String targetFolder) {
        File destinationDir = new File(targetFolder);

        if (!destinationDir.exists()) {
            System.out.println("Папки за шляхом '" + targetFolder + "' не існує, створюємо");
            if (destinationDir.mkdir()) {
                System.out.println("Папка створена");
            } else {
                throw new RuntimeException("Не вдалося створити папку");
            }
        }
        try {
            try (Git git = Git.cloneRepository()
                    .setURI(sourceRepo.getUrl())
                    .setDirectory(destinationDir)
                    .call()) {

                git.getRepository().getConfig().setString(
                        "receive", null, "denyCurrentBranch", "updateInstead"
                );
                git.getRepository().getConfig().save();

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone repository", e);
        }
    }

    private Commit convertToDomainCommit(RevCommit rev) {
        Commit c = new Commit();
        c.setMessage(rev.getFullMessage());
        c.setCommit_Id(rev.getId().hashCode());
        return c;
    }

    private boolean addToDb(Repository repo) {
        if (daoFactory.getRepositoryDao().getByPath(repo.getUrl()) == null) {
            Branch branch = new Branch();
            daoFactory.getRepositoryDao().add(repo);
            Repository repository = daoFactory.getRepositoryDao().getByName(repo.getName());
            branch.setName("master").setRepo_id(repository.getRepo_id()).setCreated_by(repository.getOwner_id());
            daoFactory.getBranchDao().add(branch);
            return true;
        } else {
            System.out.printf("Репозиторій за шляхом '%s' вже є у базі даних, не створюємо новий запис", repo.getUrl());
            return false;
        }
    }

    private void reset(String remotePath) {
        try (Git git = openRepo(new Repository().setUrl(remotePath))) {

            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("HEAD")
                    .call();

        } catch (Exception e) {
            throw new RuntimeException("Failed to perform hard reset", e);
        }
    }
}