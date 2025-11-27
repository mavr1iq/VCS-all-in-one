package vcs.repository.adapters;

import vcs.repository.classes.*;
import vcs.repository.factories.DaoFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MercurialAdapter implements VcsAdapter {
    private final DaoFactory daoFactory;

    public MercurialAdapter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    private List<String> runCommand(File workingDir, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDir != null) {
                builder.directory(workingDir);
            }
            builder.redirectErrorStream(true);

            Process process = builder.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0 && exitCode != 1) {
                System.err.println("HG Command Output: " + output);
                throw new RuntimeException("Mercurial command failed (Code " + exitCode + "): " + String.join(" ", command));
            }

            return output;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute Mercurial command", e);
        }
    }

    @Override
    public void createRepository(Repository repo) {
        File repoDir = new File(repo.getUrl());

        if (!repoDir.exists()) {
            repoDir.mkdirs();
        }

        if (new File(repoDir, ".hg").exists()) {
            System.out.println("Mercurial репозиторій вже існує в " + repo.getUrl());
            return;
        }

        runCommand(repoDir, "hg", "init");

        try {
            new File(repoDir, ".hgignore").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Created Mercurial repo at " + repoDir);
        addToDb(repo);
    }

    @Override
    public Commit commit(Repository repo, User user, String message) {
        File repoDir = new File(repo.getUrl());

        List<String> branchOutput = runCommand(repoDir, "hg", "branch");
        System.out.println(branchOutput);
        String currentBranch = branchOutput.isEmpty() ? "unknown" : branchOutput.getFirst().trim();
        System.out.println("Committing to Mercurial branch: " + currentBranch);

        runCommand(repoDir, "hg", "addremove");

        runCommand(repoDir, "hg", "commit", "-m", message, "-u", user.getUsername());

        addToDb(repo);

        return getLastCommit(repo);
    }

    @Override
    public void checkout(Repository repo, Branch branch) {
        File repoDir = new File(repo.getUrl());

        runCommand(repoDir, "hg", "update", "--clean", branch.getName());

        System.out.println("Checkout to Mercurial branch: " + branch.getName());
    }

    @Override
    public void update(Repository repo, String revision) {
        checkout(repo, new Branch().setRepo_id(repo.getRepo_id()).setName(revision).setCreated_by(repo.getOwner_id()));
    }

    @Override
    public String getGraphText(Repository repo) {
        File repoDir = new File(repo.getUrl());
        try {
            List<String> lines = runCommand(repoDir, "hg", "log", "-G", "--template", "{label('log.graph', graph)} {rev} {desc|firstline} [{author|user}]\n");
            return String.join("\n", lines);
        } catch (Exception e) {
            return "Error generating graph: " + e.getMessage();
        }
    }

    @Override
    public boolean push(Repository repo, String remotePath) {
        File repoDir = new File(repo.getUrl());
        try {

            runCommand(repoDir, "hg", "push", "--new-branch", remotePath);

            File remoteDir = new File(remotePath);
            if (remoteDir.exists()) {
                runCommand(remoteDir, "hg", "update");
            }

            System.out.println("Pushed to " + remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean pull(Repository repo, String remotePath) {
        File repoDir = new File(repo.getUrl());
        try {
            // hg pull <source>
            runCommand(repoDir, "hg", "pull", remotePath);
            // Після pull треба зробити update, щоб побачити зміни у файлах
            runCommand(repoDir, "hg", "update");

            System.out.println("Pulled and updated from " + remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void fetch(Repository repo, String remotePath) {
        // У Mercurial 'pull' без 'update' - це і є fetch
        File repoDir = new File(repo.getUrl());
        runCommand(repoDir, "hg", "pull", remotePath);
        System.out.println("Fetched from " + remotePath);
    }

    @Override
    public List<String> list(Repository repo, String ref) {
        File repoDir = new File(repo.getUrl());

        List<String> args = new ArrayList<>(Arrays.asList("hg", "files"));
        if (ref != null && !ref.isEmpty()) {
            args.add("-r");
            args.add(ref);
        }

        return runCommand(repoDir, args.toArray(new String[0]));
    }

    @Override
    public List<Commit> getCommits(Repository repo) {
        File repoDir = new File(repo.getUrl());
        List<Commit> commits = new ArrayList<>();

        String template = "{rev}###{author}###{p1rev}###{desc}\\n";

        List<String> lines = runCommand(repoDir, "hg", "log", "--template", template);

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split("###", 4);
            if (parts.length >= 4) {
                Commit c = new Commit();
                try {
                    c.setCommit_Id(Integer.parseInt(parts[0])); // Hg local revision number
                    c.setAuthor_id(repo.getOwner_id()); // Автор (хеш)

                    int parentRev = Integer.parseInt(parts[2]);
                    if (parentRev >= 0) {
                        c.setParent_commit(parentRev);
                    }

                    c.setMessage(parts[3]);
                    commits.add(c);
                } catch (NumberFormatException e) {
                }
            }
        }
        return commits;
    }

    @Override
    public void applyPatch(Repository repo, File patchFile) {
        File repoDir = new File(repo.getUrl());
        // hg import <file>
        runCommand(repoDir, "hg", "import", "--no-commit", patchFile.getAbsolutePath());
    }

    @Override
    public List<Branch> getBranches(Repository repo) {
        File repoDir = new File(repo.getUrl());
        List<Branch> branches = new ArrayList<>();

        List<String> output = runCommand(repoDir, "hg", "branches");

        for (String line : output) {
            String[] parts = line.split("\\s+"); // Розбиваємо по пробілах
            if (parts.length > 0) {
                Branch b = new Branch();
                b.setName(parts[0]); // Назва гілки
                branches.add(b);
            }
        }

        if (branches.stream().noneMatch(b -> b.getName().equals("default"))) {
            Branch defaultBranch = new Branch();
            defaultBranch.setName("default");
            branches.add(defaultBranch);
        }
        return branches;
    }

    @Override
    public void createBranch(Repository repo, Branch branch) {
        File repoDir = new File(repo.getUrl());

        runCommand(repoDir, "hg", "branch", branch.getName());

        runCommand(repoDir, "hg", "commit", "-m", "Create branch " + branch.getName(), "-u", "system");

        System.out.println("Created Mercurial branch: " + branch.getName());

        Repository repoDb = daoFactory.getRepositoryDao().getByName(repo.getName());
        if (repoDb != null) {
            Branch newBranch = new Branch();
            newBranch.setName(branch.getName());
            newBranch.setRepo_id(repoDb.getRepo_id());
            newBranch.setCreated_by(repoDb.getOwner_id());

            if (daoFactory.getBranchDao().findByRepoIdAndName(repoDb.getRepo_id(), newBranch.getName()) == null) {
                daoFactory.getBranchDao().add(newBranch);
            }
        }
    }

    @Override
    public MergeResults merge(Repository repo, Branch sourceBranch, Branch targetBranch) {
        File repoDir = new File(repo.getUrl());
        try {
            // 1. Перемикаємось на target (наприклад, default)
            runCommand(repoDir, "hg", "update", targetBranch.getName());

            // 2. Мерджимо source
            // hg merge <source>
            runCommand(repoDir, "hg", "merge", sourceBranch.getName());

            // 3. Комітимо результат мерджу
            runCommand(repoDir, "hg", "commit", "-m", "Merge " + sourceBranch.getName(), "-u", "system");

            System.out.println("Merged " + sourceBranch.getName() + " into " + targetBranch.getName());
            return new MergeResults(true, List.of());

        } catch (RuntimeException e) {
            List<String> resolveOutput = runCommand(repoDir, "hg", "resolve", "-l");
            List<FileConflict> conflicts = new ArrayList<>();

            for (String line : resolveOutput) {
                // Формат: U path/to/file (U = Unresolved)
                if (line.startsWith("U ")) {
                    conflicts.add(new FileConflict(line.substring(2), "Mercurial Merge Conflict"));
                }
            }

            if (conflicts.isEmpty()) {
                return new MergeResults(false, List.of(new FileConflict("unknown", e.getMessage())));
            }

            return new MergeResults(false, conflicts);
        }
    }

    @Override
    public void tag(Repository repo, String tagName, String message) {
        File repoDir = new File(repo.getUrl());
        // hg tag <name> -m <msg>
        runCommand(repoDir, "hg", "tag", tagName, "-m", message, "-u", "system");
        System.out.println("Created tag " + tagName);
    }

    @Override
    public void cloneRepository(Repository sourceRepo, String targetFolder) {
        File targetDir = new File(targetFolder);

        // hg clone <source> <dest>
        runCommand(null, "hg", "clone", sourceRepo.getUrl(), targetDir.getAbsolutePath());

        System.out.println("Cloned Mercurial repo to " + targetDir.getAbsolutePath());
        addToDb(sourceRepo.setUrl(targetFolder));
    }

    private boolean addToDb(Repository repo) {
        if (daoFactory.getRepositoryDao().getByPath(repo.getUrl()) == null) {
            Branch branch = new Branch();
            daoFactory.getRepositoryDao().add(repo);
            Repository repository = daoFactory.getRepositoryDao().getByName(repo.getName());
            branch.setName("default").setRepo_id(repository.getRepo_id()).setCreated_by(repository.getOwner_id());
            daoFactory.getBranchDao().add(branch);
            return true;
        }
        return false;
    }

    private Commit getLastCommit(Repository repo) {
        List<Commit> commits = getCommits(repo);
        // hg log за замовчуванням сортує від нових до старих, тому беремо першу
        if (!commits.isEmpty()) {
            return commits.getFirst();
        }
        return null;
    }
}