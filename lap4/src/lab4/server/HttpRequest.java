package lab4.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Một HTTP request đã được đọc từ TCP stream.
 * Lớp này chỉ hỗ trợ phần HTTP/1.1 cần dùng trong bài lab.
 */
public final class HttpRequest {
    public static final int MAX_HEADER_SIZE = 16 * 1024;
    public static final int MAX_BODY_SIZE = 1024 * 1024;

    private final String method;
    private final String requestTarget;
    private final String path;
    private final String version;
    private final Map<String, String> headers;
    private final byte[] body;

    private HttpRequest(
            String method,
            String requestTarget,
            String path,
            String version,
            Map<String, String> headers,
            byte[] body) {
        this.method = method;
        this.requestTarget = requestTarget;
        this.path = path;
        this.version = version;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body.clone();
    }

    public static HttpRequest parse(InputStream input) throws IOException, BadRequestException {
        byte[] headerBytes = readHeader(input);
        String headerText = new String(headerBytes, StandardCharsets.ISO_8859_1);
        String[] lines = headerText.split("\\r\\n", -1);

        if (lines.length == 0 || lines[0].isBlank()) {
            throw new BadRequestException("Thiếu request line");
        }

        String[] requestLine = lines[0].trim().split("\\s+");
        if (requestLine.length != 3) {
            throw new BadRequestException("Request line không hợp lệ");
        }

        String method = requestLine[0];
        String target = requestLine[1];
        String version = requestLine[2];

        if (!method.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
            throw new BadRequestException("HTTP method không hợp lệ");
        }
        if (!"HTTP/1.1".equals(version)) {
            throw new BadRequestException("Chỉ hỗ trợ HTTP/1.1");
        }

        String decodedPath = validateAndGetPath(target);
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new BadRequestException("Header không hợp lệ: " + line);
            }
            String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (name.isEmpty() || headers.containsKey(name)) {
                throw new BadRequestException("Header bị trùng hoặc không có tên");
            }
            headers.put(name, value);
        }

        if (!headers.containsKey("host") || headers.get("host").isBlank()) {
            throw new BadRequestException("HTTP/1.1 yêu cầu header Host");
        }
        if (headers.containsKey("transfer-encoding")) {
            throw new BadRequestException("Không hỗ trợ Transfer-Encoding");
        }

        int contentLength = parseContentLength(headers.get("content-length"));
        byte[] body = readExactly(input, contentLength);
        return new HttpRequest(method, target, decodedPath, version, headers, body);
    }

    private static byte[] readHeader(InputStream input) throws IOException, BadRequestException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        int[] delimiter = {'\r', '\n', '\r', '\n'};

        while (buffer.size() < MAX_HEADER_SIZE + delimiter.length) {
            int value = input.read();
            if (value == -1) {
                throw new BadRequestException("Kết nối đóng trước khi hết HTTP header");
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
        throw new BadRequestException("HTTP header vượt quá 16 KB");
    }

    private static int parseContentLength(String value) throws BadRequestException {
        if (value == null) {
            return 0;
        }
        try {
            long length = Long.parseLong(value);
            if (length < 0 || length > MAX_BODY_SIZE) {
                throw new BadRequestException("Content-Length không hợp lệ hoặc body quá lớn");
            }
            return (int) length;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Content-Length không phải số");
        }
    }

    private static byte[] readExactly(InputStream input, int length)
            throws IOException, BadRequestException {
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(body, offset, length - offset);
            if (count == -1) {
                throw new BadRequestException("Body ngắn hơn Content-Length");
            }
            offset += count;
        }
        return body;
    }

    private static String validateAndGetPath(String target) throws BadRequestException {
        try {
            URI uri = new URI(target);
            if (uri.isAbsolute()
                    || uri.getRawAuthority() != null
                    || uri.getRawFragment() != null
                    || uri.getRawPath() == null
                    || !uri.getRawPath().startsWith("/")) {
                throw new BadRequestException("Request target phải là đường dẫn bắt đầu bằng /");
            }
            return uri.getPath();
        } catch (URISyntaxException exception) {
            throw new BadRequestException("Request target không hợp lệ");
        }
    }

    public String getMethod() {
        return method;
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public static final class BadRequestException extends Exception {
        private static final long serialVersionUID = 1L;

        public BadRequestException(String message) {
            super(message);
        }
    }
}
