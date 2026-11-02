package lab1.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.Properties;

/** Database cua Lab 1 chi chiu trach nhiem luu tai khoan. */
public final class MySqlDB {
    private final Properties config = loadConfig();
    private boolean initialized;

    public synchronized void ensureReady() throws SQLException {
        if (initialized) return;
        String database = config.getProperty("db.name", "lab1_chat");
        try (Connection connection = DriverManager.getConnection(required("db.url"),
                required("db.user"), required("db.password"));
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "")
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                      username VARCHAR(20) PRIMARY KEY,
                      password_hash VARCHAR(128) NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )""");
        }
        initialized = true;
    }

    public void register(String username, String password) throws SQLException {
        ensureReady();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(username, password_hash) VALUES(?,?)")) {
            statement.setString(1, username);
            statement.setString(2, hash(password));
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062 || "23000".equals(exception.getSQLState())) {
                throw new SQLException("Ten nguoi dung da duoc dang ky.");
            }
            throw exception;
        }
    }

    public boolean login(String username, String password) throws SQLException {
        ensureReady();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_hash FROM users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && hash(password).equals(result.getString(1));
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(required("db.url") + "/"
                        + config.getProperty("db.name", "lab1_chat")
                        + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",
                required("db.user"), required("db.password"));
    }

    private String required(String key) {
        String value = config.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IllegalStateException("Thieu cau hinh " + key);
        return value;
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("db.properties"))) {
            properties.load(reader);
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Khong doc duoc db.properties", error);
        }
    }

    private static String hash(String password) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
