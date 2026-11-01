package lab1.common;

public enum RecipientMode {
    DIRECT("Gửi riêng"),
    MULTICAST("Gửi nhóm"),
    BROADCAST("Gửi tất cả");

    private final String displayName;

    RecipientMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
