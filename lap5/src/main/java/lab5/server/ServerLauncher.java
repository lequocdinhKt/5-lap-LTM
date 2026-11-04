package lab5.server;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lab5.config.ServerConfig;
import lab5.ui.server.ServerDashboardView;

public final class ServerLauncher extends Application {
    private ServerDashboardView view;

    @Override
    public void start(Stage stage) throws Exception {
        String configPath = getParameters().getRaw().stream()
                .filter(value -> value.startsWith("--config="))
                .map(value -> value.substring("--config=".length()))
                .findFirst()
                .orElse(System.getProperty("warehouse.config", "server.properties"));
        ServerConfig config = ServerConfig.load(configPath);
        view = new ServerDashboardView(config, configPath);
        Scene scene = new Scene(view.root(), 1180, 760);
        scene.getStylesheets().add(ServerLauncher.class.getResource("/css/lab5.css").toExternalForm());
        stage.setTitle("Lab 5 · RMI Server " + config.serverId());
        stage.setMinWidth(920);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> view.close());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
