package lab1.server;

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
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.regex.Pattern;

/** Mot worker TCP cho mot client dang ket noi. */
final class ClientHandler implements Runnable, Closeable {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[\\p{L}\\p{N}_ .-]+");
    private final ChatServer server;
    private final Socket socket;
    private final Object writeLock = new Object();
    private DataInputStream input;
    private DataOutputStream output;
    private String username;
    private volatile boolean closed;

    ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(15_000);
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            if (!login()) return;
            socket.setSoTimeout(0);
            server.clientReady(username);
            while (!closed) {
                String frame = input.readUTF();
                switch (frame) {
                    case Protocol.TEXT -> readDirectText();
                    case Protocol.GROUP_TEXT -> readGroupText();
                    case Protocol.UDP_PORT -> readUdpFilePort();
                    case Protocol.FILE_OFFER -> readDirectFile();
                    case Protocol.GROUP_FILE_OFFER -> readGroupFile();
                    case Protocol.CREATE_GROUP -> readCreateGroup();
                    case Protocol.UPDATE_GROUP -> readUpdateGroup();
                    case Protocol.DELETE_GROUP -> perform(() -> server.deleteGroup(input.readUTF(), username));
                    case Protocol.LEAVE_GROUP -> perform(() -> server.leaveGroup(input.readUTF(), username));
                    case Protocol.REQUEST_GROUP_MEMBERS -> perform(() -> server.sendMembers(input.readUTF(), username));
                    case Protocol.QUIT -> { return; }
                    default -> throw new IOException("Lenh TCP khong duoc ho tro: " + frame);
                }
            }
        } catch (EOFException | SocketException ignored) {
        } catch (IOException | IllegalArgumentException exception) {
            if (!closed) sendError("Ket noi bi dong: " + exception.getMessage());
        } finally {
            close();
            server.clientDisconnected(username, this);
        }
    }

    private boolean login() throws IOException {
        String action = input.readUTF();
        if (!Protocol.LOGIN.equals(action) && !Protocol.REGISTER.equals(action)) {
            sendLoginError("Lenh dau tien phai la LOGIN hoac REGISTER.");
            return false;
        }
        String requested = input.readUTF().trim();
        String password = input.readUTF();
        if (requested.isBlank() || requested.length() > Protocol.MAX_USERNAME_LENGTH
                || !USERNAME_PATTERN.matcher(requested).matches()) {
            sendLoginError("Ten phai co 1-20 ky tu chu, so, khoang trang, '.', '_' hoac '-'.");
            return false;
        }
        if (password.isBlank() || password.length() > 64) {
            sendLoginError("Mat khau phai co tu 1 den 64 ky tu.");
            return false;
        }
        try {
            if (!server.authenticateAndRegister(action, requested, password, this)) {
                sendLoginError("Sai tai khoan/mat khau hoac tai khoan dang online.");
                return false;
            }
        } catch (IOException exception) {
            sendLoginError(exception.getMessage());
            return false;
        }
        username = requested;
        synchronized (writeLock) {
            output.writeUTF(Protocol.LOGIN_OK);
            output.writeInt(server.udpTextPort());
            output.flush();
        }
        return true;
    }

    private void readDirectText() throws IOException {
        RecipientMode mode = RecipientMode.valueOf(input.readUTF());
        List<String> targets = Protocol.readStringList(input);
        String text = input.readUTF().trim();
        if (mode != RecipientMode.DIRECT || targets.size() != 1) {
            sendError("Chat rieng TCP can dung mot nguoi nhan.");
        } else if (!validText(text)) {
            sendError("Tin nhan phai co tu 1 den " + Protocol.MAX_TEXT_LENGTH + " ky tu.");
        } else {
            perform(() -> server.sendDirectText(username, targets.get(0), text));
        }
    }

    private void readGroupText() throws IOException {
        String groupId = input.readUTF();
        String text = input.readUTF().trim();
        if (!validText(text)) sendError("Tin nhan nhom khong hop le.");
        else perform(() -> server.sendGroupText(groupId, username, text));
    }

    private boolean validText(String text) {
        return !text.isBlank() && text.length() <= Protocol.MAX_TEXT_LENGTH;
    }

    private void readCreateGroup() throws IOException {
        String name = input.readUTF();
        List<String> members = Protocol.readStringList(input);
        perform(() -> server.createGroup(name, username, members));
    }

    private void readUpdateGroup() throws IOException {
        String id = input.readUTF();
        List<String> members = Protocol.readStringList(input);
        perform(() -> server.updateGroupMembers(id, username, members));
    }

    private void readUdpFilePort() throws IOException {
        int port = input.readInt();
        if (port < 1 || port > 65_535) sendError("Cong UDP file khong hop le.");
        else server.setUdpEndpoint(username, port, socket.getInetAddress());
    }

    private void readDirectFile() throws IOException {
        String target = input.readUTF().trim();
        String fileName = input.readUTF();
        long size = input.readLong();
        if (!validFileSize(size)) sendError("Tep vuot gioi han 20 MB.");
        else perform(() -> server.offerDirectFile(username, target, fileName, size));
    }

    private void readGroupFile() throws IOException {
        String groupId = input.readUTF();
        String fileName = input.readUTF();
        long size = input.readLong();
        if (!validFileSize(size)) sendError("Tep vuot gioi han 20 MB.");
        else perform(() -> server.offerGroupFile(username, groupId, fileName, size));
    }

    private boolean validFileSize(long size) {
        return size >= 0 && size <= Protocol.MAX_FILE_SIZE;
    }

    void sendUserList(List<String> usernames) {
        sendSafely(out -> { out.writeUTF(Protocol.USER_LIST); Protocol.writeStringList(out, usernames); });
    }

    void sendSystem(String message) {
        sendSafely(out -> { out.writeUTF(Protocol.SYSTEM); out.writeUTF(message); });
    }

    void sendGroupCatalog(List<GroupInfo> catalog) {
        sendSafely(out -> {
            out.writeUTF(Protocol.GROUP_CATALOG);
            out.writeInt(catalog.size());
            for (GroupInfo group : catalog) {
                out.writeUTF(group.id());
                out.writeUTF(group.name());
                out.writeUTF(group.owner());
                out.writeBoolean(group.systemGroup());
                out.writeUTF(group.multicastAddress());
                out.writeInt(group.memberCount());
                out.writeUTF(group.membership().name());
            }
        });
    }

    void sendGroupMembers(String id, List<String> members) {
        sendSafely(out -> {
            out.writeUTF(Protocol.GROUP_MEMBERS);
            out.writeUTF(id);
            Protocol.writeStringList(out, members);
        });
    }

    void sendGroupRemoved(String id) {
        sendSafely(out -> { out.writeUTF(Protocol.GROUP_REMOVED); out.writeUTF(id); });
    }

    void sendText(String sender, RecipientMode mode, List<String> targets, long timestamp, String text) {
        sendSafely(out -> {
            out.writeUTF(Protocol.TEXT);
            out.writeUTF(sender);
            out.writeUTF(mode.name());
            Protocol.writeStringList(out, targets);
            out.writeLong(timestamp);
            out.writeUTF(text);
        });
    }

    void sendFileReady(int id, int port, String name, long size, String kind, String target) {
        sendSafely(out -> {
            out.writeUTF(Protocol.FILE_READY); out.writeInt(id); out.writeInt(port);
            out.writeUTF(name); out.writeLong(size); out.writeUTF(kind); out.writeUTF(target);
        });
    }

    void sendFileIncoming(int id, String name, long size, String sender, String kind, String target) {
        sendSafely(out -> {
            out.writeUTF(Protocol.FILE_INCOMING); out.writeInt(id); out.writeUTF(name);
            out.writeLong(size); out.writeUTF(sender); out.writeUTF(kind); out.writeUTF(target);
        });
    }

    void sendFileDone(int id, String name) {
        sendSafely(out -> { out.writeUTF(Protocol.FILE_DONE); out.writeInt(id); out.writeUTF(name); });
    }

    void sendFileFail(int id, String reason) {
        sendSafely(out -> { out.writeUTF(Protocol.FILE_FAIL); out.writeInt(id); out.writeUTF(reason); });
    }

    private void sendLoginError(String message) {
        sendSafely(out -> { out.writeUTF(Protocol.LOGIN_ERROR); out.writeUTF(message); });
    }

    private void sendError(String message) {
        sendSafely(out -> { out.writeUTF(Protocol.SYSTEM_ERROR); out.writeUTF(message); });
    }

    private void perform(IoAction action) {
        try { action.run(); } catch (IOException exception) { sendError(exception.getMessage()); }
    }

    private void sendSafely(OutputAction action) {
        if (closed || output == null) return;
        synchronized (writeLock) {
            try { action.write(output); output.flush(); }
            catch (IOException exception) { close(); }
        }
    }

    String username() { return username; }
    String remoteAddress() { return String.valueOf(socket.getRemoteSocketAddress()); }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try { socket.close(); } catch (IOException ignored) { }
    }

    @FunctionalInterface private interface OutputAction { void write(DataOutputStream output) throws IOException; }
    @FunctionalInterface private interface IoAction { void run() throws IOException; }
}
