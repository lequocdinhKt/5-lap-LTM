package lab4.service;

import lab4.client.RawHttpClient;
import lab4.model.BrowserHistory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Điều phối request, lịch sử và tải file cho Mini Browser. */
public final class BrowserService {
    private final RawHttpClient client = new RawHttpClient();
    private final BrowserHistory history = new BrowserHistory();

    public PageResult get(String inputUrl) throws IOException {
        String url = normalize(inputUrl);
        return remember(url, client.get(url));
    }

    public PageResult head(String inputUrl) throws IOException {
        String url = normalize(inputUrl);
        return remember(url, client.head(url));
    }

    public PageResult post(String inputUrl, String name, String message) throws IOException {
        String url = submitUrl(normalize(inputUrl));
        return remember(url, client.postForm(url, name, message));
    }

    public DownloadResult download(String inputUrl, Path destination) throws IOException {
        String url = normalize(inputUrl);
        RawHttpClient.Response response = client.get(url);
        if (!response.isSuccessful()) {
            throw new IOException("Server trả về " + response.statusLine());
        }
        byte[] body = response.getBody();
        Files.write(destination, body);
        PageResult page = remember(url, response);
        return new DownloadResult(page, destination, body.length);
    }

    public List<String> history() {
        return history.getUrls();
    }

    private PageResult remember(String url, RawHttpClient.Response response) {
        history.add(url);
        return new PageResult(url, response.getStatusCode(), response.getReasonPhrase(),
                response.getHeaders(), response.getBody());
    }

    public String normalize(String inputUrl) {
        String url = inputUrl == null ? "" : inputUrl.trim();
        if (url.isEmpty()) {
            return "http://localhost:8080/";
        }
        return url.contains("://") ? url : "http://" + url;
    }

    private static String submitUrl(String baseUrl) throws IOException {
        try {
            URI base = new URI(baseUrl);
            if (!"http".equalsIgnoreCase(base.getScheme()) || base.getHost() == null) {
                throw new IOException("URL phải bắt đầu bằng http:// và có host");
            }
            return new URI("http", null, base.getHost(), base.getPort(), "/submit", null, null).toString();
        } catch (URISyntaxException error) {
            throw new IOException("URL không hợp lệ", error);
        }
    }

    public record PageResult(String url, int statusCode, String reasonPhrase,
                             Map<String, String> headers, byte[] body) {
        public PageResult {
            headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            body = body.clone();
        }

        @Override public byte[] body() {
            return body.clone();
        }

        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        public boolean successful() {
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

    public record DownloadResult(PageResult page, Path path, int bytes) { }
}
