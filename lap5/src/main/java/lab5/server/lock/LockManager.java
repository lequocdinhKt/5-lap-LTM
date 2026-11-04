package lab5.server.lock;

import lab5.model.LockLease;
import lab5.service.WarehouseException;
import lab5.network.rmi.LockService;
import lab5.db.LockRepository;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Server A's lock coordinator. The product row serializes acquisition and persists fencing. */
public final class LockManager extends UnicastRemoteObject implements LockService {
    private final DataSource dataSource;
    private final Clock clock;
    private final int leaseSeconds;
    private final LockRepository locks = new LockRepository();

    public LockManager(DataSource dataSource, int leaseSeconds) throws RemoteException {
        this(dataSource, leaseSeconds, Clock.systemUTC(), 0);
    }

    LockManager(DataSource dataSource, int leaseSeconds, Clock clock) throws RemoteException {
        this(dataSource, leaseSeconds, clock, 0);
    }

    public LockManager(DataSource dataSource, int leaseSeconds, int port) throws RemoteException {
        this(dataSource, leaseSeconds, Clock.systemUTC(), port);
    }

    private LockManager(DataSource dataSource, int leaseSeconds, Clock clock, int port) throws RemoteException {
        super(port);
        if (leaseSeconds <= 0) throw new IllegalArgumentException("Lease duration must be positive");
        this.dataSource = dataSource;
        this.leaseSeconds = leaseSeconds;
        this.clock = clock;
    }

    @Override
    public LockLease acquire(UUID resourceId, String clientId) throws RemoteException {
        if (resourceId == null || clientId == null || clientId.isBlank()) {
            throw new WarehouseException("Sản phẩm và Client ID phải hợp lệ.");
        }
        Instant now = clock.instant();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long lastFencing = locks.productFence(connection, resourceId);
                LockLease current = locks.findByResource(connection, resourceId);
                LockRules.ensureAvailable(current, clientId, now);
                if (hasPendingSync(connection, resourceId)) {
                    throw new WarehouseException("Sản phẩm đang chờ đồng bộ. Vui lòng thử lại sau.");
                }
                long next = LockRules.nextFencingToken(lastFencing);
                LockLease lease = new LockLease(resourceId, clientId, UUID.randomUUID(), next,
                        now, now.plusSeconds(leaseSeconds));
                locks.save(connection, lease);
                connection.commit();
                return lease;
            } catch (SQLException | RuntimeException | WarehouseException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new WarehouseException("Không thể cấp khóa từ MySQL.", e);
        }
    }

    @Override
    public LockLease heartbeat(UUID token, String clientId) throws RemoteException {
        Instant now = clock.instant();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LockLease current = locks.findByToken(connection, token);
                if (current == null || !LockRules.valid(current, current.resourceId(), token,
                        current.fencingToken(), clientId, now)) {
                    throw new WarehouseException("Lock token không hợp lệ hoặc đã hết hạn.");
                }
                LockLease refreshed = new LockLease(current.resourceId(), clientId, token,
                        current.fencingToken(), current.acquiredAt(), now.plusSeconds(leaseSeconds));
                locks.updateExpiry(connection, token, refreshed.expiresAt());
                connection.commit();
                return refreshed;
            } catch (SQLException | RuntimeException | WarehouseException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new WarehouseException("Không thể gia hạn khóa.", e);
        }
    }

    @Override
    public void release(UUID token, String clientId) throws RemoteException {
        try (Connection connection = dataSource.getConnection()) {
            LockLease current = locks.findByToken(connection, token);
            if (current == null || !current.ownerClientId().equals(clientId)) {
                throw new WarehouseException("Không thể giải phóng khóa không thuộc Client.");
            }
            locks.delete(connection, token, clientId);
        } catch (SQLException e) {
            throw new WarehouseException("Không thể giải phóng khóa.", e);
        }
    }

    @Override
    public boolean validate(UUID resourceId, UUID token, long fencingToken, String clientId)
            throws RemoteException {
        if (resourceId == null || token == null || clientId == null) return false;
        try (Connection connection = dataSource.getConnection()) {
            return LockRules.valid(locks.findByResource(connection, resourceId), resourceId,
                    token, fencingToken, clientId, clock.instant());
        } catch (SQLException e) {
            throw new WarehouseException("Không thể kiểm tra khóa.", e);
        }
    }

    private static boolean hasPendingSync(Connection connection, UUID id) throws SQLException {
        // Đồng bộ trực tiếp qua Direct RMI diễn ra đồng thời (synchronous), không dùng hàng đợi outbox
        return false;
    }

}
