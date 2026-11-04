package lab5.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Map;

public record ClientConfig(String serverHost, int registryPort, String clientId, String username) {
    public static ClientConfig load(String file) throws IOException {
        return load(file, System.getenv());
    }

    static ClientConfig load(String file, Map<String, String> environment) throws IOException {
        Properties p = new Properties();
        if (file != null && !file.isBlank() && Files.exists(Path.of(file))) {
            try (InputStream in = Files.newInputStream(Path.of(file))) { p.load(in); }
        }
        return new ClientConfig(
                ConfigValues.text(p, environment, "server.host", "LAB5_SERVER_HOST", "127.0.0.1"),
                ConfigValues.number(p, environment, "rmi.registry.port", "LAB5_RMI_REGISTRY_PORT", 1099),
                ConfigValues.text(p, environment, "client.id", "LAB5_CLIENT_ID", "client-a"),
                ConfigValues.text(p, environment, "client.username", "LAB5_CLIENT_USERNAME", ""));
    }

    public ClientConfig withClientId(String value) {
        return new ClientConfig(serverHost, registryPort,
                value == null || value.isBlank() ? clientId : value.trim(), username);
    }

    public ClientConfig withConnection(String host, int port) {
        return new ClientConfig(host == null || host.isBlank() ? serverHost : host.trim(),
                port, clientId, username);
    }
}
