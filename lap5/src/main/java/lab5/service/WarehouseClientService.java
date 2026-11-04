package lab5.service;

import lab5.model.InventoryTransaction;
import lab5.model.LockLease;
import lab5.model.LoginResult;
import lab5.model.Product;
import lab5.model.ProductInput;
import lab5.model.RegistrationResult;
import lab5.network.rmi.WarehouseClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class WarehouseClientService implements AutoCloseable {
    private final WarehouseClient client;
    private final String clientId;
    private final ExecutorService networkExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private volatile LockLease currentLease;
    private volatile Consumer<String> leaseStatus = ignored -> { };

    public WarehouseClientService(WarehouseClient client, String clientId) {
        this.client = client;
        this.clientId = clientId;
        this.networkExecutor = Executors.newFixedThreadPool(3, task -> daemon(task, "lab5-client-network"));
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
                task -> daemon(task, "lab5-lock-heartbeat"));
        heartbeatExecutor.scheduleWithFixedDelay(this::renewLease, 10, 10, TimeUnit.SECONDS);
    }

    public static CompletableFuture<WarehouseClientService> connect(
            String host, int port, String clientId, ExecutorService connector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new WarehouseClientService(WarehouseClient.connect(host, port), clientId);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }, connector);
    }

    public void onLeaseStatus(Consumer<String> listener) {
        leaseStatus = listener == null ? ignored -> { } : listener;
    }

    public CompletableFuture<LoginResult> login(String username, String password) {
        return remote(() -> client.warehouse().login(username, password));
    }

    public CompletableFuture<RegistrationResult> register(String username, String password, String fullName) {
        return remote(() -> client.warehouse().register(username, password, fullName));
    }

    public CompletableFuture<List<Product>> listProducts(String search) {
        return remote(() -> client.warehouse().listProducts(search));
    }

    public CompletableFuture<Boolean> isPeerAvailable() {
        return remote(() -> client.warehouse().isPeerAvailable());
    }

    public CompletableFuture<Product> createProduct(ProductInput input, String username) {
        return remote(() -> client.warehouse().createProduct(input, clientId, username));
    }

    public CompletableFuture<LockLease> acquireLease(UUID productId) {
        return remote(() -> client.locks().acquire(productId, clientId))
                .thenApply(lease -> {
                    currentLease = lease;
                    leaseStatus.accept("Đã giữ quyền thao tác sản phẩm.");
                    return lease;
                });
    }

    public CompletableFuture<Product> updateProduct(UUID productId, ProductInput input, String username) {
        LockLease lease = requireLease(productId);
        return remote(() -> client.warehouse().updateProduct(productId, input, lease.lockToken(),
                lease.fencingToken(), clientId, username)).whenComplete((ignored, error) -> releaseQuietly());
    }

    public CompletableFuture<Void> deleteProduct(UUID productId, String username) {
        LockLease lease = requireLease(productId);
        return this.<Void>remote(() -> {
            client.warehouse().deleteProduct(productId, lease.lockToken(), lease.fencingToken(), clientId, username);
            return null;
        }).whenComplete((ignored, error) -> releaseQuietly());
    }

    public CompletableFuture<InventoryTransaction> changeInventory(
            UUID productId, int quantity, boolean importing, String username) {
        LockLease lease = requireLease(productId);
        return remote(() -> client.warehouse().changeInventory(productId, quantity, importing,
                lease.lockToken(), lease.fencingToken(), clientId, username))
                .whenComplete((ignored, error) -> releaseQuietly());
    }

    public CompletableFuture<List<InventoryTransaction>> transactionHistory(UUID productId) {
        return remote(() -> client.warehouse().transactionHistory(productId));
    }

    public CompletableFuture<Void> releaseLease() {
        LockLease lease = currentLease;
        currentLease = null;
        if (lease == null) return CompletableFuture.completedFuture(null);
        return this.<Void>remote(() -> {
            client.locks().release(lease.lockToken(), clientId);
            return null;
        });
    }

    public LockLease currentLease() {
        return currentLease;
    }

    private LockLease requireLease(UUID productId) {
        LockLease lease = currentLease;
        if (lease == null || !lease.resourceId().equals(productId)) {
            throw new IllegalStateException("Chưa giữ quyền thao tác sản phẩm.");
        }
        return lease;
    }

    private void renewLease() {
        LockLease lease = currentLease;
        if (lease == null) return;
        try {
            currentLease = client.locks().heartbeat(lease.lockToken(), clientId);
        } catch (Exception error) {
            currentLease = null;
            leaseStatus.accept("Phiên thao tác đã hết hạn hoặc mất kết nối.");
        }
    }

    private void releaseQuietly() {
        LockLease lease = currentLease;
        currentLease = null;
        if (lease == null) return;
        try {
            client.locks().release(lease.lockToken(), clientId);
        } catch (Exception error) {
            leaseStatus.accept("Chưa giải phóng được khóa; khóa sẽ tự hết hạn.");
        }
    }

    private <T> CompletableFuture<T> remote(RemoteCall<T> call) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call.invoke();
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }, networkExecutor);
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void close() {
        LockLease lease = currentLease;
        currentLease = null;
        heartbeatExecutor.shutdownNow();
        if (lease != null) {
            networkExecutor.execute(() -> {
                try {
                    client.locks().release(lease.lockToken(), clientId);
                } catch (Exception ignored) {
                    // Lease tự hết hạn nếu kết nối đã đóng.
                }
            });
        }
        networkExecutor.shutdown();
    }

    @FunctionalInterface
    private interface RemoteCall<T> {
        T invoke() throws Exception;
    }
}
