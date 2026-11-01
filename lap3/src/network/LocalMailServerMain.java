package network;

import java.util.concurrent.CountDownLatch;

/** Chạy SMTP/POP3 localhost thành tiến trình riêng để máy khác trong LAN kết nối. */
public final class LocalMailServerMain {
    private LocalMailServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String bindAddress = args.length > 0 ? args[0] : "0.0.0.0";
        LocalMailServer server = new LocalMailServer(bindAddress);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "mail-server-shutdown"));
        server.start();
        System.out.printf("SMTP đang nghe tại %s:%d%n", bindAddress, LocalMailServer.SMTP_PORT);
        System.out.printf("POP3 đang nghe tại %s:%d%n", bindAddress, LocalMailServer.POP3_PORT);
        new CountDownLatch(1).await();
    }
}
