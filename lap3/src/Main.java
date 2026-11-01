import javafx.application.Application;
import javafx.stage.Stage;
import service.AppService;
import ui.MailAppController;
import ui.MailAppView;

public class Main extends Application {
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
