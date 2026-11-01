package lab4.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import lab4.client.RawHttpClient;
import lab4.server.MiniWebServer;

/** Integration test không phụ thuộc UI cho HTTP server/client tự cài. */
public final class HttpIntegrationTest {
    private HttpIntegrationTest() { }

    public static void main(String[] args) throws Exception {
        Path webRoot = Files.createTempDirectory("lab4-http-test-");
        Files.writeString(webRoot.resolve("index.html"), "<h1>Xin chào HTTP</h1>", StandardCharsets.UTF_8);
        Files.write(webRoot.resolve("sample.pdf"), new byte[] {1, 2, 3, 4, 5});

        int port = freePort();
        MiniWebServer server = new MiniWebServer(port, webRoot);
        AtomicReference<Throwable> serverError = new AtomicReference<>();
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Throwable error) {
                serverError.set(error);
            }
        }, "lab4-integration-server");
        serverThread.start();

        try {
            awaitServer(port);
            RawHttpClient client = new RawHttpClient();
            String baseUrl = "http://127.0.0.1:" + port;

            RawHttpClient.Response get = client.get(baseUrl + "/");
            require(get.getStatusCode() == 200, "GET / phải trả 200");
            require(new String(get.getBody(), StandardCharsets.UTF_8).contains("Xin chào HTTP"),
                    "GET / phải trả đúng HTML");

            RawHttpClient.Response head = client.head(baseUrl + "/sample.pdf");
            require(head.getStatusCode() == 200, "HEAD file phải trả 200");
            require(head.getBody().length == 0, "HEAD không được có body");
            require("5".equals(head.getHeader("content-length")), "HEAD phải giữ Content-Length");

            RawHttpClient.Response post = client.postForm(baseUrl + "/submit", "An", "Xin chào");
            require(post.getStatusCode() == 200, "POST /submit phải trả 200");
            require(new String(post.getBody(), StandardCharsets.UTF_8).contains("Xin chào"),
                    "POST phải phản hồi dữ liệu form");

            require(client.get(baseUrl + "/missing").getStatusCode() == 404,
                    "Tài nguyên thiếu phải trả 404");
            require(rawStatus(port, "PUT / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n") == 405,
                    "Method không hỗ trợ phải trả 405");
            require(rawStatus(port,
                    "GET /%2e%2e/secret.txt HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n") == 400,
                    "Path traversal phải trả 400");

            System.out.println("PASS: GET, HEAD, POST, 404, 405 và path traversal hoạt động.");
        } finally {
            server.stop();
            serverThread.join(5_000);
            deleteTree(webRoot);
        }

        if (serverError.get() != null) {
            throw new AssertionError("HTTP server lỗi", serverError.get());
        }
    }

    private static int rawStatus(int port, String request) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.ISO_8859_1));
            output.flush();
            String status = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.ISO_8859_1)).readLine();
            return Integer.parseInt(status.split(" ", 3)[1]);
        }
    }

    private static void awaitServer(int port) throws Exception {
        IOException lastError = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            try {
                int status = rawStatus(port,
                        "HEAD / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
                if (status == 200) {
                    return;
                }
            } catch (IOException error) {
                lastError = error;
                Thread.sleep(50);
            }
        }
        throw new IOException("HTTP server không sẵn sàng", lastError);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
