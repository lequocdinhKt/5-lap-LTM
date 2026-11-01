package lab1.server;

import lab1.common.GroupInfo;
import lab1.common.Protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Luu nhom trong RAM. Nhom se duoc lam moi khi server khoi dong lai. */
final class GroupRegistry {
    static final String ALL_GROUP_ID = "all";
    static final String ALL_GROUP_NAME = "Tất cả";

    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final AtomicInteger nextMulticast = new AtomicInteger(1);

    synchronized Group create(String name, String owner, List<String> selectedMembers,
                              List<String> onlineUsers) throws IOException {
        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.length() > 40) {
            throw new IOException("Ten nhom phai co tu 1 den 40 ky tu.");
        }
        if (ALL_GROUP_NAME.equalsIgnoreCase(normalized)
                || groups.values().stream().anyMatch(g -> g.name.equalsIgnoreCase(normalized))) {
            throw new IOException("Ten nhom da ton tai.");
        }
        Group group = new Group(UUID.randomUUID().toString(), normalized, owner, nextAddress());
        replaceMembers(group, owner, selectedMembers, onlineUsers);
        groups.put(group.id, group);
        return group;
    }

    synchronized void updateMembers(String id, String actor, List<String> selectedMembers,
                                    List<String> onlineUsers) throws IOException {
        Group group = requireUserGroup(id);
        requireOwner(group, actor);
        replaceMembers(group, actor, selectedMembers, onlineUsers);
    }

    synchronized void delete(String id, String actor) throws IOException {
        Group group = requireUserGroup(id);
        requireOwner(group, actor);
        groups.remove(id);
    }

    synchronized void leave(String id, String username) throws IOException {
        Group group = requireUserGroup(id);
        if (group.owner.equals(username)) {
            throw new IOException("Truong nhom phai xoa nhom thay vi roi nhom.");
        }
        if (!group.members.remove(username)) {
            throw new IOException("Ban khong phai thanh vien nhom.");
        }
    }

    synchronized Group requireMemberGroup(String id, String username) throws IOException {
        if (ALL_GROUP_ID.equals(id)) return null;
        Group group = requireUserGroup(id);
        if (!group.members.contains(username)) {
            throw new IOException("Ban khong phai thanh vien nhom.");
        }
        return group;
    }

    synchronized List<String> memberNames(String id, List<String> allOnline) throws IOException {
        if (ALL_GROUP_ID.equals(id)) return List.copyOf(allOnline);
        return List.copyOf(requireUserGroup(id).members);
    }

    synchronized List<String> membersForOwner(String id, String actor) throws IOException {
        Group group = requireUserGroup(id);
        requireOwner(group, actor);
        return List.copyOf(group.members);
    }

    synchronized List<GroupInfo> catalogFor(String username, int onlineCount) {
        List<GroupInfo> result = new ArrayList<>();
        result.add(new GroupInfo(ALL_GROUP_ID, ALL_GROUP_NAME, "Hệ thống", true,
                "", onlineCount, GroupInfo.Membership.MEMBER));
        groups.values().stream()
                .filter(group -> group.members.contains(username))
                .sorted(Comparator.comparing(group -> group.name.toLowerCase()))
                .map(group -> group.infoFor(username))
                .forEach(result::add);
        return result;
    }

    private void replaceMembers(Group group, String owner, List<String> selected,
                                List<String> onlineUsers) throws IOException {
        LinkedHashSet<String> requested = new LinkedHashSet<>(selected);
        requested.add(owner);
        LinkedHashSet<String> allowed = new LinkedHashSet<>(onlineUsers);
        allowed.addAll(group.members); // giu duoc thanh vien dang tam offline khi sua nhom
        if (requested.size() > Protocol.MAX_CLIENTS || !allowed.containsAll(requested)) {
            throw new IOException("Thanh vien phai la nguoi dang online.");
        }
        group.members.clear();
        group.members.addAll(requested);
    }

    private String nextAddress() throws IOException {
        int value = nextMulticast.getAndIncrement();
        if (value > 64_515) throw new IOException("Da het dia chi multicast cho phien nay.");
        int third = value / 254;
        int fourth = value % 254 + 1;
        return "239.10." + third + "." + fourth;
    }

    private Group requireUserGroup(String id) throws IOException {
        Group group = groups.get(id);
        if (group == null) throw new IOException("Nhom khong ton tai.");
        return group;
    }

    private void requireOwner(Group group, String actor) throws IOException {
        if (!group.owner.equals(actor)) throw new IOException("Chi truong nhom duoc thao tac.");
    }

    static final class Group {
        final String id;
        final String name;
        final String owner;
        final String multicastAddress;
        final LinkedHashSet<String> members = new LinkedHashSet<>();

        Group(String id, String name, String owner, String multicastAddress) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.multicastAddress = multicastAddress;
        }

        GroupInfo infoFor(String username) {
            GroupInfo.Membership membership = owner.equals(username)
                    ? GroupInfo.Membership.OWNER : GroupInfo.Membership.MEMBER;
            return new GroupInfo(id, name, owner, false, multicastAddress, members.size(), membership);
        }
    }
}
