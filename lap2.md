This file is a merged representation of a subset of the codebase, containing specifically included files, combined into a single document by Repomix.

<file_summary>
This section contains a summary of this file.

<purpose>
This file contains a packed representation of a subset of the repository's contents that is considered the most important context.
It is designed to be easily consumable by AI systems for analysis, code review,
or other automated processes.
</purpose>

<file_format>
The content is organized as follows:
1. This summary section
2. Repository information
3. Directory structure
4. Repository files (if enabled)
5. Multiple file entries, each consisting of:
  - File path as an attribute
  - Full contents of the file
</file_format>

<usage_guidelines>
- This file should be treated as read-only. Any changes should be made to the
  original repository files, not this packed version.
- When processing this file, use the file path to distinguish
  between different files in the repository.
- Be aware that this file may contain sensitive information. Handle it with
  the same level of security as you would the original repository.
</usage_guidelines>

<notes>
- Some files may have been excluded based on .gitignore rules and Repomix's configuration
- Binary files are not included in this packed representation. Please refer to the Repository Structure section for a complete list of file paths, including binary files
- Only files matching these patterns are included: lap2
- Files matching patterns in .gitignore are excluded
- Files matching default ignore patterns are excluded
- Files are sorted by Git change count (files with more changes are at the bottom)
</notes>

</file_summary>

<directory_structure>
lap2/
  ftp-root/
    alice/
      .gitkeep
    bob/
      .gitkeep
  src/
    lab2/
      client/
        FileZillaFrame.java
        FtpClient.java
        FtpClientSession.java
      common/
        FtpProtocol.java
      db/
        MySqlDB.java
      server/
        ControlHandler.java
        DataChannel.java
        FtpServer.java
        UserStore.java
      test/
        FtpIntegrationTest.java
  .gitignore
  compile.ps1
  db.properties.example
  README.md
  run-client.ps1
  run-server.ps1
</directory_structure>

<files>
This section contains the contents of the repository's files.

<file path="lap2/ftp-root/alice/.gitkeep">

</file>

<file path="lap2/ftp-root/bob/.gitkeep">

</file>

<file path="lap2/src/lab2/client/FileZillaFrame.java">
package lab2.client;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("serial")
public final class FileZillaFrame extends JFrame {
    private final JTextField hostField = new JTextField("localhost", 10);
    private final JTextField portField = new JTextField("2121", 5);
    private final JTextField userField = new JTextField("alice", 8);
    private final JPasswordField passwordField = new JPasswordField("123456", 8);
    private final JButton connectButton = new JButton("Kết nối");
    private final JButton disconnectButton = new JButton("Ngắt");
    private final JLabel localPathLabel = new JLabel();
    private final JLabel remotePathLabel = new JLabel("/");
    private final DefaultListModel<Path> localModel = new DefaultListModel<>();
    private final DefaultListModel<FtpClientSession.RemoteEntry> remoteModel = new DefaultListModel<>();
    private final JList<Path> localList = new JList<>(localModel);
    private final JList<FtpClientSession.RemoteEntry> remoteList = new JList<>(remoteModel);
    private final JTextArea logArea = new JTextArea(8, 80);
    private final List<JButton> remoteButtons = new ArrayList<>();

    private Path localDirectory = Path.of(System.getProperty("user.home")).toAbsolutePath();
    private FtpClientSession session;

