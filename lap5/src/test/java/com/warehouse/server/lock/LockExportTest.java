package com.warehouse.server.lock;

import com.warehouse.common.rmi.LockService;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import static org.junit.jupiter.api.Assertions.*;

class LockExportTest {
    @Test void exportsCoordinatorOnRequestedPort() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        LockManager manager = new LockManager(null, 30, port);
        try {
            LockService stub = (LockService) RemoteObject.toStub(manager);
            assertTrue(stub.toString().contains(":" + port));
            RemoteException error = assertThrows(RemoteException.class, () -> stub.acquire(null, "client"));
            assertTrue(error.getMessage().contains("Client ID"));
        } finally { UnicastRemoteObject.unexportObject(manager, true); }
    }
}
