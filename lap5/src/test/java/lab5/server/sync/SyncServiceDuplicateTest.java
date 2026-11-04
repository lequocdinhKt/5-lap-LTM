package lab5.server.sync;

import lab5.model.InventoryTransaction;
import lab5.model.Product;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SyncServiceDuplicateTest {
    @Test void duplicateInventoryDoesNotOverwriteNewerQuantity() throws Exception {
        try (Fixture f = new Fixture()) {
            var first = f.tx(10, 15);
            assertTrue(f.service.syncInventory(f.id, 15, 2, first));
            assertTrue(f.service.syncInventory(f.id, 20, 2, f.tx(15, 20)));
            assertTrue(f.service.syncInventory(f.id, 15, 2, first));
            assertEquals(20, f.quantity);
            assertEquals(2, f.updates);
            assertEquals(2, f.seen.size());
        }
    }
    @Test void rejectsStaleAndOutOfOrderInventoryWithoutWriting() throws Exception {
        try (Fixture f = new Fixture()) {
            f.fence = 3;
            assertThrows(RemoteException.class, () -> f.service.syncInventory(f.id, 15, 2, f.tx(10, 15)));
            assertThrows(RemoteException.class, () -> f.service.syncInventory(f.id, 15, 3, f.tx(11, 15)));
            assertEquals(10, f.quantity);
            assertEquals(0, f.updates);
            assertEquals(2, f.rollbacks);
        }
    }
    @Test void conflictingProductCodeCannotOverwriteAnotherUuid() throws Exception {
        try (Fixture f = new Fixture()) {
            f.duplicateProduct = true;
            Product other = new Product(UUID.randomUUID(), "SP001", "Other", "Test",
                    BigDecimal.ONE, 0, 0, 0, false, Instant.now());
            assertThrows(RemoteException.class, () -> f.service.syncCreateProduct(other));
            assertTrue(f.sql.stream().noneMatch(q -> q.contains("ON DUPLICATE KEY UPDATE")));
        }
    }
    @Test void repeatedCreateForSameIdentityIsAcknowledged() throws Exception {
        try (Fixture f = new Fixture()) {
            f.duplicateProduct = true;
            Product same = new Product(f.id, "SP001", "Same", "Test",
                    BigDecimal.ONE, 0, 0, 0, false, Instant.now());
            assertTrue(f.service.syncCreateProduct(same));
        }
    }
    @Test void missingTransactionUserRejectsDelivery() throws Exception {
        try (Fixture f = new Fixture()) {
            f.userExists = false;
            assertThrows(RemoteException.class, () -> f.service.syncInventory(f.id, 15, 2, f.tx(10, 15)));
            assertEquals(1, f.rollbacks);
            assertTrue(f.seen.isEmpty());
        }
    }
    @Test void userSyncPreservesIdentityAndRejectsConflict() throws Exception {
        try (Fixture f = new Fixture()) {
            UUID id = UUID.randomUUID();
            assertTrue(f.service.syncUser(id, "new_staff", "password", "Name"));
            assertEquals(id.toString(), f.userParameters.get(1));
            assertEquals("new_staff", f.userParameters.get(2));
            f.duplicateUser = true;
            assertThrows(RemoteException.class, () -> f.service.syncUser(UUID.randomUUID(), "new_staff", "other", "Other"));
        }
    }

    static final class Fixture implements AutoCloseable {
        final UUID id = UUID.randomUUID();
        final UUID user = UUID.randomUUID();
        final Set<String> seen = new HashSet<>();
        final List<String> sql = new ArrayList<>();
        Map<Integer,Object> userParameters;
        int quantity = 10, updates, rollbacks;
        long fence;
        boolean duplicateProduct, duplicateUser, userExists = true;
        final SyncServiceImpl service;
        Fixture() throws Exception {
            Connection c = proxy(Connection.class, (method, args) -> switch (method) {
                case "prepareStatement" -> statement((String) args[0]);
                case "rollback" -> { rollbacks++; yield null; }
                case "setAutoCommit", "commit", "close" -> null;
                default -> throw new UnsupportedOperationException(method);
            });
            DataSource ds = proxy(DataSource.class, (method,args) -> c);
            service = new SyncServiceImpl(ds, "server-b", 0);
        }
        InventoryTransaction tx(int before, int after) {
            return new InventoryTransaction(UUID.randomUUID(), UUID.randomUUID().toString(), id,
                    "IMPORT", after-before, before, after, "staff_a", Instant.now());
        }
        PreparedStatement statement(String query) {
            sql.add(query);
            Map<Integer,Object> params = new HashMap<>();
            return proxy(PreparedStatement.class, (method,args) -> {
                if (method.startsWith("set")) { params.put((Integer)args[0], args[1]); return null; }
                return switch(method) {
                    case "close" -> null;
                    case "executeUpdate" -> {
                        if (query.startsWith("INSERT INTO products") && duplicateProduct
                                || query.startsWith("INSERT INTO users") && duplicateUser)
                            throw new SQLException("duplicate", "23000", 1062);
                        if (query.startsWith("INSERT INTO users")) userParameters = params;
                        if (query.startsWith("UPDATE products SET quantity")) {
                            quantity = (Integer)params.get(1); fence = (Long)params.get(2); updates++;
                        }
                        if (query.startsWith("INSERT INTO inventory_transactions")) seen.add((String)params.get(1));
                        yield 1;
                    }
                    case "executeQuery" -> {
                        boolean exists = query.contains("FROM inventory_transactions") ? seen.contains(params.get(1))
                                : query.contains("FROM users") ? userExists
                                : query.startsWith("SELECT product_code") ? id.toString().equals(params.get(1)) : true;
                        boolean[] delivered = {false};
                        yield proxy(ResultSet.class, (rm,ra) -> switch(rm) {
                            case "next" -> { boolean result = exists && !delivered[0]; delivered[0]=true; yield result; }
                            case "getInt" -> quantity;
                            case "getLong" -> fence;
                            case "getBoolean" -> false;
                            case "getString" -> query.startsWith("SELECT product_code") ? "SP001" : user.toString();
                            case "close" -> null;
                            default -> throw new UnsupportedOperationException(rm);
                        });
                    }
                    default -> throw new UnsupportedOperationException(method);
                };
            });
        }
        public void close() throws Exception { UnicastRemoteObject.unexportObject(service, true); }
    }
    interface Call { Object call(String method, Object[] args) throws Throwable; }
    static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (p,m,a) -> call.call(m.getName(),a)));
    }
}