    public FileZillaFrame() {
        super("Lab 2 - FTP Client");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);
        buildUi();
        wireActions();
        refreshLocal();
        setConnected(false);
    }

    private void buildUi() {
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connection.add(new JLabel("Host"));
        connection.add(hostField);
        connection.add(new JLabel("Port"));
        connection.add(portField);
        connection.add(new JLabel("User"));
        connection.add(userField);
        connection.add(new JLabel("Password"));
        connection.add(passwordField);
        connection.add(connectButton);
        connection.add(disconnectButton);

        localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localList.setCellRenderer(new PathRenderer());
        JPanel localPanel = new JPanel(new BorderLayout(4, 4));
        localPanel.setBorder(BorderFactory.createTitledBorder("Máy local"));
        localPanel.add(localPathLabel, BorderLayout.NORTH);
        localPanel.add(new JScrollPane(localList), BorderLayout.CENTER);
        JPanel localActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton localUp = new JButton("Lên");
        JButton chooseLocal = new JButton("Chọn thư mục");
        JButton upload = new JButton("Upload →");
        localActions.add(localUp);
        localActions.add(chooseLocal);
        localActions.add(upload);
        localPanel.add(localActions, BorderLayout.SOUTH);

        JPanel remotePanel = new JPanel(new BorderLayout(4, 4));
        remotePanel.setBorder(BorderFactory.createTitledBorder("FTP Server"));
        remotePanel.add(remotePathLabel, BorderLayout.NORTH);
        remotePanel.add(new JScrollPane(remoteList), BorderLayout.CENTER);
        JPanel remoteActions = new JPanel(new GridLayout(2, 3, 4, 4));
        JButton remoteUp = button("Lên");
        JButton refresh = button("Làm mới");
        JButton download = button("← Download");
        JButton mkdir = button("Thư mục mới");
        JButton delete = button("Xóa");
        remoteActions.add(remoteUp);
        remoteActions.add(refresh);
        remoteActions.add(download);
        remoteActions.add(mkdir);
        remoteActions.add(delete);
        remotePanel.add(remoteActions, BorderLayout.SOUTH);

        JSplitPane files = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, localPanel, remotePanel);
        files.setResizeWeight(0.5);
        logArea.setEditable(false);
        logArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        JScrollPane log = new JScrollPane(logArea);
        log.setBorder(BorderFactory.createTitledBorder("Nhật ký giao thức FTP"));

        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, files, log);
        main.setResizeWeight(0.72);
        add(connection, BorderLayout.NORTH);
        add(main, BorderLayout.CENTER);

        connectButton.addActionListener(event -> connect());
        disconnectButton.addActionListener(event -> disconnect());
        localUp.addActionListener(event -> {
            if (localDirectory.getParent() != null) {
                localDirectory = localDirectory.getParent();
                refreshLocal();
            }
        });
        chooseLocal.addActionListener(event -> chooseLocalDirectory());
        upload.addActionListener(event -> upload());
        refresh.addActionListener(event -> refreshRemote());
        remoteUp.addActionListener(event -> remoteAction(() -> session.changeDirectory(".."), true));
        download.addActionListener(event -> download());
        mkdir.addActionListener(event -> makeDirectory());
        delete.addActionListener(event -> deleteRemote());
    }

    private JButton button(String text) {
        JButton button = new JButton(text);
        remoteButtons.add(button);
        return button;
    }

    private void wireActions() {
        localList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Path selected = localList.getSelectedValue();
                if (event.getClickCount() == 2 && selected != null && Files.isDirectory(selected)) {
                    localDirectory = selected;
                    refreshLocal();
                }
            }
        });
        remoteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                FtpClientSession.RemoteEntry selected = remoteList.getSelectedValue();
                if (event.getClickCount() == 2 && selected != null && selected.directory()) {
                    remoteAction(() -> session.changeDirectory(selected.name()), true);
                }
            }
        });
    }

    private void connect() {
        setBusy(true);
        FtpClientSession newSession = new FtpClientSession(this::appendLog);
        runAsync(() -> {
            newSession.connect(hostField.getText().trim(), Integer.parseInt(portField.getText().trim()),
                    userField.getText().trim(), new String(passwordField.getPassword()));
            session = newSession;
        }, () -> {
            setConnected(true);
            refreshRemote();
        }, () -> {
            newSession.close();
            setConnected(false);
        });
    }

    private void disconnect() {
        if (session != null) {
            session.close();
            session = null;
        }
        remoteModel.clear();
        remotePathLabel.setText("/");
        setConnected(false);
    }

    private void refreshLocal() {
        localModel.clear();
        localPathLabel.setText(localDirectory.toString());
        try (var stream = Files.list(localDirectory)) {
            stream.sorted(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(localModel::addElement);
        } catch (Exception error) {
            showError(error);
        }
    }

    private void refreshRemote() {
        if (session == null) {
            return;
        }
        setBusy(true);
        AtomicReference<List<FtpClientSession.RemoteEntry>> entries = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        runAsync(() -> {
            path.set(session.pwd());
            entries.set(session.list());
        }, () -> {
            remotePathLabel.setText(path.get());
            remoteModel.clear();
            entries.get().forEach(remoteModel::addElement);
            setBusy(false);
        }, () -> setBusy(false));
    }

    private void upload() {
        Path selected = localList.getSelectedValue();
        if (selected == null || !Files.isRegularFile(selected)) {
            message("Hãy chọn một file local.");
            return;
        }
        remoteAction(() -> session.upload(selected, selected.getFileName().toString()), true);
    }

    private void download() {
        FtpClientSession.RemoteEntry selected = remoteList.getSelectedValue();
        if (selected == null || selected.directory()) {
            message("Hãy chọn một file trên server.");
            return;
        }
        Path target = localDirectory.resolve(selected.name());
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this, "Ghi đè " + target.getFileName() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        remoteAction(() -> session.download(selected.name(), target), false);
    }

    private void makeDirectory() {
        String name = JOptionPane.showInputDialog(this, "Tên thư mục:");
        if (name != null && !name.isBlank()) {
            remoteAction(() -> session.makeDirectory(name.trim()), true);
        }
    }

    private void deleteRemote() {
        FtpClientSession.RemoteEntry selected = remoteList.getSelectedValue();
        if (selected == null) {
            message("Hãy chọn file hoặc thư mục.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa " + selected.name() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            remoteAction(() -> session.delete(selected.name(), selected.directory()), true);
        }
    }

    private void chooseLocalDirectory() {
        JFileChooser chooser = new JFileChooser(localDirectory.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            localDirectory = chooser.getSelectedFile().toPath();
            refreshLocal();
        }
    }

    private void remoteAction(CheckedAction action, boolean refreshRemote) {
        if (session == null) {
            message("Chưa kết nối FTP.");
            return;
        }
        setBusy(true);
        runAsync(action, () -> {
            refreshLocal();
            if (refreshRemote) {
                refreshRemote();
            } else {
                setBusy(false);
            }
        }, () -> setBusy(false));
    }

    private void runAsync(CheckedAction action, Runnable success, Runnable failure) {
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    action.run();
                } catch (Exception exception) {
                    error = exception;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error == null) {
                    success.run();
                } else {
                    failure.run();
                    showError(error);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        connectButton.setEnabled(!busy && session == null);
        disconnectButton.setEnabled(!busy && session != null);
        for (JButton button : remoteButtons) {
            button.setEnabled(!busy && session != null);
        }
    }

    private void setConnected(boolean connected) {
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
        userField.setEnabled(!connected);
        passwordField.setEnabled(!connected);
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        for (JButton button : remoteButtons) {
            button.setEnabled(connected);
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void showError(Exception error) {
        appendLog("LỖI: " + error.getMessage());
        JOptionPane.showMessageDialog(this, error.getMessage(), "Lỗi FTP", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        disconnect();
        super.dispose();
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static final class PathRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selected, boolean focus) {
            Path path = (Path) value;
            String text = (Files.isDirectory(path) ? "[Thư mục] " : "") + path.getFileName();
            return super.getListCellRendererComponent(list, text, index, selected, focus);
        }
    }
}
</file>

<file path="lap2/src/lab2/client/FtpClient.java">
package lab2.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class FtpClient {
    private FtpClient() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FileZillaFrame().setVisible(true);
        });
    }
}
</file>

