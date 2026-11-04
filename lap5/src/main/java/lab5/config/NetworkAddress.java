package lab5.config;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

/** Tiện ích mạng thuần Java, không phụ thuộc Swing hoặc JavaFX. */
public final class NetworkAddress {
    private NetworkAddress() {
    }

    public static List<String> localIpv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> candidates = networkInterface.getInetAddresses();
                while (candidates.hasMoreElements()) {
                    InetAddress address = candidates.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // UI vẫn cho phép nhập IP thủ công khi không liệt kê được card mạng.
        }
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(addresses));
        Collections.sort(unique);
        return unique;
    }

    public static boolean isIpv4(String value) {
        if (value == null || value.isBlank()) return false;
        String[] parts = value.trim().split("\\.", -1);
        if (parts.length != 4) return false;
        try {
            for (String part : parts) {
                if (part.isBlank() || (part.length() > 1 && part.startsWith("0"))) return false;
                int number = Integer.parseInt(part);
                if (number < 0 || number > 255) return false;
            }
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static void saveProperties(Path path, Properties properties) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (var output = Files.newOutputStream(path)) {
            properties.store(output, "Lab 5 local settings");
        }
    }
}
