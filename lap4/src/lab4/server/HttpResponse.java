package lab4.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tạo và ghi một HTTP/1.1 response xuống TCP stream. */
public final class HttpResponse {
    private final int statusCode;
    private final String reasonPhrase;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final byte[] body;

    public HttpResponse(int statusCode, String contentType, byte[] body) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonFor(statusCode);
        this.body = body == null ? new byte[0] : body.clone();
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", Integer.toString(this.body.length));
        headers.put("Connection", "close");
        headers.put("Server", "Lab4MiniWeb/1.0");
    }

    public static HttpResponse html(int statusCode, String title, String message) {
        String html = "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<title>" + title + "</title></head><body><h1>" + title + "</h1>"
                + "<p>" + message + "</p></body></html>";
        return new HttpResponse(
                statusCode,
                "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8));
    }

    public HttpResponse withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public void writeTo(OutputStream output, boolean includeBody) throws IOException {
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.1 ")
                .append(statusCode)
                .append(' ')
                .append(reasonPhrase)
                .append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            head.append(header.getKey())
                    .append(": ")
                    .append(header.getValue())
                    .append("\r\n");
        }
        head.append("\r\n");
        output.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        if (includeBody) {
            output.write(body);
        }
        output.flush();
    }

    public int getStatusCode() {
        return statusCode;
    }

    private static String reasonFor(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}
