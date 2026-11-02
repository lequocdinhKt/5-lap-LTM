package lab4.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Điểm khởi động Mini Web Server. */
public final class MiniWebServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int WORKER_COUNT = 4;

    private final int port;
    private final FileService fileService;
    private final ExecutorService workers;
    private volatile ServerSocket serverSocket;

    public MiniWebServer(int port, Path webRoot) throws IOException {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port phải nằm trong khoảng 1..65535");
        }
        this.port = port;
        this.fileService = new FileService(webRoot);
        this.workers = Executors.newFixedThreadPool(WORKER_COUNT);
    }

    public void start() throws IOException {
        try (ServerSocket listener = new ServerSocket(port)) {
            serverSocket = listener;
            System.out.printf("Mini Web Server đang chạy tại http://localhost:%d/%n", port);
            System.out.println("Nhấn Ctrl+C để dừng server.");

            while (!listener.isClosed()) {
                try {
                    Socket client = listener.accept();
                    workers.submit(new ClientHandler(client, fileService));
                } catch (RejectedExecutionException exception) {
                    if (!listener.isClosed()) {
                        throw exception;
                    }
                } catch (SocketException exception) {
                    if (!listener.isClosed()) {
                        throw exception;
                    }
                }
            }
        } finally {
            workers.shutdownNow();
            serverSocket = null;
        }
    }

    public void stop() {
        ServerSocket listener = serverSocket;
        if (listener != null && !listener.isClosed()) {
            try {
                listener.close();
            } catch (IOException exception) {
                System.err.println("Không thể đóng server socket: " + exception.getMessage());
            }
        }
        workers.shutdownNow();
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        Path webRoot = Path.of("www");

        try {
            if (args.length >= 1) {
                port = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                webRoot = Path.of(args[1]);
            }

            MiniWebServer server = new MiniWebServer(port, webRoot);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-shutdown"));
            server.start();
        } catch (NumberFormatException exception) {
            System.err.println("Port phải là số nguyên.");
            System.err.println("Cách dùng: java lab4.server.MiniWebServer [port] [www-path]");
        } catch (Exception exception) {
            System.err.println("Không thể chạy server: " + exception.getMessage());
        }
    }
}
