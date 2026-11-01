package lab4.ui;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lab4.client.RawHttpClient;
import lab4.model.BrowserHistory;

/** Mini Browser JavaFX; mọi request vẫn đi qua RawHttpClient tự cài trên TCP socket. */
public final class MiniBrowserApp extends Application {
    private final RawHttpClient httpClient = new RawHttpClient();
    private final BrowserHistory history = new BrowserHistory();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "http-client-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ComboBox<String> urlBox = new ComboBox<>();
    private final Button getButton = primaryButton("GET");
    private final Button headButton = secondaryButton("HEAD");
    private final Button downloadButton = secondaryButton("Tải xuống");
    private final Label connectionBadge = new Label("Chưa gửi request");
    private final Label statusLabel = new Label("Sẵn sàng");
    private final Label toastLabel = new Label();

    private final ListView<String> historyList = new ListView<>();
    private final TextField nameField = new TextField();
    private final TextArea messageArea = new TextArea();
    private final Button postButton = primaryButton("Gửi POST /submit");

    private final TextArea contentArea = new TextArea();
    private final TextArea headersArea = new TextArea();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final VBox loadingOverlay = new VBox(10);

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setCenter(buildWorkspace());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/css/lab4.css").toExternalForm());
        stage.setTitle("Network Labs · Mini Browser");
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        urlBox.getItems().add("http://localhost:8080/");
        urlBox.setValue("http://localhost:8080/");
        showWelcome();
    }

    private Node buildTopBar() {
        Label product = new Label("NETWORK LABS");
        product.getStyleClass().add("eyebrow");
        Label title = new Label("HTTP Mini Browser");
        title.getStyleClass().add("app-title");
        VBox brand = new VBox(2, product, title);

        connectionBadge.getStyleClass().addAll("status-badge", "status-idle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(16, brand, spacer, connectionBadge);
        heading.setAlignment(Pos.CENTER_LEFT);

        urlBox.setEditable(true);
        urlBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(urlBox, Priority.ALWAYS);
        urlBox.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendGet();
            }
        });
        getButton.setOnAction(event -> sendGet());
        headButton.setOnAction(event -> sendHead());
        downloadButton.setOnAction(event -> downloadCurrentUrl());

        Label protocol = new Label("HTTP");
        protocol.getStyleClass().add("protocol-chip");
        HBox address = new HBox(8, protocol, urlBox, getButton, headButton, downloadButton);
        address.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(14, heading, address);
        top.getStyleClass().add("top-bar");
        return top;
    }

    private Node buildWorkspace() {
        VBox sidebar = buildSidebar();

        headersArea.setEditable(false);
        headersArea.setWrapText(false);
        headersArea.getStyleClass().add("headers-area");
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("content-area");
        Tab contentTab = new Tab("Nội dung", contentArea);
        Tab headersTab = new Tab("Status & Headers", headersArea);
        contentTab.setClosable(false);
        headersTab.setClosable(false);
        TabPane tabs = new TabPane(contentTab, headersTab);

        Label loadingText = new Label("Đang gửi HTTP request…");
        loadingText.getStyleClass().add("loading-text");
        progressIndicator.setPrefSize(42, 42);
        loadingOverlay.getChildren().addAll(progressIndicator, loadingText);
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);

        StackPane content = new StackPane(tabs, loadingOverlay);
        content.getStyleClass().add("content-card");
        SplitPane split = new SplitPane(sidebar, content);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.25);
        split.setPadding(new Insets(16));
        return split;
    }

    private VBox buildSidebar() {
        Label historyTitle = new Label("Lịch sử phiên");
        historyTitle.getStyleClass().add("section-title");
        historyList.setItems(FXCollections.observableArrayList());
        historyList.setPlaceholder(new Label("Chưa có request"));
        historyList.setOnMouseClicked(event -> {
            String selected = historyList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                urlBox.setValue(selected);
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    sendGet();
                }
            }
        });
        VBox.setVgrow(historyList, Priority.ALWAYS);

        Label postTitle = new Label("POST /submit");
        postTitle.getStyleClass().add("section-title");
        nameField.setPromptText("Tên người gửi");
        messageArea.setPromptText("Nội dung gửi tới server");
        messageArea.setPrefRowCount(3);
        messageArea.setWrapText(true);
        postButton.setMaxWidth(Double.MAX_VALUE);
        postButton.setOnAction(event -> sendPost());

        VBox sidebar = new VBox(10,
                historyTitle,
                historyList,
                new Separator(),
                postTitle,
                labeledField("Tên", nameField),
                labeledField("Nội dung", messageArea),
                postButton);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setMinWidth(210);
        sidebar.setPrefWidth(280);
        return sidebar;
    }

    private Node buildStatusBar() {
        toastLabel.getStyleClass().add("toast");
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);
        HBox status = new HBox(10, statusLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        VBox bottom = new VBox(6, toastLabel, status);
        bottom.getStyleClass().add("status-bar");
        return bottom;
    }

    private void sendGet() {
        String url = normalizedCurrentUrl();
        executeRequest(url, () -> httpClient.get(url));
    }

    private void sendHead() {
        String url = normalizedCurrentUrl();
        executeRequest(url, () -> httpClient.head(url));
    }

    private void sendPost() {
        try {
            String url = submitUrlFor(normalizedCurrentUrl());
            executeRequest(url, () -> httpClient.postForm(url, nameField.getText(), messageArea.getText()));
        } catch (IOException error) {
            showError(error);
        }
    }

    private void executeRequest(String url, RequestOperation request) {
        runTask(request::execute, response -> {
            addHistory(url);
            displayResponse(response);
            setConnectionState(response.isSuccessful(), response.statusLine());
        }, "Đang gửi request tới " + url);
    }

    private void downloadCurrentUrl() {
        String url = normalizedCurrentUrl();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu nội dung tải xuống");
        chooser.setInitialFileName(suggestedFileName(url));
        var destination = chooser.showSaveDialog(urlBox.getScene().getWindow());
        if (destination == null) {
            return;
        }

        runTask(() -> {
            RawHttpClient.Response response = httpClient.get(url);
            if (!response.isSuccessful()) {
                throw new IOException("Server trả về " + response.statusLine());
            }
            byte[] body = response.getBody();
            Files.write(destination.toPath(), body);
            return new DownloadResult(response, destination.toPath(), body.length);
        }, result -> {
            addHistory(url);
            displayResponse(result.response());
            setConnectionState(true, result.response().statusLine());
            showToast("Đã lưu " + readableBytes(result.bytes()) + " vào " + result.path().getFileName(), false);
        }, "Đang tải xuống " + url);
    }

    private <T> void runTask(CheckedSupplier<T> operation, Consumer<T> success, String status) {
        setBusy(true, status);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.get();
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false, "Sẵn sàng");
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            setBusy(false, "Request thất bại");
            setConnectionState(false, "Không kết nối");
            showError(task.getException());
        });
        worker.execute(task);
    }

    private void displayResponse(RawHttpClient.Response response) {
        headersArea.setText(response.headersAsText());
        headersArea.positionCaret(0);
        byte[] body = response.getBody();
        statusLabel.setText(response.statusLine() + " · " + readableBytes(body.length));

        if (body.length == 0) {
            contentArea.setText("Response không có body.\n\n"
                    + "Đây là kết quả đúng đối với HEAD hoặc status không có nội dung.");
            return;
        }

        String contentType = response.getHeader("content-type");
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("text/")) {
            contentArea.setText(new String(body, charsetFrom(contentType)));
        } else {
            contentArea.setText("Tài nguyên nhị phân\n\nContent-Type: "
                    + (contentType == null ? "không xác định" : contentType)
                    + "\nDung lượng: " + readableBytes(body.length)
                    + "\n\nDùng nút Tải xuống để lưu file.");
        }
    }

    private void addHistory(String url) {
        history.add(url);
        historyList.getItems().setAll(history.getUrls());
        urlBox.getItems().setAll(history.getUrls());
        urlBox.setValue(url);
    }

    private String normalizedCurrentUrl() {
        String url = urlBox.getEditor().getText().trim();
        if (url.isEmpty()) {
            url = "http://localhost:8080/";
        } else if (!url.contains("://")) {
            url = "http://" + url;
        }
        urlBox.getEditor().setText(url);
        return url;
    }

    private static String submitUrlFor(String baseUrl) throws IOException {
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

    private static String suggestedFileName(String url) {
        try {
            String path = new URI(url).getPath();
            int slash = path == null ? -1 : path.lastIndexOf('/');
            return path == null || path.isBlank() || slash == path.length() - 1
                    ? "download.bin" : path.substring(slash + 1);
        } catch (URISyntaxException error) {
            return "download.bin";
        }
    }

    private static Charset charsetFrom(String contentType) {
        if (contentType != null) {
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
        }
        return StandardCharsets.UTF_8;
    }

    private void setBusy(boolean busy, String status) {
        getButton.setDisable(busy);
        headButton.setDisable(busy);
        downloadButton.setDisable(busy);
        postButton.setDisable(busy);
        loadingOverlay.setManaged(busy);
        loadingOverlay.setVisible(busy);
        statusLabel.setText(status);
    }

    private void setConnectionState(boolean successful, String text) {
        connectionBadge.setText(text);
        connectionBadge.getStyleClass().removeAll("status-idle", "status-online", "status-error");
        connectionBadge.getStyleClass().add(successful ? "status-online" : "status-error");
    }

    private void showWelcome() {
        contentArea.setText("Mini Browser đã sẵn sàng\n\n"
                + "Khởi động Mini Web Server, nhập URL rồi chọn GET, HEAD hoặc POST.\n"
                + "Tab Nội dung hiển thị body HTTP; tab Status & Headers hiển thị phần giao thức.");
    }

    private void showToast(String message, boolean error) {
        toastLabel.setText(message);
        toastLabel.getStyleClass().removeAll("toast-success", "toast-error");
        toastLabel.getStyleClass().add(error ? "toast-error" : "toast-success");
        toastLabel.setOpacity(1);
        toastLabel.setManaged(true);
        toastLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        FadeTransition fade = new FadeTransition(Duration.millis(220), toastLabel);
        fade.setFromValue(1);
        fade.setToValue(0);
        SequentialTransition animation = new SequentialTransition(pause, fade);
        animation.setOnFinished(event -> {
            toastLabel.setVisible(false);
            toastLabel.setManaged(false);
        });
        animation.play();
    }

    private void showError(Throwable error) {
        String message = error == null || error.getMessage() == null ? "Lỗi không xác định" : error.getMessage();
        showToast(message, true);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("HTTP Client");
        alert.setHeaderText("Không thể thực hiện request");
        alert.show();
    }

    private static VBox labeledField(String label, Node field) {
        Label text = new Label(label);
        text.getStyleClass().add("field-label");
        return new VBox(4, text, field);
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-primary");
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-secondary");
        return button;
    }

    private static String readableBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public void stop() {
        worker.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record DownloadResult(RawHttpClient.Response response, Path path, int bytes) { }

    @FunctionalInterface
    private interface RequestOperation {
        RawHttpClient.Response execute() throws Exception;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
