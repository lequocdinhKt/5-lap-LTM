package lab5.server.service;

import lab5.model.LoginResult;
import lab5.model.ProductInput;
import lab5.model.RegistrationResult;
import lab5.model.InventoryTransaction;
import lab5.model.Product;
import lab5.model.SyncActivity;
import lab5.network.rmi.RmiNames;
import lab5.network.rmi.SyncService;
import lab5.network.rmi.WarehouseService;
import lab5.service.WarehouseException;
import lab5.network.rmi.LockService;
import lab5.db.ProductRepository;
import lab5.db.UserRepository;
import lab5.db.InventoryRepository;
import lab5.db.TransactionManager;
import lab5.server.sync.SyncValues;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Consumer;

public final class WarehouseServiceImpl extends UnicastRemoteObject implements WarehouseService {
    private static final Logger LOG = Logger.getLogger(WarehouseServiceImpl.class.getName());

    private final javax.sql.DataSource dataSource;
    private final ProductRepository products;
    private final UserRepository users;
    private final InventoryRepository inventory;
    private final TransactionManager transactions;
    private final LockService lockManager;
    private final String serverId;
    private final String peerHost;
    private final int peerPort;
    private volatile Registry peerRegistry;
    private final Consumer<SyncActivity> activitySink;

    public WarehouseServiceImpl(javax.sql.DataSource dataSource, LockService lockManager,
                                String serverId, int servicePort, Registry peerRegistry)
            throws RemoteException {
        this(dataSource, lockManager, serverId, servicePort, null, 0, peerRegistry, null);
    }

    public WarehouseServiceImpl(javax.sql.DataSource dataSource, LockService lockManager,
                                String serverId, int servicePort,
                                String peerHost, int peerPort, Registry peerRegistry)
            throws RemoteException {
        this(dataSource, lockManager, serverId, servicePort, peerHost, peerPort, peerRegistry, null);
    }

    public WarehouseServiceImpl(javax.sql.DataSource dataSource, LockService lockManager,
                                String serverId, int servicePort, String peerHost, int peerPort,
                                Registry peerRegistry, Consumer<SyncActivity> activitySink)
            throws RemoteException {
        super(servicePort);
        this.dataSource = dataSource;
        this.products = new ProductRepository(dataSource);
        this.users = new UserRepository(dataSource);
        this.inventory = new InventoryRepository(dataSource);
        this.transactions = new TransactionManager(dataSource);
        this.lockManager = lockManager;
        this.serverId = SyncValues.origin(serverId);
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.peerRegistry = peerRegistry;
        this.activitySink = activitySink == null ? ignored -> { } : activitySink;
    }

    @Override
    public LoginResult login(String username, String password) throws RemoteException {
        String selectedUsername = username == null ? "" : username.trim();
        if (selectedUsername.isEmpty() || password == null || password.isEmpty()) {
            return new LoginResult(false, selectedUsername, "", "", "Vui lòng nhập username và mật khẩu.");
        }
        try {
            UserRepository.UserAccount account = users.findByUsername(selectedUsername);
            if (account == null || !password.equals(account.password())) {
                return new LoginResult(false, selectedUsername, "", "", "Sai username hoặc mật khẩu.");
            }
            if (!account.active()) {
                return new LoginResult(false, selectedUsername, "", "", "Tài khoản nhân viên đã bị vô hiệu hóa.");
            }
            return new LoginResult(true, account.username(), account.fullName(), account.role(), "OK");
        } catch (SQLException e) {
            throw new WarehouseException("Không thể đăng nhập do lỗi cơ sở dữ liệu.", e);
        }
    }

