package lab5.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPrecedenceTest {
    @TempDir Path directory;

    @Test
    void environmentOverridesClientProperties() throws Exception {
        Path file = directory.resolve("client.properties");
        Files.writeString(file, "server.host=10.0.0.1\nrmi.registry.port=1099\nclient.id=file-client\n");
        ClientConfig config = ClientConfig.load(file.toString(), Map.of(
                "LAB5_SERVER_HOST", "10.0.0.8",
                "LAB5_RMI_REGISTRY_PORT", "1199",
                "LAB5_CLIENT_ID", "env-client"));
        assertEquals("10.0.0.8", config.serverHost());
        assertEquals(1199, config.registryPort());
        assertEquals("env-client", config.clientId());
    }

    @Test
    void serverRejectsMissingRequiredValues() throws Exception {
        Path file = directory.resolve("server.properties");
        Files.writeString(file, "server.id=server-a\n");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(file.toString(), Map.of()));
        assertTrue(error.getMessage().contains("peer.server.host"));
    }

    @Test
    void commandStyleOverridesCreateNewImmutableConfig() {
        ClientConfig base = new ClientConfig("127.0.0.1", 1099, "client-a", "staff_a");
        ClientConfig selected = base.withConnection("192.168.1.10", 1199).withClientId("client-b");
        assertEquals("127.0.0.1", base.serverHost());
        assertEquals("192.168.1.10", selected.serverHost());
        assertEquals(1199, selected.registryPort());
        assertEquals("client-b", selected.clientId());
    }
}
