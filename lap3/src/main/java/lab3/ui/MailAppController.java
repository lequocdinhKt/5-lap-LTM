package lab3.ui;

import javafx.concurrent.Task;
import java.util.function.Consumer;
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
        runBackground("Đang đăng ký...", null,
                () -> appService.register(form.username, form.password), ok -> {
            if (ok) {
                view.showInfo("Đăng ký thành công.");
            } else {
                view.showError("Đăng ký thất bại.");
            }
        });
    }

    private void handleLogin(AuthForm form) {
        runBackground("Đang đăng nhập...", null,
                () -> appService.login(form.username, form.password), ok -> {
            if (ok) {
                view.showSession(appService.getCurrentUser());
                view.showInfo("Đăng nhập thành công.");
            } else {
                view.showSession(null);
                view.showError("Sai tên đăng nhập hoặc mật khẩu.");
            }
        });
    }

    private void handleLogout() {
        appService.logout();
        view.showSession(null);
        view.showInfo("Đã đăng xuất.");
    }

    private void handleSend(SendForm form) {
        runBackground("Đang gửi qua SMTP...", null, () -> appService.sendMail(
                    form.serverType,
                    form.sender,
                    form.password,
                    form.receiver,
                    form.subject,
                    form.content
            ), saved -> {
            if (saved) {
                view.showInfo("Gửi và lưu mail thành công.");
            } else {
                view.showError("Đã gửi mail nhưng không lưu được lịch sử.");
            }
        });
    }

    private void handleRead(ReadForm form) {
        runBackground("Đang tải POP3...", view::showInboxLoading,
                () -> appService.readMails(form.serverType, form.email, form.password),
                view::showInbox);
    }

    private <T> void runBackground(String message, Runnable before,
                                   CheckedSupplier<T> action, Consumer<T> onSuccess) {
        if (before != null) before.run();
        view.setBusy(true, message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.get();
            }
        };
        task.setOnSucceeded(event -> {
            view.setBusy(false, "Sẵn sàng");
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            view.setBusy(false, "Thao tác thất bại");
            Throwable error = task.getException();
            view.showError(error == null ? "Có lỗi xảy ra." : error.getMessage());
        });
        Thread worker = new Thread(task, "mail-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