    @Override
    public RegistrationResult register(String username, String password, String fullName)
            throws RemoteException {
        String selectedUsername = username == null ? "" : username.trim();
        String selectedFullName = fullName == null ? "" : fullName.trim();
        if (selectedUsername.isEmpty() || password == null || password.isEmpty()
                || selectedFullName.isEmpty()) {
            return new RegistrationResult(false, selectedUsername,
                    "Vui lòng nhập đầy đủ username, họ tên và mật khẩu.");
        }
        if (selectedUsername.length() > 100 || selectedFullName.length() > 255
                || password.length() > 255) {
            return new RegistrationResult(false, selectedUsername,
                    "Tài khoản, họ tên hoặc mật khẩu vượt quá độ dài cho phép.");
        }
        try {
            transactions.execute(c -> {
                UUID userId = users.insert(c, selectedUsername, password, selectedFullName);
                if (!requirePeerSync().syncUser(userId, selectedUsername, password, selectedFullName)) {
                    throw new WarehouseException("Peer rejected user synchronization.");
                }
                return null;
            });
            record("ĐĂNG KÝ", selectedUsername);
            return new RegistrationResult(true, selectedUsername,
                    "Đăng ký thành công. Bạn có thể đăng nhập.");
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                return new RegistrationResult(false, selectedUsername,
                        "Username đã tồn tại. Vui lòng chọn username khác.");
            }
            throw new WarehouseException("Không thể đăng ký tài khoản do lỗi cơ sở dữ liệu.", e);
        } catch (WarehouseException e) {
            throw e;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể đăng ký vì Server còn lại đang mất kết nối.", e);
        } catch (Exception e) {
            throw new WarehouseException("Không thể đăng ký tài khoản.", e);
        }
    }

    @Override
    public List<Product> listProducts(String search) throws RemoteException {
        try {
            return products.findAll(search);
        } catch (SQLException e) {
            throw new WarehouseException("Không thể tải danh sách sản phẩm.", e);
        }
    }

    @Override
    public Product createProduct(ProductInput input, String clientId, String username) throws RemoteException {
        validateInput(input);
        try {
            Product product = transactions.execute(c -> {
                Product saved = products.insert(c, input);
                SyncService peerSync = requirePeerSync();
                if (!peerSync.syncCreateProduct(saved)) throw new WarehouseException("Peer rejected synchronization.");
                return saved;
            });
            record("THÊM", product.productCode());
            return product;
        } catch (SQLException e) {
            if (isDuplicateKey(e)) throw new WarehouseException("Mã sản phẩm đã tồn tại.");
            throw new WarehouseException("Không thể thêm sản phẩm.", e);
        } catch (WarehouseException e) {
            throw e;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.", e);
        } catch (Exception e) {
            throw new WarehouseException("Không thể thêm sản phẩm.", e);
        }
    }

    @Override
    public Product updateProduct(UUID id, ProductInput input, UUID lockToken, long fencingToken,
                                 String clientId, String username) throws RemoteException {
        validateInput(input);
        requireLock(id, lockToken, fencingToken, clientId);
        try {
            Product product = transactions.execute(c -> {
                Product saved = products.update(c, id, input, fencingToken);
                if (saved == null) throw new WarehouseException("Không tìm thấy sản phẩm.");
                SyncService peerSync = requirePeerSync();
                if (!peerSync.syncUpdateProduct(saved)) throw new WarehouseException("Peer rejected synchronization.");
                return saved;
            });
            record("CẬP NHẬT", product.productCode());
            return product;
        } catch (SQLException e) {
            throw new WarehouseException("Không thể cập nhật sản phẩm.", e);
        } catch (WarehouseException e) {
            throw e;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.", e);
        } catch (Exception e) {
            throw new WarehouseException("Không thể cập nhật sản phẩm.", e);
        }
    }

    @Override
    public void deleteProduct(UUID id, UUID lockToken, long fencingToken,
                              String clientId, String username) throws RemoteException {
        requireLock(id, lockToken, fencingToken, clientId);
        try {
            transactions.execute(c -> {
                products.softDelete(c, id, fencingToken);
                SyncService peerSync = requirePeerSync();
                if (!peerSync.syncDeleteProduct(id, fencingToken)) throw new WarehouseException("Peer rejected synchronization.");
                return null;
            });
            record("XÓA", id.toString());
        } catch (SQLException e) {
            throw new WarehouseException("Không thể xóa sản phẩm.", e);
        } catch (WarehouseException e) {
            throw e;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.", e);
        } catch (Exception e) {
            throw new WarehouseException("Không thể xóa sản phẩm.", e);
        }
    }

    @Override
    public InventoryTransaction changeInventory(UUID id, int amount, boolean importing,
                                                UUID lockToken, long fencingToken,
                                                String clientId, String username)
            throws RemoteException {
        requireLock(id, lockToken, fencingToken, clientId);
        try {
            InventoryTransaction result = transactions.execute(c -> {
                Product product = products.find(c, id);
                if (product == null || product.deleted()) throw new WarehouseException("Không tìm thấy sản phẩm.");
                if (product.lastFencingToken() > fencingToken) {
                    throw new WarehouseException("Khóa đã bị thay thế bởi phiên mới.");
                }
                int after = InventoryRules.quantityAfter(product.quantity(), amount, importing);
                if (!inventory.updateQuantity(c, id, after, fencingToken)) {
                    throw new WarehouseException("Khóa đã hết hạn, bị thay thế hoặc sản phẩm đã bị xóa.");
                }
                UUID userId = users.findActiveId(c, username);
                if (userId == null) throw new WarehouseException("Tài khoản không tồn tại hoặc đã bị khóa.");
                InventoryTransaction transaction = new InventoryTransaction(UUID.randomUUID(),
                        "TX-" + UUID.randomUUID(), id, SyncValues.transactionType(importing), amount,
                        product.quantity(), after, username, Instant.now());
                inventory.insert(c, transaction, userId);
                SyncService peerSync = requirePeerSync();
                if (!peerSync.syncInventory(id, after, fencingToken, transaction)) throw new WarehouseException("Peer rejected synchronization.");
                return transaction;
            });
            record(importing ? "NHẬP KHO" : "XUẤT KHO", id + " · " + amount);
            return result;
        } catch (SQLException e) {
            throw new WarehouseException("Không thể cập nhật tồn kho.", e);
        } catch (WarehouseException e) {
            throw e;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.", e);
        } catch (Exception e) {
            throw new WarehouseException("Không thể cập nhật tồn kho.", e);
        }
    }

    @Override
    public List<InventoryTransaction> transactionHistory(UUID productId) throws RemoteException {
        try {
            return inventory.history(productId);
        } catch (SQLException e) {
            throw new WarehouseException("Không thể tải lịch sử giao dịch.", e);
        }
    }

    @Override
    public boolean isPeerAvailable() {
        try {
            SyncService peer = getPeerSync();
            return peer != null && peer.ping();
        } catch (Exception e) {
            return false;
        }
    }

    public void setPeerRegistry(Registry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    private SyncService getPeerSync() {
        try {
            Registry reg = this.peerRegistry;
            if (reg == null && peerHost != null && !peerHost.isBlank() && peerPort > 0) {
                reg = LocateRegistry.getRegistry(peerHost, peerPort);
            }
            if (reg != null) {
                return (SyncService) reg.lookup(RmiNames.SYNC);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private SyncService requirePeerSync() throws WarehouseException {
        SyncService peer = getPeerSync();
        if (peer == null) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.");
        }
        try {
            if (!peer.ping()) {
                throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.");
            }
            return peer;
        } catch (RemoteException e) {
            throw new WarehouseException("Không thể cập nhật dữ liệu vì Server còn lại đang mất kết nối.", e);
        }
    }

    private void record(String action, String detail) {
        activitySink.accept(SyncActivity.success(serverId, action, detail));
    }

    private static boolean isDuplicateKey(SQLException e) {
        SQLException current = e;
        while (current != null) {
            if (current.getErrorCode() == 1062) return true;
            String state = current.getSQLState();
            if (state != null && (state.equals("23000") || state.equals("23505"))) {
                return true;
            }
            Throwable cause = current.getCause();
            current = (cause instanceof SQLException sqlEx) ? sqlEx : null;
        }
        return false;
    }

    private void requireLock(UUID id, UUID token, long fencing, String clientId)
            throws RemoteException {
        if (token == null || !lockManager.validate(id, token, fencing, clientId)) {
            throw new WarehouseException("Lock không hợp lệ, đã hết hạn hoặc đã bị thay thế.");
        }
    }

    private void validateInput(ProductInput input) throws WarehouseException {
        if (input == null || input.productCode() == null || input.productCode().isBlank()
                || input.productName() == null || input.productName().isBlank()
                || input.category() == null || input.category().isBlank() || input.price() == null || input.price().signum() < 0) {
            throw new WarehouseException("Thông tin sản phẩm không hợp lệ.");
        }
    }

}
