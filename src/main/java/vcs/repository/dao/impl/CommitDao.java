package vcs.repository.dao.impl;

import vcs.repository.classes.Commit;
import vcs.repository.dao.GeneralDao;
import vcs.repository.dao.db.DatabaseContext;
import vcs.repository.iterators.CommitHistoryIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommitDao implements GeneralDao<Commit> {
    private final DatabaseContext dbContext;

    public CommitDao(DatabaseContext dbContext) {
        this.dbContext = dbContext;
    }

    @Override
    public Commit getById(int id) {
        String sql = "SELECT * FROM commits WHERE commit_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Commit commit = new Commit();
                    commit.setParent_commit(rs.getInt("parent_commit"));
                    commit.setAuthor_id(rs.getInt("author_id"));
                    commit.setCommit_Id(rs.getInt("commit_id"));
                    commit.setMessage(rs.getString("message"));
                    commit.setBranch_Id(rs.getInt("branch_id"));
                    return commit;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Commit> getAll() {
        String sql = "SELECT * FROM commits";
        List<Commit> commits = new ArrayList<>();

        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public Iterable<Commit> getAllPreviousCommitsById(int id) {
        return () -> new CommitHistoryIterator(id, CommitDao.this);
    }

    @Override
    public void add(Commit commit) {
        String sql = "INSERT INTO commits(author_id, parent_commit, message, branch_id) VALUES(?, ?, ?, ?)";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commit.getAuthor_id());
            ps.setInt(2, commit.getParent_commit());
            ps.setString(3, commit.getMessage());
            ps.setInt(4, commit.getBranch_id());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Commit commit) {
        String sql = "UPDATE commits SET parent_commit = ?, message = ?, branch_id = ?, author_id = ? WHERE commit_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commit.getParent_commit());
            ps.setString(2, commit.getMessage());
            ps.setInt(3, commit.getBranch_id());
            ps.setInt(4, commit.getAuthor_id());
            ps.setInt(5, commit.getCommit_Id());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(Commit commit) {
        String sql = "DELETE FROM commits WHERE commit_id = ?";
        try (Connection conn = dbContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commit.getCommit_Id());
            ps.executeUpdate();
        }catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
