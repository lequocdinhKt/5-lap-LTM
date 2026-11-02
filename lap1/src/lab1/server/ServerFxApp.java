package lab1.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lab1.common.Protocol;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Man hinh server: hien ro ba cong va giao thuc dang dung. */
public final class ServerFxApp extends Application {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ObservableList<ServerListener.ConnectedClient> clients = FXCollections.observableArrayList();
    private final TextArea log = new TextArea();
    private final Label status = new Label("Đã dừng");
    private final TextField ip = new TextField(ChatServer.primaryLanAddress());
    private final Spinner<Integer> tcp = portSpinner(Protocol.DEFAULT_TCP_PORT);
    private final Spinner<Integer> udpText = portSpinner(Protocol.DEFAULT_UDP_TEXT_PORT);
    private final Spinner<Integer> udpFile = portSpinner(Protocol.DEFAULT_UDP_FILE_PORT);
    private final Button start = new Button("Khởi động server");
    private final Button stop = new Button("Dừng server");
    private final Label clientCount = new Label("Client online: 0");
    private ChatServer server;

    @Override
    public void start(Stage stage) {
        ip.setEditable(false);
        log.setEditable(false);
        log.setWrapText(true);
        stop.setDisable(true);
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("IP server:"), ip);
        form.addRow(1, new Label("TCP điều khiển/chat riêng:"), tcp);
        form.addRow(2, new Label("UDP multicast/broadcast text:"), udpText);
        form.addRow(3, new Label("UDP unicast file:"), udpFile);
        form.addRow(4, new Label("Trạng thái:"), status);

        TableView<ServerListener.ConnectedClient> table = new TableView<>(clients);
        TableColumn<ServerListener.ConnectedClient, String> name = new TableColumn<>("Tên");
        name.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().username()));
        TableColumn<ServerListener.ConnectedClient, String> address = new TableColumn<>("IP");
        address.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().ip()));
        TableColumn<ServerListener.ConnectedClient, String> state = new TableColumn<>("Trạng thái");
        state.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().status()));
        table.getColumns().add(name);
        table.getColumns().add(address);
        table.getColumns().add(state);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        start.setOnAction(event -> startServer());
        stop.setOnAction(event -> stopServer());
        VBox top = new VBox(12, new Label("LAB 1 - CHAT CLIENT SERVER"), form,
                new HBox(10, start, stop, clientCount));
        top.setPadding(new Insets(15));
        VBox center = new VBox(8, new Label("Client đang online"), table,
                new Label("Nhật ký TCP / UDP"), log);
        center.setPadding(new Insets(0, 15, 15, 15));
        VBox.setVgrow(table, Priority.SOMETIMES);
        VBox.setVgrow(log, Priority.ALWAYS);
        BorderPane root = new BorderPane(center, top, null, null, null);
        stage.setScene(new Scene(root, 820, 680));
        stage.setTitle("Lab 1 - Chat Server");
        stage.setOnCloseRequest(event -> stopServer());
        stage.show();
        append("IP co the dung: " + String.join(", ", ChatServer.localAddresses()));
    }

    private void startServer() {
        if (server != null && server.isRunning()) return;
        server = new ChatServer(tcp.getValue(), udpText.getValue(), udpFile.getValue());
        server.setListener(new ServerListener() {
            public void onStarted(String address, int tcpValue, int textValue, int fileValue) {
                Platform.runLater(() -> {
                    ip.setText(address);
                    status.setText("Đang chạy");
                    setRunning(true);
                    append("Server " + address + " | TCP " + tcpValue
                            + " | UDP text " + textValue + " | UDP file " + fileValue);
                });
            }
            public void onStopped() { Platform.runLater(() -> setRunning(false)); }
            public void onClientsChanged(List<ConnectedClient> value) {
                Platform.runLater(() -> {
                    clients.setAll(value);
                    clientCount.setText("Client online: " + value.size());
                });
            }
            public void onLog(String message) { Platform.runLater(() -> append(message)); }
        });
        try { server.startAsync(); }
        catch (Exception exception) { append("Khong khoi dong duoc: " + exception.getMessage()); }
    }

    private void setRunning(boolean running) {
        start.setDisable(running);
        stop.setDisable(!running);
        tcp.setDisable(running);
        udpText.setDisable(running);
        udpFile.setDisable(running);
        status.setText(running ? "Đang chạy" : "Đã dừng");
        if (!running) {
            clients.clear();
            clientCount.setText("Client online: 0");
        }
    }

    private void stopServer() {
        if (server != null) server.close();
        server = null;
    }

    private void append(String message) {
        log.appendText("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
    }

    private static Spinner<Integer> portSpinner(int value) {
        Spinner<Integer> spinner = new Spinner<>(1, 65_535, value);
        spinner.setEditable(true);
        return spinner;
    }

    @Override public void stop() { stopServer(); }
    public static void main(String[] args) { launch(args); }
}
