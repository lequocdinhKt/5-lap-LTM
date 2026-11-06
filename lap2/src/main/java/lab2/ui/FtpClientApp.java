package lab2.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lab2.service.FtpService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** JavaFX FTP Client: UI gọi FtpService, service điều phối lớp giao thức mạng. */
public final class FtpClientApp extends Application {
    private final TextField hostField = new TextField("localhost");
    private final TextField portField = new TextField("2121");
    private final TextField userField = new TextField("alice");
    private final PasswordField passwordField = new PasswordField();
    private final Button connectButton = primaryButton("Kết nối");
    private final Button disconnectButton = secondaryButton("Ngắt");
    private final Label connectionBadge = new Label("Chưa kết nối");

    private final Label localPathLabel = new Label();
    private final Label remotePathLabel = new Label("/");
    private final ObservableList<Path> localItems = FXCollections.observableArrayList();
    private final ObservableList<FtpService.RemoteEntry> remoteItems = FXCollections.observableArrayList();
    private final ListView<Path> localList = new ListView<>(localItems);
    private final ListView<FtpService.RemoteEntry> remoteList = new ListView<>(remoteItems);

    private final TextArea logArea = new TextArea();
    private final Label statusLabel = new Label("Sẵn sàng");
    private final Label toastLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "ftp-client-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Path localDirectory = Path.of(System.getProperty("user.home")).toAbsolutePath();
    private final FtpService ftpService = new FtpService(this::appendLog);
    private List<Node> remoteActions = List.of();

    @Override
    public void start(Stage stage) {
        passwordField.setText("123456");
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setCenter(buildWorkspace(stage));
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/css/lab2.css").toExternalForm());
        stage.setTitle("Network Labs · FTP Client");
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> disconnect());
        stage.show();

