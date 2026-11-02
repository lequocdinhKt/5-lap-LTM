package lab1.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private final Label toast = new Label();
    private PauseTransition toastTimer;
    private final ProgressIndicator activity = new ProgressIndicator();
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
        // Script demo: "--connect" tự đăng nhập; "--register" đăng ký trước khi vào.
        boolean auto = hasFlag(args, "--connect");
        boolean register = hasFlag(args, "--register");
        showLogin(host, port, username, password);
        if (auto && !username.isBlank()) {
            if (password.isBlank()) {
                // Không truyền mật khẩu: dùng tài khoản demo quy ước "lab1-<tên>", tự đăng ký nếu chưa có.
                Platform.runLater(() -> connectDemoAccount(host, port, username));
            } else {
                Platform.runLater(() -> connect(host, port, username, password, register));
            }
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
        login.getStyleClass().add("primary-button");
        register.getStyleClass().add("secondary-button");
        Label eyebrow = new Label("LAB 1 · LẬP TRÌNH MẠNG");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Kết nối Chat");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Đăng nhập để chat TCP, multicast và broadcast UDP.");
        subtitle.getStyleClass().add("muted-label");
        Label note = new Label("TCP: điều khiển/chat riêng  •  UDP: nhóm, tất cả và file");
        note.getStyleClass().add("hint-box");
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
        HBox actions = new HBox(10, register, login, activity);
        actions.setAlignment(Pos.CENTER_LEFT);
        activity.setPrefSize(22, 22);
        activity.setVisible(false);
        activity.setManaged(false);
        status.getStyleClass().setAll("status-badge", "status-neutral");
        VBox card = new VBox(16, eyebrow, title, subtitle, form, actions, note, status);
        card.getStyleClass().add("login-card");
        VBox root = new VBox(card);
        root.getStyleClass().add("app-background");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        login.setDefaultButton(true);
        login.setOnAction(event -> connect(server.getText().trim(), tcp.getValue(),
                name.getText().trim(), pass.getText(), false));
        register.setOnAction(event -> connect(server.getText().trim(), tcp.getValue(),
                name.getText().trim(), pass.getText(), true));
        Scene scene = new Scene(root, 620, 560);
        applyStyles(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.setTitle("Lab 1 - Đăng nhập");
        stage.show();
    }

    private void connect(String host, int port, String username, String password, boolean register) {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            showError("Vui lòng nhập đủ thông tin.");
            return;
        }
        connectAsync(() -> ClientConnection.connect(host, port, username, password, register));
    }

    /** Tài khoản demo cho script: mật khẩu quy ước "lab1-<tên>", tự đăng ký nếu chưa tồn tại. */
    private void connectDemoAccount(String host, int port, String username) {
        connectAsync(() -> ClientConnection.connect(host, port, username));
    }

    private void connectAsync(IoSupplier<ClientConnection> attempt) {
        status.setText("Đang kết nối TCP...");
        setBusy(true);
        CompletableFuture.supplyAsync(() -> {
            try { return attempt.get(); }
            catch (IOException exception) { throw new RuntimeException(exception); }
        }).whenComplete((connected, error) -> Platform.runLater(() -> {
            setBusy(false);
            if (error != null) { setStatus("Kết nối thất bại", "status-error"); showError(rootMessage(error)); return; }
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
        createGroup.getStyleClass().add("secondary-button");
        createGroup.setMaxWidth(Double.MAX_VALUE);
        createGroup.setOnAction(event -> showCreateGroup());
        Label brand = new Label("Network Chat");
        brand.getStyleClass().add("sidebar-brand");
        Label onlineTitle = new Label("NGƯỜI ĐANG ONLINE");
        Label groupTitle = new Label("NHÓM UDP");
        onlineTitle.getStyleClass().add("section-label");
        groupTitle.getStyleClass().add("section-label");
        VBox sidebar = new VBox(10, brand, onlineTitle, users, groupTitle, groups, createGroup);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(210);
        VBox.setVgrow(users, Priority.SOMETIMES);
        VBox.setVgrow(groups, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, conversationTitle, spacer, membersButton, leaveButton, deleteButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("topbar");
        conversationTitle.getStyleClass().add("conversation-title");
        deleteButton.getStyleClass().add("danger-button");
        membersButton.setOnAction(event -> withGroup(group ->
                runIo(() -> connection.requestGroupMembers(group.id()), "Đã tải thành viên.")));
        leaveButton.setOnAction(event -> withGroup(group ->
                runIo(() -> connection.leaveGroup(group.id()), "Đã rời nhóm.")));
        deleteButton.setOnAction(event -> withGroup(this::confirmDelete));

        messages.setPadding(new Insets(10));
        messages.getStyleClass().add("message-list");
        ScrollPane scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("message-scroll");
        messages.heightProperty().addListener((value, oldHeight, newHeight) -> scroll.setVvalue(1));
        messageField.setPromptText("Nhập tin nhắn...");
        messageField.setOnAction(event -> sendCurrentText());
        sendButton.setOnAction(event -> sendCurrentText());
        sendButton.getStyleClass().add("primary-button");
        attachButton.setOnAction(event -> sendCurrentFile());
        emojiButton.setOnAction(event -> showEmojiMenu());
        HBox composer = new HBox(7, emojiButton, attachButton, messageField, sendButton);
        composer.getStyleClass().add("composer");
        composer.setAlignment(Pos.CENTER);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        status.getStyleClass().setAll("status-badge", "status-success");
        activity.setPrefSize(18, 18);
        HBox statusBar = new HBox(8, activity, status);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        VBox center = new VBox(12, header, scroll, composer, statusBar);
        center.getStyleClass().add("content-pane");
        center.setPadding(new Insets(16));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        BorderPane shell = new BorderPane(center, null, null, null, sidebar);
        shell.getStyleClass().add("app-shell");
        toast.getStyleClass().add("toast");
        toast.setVisible(false);
        toast.setManaged(false);
        toast.setOnMouseClicked(event -> hideToast());
        StackPane root = new StackPane(shell, toast);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 20, 22, 20));
        Scene scene = new Scene(root, 1080, 710);
        applyStyles(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setScene(scene);
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
        bubble.setMaxWidth(550);
        bubble.getStyleClass().addAll("message-bubble", mine ? "message-mine" : "message-other");
        head.getStyleClass().add("message-meta");
        if (message.file() != null) {
            String label = message.kind() == null ? "Mở tệp" : "Mở " + message.kind().display;
            Hyperlink open = new Hyperlink(label);
            open.setOnAction(event -> openFile(message.file()));
            bubble.getChildren().add(open);
        }
        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messages.getChildren().add(row);
        FadeTransition fade = new FadeTransition(Duration.millis(180), row);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), row);
        slide.setFromY(6);
        slide.setToY(0);
        fade.play();
        slide.play();
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
        setBusy(true);
        setStatus("Đang xử lý...", "status-warning");
        CompletableFuture.runAsync(() -> {
            try { action.run(); } catch (IOException exception) { throw new RuntimeException(exception); }
        }).whenComplete((unused, error) -> Platform.runLater(() -> {
            setBusy(false);
            if (error == null) {
                setStatus(success, "status-success");
                showToast(success, false);
                onSuccess.run();
            } else {
                setStatus("Có lỗi xảy ra", "status-error");
                showToast(rootMessage(error), true);
            }
        }));
    }

    @Override public void onUserList(List<String> names) {
        Platform.runLater(() -> users.setItems(FXCollections.observableArrayList(
                names.stream().filter(name -> !name.equals(connection.username())).toList())));
    }
    @Override public void onSystemMessage(String message) {
        Platform.runLater(() -> {
            setStatus(message, "status-neutral");
            showToast(message, false);
        });
    }
    @Override public void onError(String message) { Platform.runLater(() -> { setStatus(message, "status-error"); showError(message); }); }

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
        Platform.runLater(() -> { setStatus(reason, "status-error"); messageField.setDisable(true); sendButton.setDisable(true); });
    }

    private void setBusy(boolean busy) {
        activity.setVisible(busy);
        activity.setManaged(busy);
    }

    private void setStatus(String message, String stateClass) {
        status.setText(message);
        status.getStyleClass().setAll("status-badge", stateClass);
    }

    /** Toast ngắn gọn cho thao tác thành công/lỗi; lỗi giữ lâu hơn để người dùng kịp đọc. */
    private void showToast(String message, boolean error) {
        if (toastTimer != null) toastTimer.stop();
        toast.setText((error ? "⚠  " : "✓  ") + message);
        toast.getStyleClass().setAll("toast", error ? "toast-error" : "toast-success");
        toast.setVisible(true);
        toast.setManaged(true);
        FadeTransition fade = new FadeTransition(Duration.millis(160), toast);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        toastTimer = new PauseTransition(Duration.seconds(error ? 8 : 3));
        toastTimer.setOnFinished(event -> hideToast());
        toastTimer.play();
    }

    private void hideToast() {
        toast.setVisible(false);
        toast.setManaged(false);
    }

    private void applyStyles(Scene scene) {
        var css = ChatFxApp.class.getResource("/css/lab1.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
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

    private static boolean hasFlag(List<String> args, String flag) {
        return args.stream().anyMatch(value -> flag.equalsIgnoreCase(value));
    }

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
    @FunctionalInterface private interface IoSupplier<T> { T get() throws IOException; }
    @FunctionalInterface private interface IoAction { void run() throws IOException; }
}
