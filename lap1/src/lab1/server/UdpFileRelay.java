package lab1.server;

import lab1.common.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Relay file UDP. Ca sender->server va server->receiver deu co ACK/retry. */
final class UdpFileRelay implements Closeable, Runnable {
    private final int udpPort;
    private final ChatServer server;
    private final Map<Integer, Transfer> transfers = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ExecutorService retryPool = Executors.newCachedThreadPool();
    private DatagramSocket socket;
    private volatile boolean running;

    UdpFileRelay(int udpPort, ChatServer server) {
        this.udpPort = udpPort;
        this.server = server;
    }

    void start() throws SocketException {
        socket = new DatagramSocket(udpPort);
        running = true;
        Thread thread = new Thread(this, "udp-file-relay");
        thread.setDaemon(true);
        thread.start();
    }

    int createTransfer(String sender, String target, String fileName, long size, boolean group,
                       List<String> recipients) {
        int id = nextId.getAndIncrement();
        int total = size == 0 ? 1 : (int) ((size + Protocol.UDP_CHUNK_SIZE - 1) / Protocol.UDP_CHUNK_SIZE);
        transfers.put(id, new Transfer(id, sender, target, fileName, size, group,
                List.copyOf(recipients), total));
        return id;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[Protocol.UDP_CHUNK_SIZE + 64];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(packet);
            } catch (IOException exception) {
                if (running) System.err.println("UDP file relay loi: " + exception.getMessage());
            }
        }
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        ByteBuffer data = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
        if (data.remaining() < 2) return;
        short magic = data.getShort();
        if (magic == Protocol.UDP_MAGIC_ACK) {
            if (data.remaining() < 8) return;
            int transferId = data.getInt();
            int sequence = data.getInt();
            acceptReceiverAck(transferId, sequence, (InetSocketAddress) packet.getSocketAddress());
            return;
        }
        if (magic != Protocol.UDP_MAGIC_CHUNK || data.remaining() < 14) return;
        int transferId = data.getInt();
        int sequence = data.getInt();
        int total = data.getInt();
        int length = data.getShort() & 0xFFFF;
        if (length > data.remaining() || length > Protocol.UDP_CHUNK_SIZE) return;
        Transfer transfer = transfers.get(transferId);
        if (transfer == null || transfer.finished.get()) return;
        byte[] chunk = new byte[length];
        data.get(chunk);
        transfer.senderAddress = (InetSocketAddress) packet.getSocketAddress();
        transfer.totalChunks = total;
        boolean first = transfer.chunks.putIfAbsent(sequence, chunk) == null;
        sendAck(transfer.senderAddress, transferId, sequence);
        if (first) {
            for (String recipient : transfer.recipients) {
                retryPool.submit(() -> deliverWithRetry(transfer, recipient, sequence, chunk));
            }
        }
        finishIfComplete(transfer);
    }

    private void acceptReceiverAck(int transferId, int sequence, InetSocketAddress source) {
        Transfer transfer = transfers.get(transferId);
        if (transfer == null) return;
        for (String recipient : transfer.recipients) {
            InetSocketAddress expected = server.udpAddressOf(recipient);
            if (source.equals(expected)) {
                transfer.acks.computeIfAbsent(recipient, ignored -> ConcurrentHashMap.newKeySet())
                        .add(sequence);
                finishIfComplete(transfer);
                return;
            }
        }
    }

    private void deliverWithRetry(Transfer transfer, String recipient, int sequence, byte[] chunk) {
        byte[] payload = encodeChunk(transfer.id, sequence, transfer.totalChunks, chunk);
        for (int attempt = 0; attempt < Protocol.UDP_MAX_RETRIES && running; attempt++) {
            if (transfer.finished.get() || transfer.hasAck(recipient, sequence)) return;
            InetSocketAddress address = server.udpAddressOf(recipient);
            if (address == null) {
                fail(transfer);
                return;
            }
            try {
                synchronized (this) {
                    socket.send(new DatagramPacket(payload, payload.length, address));
                }
                long deadline = System.currentTimeMillis() + Protocol.UDP_ACK_TIMEOUT_MS;
                while (System.currentTimeMillis() < deadline && !transfer.hasAck(recipient, sequence)) {
                    Thread.sleep(10);
                }
                if (transfer.hasAck(recipient, sequence)) {
                    finishIfComplete(transfer);
                    return;
                }
            } catch (IOException exception) {
                if (!running) return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!transfer.hasAck(recipient, sequence)) fail(transfer);
    }

    private void finishIfComplete(Transfer transfer) {
        if (transfer.allChunksStored() && transfer.allRecipientsAcked()
                && transfer.finished.compareAndSet(false, true)) {
            transfers.remove(transfer.id, transfer);
            server.onUdpTransferFinished(transfer, true);
        }
    }

    private void fail(Transfer transfer) {
        if (transfer.finished.compareAndSet(false, true)) {
            transfers.remove(transfer.id, transfer);
            server.onUdpTransferFinished(transfer, false);
        }
    }

    private synchronized void sendAck(InetSocketAddress address, int transferId, int sequence)
            throws IOException {
        byte[] ack = encodeAck(transferId, sequence);
        socket.send(new DatagramPacket(ack, ack.length, address));
    }

    static byte[] encodeChunk(int id, int sequence, int total, byte[] chunk) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 4 + 4 + 4 + 2 + chunk.length);
        buffer.putShort(Protocol.UDP_MAGIC_CHUNK).putInt(id).putInt(sequence).putInt(total)
                .putShort((short) chunk.length).put(chunk);
        return buffer.array();
    }

    static byte[] encodeAck(int id, int sequence) {
        return ByteBuffer.allocate(10).putShort(Protocol.UDP_MAGIC_ACK)
                .putInt(id).putInt(sequence).array();
    }

    @Override
    public void close() {
        running = false;
        if (socket != null) socket.close();
        retryPool.shutdownNow();
        transfers.clear();
    }

    static final class Transfer {
        final int id;
        final String sender;
        final String target;
        final String fileName;
        final long size;
        final boolean group;
        final List<String> recipients;
        final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();
        final Map<String, Set<Integer>> acks = new ConcurrentHashMap<>();
        final AtomicBoolean finished = new AtomicBoolean();
        volatile int totalChunks;
        volatile InetSocketAddress senderAddress;

        Transfer(int id, String sender, String target, String fileName, long size, boolean group,
                 List<String> recipients, int totalChunks) {
            this.id = id;
            this.sender = sender;
            this.target = target;
            this.fileName = fileName;
            this.size = size;
            this.group = group;
            this.recipients = recipients;
            this.totalChunks = totalChunks;
        }

        boolean hasAck(String recipient, int sequence) {
            return acks.getOrDefault(recipient, Set.of()).contains(sequence);
        }

        boolean allChunksStored() {
            return totalChunks > 0 && chunks.size() >= totalChunks;
        }

        boolean allRecipientsAcked() {
            return recipients.stream().allMatch(name ->
                    acks.getOrDefault(name, Set.of()).size() >= totalChunks);
        }
    }
}
