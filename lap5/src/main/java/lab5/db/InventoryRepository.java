package lab5.db;

import lab5.model.InventoryTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InventoryRepository {
    private final DataSource dataSource;

    public InventoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean updateQuantity(Connection connection, UUID productId, int quantity, long fencingToken)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET quantity=?,version=version+1,last_fencing_token=?,updated_at=now() "
                        + "WHERE id=? AND is_deleted = 0 AND last_fencing_token<=?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, fencingToken);
            statement.setString(3, productId.toString());
            statement.setLong(4, fencingToken);
            return statement.executeUpdate() == 1;
        }
    }

    public void insert(Connection connection, InventoryTransaction transaction, UUID userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO inventory_transactions(id,transaction_code,product_id,transaction_type,"
                        + "quantity,quantity_before,quantity_after,created_by) VALUES (?,?,?,?,?,?,?,?)")) {
            statement.setString(1, transaction.id().toString());
            statement.setString(2, transaction.transactionCode());
            statement.setString(3, transaction.productId().toString());
            statement.setString(4, transaction.transactionType());
            statement.setInt(5, transaction.quantity());
            statement.setInt(6, transaction.quantityBefore());
            statement.setInt(7, transaction.quantityAfter());
            statement.setString(8, userId.toString());
            statement.executeUpdate();
        }
    }

    public List<InventoryTransaction> history(UUID productId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT t.*,u.username AS created_by_username FROM inventory_transactions t "
                             + "LEFT JOIN users u ON u.id=t.created_by WHERE t.product_id=? ORDER BY t.created_at DESC")) {
            statement.setString(1, productId.toString());
            try (ResultSet row = statement.executeQuery()) {
                List<InventoryTransaction> result = new ArrayList<>();
                while (row.next()) {
                    result.add(new InventoryTransaction(UUID.fromString(row.getString("id")),
                            row.getString("transaction_code"), UUID.fromString(row.getString("product_id")),
                            row.getString("transaction_type"), row.getInt("quantity"),
                            row.getInt("quantity_before"), row.getInt("quantity_after"),
                            row.getString("created_by_username"), row.getTimestamp("created_at").toInstant()));
                }
                return result;
            }
        }
    }
}
