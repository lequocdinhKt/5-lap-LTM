package lab5.server.service;

import lab5.network.rmi.SyncService;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseRegistrationTest {
    @Test void commitsOnlyAfterPeerReceivesSameUserIdentity() throws Exception {
        register(true);
    }
    @Test void rollsBackWhenPeerRejectsRegistration() throws Exception {
        register(false);
    }
    private void register(boolean accepted) throws Exception {
        List<String> actions = new ArrayList<>();
        Map<Integer,Object> parameters = new HashMap<>();
        PreparedStatement statement = proxy(PreparedStatement.class, (method,args) -> {
            if (method.startsWith("set")) { parameters.put((Integer)args[0], args[1]); return null; }
            if (method.equals("executeUpdate")) { actions.add("insert"); return 1; }
            return null;
        });
        Connection connection = proxy(Connection.class, (method,args) -> {
            if (method.equals("prepareStatement")) return statement;
            if (method.equals("commit") || method.equals("rollback")) actions.add(method);
            return null;
        });
        DataSource ds = proxy(DataSource.class, (method,args) -> connection);
        SyncService peer = proxy(SyncService.class, (method,args) -> {
            if (method.equals("ping")) return true;
            assertEquals("syncUser", method);
            assertEquals(parameters.get(1), args[0].toString());
            assertEquals("staff_new", args[1]);
            actions.add("peer");
            return accepted;
        });
        Registry registry = proxy(Registry.class, (method,args) -> peer);
        WarehouseServiceImpl service = new WarehouseServiceImpl(ds, null, "server-a", 0, registry);
        try {
            if (accepted) assertTrue(service.register("staff_new", "password", "New Staff").registered());
            else assertThrows(RemoteException.class, () -> service.register("staff_new", "password", "New Staff"));
            assertEquals(List.of("insert", "peer", accepted ? "commit" : "rollback"), actions);
        } finally { UnicastRemoteObject.unexportObject(service, true); }
    }
    interface Call { Object call(String method, Object[] args) throws Throwable; }
    static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (p,m,a) -> call.call(m.getName(),a)));
    }
}
