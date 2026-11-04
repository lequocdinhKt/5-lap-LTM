package lab5.ui.server;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lab5.config.NetworkAddress;
import lab5.config.ServerConfig;
import lab5.model.SyncActivity;
import lab5.service.ServerRuntimeService;
import lab5.service.ServerSettingsService;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ServerDashboardView implements AutoCloseable {
    private final BorderPane root = new BorderPane();
    private final ServerConfig baseConfig;
    private final String configPath;
    private final ServerRuntimeService runtime = new ServerRuntimeService();
    private final ExecutorService actions = Executors.newSingleThreadExecutor(task -> daemon(task, "lab5-server-action"));
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(
            task -> daemon(task, "lab5-server-monitor"));
    private final Label databaseBadge = badge("Chưa khởi động", "badge-danger");
    private final Label rmiBadge = badge("Chưa khởi động", "badge-danger");
    private final Label peerBadge = badge("Chưa kiểm tra", "badge-danger");
    private final Label lockBadge = badge("Chưa kiểm tra", "badge-danger");
    private final Label productCount = new Label("0");
    private final ListView<String> timeline = new ListView<>();
    private final TextField localHost;
    private final TextField peerHost;
    private final TextField registryPort;
    private final Button start = primary("Khởi động server");
    private final Button stop = danger("Dừng server");

    public ServerDashboardView(ServerConfig baseConfig, String configPath) {
        this.baseConfig = baseConfig;
        this.configPath = configPath;
        localHost = field(baseConfig.serverHost(), "IPv4 máy hiện tại");
        peerHost = field(baseConfig.peerHost(), "IPv4 server còn lại");
        registryPort = field(String.valueOf(baseConfig.registryPort()), "Cổng Registry");
        build();
        monitor.scheduleWithFixedDelay(this::refresh, 1, 2, TimeUnit.SECONDS);
    }

    public Parent root() {
        return root;
    }

    private void build() {
        root.getStyleClass().add("app-root");
        Label lab = new Label("Lab 5");
        lab.getStyleClass().add("sidebar-title");
        Label overview = new Label("Tổng quan server");
        overview.getStyleClass().add("nav-active");
        Label sync = new Label("Timeline đồng bộ");
        sync.getStyleClass().add("nav-item");
        Region spacer = new Region();
        VBox sidebar = new VBox(12, lab, overview, sync, spacer,
                new Label("RMI " + baseConfig.registryPort() + " · " + baseConfig.servicePort() + "–" + (baseConfig.servicePort() + 2)));
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");

        Label title = new Label("RMI Server " + baseConfig.serverId());
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Hai database độc lập · đồng bộ trực tiếp");
        subtitle.getStyleClass().add("muted");
        Region topSpacer = new Region();
        HBox topbar = new HBox(16, title, subtitle, topSpacer, rmiBadge);
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");

        HBox cards = new HBox(12,
                statusCard("Cơ sở dữ liệu", databaseBadge),
                statusCard("RMI local", rmiBadge),
                statusCard("Server còn lại", peerBadge),
                statusCard("Lock coordinator", lockBadge),
                metricCard("Sản phẩm", productCount));
        cards.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox connection = new VBox(10, section("Cấu hình kết nối"), localHost, peerHost, registryPort,
                new HBox(8, start, stop));
        connection.getStyleClass().add("card");
        stop.setDisable(true);
        start.setOnAction(event -> startServer());
        stop.setOnAction(event -> stopServer());

        timeline.setPlaceholder(new Label("Chưa có hoạt động đồng bộ."));
        VBox activity = new VBox(10, section("Timeline đồng bộ"), timeline);
        VBox.setVgrow(timeline, Priority.ALWAYS);
        activity.getStyleClass().add("card");
        SplitPane split = new SplitPane(connection, activity);
        split.setDividerPositions(0.32);
        VBox content = new VBox(16, cards, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        content.setPadding(new Insets(20));
        root.setLeft(sidebar);
        root.setTop(topbar);
        root.setCenter(content);
        FadeTransition fade = new FadeTransition(Duration.millis(190), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void startServer() {
        String local = localHost.getText().trim();
        String peer = peerHost.getText().trim();
        int port;
        try {
            if (!NetworkAddress.isIpv4(local) || !NetworkAddress.isIpv4(peer))
                throw new IllegalArgumentException("Nhập IPv4 hợp lệ cho cả hai server.");
            port = Integer.parseInt(registryPort.getText().trim());
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Cổng Registry không hợp lệ.");
        } catch (Exception error) {
            alert(error.getMessage());
            return;
        }
        setControls(false);
        setBadge(rmiBadge, "Đang khởi động", "badge-warning");
        ServerConfig selected = baseConfig.withNetwork(local, peer, port);
        String coordinator = "server-a".equalsIgnoreCase(selected.serverId()) ? local : peer;
        actions.submit(() -> {
            try {
                ServerRuntimeService.RuntimeStatus status = runtime.start(selected);
                ServerSettingsService.saveNetwork(Path.of(configPath), local, peer, coordinator);
                Platform.runLater(() -> {
                    apply(status);
                    start.setDisable(true);
                    stop.setDisable(false);
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    setControls(true);
                    setBadge(rmiBadge, "Khởi động lỗi", "badge-danger");
                    alert(error.getMessage());
                });
            }
        });
    }

    private void stopServer() {
        stop.setDisable(true);
        actions.submit(() -> {
            runtime.stop();
            Platform.runLater(() -> {
                setControls(true);
                setBadge(databaseBadge, "Đã dừng", "badge-danger");
                setBadge(rmiBadge, "Đã dừng", "badge-danger");
                setBadge(peerBadge, "Đã dừng", "badge-danger");
                setBadge(lockBadge, "Đã dừng", "badge-danger");
                productCount.setText("0");
            });
        });
    }

    private void refresh() {
        ServerRuntimeService.RuntimeStatus status = runtime.status();
        var activities = runtime.activities();
        Platform.runLater(() -> {
            apply(status);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
            timeline.setItems(FXCollections.observableArrayList(activities.stream()
                    .map(item -> line(formatter, item)).toList()));
        });
    }

    private void apply(ServerRuntimeService.RuntimeStatus status) {
        setBadge(databaseBadge, status.databaseAvailable() ? "Đã kết nối" : "Chưa kết nối",
                status.databaseAvailable() ? "badge-success" : "badge-danger");
        setBadge(rmiBadge, status.running() ? "Đang chạy" : "Chưa khởi động",
                status.running() ? "badge-success" : "badge-danger");
        setBadge(peerBadge, status.peerAvailable() ? "Đã kết nối" : "Mất kết nối",
                status.peerAvailable() ? "badge-success" : "badge-danger");
        setBadge(lockBadge, status.lockCoordinatorAvailable() ? "Sẵn sàng" : "Mất kết nối",
                status.lockCoordinatorAvailable() ? "badge-success" : "badge-danger");
        productCount.setText(String.valueOf(status.productCount()));
    }

    private void setControls(boolean enabled) {
        localHost.setDisable(!enabled);
        peerHost.setDisable(!enabled);
        registryPort.setDisable(!enabled);
        start.setDisable(!enabled);
        stop.setDisable(enabled);
    }

    private static String line(DateTimeFormatter formatter, SyncActivity item) {
        return formatter.format(item.time()) + "  " + (item.success() ? "✓" : "!") + "  "
                + item.source() + " · " + item.action() + " · " + item.detail();
    }

    private static VBox statusCard(String title, Label value) {
        VBox box = new VBox(8, muted(title), value);
        box.getStyleClass().add("status-card");
        return box;
    }

    private static VBox metricCard(String title, Label value) {
        value.getStyleClass().add("metric");
        VBox box = new VBox(8, muted(title), value);
        box.getStyleClass().add("status-card");
        return box;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private static TextField field(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        return field;
    }

    private static Button primary(String text) { return button(text, "button-primary"); }
    private static Button danger(String text) { return button(text, "button-danger"); }
    private static Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        return button;
    }

    private static Label badge(String text, String style) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", style);
        return badge;
    }

    private static void setBadge(Label badge, String text, String style) {
        badge.setText(text);
        badge.getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger");
        badge.getStyleClass().add(style);
    }

    private static void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Không thể thực hiện." : message, ButtonType.OK);
        alert.setHeaderText("Không thể khởi động server");
        alert.showAndWait();
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void close() {
        monitor.shutdownNow();
        actions.submit(runtime::close);
        actions.shutdown();
    }
}
