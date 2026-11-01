package lab4.client;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Giao diện Mini Web Browser dùng RawHttpClient ở worker thread. */
@SuppressWarnings("serial") // JFrame có Serializable nhưng cửa sổ này không được lưu/khôi phục.
public final class MiniBrowser extends JFrame {
    private final RawHttpClient httpClient = new RawHttpClient();
    private final BrowserHistory history = new BrowserHistory();

    private final JComboBox<String> urlBox = new JComboBox<>();
    private final JButton goButton = new JButton("Go");
    private final JButton headButton = new JButton("HEAD");
    private final JButton downloadButton = new JButton("Tải xuống");
    private final JButton postButton = new JButton("Gửi POST");
    private final JTextField nameField = new JTextField(18);
    private final JTextArea messageArea = new JTextArea(3, 30);
    private final JEditorPane htmlPane = new JEditorPane();
    private final JTextArea headersArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Sẵn sàng");

    public MiniBrowser() {
        super("Lab 4 - Mini Web Browser");
        buildUi();
        bindActions();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        urlBox.setEditable(true);
        urlBox.addItem("http://localhost:8080/");

        JPanel addressBar = new JPanel(new BorderLayout(8, 0));
        addressBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        addressBar.add(new JLabel("URL:"), BorderLayout.WEST);
        addressBar.add(urlBox, BorderLayout.CENTER);

        JPanel requestButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        requestButtons.add(goButton);
        requestButtons.add(headButton);
        requestButtons.add(downloadButton);
        addressBar.add(requestButtons, BorderLayout.EAST);

        htmlPane.setEditable(false);
        htmlPane.setContentType("text/html; charset=UTF-8");
        htmlPane.setText("<html><body><h2>Mini Web Browser</h2>"
                + "<p>Khởi động server rồi nhấn Go.</p></body></html>");

        headersArea.setEditable(false);
        headersArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nội dung", new JScrollPane(htmlPane));
        tabs.addTab("Status và Headers", new JScrollPane(headersArea));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(createPostPanel(), BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        bottom.add(statusLabel, BorderLayout.SOUTH);

        add(addressBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel createPostPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("POST /submit"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 5, 3, 5);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Tên:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Nội dung:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        panel.add(new JScrollPane(messageArea), constraints);

        constraints.gridx = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(postButton, constraints);
        return panel;
    }

    private void bindActions() {
        goButton.addActionListener(event -> {
            String url = normalizedCurrentUrl();
            executeRequest(url, () -> httpClient.get(url));
        });
        headButton.addActionListener(event -> {
            String url = normalizedCurrentUrl();
            executeRequest(url, () -> httpClient.head(url));
        });
        postButton.addActionListener(event -> sendPost());
        downloadButton.addActionListener(event -> downloadCurrentUrl());
    }

    private void sendPost() {
        try {
            String baseUrl = normalizedCurrentUrl();
            String submitUrl = submitUrlFor(baseUrl);
            executeRequest(
                    submitUrl,
                    () -> httpClient.postForm(submitUrl, nameField.getText(), messageArea.getText()));
        } catch (IOException exception) {
            showError(exception);
        }
    }

    private void executeRequest(String url, RequestOperation operation) {
        setBusy(true, "Đang gửi request tới " + url);
        new SwingWorker<RawHttpClient.Response, Void>() {
            @Override
            protected RawHttpClient.Response doInBackground() throws Exception {
                return operation.execute();
            }

            @Override
            protected void done() {
                setBusy(false, "Sẵn sàng");
                try {
                    RawHttpClient.Response response = get();
                    history.add(url);
                    refreshHistory(url);
                    displayResponse(response);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(exception);
                } catch (ExecutionException exception) {
                    showError(exception.getCause());
                }
            }
        }.execute();
    }

    private void downloadCurrentUrl() {
        String url = normalizedCurrentUrl();
        String suggestedName;
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            int slash = path == null ? -1 : path.lastIndexOf('/');
            suggestedName = path == null || slash == path.length() - 1
                    ? "download.bin"
                    : path.substring(slash + 1);
        } catch (URISyntaxException exception) {
            showError(new IOException("URL không hợp lệ", exception));
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn nơi lưu file");
        chooser.setSelectedFile(new java.io.File(suggestedName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath();

        setBusy(true, "Đang tải " + url);
        new SwingWorker<RawHttpClient.Response, Void>() {
            @Override
            protected RawHttpClient.Response doInBackground() throws Exception {
                RawHttpClient.Response response = httpClient.get(url);
                if (!response.isSuccessful()) {
                    throw new IOException("Server trả về " + response.statusLine());
                }
                Files.write(destination, response.getBody());
                return response;
            }

            @Override
            protected void done() {
                setBusy(false, "Sẵn sàng");
                try {
                    RawHttpClient.Response response = get();
                    history.add(url);
                    refreshHistory(url);
                    displayResponse(response);
                    JOptionPane.showMessageDialog(
                            MiniBrowser.this,
                            "Đã lưu " + response.getBody().length + " byte vào:\n" + destination,
                            "Tải xuống thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(exception);
                } catch (ExecutionException exception) {
                    showError(exception.getCause());
                }
            }
        }.execute();
    }

    private void displayResponse(RawHttpClient.Response response) {
        headersArea.setText(response.headersAsText());
        headersArea.setCaretPosition(0);
        statusLabel.setText(response.statusLine() + " — " + response.getBody().length + " byte body");

        byte[] body = response.getBody();
        if (body.length == 0) {
            htmlPane.setContentType("text/plain; charset=UTF-8");
            htmlPane.setText("Response không có body. Đây là kết quả đúng đối với HEAD.");
        } else {
            String contentType = response.getHeader("content-type");
            if (contentType != null
                    && contentType.toLowerCase(Locale.ROOT).startsWith("text/html")) {
                htmlPane.setContentType("text/html; charset=UTF-8");
                htmlPane.setText(new String(body, charsetFrom(contentType)));
            } else {
                htmlPane.setContentType("text/plain; charset=UTF-8");
                htmlPane.setText("Nội dung nhị phân hoặc không phải HTML ("
                        + (contentType == null ? "không rõ Content-Type" : contentType)
                        + ").\nDùng nút Tải xuống để lưu file.");
            }
        }
        htmlPane.setCaretPosition(0);
    }

    private void refreshHistory(String selectedUrl) {
        urlBox.removeAllItems();
        for (String url : history.getUrls()) {
            urlBox.addItem(url);
        }
        urlBox.setSelectedItem(selectedUrl);
    }

    private String normalizedCurrentUrl() {
        Object item = urlBox.getEditor().getItem();
        String url = item == null ? "" : item.toString().trim();
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        urlBox.getEditor().setItem(url);
        return url;
    }

    private static String submitUrlFor(String baseUrl) throws IOException {
        try {
            URI base = new URI(baseUrl);
            if (!"http".equalsIgnoreCase(base.getScheme()) || base.getHost() == null) {
                throw new IOException("URL phải bắt đầu bằng http:// và có host");
            }
            return new URI("http", null, base.getHost(), base.getPort(), "/submit", null, null)
                    .toString();
        } catch (URISyntaxException exception) {
            throw new IOException("URL không hợp lệ", exception);
        }
    }

    private static Charset charsetFrom(String contentType) {
        for (String part : contentType.split(";")) {
            String value = part.trim();
            if (value.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                try {
                    return Charset.forName(value.substring("charset=".length()).replace("\"", ""));
                } catch (Exception ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private void setBusy(boolean busy, String text) {
        goButton.setEnabled(!busy);
        headButton.setEnabled(!busy);
        postButton.setEnabled(!busy);
        downloadButton.setEnabled(!busy);
        statusLabel.setText(text);
    }

    private void showError(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "Lỗi không xác định"
                : error.getMessage();
        statusLabel.setText("Lỗi: " + message);
        JOptionPane.showMessageDialog(this, message, "Không thể thực hiện", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface RequestOperation {
        RawHttpClient.Response execute() throws Exception;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Nếu look-and-feel hệ thống không có, Swing dùng giao diện mặc định.
            }
            new MiniBrowser().setVisible(true);
        });
    }
}
