package lab5.service;

import lab5.config.NetworkAddress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ServerSettingsService {
    private ServerSettingsService() { }

    public static void saveNetwork(Path file, String localHost, String peerHost, String coordinatorHost) {
        try {
            Properties values = new Properties();
            if (Files.exists(file)) {
                try (var input = Files.newInputStream(file)) {
                    values.load(input);
                }
            }
            values.setProperty("server.host", localHost);
            values.setProperty("peer.server.host", peerHost);
            values.setProperty("lock.coordinator.host", coordinatorHost);
            NetworkAddress.saveProperties(file, values);
        } catch (Exception ignored) {
            // Không làm hỏng phiên server chỉ vì không lưu được cấu hình tiện ích.
        }
    }
}
