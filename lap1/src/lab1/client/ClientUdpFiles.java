package lab1.client;

import lab1.common.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class ClientUdpFiles implements Closeable, Runnable {
    private final DatagramSocket socket = new DatagramSocket(0);
    private final Map<Integer, CountDownLatch> ackWaiters = new ConcurrentHashMap<>();
    private final Map<Integer, Incoming> incomings = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile Listener listener;

    ClientUdpFiles() throws IOException {
        Thread thread = new Thread(this, "client-udp-files");
        thread.setDaemon(true);
        thread.start();
    }

    int localPort() {
        return socket.getLocalPort();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void prepareIncoming(int transferId, String fileName, long size, String sender,
                         String kind, String target, Path saveDirectory) throws IOException {
        Files.createDirectories(saveDirectory);
        Path destination = uniqueDestination(saveDirectory, fileName);
        int total = size == 0 ? 1 : (int) ((size + Protocol.UDP_CHUNK_SIZE - 1) / Protocol.UDP_CHUNK_SIZE);
        incomings.put(transferId, new Incoming(transferId, fileName, size, sender, kind, target, destination, total));
    }

    void sendFile(InetAddress serverHost, int serverUdpPort, int transferId, Path file) throws IOException {
        byte[] all = Files.readAllBytes(file);
        int total = all.length == 0 ? 1 : (int) ((all.length + Protocol.UDP_CHUNK_SIZE - 1L) / Protocol.UDP_CHUNK_SIZE);
        InetSocketAddress server = new InetSocketAddress(serverHost, serverUdpPort);
        for (int seq = 0; seq < total; seq++) {
            int offset = seq * Protocol.UDP_CHUNK_SIZE;
            int length = all.length == 0 ? 0 : Math.min(Protocol.UDP_CHUNK_SIZE, all.length - offset);
            byte[] chunk = new byte[length];
            if (length > 0) {
                System.arraycopy(all, offset, chunk, 0, length);
            }
            byte[] packet = encodeChunk(transferId, seq, total, chunk);
            boolean acked = false;
            for (int attempt = 0; attempt < Protocol.UDP_MAX_RETRIES && !acked; attempt++) {
                CountDownLatch latch = new CountDownLatch(1);
                ackWaiters.put(ackKey(transferId, seq), latch);
                socket.send(new DatagramPacket(packet, packet.length, server));
                try {
                    acked = latch.await(Protocol.UDP_ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Bị gián đoạn khi gửi file UDP");
                } finally {
                    ackWaiters.remove(ackKey(transferId, seq));
                }
            }
            if (!acked) {
                throw new IOException("Timeout UDP khi gửi chunk " + seq + "/" + total);
            }
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[Protocol.UDP_CHUNK_SIZE + 64];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handle(packet);
            } catch (IOException ignored) {
                if (!running.get()) {
                    return;
                }
            }
        }
    }

    private void handle(DatagramPacket packet) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
        if (buffer.remaining() < 2) {
            return;
        }
        short magic = buffer.getShort();
        if (magic == Protocol.UDP_MAGIC_ACK) {
            if (buffer.remaining() < 8) {
                return;
            }
            int transferId = buffer.getInt();
            int seq = buffer.getInt();
            CountDownLatch latch = ackWaiters.get(ackKey(transferId, seq));
            if (latch != null) {
                latch.countDown();
            }
            return;
        }
        if (magic != Protocol.UDP_MAGIC_CHUNK || buffer.remaining() < 14) {
            return;
        }
        int transferId = buffer.getInt();
        int seq = buffer.getInt();
        int total = buffer.getInt();
        int length = buffer.getShort() & 0xFFFF;
        if (length > buffer.remaining()) {
            return;
        }
        byte[] data = new byte[length];
        buffer.get(data);
        Incoming incoming = incomings.get(transferId);
        if (incoming == null) {
            return;
        }
        incoming.total = total;
        incoming.chunks.putIfAbsent(seq, data);
        byte[] ack = encodeAck(transferId, seq);
        socket.send(new DatagramPacket(ack, ack.length, packet.getSocketAddress()));
        if (incoming.chunks.size() >= incoming.total) {
            writeIncoming(incoming);
            incomings.remove(transferId);
            if (listener != null) {
                listener.onFileAssembled(incoming);
            }
        }
    }

    private void writeIncoming(Incoming incoming) throws IOException {
        if (incoming.size == 0) {
            Files.write(incoming.destination, new byte[0]);
            return;
        }
        byte[] all = new byte[(int) incoming.size];
        int offset = 0;
        for (int seq = 0; seq < incoming.total; seq++) {
            byte[] chunk = incoming.chunks.get(seq);
            if (chunk == null) {
                throw new IOException("Thiếu chunk " + seq);
            }
            System.arraycopy(chunk, 0, all, offset, chunk.length);
            offset += chunk.length;
        }
        Files.write(incoming.destination, Arrays.copyOf(all, offset));
    }

    private static int ackKey(int transferId, int seq) {
        return transferId * 1_000_003 + seq;
    }

    private static byte[] encodeChunk(int transferId, int seq, int total, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 4 + 4 + 4 + 2 + data.length);
        buffer.putShort(Protocol.UDP_MAGIC_CHUNK);
        buffer.putInt(transferId);
        buffer.putInt(seq);
        buffer.putInt(total);
        buffer.putShort((short) data.length);
        buffer.put(data);
        return buffer.array();
    }

    private static byte[] encodeAck(int transferId, int seq) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 4 + 4);
        buffer.putShort(Protocol.UDP_MAGIC_ACK);
        buffer.putInt(transferId);
        buffer.putInt(seq);
        return buffer.array();
    }

    private static Path uniqueDestination(Path directory, String fileName) {
        Path destination = directory.resolve(Path.of(fileName).getFileName().toString());
        if (!Files.exists(destination)) {
            return destination;
        }
        String fileNameOnly = destination.getFileName().toString();
        int dotIndex = fileNameOnly.lastIndexOf('.');
        String base = dotIndex > 0 ? fileNameOnly.substring(0, dotIndex) : fileNameOnly;
        String extension = dotIndex > 0 ? fileNameOnly.substring(dotIndex) : "";
        int counter = 2;
        while (Files.exists(destination)) {
            destination = directory.resolve(base + "_" + counter + extension);
            counter++;
        }
        return destination;
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
    }

    interface Listener {
        void onFileAssembled(Incoming incoming);
    }

    static final class Incoming {
        final int transferId;
        final String fileName;
        final long size;
        final String sender;
        final String kind;
        final String target;
        final Path destination;
        final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();
        volatile int total;

        Incoming(int transferId, String fileName, long size, String sender, String kind, String target,
                 Path destination, int total) {
            this.transferId = transferId;
            this.fileName = fileName;
            this.size = size;
            this.sender = sender;
            this.kind = kind;
            this.target = target;
            this.destination = destination;
            this.total = total;
        }
    }
}
