package lab1.client;

import lab1.common.GroupInfo;
import lab1.common.Protocol;
import lab1.common.RecipientMode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** API mang cua client: TCP dieu khien, UDP text va UDP file. */
public final class ClientConnection implements Closeable {
    private final String username;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final InetAddress serverAddress;
    private final ClientUdpFiles udpFiles;
    private final ClientUdpText udpText;
    private final Queue<PendingSend> pendingSends = new ConcurrentLinkedQueue<>();
    private volatile Listener listener;

    private ClientConnection(String username, Socket socket, DataInputStream input,
                             DataOutputStream output, ClientUdpFiles udpFiles,
                             ClientUdpText udpText) throws IOException {
        this.username = username;
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.udpFiles = udpFiles;
        this.udpText = udpText;
        serverAddress = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress();
        send(out -> { out.writeUTF(Protocol.UDP_PORT); out.writeInt(udpFiles.localPort()); });
        udpFiles.setListener(incoming -> {
            Listener current = listener;
            if (current == null) return;
            if ("GROUP".equals(incoming.kind)) {
                current.onGroupFile(incoming.target, incoming.sender, System.currentTimeMillis(),
                        incoming.fileName, incoming.destination.toAbsolutePath());
            } else {
                current.onFile(incoming.sender, RecipientMode.DIRECT, List.of(username),
                        System.currentTimeMillis(), incoming.fileName, incoming.destination.toAbsolutePath());
            }
        });
    }

    public static ClientConnection connect(String host, int port, String username, String password,
                                           boolean register) throws IOException {
        Socket socket = new Socket();
        ClientUdpFiles files = new ClientUdpFiles();
        ClientUdpText text = null;
        try {
            socket.connect(new InetSocketAddress(host, port), 5_000);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(10_000);
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            output.writeUTF(register ? Protocol.REGISTER : Protocol.LOGIN);
            output.writeUTF(username);
            output.writeUTF(password);
            output.flush();
            String response = input.readUTF();
            if (Protocol.LOGIN_ERROR.equals(response)) throw new IOException(input.readUTF());
            if (!Protocol.LOGIN_OK.equals(response)) throw new IOException("Phan hoi dang nhap khong hop le.");
            int udpTextPort = input.readInt();
            text = new ClientUdpText(udpTextPort);
            socket.setSoTimeout(0);
            return new ClientConnection(username, socket, input, output, files, text);
        } catch (IOException exception) {
            files.close();
            if (text != null) text.close();
            socket.close();
            throw exception;
        }
    }

    /** Ho tro test/demo: dang ky truoc, neu da co thi dang nhap. */
    public static ClientConnection connect(String host, int port, String username) throws IOException {
        String password = "lab1-" + username;
        try { return connect(host, port, username, password, true); }
        catch (IOException ignored) { return connect(host, port, username, password, false); }
    }

    public void startListening(Listener value) {
        if (listener != null) throw new IllegalStateException("Listener da duoc khoi dong.");
        listener = value;
        udpText.setListener(message -> value.onGroupText(message.groupId(), message.sender(),
                message.timestamp(), message.text()));
        Thread thread = new Thread(this::readLoop, "chat-reader-" + username);
        thread.setDaemon(true);
        thread.start();
    }

    public void sendText(RecipientMode mode, List<String> recipients, String text) throws IOException {
        validateText(text);
        send(out -> {
            out.writeUTF(Protocol.TEXT);
            out.writeUTF(mode.name());
            Protocol.writeStringList(out, recipients);
            out.writeUTF(text);
        });
    }

    public void sendGroupText(String groupId, String text) throws IOException {
        validateText(text);
        sendStrings(Protocol.GROUP_TEXT, groupId, text);
    }

    public void createGroup(String name, List<String> members) throws IOException {
        send(out -> {
            out.writeUTF(Protocol.CREATE_GROUP);
            out.writeUTF(name);
            Protocol.writeStringList(out, members);
        });
    }

