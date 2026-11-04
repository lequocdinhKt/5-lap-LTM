package lab5.model;

import java.io.Serializable;

public record RegistrationResult(boolean registered, String username, String message)
        implements Serializable {
}
