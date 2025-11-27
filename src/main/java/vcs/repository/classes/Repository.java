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

    public Repository setRepo_id(int repo_id) { this.repo_id = repo_id; return this; }
    public Repository setName(String name) { this.name = name; return this; }
    public Repository setOwner_id(int owner_id) { this.owner_id = owner_id; return this; }
    public Repository setType(VcsType type) { this.type = type; return this; }
    public Repository setUrl(String url) { this.url = url; return this; }
    public Repository setDescription(String description) { this.description = description; return this; }
}
