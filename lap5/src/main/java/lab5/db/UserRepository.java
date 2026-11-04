package lab5.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UserAccount findByUsername(String username) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT username, password, full_name, role, is_active FROM users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? new UserAccount(row.getString("username"), row.getString("password"),
                        row.getString("full_name"), row.getString("role"), row.getBoolean("is_active")) : null;
            }
        }
    }

    public UUID insert(Connection connection, String username, String password, String fullName)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users(id,username,password,full_name,role,is_active,created_at) "
                        + "VALUES (?,?,?,?, 'STAFF', 1, NOW(3))")) {
            statement.setString(1, id.toString());
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, fullName);
            statement.executeUpdate();
        }
        return id;
    }

    public UUID findActiveId(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE username=? AND is_active=1")) {
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? UUID.fromString(row.getString(1)) : null;
            }
        }
    }

    public record UserAccount(String username, String password, String fullName,
                              String role, boolean active) { }
}
