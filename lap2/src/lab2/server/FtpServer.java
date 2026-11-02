package lab2.server;

import lab2.common.FtpProtocol;
import lab2.db.MySqlDB;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FtpServer {
    private FtpServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : FtpProtocol.DEFAULT_PORT;
        Path root = Path.of(args.length > 1 ? args[1] : "ftp-root").toAbsolutePath().normalize();
        MySqlDB database = new MySqlDB();
        try {
            database.ensureReady();
        } catch (SQLException error) {
            System.err.println("Không thể kết nối MySQL. Hãy sửa user/password trong db.properties.");
            System.err.println("Chi tiết: " + error.getMessage());
            return;
        }

        ExecutorService workers = Executors.newFixedThreadPool(20);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.printf("Lab 2 FTP Server: ftp://localhost:%d%n", port);
            System.out.println("FTP root: " + root);
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = server.accept();
                client.setSoTimeout(120_000);
                workers.execute(new ControlHandler(client, database, root));
            }
        } finally {
            workers.shutdownNow();
        }
    }
}
