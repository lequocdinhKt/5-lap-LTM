package lab5.model;

import java.io.Serializable;

public record LoginResult(boolean authenticated, String username, String fullName,
                          String role, String message) implements Serializable {
}
