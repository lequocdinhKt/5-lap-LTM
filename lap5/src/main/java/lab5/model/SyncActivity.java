package lab5.model;

import java.time.Instant;

public record SyncActivity(Instant time, String source, String action, String detail, boolean success) {
    public static SyncActivity success(String source, String action, String detail) {
        return new SyncActivity(Instant.now(), source, action, detail, true);
    }

    public static SyncActivity failure(String source, String action, String detail) {
        return new SyncActivity(Instant.now(), source, action, detail, false);
    }
}