<file path="lap2/src/lab2/client/FtpClientSession.java">
package lab2.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FtpClientSession implements Closeable {
    private static final Pattern PASV = Pattern.compile("\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)");

    private final Consumer<String> logger;
    private Socket control;
    private BufferedReader reader;
    private BufferedWriter writer;

    public FtpClientSession(Consumer<String> logger) {
        this.logger = logger;
    }

    public void connect(String host, int port, String username, String password) throws IOException {
        close();
        control = new Socket();
        control.connect(new InetSocketAddress(host, port), 10_000);
        control.setSoTimeout(30_000);
        reader = new BufferedReader(new InputStreamReader(control.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(control.getOutputStream(), StandardCharsets.UTF_8));
        expect(readReply(), 220);
        expect(command("USER " + username, false), 331);
        expect(command("PASS " + password, true), 230);
        expect(command("TYPE I", false), 200);
    }

    public List<RemoteEntry> list() throws IOException {
        Socket data = openPassive();
        Reply first = command("LIST", false);
        expect(first, 150, 125);
        List<RemoteEntry> entries = new ArrayList<>();
        try (data; BufferedReader lines = new BufferedReader(new InputStreamReader(
                data.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = lines.readLine()) != null) {
                RemoteEntry entry = parseListLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        expect(readReply(), 226);
        return entries;
    }

    public void upload(Path localFile, String remoteName) throws IOException {
        Socket data = openPassive();
        expect(command("STOR " + remoteName, false), 150, 125);
        try (data; var input = Files.newInputStream(localFile); var output = data.getOutputStream()) {
            input.transferTo(output);
        }
        expect(readReply(), 226);
    }

    public void download(String remoteName, Path localFile) throws IOException {
        Socket data = openPassive();
        expect(command("RETR " + remoteName, false), 150, 125);
        Path temporary = localFile.resolveSibling(localFile.getFileName() + ".downloading");
        try (data; var input = data.getInputStream()) {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, localFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            Files.deleteIfExists(temporary);
            throw error;
        }
        expect(readReply(), 226);
    }

    public String pwd() throws IOException {
        Reply reply = command("PWD", false);
        expect(reply, 257);
        int firstQuote = reply.text.indexOf('"');
        int secondQuote = reply.text.indexOf('"', firstQuote + 1);
        return firstQuote >= 0 && secondQuote > firstQuote
                ? reply.text.substring(firstQuote + 1, secondQuote) : "/";
    }

    public void changeDirectory(String path) throws IOException {
        expect(command("CWD " + path, false), 250);
    }

    public void makeDirectory(String name) throws IOException {
        expect(command("MKD " + name, false), 257);
    }

    public void delete(String name, boolean directory) throws IOException {
        expect(command((directory ? "RMD " : "DELE ") + name, false), 250);
    }

    public boolean isConnected() {
        return control != null && control.isConnected() && !control.isClosed();
    }

    private Socket openPassive() throws IOException {
        Reply reply = command("PASV", false);
        expect(reply, 227);
        Matcher matcher = PASV.matcher(reply.text);
        if (!matcher.find()) {
            throw new IOException("Server trả PASV không hợp lệ: " + reply.text);
        }
        String host = String.join(".", matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
        int port = Integer.parseInt(matcher.group(5)) * 256 + Integer.parseInt(matcher.group(6));
        Socket data = new Socket();
        data.connect(new InetSocketAddress(host, port), 10_000);
        data.setSoTimeout(30_000);
        return data;
    }

    private Reply command(String line, boolean password) throws IOException {
        logger.accept("> " + (password ? "PASS ******" : line));
        writer.write(line + "\r\n");
        writer.flush();
        return readReply();
    }

    private Reply readReply() throws IOException {
        String first = reader.readLine();
        if (first == null) {
            throw new IOException("Server đã đóng kết nối");
        }
        logger.accept("< " + first);
        if (first.length() < 3) {
            throw new IOException("FTP reply không hợp lệ: " + first);
        }
        int code;
        try {
            code = Integer.parseInt(first.substring(0, 3));
        } catch (NumberFormatException error) {
            throw new IOException("FTP reply không hợp lệ: " + first);
        }
        StringBuilder text = new StringBuilder(first);
        if (first.length() > 3 && first.charAt(3) == '-') {
            String ending = first.substring(0, 3) + " ";
            String line;
            do {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("Reply nhiều dòng bị ngắt");
                }
                logger.accept("< " + line);
                text.append('\n').append(line);
            } while (!line.startsWith(ending));
        }
        return new Reply(code, text.toString());
    }

    private static void expect(Reply reply, int... expected) throws IOException {
        for (int code : expected) {
            if (reply.code == code) {
                return;
            }
        }
        throw new IOException(reply.text);
    }

    private static RemoteEntry parseListLine(String line) {
        String[] parts = line.trim().split("\\s+", 9);
        if (parts.length < 9) {
            return null;
        }
        boolean directory = parts[0].startsWith("d");
        long size;
        try {
            size = Long.parseLong(parts[4]);
        } catch (NumberFormatException error) {
            size = 0;
        }
        return new RemoteEntry(parts[8], directory, size);
    }

    @Override
    public void close() {
        if (writer != null && isConnected()) {
            try {
                command("QUIT", false);
            } catch (IOException ignored) {
            }
        }
        if (control != null) {
            try {
                control.close();
            } catch (IOException ignored) {
            }
        }
        control = null;
        reader = null;
        writer = null;
    }

    public record RemoteEntry(String name, boolean directory, long size) {
        @Override
        public String toString() {
            return (directory ? "[Thư mục] " : "") + name + (directory ? "" : "  (" + size + " B)");
        }
    }

    private record Reply(int code, String text) {
    }
}
</file>

<file path="lap2/src/lab2/common/FtpProtocol.java">
package lab2.common;

import java.net.InetAddress;

public final class FtpProtocol {
    public static final int DEFAULT_PORT = 2121;
    public static final String CRLF = "\r\n";

    private FtpProtocol() {
    }

    public record Command(String name, String argument) {
    }

    public static Command parseCommand(String line) {
        String value = line == null ? "" : line.trim();
        int separator = value.indexOf(' ');
        String name = (separator < 0 ? value : value.substring(0, separator)).toUpperCase();
        String argument = separator < 0 ? "" : value.substring(separator + 1).trim();
        return new Command(name, argument);
    }

    public static String passiveReply(InetAddress address, int port) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            bytes = new byte[] {127, 0, 0, 1};
        }
        return String.format("227 Entering Passive Mode (%d,%d,%d,%d,%d,%d)",
                bytes[0] & 0xff, bytes[1] & 0xff, bytes[2] & 0xff, bytes[3] & 0xff,
                port / 256, port % 256);
    }
}
</file>

<file path="lap2/src/lab2/db/MySqlDB.java">
package lab2.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class MySqlDB {
    private final Properties config = loadConfig();
    private boolean initialized;

    public synchronized void ensureReady() throws SQLException {
        if (initialized) {
            return;
        }
        String baseUrl = required("db.url");
        String database = config.getProperty("db.name", "lab2_ftp");
        try (Connection connection = DriverManager.getConnection(baseUrl, required("db.user"), required("db.password"));
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "") +
                    "` CHARACTER SET utf8mb4");
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      password VARCHAR(100) NOT NULL,
                      home_dir VARCHAR(255) NOT NULL
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transfer_logs (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL,
                      command VARCHAR(10) NOT NULL,
                      filename VARCHAR(500) NOT NULL,
                      bytes BIGINT NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )""");
        }
        seed("alice", "123456", "alice");
        seed("bob", "123456", "bob");
        initialized = true;
    }

    public String authenticate(String username, String password) throws SQLException {
        ensureReady();
        String sql = "SELECT home_dir FROM users WHERE username=? AND password=?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    public void logTransfer(String username, String command, String filename, long bytes) {
        try {
            ensureReady();
            String sql = "INSERT INTO transfer_logs(username,command,filename,bytes) VALUES(?,?,?,?)";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, command);
                statement.setString(3, filename);
                statement.setLong(4, bytes);
                statement.executeUpdate();
            }
        } catch (SQLException error) {
            System.err.println("Không thể ghi transfer_logs: " + error.getMessage());
        }
    }

    private void seed(String username, String password, String home) throws SQLException {
        String sql = "INSERT IGNORE INTO users(username,password,home_dir) VALUES(?,?,?)";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, home);
            statement.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(required("db.url") + "/" + config.getProperty("db.name", "lab2_ftp")
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",
                required("db.user"), required("db.password"));
    }

    private String required(String key) {
        String value = config.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình " + key + " trong db.properties");
        }
        return value;
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("db.properties"))) {
            properties.load(reader);
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Không đọc được db.properties", error);
        }
    }
}
</file>

<file path="lap2/src/lab2/server/ControlHandler.java">
package lab2.server;

import lab2.common.FtpProtocol;
import lab2.db.MySqlDB;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ControlHandler implements Runnable {
    private static final DateTimeFormatter LIST_TIME =
            DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneId.systemDefault());

    private final Socket controlSocket;
    private final MySqlDB database;
    private final Path ftpRoot;
    private final DataChannel dataChannel = new DataChannel();
    private BufferedWriter writer;
    private String pendingUsername;
    private String username;
    private UserStore store;

    public ControlHandler(Socket controlSocket, MySqlDB database, Path ftpRoot) {
        this.controlSocket = controlSocket;
        this.database = database;
        this.ftpRoot = ftpRoot;
    }

    @Override
    public void run() {
        String remote = controlSocket.getRemoteSocketAddress().toString();
        System.out.println("FTP control connected: " + remote);
        try (controlSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     controlSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                     controlSocket.getOutputStream(), StandardCharsets.UTF_8))) {
            writer = output;
            reply(220, "Lab 2 FTP Server ready");
            String line;
            while ((line = reader.readLine()) != null) {
                FtpProtocol.Command command = FtpProtocol.parseCommand(line);
                System.out.println(remote + " > " + command.name()
                        + ("PASS".equals(command.name()) ? " ******" : " " + command.argument()));
                if (!execute(command)) {
                    break;
                }
            }
        } catch (Exception error) {
            System.err.println("FTP session error " + remote + ": " + error.getMessage());
        } finally {
            try {
                dataChannel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean execute(FtpProtocol.Command command) throws Exception {
        switch (command.name()) {
            case "USER" -> user(command.argument());
            case "PASS" -> pass(command.argument());
            case "QUIT" -> {
                reply(221, "Goodbye");
                return false;
            }
            case "SYST" -> reply(215, "UNIX Type: L8");
            case "FEAT" -> multiline("211-Features", " UTF8", " PASV", " PORT", " SIZE", "211 End");
            case "OPTS" -> reply(200, "UTF8 enabled");
            case "NOOP" -> reply(200, "OK");
            default -> {
                if (!authenticated()) {
                    reply(530, "Please login with USER and PASS");
                } else {
                    executeAuthenticated(command);
                }
            }
        }
        return true;
    }

    private void executeAuthenticated(FtpProtocol.Command command) throws Exception {
        switch (command.name()) {
            case "PWD", "XPWD" -> reply(257, "\"" + store.workingDirectory() + "\" is current directory");
            case "CWD" -> changeDirectory(command.argument());
            case "CDUP" -> changeDirectory("..");
            case "TYPE" -> reply(200, "Type set to " + ("A".equalsIgnoreCase(command.argument()) ? "A" : "I"));
            case "PASV" -> passive();
            case "PORT" -> active(command.argument());
            case "LIST" -> list(false);
            case "NLST" -> list(true);
            case "RETR" -> retrieve(command.argument());
            case "STOR" -> store(command.argument());
            case "DELE" -> delete(command.argument(), false);
            case "RMD" -> delete(command.argument(), true);
            case "MKD" -> makeDirectory(command.argument());
            case "SIZE" -> size(command.argument());
            default -> reply(502, "Command not implemented");
        }
    }

    private void user(String argument) throws IOException {
        pendingUsername = argument;
        username = null;
        store = null;
        reply(331, "Password required");
    }

    private void pass(String password) throws Exception {
        if (pendingUsername == null || pendingUsername.isBlank()) {
            reply(503, "Send USER first");
            return;
        }
        String home = database.authenticate(pendingUsername, password);
        if (home == null) {
            reply(530, "Login incorrect");
            return;
        }
        username = pendingUsername;
        store = new UserStore(ftpRoot, home);
        reply(230, "Login successful");
    }

    private boolean authenticated() {
        return username != null && store != null;
    }

    private void changeDirectory(String path) throws IOException {
        try {
            store.changeDirectory(path);
            reply(250, "Directory changed to " + store.workingDirectory());
        } catch (IOException error) {
            reply(550, error.getMessage());
        }
    }

    private void passive() throws IOException {
        int port = dataChannel.enterPassive(controlSocket.getLocalAddress());
        sendLine(FtpProtocol.passiveReply(controlSocket.getLocalAddress(), port));
    }

    private void active(String argument) throws IOException {
        try {
            dataChannel.enterActive(argument);
            reply(200, "PORT command successful");
        } catch (RuntimeException | IOException error) {
            reply(501, "Invalid PORT");
        }
    }

    private void list(boolean namesOnly) throws IOException {
        if (!requireDataChannel()) {
            return;
        }
        reply(150, "Opening data connection for directory list");
        try (Socket data = dataChannel.open();
             BufferedWriter listing = new BufferedWriter(new OutputStreamWriter(
                     data.getOutputStream(), StandardCharsets.UTF_8));
             DirectoryStream<Path> entries = Files.newDirectoryStream(store.current())) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().equals(".gitkeep")) {
                    continue;
                }
                if (namesOnly) {
                    listing.write(entry.getFileName().toString());
                } else {
                    boolean directory = Files.isDirectory(entry);
                    long size = directory ? 0L : Files.size(entry);
                    Instant modified = Files.getLastModifiedTime(entry).toInstant();
                    listing.write(String.format("%crw-r--r-- 1 ftp ftp %12d %s %s",
                            directory ? 'd' : '-', size, LIST_TIME.format(modified), entry.getFileName()));
                }
                listing.write(FtpProtocol.CRLF);
            }
        } catch (IOException error) {
            reply(425, "Data connection failed: " + error.getMessage());
            return;
        }
        reply(226, "Directory send OK");
    }

    private void retrieve(String argument) throws IOException {
        Path file;
        try {
            file = store.resolve(argument);
            if (!Files.isRegularFile(file)) {
                reply(550, "File not found");
                return;
            }
        } catch (IOException error) {
            reply(550, error.getMessage());
            return;
        }
        if (!requireDataChannel()) {
            return;
        }
        long bytes = Files.size(file);
        reply(150, "Opening binary data connection");
        try (Socket data = dataChannel.open(); OutputStream output = data.getOutputStream()) {
            Files.copy(file, output);
        } catch (IOException error) {
            reply(425, "Data connection failed: " + error.getMessage());
            return;
        }
        database.logTransfer(username, "RETR", store.workingDirectory() + "/" + argument, bytes);
        reply(226, "Transfer complete");
    }

    private void store(String argument) throws IOException {
        if (argument.isBlank()) {
            reply(501, "Missing filename");
            return;
        }
        Path target;
        try {
            target = store.resolve(argument);
            if (Files.isDirectory(target)) {
                reply(550, "Target is a directory");
                return;
            }
        } catch (IOException error) {
            reply(550, error.getMessage());
            return;
        }
        if (!requireDataChannel()) {
            return;
        }
        reply(150, "Opening binary data connection");
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try (Socket data = dataChannel.open()) {
            Files.copy(data.getInputStream(), temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            Files.deleteIfExists(temporary);
            reply(425, "Data connection failed: " + error.getMessage());
            return;
        }
        long bytes = Files.size(target);
        database.logTransfer(username, "STOR", store.workingDirectory() + "/" + argument, bytes);
        reply(226, "Transfer complete");
    }

    private void delete(String argument, boolean directory) throws IOException {
        try {
            Path target = store.resolve(argument);
            if (target.equals(store.current())) {
                throw new IOException("Không thể xóa thư mục hiện tại");
            }
            if (directory && !Files.isDirectory(target)) {
                throw new IOException("Không phải thư mục");
            }
            if (!directory && !Files.isRegularFile(target)) {
                throw new IOException("Không phải file");
            }
            Files.delete(target);
            reply(250, "Delete successful");
        } catch (IOException error) {
            reply(550, error.getMessage());
        }
    }

    private void makeDirectory(String argument) throws IOException {
        try {
            Path target = store.resolve(argument);
            Files.createDirectory(target);
            reply(257, "\"" + argument + "\" created");
        } catch (IOException error) {
            reply(550, error.getMessage());
        }
    }

    private void size(String argument) throws IOException {
        try {
            Path target = store.resolve(argument);
            if (!Files.isRegularFile(target)) {
                throw new IOException("File not found");
            }
            reply(213, Long.toString(Files.size(target)));
        } catch (IOException error) {
            reply(550, error.getMessage());
        }
    }

    private boolean requireDataChannel() throws IOException {
        if (!dataChannel.isPrepared()) {
            reply(425, "Use PASV or PORT first");
            return false;
        }
        return true;
    }

    private void reply(int code, String message) throws IOException {
        sendLine(code + " " + message);
    }

    private void multiline(String... lines) throws IOException {
        for (String line : lines) {
            sendLine(line);
        }
    }

    private void sendLine(String line) throws IOException {
        writer.write(line);
        writer.write(FtpProtocol.CRLF);
        writer.flush();
        System.out.println("< " + line);
    }
}
</file>

<file path="lap2/src/lab2/server/DataChannel.java">
package lab2.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class DataChannel implements Closeable {
    private ServerSocket passiveSocket;
    private InetSocketAddress activeAddress;

    public int enterPassive(InetAddress bindAddress) throws IOException {
        close();
        passiveSocket = new ServerSocket();
        passiveSocket.bind(new InetSocketAddress(bindAddress, 0));
        passiveSocket.setSoTimeout(15_000);
        return passiveSocket.getLocalPort();
    }

    public void enterActive(String argument) throws IOException {
        close();
        String[] values = argument.split(",");
        if (values.length != 6) {
            throw new IOException("PORT phải có 6 số");
        }
        int[] numbers = new int[6];
        for (int index = 0; index < 6; index++) {
            numbers[index] = Integer.parseInt(values[index].trim());
            if (numbers[index] < 0 || numbers[index] > 255) {
                throw new IOException("PORT không hợp lệ");
            }
        }
        activeAddress = new InetSocketAddress(
                numbers[0] + "." + numbers[1] + "." + numbers[2] + "." + numbers[3],
                numbers[4] * 256 + numbers[5]);
    }

    public Socket open() throws IOException {
        if (passiveSocket != null) {
            ServerSocket server = passiveSocket;
            passiveSocket = null;
            try (server) {
                return server.accept();
            }
        }
        if (activeAddress != null) {
            InetSocketAddress target = activeAddress;
            activeAddress = null;
            Socket socket = new Socket();
            socket.connect(target, 10_000);
            return socket;
        }
        throw new IOException("Hãy gửi PASV hoặc PORT trước");
    }

    public boolean isPrepared() {
        return passiveSocket != null || activeAddress != null;
    }

    @Override
    public void close() throws IOException {
        activeAddress = null;
        if (passiveSocket != null) {
            passiveSocket.close();
            passiveSocket = null;
        }
    }
}
</file>

<file path="lap2/src/lab2/server/FtpServer.java">
package lab2.server;

import lab2.common.FtpProtocol;
import lab2.db.MySqlDB;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FtpServer {
    private FtpServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : FtpProtocol.DEFAULT_PORT;
        Path root = Path.of(args.length > 1 ? args[1] : "ftp-root").toAbsolutePath().normalize();
        MySqlDB database = new MySqlDB();
        try {
            database.ensureReady();
        } catch (SQLException error) {
            System.err.println("Không thể kết nối MySQL. Hãy sửa user/password trong db.properties.");
            System.err.println("Chi tiết: " + error.getMessage());
            return;
        }

        ExecutorService workers = Executors.newFixedThreadPool(20);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.printf("Lab 2 FTP Server: ftp://localhost:%d%n", port);
            System.out.println("FTP root: " + root);
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = server.accept();
                client.setSoTimeout(120_000);
                workers.execute(new ControlHandler(client, database, root));
            }
        } finally {
            workers.shutdownNow();
        }
    }
}
</file>

<file path="lap2/src/lab2/server/UserStore.java">
package lab2.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UserStore {
    private final Path root;
    private Path current;

    public UserStore(Path ftpRoot, String homeDirectory) throws IOException {
        root = ftpRoot.resolve(homeDirectory).toAbsolutePath().normalize();
        Files.createDirectories(root);
        current = root;
    }

    public Path resolve(String ftpPath) throws IOException {
        String clean = ftpPath == null ? "" : ftpPath.replace('\\', '/');
        Path candidate = clean.startsWith("/")
                ? root.resolve(clean.substring(1))
                : current.resolve(clean);
        candidate = candidate.normalize();
        if (!candidate.startsWith(root)) {
            throw new IOException("Đường dẫn nằm ngoài thư mục người dùng");
        }
        return candidate;
    }

    public void changeDirectory(String path) throws IOException {
        Path target = resolve(path);
        if (!Files.isDirectory(target)) {
            throw new IOException("Thư mục không tồn tại");
        }
        current = target;
    }

    public String workingDirectory() {
        Path relative = root.relativize(current);
        return relative.getNameCount() == 0 ? "/" : "/" + relative.toString().replace('\\', '/');
    }

    public Path current() {
        return current;
    }
}
</file>

<file path="lap2/src/lab2/test/FtpIntegrationTest.java">
package lab2.test;

import lab2.client.FtpClientSession;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class FtpIntegrationTest {
    private FtpIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2121;
        byte[] expected = ("Lab 2 FTP integration test\nTiếng Việt\n").getBytes(StandardCharsets.UTF_8);
        Path source = Files.createTempFile("lab2-upload-", ".txt");
        Path downloaded = Files.createTempFile("lab2-download-", ".txt");
        String remoteName = "integration-" + System.currentTimeMillis() + ".txt";
        Files.write(source, expected);

        try (FtpClientSession client = new FtpClientSession(System.out::println)) {
            client.connect(host, port, "alice", "123456");
            client.upload(source, remoteName);
            boolean listed = client.list().stream().anyMatch(entry -> entry.name().equals(remoteName));
            require(listed, "LIST không thấy file vừa upload");
            client.download(remoteName, downloaded);
            require(Arrays.equals(expected, Files.readAllBytes(downloaded)), "RETR không khớp byte");
            client.delete(remoteName, false);
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(downloaded);
        }
        System.out.println("TẤT CẢ KIỂM THỬ FTP ĐÃ THÀNH CÔNG.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
</file>

<file path="lap2/.gitignore">
out/
lib/
db.properties
ftp-root/*/*
!ftp-root/*/.gitkeep
*.downloading
*.uploading
</file>

<file path="lap2/compile.ps1">
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$version = "9.5.0"
$jar = "lib\mysql-connector-j-$version.jar"
if (-not (Test-Path $jar)) {
    New-Item -ItemType Directory -Force lib | Out-Null
    $url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$version/mysql-connector-j-$version.jar"
    Write-Host "Dang tai MySQL Connector/J $version..."
    Invoke-WebRequest -Uri $url -OutFile $jar
}

New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Path src -Recurse -Filter *.java
javac --release 17 -encoding UTF-8 -Xlint:all -d out $sources.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Bien dich that bai."
}
Write-Host "Bien dich thanh cong."
</file>

<file path="lap2/db.properties.example">
db.url=jdbc:mysql://localhost:3306
db.name=lab2_ftp
db.user=root
db.password=YOUR_MYSQL_PASSWORD
</file>

<file path="lap2/README.md">
# Lab 2 - FTP Server kèm FTP Client bằng Java

Ứng dụng tự cài đặt giao thức FTP trên TCP Socket, gồm FTP Server có MySQL và FTP Client Swing hai cột tương tự FileZilla. Không dùng Apache FtpServer hoặc thư viện FTP cấp cao.

## Chức năng

- Đăng nhập bằng `USER` / `PASS`, tài khoản lưu trong MySQL.
- Danh sách file, đổi thư mục, tạo/xóa thư mục và xóa file.
- Upload (`STOR`) và download (`RETR`) mọi loại file ở chế độ binary.
- Hỗ trợ data connection **PASV** và **PORT**; client đi kèm dùng PASV.
- Ghi nhật ký upload/download vào bảng `transfer_logs`.
- Mỗi user có home riêng và không thể dùng `..` để thoát ra ngoài.
- Nhiều client đồng thời qua thread pool.

## Kiến trúc FTP

FTP dùng hai kết nối TCP độc lập:

```text
FTP Client ── TCP control :2121 ──> FTP Server
           USER, PASS, PWD, LIST, RETR, STOR...

FTP Client ── TCP data tạm thời ──> FTP Server (PASV)
           danh sách thư mục hoặc byte của file
```

- **Control connection** tồn tại suốt phiên và trao đổi lệnh/response mã 3 chữ số.
- **Data connection** được mở riêng cho từng `LIST`, `RETR` hoặc `STOR`, rồi đóng.
- **PASV:** server mở một cổng tạm, client chủ động kết nối. Dễ chạy qua NAT/firewall hơn.
- **PORT:** client mở cổng và yêu cầu server chủ động kết nối ngược lại.
- TCP phù hợp chuyển file vì đảm bảo đủ byte, đúng thứ tự và tự truyền lại khi mất gói.

## Yêu cầu

- JDK 17 trở lên.
- MySQL Server chạy tại `localhost:3306`.
- PowerShell và kết nối Internet trong lần biên dịch đầu để tải MySQL Connector/J.

## Chuẩn bị MySQL

Sửa `db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306
db.name=lab2_ftp
db.user=root
db.password=MAT_KHAU_MYSQL
```

Server tự tạo database `lab2_ftp`, hai bảng `users`, `transfer_logs`, cùng hai tài khoản:

- `alice` / `123456`
- `bob` / `123456`

Mật khẩu để dạng rõ chỉ nhằm phục vụ bài thực hành và demo giao thức, không dùng cho hệ thống thật.

## Chạy

Terminal thứ nhất, tại thư mục `lap2`:

```powershell
.\run-server.ps1
```

Terminal thứ hai (có thể mở nhiều client):

```powershell
.\run-client.ps1
```

Nhập `localhost`, cổng `2121`, tài khoản `alice` hoặc `bob`. Cột trái là máy local, cột phải là server. Nhấp đúp thư mục để đi vào.

Nếu PowerShell chặn script:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-server.ps1
```

## Kiểm thử tích hợp

Khởi động server trước, sau đó:

```powershell
.\compile.ps1
java -cp out lab2.test.FtpIntegrationTest
```

Test đăng nhập, upload, `LIST`, download, so sánh byte và xóa file. Kết quả cuối:

```text
TẤT CẢ KIỂM THỬ FTP ĐÃ THÀNH CÔNG.
```

## Kết nối bằng FileZilla thật

Tạo site với:

- Protocol: FTP
- Host: `127.0.0.1`
- Port: `2121`
- Encryption: Only use plain FTP
- Logon Type: Normal
- User/password: `alice` / `123456`
- Transfer mode: Passive

Server hỗ trợ: `USER`, `PASS`, `QUIT`, `SYST`, `FEAT`, `OPTS`, `PWD`, `CWD`, `CDUP`, `TYPE`, `PASV`, `PORT`, `LIST`, `NLST`, `RETR`, `STOR`, `DELE`, `MKD`, `RMD`, `SIZE`, `NOOP`.

## Kịch bản trình bày

1. Chạy server và hai client đăng nhập bằng `alice`, `bob`.
2. Upload một file từ cột local; mở client khác để chứng minh home tách riêng.
3. Download file và so sánh nội dung.
4. Tạo/xóa thư mục, quan sát lệnh và response FTP ở khung log.
5. Mở MySQL:

```sql
USE lab2_ftp;
SELECT username, command, filename, bytes, created_at
FROM transfer_logs ORDER BY id DESC;
```

6. Kết nối FileZilla thật để chứng minh server sử dụng FTP chuẩn.
7. Giải thích control channel, data channel và khác nhau giữa PASV/PORT.

## Giới hạn có chủ đích

Không hỗ trợ FTPS/TLS, resume (`REST`), IPv6, symbolic link hoặc phân quyền hệ điều hành. Cổng `2121` được dùng thay cổng chuẩn `21` để không cần quyền Administrator.
</file>

<file path="lap2/run-client.ps1">
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\compile.ps1
java -cp out lab2.client.FtpClient
</file>

<file path="lap2/run-server.ps1">
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\compile.ps1
$jar = Get-ChildItem "lib\mysql-connector-j-*.jar" | Select-Object -First 1
java -cp "out;$($jar.FullName)" lab2.server.FtpServer 2121 ftp-root
</file>

</files>
