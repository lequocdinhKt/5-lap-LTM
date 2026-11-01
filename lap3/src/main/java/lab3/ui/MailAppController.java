package lab3.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lab3.service.AppService;
import lab3.ui.MailAppView.AuthForm;
import lab3.ui.MailAppView.ReadForm;
import lab3.ui.MailAppView.SendForm;

public class MailAppController {
    private final AppService appService;
    private final MailAppView view;

    public MailAppController(AppService appService, MailAppView view) {
        this.appService = appService;
        this.view = view;
        view.setOnRegister(this::handleRegister);
        view.setOnLogin(this::handleLogin);
        view.setOnLogout(this::handleLogout);
        view.setOnSend(this::handleSend);
        view.setOnRead(this::handleRead);
    }

    private void handleRegister(AuthForm form) {
        try {
            boolean ok = appService.register(form.username, form.password);
            if (ok) {
                view.showInfo("Đăng ký thành công.");
            } else {
                view.showError("Đăng ký thất bại.");
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void handleLogin(AuthForm form) {
        try {
            boolean ok = appService.login(form.username, form.password);
            if (ok) {
                view.showSession(appService.getCurrentUser());
                view.showInfo("Đăng nhập thành công.");
            } else {
                view.showSession(null);
                view.showError("Sai tên đăng nhập hoặc mật khẩu.");
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void handleLogout() {
        appService.logout();
        view.showSession(null);
        view.showInfo("Đã đăng xuất.");
    }

    private void handleSend(SendForm form) {
        runBackground(() -> {
            boolean saved = appService.sendMail(
                    form.serverType,
                    form.sender,
                    form.password,
                    form.receiver,
                    form.subject,
                    form.content
            );
            Platform.runLater(() -> {
                if (saved) {
                    view.showInfo("Gửi và lưu mail thành công.");
                } else {
                    view.showError("Đã gửi mail nhưng không lưu được lịch sử.");
                }
            });
        });
    }

    private void handleRead(ReadForm form) {
        runBackground(() -> {
            var mails = appService.readMails(form.serverType, form.email, form.password);
            Platform.runLater(() -> view.showInbox(mails));
        });
    }

    private void runBackground(CheckedAction action) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.run();
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            view.showError(error == null ? "Có lỗi xảy ra." : error.getMessage());
        });
        new Thread(task, "mail-task").start();
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
