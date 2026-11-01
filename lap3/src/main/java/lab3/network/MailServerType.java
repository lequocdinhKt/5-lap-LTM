package lab3.network;

public enum MailServerType {
    LOCALHOST("Localhost"),
    GMAIL("Gmail");

    private final String displayName;

    MailServerType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
