package lab1.client;

import lab1.common.Protocol;

import javafx.application.Application;

public final class ChatClient {
    private ChatClient() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = parsePort(args.length > 1 ? args[1] : null);
        String username = args.length > 2 ? args[2] : "";
        String password = args.length > 3 ? args[3] : "";
        boolean autoConnect = args.length > 4 && "--connect".equalsIgnoreCase(args[4]);

        Application.launch(ChatFxApp.class, host, Integer.toString(port), username, password,
                autoConnect ? "--connect" : "");
    }

    private static int parsePort(String value) {
        if (value == null) {
            return Protocol.DEFAULT_TCP_PORT;
        }
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65_535 ? port : Protocol.DEFAULT_TCP_PORT;
        } catch (NumberFormatException exception) {
            return Protocol.DEFAULT_TCP_PORT;
        }
    }
}
