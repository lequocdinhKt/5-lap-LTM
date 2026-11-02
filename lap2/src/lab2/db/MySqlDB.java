package lab2.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class MySqlDB {
    private final Properties config = loadConfig();
    private boolean initialized;

    public synchronized void ensureReady() throws SQLException {
        if (initialized) {
            return;
        }
        String baseUrl = required("db.url");
        String database = config.getProperty("db.name", "lab2_ftp");
        try (Connection connection = DriverManager.getConnection(baseUrl, required("db.user"), required("db.password"));
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "") +
                    "` CHARACTER SET utf8mb4");
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      password VARCHAR(100) NOT NULL,
                      home_dir VARCHAR(255) NOT NULL
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transfer_logs (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL,
                      command VARCHAR(10) NOT NULL,
                      filename VARCHAR(500) NOT NULL,
                      bytes BIGINT NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )""");
        }
        seed("alice", "123456", "alice");
        seed("bob", "123456", "bob");
        initialized = true;
    }

    public String authenticate(String username, String password) throws SQLException {
        ensureReady();
        String sql = "SELECT home_dir FROM users WHERE username=? AND password=?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    public void logTransfer(String username, String command, String filename, long bytes) {
        try {
            ensureReady();
            String sql = "INSERT INTO transfer_logs(username,command,filename,bytes) VALUES(?,?,?,?)";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, command);
                statement.setString(3, filename);
                statement.setLong(4, bytes);
                statement.executeUpdate();
            }
        } catch (SQLException error) {
            System.err.println("Không thể ghi transfer_logs: " + error.getMessage());
        }
    }

    private void seed(String username, String password, String home) throws SQLException {
        String sql = "INSERT IGNORE INTO users(username,password,home_dir) VALUES(?,?,?)";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, home);
            statement.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(required("db.url") + "/" + config.getProperty("db.name", "lab2_ftp")
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",
                required("db.user"), required("db.password"));
    }

    private String required(String key) {
        String value = config.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình " + key + " trong db.properties");
        }
        return value;
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("db.properties"))) {
            properties.load(reader);
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Không đọc được db.properties", error);
        }
    }
}
