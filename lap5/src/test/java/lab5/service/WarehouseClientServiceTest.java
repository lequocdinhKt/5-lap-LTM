package lab5.service;

import lab5.model.*;
import lab5.network.rmi.LockService;
import lab5.network.rmi.WarehouseClient;
import lab5.network.rmi.WarehouseService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseClientServiceTest {
    @Test
    void delegatesAuthenticationAndProductQueries() throws Exception {
        Product product = product();
        WarehouseService warehouse = proxy(WarehouseService.class, (method, args) -> switch (method) {
            case "login" -> new LoginResult(true, "staff_a", "Nhân viên A", "STAFF", "OK");
            case "listProducts" -> List.of(product);
            default -> throw new UnsupportedOperationException(method);
        });
        LockService locks = proxy(LockService.class, (method, args) -> null);
        try (WarehouseClientService service = new WarehouseClientService(new WarehouseClient(warehouse, locks), "client-a")) {
            assertTrue(service.login("staff_a", "Demo@12345").get(2, TimeUnit.SECONDS).authenticated());
            assertEquals("SP001", service.listProducts("").get(2, TimeUnit.SECONDS).get(0).productCode());
        }
    }

    @Test
    void acquiresAndReleasesLeaseThroughServiceBoundary() throws Exception {
        Product product = product();
        AtomicBoolean released = new AtomicBoolean();
        LockLease lease = new LockLease(product.id(), "client-a", UUID.randomUUID(), 1,
                Instant.now(), Instant.now().plusSeconds(30));
        LockService locks = proxy(LockService.class, (method, args) -> switch (method) {
            case "acquire", "heartbeat" -> lease;
            case "release" -> { released.set(true); yield null; }
            default -> false;
        });
        WarehouseService warehouse = proxy(WarehouseService.class,
                (method, args) -> { throw new UnsupportedOperationException(method); });
        try (WarehouseClientService service = new WarehouseClientService(new WarehouseClient(warehouse, locks), "client-a")) {
            assertEquals(lease, service.acquireLease(product.id()).get(2, TimeUnit.SECONDS));
            service.releaseLease().get(2, TimeUnit.SECONDS);
            assertTrue(released.get());
            assertNull(service.currentLease());
        }
    }

    private static Product product() {
        return new Product(UUID.randomUUID(), "SP001", "Bút", "Văn phòng phẩm",
                BigDecimal.valueOf(5000), 10, 0, 0, false, Instant.now());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> call.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Call {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
