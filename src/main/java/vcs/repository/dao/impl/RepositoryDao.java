package vcs.repository.dao.impl;

import vcs.repository.VcsType;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Repository;
import vcs.repository.dao.GeneralDao;
import vcs.repository.dao.db.DatabaseContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepositoryDao implements GeneralDao<Repository> {
    private final DatabaseContext dbContext;

    public RepositoryDao(DatabaseContext dbContext) {
        this.dbContext = dbContext;
    }

    @Override
    public Repository getById(int id) {
        String sql = "SELECT * FROM repositories WHERE repo_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Repository repo = new Repository();
                    repo.setRepo_id(rs.getInt("repo_id"));
                    repo.setName(rs.getString("name"));
                    repo.setUrl(rs.getString("url"));
                    repo.setType((VcsType) rs.getObject("type"));
                    repo.setOwner_id(rs.getInt("owner_id"));
                    repo.setDescription(rs.getString("description"));
                    return repo;
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public List<Repository> getAll() {
        String sql = "SELECT * FROM repositories";
        List<Repository> repos = new ArrayList<>();

        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Branch> getAllBranchesById(int id) {
        String sql = "SELECT * FROM branches WHERE repo_id = ?";
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
    public void add(Repository repository) {
        String sql = "INSERT INTO repositories (name, url, type, owner_id, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repository.getName());
            ps.setString(2, repository.getUrl());
            ps.setString(3, String.valueOf(repository.getType()));
            ps.setInt(4, repository.getOwner_id());
            ps.setString(5, repository.getDescription());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Repository repository) {
        String sql = "UPDATE repositories SET name = ?, url = ?, type = ?, owner_id = ?, description = ? WHERE repo_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repository.getName());
            ps.setString(2, repository.getUrl());
            ps.setString(3, String.valueOf(repository.getType()));
            ps.setInt(4, repository.getOwner_id());
            ps.setString(5, repository.getDescription());
            ps.setInt(6, repository.getRepo_id());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(Repository repository) {
        String sql = "DELETE FROM repositories WHERE repo_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, repository.getRepo_id());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
