package lab5.db;

import lab5.model.LockLease;
import lab5.service.WarehouseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public final class LockRepository {
    public long productFence(Connection connection, UUID id) throws SQLException, WarehouseException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_fencing_token FROM products WHERE id=? AND NOT is_deleted FOR UPDATE")) {
            statement.setString(1, id.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new WarehouseException("Không tìm thấy sản phẩm để khóa.");
                return row.getLong(1);
            }
        }
    }

    public LockLease findByResource(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM product_locks WHERE resource_id=? FOR UPDATE")) {
            statement.setString(1, id.toString());
            try (ResultSet row = statement.executeQuery()) { return row.next() ? map(row) : null; }
        }
    }

    public LockLease findByToken(Connection connection, UUID token) throws SQLException {
        if (token == null) return null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM product_locks WHERE lock_token=? FOR UPDATE")) {
            statement.setString(1, token.toString());
            try (ResultSet row = statement.executeQuery()) { return row.next() ? map(row) : null; }
        }
    }

    public void save(Connection connection, LockLease lease) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE products SET last_fencing_token=? WHERE id=?")) {
            update.setLong(1, lease.fencingToken());
            update.setString(2, lease.resourceId().toString());
            update.executeUpdate();
        }
        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO product_locks(resource_id,owner_client_id,lock_token,fencing_token,"
                        + "acquired_at,expires_at) VALUES (?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE owner_client_id=VALUES(owner_client_id),"
                        + "lock_token=VALUES(lock_token),fencing_token=VALUES(fencing_token),"
                        + "acquired_at=VALUES(acquired_at),expires_at=VALUES(expires_at)")) {
            upsert.setString(1, lease.resourceId().toString());
            upsert.setString(2, lease.ownerClientId());
            upsert.setString(3, lease.lockToken().toString());
            upsert.setLong(4, lease.fencingToken());
            upsert.setTimestamp(5, Timestamp.from(lease.acquiredAt()));
            upsert.setTimestamp(6, Timestamp.from(lease.expiresAt()));
            upsert.executeUpdate();
        }
    }

    public void updateExpiry(Connection connection, UUID token, Instant expiresAt) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE product_locks SET expires_at=? WHERE lock_token=?")) {
            update.setTimestamp(1, Timestamp.from(expiresAt));
            update.setString(2, token.toString());
            update.executeUpdate();
        }
    }

    public void delete(Connection connection, UUID token, String clientId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM product_locks WHERE lock_token=? AND owner_client_id=?")) {
            delete.setString(1, token.toString());
            delete.setString(2, clientId);
            delete.executeUpdate();
        }
    }

    private static LockLease map(ResultSet row) throws SQLException {
        return new LockLease(UUID.fromString(row.getString("resource_id")),
                row.getString("owner_client_id"), UUID.fromString(row.getString("lock_token")),
                row.getLong("fencing_token"), row.getTimestamp("acquired_at").toInstant(),
                row.getTimestamp("expires_at").toInstant());
    }
}
