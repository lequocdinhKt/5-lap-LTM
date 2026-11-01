package ui;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.Mail;
import network.LocalMailServer;
import network.MailServerType;

public class MailAppView {
    private final Label sessionLabel = new Label("Chưa đăng nhập");
    private final TextArea inboxArea = new TextArea();

    private Consumer<AuthForm> onRegister;
    private Consumer<AuthForm> onLogin;
    private Runnable onLogout;
    private Consumer<SendForm> onSend;
    private Consumer<ReadForm> onRead;

    public Scene createScene() {
        TabPane tabs = new TabPane(
                new Tab("Tài khoản", buildAuthPane()),
                new Tab("Gửi mail", buildSendPane()),
                new Tab("Đọc mail", buildReadPane())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox root = new VBox(12, sessionLabel, tabs);
        root.setPadding(new Insets(16));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return new Scene(root, 720, 520);
    }

    public void setOnRegister(Consumer<AuthForm> handler) {
        this.onRegister = handler;
    }

    public void setOnLogin(Consumer<AuthForm> handler) {
        this.onLogin = handler;
    }

    public void setOnLogout(Runnable handler) {
        this.onLogout = handler;
    }

    public void setOnSend(Consumer<SendForm> handler) {
        this.onSend = handler;
    }

    public void setOnRead(Consumer<ReadForm> handler) {
        this.onRead = handler;
    }

    public void showSession(String username) {
        if (username == null || username.isBlank()) {
            sessionLabel.setText("Chưa đăng nhập");
        } else {
            sessionLabel.setText("Đang đăng nhập: " + username);
        }
    }

    public void showInbox(List<Mail> mails) {
        if (mails == null || mails.isEmpty()) {
            inboxArea.setText("Hộp thư đến trống.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mails.size(); i++) {
            Mail mail = mails.get(i);
            builder.append("--- Email ").append(i + 1).append(" ---\n")
                    .append("Người gửi: ").append(mail.sender).append('\n')
                    .append("Người nhận: ").append(mail.receiver).append('\n')
                    .append("Tiêu đề: ").append(mail.subject).append('\n')
                    .append("Nội dung:\n").append(mail.content).append("\n\n");
        }
        inboxArea.setText(builder.toString());
    }

    public void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, message);
    }

    public void showError(String message) {
        showAlert(Alert.AlertType.ERROR, message);
    }

    private VBox buildAuthPane() {
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();

        Button registerButton = new Button("Đăng ký");
        Button loginButton = new Button("Đăng nhập");
        Button logoutButton = new Button("Đăng xuất");

        registerButton.setOnAction(e -> {
            if (onRegister != null) {
                onRegister.accept(new AuthForm(usernameField.getText(), passwordField.getText()));
            }
        });
        loginButton.setOnAction(e -> {
            if (onLogin != null) {
                onLogin.accept(new AuthForm(usernameField.getText(), passwordField.getText()));
            }
        });
        logoutButton.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        GridPane form = formGrid();
        form.add(new Label("Tên đăng nhập"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Mật khẩu"), 0, 1);
        form.add(passwordField, 1, 1);

        HBox actions = new HBox(10, registerButton, loginButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        return new VBox(12, form, actions);
    }

    private VBox buildSendPane() {
        ComboBox<MailServerType> serverBox = serverBox();
        TextField senderField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField receiverField = new TextField();
        TextField subjectField = new TextField();
        TextArea contentArea = new TextArea();
        contentArea.setPrefRowCount(8);

        Button sendButton = new Button("Gửi mail");
        sendButton.setOnAction(e -> {
            if (onSend != null) {
                onSend.accept(new SendForm(
                        serverBox.getValue(),
                        senderField.getText(),
                        passwordField.getText(),
                        receiverField.getText(),
                        subjectField.getText(),
                        contentArea.getText()
                ));
            }
        });

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

        return new VBox(12, localAccountHint(), form, sendButton);
    }

    private VBox buildReadPane() {
        ComboBox<MailServerType> serverBox = serverBox();
        TextField emailField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button readButton = new Button("Đọc hộp thư (POP3)");
        inboxArea.setEditable(false);
        inboxArea.setWrapText(true);
        VBox.setVgrow(inboxArea, Priority.ALWAYS);

        readButton.setOnAction(e -> {
            if (onRead != null) {
                onRead.accept(new ReadForm(serverBox.getValue(), emailField.getText(), passwordField.getText()));
            }
        });

        GridPane form = formGrid();
        form.add(new Label("Máy chủ mail"), 0, 0);
        form.add(serverBox, 1, 0);
        form.add(new Label("Email cần đọc"), 0, 1);
        form.add(emailField, 1, 1);
        form.add(new Label("Mật khẩu / App Password"), 0, 2);
        form.add(passwordField, 1, 2);

        return new VBox(12, localAccountHint(), form, readButton, inboxArea);
    }

    private ComboBox<MailServerType> serverBox() {
        ComboBox<MailServerType> box = new ComboBox<>();
        box.getItems().addAll(MailServerType.values());
        box.setValue(MailServerType.LOCALHOST);
        return box;
    }

    private Label localAccountHint() {
        return new Label("Localhost: " + LocalMailServer.USER_1 + " hoặc "
                + LocalMailServer.USER_2 + " | Mật khẩu: " + LocalMailServer.PASSWORD);
    }

    private GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(4));
        return form;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AuthForm {
        public final String username;
        public final String password;

        public AuthForm(String username, String password) {
            this.username = username;
            this.password = password;
        }
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
            this.serverType = serverType;
            this.sender = sender;
            this.password = password;
            this.receiver = receiver;
            this.subject = subject;
            this.content = content;
        }
    }

    public static class ReadForm {
        public final MailServerType serverType;
        public final String email;
        public final String password;

        public ReadForm(MailServerType serverType, String email, String password) {
            this.serverType = serverType;
            this.email = email;
            this.password = password;
        }
    }
}
