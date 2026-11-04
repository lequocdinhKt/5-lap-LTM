package lab5.service;

import lab5.config.ServerConfig;
import lab5.db.DatabaseConnection;
import lab5.model.SyncActivity;
import lab5.network.rmi.LockService;
import lab5.network.rmi.RmiNames;
import lab5.network.rmi.SyncService;
import lab5.server.lock.LockManager;
import lab5.server.lock.ReconnectingLockService;
import lab5.server.service.WarehouseServiceImpl;
import lab5.server.sync.SyncServiceImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public final class ServerRuntimeService implements AutoCloseable {
    private final SyncActivityLog activities = new SyncActivityLog();
    private DatabaseConnection database;
    private Registry registry;
    private Registry peerRegistry;
    private Remote warehouse;
    private Remote sync;
    private Remote lock;
    private ServerConfig config;
    private boolean ownsRegistry;

    public synchronized RuntimeStatus start(ServerConfig selectedConfig) throws Exception {
        if (isRunning()) throw new IllegalStateException("Server đang chạy.");
        config = selectedConfig;
        System.setProperty("java.rmi.server.hostname", selectedConfig.serverHost());
        activities.accept(SyncActivity.success(selectedConfig.serverId(), "KHỞI ĐỘNG",
                selectedConfig.serverHost() + ":" + selectedConfig.registryPort()));

        DatabaseConnection openedDatabase = new DatabaseConnection(selectedConfig.dbUrl(),
                selectedConfig.dbUsername(), selectedConfig.dbPassword());
        Registry openedRegistry = null;
        Remote openedWarehouse = null;
        Remote openedSync = null;
        Remote openedLock = null;
        boolean createdRegistry = false;
        try {
            int products = openedDatabase.verify();
            RegistryHandle registryHandle = createRegistry(selectedConfig.registryPort());
            openedRegistry = registryHandle.registry();
            createdRegistry = registryHandle.created();
            boolean serverA = "server-a".equalsIgnoreCase(selectedConfig.serverId());
            LockService lockService;
            if (serverA) {
                lockService = new LockManager(openedDatabase.dataSource(), selectedConfig.leaseSeconds(),
                        selectedConfig.servicePort() + 2);
            } else {
                lockService = new ReconnectingLockService(selectedConfig.coordinatorHost(),
                        selectedConfig.coordinatorPort(), selectedConfig.servicePort() + 2);
            }
            Registry remoteRegistry = LocateRegistry.getRegistry(selectedConfig.peerHost(),
                    selectedConfig.peerPort());
            WarehouseServiceImpl warehouseService = new WarehouseServiceImpl(openedDatabase.dataSource(),
                    lockService, selectedConfig.serverId(), selectedConfig.servicePort(),
                    selectedConfig.peerHost(), selectedConfig.peerPort(), remoteRegistry, activities);
            SyncServiceImpl syncService = new SyncServiceImpl(openedDatabase.dataSource(),
                    selectedConfig.serverId(), selectedConfig.servicePort() + 1, activities);
            openedRegistry.rebind(RmiNames.WAREHOUSE, warehouseService);
            openedRegistry.rebind(RmiNames.LOCK, lockService);
            openedRegistry.rebind(RmiNames.SYNC, syncService);
            openedWarehouse = warehouseService;
            openedSync = syncService;
            openedLock = (Remote) lockService;

            database = openedDatabase;
            registry = openedRegistry;
            peerRegistry = remoteRegistry;
            warehouse = warehouseService;
            sync = syncService;
            lock = (Remote) lockService;
            ownsRegistry = createdRegistry;
            activities.accept(SyncActivity.success(selectedConfig.serverId(), "RMI SẴN SÀNG",
                    "Registry " + selectedConfig.registryPort() + " · dịch vụ " + selectedConfig.servicePort()));
            return new RuntimeStatus(true, true, peerAvailable(), lockCoordinatorAvailable(), products);
        } catch (Exception error) {
            unbind(openedRegistry);
            unexport(openedWarehouse);
            unexport(openedSync);
            unexport(openedLock);
            if (createdRegistry) unexport(openedRegistry);
            openedDatabase.close();
            activities.accept(SyncActivity.failure(selectedConfig.serverId(), "KHỞI ĐỘNG LỖI",
                    message(error)));
            config = null;
            throw error;
        }
    }

    public synchronized RuntimeStatus status() {
        if (!isRunning()) return new RuntimeStatus(false, false, false, false, 0);
        int productCount = 0;
        boolean databaseAvailable = false;
        try {
            productCount = database.verify();
            databaseAvailable = true;
        } catch (Exception ignored) { }
        return new RuntimeStatus(true, databaseAvailable, peerAvailable(),
                lockCoordinatorAvailable(), productCount);
    }

    public synchronized boolean peerAvailable() {
        if (peerRegistry == null) return false;
        try {
            return ((SyncService) peerRegistry.lookup(RmiNames.SYNC)).ping();
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized boolean lockCoordinatorAvailable() {
        if (config == null) return false;
        if ("server-a".equalsIgnoreCase(config.serverId())) return lock != null;
        try {
            Registry coordinator = LocateRegistry.getRegistry(config.coordinatorHost(), config.coordinatorPort());
            return coordinator.lookup(RmiNames.LOCK) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized boolean isRunning() {
        return registry != null;
    }

    public List<SyncActivity> activities() {
        return activities.snapshot();
    }

    public synchronized void stop() {
        if (registry == null) return;
        unbind(registry);
        unexport(warehouse);
        unexport(sync);
        unexport(lock);
        if (ownsRegistry) unexport(registry);
        if (database != null) database.close();
        String serverId = config == null ? "server" : config.serverId();
        activities.accept(SyncActivity.success(serverId, "DỪNG", "Đã đóng RMI và database."));
        database = null;
        registry = null;
        peerRegistry = null;
        warehouse = null;
        sync = null;
        lock = null;
        config = null;
        ownsRegistry = false;
    }

    @Override
    public void close() {
        stop();
    }

    private static RegistryHandle createRegistry(int port) throws RemoteException {
        try {
            return new RegistryHandle(LocateRegistry.createRegistry(port), true);
        } catch (ExportException alreadyRunning) {
            Registry existing = LocateRegistry.getRegistry(port);
            existing.list();
            throw new RemoteException("Cổng RMI Registry " + port + " đang được sử dụng.", alreadyRunning);
        }
    }

    private static void unbind(Registry target) {
        if (target == null) return;
        try { target.unbind(RmiNames.WAREHOUSE); } catch (Exception ignored) { }
        try { target.unbind(RmiNames.LOCK); } catch (Exception ignored) { }
        try { target.unbind(RmiNames.SYNC); } catch (Exception ignored) { }
    }

    private static void unexport(Remote remote) {
        if (remote == null) return;
        try {
            UnicastRemoteObject.unexportObject(remote, true);
        } catch (RemoteException ignored) { }
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    public record RuntimeStatus(boolean running, boolean databaseAvailable, boolean peerAvailable,
                                boolean lockCoordinatorAvailable, int productCount) { }

    private record RegistryHandle(Registry registry, boolean created) { }
}
