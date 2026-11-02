package lab1.test;

import lab1.client.ClientConnection;
import lab1.common.GroupInfo;
import lab1.common.Protocol;
import lab1.common.RecipientMode;
import lab1.server.ChatServer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ChatIntegrationTest {
    private ChatIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        int tcpPort = freeTcpPort();
        int udpPort = freeTcpPort();
        ChatServer server = new ChatServer(tcpPort, udpPort);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException exception) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Test server dừng: " + exception.getMessage());
                }
            }
        }, "integration-test-server");
        serverThread.start();
        Thread.sleep(500);

        List<TestClient> clients = new ArrayList<>();
        String persistedUser;
        try {
            String suffix = Long.toString(System.nanoTime() % 100_000);
            String[] names = {"An" + suffix, "Binh" + suffix, "Chi" + suffix,
                    "Dung" + suffix, "Ha" + suffix, "Lan" + suffix};
            persistedUser = names[0];

            for (String name : names) {
                TestClient client = connectWithRetry(tcpPort, name);
                clients.add(client);
            }
            System.out.println("[PASS] 6 client đăng ký/đăng nhập MySQL và kết nối đồng thời");

            boolean duplicateRejected = false;
            ClientConnection duplicateConnection = null;
            try {
                duplicateConnection = ClientConnection.connect("localhost", tcpPort, names[0], "lab1-" + names[0], false);
            } catch (IOException expected) {
                duplicateRejected = true;
            } finally {
                if (duplicateConnection != null) {
                    duplicateConnection.close();
                }
            }
            require(duplicateRejected, "Server phải từ chối tên đang online");
            System.out.println("[PASS] Từ chối tên đang online");

            TestClient an = clients.get(0);
            TestClient binh = clients.get(1);
            TestClient chi = clients.get(2);
            GroupInfo allGroup = awaitGroup(an, "Tất cả");
            clearQueues(clients);

            an.connection.sendText(RecipientMode.DIRECT, List.of(binh.name), "Tin nhắn riêng");
            requireText(an, "Tin nhắn riêng");
            requireText(binh, "Tin nhắn riêng");
            requireNoText(clients.subList(2, clients.size()));
            System.out.println("[PASS] DIRECT chỉ tới người gửi và một người nhận");

            clearQueues(clients);
            an.connection.sendText(RecipientMode.DIRECT, List.of("NguoiDaOffline"), "Không được chuyển");
            requireError(an, "offline");
            requireNoText(clients);
            System.out.println("[PASS] Báo lỗi khi người nhận không còn online");

            clearQueues(clients);
            an.connection.createGroup("Nhóm Java " + suffix, List.of(binh.name));
            GroupInfo created = awaitGroup(binh, "Nhóm Java " + suffix);
            require(created.membership() == GroupInfo.Membership.MEMBER,
                    "Nguoi duoc chon phai la thanh vien");
            System.out.println("[PASS] Truong nhom chon thanh vien khi tao nhom");

            clearQueues(clients);
            an.connection.sendGroupText(created.id(), "Tin multicast");
            requireGroupText(an, "Tin multicast");
            requireGroupText(binh, "Tin multicast");
            requireNoGroupText(clients.subList(2, clients.size()));
            System.out.println("[PASS] UDP MULTICAST chỉ tới thành viên được chọn");

            an.connection.updateGroupMembers(created.id(), List.of(chi.name));
            awaitMembership(chi, created.id(), GroupInfo.Membership.MEMBER);
            an.connection.requestGroupMembers(created.id());
            GroupMembersEvent updatedMembers = an.groupMembers.poll(2, TimeUnit.SECONDS);
            require(updatedMembers != null && updatedMembers.members.contains(an.name)
                            && updatedMembers.members.contains(chi.name)
                            && !updatedMembers.members.contains(binh.name),
                    "Danh sach thanh vien sau khi sua khong dung");
            clearQueues(clients);
            an.connection.sendGroupText(created.id(), "Tin sau khi sửa thành viên");
            requireGroupText(an, "Tin sau khi sửa thành viên");
            requireGroupText(chi, "Tin sau khi sửa thành viên");
            requireNoGroupText(List.of(binh, clients.get(3), clients.get(4), clients.get(5)));
            System.out.println("[PASS] Join/leave multicast thay đổi đúng khi sửa thành viên");

            clearQueues(clients);
            byte[] audioData = "audio-demo-data".getBytes(StandardCharsets.UTF_8);
            Path audioFile = Files.createTempFile("lap1-audio-", ".mp3");
            Files.write(audioFile, audioData);
            try {
                an.connection.sendGroupFile(created.id(), audioFile);
                FileEvent receivedAudio = requireFile(chi);
                require(java.util.Arrays.equals(audioData, Files.readAllBytes(receivedAudio.path)),
                        "Audio nhom bi thay doi byte");
                requireNoFile(List.of(binh, clients.get(3), clients.get(4), clients.get(5)));
                Files.deleteIfExists(receivedAudio.path);
                deleteDirectoryIfEmpty(receivedAudio.path.getParent());
            } finally {
                Files.deleteIfExists(audioFile);
            }
            System.out.println("[PASS] Audio nhóm UDP chỉ tới đúng thành viên và đúng byte");

            clearQueues(clients);
            an.connection.sendGroupText(allGroup.id(), "Xin chào tất cả 😀");
            for (TestClient client : clients) {
                requireGroupText(client, "Xin chào tất cả 😀");
            }
            System.out.println("[PASS] Nhóm Tất cả BROADCAST tới đủ 6 client");

            an.connection.deleteGroup(created.id());
            for (TestClient client : clients) {
                require(created.id().equals(client.removedGroups.poll(2, TimeUnit.SECONDS)),
                        client.name + " chưa nhận sự kiện xóa nhóm");
            }
            System.out.println("[PASS] Xóa nhóm biến mất trên mọi client");

            clearQueues(clients);
            byte[] originalData = "Dữ liệu file mẫu gửi bằng UDP.".getBytes(StandardCharsets.UTF_8);
            Path temporaryFile = Files.createTempFile("lap1-chat-test-", ".bin");
            Files.write(temporaryFile, originalData);
            try {
                an.connection.sendFile(RecipientMode.DIRECT, List.of(binh.name), temporaryFile);
                FileEvent receiverFile = requireFile(binh);
                require(java.util.Arrays.equals(originalData, Files.readAllBytes(receiverFile.path)),
                        "File phía người nhận bị thay đổi");
                requireNoFile(clients.subList(2, clients.size()));
                Files.deleteIfExists(receiverFile.path);
                deleteDirectoryIfEmpty(receiverFile.path.getParent());
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
            System.out.println("[PASS] Truyền file UDP đúng byte và đúng người nhận");

            Path oversizedFile = Files.createTempFile("lap1-too-large-", ".bin");
            boolean oversizedRejected = false;
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(oversizedFile.toFile(), "rw")) {
                randomAccessFile.setLength(Protocol.MAX_FILE_SIZE + 1);
            }
            try {
                an.connection.sendFile(RecipientMode.DIRECT, List.of(binh.name), oversizedFile);
            } catch (IOException expected) {
                oversizedRejected = expected.getMessage().contains("20 MB");
            } finally {
                Files.deleteIfExists(oversizedFile);
            }
            require(oversizedRejected, "Client phải từ chối file vượt 20 MB trước khi gửi");
            System.out.println("[PASS] Từ chối file vượt giới hạn 20 MB");

            an.connection.createGroup("Nhóm chỉ tồn tại trong RAM " + suffix, List.of(binh.name));
            awaitGroup(binh, "Nhóm chỉ tồn tại trong RAM " + suffix);

            deleteDirectoryIfEmpty(Path.of("received"));
        } finally {
            clients.forEach(client -> client.connection.close());
            server.close();
            serverThread.interrupt();
            serverThread.join(3_000);
        }

        int restartTcp = freeTcpPort();
        int restartFileUdp = freeTcpPort();
        ChatServer restarted = new ChatServer(restartTcp, restartFileUdp);
        Thread restartThread = new Thread(() -> {
            try { restarted.start(); } catch (IOException ignored) { }
        }, "restart-test-server");
        restartThread.start();
        Thread.sleep(500);
        ClientConnection restored = null;
        try {
            restored = ClientConnection.connect("localhost", restartTcp, persistedUser,
                    "lab1-" + persistedUser, false);
            TestClient restoredClient = new TestClient(persistedUser, restored);
            restored.startListening(restoredClient.listener);
            List<GroupInfo> catalog = restoredClient.groupCatalogs.poll(3, TimeUnit.SECONDS);
            require(catalog != null && catalog.size() == 1 && catalog.get(0).systemGroup(),
                    "Khoi dong lai phai giu tai khoan nhung lam moi nhom RAM");
            System.out.println("[PASS] Khởi động lại giữ tài khoản MySQL nhưng làm mới nhóm RAM");
        } finally {
            if (restored != null) restored.close();
            restarted.close();
            restartThread.interrupt();
            restartThread.join(3_000);
        }
        System.out.println("TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.");
    }

    private static TestClient connectWithRetry(int port, String name) throws Exception {
        IOException lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                ClientConnection connection = ClientConnection.connect("localhost", port, name);
                TestClient client = new TestClient(name, connection);
                connection.startListening(client.listener);
                Thread.sleep(100);
                return client;
            } catch (IOException exception) {
                lastError = exception;
                Thread.sleep(100);
            }
        }
        throw lastError;
    }

    private static void clearQueues(List<TestClient> clients) {
        clients.forEach(client -> {
            client.texts.clear();
            client.files.clear();
            client.errors.clear();
            client.groupTexts.clear();
            client.groupCatalogs.clear();
            client.groupMembers.clear();
            client.removedGroups.clear();
        });
    }

    private static GroupInfo awaitGroup(TestClient client, String name) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            List<GroupInfo> catalog = client.groupCatalogs.poll(500, TimeUnit.MILLISECONDS);
            if (catalog != null) {
                GroupInfo found = catalog.stream()
                        .filter(group -> group.name().equals(name)).findFirst().orElse(null);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError(client.name + " không thấy nhóm " + name);
    }

    private static void awaitMembership(TestClient client, String id, GroupInfo.Membership expected)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            List<GroupInfo> catalog = client.groupCatalogs.poll(500, TimeUnit.MILLISECONDS);
            if (catalog != null && catalog.stream()
                    .anyMatch(group -> group.id().equals(id) && group.membership() == expected)) {
                return;
            }
        }
        throw new AssertionError(client.name + " chưa có quyền " + expected);
    }

    private static void requireText(TestClient client, String expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            TextEvent event = client.texts.poll(300, TimeUnit.MILLISECONDS);
            if (event != null && expected.equals(event.text)) {
                return;
            }
        }
        throw new AssertionError(client.name + " không nhận được tin: " + expected);
    }

    private static void requireNoText(List<TestClient> clients) throws InterruptedException {
        for (TestClient client : clients) {
            require(client.texts.poll(300, TimeUnit.MILLISECONDS) == null,
                    client.name + " nhận nhầm tin nhắn");
        }
    }

    private static FileEvent requireFile(TestClient client) throws InterruptedException {
        FileEvent event = client.files.poll(10, TimeUnit.SECONDS);
        require(event != null, client.name + " không nhận được file");
        return event;
    }

    private static void requireGroupText(TestClient client, String expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            GroupTextEvent event = client.groupTexts.poll(300, TimeUnit.MILLISECONDS);
            if (event != null && expected.equals(event.text)) {
                return;
            }
        }
        throw new AssertionError(client.name + " không nhận được tin nhóm: " + expected);
    }

    private static void requireNoGroupText(List<TestClient> clients) throws InterruptedException {
        for (TestClient client : clients) {
            require(client.groupTexts.poll(300, TimeUnit.MILLISECONDS) == null,
                    client.name + " nhận nhầm tin nhóm");
        }
    }

    private static void requireError(TestClient client, String expectedPart) throws InterruptedException {
        String error = client.errors.poll(2, TimeUnit.SECONDS);
        require(error != null && error.toLowerCase().contains(expectedPart.toLowerCase()),
                client.name + " không nhận được lỗi chứa: " + expectedPart);
    }

    private static void requireNoFile(List<TestClient> clients) throws InterruptedException {
        for (TestClient client : clients) {
            require(client.files.poll(300, TimeUnit.MILLISECONDS) == null,
                    client.name + " nhận nhầm file");
        }
    }

    private static void deleteDirectoryIfEmpty(Path directory) throws IOException {
        if (directory != null && Files.isDirectory(directory)) {
            try (var entries = Files.list(directory)) {
                if (entries.findAny().isEmpty()) {
                    Files.deleteIfExists(directory);
                }
            }
        }
    }

    private static int freeTcpPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record TextEvent(String text) {
    }

    private record FileEvent(Path path) {
    }

    private record GroupTextEvent(String text) {
    }

    private record GroupMembersEvent(List<String> members) {
    }

    private static final class TestClient {
        private final String name;
        private final ClientConnection connection;
        private final BlockingQueue<TextEvent> texts = new LinkedBlockingQueue<>();
        private final BlockingQueue<FileEvent> files = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
        private final BlockingQueue<List<GroupInfo>> groupCatalogs = new LinkedBlockingQueue<>();
        private final BlockingQueue<GroupTextEvent> groupTexts = new LinkedBlockingQueue<>();
        private final BlockingQueue<GroupMembersEvent> groupMembers = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> removedGroups = new LinkedBlockingQueue<>();
        private final ClientConnection.Listener listener = new ClientConnection.Listener() {
            @Override
            public void onUserList(List<String> usernames) {
            }

            @Override
            public void onSystemMessage(String message) {
            }

            @Override
            public void onError(String message) {
                errors.add(message);
            }

            @Override
            public void onText(String sender, RecipientMode mode, List<String> targets,
                               long timestamp, String text) {
                texts.add(new TextEvent(text));
            }

            @Override
            public void onFile(String sender, RecipientMode mode, List<String> targets,
                               long timestamp, String originalName, Path savedPath) {
                files.add(new FileEvent(savedPath));
            }

            @Override
            public void onDisconnected(String reason) {
            }

            @Override
            public void onGroupCatalog(List<GroupInfo> groups) {
                groupCatalogs.add(groups);
            }

            @Override
            public void onGroupMembers(String groupId, List<String> members) {
                groupMembers.add(new GroupMembersEvent(members));
            }

            @Override
            public void onGroupRemoved(String groupId) {
                removedGroups.add(groupId);
            }

            @Override
            public void onGroupText(String groupId, String sender, long timestamp, String text) {
                groupTexts.add(new GroupTextEvent(text));
            }

            @Override
            public void onGroupFile(String groupId, String sender, long timestamp,
                                    String originalName, Path savedPath) {
                files.add(new FileEvent(savedPath));
            }
        };

        private TestClient(String name, ClientConnection connection) {
            this.name = name;
            this.connection = connection;
        }
    }
}