    public void updateGroupMembers(String groupId, List<String> members) throws IOException {
        send(out -> {
            out.writeUTF(Protocol.UPDATE_GROUP);
            out.writeUTF(groupId);
            Protocol.writeStringList(out, members);
        });
    }

    public void deleteGroup(String groupId) throws IOException { sendStrings(Protocol.DELETE_GROUP, groupId); }
    public void leaveGroup(String groupId) throws IOException { sendStrings(Protocol.LEAVE_GROUP, groupId); }
    public void requestGroupMembers(String groupId) throws IOException {
        sendStrings(Protocol.REQUEST_GROUP_MEMBERS, groupId);
    }

    public void sendFile(RecipientMode mode, List<String> recipients, Path file) throws IOException {
        if (mode != RecipientMode.DIRECT || recipients.size() != 1) {
            throw new IOException("Gui file rieng can dung mot nguoi nhan.");
        }
        sendFileOffer(file, out -> {
            out.writeUTF(Protocol.FILE_OFFER);
            out.writeUTF(recipients.get(0));
        });
    }

    public void sendGroupFile(String groupId, Path file) throws IOException {
        sendFileOffer(file, out -> { out.writeUTF(Protocol.GROUP_FILE_OFFER); out.writeUTF(groupId); });
    }

    private void sendFileOffer(Path file, OutputAction prefix) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > Protocol.MAX_FILE_SIZE) {
            throw new IOException("Tep khong hop le hoac vuot gioi han 20 MB.");
        }
        PendingSend pending = new PendingSend(file);
        pendingSends.add(pending);
        send(out -> {
            prefix.write(out);
            out.writeUTF(file.getFileName().toString());
            out.writeLong(pending.size);
        });
        waitPending(pending);
    }

    private void validateText(String text) throws IOException {
        if (text.isBlank() || text.length() > Protocol.MAX_TEXT_LENGTH) {
            throw new IOException("Tin nhan phai co tu 1 den " + Protocol.MAX_TEXT_LENGTH + " ky tu.");
        }
    }

    private void sendStrings(String frame, String... values) throws IOException {
        send(out -> {
            out.writeUTF(frame);
            for (String value : values) out.writeUTF(value);
        });
    }

    private void waitPending(PendingSend pending) throws IOException {
        try {
            String error = pending.future.get(120, TimeUnit.SECONDS);
            if (error != null) throw new IOException(error);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Gui file UDP that bai: " + exception.getMessage(), exception);
        } finally {
            pendingSends.remove(pending);
        }
    }

    private void readLoop() {
        String reason = "Da ngat ket noi voi server.";
        try {
            while (!closed.get()) {
                switch (input.readUTF()) {
                    case Protocol.USER_LIST -> listener.onUserList(Protocol.readStringList(input));
                    case Protocol.SYSTEM -> listener.onSystemMessage(input.readUTF());
                    case Protocol.SYSTEM_ERROR -> listener.onError(input.readUTF());
                    case Protocol.TEXT -> readDirectText();
                    case Protocol.GROUP_CATALOG -> readGroupCatalog();
                    case Protocol.GROUP_MEMBERS -> listener.onGroupMembers(
                            input.readUTF(), Protocol.readStringList(input));
                    case Protocol.GROUP_REMOVED -> listener.onGroupRemoved(input.readUTF());
                    case Protocol.FILE_READY -> readFileReady();
                    case Protocol.FILE_INCOMING -> readFileIncoming();
                    case Protocol.FILE_DONE -> readFileDone();
                    case Protocol.FILE_FAIL -> readFileFail();
                    default -> throw new IOException("Server gui frame khong hop le.");
                }
            }
        } catch (EOFException | SocketException ignored) {
        } catch (IOException | IllegalArgumentException exception) {
            reason = "Mat ket noi: " + exception.getMessage();
        } finally {
            boolean first = closed.compareAndSet(false, true);
            closeResources();
            if (first && listener != null) listener.onDisconnected(reason);
        }
    }

    private void readDirectText() throws IOException {
        String sender = input.readUTF();
        RecipientMode mode = RecipientMode.valueOf(input.readUTF());
        List<String> targets = Protocol.readStringList(input);
        listener.onText(sender, mode, targets, input.readLong(), input.readUTF());
    }

    private void readGroupCatalog() throws IOException {
        int count = input.readInt();
        if (count < 0 || count > 1_000) throw new IOException("Danh sach nhom khong hop le.");
        List<GroupInfo> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new GroupInfo(input.readUTF(), input.readUTF(), input.readUTF(),
                    input.readBoolean(), input.readUTF(), input.readInt(),
                    GroupInfo.Membership.valueOf(input.readUTF())));
        }
        udpText.syncGroups(result);
        listener.onGroupCatalog(result);
    }

    private void readFileReady() throws IOException {
        int id = input.readInt();
        int port = input.readInt();
        String name = input.readUTF();
        long size = input.readLong();
        input.readUTF();
        input.readUTF();
        PendingSend pending = pendingSends.stream()
                .filter(item -> item.file.getFileName().toString().equals(name)
                        && item.size == size && !item.started).findFirst().orElse(null);
        if (pending == null) return;
        pending.started = true;
        pending.transferId = id;
        CompletableFuture.runAsync(() -> {
            try { udpFiles.sendFile(serverAddress, port, id, pending.file); }
            catch (IOException exception) { pending.future.complete(exception.getMessage()); }
        });
    }

    private void readFileIncoming() throws IOException {
        int id = input.readInt();
        String name = input.readUTF();
        long size = input.readLong();
        String sender = input.readUTF();
        String kind = input.readUTF();
        String target = input.readUTF();
        udpFiles.prepareIncoming(id, name, size, sender, kind, target,
                Path.of("received", safeName(username)));
    }

    private void readFileDone() throws IOException {
        int id = input.readInt();
        input.readUTF();
        pendingSends.stream().filter(item -> item.transferId == id)
                .forEach(item -> item.future.complete(null));
    }

    private void readFileFail() throws IOException {
        int id = input.readInt();
        String reason = input.readUTF();
        pendingSends.stream().filter(item -> item.transferId == id)
                .forEach(item -> item.future.complete(reason));
        listener.onError(reason);
    }

    private String safeName(String value) { return value.replaceAll("[^\\p{L}\\p{N}_.-]", "_"); }

    private void send(OutputAction action) throws IOException {
        if (closed.get()) throw new IOException("Ket noi da dong.");
        synchronized (writeLock) { action.write(output); output.flush(); }
    }

    public String username() { return username; }
    public boolean isClosed() { return closed.get(); }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (writeLock) {
            try { output.writeUTF(Protocol.QUIT); output.flush(); } catch (IOException ignored) { }
        }
        closeResources();
    }

    private void closeResources() {
        udpText.close();
        udpFiles.close();
        try { socket.close(); } catch (IOException ignored) { }
    }

    @FunctionalInterface private interface OutputAction { void write(DataOutputStream output) throws IOException; }

    private static final class PendingSend {
        final Path file;
        final long size;
        final CompletableFuture<String> future = new CompletableFuture<>();
        volatile boolean started;
        volatile int transferId = -1;
        PendingSend(Path file) throws IOException { this.file = file; this.size = Files.size(file); }
    }

    public interface Listener {
        void onUserList(List<String> usernames);
        void onSystemMessage(String message);
        void onError(String message);
        void onText(String sender, RecipientMode mode, List<String> targets, long timestamp, String text);
        void onFile(String sender, RecipientMode mode, List<String> targets,
                    long timestamp, String originalName, Path savedPath);
        void onDisconnected(String reason);
        default void onGroupCatalog(List<GroupInfo> groups) { }
        default void onGroupMembers(String groupId, List<String> members) { }
        default void onGroupRemoved(String groupId) { }
        default void onGroupText(String groupId, String sender, long timestamp, String text) { }
        default void onGroupFile(String groupId, String sender, long timestamp,
                                 String originalName, Path savedPath) { }
    }
}
