package vcs.repository.dao.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DatabaseContext {
    private final String url;

    public DatabaseContext() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/db.properties")) {
            props.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.url = props.getProperty("db.url");
        init();
    }
    private void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Users (
                    user_id SERIAL PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL
                    );""");
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Repositories (
                      repo_id SERIAL PRIMARY KEY,
                      owner_id INT REFERENCES Users(user_id) NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      url VARCHAR(255) NOT NULL,
                      description VARCHAR(255),
                      type vcs NOT NULL
                      );""");
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Branches (
                    branch_id SERIAL PRIMARY KEY,
                    created_by INT REFERENCES Users(user_id) NOT NULL,
                    repo_id INT REFERENCES Repositories NOT NULL,
                    name VARCHAR(100) NOT NULL
                    );""");
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Commits (
                    commit_id SERIAL PRIMARY KEY,
                    author_id INT REFERENCES Users(user_id),
                    branch_id INT REFERENCES Branches,
                    parent_commit INT REFERENCES Commits(commit_id),
                    message VARCHAR(255)
                    );""");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
