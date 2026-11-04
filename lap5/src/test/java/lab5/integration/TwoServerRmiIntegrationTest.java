package lab5.integration;

import lab5.config.ServerConfig;
import lab5.model.LockLease;
import lab5.model.InventoryTransaction;
import lab5.model.Product;
import lab5.model.ProductInput;
import lab5.network.rmi.RmiNames;
import lab5.network.rmi.SyncService;
import lab5.network.rmi.WarehouseClient;
import lab5.service.ServerRuntimeService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TwoServerRmiIntegrationTest {
    @Test
    void twoIndependentDatabasesSynchronizeAndRollbackWhenPeerStops() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("LAB5_INTEGRATION")),
                "Chỉ chạy qua run-integration-tests.ps1");
        String user = required("LAB5_TEST_DB_USER");
        String password = System.getenv().getOrDefault("LAB5_TEST_DB_PASSWORD", "");
        ServerConfig configA = config("server-a", 1199, 1200, 1299,
                required("LAB5_TEST_DB_URL_A"), user, password);
        ServerConfig configB = config("server-b", 1299, 1300, 1199,
                required("LAB5_TEST_DB_URL_B"), user, password);

        try (ServerRuntimeService serverA = new ServerRuntimeService();
             ServerRuntimeService serverB = new ServerRuntimeService()) {
            serverA.start(configA);
            serverB.start(configB);
            WarehouseClient clientA = WarehouseClient.connect("127.0.0.1", 1199);
            WarehouseClient clientB = WarehouseClient.connect("127.0.0.1", 1299);

            String username = "integration_" + System.nanoTime();
            assertTrue(clientA.warehouse().register(username, "Demo@12345", "Tài khoản tích hợp").registered());
            assertTrue(clientB.warehouse().login(username, "Demo@12345").authenticated());

            Product created = clientA.warehouse().createProduct(
                    new ProductInput("IT-RMI-001", "Sản phẩm tích hợp", "Kiểm thử", BigDecimal.valueOf(12345)),
                    "integration-a", "staff_a");
            assertTrue(hasCode(clientB.warehouse().listProducts("IT-RMI-001"), "IT-RMI-001"));

            LockLease lease = clientB.locks().acquire(created.id(), "integration-b");
            Product updated = clientB.warehouse().updateProduct(created.id(),
                    new ProductInput("IT-RMI-001", "Đã cập nhật từ B", "Kiểm thử", BigDecimal.valueOf(13000)),
                    lease.lockToken(), lease.fencingToken(), "integration-b", "staff_b");
            clientB.locks().release(lease.lockToken(), "integration-b");
            assertEquals("Đã cập nhật từ B", updated.productName());
            assertEquals("Đã cập nhật từ B", clientA.warehouse().listProducts("IT-RMI-001").get(0).productName());

            LockLease inventoryLease = clientA.locks().acquire(created.id(), "integration-a");
            InventoryTransaction transaction = clientA.warehouse().changeInventory(created.id(), 10, true,
                    inventoryLease.lockToken(), inventoryLease.fencingToken(), "integration-a", "staff_a");
            clientA.locks().release(inventoryLease.lockToken(), "integration-a");
            List<InventoryTransaction> history = clientB.warehouse().transactionHistory(created.id());
            assertEquals(transaction.id(), history.get(0).id());
            assertEquals(10, clientB.warehouse().listProducts("IT-RMI-001").get(0).quantity());

            SyncService peerSync = (SyncService) LocateRegistry.getRegistry("127.0.0.1", 1299)
                    .lookup(RmiNames.SYNC);
            assertTrue(peerSync.syncInventory(created.id(), 10, inventoryLease.fencingToken(), transaction));
            assertEquals(10, clientB.warehouse().listProducts("IT-RMI-001").get(0).quantity());
            assertEquals(1, clientB.warehouse().transactionHistory(created.id()).size());

            LockLease first = clientA.locks().acquire(created.id(), "integration-a");
            assertThrows(Exception.class, () -> clientB.locks().acquire(created.id(), "integration-b"));
            clientA.locks().release(first.lockToken(), "integration-a");

            LockLease newer = clientA.locks().acquire(created.id(), "integration-a");
            assertThrows(Exception.class, () -> clientA.warehouse().updateProduct(created.id(),
                    new ProductInput("IT-RMI-001", "Fencing cũ", "Kiểm thử", BigDecimal.ONE),
                    first.lockToken(), first.fencingToken(), "integration-a", "staff_a"));
            clientA.locks().release(newer.lockToken(), "integration-a");

            LockLease expiring = clientA.locks().acquire(created.id(), "integration-a");
            Thread.sleep(2_200);
            LockLease afterExpiry = clientB.locks().acquire(created.id(), "integration-b");
            assertTrue(afterExpiry.fencingToken() > expiring.fencingToken());
            clientB.locks().release(afterExpiry.lockToken(), "integration-b");

            Product fromB = clientB.warehouse().createProduct(
                    new ProductInput("IT-RMI-002", "Tạo từ B", "Kiểm thử", BigDecimal.TEN),
                    "integration-b", "staff_b");
            assertTrue(hasCode(clientA.warehouse().listProducts("IT-RMI-002"), "IT-RMI-002"));
            LockLease deleteLease = clientA.locks().acquire(fromB.id(), "integration-a");
            clientA.warehouse().deleteProduct(fromB.id(), deleteLease.lockToken(), deleteLease.fencingToken(),
                    "integration-a", "staff_a");
            assertFalse(hasCode(clientB.warehouse().listProducts("IT-RMI-002"), "IT-RMI-002"));

            assertThrows(Exception.class, () -> clientA.warehouse().createProduct(
                    new ProductInput("", "", "", BigDecimal.valueOf(-1)), "integration-a", "staff_a"));
            try (ServerRuntimeService busyPort = new ServerRuntimeService()) {
                assertThrows(Exception.class, () -> busyPort.start(configA));
            }
            try (ServerRuntimeService badDatabase = new ServerRuntimeService()) {
                ServerConfig invalid = config("server-a", 1399, 1400, 1299,
                        "jdbc:mysql://127.0.0.1:1/missing", user, password);
                assertThrows(Exception.class, () -> badDatabase.start(invalid));
            }

            serverB.stop();
            assertThrows(Exception.class, () -> clientA.warehouse().createProduct(
                    new ProductInput("IT-ROLLBACK", "Phải rollback", "Kiểm thử", BigDecimal.ONE),
                    "integration-a", "staff_a"));
            assertFalse(hasCode(clientA.warehouse().listProducts("IT-ROLLBACK"), "IT-ROLLBACK"));
        }
    }

    private static ServerConfig config(String id, int registryPort, int servicePort, int peerPort,
                                       String dbUrl, String user, String password) {
        return new ServerConfig(id, "127.0.0.1", registryPort, servicePort,
                "127.0.0.1", peerPort, "127.0.0.1", 1199,
                dbUrl, user, password, 2);
    }

    private static boolean hasCode(List<Product> products, String code) {
        return products.stream().anyMatch(product -> code.equals(product.productCode()));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Thiếu " + name);
        return value;
    }
}
