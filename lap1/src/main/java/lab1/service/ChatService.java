package lab1.service;

import lab1.client.ClientConnection;
import lab1.common.GroupInfo;
import lab1.common.RecipientMode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Điều phối các thao tác chat giữa giao diện và kết nối mạng. */
public final class ChatService implements Closeable, ClientConnection.Listener {
    private ClientConnection connection;
    private volatile Listener listener;

    public void connect(String host, int port, String username, String password, boolean register)
            throws IOException {
        replaceConnection(ClientConnection.connect(host, port, username, password, register));
    }

    public void connectDemo(String host, int port, String username) throws IOException {
        replaceConnection(ClientConnection.connect(host, port, username));
    }

    public void startListening(Listener value) {
        listener = value;
        requireConnection().startListening(this);
    }

    public String username() {
        return requireConnection().username();
    }

    public void sendText(RecipientMode mode, List<String> recipients, String text) throws IOException {
        requireConnection().sendText(mode, recipients, text);
    }

    public void sendGroupText(String groupId, String text) throws IOException {
        requireConnection().sendGroupText(groupId, text);
    }

    public void createGroup(String name, List<String> members) throws IOException {
        requireConnection().createGroup(name, members);
    }

    public void updateGroupMembers(String groupId, List<String> members) throws IOException {
        requireConnection().updateGroupMembers(groupId, members);
    }

    public void deleteGroup(String groupId) throws IOException {
        requireConnection().deleteGroup(groupId);
    }

    public void leaveGroup(String groupId) throws IOException {
        requireConnection().leaveGroup(groupId);
    }

    public void requestGroupMembers(String groupId) throws IOException {
        requireConnection().requestGroupMembers(groupId);
    }

    public void sendFile(RecipientMode mode, List<String> recipients, Path file) throws IOException {
        requireConnection().sendFile(mode, recipients, file);
    }

    public void sendGroupFile(String groupId, Path file) throws IOException {
        requireConnection().sendGroupFile(groupId, file);
    }

    private void replaceConnection(ClientConnection value) {
        if (connection != null) {
            connection.close();
        }
        connection = value;
    }

    private ClientConnection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Chưa kết nối Chat Server.");
        }
        return connection;
    }

    private Listener currentListener() {
        return listener;
    }

    @Override public void onUserList(List<String> usernames) {
        Listener current = currentListener();
        if (current != null) current.onUserList(usernames);
    }

    @Override public void onSystemMessage(String message) {
        Listener current = currentListener();
        if (current != null) current.onSystemMessage(message);
    }

    @Override public void onError(String message) {
        Listener current = currentListener();
        if (current != null) current.onError(message);
    }

    @Override public void onText(String sender, RecipientMode mode, List<String> targets,
                                 long timestamp, String text) {
        Listener current = currentListener();
        if (current != null) current.onText(sender, mode, targets, timestamp, text);
    }

    @Override public void onFile(String sender, RecipientMode mode, List<String> targets,
                                 long timestamp, String originalName, Path savedPath) {
        Listener current = currentListener();
        if (current != null) current.onFile(sender, mode, targets, timestamp, originalName, savedPath);
    }

    @Override public void onDisconnected(String reason) {
        Listener current = currentListener();
        if (current != null) current.onDisconnected(reason);
    }

    @Override public void onGroupCatalog(List<GroupInfo> groups) {
        Listener current = currentListener();
        if (current != null) current.onGroupCatalog(groups);
    }

    @Override public void onGroupMembers(String groupId, List<String> members) {
        Listener current = currentListener();
        if (current != null) current.onGroupMembers(groupId, members);
    }

    @Override public void onGroupRemoved(String groupId) {
        Listener current = currentListener();
        if (current != null) current.onGroupRemoved(groupId);
    }

    @Override public void onGroupText(String groupId, String sender, long timestamp, String text) {
        Listener current = currentListener();
        if (current != null) current.onGroupText(groupId, sender, timestamp, text);
    }

    @Override public void onGroupFile(String groupId, String sender, long timestamp,
                                      String originalName, Path savedPath) {
        Listener current = currentListener();
        if (current != null) current.onGroupFile(groupId, sender, timestamp, originalName, savedPath);
    }

    @Override public void close() {
        listener = null;
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    public interface Listener {
        void onUserList(List<String> usernames);
        void onSystemMessage(String message);
        void onError(String message);
        void onText(String sender, RecipientMode mode, List<String> targets, long timestamp, String text);
        void onFile(String sender, RecipientMode mode, List<String> targets,
                    long timestamp, String originalName, Path savedPath);
        void onDisconnected(String reason);
        default void onGroupCatalog(List<GroupInfo> groups) { }
        default void onGroupMembers(String groupId, List<String> members) { }
        default void onGroupRemoved(String groupId) { }
        default void onGroupText(String groupId, String sender, long timestamp, String text) { }
        default void onGroupFile(String groupId, String sender, long timestamp,
                                 String originalName, Path savedPath) { }
    }
}
