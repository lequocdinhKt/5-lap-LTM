package lab1.client;

import lab1.common.GroupInfo;
import lab1.common.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Nhan UDP multicast/broadcast text tren mot cong dung chung cho moi client. */
final class ClientUdpText implements Closeable, Runnable {
    private final int port;
    private final MulticastSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, List<NetworkInterface>> joined = new HashMap<>();
    private final Set<Long> receivedIds = ConcurrentHashMap.newKeySet();
    private volatile Set<String> allowedGroupIds = Set.of(GroupRegistryIds.ALL);
    private volatile Listener listener;

    ClientUdpText(int port) throws IOException {
        this.port = port;
        socket = new MulticastSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        Thread thread = new Thread(this, "client-udp-text");
        thread.setDaemon(true);
        thread.start();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    synchronized void syncGroups(List<GroupInfo> groups) {
        Set<String> wantedAddresses = new HashSet<>();
        Set<String> wantedIds = new HashSet<>();
        wantedIds.add(GroupRegistryIds.ALL);
        for (GroupInfo group : groups) {
            wantedIds.add(group.id());
            if (!group.systemGroup() && !group.multicastAddress().isBlank()) {
                wantedAddresses.add(group.multicastAddress());
            }
        }
        for (String oldAddress : new ArrayList<>(joined.keySet())) {
            if (!wantedAddresses.contains(oldAddress)) leave(oldAddress);
        }
        for (String address : wantedAddresses) {
            if (!joined.containsKey(address)) join(address);
        }
        allowedGroupIds = Set.copyOf(wantedIds);
    }

    private void join(String addressText) {
        List<NetworkInterface> successful = new ArrayList<>();
        try {
            InetAddress address = InetAddress.getByName(addressText);
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                try {
                    if (network.isUp() && (network.supportsMulticast() || network.isLoopback())) {
                        socket.joinGroup(new InetSocketAddress(address, port), network);
                        successful.add(network);
                    }
                } catch (IOException ignored) {
                }
            }
            joined.put(addressText, successful);
        } catch (IOException ignored) {
            // Catalog TCP van hoat dong; UI se khong nhan UDP neu card mang chan multicast.
        }
    }

    private void leave(String addressText) {
        try {
            InetAddress address = InetAddress.getByName(addressText);
            List<NetworkInterface> networks = joined.remove(addressText);
            if (networks != null) {
                for (NetworkInterface network : networks) {
                    try {
                        socket.leaveGroup(new InetSocketAddress(address, port), network);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[Protocol.MAX_UDP_TEXT_PACKET];
        while (running.get()) {
            try {
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                socket.receive(datagram);
                Protocol.UdpTextPacket message = Protocol.decodeUdpText(
                        datagram.getData(), datagram.getLength());
                if (!allowedGroupIds.contains(message.groupId()) || !receivedIds.add(message.messageId())) {
                    continue;
                }
                if (receivedIds.size() > 5_000) receivedIds.clear();
                Listener current = listener;
                if (current != null) current.onUdpText(message);
            } catch (IOException ignored) {
                if (!running.get()) return;
            }
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        socket.close();
        joined.clear();
    }

    interface Listener {
        void onUdpText(Protocol.UdpTextPacket message);
    }

    private static final class GroupRegistryIds {
        static final String ALL = "all";
    }
}
