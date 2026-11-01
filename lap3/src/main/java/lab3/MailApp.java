package lab3;

import javafx.application.Application;
import javafx.stage.Stage;
import lab3.service.AppService;
import lab3.ui.MailAppController;
import lab3.ui.MailAppView;

public class MailApp extends Application {
    @Override
    public void start(Stage stage) {
        MailAppView view = new MailAppView();
        AppService service = new AppService();
        new MailAppController(service, view);

        stage.setTitle("Lab 3 - Mail Client");
        stage.setScene(view.createScene());
        stage.setOnCloseRequest(event -> service.close());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
