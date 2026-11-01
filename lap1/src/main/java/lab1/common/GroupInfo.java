package lab1.common;

/** Thong tin nhom ma server gui cho mot client qua TCP. */
public record GroupInfo(String id, String name, String owner, boolean systemGroup,
                        String multicastAddress, int memberCount, Membership membership) {
    public enum Membership {
        OWNER, MEMBER
    }
}
