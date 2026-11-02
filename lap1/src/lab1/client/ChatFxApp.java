package lab1.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lab1.common.GroupInfo;
import lab1.common.Protocol;
import lab1.common.RecipientMode;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Giao dien chat; moi thao tac mang deu di qua ClientConnection. */
public final class ChatFxApp extends Application implements ClientConnection.Listener {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String[] EMOJIS = {"😀", "😂", "😍", "👍", "❤️", "🎉", "😢", "😮"};

    private Stage stage;
    private ClientConnection connection;
    private final ListView<String> users = new ListView<>();
    private final ListView<GroupInfo> groups = new ListView<>();
    private final VBox messages = new VBox(7);
    private final Map<String, List<MessageView>> conversations = new HashMap<>();
    private final Map<String, GroupInfo> groupById = new HashMap<>();
    private final TextField messageField = new TextField();
    private final Label conversationTitle = new Label("Chọn một cuộc trò chuyện");
    private final Label status = new Label("Sẵn sàng");
    private final Button sendButton = new Button("Gửi");
    private final Button attachButton = new Button("Đính kèm");
    private final Button emojiButton = new Button("😊");
    private final Button membersButton = new Button("Sửa thành viên");
    private final Button leaveButton = new Button("Rời nhóm");
    private final Button deleteButton = new Button("Xóa nhóm");
    private String activeKey;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        List<String> args = getParameters().getRaw();
        String host = args.size() > 0 ? args.get(0) : "localhost";
        int port = parsePort(args.size() > 1 ? args.get(1) : null);
        String username = args.size() > 2 ? args.get(2) : "";
        String password = args.size() > 3 ? args.get(3) : "";
        boolean auto = args.size() > 4 && "--connect".equalsIgnoreCase(args.get(4));
        showLogin(host, port, username, password);
        if (auto && !username.isBlank() && !password.isBlank()) {
            Platform.runLater(() -> connect(host, port, username, password, false));
        }
    }

    private void showLogin(String host, int port, String username, String password) {
        TextField name = new TextField(username);
        PasswordField pass = new PasswordField();
        pass.setText(password);
        TextField server = new TextField(host);
        Spinner<Integer> tcp = new Spinner<>(1, 65_535, port);
        Button login = new Button("Đăng nhập");
        Button register = new Button("Đăng ký");
        Label note = new Label("TCP: điều khiển/chat riêng | UDP: nhóm, tất cả và file");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.addRow(0, new Label("Tên người dùng:"), name);
        form.addRow(1, new Label("Mật khẩu:"), pass);
        form.addRow(2, new Label("IP server:"), server);
        form.addRow(3, new Label("Cổng TCP:"), tcp);
        ColumnConstraints grow = new ColumnConstraints();
        grow.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(new ColumnConstraints(), grow);
        VBox root = new VBox(18, new Label("CHAT CLIENT - SERVER"), form,
                new HBox(10, register, login), note);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        login.setDefaultButton(true);
        login.setOnAction(event -> connect(server.getText().trim(), tcp.getValue(),
                name.getText().trim(), pass.getText(), false));
        register.setOnAction(event -> connect(server.getText().trim(), tcp.getValue(),
                name.getText().trim(), pass.getText(), true));
        stage.setScene(new Scene(root, 540, 410));
        stage.setTitle("Lab 1 - Đăng nhập");
        stage.show();
    }

    private void connect(String host, int port, String username, String password, boolean register) {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            showError("Vui lòng nhập đủ thông tin.");
            return;
        }
        status.setText("Đang kết nối TCP...");
        CompletableFuture.supplyAsync(() -> {
            try { return ClientConnection.connect(host, port, username, password, register); }
            catch (IOException exception) { throw new RuntimeException(exception); }
        }).whenComplete((connected, error) -> Platform.runLater(() -> {
            if (error != null) { showError(rootMessage(error)); return; }
            connection = connected;
            showChat();
            connection.startListening(this);
        }));
    }

    private void showChat() {
        users.setPlaceholder(new Label("Chưa có người khác online"));
        users.setOnMouseClicked(event -> {
            String selected = users.getSelectionModel().getSelectedItem();
            if (selected != null) {
                groups.getSelectionModel().clearSelection();
                selectConversation("user:" + selected, "💬 " + selected);
            }
        });
        groups.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(GroupInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : groupLabel(item));
            }
        });
        groups.setOnMouseClicked(event -> {
            GroupInfo selected = groups.getSelectionModel().getSelectedItem();
            if (selected != null) {
                users.getSelectionModel().clearSelection();
                selectConversation("group:" + selected.id(),
                        (selected.systemGroup() ? "📢 " : "👥 ") + selected.name());
            }
        });
        Button createGroup = new Button("+ Tạo nhóm");
        createGroup.setMaxWidth(Double.MAX_VALUE);
        createGroup.setOnAction(event -> showCreateGroup());
        VBox sidebar = new VBox(7, new Label("NGƯỜI ĐANG ONLINE"), users,
                new Label("NHÓM UDP"), groups, createGroup);
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(280);
        VBox.setVgrow(users, Priority.SOMETIMES);
        VBox.setVgrow(groups, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, conversationTitle, spacer, membersButton, leaveButton, deleteButton);
        header.setAlignment(Pos.CENTER_LEFT);
        membersButton.setOnAction(event -> withGroup(group ->
                runIo(() -> connection.requestGroupMembers(group.id()), "Đã tải thành viên.")));
        leaveButton.setOnAction(event -> withGroup(group ->
                runIo(() -> connection.leaveGroup(group.id()), "Đã rời nhóm.")));
        deleteButton.setOnAction(event -> withGroup(this::confirmDelete));

        messages.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        messages.heightProperty().addListener((value, oldHeight, newHeight) -> scroll.setVvalue(1));
        messageField.setPromptText("Nhập tin nhắn...");
        messageField.setOnAction(event -> sendCurrentText());
        sendButton.setOnAction(event -> sendCurrentText());
        attachButton.setOnAction(event -> sendCurrentFile());
        emojiButton.setOnAction(event -> showEmojiMenu());
        HBox composer = new HBox(7, emojiButton, attachButton, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        VBox center = new VBox(8, header, scroll, composer, status);
        center.setPadding(new Insets(12));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        stage.setScene(new Scene(new BorderPane(center, null, null, null, sidebar), 1080, 710));
        stage.setTitle("Lab 1 Chat - " + connection.username());
        stage.setOnCloseRequest(event -> connection.close());
        updateControls();
    }

    private String groupLabel(GroupInfo group) {
        String role = group.membership() == GroupInfo.Membership.OWNER ? " • Trưởng nhóm" : "";
        String network = group.systemGroup() ? "UDP broadcast" : group.multicastAddress();
        return (group.systemGroup() ? "📢 " : "👥 ") + group.name()
                + " (" + group.memberCount() + ") " + network + role;
    }

    private void showCreateGroup() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo nhóm multicast");
        TextField name = new TextField();
        name.setPromptText("Tên nhóm");
        VBox memberBox = memberCheckboxes(List.of());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Tên nhóm:"), name,
                new Label("Chọn thành viên đang online:"), memberBox));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            String value = name.getText().trim();
            if (!value.isEmpty()) runIo(() -> connection.createGroup(value, selectedMembers(memberBox)),
                    "Đã tạo nhóm multicast.");
        });
    }

    private VBox memberCheckboxes(List<String> selected) {
        LinkedHashSet<String> names = new LinkedHashSet<>(selected);
        names.addAll(users.getItems());
        VBox box = new VBox(6);
        for (String name : names) {
            if (name.equals(connection.username())) continue;
            CheckBox check = new CheckBox(name);
            check.setSelected(selected.contains(name));
            box.getChildren().add(check);
        }
        return box;
    }

    private List<String> selectedMembers(VBox box) {
        return box.getChildren().stream().filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast).filter(CheckBox::isSelected)
                .map(CheckBox::getText).toList();
    }

    private void showMemberEditor(String groupId, List<String> current) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sửa thành viên nhóm");
        VBox checks = memberCheckboxes(current);
        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Chọn người được nhận multicast:"), checks));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button ->
                runIo(() -> connection.updateGroupMembers(groupId, selectedMembers(checks)),
                        "Đã cập nhật thành viên."));
    }

    private void confirmDelete(GroupInfo group) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa nhóm '" + group.name() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            runIo(() -> connection.deleteGroup(group.id()), "Đã xóa nhóm.");
        }
    }

    private void selectConversation(String key, String title) {
        activeKey = key;
        conversationTitle.setText(title);
        conversations.computeIfAbsent(key, ignored -> new ArrayList<>());
        renderConversation();
        updateControls();
        messageField.requestFocus();
    }

    private void sendCurrentText() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || activeKey == null) return;
        messageField.clear();
        if (activeKey.startsWith("user:")) {
            runIo(() -> connection.sendText(RecipientMode.DIRECT,
                    List.of(activeKey.substring(5)), text), "Đã gửi bằng TCP.");
        } else {
            runIo(() -> connection.sendGroupText(activeKey.substring(6), text),
                    "Đã phát UDP (best effort, không ACK).");
        }
    }

    private void sendCurrentFile() {
        if (activeKey == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn file, audio hoặc video tối đa 20 MB");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tat ca tep", "*.*"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a", "*.ogg"),
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm"));
        java.io.File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;
        Path file = selected.toPath();
        String key = activeKey;
        IoAction action = key.startsWith("user:")
                ? () -> connection.sendFile(RecipientMode.DIRECT, List.of(key.substring(5)), file)
                : () -> connection.sendGroupFile(key.substring(6), file);
        runIo(action, "Đã gửi " + AttachmentKind.of(file).display + " bằng UDP có ACK.", () ->
                append(key, new MessageView(connection.username(), System.currentTimeMillis(),
                        AttachmentKind.of(file).icon + " " + file.getFileName(), file,
                        AttachmentKind.of(file))));
    }

    private void showEmojiMenu() {
        ContextMenu menu = new ContextMenu();
        for (String emoji : EMOJIS) {
            MenuItem item = new MenuItem(emoji);
            item.setOnAction(event -> messageField.appendText(emoji));
            menu.getItems().add(item);
        }
        menu.show(emojiButton, javafx.geometry.Side.TOP, 0, 0);
    }

    private void renderConversation() {
        messages.getChildren().clear();
        conversations.getOrDefault(activeKey, List.of()).forEach(this::addBubble);
    }

    private void addBubble(MessageView message) {
        boolean mine = connection.username().equals(message.sender());
        Label head = new Label((mine ? "Bạn" : message.sender()) + " • "
                + TIME.format(Instant.ofEpochMilli(message.time())));
        Label body = new Label(message.text());
        body.setWrapText(true);
        VBox bubble = new VBox(3, head, body);
        bubble.setPadding(new Insets(7, 10, 7, 10));
        bubble.setMaxWidth(550);
        bubble.setStyle(mine ? "-fx-background-color:#d9edff;-fx-background-radius:10;"
                : "-fx-background-color:white;-fx-border-color:#d5d9dd;-fx-background-radius:10;");
        if (message.file() != null) {
            String label = message.kind() == null ? "Mở tệp" : "Mở " + message.kind().display;
            Hyperlink open = new Hyperlink(label);
            open.setOnAction(event -> openFile(message.file()));
            bubble.getChildren().add(open);
        }
        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messages.getChildren().add(row);
    }

    private void openFile(Path file) {
        try {
            if (!Desktop.isDesktopSupported()) throw new IOException("He dieu han khong ho tro mo tep.");
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException exception) { showError(exception.getMessage()); }
    }

    private void append(String key, MessageView message) {
        conversations.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
        if (key.equals(activeKey)) addBubble(message);
    }

    private void updateControls() {
        boolean direct = activeKey != null && activeKey.startsWith("user:");
        GroupInfo group = selectedGroup();
        boolean canChat = direct || group != null;
        messageField.setDisable(!canChat);
        sendButton.setDisable(!canChat);
        attachButton.setDisable(!canChat);
        emojiButton.setDisable(!canChat);
        setShown(membersButton, group != null && !group.systemGroup()
                && group.membership() == GroupInfo.Membership.OWNER);
        setShown(deleteButton, group != null && !group.systemGroup()
                && group.membership() == GroupInfo.Membership.OWNER);
        setShown(leaveButton, group != null && !group.systemGroup()
                && group.membership() == GroupInfo.Membership.MEMBER);
    }

    private void setShown(Control control, boolean shown) {
        control.setVisible(shown);
        control.setManaged(shown);
    }

    private GroupInfo selectedGroup() {
        return activeKey != null && activeKey.startsWith("group:")
                ? groupById.get(activeKey.substring(6)) : null;
    }

    private void withGroup(java.util.function.Consumer<GroupInfo> action) {
        GroupInfo group = selectedGroup();
        if (group != null) action.accept(group);
    }

    private void runIo(IoAction action, String success) { runIo(action, success, () -> { }); }

    private void runIo(IoAction action, String success, Runnable onSuccess) {
        status.setText("Đang xử lý...");
        CompletableFuture.runAsync(() -> {
            try { action.run(); } catch (IOException exception) { throw new RuntimeException(exception); }
        }).whenComplete((unused, error) -> Platform.runLater(() -> {
            status.setText(error == null ? success : "Lỗi");
            if (error == null) onSuccess.run(); else showError(rootMessage(error));
        }));
    }

    @Override public void onUserList(List<String> names) {
        Platform.runLater(() -> users.setItems(FXCollections.observableArrayList(
                names.stream().filter(name -> !name.equals(connection.username())).toList())));
    }
    @Override public void onSystemMessage(String message) { Platform.runLater(() -> status.setText(message)); }
    @Override public void onError(String message) { Platform.runLater(() -> { status.setText(message); showError(message); }); }

    @Override
    public void onText(String sender, RecipientMode mode, List<String> targets, long time, String text) {
        Platform.runLater(() -> append("user:" + directPartner(sender, targets),
                new MessageView(sender, time, text, null, null)));
    }

    @Override
    public void onFile(String sender, RecipientMode mode, List<String> targets,
                       long time, String originalName, Path savedPath) {
        AttachmentKind kind = AttachmentKind.of(savedPath);
        Platform.runLater(() -> append("user:" + directPartner(sender, targets),
                new MessageView(sender, time, kind.icon + " " + originalName, savedPath, kind)));
    }

    private String directPartner(String sender, List<String> targets) {
        return sender.equals(connection.username()) ? targets.get(0) : sender;
    }

    @Override
    public void onGroupCatalog(List<GroupInfo> catalog) {
        Platform.runLater(() -> {
            Set<String> current = new HashSet<>();
            catalog.forEach(group -> current.add(group.id()));
            groupById.clear();
            catalog.forEach(group -> groupById.put(group.id(), group));
            groups.setItems(FXCollections.observableArrayList(catalog));
            if (activeKey != null && activeKey.startsWith("group:")
                    && !current.contains(activeKey.substring(6))) {
                activeKey = null;
                conversationTitle.setText("Bạn không còn trong nhóm");
                renderConversation();
            }
            updateControls();
        });
    }

    @Override public void onGroupMembers(String groupId, List<String> members) {
        Platform.runLater(() -> showMemberEditor(groupId, members));
    }

    @Override public void onGroupRemoved(String groupId) {
        Platform.runLater(() -> {
            conversations.remove("group:" + groupId);
            if (Objects.equals(activeKey, "group:" + groupId)) {
                activeKey = null;
                conversationTitle.setText("Nhóm đã đóng");
                renderConversation();
                updateControls();
            }
        });
    }

    @Override public void onGroupText(String groupId, String sender, long time, String text) {
        Platform.runLater(() -> append("group:" + groupId,
                new MessageView(sender, time, text, null, null)));
    }

    @Override public void onGroupFile(String groupId, String sender, long time,
                                      String originalName, Path savedPath) {
        AttachmentKind kind = AttachmentKind.of(savedPath);
        Platform.runLater(() -> append("group:" + groupId,
                new MessageView(sender, time, kind.icon + " " + originalName, savedPath, kind)));
    }

    @Override public void onDisconnected(String reason) {
        Platform.runLater(() -> { status.setText(reason); messageField.setDisable(true); sendButton.setDisable(true); });
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }
    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message == null ? "Có lỗi xảy ra." : message).showAndWait();
    }
    @Override public void stop() { if (connection != null) connection.close(); }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65_535 ? port : Protocol.DEFAULT_TCP_PORT;
        } catch (Exception ignored) { return Protocol.DEFAULT_TCP_PORT; }
    }

    private record MessageView(String sender, long time, String text, Path file, AttachmentKind kind) { }
    private enum AttachmentKind {
        FILE("tệp", "📎"), AUDIO("audio", "🎵"), VIDEO("video", "🎬");
        final String display;
        final String icon;
        AttachmentKind(String display, String icon) { this.display = display; this.icon = icon; }
        static AttachmentKind of(Path file) {
            try {
                String mime = Files.probeContentType(file);
                if (mime != null && mime.startsWith("audio/")) return AUDIO;
                if (mime != null && mime.startsWith("video/")) return VIDEO;
            } catch (IOException ignored) { }
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.matches(".*\\.(mp3|wav|m4a|ogg|flac)$")) return AUDIO;
            if (name.matches(".*\\.(mp4|avi|mkv|mov|webm|mpeg)$")) return VIDEO;
            return FILE;
        }
    }
    @FunctionalInterface private interface IoAction { void run() throws IOException; }
}
