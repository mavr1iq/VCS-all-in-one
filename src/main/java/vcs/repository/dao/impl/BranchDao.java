package vcs.repository.dao.impl;

import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.dao.GeneralDao;
import vcs.repository.dao.db.DatabaseContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranchDao implements GeneralDao<Branch> {
    private final DatabaseContext dbContext;

    public BranchDao(DatabaseContext dbContext) {
        this.dbContext = dbContext;
    }

    @Override
    public Branch getById(int id) {
        String sql = "SELECT * FROM branches WHERE branch_id = ?";
        try (Connection conn = dbContext.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Branch result = new Branch();
                    result.setBranch_id(rs.getInt("branch_id"));
                    result.setName(rs.getString("name"));
                    result.setCreated_by(rs.getInt("created_by"));
                    result.setRepo_id(rs.getInt("repo_id"));
                    return result;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Branch> getAll() {
        String sql = "SELECT * FROM branches";
        List<Branch> result = new ArrayList<>();

        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Branch branch = new Branch();
                    branch.setBranch_id(rs.getInt("branch_id"));
                    branch.setName(rs.getString("name"));
                    branch.setCreated_by(rs.getInt("created_by"));
                    branch.setRepo_id(rs.getInt("repo_id"));
                    result.add(branch);
                }
                return result;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Commit> getAllCommitsById(int id) {
        String sql = "SELECT * FROM commits WHERE branch_id = ?";
        List<Commit> result = new ArrayList<>();
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commit commit = new Commit();
                    commit.setCommit_Id(rs.getInt("commit_id"));
                    commit.setBranch_Id(rs.getInt("branch_id"));
                    commit.setAuthor_id(rs.getInt("author_id"));
                    commit.setParent_commit(rs.getInt("parent_commit"));
                    commit.setMessage(rs.getString("message"));
                    result.add(commit);
                }
                return result;
            }
        }catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void add(Branch o) {
        String sql = "INSERT INTO branches(name, created_by, repo_id) VALUES(?, ?, ?)";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, o.getName());
            ps.setInt(2, o.getCreated_by());
            ps.setInt(3, o.getRepo_id());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Branch o) {
        String sql = "UPDATE branches SET name = ?, created_by = ?, repo_id = ? WHERE branch_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, o.getName());
            ps.setInt(2, o.getCreated_by());
            ps.setInt(3, o.getRepo_id());
            ps.setInt(4, o.getBranch_id());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(Branch o) {
        String sql = "DELETE FROM branches WHERE branch_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, o.getBranch_id());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Branch findByRepoIdAndName(int repo_id, String name) {
        String sql = "SELECT * FROM branches WHERE repo_id = ? AND name = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, repo_id);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Branch result = new Branch();
                    result.setBranch_id(rs.getInt("branch_id"));
                    result.setName(rs.getString("name"));
                    result.setCreated_by(rs.getInt("created_by"));
                    result.setRepo_id(rs.getInt("repo_id"));
                    return result;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
