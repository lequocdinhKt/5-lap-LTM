package lab5.server.sync;

import java.util.Locale;

/** Giá trị được lưu trong các cột ENUM của MySQL. */
public final class SyncValues {
    private SyncValues() { }

    public static String origin(String serverId) {
        if (serverId == null) throw new IllegalArgumentException("Server ID is required");
        return switch (serverId.trim().toLowerCase(Locale.ROOT)) {
            case "server-a", "a" -> "A";
            case "server-b", "b" -> "B";
            default -> throw new IllegalArgumentException("Unknown server ID: " + serverId);
        };
    }

    public static String action(String action) {
        if (action == null) throw new IllegalArgumentException("Action is required");
        return switch (action.trim().toUpperCase(Locale.ROOT)) {
            case "CREATE", "INSERT" -> "INSERT";
            case "UPDATE", "DELETE", "IMPORT", "EXPORT" -> action.trim().toUpperCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("Unknown sync action: " + action);
        };
    }

    public static String transactionType(boolean importing) {
        return importing ? "IMPORT" : "EXPORT";
    }
}
