package lab3.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lab3.model.Mail;
import lab3.network.LocalMailServer;
import lab3.network.MailServerType;

/** JavaFX view thuần: chỉ hiển thị dữ liệu và phát sự kiện cho controller. */
public class MailAppView {
    private final Label sessionLabel = new Label("Chưa đăng nhập");
    private final Label operationLabel = new Label("Sẵn sàng");
    private final TextArea inboxArea = new TextArea();
    private final VBox inboxSkeleton = createSkeleton();
    private final StackPane content = new StackPane();
    private final Label pageTitle = new Label("Tài khoản");
    private final Label pageDescription = new Label("Đăng ký hoặc đăng nhập tài khoản ứng dụng.");
    private final Label toast = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final List<Button> taskButtons = new ArrayList<>();
    private PauseTransition toastTimer;

    private Consumer<AuthForm> onRegister;
    private Consumer<AuthForm> onLogin;
    private Runnable onLogout;
    private Consumer<SendForm> onSend;
    private Consumer<ReadForm> onRead;

    public Scene createScene() {
        Node authPane = buildAuthPane();
        Node sendPane = buildSendPane();
        Node readPane = buildReadPane();
        content.getChildren().addAll(authPane, sendPane, readPane);

        Button accountNav = navButton("Tài khoản");
        Button composeNav = navButton("Soạn thư");
        Button inboxNav = navButton("Hộp thư");
        List<Button> navigation = List.of(accountNav, composeNav, inboxNav);
        accountNav.setOnAction(event -> showPage(authPane, accountNav, navigation,
                "Tài khoản", "Đăng ký hoặc đăng nhập tài khoản ứng dụng."));
        composeNav.setOnAction(event -> showPage(sendPane, composeNav, navigation,
                "Soạn thư", "Gửi mail qua SMTP localhost hoặc Gmail."));
        inboxNav.setOnAction(event -> showPage(readPane, inboxNav, navigation,
                "Hộp thư đến", "Tải thư bằng giao thức POP3."));

        Label brand = new Label("Network Mail");
        brand.getStyleClass().add("sidebar-brand");
        Region sidebarSpacer = new Region();
        VBox sidebar = new VBox(10, brand, accountNav, composeNav, inboxNav, sidebarSpacer,
                new Label("SMTP · POP3"));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(22, 16, 18, 16));
        sidebar.setPrefWidth(220);
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        pageTitle.getStyleClass().add("page-title");
        pageDescription.getStyleClass().add("muted-label");
        sessionLabel.getStyleClass().addAll("status-badge", "status-neutral");
        operationLabel.getStyleClass().addAll("status-badge", "status-neutral");
        progress.setPrefSize(20, 20);
        progress.setVisible(false);
        progress.setManaged(false);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox titles = new VBox(2, pageTitle, pageDescription);
        HBox topbar = new HBox(12, titles, spacer, progress, operationLabel, sessionLabel);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(14, 20, 14, 20));

        content.getStyleClass().add("content-stack");
        BorderPane layout = new BorderPane(content, topbar, null, null, sidebar);
        layout.getStyleClass().add("app-shell");
        BorderPane.setMargin(content, new Insets(18));

        toast.getStyleClass().add("toast");
        toast.setVisible(false);
        toast.setManaged(false);
        toast.setOnMouseClicked(event -> hideToast());
        StackPane root = new StackPane(layout, toast);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 20, 22, 20));

        showPage(authPane, accountNav, navigation,
                "Tài khoản", "Đăng ký hoặc đăng nhập tài khoản ứng dụng.");
        Scene scene = new Scene(root, 980, 680);
        var css = MailAppView.class.getResource("/css/lab3.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        return scene;
    }

    public void setOnRegister(Consumer<AuthForm> handler) { this.onRegister = handler; }
    public void setOnLogin(Consumer<AuthForm> handler) { this.onLogin = handler; }
    public void setOnLogout(Runnable handler) { this.onLogout = handler; }
    public void setOnSend(Consumer<SendForm> handler) { this.onSend = handler; }
    public void setOnRead(Consumer<ReadForm> handler) { this.onRead = handler; }

    public void showSession(String username) {
        boolean loggedIn = username != null && !username.isBlank();
        sessionLabel.setText(loggedIn ? "● " + username : "○ Chưa đăng nhập");
        sessionLabel.getStyleClass().setAll("status-badge",
                loggedIn ? "status-success" : "status-neutral");
    }

    public void setBusy(boolean busy, String message) {
        progress.setVisible(busy);
        progress.setManaged(busy);
        taskButtons.forEach(button -> button.setDisable(busy));
        operationLabel.setText(message == null || message.isBlank() ? "Sẵn sàng" : message);
        operationLabel.getStyleClass().setAll("status-badge",
                busy ? "status-warning" : "status-neutral");
    }

    public void showInboxLoading() {
        inboxArea.setVisible(false);
        inboxArea.setManaged(false);
        inboxSkeleton.setVisible(true);
        inboxSkeleton.setManaged(true);
    }

    public void showInbox(List<Mail> mails) {
        inboxSkeleton.setVisible(false);
        inboxSkeleton.setManaged(false);
        inboxArea.setVisible(true);
        inboxArea.setManaged(true);
        if (mails == null || mails.isEmpty()) {
            inboxArea.setText("Hộp thư đến trống. Hãy gửi một email thử rồi tải lại.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mails.size(); i++) {
            Mail mail = mails.get(i);
            builder.append("EMAIL ").append(i + 1).append('\n')
                    .append("Người gửi: ").append(mail.sender).append('\n')
                    .append("Người nhận: ").append(mail.receiver).append('\n')
                    .append("Tiêu đề: ").append(mail.subject).append('\n')
                    .append("────────────────────────────────\n")
                    .append(mail.content).append("\n\n");
        }
        inboxArea.setText(builder.toString());
    }

    public void showInfo(String message) { showToast(message, false); }
    public void showError(String message) {
        showToast(message == null || message.isBlank() ? "Có lỗi xảy ra." : message, true);
    }

    private VBox buildAuthPane() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Ví dụ: sinhvien01");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu tài khoản ứng dụng");
        Button registerButton = secondaryButton("Đăng ký");
        Button loginButton = primaryButton("Đăng nhập");
        Button logoutButton = secondaryButton("Đăng xuất");
        taskButtons.addAll(List.of(registerButton, loginButton, logoutButton));
        registerButton.setOnAction(e -> { if (onRegister != null) onRegister.accept(new AuthForm(usernameField.getText(), passwordField.getText())); });
        loginButton.setOnAction(e -> { if (onLogin != null) onLogin.accept(new AuthForm(usernameField.getText(), passwordField.getText())); });
        logoutButton.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        GridPane form = formGrid();
        form.add(new Label("Tên đăng nhập"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Mật khẩu"), 0, 1);
        form.add(passwordField, 1, 1);
        HBox actions = new HBox(10, registerButton, loginButton, logoutButton);
        VBox card = new VBox(18, cardHeading("Tài khoản ứng dụng",
                "Dùng để lưu lịch sử gửi mail trong MySQL."), form, actions);
        return pageCard(card);
    }

    private VBox buildSendPane() {
        ComboBox<MailServerType> serverBox = serverBox();
        TextField senderField = new TextField();
        senderField.setPromptText("sender@example.com");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu local hoặc App Password");
        TextField receiverField = new TextField();
        receiverField.setPromptText("receiver@example.com");
        TextField subjectField = new TextField();
        subjectField.setPromptText("Tiêu đề thư");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Nhập nội dung thư...");
        contentArea.setPrefRowCount(9);
        Button sendButton = primaryButton("Gửi mail qua SMTP");
        taskButtons.add(sendButton);
        sendButton.setOnAction(e -> { if (onSend != null) onSend.accept(new SendForm(
                serverBox.getValue(), senderField.getText(), passwordField.getText(),
                receiverField.getText(), subjectField.getText(), contentArea.getText())); });
        GridPane form = formGrid();
        form.add(new Label("Máy chủ mail"), 0, 0);
        form.add(serverBox, 1, 0);
        form.add(new Label("Email người gửi"), 0, 1);
        form.add(senderField, 1, 1);
        form.add(new Label("Mật khẩu / App Password"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(new Label("Email người nhận"), 0, 3);
        form.add(receiverField, 1, 3);
        form.add(new Label("Tiêu đề"), 0, 4);
        form.add(subjectField, 1, 4);
        form.add(new Label("Nội dung"), 0, 5);
        form.add(contentArea, 1, 5);
        GridPane.setVgrow(contentArea, Priority.ALWAYS);
        VBox card = new VBox(14, localAccountHint(), form, sendButton);
        return pageCard(card);
    }

    private VBox buildReadPane() {
        ComboBox<MailServerType> serverBox = serverBox();
        TextField emailField = new TextField();
        emailField.setPromptText("Email cần đọc");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu local hoặc App Password");
        Button readButton = primaryButton("Tải hộp thư bằng POP3");
        taskButtons.add(readButton);
        inboxArea.setEditable(false);
        inboxArea.setWrapText(true);
        inboxArea.setPromptText("Email tải về sẽ hiển thị ở đây.");
        VBox.setVgrow(inboxArea, Priority.ALWAYS);
        inboxSkeleton.setVisible(false);
        inboxSkeleton.setManaged(false);
        readButton.setOnAction(e -> { if (onRead != null) onRead.accept(
                new ReadForm(serverBox.getValue(), emailField.getText(), passwordField.getText())); });
        GridPane form = formGrid();
        form.add(new Label("Máy chủ mail"), 0, 0);
        form.add(serverBox, 1, 0);
        form.add(new Label("Email cần đọc"), 0, 1);
        form.add(emailField, 1, 1);
        form.add(new Label("Mật khẩu / App Password"), 0, 2);
        form.add(passwordField, 1, 2);
        VBox card = new VBox(14, localAccountHint(), form, readButton, inboxSkeleton, inboxArea);
        VBox.setVgrow(inboxArea, Priority.ALWAYS);
        return pageCard(card);
    }

    private void showPage(Node page, Button selected, List<Button> navigation,
                          String title, String description) {
        content.getChildren().forEach(node -> { node.setVisible(node == page); node.setManaged(node == page); });
        navigation.forEach(button -> button.getStyleClass().remove("nav-selected"));
        if (!selected.getStyleClass().contains("nav-selected")) selected.getStyleClass().add("nav-selected");
        pageTitle.setText(title);
        pageDescription.setText(description);
        FadeTransition fade = new FadeTransition(Duration.millis(180), page);
        fade.setFromValue(0.3);
        fade.setToValue(1);
        fade.play();
    }

    private VBox pageCard(VBox body) {
        body.getStyleClass().add("content-card");
        body.setPadding(new Insets(22));
        VBox wrapper = new VBox(body);
        VBox.setVgrow(body, Priority.ALWAYS);
        return wrapper;
    }

    private VBox cardHeading(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        Label caption = new Label(description);
        caption.getStyleClass().add("muted-label");
        return new VBox(3, heading, caption);
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private ComboBox<MailServerType> serverBox() {
        ComboBox<MailServerType> box = new ComboBox<>();
        box.getItems().addAll(MailServerType.values());
        box.setValue(MailServerType.LOCALHOST);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label localAccountHint() {
        Label label = new Label("Demo localhost: " + LocalMailServer.USER_1 + " hoặc "
                + LocalMailServer.USER_2 + "  •  Mật khẩu: " + LocalMailServer.PASSWORD);
        label.getStyleClass().add("hint-box");
        label.setWrapText(true);
        return label;
    }

    private GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);
        return form;
    }

    private static VBox createSkeleton() {
        VBox box = new VBox(10);
        box.getStyleClass().add("skeleton-box");
        for (int i = 0; i < 4; i++) {
            Region line = new Region();
            line.getStyleClass().add("skeleton-line");
            line.setPrefHeight(i == 0 ? 18 : 12);
            line.setMaxWidth(i == 3 ? 320 : Double.MAX_VALUE);
            box.getChildren().add(line);
        }
        return box;
    }

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

    public static class AuthForm {
        public final String username;
        public final String password;
        public AuthForm(String username, String password) { this.username = username; this.password = password; }
    }

    public static class SendForm {
        public final MailServerType serverType;
        public final String sender;
        public final String password;
        public final String receiver;
        public final String subject;
        public final String content;
        public SendForm(MailServerType serverType, String sender, String password,
                        String receiver, String subject, String content) {
            this.serverType = serverType; this.sender = sender; this.password = password;
            this.receiver = receiver; this.subject = subject; this.content = content;
        }
    }

    public static class ReadForm {
        public final MailServerType serverType;
        public final String email;
        public final String password;
        public ReadForm(MailServerType serverType, String email, String password) {
            this.serverType = serverType; this.email = email; this.password = password;
        }
    }
}
