package db;

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

import models.Mail;

public class MySqlDB {
    private static final Path CONFIG_FILE = Path.of("db.properties");
    private final Properties config = loadConfig();
    private boolean initialized;

    public synchronized void ensureReady() {
        if (initialized) {
            return;
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        password VARCHAR(100) NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS mails (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        mail_server VARCHAR(20) NOT NULL,
                        sender VARCHAR(255) NOT NULL,
                        receiver VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        content TEXT NOT NULL,
                        sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            initialized = true;
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public boolean registerUser(String username, String password) {
        ensureReady();
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false;
            }
            throw databaseError(e);
        }
    }

    public boolean loginUser(String username, String password) {
        ensureReady();
        String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public boolean saveMail(Mail mail, String mailServer) {
        ensureReady();
        String sql = "INSERT INTO mails (mail_server, sender, receiver, subject, content) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, mailServer);
            statement.setString(2, mail.sender);
            statement.setString(3, mail.receiver);
            statement.setString(4, mail.subject);
            statement.setString(5, mail.content);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("LTM_DB_URL", "url"),
                setting("LTM_DB_USER", "user"),
                setting("LTM_DB_PASSWORD", "password")
        );
    }

    private static Properties loadConfig() {
        if (!Files.isRegularFile(CONFIG_FILE)) {
            if (hasEnvironmentConfig()) {
                return new Properties();
            }
            throw new IllegalStateException(
                    "Thiếu db.properties. Hãy sao chép db.properties.example thành db.properties và nhập mật khẩu MySQL."
            );
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được db.properties.", e);
        }

        for (String key : new String[]{"url", "user", "password"}) {
            if (properties.getProperty(key) == null && System.getenv(environmentName(key)) == null) {
                throw new IllegalStateException("db.properties thiếu cấu hình: " + key);
            }
        }
        return properties;
    }

    private String setting(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        return value == null || value.isBlank() ? config.getProperty(propertyName) : value.trim();
    }

    private static boolean hasEnvironmentConfig() {
        return !blank(System.getenv("LTM_DB_URL"))
                && !blank(System.getenv("LTM_DB_USER"))
                && System.getenv("LTM_DB_PASSWORD") != null;
    }

    private static String environmentName(String propertyName) {
        return switch (propertyName) {
            case "url" -> "LTM_DB_URL";
            case "user" -> "LTM_DB_USER";
            case "password" -> "LTM_DB_PASSWORD";
            default -> throw new IllegalArgumentException(propertyName);
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static IllegalStateException databaseError(SQLException e) {
        return new IllegalStateException(
                "Không kết nối được MySQL. Kiểm tra MySQL đang chạy và thông tin trong db.properties.\n" + e.getMessage(),
                e
        );
    }
}
