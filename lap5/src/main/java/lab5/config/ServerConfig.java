package lab5.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Map;

public record ServerConfig(String serverId, String serverHost, int registryPort, int servicePort,
                           String peerHost, int peerPort, String coordinatorHost,
                           int coordinatorPort, String dbUrl, String dbUsername,
                           String dbPassword, int leaseSeconds) {
    public static ServerConfig load(String file) throws IOException {
        return load(file, System.getenv());
    }

    static ServerConfig load(String file, Map<String, String> environment) throws IOException {
        Properties p = new Properties();
        if (file != null && !file.isBlank() && Files.exists(Path.of(file))) {
            try (InputStream in = Files.newInputStream(Path.of(file))) {
                p.load(in);
            }
        }
        return new ServerConfig(
                ConfigValues.required(p, environment, "server.id", "LAB5_SERVER_ID"),
                ConfigValues.text(p, environment, "server.host", "LAB5_SERVER_HOST", "127.0.0.1"),
                ConfigValues.number(p, environment, "rmi.registry.port", "LAB5_RMI_REGISTRY_PORT", 1099),
                ConfigValues.number(p, environment, "rmi.service.port", "LAB5_RMI_SERVICE_PORT", 1100),
                ConfigValues.required(p, environment, "peer.server.host", "LAB5_PEER_HOST"),
                ConfigValues.number(p, environment, "peer.rmi.port", "LAB5_PEER_RMI_PORT", 1099),
                ConfigValues.required(p, environment, "lock.coordinator.host", "LAB5_LOCK_COORDINATOR_HOST"),
                ConfigValues.number(p, environment, "lock.coordinator.port", "LAB5_LOCK_COORDINATOR_PORT", 1099),
                ConfigValues.required(p, environment, "db.url", "LAB5_DB_URL"),
                ConfigValues.required(p, environment, "db.username", "LAB5_DB_USER"),
                ConfigValues.text(p, environment, "db.password", "LAB5_DB_PASSWORD", ""),
                Integer.parseInt(ConfigValues.text(p, environment, "lock.lease.seconds",
                        "LAB5_LOCK_LEASE_SECONDS", "30")));
    }

    public ServerConfig withNetwork(String localHost, String remoteHost, int selectedRegistryPort) {
        String coordinator = "server-a".equalsIgnoreCase(serverId) ? localHost : remoteHost;
        return new ServerConfig(serverId, localHost, selectedRegistryPort, servicePort,
                remoteHost, peerPort, coordinator, coordinatorPort, dbUrl, dbUsername,
                dbPassword, leaseSeconds);
    }
}
