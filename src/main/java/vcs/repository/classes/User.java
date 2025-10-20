package vcs.repository.classes;

public class User {
    private int id;
    private String username;
    private String email;
    private String password_hash;

    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() { return email; }
    public String getPassword_hash() {
        return password_hash;
    }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword_hash(String password_hash) { this.password_hash = password_hash; }
}
