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
        upload(localFile, remoteName, TransferProgress.NONE);
    }

    public void upload(Path localFile, String remoteName, TransferProgress progress) throws IOException {
        Socket data = openPassive();
        expect(command("STOR " + remoteName, false), 150, 125);
        try (data; var input = Files.newInputStream(localFile); var output = data.getOutputStream()) {
            transfer(input, output, Files.size(localFile), progress);
        }
        expect(readReply(), 226);
    }

    public void download(String remoteName, Path localFile) throws IOException {
        download(remoteName, localFile, 0, TransferProgress.NONE);
    }

    public void download(String remoteName, Path localFile, long expectedSize, TransferProgress progress)
            throws IOException {
        Socket data = openPassive();
        expect(command("RETR " + remoteName, false), 150, 125);
        Path temporary = localFile.resolveSibling(localFile.getFileName() + ".downloading");
        try (data; var input = data.getInputStream(); var output = Files.newOutputStream(temporary)) {
            transfer(input, output, expectedSize, progress);
        } catch (IOException error) {
            Files.deleteIfExists(temporary);
            throw error;
        }
        try {
            Files.move(temporary, localFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    private static void transfer(
            java.io.InputStream input,
            java.io.OutputStream output,
            long total,
            TransferProgress progress) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long transferred = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
            transferred += count;
            progress.update(transferred, total);
        }
        output.flush();
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
        String value = line == null ? "" : line.trim();
        if (value.isEmpty()) {
            return null;
        }

        boolean directory = value.startsWith("d");
        String[] parts = value.split("\\s+");
        if (parts.length < 9) {
            return null;
        }

        long size;
        try {
            size = Long.parseLong(parts[4]);
        } catch (NumberFormatException error) {
            size = 0;
        }

        // Sau size là ngày giờ rồi mới tới tên file.
        // Tiếng Anh: "Oct 25 14:30" (3 token). Tiếng Việt: "thg 10 25 14:30" (4 token).
        int nameIndex = parts[5].equalsIgnoreCase("thg") ? 9 : 8;
        if (parts.length <= nameIndex) {
            return null;
        }

        String name = String.join(" ", java.util.Arrays.copyOfRange(parts, nameIndex, parts.length));
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return null;
        }
        return new RemoteEntry(name, directory, size);
    }

    @Override
    public void close() {
        if (writer != null && isConnected()) {
            try {
                logger.accept("> QUIT");
                writer.write("QUIT\r\n");
                writer.flush();
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

    @FunctionalInterface
    public interface TransferProgress {
        TransferProgress NONE = (transferred, total) -> { };

        void update(long transferred, long total);
    }

    private record Reply(int code, String text) {
    }
}
