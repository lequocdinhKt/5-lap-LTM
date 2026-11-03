package com.warehouse.server.sync;

import com.warehouse.common.dto.SyncEvent;
import com.warehouse.common.model.InventoryTransaction;
import com.warehouse.common.model.Product;
import com.warehouse.common.rmi.SyncService;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý tiếp nhận đồng bộ trực tiếp qua Java RMI từ Server đối diện.
 * Áp dụng trực tiếp vào MariaDB cục bộ, không phát sinh thêm sự kiện đồng bộ để tránh vòng lặp vô hạn.
 * Sử dụng cơ chế idempotent để chống xử lý trùng lặp khi mạng chập chờn.
 */
public final class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private static final Logger LOG = Logger.getLogger(SyncServiceImpl.class.getName());
    private final DataSource dataSource;
    private final String serverId;

    public SyncServiceImpl(DataSource dataSource, String serverId, int port) throws RemoteException {
        super(port);
        this.dataSource = dataSource;
        this.serverId = SyncValues.origin(serverId);
    }

    @Override
    public boolean syncUser(UUID id, String username, String password, String fullName) throws RemoteException {
        if (id == null || username == null || username.isBlank() || password == null
                || password.isEmpty() || fullName == null || fullName.isBlank()) {
            throw new RemoteException("Invalid user synchronization payload");
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(id,username,password,full_name,role,is_active,created_at) "
                             + "VALUES (?,?,?,?,'STAFF',1,NOW(3))")) {
            ps.setString(1, id.toString());
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, fullName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RemoteException("Cannot synchronize user (possibly conflicting username)", e);
        }
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public boolean syncCreateProduct(Product product) throws RemoteException {
        if (product == null || product.id() == null) {
            return false;
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO products(id,product_code,product_name,category,price,quantity,version,"
                             + "last_fencing_token,is_deleted) VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, product.id().toString());
            ps.setString(2, product.productCode());
            ps.setString(3, product.productName());
            ps.setString(4, product.category());
            ps.setBigDecimal(5, product.price());
            ps.setInt(6, product.quantity());
            ps.setLong(7, product.version());
            ps.setLong(8, product.lastFencingToken());
            ps.setBoolean(9, product.deleted());
            ps.executeUpdate();
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI CREATE thành công: " + product.productCode());
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                // A repeated CREATE may be acknowledged, but never change a different UUID.
                try (Connection c = dataSource.getConnection();
                     PreparedStatement check = c.prepareStatement("SELECT product_code FROM products WHERE id=?")) {
                    check.setString(1, product.id().toString());
                    try (ResultSet row = check.executeQuery()) {
                        if (row.next() && product.productCode().equals(row.getString("product_code"))) return true;
                    }
                } catch (SQLException checkError) {
                    e.addSuppressed(checkError);
                }
            }
            LOG.log(Level.SEVERE, "[" + serverId + "] Lỗi đồng bộ syncCreateProduct: " + product.productCode(), e);
            throw new RemoteException("Lỗi syncCreateProduct trên server " + serverId, e);
        }
    }

    @Override
    public boolean syncUpdateProduct(Product product) throws RemoteException {
        if (product == null || product.id() == null) {
            return false;
        }
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE products SET product_code=?,product_name=?,category=?,price=?,quantity=?,"
                            + "version=?,last_fencing_token=GREATEST(last_fencing_token,?),is_deleted=? "
                            + "WHERE id=? AND version<=? AND last_fencing_token<=?")) {
                ps.setString(1, product.productCode());
                ps.setString(2, product.productName());
                ps.setString(3, product.category());
                ps.setBigDecimal(4, product.price());
                ps.setInt(5, product.quantity());
                ps.setLong(6, product.version());
                ps.setLong(7, product.lastFencingToken());
                ps.setBoolean(8, product.deleted());
                ps.setString(9, product.id().toString());
                ps.setLong(10, product.version());
                ps.setLong(11, product.lastFencingToken());
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    try (PreparedStatement check = c.prepareStatement("SELECT id FROM products WHERE id=?")) {
                        check.setString(1, product.id().toString());
                        try (ResultSet row = check.executeQuery()) {
                            if (!row.next()) return syncCreateProduct(product);
                        }
                    }
                    throw new RemoteException("Peer rejected stale product update");
                }
            }
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI UPDATE thành công: " + product.productCode());
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[" + serverId + "] Lỗi đồng bộ syncUpdateProduct: " + product.id(), e);
            throw new RemoteException("Lỗi syncUpdateProduct trên server " + serverId, e);
        }
    }

    @Override
    public boolean syncDeleteProduct(UUID productId, long fencingToken) throws RemoteException {
        if (productId == null) {
            return false;
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE products SET is_deleted=1,version=version+1,"
                             + "last_fencing_token=GREATEST(last_fencing_token,?),updated_at=now() "
                             + "WHERE id=? AND is_deleted=0 AND last_fencing_token<=?")) {
            ps.setLong(1, fencingToken);
            ps.setString(2, productId.toString());
            ps.setLong(3, fencingToken);
            ps.executeUpdate();
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI DELETE thành công: " + productId);
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[" + serverId + "] Lỗi đồng bộ syncDeleteProduct: " + productId, e);
            throw new RemoteException("Lỗi syncDeleteProduct trên server " + serverId, e);
        }
    }

    @Override
    public boolean syncInventory(UUID productId, int newQuantity, long fencingToken,
                                 InventoryTransaction transaction) throws RemoteException {
        if (productId == null) {
            return false;
        }
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (transaction == null || transaction.id() == null
                        || !productId.equals(transaction.productId()) || newQuantity < 0
                        || newQuantity != transaction.quantityAfter()) {
                    throw new SQLException("Invalid inventory synchronization payload");
                }
                // Serialize deliveries for this product before checking the transaction ID.
                try (PreparedStatement row = c.prepareStatement(
                        "SELECT quantity,last_fencing_token,is_deleted FROM products WHERE id=? FOR UPDATE")) {
                    row.setString(1, productId.toString());
                    try (ResultSet rs = row.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Product missing on peer");
                        try (PreparedStatement seen = c.prepareStatement(
                                "SELECT id FROM inventory_transactions WHERE id=?")) {
                            seen.setString(1, transaction.id().toString());
                            try (ResultSet existing = seen.executeQuery()) {
                                if (existing.next()) { c.commit(); return true; }
                            }
                        }
                        if (rs.getBoolean("is_deleted") || rs.getLong("last_fencing_token") > fencingToken
                                || rs.getInt("quantity") != transaction.quantityBefore()) {
                            throw new SQLException("Stale or out-of-order inventory synchronization");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE products SET quantity=?,version=version+1,"
                                + "last_fencing_token=?,updated_at=now(3) WHERE id=?")) {
                    ps.setInt(1, newQuantity);
                    ps.setLong(2, fencingToken);
                    ps.setString(3, productId.toString());
                    if (ps.executeUpdate() != 1) throw new SQLException("Product missing on peer");
                }
                UUID userId = findUserIdByUsername(c, transaction.createdBy());
                if (userId == null) throw new SQLException("Transaction user missing on peer");
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO inventory_transactions(id,transaction_code,product_id,transaction_type,"
                                + "quantity,quantity_before,quantity_after,created_by,created_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, transaction.id().toString());
                    ps.setString(2, transaction.transactionCode());
                    ps.setString(3, transaction.productId().toString());
                    ps.setString(4, transaction.transactionType());
                    ps.setInt(5, transaction.quantity());
                    ps.setInt(6, transaction.quantityBefore());
                    ps.setInt(7, transaction.quantityAfter());
                    ps.setString(8, userId.toString());
                    ps.setTimestamp(9, Timestamp.from(transaction.createdAt()));
                    ps.executeUpdate();
                }
                c.commit();
                LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI INVENTORY thành công: " + productId
                        + ", newQuantity=" + newQuantity);
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[" + serverId + "] Lỗi đồng bộ syncInventory: " + productId, e);
            throw new RemoteException("Lỗi syncInventory trên server " + serverId, e);
        }
    }

    @Override
    public boolean applyEvent(SyncEvent event) throws RemoteException {
        return false;
    }

    private static UUID findUserIdByUsername(Connection connection, String username) throws SQLException {
        if (username == null || username.isBlank()) return null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("id"));
            }
        }
        return null;
    }
}
