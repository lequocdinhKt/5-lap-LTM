package lab5.service;

import lab5.config.NetworkAddress;

import java.nio.file.Path;
import java.util.Properties;

public final class ClientSettingsService {
    private ClientSettingsService() { }

    public static void save(Path file, String host, int port, String clientId, String username) {
        try {
            Properties values = new Properties();
            values.setProperty("server.host", host);
            values.setProperty("rmi.registry.port", String.valueOf(port));
            values.setProperty("client.id", clientId);
            values.setProperty("client.username", username == null ? "" : username.trim());
            NetworkAddress.saveProperties(file, values);
        } catch (Exception ignored) {
            // Kết nối vẫn dùng được; việc lưu địa chỉ chỉ là tiện ích cục bộ.
        }
    }
}
