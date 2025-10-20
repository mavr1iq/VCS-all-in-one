package vcs.repository.classes;

public class Branch {
    private String name;
    private int branch_id;
    private int repo_id;
    private int created_by;

    public String getName() { return name; }
    public int getBranch_id() { return branch_id; }
    public int getCreated_by() { return created_by; }
    public int getRepo_id() { return repo_id; }

    public void setName(String name) { this.name = name; }
    public void setBranch_id(int branch_id) { this.branch_id = branch_id; }
    public void setCreated_by(int created_by) { this.created_by = created_by; }
    public void setRepo_id(int repoId) { this.repo_id = repoId; }
}