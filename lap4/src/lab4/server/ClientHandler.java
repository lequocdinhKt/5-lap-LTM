package lab4.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Xử lý đúng một request trên một TCP connection rồi đóng connection. */
public final class ClientHandler implements Runnable {
    private static final int SOCKET_TIMEOUT_MS = 10_000;

    private final Socket socket;
    private final FileService fileService;

    public ClientHandler(Socket socket, FileService fileService) {
        this.socket = socket;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        String method = "?";
        String path = "?";
        int status = 500;

        try (Socket client = socket;
                InputStream input = client.getInputStream();
                OutputStream output = client.getOutputStream()) {
            client.setSoTimeout(SOCKET_TIMEOUT_MS);
            HttpResponse response;
            boolean includeBody = true;

            try {
                HttpRequest request = HttpRequest.parse(input);
                method = request.getMethod();
                path = request.getRequestTarget();
                // Nếu HEAD gặp cả lỗi 400/500 trong lúc điều phối thì vẫn không gửi body.
                includeBody = !"HEAD".equals(method);
                HandledResponse handled = dispatch(request);
                response = handled.response();
                includeBody = handled.includeBody();
            } catch (HttpRequest.BadRequestException exception) {
                response = errorResponse(400, "400 Bad Request", exception.getMessage());
            } catch (SocketTimeoutException exception) {
                response = errorResponse(400, "400 Bad Request", "Hết thời gian chờ request");
            } catch (Exception exception) {
                System.err.println("Lỗi xử lý request: " + exception.getMessage());
                response = errorResponse(500, "500 Internal Server Error", "Server gặp lỗi nội bộ");
            }

            status = response.getStatusCode();
            response.writeTo(output, includeBody);
        } catch (IOException exception) {
            System.err.println("Lỗi kết nối với client: " + exception.getMessage());
        } finally {
            System.out.printf("[%s] %s %s -> %d%n",
                    Thread.currentThread().getName(), method, path, status);
        }
    }

    private HandledResponse dispatch(HttpRequest request)
            throws IOException, HttpRequest.BadRequestException {
        return switch (request.getMethod()) {
            case "GET" -> serveFile(request, true);
            case "HEAD" -> serveFile(request, false);
            case "POST" -> handlePost(request);
            default -> new HandledResponse(
                    errorResponse(405, "405 Method Not Allowed", "Phương thức không được hỗ trợ")
                            .withHeader("Allow", "GET, HEAD, POST"),
                    true);
        };
    }

    private HandledResponse serveFile(HttpRequest request, boolean includeBody)
            throws IOException, HttpRequest.BadRequestException {
        try {
            FileService.Resource resource = fileService.load(request.getPath());
            return new HandledResponse(
                    new HttpResponse(200, resource.contentType(), resource.bytes()),
                    includeBody);
        } catch (NoSuchFileException | FileNotFoundException exception) {
            return new HandledResponse(
                    errorResponse(404, "404 Not Found", "Không tìm thấy tài nguyên yêu cầu"),
                    includeBody);
        }
    }

    private HandledResponse handlePost(HttpRequest request)
            throws HttpRequest.BadRequestException {
        if (!"/submit".equals(request.getPath())) {
            return new HandledResponse(
                    errorResponse(404, "404 Not Found", "Endpoint POST không tồn tại"),
                    true);
        }

        String contentType = request.getHeader("content-type");
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT)
                        .startsWith("application/x-www-form-urlencoded")) {
            throw new HttpRequest.BadRequestException(
                    "POST /submit yêu cầu application/x-www-form-urlencoded");
        }

        String formText = new String(request.getBody(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(formText);
        String name = escapeHtml(form.getOrDefault("name", ""));
        String message = escapeHtml(form.getOrDefault("message", ""));

        String html = "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<title>Kết quả POST</title></head><body>"
                + "<h1>Server đã nhận dữ liệu</h1>"
                + "<p><strong>Tên:</strong> " + name + "</p>"
                + "<p><strong>Nội dung:</strong> " + message + "</p>"
                + "<p><a href=\"/\">Về trang chủ</a></p></body></html>";

        return new HandledResponse(
                new HttpResponse(
                        200,
                        "text/html; charset=UTF-8",
                        html.getBytes(StandardCharsets.UTF_8)),
                true);
    }

    private static Map<String, String> parseForm(String body)
            throws HttpRequest.BadRequestException {
        Map<String, String> values = new LinkedHashMap<>();
        if (body.isEmpty()) {
            return values;
        }

        try {
            for (String pair : body.split("&")) {
                int equals = pair.indexOf('=');
                String rawName = equals >= 0 ? pair.substring(0, equals) : pair;
                String rawValue = equals >= 0 ? pair.substring(equals + 1) : "";
                String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                values.put(name, value);
            }
            return values;
        } catch (IllegalArgumentException exception) {
            throw new HttpRequest.BadRequestException("Form URL-encoded không hợp lệ");
        }
    }

    private static HttpResponse errorResponse(int status, String title, String message) {
        return HttpResponse.html(status, title, escapeHtml(message));
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record HandledResponse(HttpResponse response, boolean includeBody) {
    }
}
