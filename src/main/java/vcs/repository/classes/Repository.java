package vcs.repository.classes;

import vcs.repository.VcsType;

import java.time.LocalDateTime;

public class Repository {
    private int repo_id;
    private int owner_id;
    private String name;
    private VcsType type;
    private String url;
    private String description;

    public int getRepo_id() { return repo_id; }
    public String getName() { return name; }
    public int getOwner_id() { return owner_id; }
    public VcsType getType() { return type; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }

    public void setRepo_id(int repo_id) { this.repo_id = repo_id; }
    public void setName(String name) { this.name = name; }
    public void setOwner_id(int owner_id) { this.owner_id = owner_id; }
    public void setType(VcsType type) { this.type = type; }
    public void setUrl(String url) { this.url = url; }
    public void setDescription(String description) { this.description = description; }
}
