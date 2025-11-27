package vcs.repository.adapters;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import java.util.Collections;
import vcs.repository.classes.*;
import vcs.repository.factories.DaoFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SvnAdapter implements VcsAdapter {
    private final DaoFactory daoFactory;
    private final DefaultSVNOptions options;

    public SvnAdapter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        this.options = SVNWCUtil.createDefaultOptions(true);
        FSRepositoryFactory.setup();
    }

    private SVNClientManager getClientManager(File workingCopy) {
        return SVNClientManager.newInstance(options, (ISVNAuthenticationManager) null);
    }

    @Override
    public void createRepository(Repository repo) {
        File repoDir = new File(repo.getUrl());

        if (!repoDir.exists()) {
            repoDir.mkdir();
            System.out.println("Папки за шляхом '" + repo.getUrl() + "' не існує, створюємо");
        }

        if (new File(repoDir, ".svn").exists()) {
            throw new RuntimeException("У папці '" + repo.getUrl() + "' вже існує SVN робоча копія");
        }

        try {
            File serverDir = new File(repoDir, "repo_server");
            File workingDir = new File(repoDir, "repo_working");

            if (!serverDir.exists()) serverDir.mkdirs();
            if (!workingDir.exists()) workingDir.mkdirs();

            SVNURL repoURL = SVNRepositoryFactory.createLocalRepository(serverDir, true, false);

            SVNClientManager clientManager = getClientManager(workingDir);
            SVNCommitClient commitClient = clientManager.getCommitClient();

            commitClient.doMkDir(
                    new SVNURL[]{
                            repoURL.appendPath("trunk", false),
                            repoURL.appendPath("branches", false),
                            repoURL.appendPath("tags", false)
                    },
                    "Initial repository structure"
            );

            clientManager.getUpdateClient().doCheckout(
                    repoURL.appendPath("trunk", false),
                    workingDir,
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false
            );

            clientManager.dispose();

            System.out.println("Created SVN repository at " + repoURL);
            System.out.println("Working copy at " + workingDir.getAbsolutePath());

            // Зберігаємо шлях до робочої копії
            repo.setUrl(workingDir.getAbsolutePath());
            addToDb(repo);

        } catch (SVNException e) {
            throw new RuntimeException("Failed to create SVN repository", e);
        }
    }

    @Override
    public Commit commit(Repository repo, User user, String message) {
        File workingCopy = new File(repo.getUrl());
        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            SVNCommitClient commitClient = clientManager.getCommitClient();

            clientManager.getWCClient().doAdd(
                    workingCopy,
                    true,
                    false,
                    false,
                    SVNDepth.INFINITY,
                    false,
                    false
            );

            // Commit
            SVNCommitInfo commitInfo = commitClient.doCommit(
                    new File[]{workingCopy},
                    false,
                    message,
                    null,
                    null,
                    false,
                    false,
                    SVNDepth.INFINITY
            );

            System.out.printf("SVN Commit revision: %d%n", commitInfo.getNewRevision());

            addToDb(repo);

            Repository repository = daoFactory.getRepositoryDao().getByPath(repo.getUrl());
            Branch branch = daoFactory.getBranchDao().findByRepoIdAndName(repository.getRepo_id(), "trunk");

            Commit commit = new Commit();
            commit.setBranch_Id(branch.getBranch_id())
                    .setMessage(message)
                    .setAuthor_id(user.getId());

            Commit sameCommit = daoFactory.getCommitDao().getCommitByMessageAndBranchId(commit);
            if (sameCommit == null) {
                daoFactory.getCommitDao().add(commit);
            } else {
                System.out.println("Commit already exists");
            }

            commit.setCommit_Id(daoFactory.getCommitDao().getCommitByMessageAndBranchId(commit).getCommit_Id());
            return commit;

        } catch (SVNException e) {
            throw new RuntimeException("SVN commit failed", e);
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    // Аналог Pull але з рухом по історії
    public void update(Repository repo, String revision) {
        File workingCopy = new File(repo.getUrl());
        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            SVNRevision svnRevision = SVNRevision.parse(revision);
            clientManager.getUpdateClient().doUpdate(
                    workingCopy,
                    svnRevision,
                    SVNDepth.INFINITY,
                    false,
                    false
            );
            System.out.printf("Updated to revision %s%n", revision);
        } catch (SVNException e) {
            throw new RuntimeException("SVN update failed", e);
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    public String getGraphText(Repository repo) {
        StringBuilder sb = new StringBuilder();
        sb.append("SVN does not support Branch Graphs (Linear History):\n\n");

        List<Commit> commits = getCommits(repo);
        int limit = Math.min(commits.size(), 50);

        for (int i = 0; i < limit; i++) {
            Commit c = commits.get(i);
            sb.append(String.format("* r%d | %s\n", c.getCommit_Id(), c.getMessage()));
            sb.append("  |\n");
        }
        sb.append("  (end of history)");
        return sb.toString();
    }

    @Override
    public boolean push(Repository repo, String remotePath) {
        System.out.println("SVN використовує централізовану модель - push не потрібен");
        return true;
    }

    @Override
    public boolean pull(Repository repo, String remotePath) {
        try {
            update(repo, "HEAD");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void fetch(Repository repo, String remotePath) {
        System.out.println("SVN автоматично отримує інформацію при update");
        update(repo, "HEAD");
    }

    @Override
    public List<String> list(Repository repo, String ref) {
        List<String> files = new ArrayList<>();
        File workingCopy = new File(repo.getUrl());

        if (!workingCopy.exists()) {
            return files;
        }

        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            SVNStatusClient statusClient = clientManager.getStatusClient();

            statusClient.doStatus(
                    workingCopy,
                    SVNRevision.WORKING,
                    SVNDepth.INFINITY,
                    false,
                    true,
                    false,
                    false,
                    status -> {
                        if (status.getFile().isDirectory()) return;

                        SVNStatusType s = status.getContentsStatus();

                        if (s != SVNStatusType.STATUS_DELETED && s != SVNStatusType.STATUS_IGNORED) {
                            files.add(status.getFile().getName());
                        }
                    },
                    null
            );
        } catch (SVNException e) {
            e.printStackTrace();
        } finally {
            clientManager.dispose();
        }

        return files;
    }

    @Override
    public List<Commit> getCommits(Repository repo) {
        List<Commit> commits = new ArrayList<>();

        File serverCopy = new File(repo.getUrl().replace("repo_working", "repo_server"));

        try {
            SVNURL repoURL = SVNURL.fromFile(serverCopy);
            SVNRepository repository = SVNRepositoryFactory.create(repoURL);

            Collection logEntries = repository.log(
                    new String[]{""},
                    null,
                    0,
                    -1,
                    true,
                    true
            );

            for (Object obj : logEntries) {
                SVNLogEntry logEntry = (SVNLogEntry) obj;
                Commit commit = new Commit();
                commit.setCommit_Id((int) logEntry.getRevision());
                commit.setMessage(logEntry.getMessage());
                commits.add(commit);
            }

        } catch (SVNException e) {
            e.printStackTrace();
        }

        return commits;
    }

    @Override
    public void applyPatch(Repository repo, File patchFile) {
        File workingCopy = new File(repo.getUrl());
        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            clientManager.getDiffClient().doPatch(
                    patchFile,
                    workingCopy,
                    false,
                    0, false, false, false
            );
            System.out.println("Applied patch from " + patchFile.getName());
        } catch (SVNException e) {
            e.printStackTrace();
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    public List<Branch> getBranches(Repository repo) {
        List<Branch> branches = new ArrayList<>();

        Branch trunk = new Branch();
        trunk.setName("trunk");
        branches.add(trunk);

        File serverDir = new File(repo.getUrl().replace("repo_working", "repo_server"));

        SVNClientManager clientManager = SVNClientManager.newInstance();

        try {
            SVNURL repoRootURL = SVNURL.fromFile(serverDir);
            SVNURL branchesURL = repoRootURL.appendPath("branches", false);

            System.out.println("Listing branches from: " + branchesURL);

            clientManager.getLogClient().doList(
                    branchesURL,
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    false,
                    SVNDepth.IMMEDIATES,
                    SVNDirEntry.DIRENT_ALL,
                    (SVNDirEntry entry) -> {
                        if (entry.getName().isEmpty()) return;

                        if (entry.getKind() == SVNNodeKind.DIR) {
                            Branch branch = new Branch();
                            branch.setName("branches/" + entry.getName());
                            branches.add(branch);
                        }
                    }
            );

        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode().getCode() == 160013) {
                System.out.println("No 'branches' directory found on server yet.");
            } else {
                e.printStackTrace();
            }
        } finally {
            clientManager.dispose();
        }

        return branches;
    }

    @Override
    public void createBranch(Repository repo, Branch branch) {
        File serverCopy = new File(repo.getUrl().replace("repo_working", "repo_server"));

        SVNClientManager clientManager = SVNClientManager.newInstance();

        try {
            SVNURL repoRootURL = SVNURL.fromFile(serverCopy);

            SVNURL sourceURL = repoRootURL.appendPath("trunk", false);
            SVNURL targetURL = repoRootURL.appendPath("branches/" + branch.getName(), false);

            System.out.println("Source: " + sourceURL);
            System.out.println("Target: " + targetURL);

            SVNCopySource copySource = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, sourceURL);

            clientManager.getCopyClient().doCopy(
                    new SVNCopySource[]{copySource},
                    targetURL,
                    false,
                    true,
                    false,
                    "Created branch: " + branch.getName(),
                    null
            );

            System.out.printf("Created SVN branch: %s%n", branch.getName());

            Repository repoDb = daoFactory.getRepositoryDao().getByName(repo.getName());
            Branch branchDb = daoFactory.getBranchDao().findByRepoIdAndName(repoDb.getRepo_id(), branch.getName());

            if (branchDb == null) {
                daoFactory.getBranchDao().add(branch);
            }

        } catch (SVNException e) {
            System.out.println("Branch creation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    public MergeResults merge(Repository repo, Branch sourceBranch, Branch targetBranch) {
        File workingCopy = new File(repo.getUrl());
        File serverDir = new File(repo.getUrl().replace("repo_working", "repo_server"));

        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            SVNURL repoRootURL = SVNURL.fromFile(serverDir);

            SVNURL sourceURL = getBranchURL(repoRootURL, sourceBranch.getName());
            SVNURL targetURL = getBranchURL(repoRootURL, targetBranch.getName());

            System.out.println("Merging FROM: " + sourceURL);
            System.out.println("Merging INTO WC (switched to): " + targetURL);

            clientManager.getUpdateClient().doSwitch(
                    workingCopy,
                    targetURL,
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    false
            );

            SVNRevisionRange range = new SVNRevisionRange(SVNRevision.create(1), SVNRevision.HEAD);

            Collection<org.tmatesoft.svn.core.wc.SVNRevisionRange> ranges = Collections.singletonList(range);

            clientManager.getDiffClient().doMerge(
                    sourceURL,
                    SVNRevision.HEAD,
                    ranges,
                    workingCopy,
                    SVNDepth.INFINITY,
                    true,
                    false,
                    false,
                    false
            );

            List<FileConflict> conflicts = new ArrayList<>();
            clientManager.getStatusClient().doStatus(
                    workingCopy,
                    SVNRevision.WORKING,
                    SVNDepth.INFINITY,
                    false,
                    false,
                    false,
                    false,
                    status -> {
                        if (status.getContentsStatus() == SVNStatusType.STATUS_CONFLICTED) {
                            conflicts.add(new FileConflict(
                                    status.getFile().getPath(),
                                    "SVN merge conflict"
                            ));
                        }
                    },
                    null
            );

            System.out.printf("Merged from %s to %s%n", sourceBranch.getName(), targetBranch.getName());
            System.out.println("Make commit to save changes to trunk");
            return new MergeResults(conflicts.isEmpty(), conflicts);

        } catch (SVNException e) {
            e.printStackTrace();
            return new MergeResults(false, List.of(new FileConflict("unknown", "Merge failed: " + e.getMessage())));
        } finally {
            clientManager.dispose();
        }
    }

    // Допоміжний метод для формування URL гілки
    private SVNURL getBranchURL(SVNURL root, String branchName) throws SVNException {
        if (branchName.equalsIgnoreCase("trunk") || branchName.equalsIgnoreCase("master")) {
            return root.appendPath("trunk", false);
        } else {
            String path = branchName.startsWith("branches/") ? branchName : "branches/" + branchName;
            return root.appendPath(path, false);
        }
    }

    @Override
    public void tag(Repository repo, String tagName, String message) {
        File serverDir = new File(repo.getUrl().replace("repo_working", "repo_server"));

        SVNClientManager clientManager = SVNClientManager.newInstance();

        try {
            SVNURL repoRootURL = SVNURL.fromFile(serverDir);

            SVNURL sourceURL = repoRootURL.appendPath("trunk", false);
            SVNURL tagURL = repoRootURL.appendPath("tags/" + tagName, false);

            System.out.println("Tagging source: " + sourceURL);
            System.out.println("Tagging dest: " + tagURL);

            SVNCopySource copySource = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, sourceURL);

            clientManager.getCopyClient().doCopy(
                    new SVNCopySource[]{copySource},
                    tagURL,
                    false,
                    true,
                    false,
                    message,
                    null
            );

            System.out.printf("Created SVN tag: %s%n", tagName);

            // Тут можна додати запис в ActionLogDao

        } catch (SVNException e) {
            System.err.println("Error creating tag: " + e.getMessage());
            e.printStackTrace();
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    public void cloneRepository(Repository sourceRepo, String targetFolder) {
        File destinationDir = new File(targetFolder);

        if (!destinationDir.exists()) {
            System.out.println("Створюємо папку: " + targetFolder);
            if (!destinationDir.mkdirs()) {
                throw new RuntimeException("Не вдалося створити папку");
            }
        }

        SVNClientManager clientManager = getClientManager(destinationDir);

        try {
            SVNURL sourceURL = SVNURL.fromFile(new File(sourceRepo.getUrl().replace("/repo_working", "/repo_server")));

            clientManager.getUpdateClient().doCheckout(
                    sourceURL,
                    destinationDir,
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false
            );

            System.out.println("Cloned SVN repository to " + targetFolder);

        } catch (SVNException e) {
            throw new RuntimeException("Failed to clone SVN repository", e);
        } finally {
            clientManager.dispose();
        }
    }

    @Override
    public void checkout(Repository repo, Branch branch) {
        File workingCopy = new File(repo.getUrl());
        File serverDir = new File(repo.getUrl().replace("repo_working", "repo_server"));

        SVNClientManager clientManager = getClientManager(workingCopy);

        try {
            SVNURL repoRootURL = SVNURL.fromFile(serverDir);
            SVNURL targetURL;

            if (branch.getName().equalsIgnoreCase("trunk")){
                targetURL = repoRootURL.appendPath("trunk", false);
            } else {
                // Якщо ім'я вже містить "branches/", не дублюємо
                String path = branch.getName().startsWith("branches/") ? branch.getName() : "branches/" + branch.getName();
                targetURL = repoRootURL.appendPath(path, false);
            }

            System.out.println("Switching working copy to: " + targetURL);

            clientManager.getUpdateClient().doSwitch(
                    workingCopy,
                    targetURL,
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    false
            );

            System.out.printf("Successfully switched to branch: %s%n", branch.getName());

        } catch (SVNException e) {
            throw new RuntimeException("Failed to checkout/switch to branch: " + branch.getName(), e);
        } finally {
            clientManager.dispose();
        }
    }

    private boolean addToDb(Repository repo) {
        if (daoFactory.getRepositoryDao().getByPath(repo.getUrl()) == null) {
            Branch branch = new Branch();
            daoFactory.getRepositoryDao().add(repo);
            Repository repository = daoFactory.getRepositoryDao().getByName(repo.getName());
            branch.setName("trunk")
                    .setRepo_id(repository.getRepo_id())
                    .setCreated_by(repository.getOwner_id());
            daoFactory.getBranchDao().add(branch);
            return true;
        } else {
            System.out.printf("Репозиторій за шляхом '%s' вже є у базі даних%n", repo.getUrl());
            return false;
        }
    }
}