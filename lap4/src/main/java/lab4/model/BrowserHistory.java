package lab4.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lịch sử URL trong phiên chạy hiện tại. */
public final class BrowserHistory {
    private static final int MAX_ENTRIES = 30;
    private final List<String> urls = new ArrayList<>();

    public void add(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!urls.isEmpty() && urls.get(0).equals(url)) {
            return;
        }
        urls.remove(url);
        urls.add(0, url);
        if (urls.size() > MAX_ENTRIES) {
            urls.remove(urls.size() - 1);
        }
    }

    public List<String> getUrls() {
        return Collections.unmodifiableList(new ArrayList<>(urls));
    }
}
