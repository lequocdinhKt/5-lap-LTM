package lab4.client;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP client tối giản tự gửi và phân tích HTTP/1.1 trên TCP Socket.
 * Không sử dụng HttpURLConnection hoặc thư viện HTTP cấp cao.
 */
public final class RawHttpClient {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_HEADER_SIZE = 32 * 1024;
    private static final int MAX_RESPONSE_SIZE = 100 * 1024 * 1024;

    public Response get(String url) throws IOException {
        return send("GET", url, null, null);
    }

    public Response head(String url) throws IOException {
        return send("HEAD", url, null, null);
    }

    public Response postForm(String url, String name, String message) throws IOException {
        String form = "name=" + encodeForm(name) + "&message=" + encodeForm(message);
        byte[] body = form.getBytes(StandardCharsets.UTF_8);
        return send(
                "POST",
                url,
                "application/x-www-form-urlencoded; charset=UTF-8",
                body);
    }

    private Response send(String method, String url, String contentType, byte[] requestBody)
            throws IOException {
        URI uri = parseHttpUri(url);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        String target = buildRequestTarget(uri);
        byte[] body = requestBody == null ? new byte[0] : requestBody;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            try (OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream()) {
                StringBuilder requestHead = new StringBuilder();
                requestHead.append(method).append(' ').append(target).append(" HTTP/1.1\r\n");
                requestHead.append("Host: ").append(hostHeader(host, port)).append("\r\n");
                requestHead.append("Connection: close\r\n");
                requestHead.append("User-Agent: Lab4MiniBrowser/1.0\r\n");
                if (body.length > 0 || "POST".equals(method)) {
                    requestHead.append("Content-Type: ").append(contentType).append("\r\n");
                    requestHead.append("Content-Length: ").append(body.length).append("\r\n");
                }
                requestHead.append("\r\n");

                output.write(requestHead.toString().getBytes(StandardCharsets.ISO_8859_1));
                if (body.length > 0) {
                    output.write(body);
                }
                output.flush();

                return readResponse(input, "HEAD".equals(method));
            }
        }
    }

    private static Response readResponse(InputStream input, boolean headRequest) throws IOException {
        byte[] headerBytes = readHeader(input);
        String headerText = new String(headerBytes, StandardCharsets.ISO_8859_1);
        String[] lines = headerText.split("\\r\\n", -1);
        if (lines.length == 0) {
            throw new IOException("Response không có status line");
        }

        String[] statusParts = lines[0].split(" ", 3);
        if (statusParts.length < 2 || !statusParts[0].startsWith("HTTP/")) {
            throw new IOException("Status line không hợp lệ: " + lines[0]);
        }

        int statusCode;
        try {
            statusCode = Integer.parseInt(statusParts[1]);
        } catch (NumberFormatException exception) {
            throw new IOException("Status code không hợp lệ", exception);
        }
        String reasonPhrase = statusParts.length == 3 ? statusParts[2] : "";

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            int colon = lines[i].indexOf(':');
            if (colon <= 0) {
                throw new IOException("Response header không hợp lệ: " + lines[i]);
            }
            String name = lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = lines[i].substring(colon + 1).trim();
            if (headers.containsKey(name)) {
                throw new IOException("Response có header trùng: " + name);
            }
            headers.put(name, value);
        }

        String transferEncoding = headers.get("transfer-encoding");
        if (transferEncoding != null && !"identity".equalsIgnoreCase(transferEncoding)) {
            throw new IOException("Client không hỗ trợ Transfer-Encoding: " + transferEncoding);
        }

        boolean statusHasNoBody = statusCode / 100 == 1 || statusCode == 204 || statusCode == 304;
        byte[] responseBody;
        if (headRequest || statusHasNoBody) {
            responseBody = new byte[0];
        } else if (headers.containsKey("content-length")) {
            int contentLength = parseResponseLength(headers.get("content-length"));
            responseBody = readExactly(input, contentLength);
        } else {
            responseBody = readToEnd(input);
        }

        return new Response(statusCode, reasonPhrase, headers, responseBody);
    }

    private static byte[] readHeader(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        int[] delimiter = {'\r', '\n', '\r', '\n'};

        while (buffer.size() < MAX_HEADER_SIZE + delimiter.length) {
            int value = input.read();
            if (value == -1) {
                throw new EOFException("Server đóng kết nối trước khi hết response header");
            }
            buffer.write(value);
            if (value == delimiter[matched]) {
                matched++;
                if (matched == delimiter.length) {
                    byte[] all = buffer.toByteArray();
                    byte[] headerOnly = new byte[all.length - delimiter.length];
                    System.arraycopy(all, 0, headerOnly, 0, headerOnly.length);
                    return headerOnly;
                }
            } else {
                matched = value == delimiter[0] ? 1 : 0;
            }
        }
        throw new IOException("Response header vượt quá 32 KB");
    }

    private static int parseResponseLength(String value) throws IOException {
        try {
            long length = Long.parseLong(value);
            if (length < 0 || length > MAX_RESPONSE_SIZE) {
                throw new IOException("Response body quá lớn hoặc Content-Length không hợp lệ");
            }
            return (int) length;
        } catch (NumberFormatException exception) {
            throw new IOException("Content-Length của response không phải số", exception);
        }
    }

    private static byte[] readExactly(InputStream input, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(data, offset, length - offset);
            if (count == -1) {
                throw new EOFException("Response body ngắn hơn Content-Length");
            }
            offset += count;
        }
        return data;
    }

    private static byte[] readToEnd(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count;
        while ((count = input.read(chunk)) != -1) {
            if (output.size() + count > MAX_RESPONSE_SIZE) {
                throw new IOException("Response body vượt quá 100 MB");
            }
            output.write(chunk, 0, count);
        }
        return output.toByteArray();
    }

    private static URI parseHttpUri(String url) throws IOException {
        try {
            URI uri = new URI(url);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new IOException("URL phải có dạng http://host[:port]/path");
            }
            if (uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65_535) {
                throw new IOException("Port trong URL không hợp lệ");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new IOException("URL không hợp lệ: " + exception.getMessage(), exception);
        }
    }

    private static String buildRequestTarget(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        return path;
    }

    private static String hostHeader(String host, int port) {
        String printableHost = host.contains(":") ? "[" + host + "]" : host;
        return port == 80 ? printableHost : printableHost + ":" + port;
    }

    private static String encodeForm(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** Kết quả response; body luôn giữ dưới dạng byte để không làm hỏng file nhị phân. */
    public static final class Response {
        private final int statusCode;
        private final String reasonPhrase;
        private final Map<String, String> headers;
        private final byte[] body;

        private Response(
                int statusCode,
                String reasonPhrase,
                Map<String, String> headers,
                byte[] body) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            this.body = body.clone();
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getReasonPhrase() {
            return reasonPhrase;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getHeader(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        public byte[] getBody() {
            return body.clone();
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public String statusLine() {
            return "HTTP/1.1 " + statusCode + " " + reasonPhrase;
        }

        public String headersAsText() {
            StringBuilder text = new StringBuilder(statusLine()).append('\n');
            headers.forEach((name, value) -> text.append(name).append(": ").append(value).append('\n'));
            return text.toString();
        }
    }

    /** Chạy thử client ở chế độ console, độc lập với giao diện JavaFX. */
    public static void main(String[] args) {
        String url = args.length == 0 ? "http://localhost:8080/" : args[0];
        try {
            Response response = new RawHttpClient().get(url);
            System.out.print(response.headersAsText());
            System.out.println("Body: " + response.getBody().length + " byte");
        } catch (IOException exception) {
            System.err.println("Không thể gửi request: " + exception.getMessage());
        }
    }
}
