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
import java.util.Locale;

public final class ControlHandler implements Runnable {
    private static final DateTimeFormatter LIST_TIME =
            DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

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
