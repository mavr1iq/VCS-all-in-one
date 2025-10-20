package vcs.repository.classes;

public class Commit {
    private int commit_id;
    private int branch_id;
    private String message;
    private int author_id;
    private int parent_commit;

    public int getCommit_Id() { return commit_id; }
    public String getMessage() { return message; }
    public int getAuthor_id() { return author_id; }
    public int getBranch_id() { return branch_id; }
    public int getParent_commit() { return parent_commit; }

    public void setCommit_Id(int commit_id) { this.commit_id = commit_id; }
    public void setBranch_Id(int branch_id) { this.branch_id = branch_id; }
    public void setMessage(String message) { this.message = message; }
    public void setAuthor_id(int author_id) { this.author_id = author_id; }
    public void setParent_commit(int parent_commit) { this.parent_commit = parent_commit; }

    @Override
    public String toString() {
        return "Commit [commit_id=" + commit_id + ", branch_id=" + branch_id + ", message=" + message + ", author_id=" + author_id + ", parent_commit=" + parent_commit + "]";
    }
}

