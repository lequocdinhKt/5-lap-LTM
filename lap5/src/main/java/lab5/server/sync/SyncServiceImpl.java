package lab5.server.sync;

import lab5.db.PeerSyncRepository;
import lab5.model.InventoryTransaction;
import lab5.model.Product;
import lab5.model.SyncActivity;
import lab5.network.rmi.SyncService;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * RMI adapter nhận đồng bộ trực tiếp từ peer và ủy quyền việc ghi dữ liệu cho repository.
 * Adapter không phát sinh sự kiện đồng bộ ngược để tránh vòng lặp vô hạn.
 */
public final class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private static final Logger LOG = Logger.getLogger(SyncServiceImpl.class.getName());
    private final PeerSyncRepository repository;
    private final String serverId;
    private final Consumer<SyncActivity> activitySink;

    public SyncServiceImpl(DataSource dataSource, String serverId, int port) throws RemoteException {
        this(dataSource, serverId, port, null);
    }

    public SyncServiceImpl(DataSource dataSource, String serverId, int port,
                           Consumer<SyncActivity> activitySink) throws RemoteException {
        super(port);
        this.repository = new PeerSyncRepository(dataSource);
        this.serverId = SyncValues.origin(serverId);
        this.activitySink = activitySink == null ? ignored -> { } : activitySink;
    }

    @Override
    public boolean syncUser(UUID id, String username, String password, String fullName) throws RemoteException {
        if (id == null || username == null || username.isBlank() || password == null
                || password.isEmpty() || fullName == null || fullName.isBlank()) {
            throw new RemoteException("Invalid user synchronization payload");
        }
        try {
            repository.insertUser(id, username, password, fullName);
            record("NHẬN ĐĂNG KÝ", username);
            return true;
        } catch (SQLException error) {
            throw new RemoteException("Cannot synchronize user (possibly conflicting username)", error);
        }
    }

    @Override
    public boolean ping() {
        return true;
    }

    @Override
    public boolean syncCreateProduct(Product product) throws RemoteException {
        if (product == null || product.id() == null) return false;
        try {
            repository.createProduct(product);
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI CREATE thành công: " + product.productCode());
            record("NHẬN THÊM", product.productCode());
            return true;
        } catch (SQLException error) {
            return failure("syncCreateProduct", product.productCode(), error);
        }
    }

    @Override
    public boolean syncUpdateProduct(Product product) throws RemoteException {
        if (product == null || product.id() == null) return false;
        try {
            repository.updateProduct(product);
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI UPDATE thành công: " + product.productCode());
            record("NHẬN CẬP NHẬT", product.productCode());
            return true;
        } catch (SQLException error) {
            return failure("syncUpdateProduct", product.id().toString(), error);
        }
    }

    @Override
    public boolean syncDeleteProduct(UUID productId, long fencingToken) throws RemoteException {
        if (productId == null) return false;
        try {
            repository.deleteProduct(productId, fencingToken);
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI DELETE thành công: " + productId);
            record("NHẬN XÓA", productId.toString());
            return true;
        } catch (SQLException error) {
            return failure("syncDeleteProduct", productId.toString(), error);
        }
    }

    @Override
    public boolean syncInventory(UUID productId, int newQuantity, long fencingToken,
                                 InventoryTransaction transaction) throws RemoteException {
        if (productId == null) return false;
        try {
            repository.applyInventory(productId, newQuantity, fencingToken, transaction);
            LOG.info(() -> "[" + serverId + "] Đồng bộ Direct RMI INVENTORY thành công: " + productId
                    + ", newQuantity=" + newQuantity);
            record("NHẬN TỒN KHO", productId + " · " + newQuantity);
            return true;
        } catch (SQLException error) {
            return failure("syncInventory", productId.toString(), error);
        }
    }

    private boolean failure(String operation, String target, SQLException error) throws RemoteException {
        LOG.log(Level.SEVERE, "[" + serverId + "] Lỗi đồng bộ " + operation + ": " + target, error);
        throw new RemoteException("Lỗi " + operation + " trên server " + serverId, error);
    }

    private void record(String action, String detail) {
        activitySink.accept(SyncActivity.success(serverId, action, detail));
    }
}
