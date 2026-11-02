package lab1.server;

import lab1.common.Protocol;
import lab1.common.RecipientMode;
import lab1.db.MySqlDB;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Dieu phoi TCP, UDP text, UDP file va tai khoan cua Lab 1. */
public final class ChatServer implements Closeable {
    private final int tcpPort;
    private final int udpTextPort;
    private final int udpFilePort;
    private final ClientRegistry clients = new ClientRegistry();
    private final GroupRegistry groups = new GroupRegistry();
    private final MySqlDB database = new MySqlDB();
    private final Map<String, InetSocketAddress> udpFileEndpoints = new ConcurrentHashMap<>();
    private final ExecutorService clientPool = Executors.newFixedThreadPool(Protocol.MAX_CLIENTS);
    private volatile boolean running;
    private ServerSocket serverSocket;
    private UdpFileRelay fileRelay;
    private UdpTextBroadcaster textBroadcaster;
    private volatile ServerListener listener = new NoOpListener();

    public ChatServer(int tcpPort, int udpFilePort) {
        this(tcpPort, Protocol.DEFAULT_UDP_TEXT_PORT, udpFilePort);
    }

    public ChatServer(int tcpPort, int udpTextPort, int udpFilePort) {
        this.tcpPort = tcpPort;
        this.udpTextPort = udpTextPort;
        this.udpFilePort = udpFilePort;
    }

    public void setListener(ServerListener value) {
        listener = value == null ? new NoOpListener() : value;
    }

    public int tcpPort() { return tcpPort; }
    public int udpTextPort() { return udpTextPort; }
    public int udpPort() { return udpFilePort; }
    public int udpFilePort() { return udpFilePort; }
    public boolean isRunning() { return running; }
    public MySqlDB database() { return database; }

