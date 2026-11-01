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
