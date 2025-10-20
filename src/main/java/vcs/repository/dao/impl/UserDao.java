package vcs.repository.dao.impl;

import vcs.repository.VcsType;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.Repository;
import vcs.repository.classes.User;
import vcs.repository.dao.GeneralDao;
import vcs.repository.dao.db.DatabaseContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao implements GeneralDao<User> {
    private final DatabaseContext dbContext;

    public UserDao(DatabaseContext dbContext) {
        this.dbContext = dbContext;
    }

    @Override
    public User getById(int id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> getAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    users.add(user);
                }
                return users;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Repository> getAllReposById(int id) {
        String sql = "Select * FROM repositories WHERE owner_id = ?";
        List<Repository> repos = new ArrayList<>();
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Repository repo = new Repository();
                    repo.setRepo_id(rs.getInt("repo_id"));
                    repo.setName(rs.getString("name"));
                    repo.setUrl(rs.getString("url"));
                    repo.setType((VcsType) rs.getObject("type"));
                    repo.setOwner_id(rs.getInt("owner_id"));
                    repo.setDescription(rs.getString("description"));
                    repos.add(repo);
                }
                return repos;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Commit> getAllCommitsById(int id) {
        String sql = "Select * FROM commits WHERE commits.author_id = ?";
        List<Commit> commits = new ArrayList<>();
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commit commit = new Commit();
                    commit.setParent_commit(rs.getInt("parent_commit"));
                    commit.setAuthor_id(rs.getInt("author_id"));
                    commit.setCommit_Id(rs.getInt("commit_id"));
                    commit.setMessage(rs.getString("message"));
                    commit.setBranch_Id(rs.getInt("branch_id"));
                    commits.add(commit);
                }
                return commits;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Branch> getAllBranchesById(int id) {
        String sql = "SELECT * FROM branches WHERE created_by = ?";
        List<Branch> branches = new ArrayList<>();
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Branch branch = new Branch();
                    branch.setBranch_id(rs.getInt("branch_id"));
                    branch.setName(rs.getString("name"));
                    branch.setCreated_by(rs.getInt("created_by"));
                    branch.setRepo_id(rs.getInt("repo_id"));
                    branches.add(branch);
                }
                return branches;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void add(User user) {
        String sql = "INSERT INTO users(username, email, password_hash) VALUES(?, ?, ?)";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword_hash());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ? WHERE user_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword_hash());
            ps.setInt(4, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(User user) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
