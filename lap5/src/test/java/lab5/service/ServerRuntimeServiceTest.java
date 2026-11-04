package lab5.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRuntimeServiceTest {
    @Test
    void newRuntimeIsStoppedAndCloseIsIdempotent() {
        ServerRuntimeService runtime = new ServerRuntimeService();
        assertFalse(runtime.isRunning());
        assertFalse(runtime.status().running());
        runtime.stop();
        runtime.close();
        assertFalse(runtime.isRunning());
        assertTrue(runtime.activities().isEmpty());
    }
}
