package lab5.db;

import lab5.model.InventoryTransaction;
import lab5.model.Product;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Áp dụng dữ liệu do peer gửi vào MySQL cục bộ.
 * Mọi câu SQL của luồng nhận đồng bộ được giữ tại repository này để RMI adapter chỉ điều phối.
 */
public final class PeerSyncRepository {
    private final DataSource dataSource;

    public PeerSyncRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertUser(UUID id, String username, String password, String fullName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(id,username,password,full_name,role,is_active,created_at) "
                             + "VALUES (?,?,?,?,'STAFF',1,NOW(3))")) {
            statement.setString(1, id.toString());
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, fullName);
            statement.executeUpdate();
        }
    }

    public void createProduct(Product product) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO products(id,product_code,product_name,category,price,quantity,version,"
                             + "last_fencing_token,is_deleted) VALUES (?,?,?,?,?,?,?,?,?)")) {
            bindProductForInsert(statement, product);
            statement.executeUpdate();
        } catch (SQLException error) {
            if (error.getErrorCode() == 1062 && isSameProduct(product)) return;
            throw error;
        }
    }

    public void updateProduct(Product product) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE products SET product_code=?,product_name=?,category=?,price=?,quantity=?,"
                             + "version=?,last_fencing_token=GREATEST(last_fencing_token,?),is_deleted=? "
                             + "WHERE id=? AND version<=? AND last_fencing_token<=?")) {
            statement.setString(1, product.productCode());
            statement.setString(2, product.productName());
            statement.setString(3, product.category());
            statement.setBigDecimal(4, product.price());
            statement.setInt(5, product.quantity());
            statement.setLong(6, product.version());
            statement.setLong(7, product.lastFencingToken());
            statement.setBoolean(8, product.deleted());
            statement.setString(9, product.id().toString());
            statement.setLong(10, product.version());
            statement.setLong(11, product.lastFencingToken());
            if (statement.executeUpdate() == 1) return;
            if (!exists(connection, product.id())) {
                createProduct(product);
                return;
            }
            throw new SQLException("Peer rejected stale product update");
        }
    }

    public void deleteProduct(UUID productId, long fencingToken) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE products SET is_deleted=1,version=version+1,"
                             + "last_fencing_token=GREATEST(last_fencing_token,?),updated_at=NOW(3) "
                             + "WHERE id=? AND is_deleted=0 AND last_fencing_token<=?")) {
            statement.setLong(1, fencingToken);
            statement.setString(2, productId.toString());
            statement.setLong(3, fencingToken);
            statement.executeUpdate();
        }
    }

    public void applyInventory(UUID productId, int newQuantity, long fencingToken,
                               InventoryTransaction transaction) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                validateInventory(productId, newQuantity, transaction);
                if (lockAndValidateProduct(connection, productId, fencingToken, transaction)) {
                    connection.commit();
                    return;
                }
                updateQuantity(connection, productId, newQuantity, fencingToken);
                UUID userId = findUserId(connection, transaction.createdBy());
                if (userId == null) throw new SQLException("Transaction user missing on peer");
                insertTransaction(connection, transaction, userId);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sqlError) throw sqlError;
                throw new SQLException("Cannot apply inventory synchronization", error);
            }
        }
    }

    private boolean isSameProduct(Product product) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT product_code FROM products WHERE id=?")) {
            statement.setString(1, product.id().toString());
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && product.productCode().equals(row.getString("product_code"));
            }
        }
    }

    private static void bindProductForInsert(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.id().toString());
        statement.setString(2, product.productCode());
        statement.setString(3, product.productName());
        statement.setString(4, product.category());
        statement.setBigDecimal(5, product.price());
        statement.setInt(6, product.quantity());
        statement.setLong(7, product.version());
        statement.setLong(8, product.lastFencingToken());
        statement.setBoolean(9, product.deleted());
    }

    private static boolean exists(Connection connection, UUID productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM products WHERE id=?")) {
            statement.setString(1, productId.toString());
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    private static void validateInventory(UUID productId, int newQuantity, InventoryTransaction transaction)
            throws SQLException {
        if (transaction == null || transaction.id() == null || !productId.equals(transaction.productId())
                || newQuantity < 0 || newQuantity != transaction.quantityAfter()) {
            throw new SQLException("Invalid inventory synchronization payload");
        }
    }

    private static boolean lockAndValidateProduct(Connection connection, UUID productId, long fencingToken,
                                                  InventoryTransaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantity,last_fencing_token,is_deleted FROM products WHERE id=? FOR UPDATE")) {
            statement.setString(1, productId.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Product missing on peer");
                if (transactionExists(connection, transaction.id())) return true;
                if (row.getBoolean("is_deleted") || row.getLong("last_fencing_token") > fencingToken
                        || row.getInt("quantity") != transaction.quantityBefore()) {
                    throw new SQLException("Stale or out-of-order inventory synchronization");
                }
                return false;
            }
        }
    }

    private static boolean transactionExists(Connection connection, UUID transactionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM inventory_transactions WHERE id=?")) {
            statement.setString(1, transactionId.toString());
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    private static void updateQuantity(Connection connection, UUID productId, int quantity, long fencingToken)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET quantity=?,version=version+1,last_fencing_token=?,updated_at=NOW(3) "
                        + "WHERE id=?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, fencingToken);
            statement.setString(3, productId.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Product missing on peer");
        }
    }

    private static UUID findUserId(Connection connection, String username) throws SQLException {
        if (username == null || username.isBlank()) return null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? UUID.fromString(row.getString("id")) : null;
            }
        }
    }

    private static void insertTransaction(Connection connection, InventoryTransaction transaction, UUID userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO inventory_transactions(id,transaction_code,product_id,transaction_type,"
                        + "quantity,quantity_before,quantity_after,created_by,created_at) VALUES (?,?,?,?,?,?,?,?,?)")) {
            statement.setString(1, transaction.id().toString());
            statement.setString(2, transaction.transactionCode());
            statement.setString(3, transaction.productId().toString());
            statement.setString(4, transaction.transactionType());
            statement.setInt(5, transaction.quantity());
            statement.setInt(6, transaction.quantityBefore());
            statement.setInt(7, transaction.quantityAfter());
            statement.setString(8, userId.toString());
            statement.setTimestamp(9, Timestamp.from(transaction.createdAt()));
            statement.executeUpdate();
        }
    }
}
