package lab1.test;

import lab1.client.ClientConnection;
import lab1.common.GroupInfo;
import lab1.common.RecipientMode;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class LiveDemoBot {
    private LiveDemoBot() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String directTarget = args.length > 2 ? args[2] : "An";

        CountDownLatch userListReady = new CountDownLatch(1);
        AtomicReference<List<String>> onlineUsers = new AtomicReference<>(List.of());
        AtomicReference<Throwable> listenerError = new AtomicReference<>();
        AtomicReference<String> allGroupId = new AtomicReference<>();
        CountDownLatch allGroupReady = new CountDownLatch(1);

        try (ClientConnection bot = ClientConnection.connect(host, port, "BotDemo")) {
            bot.startListening(new ClientConnection.Listener() {
                @Override
                public void onUserList(List<String> usernames) {
                    onlineUsers.set(usernames);
                    if (usernames.contains(directTarget)) {
                        userListReady.countDown();
                    }
                }

                @Override
                public void onSystemMessage(String message) {
                    System.out.println("[SYSTEM] " + message);
                }

                @Override
                public void onError(String message) {
                    listenerError.set(new IllegalStateException(message));
                }

                @Override
                public void onText(String sender, RecipientMode mode, List<String> targets,
                                   long timestamp, String text) {
                    System.out.printf("[NHẬN LẠI] %s - %s%n", mode.name(), text);
                }

                @Override
                public void onFile(String sender, RecipientMode mode, List<String> targets,
                                   long timestamp, String originalName, Path savedPath) {
                    System.out.println("[NHẬN FILE UDP] " + savedPath);
                }

                @Override
                public void onDisconnected(String reason) {
                    System.out.println("[NGẮT KẾT NỐI] " + reason);
                }

                @Override
                public void onGroupCatalog(List<GroupInfo> groups) {
                    groups.stream().filter(GroupInfo::systemGroup).findFirst().ifPresent(group -> {
                        allGroupId.set(group.id());
                        allGroupReady.countDown();
                    });
                }
            });

            if (!userListReady.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Chưa thấy client demo. Đang online: " + onlineUsers.get());
            }

            bot.sendText(RecipientMode.DIRECT, List.of(directTarget),
                    "[AUTO DEMO] DIRECT: tin này chỉ gửi riêng cho " + directTarget);
            Thread.sleep(500);

            if (!allGroupReady.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Chưa nhận được nhóm Tất cả.");
            }
            bot.sendGroupText(allGroupId.get(),
                    "[AUTO DEMO] BROADCAST qua nhóm Tất cả");
            Thread.sleep(500);

            bot.createGroup("Nhóm demo " + System.currentTimeMillis() % 10_000,
                    List.of(directTarget));
            Thread.sleep(1_000);

            if (listenerError.get() != null) {
                throw new IllegalStateException("Server báo lỗi trong lúc demo", listenerError.get());
            }
            System.out.println("AUTO DEMO HOÀN TẤT: DIRECT, NHÓM TẤT CẢ VÀ TẠO NHÓM.");
        }
    }
}
