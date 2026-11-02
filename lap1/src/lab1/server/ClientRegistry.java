package lab1.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ClientRegistry {
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    boolean register(String username, ClientHandler handler) {
        return clients.putIfAbsent(username, handler) == null;
    }

    void remove(String username, ClientHandler handler) {
        if (username != null) {
            clients.remove(username, handler);
        }
    }

    ClientHandler find(String username) {
        return clients.get(username);
    }

    List<ClientHandler> handlers() {
        return new ArrayList<>(clients.values());
    }

    List<String> usernames() {
        return clients.keySet().stream()
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    List<ServerListener.ConnectedClient> snapshots() {
        return clients.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> new ServerListener.ConnectedClient(
                        entry.getKey(),
                        extractIp(entry.getValue().remoteAddress()),
                        "Online"))
                .toList();
    }

    private static String extractIp(String remoteAddress) {
        if (remoteAddress == null) {
            return "?";
        }
        String value = remoteAddress;
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon) {
            return value.substring(0, colon);
        }
        if (value.startsWith("[") && value.contains("]:")) {
            return value.substring(1, value.indexOf("]:"));
        }
        return value;
    }
}