    public void start() throws IOException {
        if (running) throw new IOException("Server dang chay.");
        try {
            database.ensureReady();
        } catch (SQLException exception) {
            throw new IOException("Khong ket noi duoc MySQL: " + exception.getMessage(), exception);
        }
        serverSocket = new ServerSocket(tcpPort);
        fileRelay = new UdpFileRelay(udpFilePort, this);
        fileRelay.start();
        textBroadcaster = new UdpTextBroadcaster(udpTextPort);
        running = true;

        String bindAddress = primaryLanAddress();
        listener.onStarted(bindAddress, tcpPort, udpTextPort, udpFilePort);
        listener.onLog("TCP " + tcpPort + " | UDP text " + udpTextPort + " | UDP file " + udpFilePort);
        listener.onLog("UDP text dung card " + textBroadcaster.networkName()
                + " | broadcast " + textBroadcaster.broadcastAddress());
        listener.onLog("MySQL chi luu tai khoan; nhom duoc luu trong RAM");
        try {
            while (running) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                listener.onLog("Ket noi TCP moi: " + socket.getRemoteSocketAddress());
                clientPool.submit(new ClientHandler(this, socket));
            }
        } catch (SocketException exception) {
            if (running) throw exception;
        } finally {
            close();
        }
    }

    public synchronized void startAsync() throws IOException {
        if (running) throw new IOException("Server dang chay.");
        Thread thread = new Thread(() -> {
            try {
                start();
            } catch (IOException exception) {
                listener.onLog("Loi server: " + exception.getMessage());
                close();
            }
        }, "chat-server-accept");
        thread.setDaemon(true);
        thread.start();
    }

    boolean authenticateAndRegister(String action, String username, String password, ClientHandler handler)
            throws IOException {
        try {
            if (Protocol.REGISTER.equals(action)) database.register(username, password);
            else if (!database.login(username, password)) return false;
        } catch (SQLException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
        boolean registered = clients.register(username, handler);
        if (registered) listener.onLog("Dang nhap: " + username + " tu " + handler.remoteAddress());
        return registered;
    }

    void setUdpEndpoint(String username, int port, InetAddress address) {
        udpFileEndpoints.put(username, new InetSocketAddress(address, port));
    }

    InetSocketAddress udpAddressOf(String username) {
        return udpFileEndpoints.get(username);
    }

    void clientReady(String username) {
        broadcastSystem(username + " da tham gia phong chat.");
        publishCatalogs();
    }

    void clientDisconnected(String username, ClientHandler handler) {
        if (username == null) return;
        boolean existed = clients.find(username) == handler;
        clients.remove(username, handler);
        udpFileEndpoints.remove(username);
        if (existed) {
            listener.onLog("Ngat ket noi: " + username);
            broadcastSystem(username + " da roi phong chat.");
            publishCatalogs();
        }
    }

    void sendDirectText(String sender, String target, String text) throws IOException {
        List<ClientHandler> recipients = directRecipients(sender, target);
        long time = System.currentTimeMillis();
        recipients.forEach(handler -> handler.sendText(sender, RecipientMode.DIRECT,
                List.of(target), time, text));
    }

    void sendGroupText(String id, String sender, String text) throws IOException {
        GroupRegistry.Group group = groups.requireMemberGroup(id, sender);
        if (group == null) {
            textBroadcaster.broadcast(id, sender, text);
            listener.onLog("UDP BROADCAST text: " + sender + " -> Tat ca");
        } else {
            textBroadcaster.multicast(group.multicastAddress, id, sender, text);
            listener.onLog("UDP MULTICAST text: " + sender + " -> " + group.name
                    + " (" + group.multicastAddress + ")");
        }
    }

    void createGroup(String name, String owner, List<String> members) throws IOException {
        GroupRegistry.Group group = groups.create(name, owner, members, clients.usernames());
        publishGroupCatalog();
        listener.onLog("Tao nhom " + group.name + " tai " + group.multicastAddress);
    }

    void updateGroupMembers(String id, String owner, List<String> members) throws IOException {
        groups.updateMembers(id, owner, members, clients.usernames());
        publishGroupCatalog();
        listener.onLog(owner + " cap nhat thanh vien nhom " + id);
    }

    void deleteGroup(String id, String actor) throws IOException {
        groups.delete(id, actor);
        clients.handlers().forEach(handler -> handler.sendGroupRemoved(id));
        publishGroupCatalog();
    }

    void leaveGroup(String id, String username) throws IOException {
        groups.leave(id, username);
        ClientHandler handler = clients.find(username);
        if (handler != null) handler.sendGroupRemoved(id);
        publishGroupCatalog();
    }

    void sendMembers(String id, String actor) throws IOException {
        ClientHandler handler = clients.find(actor);
        if (handler != null) handler.sendGroupMembers(id, groups.membersForOwner(id, actor));
    }

    void offerDirectFile(String sender, String target, String fileName, long size) throws IOException {
        ClientHandler receiver = clients.find(target);
        if (receiver == null) throw new IOException("Nguoi nhan dang offline.");
        requireFileEndpoints(List.of(sender, target));
        int transferId = fileRelay.createTransfer(sender, target, fileName, size,
                false, List.of(target));
        receiver.sendFileIncoming(transferId, fileName, size, sender, "DIRECT", target);
        clients.find(sender).sendFileReady(transferId, udpFilePort, fileName, size, "DIRECT", target);
    }

    void offerGroupFile(String sender, String groupId, String fileName, long size) throws IOException {
        groups.requireMemberGroup(groupId, sender);
        List<String> recipients = new ArrayList<>(groups.memberNames(groupId, clients.usernames()));
        recipients.remove(sender);
        recipients.removeIf(name -> clients.find(name) == null || udpAddressOf(name) == null);
        requireFileEndpoints(List.of(sender));
        int transferId = fileRelay.createTransfer(sender, groupId, fileName, size,
                true, recipients);
        for (String name : recipients) {
            clients.find(name).sendFileIncoming(transferId, fileName, size, sender, "GROUP", groupId);
        }
        clients.find(sender).sendFileReady(transferId, udpFilePort, fileName, size, "GROUP", groupId);
    }

    void onUdpTransferFinished(UdpFileRelay.Transfer transfer, boolean success) {
        ClientHandler sender = clients.find(transfer.sender);
        if (sender != null) notifyFileResult(sender, transfer, success);
        for (String recipient : transfer.recipients) {
            ClientHandler handler = clients.find(recipient);
            if (handler != null) notifyFileResult(handler, transfer, success);
        }
        listener.onLog("UDP file " + transfer.id + (success ? " hoan tat" : " that bai"));
    }

    private void notifyFileResult(ClientHandler handler, UdpFileRelay.Transfer transfer, boolean success) {
        if (success) handler.sendFileDone(transfer.id, transfer.fileName);
        else handler.sendFileFail(transfer.id, "Truyen file UDP that bai.");
    }

    private List<ClientHandler> directRecipients(String sender, String target) throws IOException {
        Set<String> names = new LinkedHashSet<>(List.of(sender, target));
        if (sender.equals(target)) throw new IOException("Hay chon mot nguoi nhan khac.");
        List<ClientHandler> result = names.stream().map(clients::find)
                .filter(java.util.Objects::nonNull).toList();
        if (result.size() != 2) throw new IOException("Nguoi nhan dang offline.");
        return result;
    }

    private void requireFileEndpoints(List<String> names) throws IOException {
        if (names.stream().anyMatch(name -> udpAddressOf(name) == null)) {
            throw new IOException("Chua san sang cong UDP file.");
        }
    }

    private void publishCatalogs() {
        broadcastUserList();
        publishGroupCatalog();
        listener.onClientsChanged(clients.snapshots());
    }

    private void broadcastUserList() {
        List<String> names = clients.usernames();
        clients.handlers().forEach(handler -> handler.sendUserList(names));
    }

    private void publishGroupCatalog() {
        int online = clients.usernames().size();
        clients.handlers().forEach(handler ->
                handler.sendGroupCatalog(groups.catalogFor(handler.username(), online)));
    }

    private void broadcastSystem(String message) {
        clients.handlers().forEach(handler -> handler.sendSystem(message));
    }

    public static String primaryLanAddress() {
        List<String> addresses = localAddresses();
        return addresses.isEmpty() ? "127.0.0.1" : addresses.get(0);
    }

    public static List<String> localAddresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface network : Collections.list(networks)) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        result.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        if (result.isEmpty()) result.add("127.0.0.1");
        return result;
    }

    @Override
    public synchronized void close() {
        boolean wasRunning = running || serverSocket != null;
        running = false;
        if (textBroadcaster != null) textBroadcaster.close();
        textBroadcaster = null;
        if (fileRelay != null) fileRelay.close();
        fileRelay = null;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) { }
            serverSocket = null;
        }
        clients.handlers().forEach(ClientHandler::close);
        clientPool.shutdownNow();
        try { clientPool.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        udpFileEndpoints.clear();
        if (wasRunning) {
            listener.onClientsChanged(List.of());
            listener.onStopped();
            listener.onLog("Server da dung.");
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--console".equalsIgnoreCase(args[0])) {
            int tcp = parsePort(args, 1, Protocol.DEFAULT_TCP_PORT);
            int text = parsePort(args, 2, Protocol.DEFAULT_UDP_TEXT_PORT);
            int file = parsePort(args, 3, Protocol.DEFAULT_UDP_FILE_PORT);
            ChatServer server = new ChatServer(tcp, text, file);
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            try { server.start(); }
            catch (IOException exception) { System.err.println(exception.getMessage()); }
            return;
        }
        ServerFxApp.main(args);
    }

    private static int parsePort(String[] args, int index, int fallback) {
        if (args.length <= index) return fallback;
        try {
            int value = Integer.parseInt(args[index]);
            return value >= 1 && value <= 65_535 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class NoOpListener implements ServerListener {
        public void onStarted(String address, int tcp, int text, int file) { }
        public void onStopped() { }
        public void onClientsChanged(List<ConnectedClient> value) { }
        public void onLog(String message) { System.out.println(message); }
    }
}
