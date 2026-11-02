package lab1.server;

import java.util.List;

public interface ServerListener {
    void onStarted(String bindAddress, int tcpPort, int udpTextPort, int udpFilePort);
    void onStopped();
    void onClientsChanged(List<ConnectedClient> clients);
    void onLog(String message);

    record ConnectedClient(String username, String ip, String status) {
    }
}
