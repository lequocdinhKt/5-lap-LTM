package service;

import java.util.List;

import db.MySqlDB;
import models.Mail;
import network.LocalMailServer;
import network.MailProtocol;
import network.MailServerType;

public class AppService implements AutoCloseable {
    private final MySqlDB database = new MySqlDB();
    private final LocalMailServer localMailServer;
    private String currentUser;

    public AppService() {
        if (embeddedMailServerEnabled()) {
            localMailServer = new LocalMailServer();
            localMailServer.start();
        } else {
            localMailServer = null;
        }
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Đăng ký tài khoản mới.
    // Tham số: username, password.
    // Trả về: true nếu thành công.
    public boolean register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }
        return database.registerUser(username.trim(), password);
    }

    // Đăng nhập và lưu phiên hiện tại.
    // Tham số: username, password.
    // Trả về: true nếu thành công.
    public boolean login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }
        boolean ok = database.loginUser(username.trim(), password);
        currentUser = ok ? username.trim() : null;
        return ok;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean sendMail(MailServerType serverType, String sender, String password,
                            String receiver, String subject, String content) throws Exception {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Vui lòng đăng nhập trước khi gửi mail.");
        }
        if (serverType == null || isBlank(sender) || isBlank(password) || isBlank(receiver)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ máy chủ, email và mật khẩu.");
        }

        Mail mail = new Mail(
                sender.trim(),
                receiver.trim(),
                nullToEmpty(subject),
                nullToEmpty(content)
        );
        database.ensureReady();
        MailProtocol.sendMail(serverType, mail, password);
        return database.saveMail(mail, serverType.name());
    }

    public List<Mail> readMails(MailServerType serverType, String email, String password) throws Exception {
        if (serverType == null || isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ máy chủ, email và mật khẩu.");
        }
        return MailProtocol.readMails(serverType, email.trim(), password);
    }

    @Override
    public void close() {
        if (localMailServer != null) {
            localMailServer.close();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean embeddedMailServerEnabled() {
        String value = System.getenv("LAB3_EMBEDDED_MAIL_SERVER");
        return value == null || value.isBlank() || Boolean.parseBoolean(value);
    }
}
