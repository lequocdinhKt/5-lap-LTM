package lab5.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lab5.config.ClientConfig;
import lab5.ui.client.ClientView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientLauncher extends Application {
    private ClientView view;

    @Override
    public void start(Stage stage) throws Exception {
        Map<String, String> arguments = parse(getParameters().getRaw());
        String configPath = arguments.getOrDefault("config",
                System.getProperty("warehouse.config", "client.properties"));
        ClientConfig config = ClientConfig.load(configPath);
        if (arguments.containsKey("server-host") || arguments.containsKey("port")) {
            int port = Integer.parseInt(arguments.getOrDefault("port", String.valueOf(config.registryPort())));
            config = config.withConnection(arguments.get("server-host"), port);
        }
        config = config.withClientId(runtimeClientId(arguments.get("client-id"), config.clientId()));
        view = new ClientView(config, configPath, arguments.getOrDefault("username", config.username()));
        Scene scene = new Scene(view.root(), 1180, 760);
        scene.getStylesheets().add(ClientLauncher.class.getResource("/css/lab5.css").toExternalForm());
        stage.setTitle("Lab 5 · Quản lý kho RMI");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> view.close());
        stage.show();
    }

    private static String runtimeClientId(String requestedId, String configuredId) {
        String base = requestedId == null || requestedId.isBlank() ? configuredId : requestedId.trim();
        if (base == null || base.isBlank()) base = "client";
        String host = System.getenv().getOrDefault("COMPUTERNAME", "machine")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return base + "@" + host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, String> parse(java.util.List<String> raw) {
        Map<String, String> values = new HashMap<>();
        for (String argument : raw) {
            if (!argument.startsWith("--") || !argument.contains("=")) continue;
            int separator = argument.indexOf('=');
            values.put(argument.substring(2, separator), argument.substring(separator + 1));
        }
        return values;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