        refreshLocal();
        setConnected(false);
    }

    private Node buildTopBar() {
        Label product = new Label("NETWORK LABS");
        product.getStyleClass().add("eyebrow");
        Label title = new Label("FTP Client");
        title.getStyleClass().add("app-title");
        VBox brand = new VBox(2, product, title);

        connectionBadge.getStyleClass().addAll("status-badge", "status-offline");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(16, brand, spacer, connectionBadge);
        heading.setAlignment(Pos.CENTER_LEFT);

        configureField(hostField, "Host", 150);
        configureField(portField, "Port", 84);
        configureField(userField, "Tài khoản", 120);
        configureField(passwordField, "Mật khẩu", 120);
        connectButton.setOnAction(event -> connect());
        disconnectButton.setOnAction(event -> disconnect());

        FlowPane form = new FlowPane(10, 8, labeledField("Host", hostField), labeledField("Port", portField),
                labeledField("Tài khoản", userField), labeledField("Mật khẩu", passwordField),
                connectButton, disconnectButton);
        form.setAlignment(Pos.BOTTOM_LEFT);

        VBox top = new VBox(14, heading, form);
        top.getStyleClass().add("top-bar");
        return top;
    }

    private Node buildWorkspace(Stage stage) {
        Button localUp = secondaryButton("↑ Lên một cấp");
        Button chooseLocal = secondaryButton("Chọn thư mục");
        Button upload = primaryButton("Tải lên →");
        localUp.setOnAction(event -> goLocalUp());
        chooseLocal.setOnAction(event -> chooseLocalDirectory(stage));
        upload.setOnAction(event -> uploadSelected());

        localList.setCellFactory(list -> new LocalFileCell());
        localList.setPlaceholder(new Label("Thư mục trống"));
        localList.setOnMouseClicked(event -> {
            Path selected = localList.getSelectionModel().getSelectedItem();
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && selected != null && Files.isDirectory(selected)) {
                localDirectory = selected;
                refreshLocal();
            }
        });
        VBox localCard = fileCard("Máy của tôi", localPathLabel, localList,
                new FlowPane(8, 8, localUp, chooseLocal, upload));

        Button remoteUp = secondaryButton("↑ Lên một cấp");
        Button refresh = secondaryButton("Làm mới");
        Button download = primaryButton("← Tải xuống");
        Button mkdir = secondaryButton("Thư mục mới");
        Button delete = dangerButton("Xóa");
        remoteActions = List.of(remoteUp, refresh, download, mkdir, delete, upload);
        remoteUp.setOnAction(event -> runRemoteAction(() -> ftpService.changeDirectory(".."), "Đang đổi thư mục"));
        refresh.setOnAction(event -> refreshRemote());
        download.setOnAction(event -> downloadSelected());
        mkdir.setOnAction(event -> createRemoteDirectory());
        delete.setOnAction(event -> deleteRemoteSelected());

        remoteList.setCellFactory(list -> new RemoteFileCell());
        remoteList.setPlaceholder(new Label("Kết nối để xem file trên server"));
        remoteList.setOnMouseClicked(event -> {
            FtpService.RemoteEntry selected = remoteList.getSelectionModel().getSelectedItem();
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && selected != null && selected.directory()) {
                runRemoteAction(() -> ftpService.changeDirectory(selected.name()), "Đang mở thư mục");
            }
        });
        VBox remoteCard = fileCard("FTP Server", remotePathLabel, remoteList,
                new FlowPane(8, 8, remoteUp, refresh, download, mkdir, delete));

        SplitPane files = new SplitPane(localCard, remoteCard);
        files.setOrientation(Orientation.HORIZONTAL);
        files.setDividerPositions(0.5);
        VBox.setVgrow(files, Priority.ALWAYS);

        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.getStyleClass().add("protocol-log");
        TitledPane logPane = new TitledPane("Nhật ký giao thức FTP", logArea);
        logPane.setExpanded(false);
        logPane.setAnimated(true);

        VBox content = new VBox(12, files, logPane);
        content.setPadding(new Insets(16));
        return content;
    }

    private Node buildStatusBar() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        toastLabel.getStyleClass().add("toast");
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);

        HBox status = new HBox(12, statusLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        VBox bar = new VBox(6, toastLabel, progressBar, status);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private VBox fileCard(String title, Label path, ListView<?> list, FlowPane actions) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        path.getStyleClass().add("path-label");
        ScrollPane pathScroll = new ScrollPane(path);
        pathScroll.setFitToHeight(true);
        pathScroll.setPannable(true);
        pathScroll.getStyleClass().add("path-scroll");
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox card = new VBox(10, heading, pathScroll, new Separator(), list, actions);
        card.getStyleClass().add("card");
        card.setMinWidth(280);
        return card;
    }

    private void connect() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException error) {
            showError(new IllegalArgumentException("Port phải là số từ 1 đến 65535."));
            return;
        }

        String host = hostField.getText().trim();
        String username = userField.getText().trim();
        String password = passwordField.getText();
        runTask(() -> {
            ftpService.connect(host, port, username, password);
            return null;
        }, ignored -> {
            setConnected(true);
            showToast("Đã kết nối FTP Server", false);
            refreshRemote();
        }, error -> {
            ftpService.close();
            setConnected(false);
        }, "Đang kết nối tới FTP Server…");
    }

    private void disconnect() {
        ftpService.close();
        remoteItems.clear();
        remotePathLabel.setText("/");
        setConnected(false);
        statusLabel.setText("Đã ngắt kết nối");
    }

    private void refreshLocal() {
        localPathLabel.setText(localDirectory.toString());
        try (var stream = Files.list(localDirectory)) {
            List<Path> files = stream.sorted(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();
            localItems.setAll(files);
        } catch (Exception error) {
            showError(error);
        }
    }

    private void refreshRemote() {
        if (!requireConnection()) {
            return;
        }
        runTask(ftpService::browse, snapshot -> {
            remotePathLabel.setText(snapshot.path());
            remoteItems.setAll(snapshot.entries());
        }, ignored -> { }, "Đang tải danh sách trên server…");
    }

    private void goLocalUp() {
        if (localDirectory.getParent() != null) {
            localDirectory = localDirectory.getParent();
            refreshLocal();
        }
    }

    private void chooseLocalDirectory(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục trên máy");
        if (Files.isDirectory(localDirectory)) {
            chooser.setInitialDirectory(localDirectory.toFile());
        }
        var selected = chooser.showDialog(stage);
        if (selected != null) {
            localDirectory = selected.toPath();
            refreshLocal();
        }
    }

    private void uploadSelected() {
        Path selected = localList.getSelectionModel().getSelectedItem();
        if (selected == null || !Files.isRegularFile(selected)) {
            showMessage("Hãy chọn một file trên máy để tải lên.");
            return;
        }
        if (!requireConnection()) {
            return;
        }
        runTask(() -> {
            ftpService.upload(selected, selected.getFileName().toString(), this::updateTransferProgress);
            return null;
        }, ignored -> {
            showToast("Tải lên thành công: " + selected.getFileName(), false);
            refreshRemote();
        }, ignored -> { }, "Đang tải lên " + selected.getFileName() + "…");
    }

    private void downloadSelected() {
        FtpService.RemoteEntry selected = remoteList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.directory()) {
            showMessage("Hãy chọn một file trên server để tải xuống.");
            return;
        }
        Path target = localDirectory.resolve(selected.name());
        if (Files.exists(target) && !confirm("Ghi đè file", "File " + selected.name() + " đã tồn tại. Ghi đè?")) {
            return;
        }
        runTask(() -> {
            ftpService.download(selected.name(), target, selected.size(), this::updateTransferProgress);
            return null;
        }, ignored -> {
            refreshLocal();
            showToast("Đã lưu: " + target, false);
        }, ignored -> { }, "Đang tải xuống " + selected.name() + "…");
    }

    private void createRemoteDirectory() {
        if (!requireConnection()) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thư mục mới");
        dialog.setHeaderText("Tạo thư mục trên FTP Server");
        dialog.setContentText("Tên thư mục:");
        dialog.showAndWait().map(String::trim).filter(name -> !name.isBlank())
                .ifPresent(name -> runRemoteAction(() -> ftpService.createDirectory(name), "Đang tạo thư mục…"));
    }

    private void deleteRemoteSelected() {
        FtpService.RemoteEntry selected = remoteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Hãy chọn file hoặc thư mục trên server.");
            return;
        }
        if (confirm("Xác nhận xóa", "Xóa " + selected.name() + "?")) {
            runRemoteAction(() -> ftpService.delete(selected.name(), selected.directory()), "Đang xóa…");
        }
    }

    private void runRemoteAction(CheckedAction action, String status) {
        if (!requireConnection()) {
            return;
        }
        runTask(() -> {
            action.run();
            return null;
        }, ignored -> refreshRemote(), ignored -> { }, status);
    }

    private <T> void runTask(
            CheckedSupplier<T> operation,
            Consumer<T> success,
            Consumer<Throwable> failure,
            String status) {
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
            setBusy(false, "Thao tác thất bại");
            Throwable error = task.getException();
            failure.accept(error);
            showError(error);
        });
        workers.execute(task);
    }

    private void updateTransferProgress(long transferred, long total) {
        Platform.runLater(() -> {
            progressBar.setProgress(total > 0 ? Math.min(1.0, (double) transferred / total) : -1);
            statusLabel.setText("Đã truyền " + readableBytes(transferred)
                    + (total > 0 ? " / " + readableBytes(total) : ""));
        });
    }

    private void setBusy(boolean busy, String status) {
        statusLabel.setText(status);
        progressBar.setManaged(busy);
        progressBar.setVisible(busy);
        progressBar.setProgress(busy ? -1 : 0);
        boolean connected = ftpService.isConnected();
        connectButton.setDisable(busy || connected);
        disconnectButton.setDisable(busy || !connected);
        remoteActions.forEach(node -> node.setDisable(busy || !connected));
    }

    private void setConnected(boolean connected) {
        hostField.setDisable(connected);
        portField.setDisable(connected);
        userField.setDisable(connected);
        passwordField.setDisable(connected);
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        remoteActions.forEach(node -> node.setDisable(!connected));
        connectionBadge.setText(connected ? "Đã kết nối" : "Chưa kết nối");
        connectionBadge.getStyleClass().removeAll("status-online", "status-offline");
        connectionBadge.getStyleClass().add(connected ? "status-online" : "status-offline");
    }

    private boolean requireConnection() {
        if (!ftpService.isConnected()) {
            showMessage("Chưa kết nối FTP Server.");
            return false;
        }
        return true;
    }

    private void appendLog(String line) {
        Platform.runLater(() -> {
            logArea.appendText(line + System.lineSeparator());
            logArea.positionCaret(logArea.getLength());
        });
    }

    private void showToast(String text, boolean error) {
        toastLabel.setText(text);
        toastLabel.getStyleClass().removeAll("toast-success", "toast-error");
        toastLabel.getStyleClass().add(error ? "toast-error" : "toast-success");
        toastLabel.setOpacity(1);
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.4));
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
        appendLog("LỖI: " + message);
        showToast(message, true);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Lỗi FTP");
        alert.setHeaderText("Không thể thực hiện thao tác");
        alert.show();
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("FTP Client");
        alert.setHeaderText(null);
        alert.show();
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static VBox labeledField(String label, Node field) {
        Label text = new Label(label);
        text.getStyleClass().add("field-label");
        return new VBox(4, text, field);
    }

    private static void configureField(TextField field, String prompt, double width) {
        field.setPromptText(prompt);
        field.setPrefWidth(width);
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

    private static Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-danger");
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
        disconnect();
        workers.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static final class LocalFileCell extends ListCell<Path> {
        @Override
        protected void updateItem(Path path, boolean empty) {
            super.updateItem(path, empty);
            setText(empty || path == null ? null
                    : (Files.isDirectory(path) ? "📁  " : "📄  ") + path.getFileName());
        }
    }

    private static final class RemoteFileCell extends ListCell<FtpService.RemoteEntry> {
        @Override
        protected void updateItem(FtpService.RemoteEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            setText(empty || entry == null ? null
                    : (entry.directory() ? "📁  " + entry.name()
                    : "📄  " + entry.name() + "   ·   " + readableBytes(entry.size())));
        }
    }
}
