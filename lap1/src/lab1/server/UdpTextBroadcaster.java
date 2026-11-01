package lab1.server;

import lab1.common.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/** Phat cung message-id tren LAN va loopback de demo duoc ca nhieu may lan mot may. */
final class UdpTextBroadcaster implements Closeable {
    private final int destinationPort;
    private final MulticastSocket socket;
    private final AtomicLong nextMessageId = new AtomicLong(System.nanoTime());
    private final InetAddress broadcastAddress;
    private final NetworkInterface lanInterface;
    private final NetworkInterface loopbackInterface;

    UdpTextBroadcaster(int destinationPort) throws IOException {
        this.destinationPort = destinationPort;
        socket = new MulticastSocket();
        socket.setTimeToLive(1);
        socket.setBroadcast(true);
        NetworkTarget target = findNetworkTarget();
        broadcastAddress = target.broadcastAddress();
        lanInterface = target.lanInterface();
        loopbackInterface = target.loopbackInterface();
    }

    synchronized void multicast(String groupAddress, String groupId, String sender, String text)
            throws IOException {
        byte[] payload = payload(Protocol.UDP_TEXT_MULTICAST, groupId, sender, text);
        InetAddress group = InetAddress.getByName(groupAddress);
        sendOn(lanInterface, group, payload);
        if (loopbackInterface != null && loopbackInterface != lanInterface) {
            sendOn(loopbackInterface, group, payload);
        }
    }

    synchronized void broadcast(String groupId, String sender, String text) throws IOException {
        byte[] payload = payload(Protocol.UDP_TEXT_BROADCAST, groupId, sender, text);
        sendOn(lanInterface, broadcastAddress, payload);
        if (loopbackInterface != null && loopbackInterface != lanInterface) {
            sendOn(loopbackInterface, InetAddress.getByName("127.255.255.255"), payload);
        }
    }

    String broadcastAddress() { return broadcastAddress.getHostAddress(); }
    String networkName() { return lanInterface == null ? "mac dinh" : lanInterface.getName(); }

    private byte[] payload(byte type, String groupId, String sender, String text) throws IOException {
        return Protocol.encodeUdpText(new Protocol.UdpTextPacket(type,
                nextMessageId.incrementAndGet(), System.currentTimeMillis(), groupId, sender, text));
    }

    private void sendOn(NetworkInterface network, InetAddress target, byte[] payload) throws IOException {
        if (network != null) socket.setNetworkInterface(network);
        socket.send(new DatagramPacket(payload, payload.length, target, destinationPort));
    }

    private static NetworkTarget findNetworkTarget() throws IOException {
        NetworkInterface loopback = null;
        for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!network.isUp()) continue;
            if (network.isLoopback()) {
                loopback = network;
                continue;
            }
            if (network.isVirtual()) continue;
            for (InterfaceAddress address : network.getInterfaceAddresses()) {
                if (address.getAddress() instanceof Inet4Address && address.getBroadcast() != null) {
                    return new NetworkTarget(network, address.getBroadcast(), loopbackOrFind(loopback));
                }
            }
        }
        return new NetworkTarget(null, InetAddress.getByName("255.255.255.255"),
                loopbackOrFind(loopback));
    }

    private static NetworkInterface loopbackOrFind(NetworkInterface known) throws IOException {
        if (known != null) return known;
        for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (network.isUp() && network.isLoopback()) return network;
        }
        return null;
    }

    @Override public void close() { socket.close(); }

    private record NetworkTarget(NetworkInterface lanInterface, InetAddress broadcastAddress,
                                 NetworkInterface loopbackInterface) { }
}
