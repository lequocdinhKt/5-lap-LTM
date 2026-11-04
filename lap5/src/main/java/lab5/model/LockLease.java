package lab5.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record LockLease(UUID resourceId, String ownerClientId, UUID lockToken,
                        long fencingToken, Instant acquiredAt, Instant expiresAt)
        implements Serializable {
}
