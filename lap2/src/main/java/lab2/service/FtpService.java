package lab2.service;

import lab2.client.FtpClientSession;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Điều phối nghiệp vụ FTP giữa giao diện và phiên kết nối. */
public final class FtpService implements Closeable {
    private final Consumer<String> logger;
    private FtpClientSession session;

    public FtpService(Consumer<String> logger) {
        this.logger = logger == null ? ignored -> { } : logger;
    }

    public void connect(String host, int port, String username, String password) throws IOException {
        FtpClientSession candidate = new FtpClientSession(logger);
        try {
            candidate.connect(host, port, username, password);
            close();
            session = candidate;
        } catch (IOException error) {
            candidate.close();
            throw error;
        }
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public RemoteSnapshot browse() throws IOException {
        FtpClientSession current = requireSession();
        List<RemoteEntry> entries = current.list().stream()
                .map(entry -> new RemoteEntry(entry.name(), entry.directory(), entry.size()))
                .toList();
        return new RemoteSnapshot(current.pwd(), entries);
    }

    public void changeDirectory(String path) throws IOException {
        requireSession().changeDirectory(path);
    }

    public void upload(Path localFile, String remoteName, TransferProgress progress) throws IOException {
        requireSession().upload(localFile, remoteName, progress == null
                ? FtpClientSession.TransferProgress.NONE : progress::update);
    }

    public void download(String remoteName, Path localFile, long expectedSize,
                         TransferProgress progress) throws IOException {
        requireSession().download(remoteName, localFile, expectedSize, progress == null
                ? FtpClientSession.TransferProgress.NONE : progress::update);
    }

    public void createDirectory(String name) throws IOException {
        requireSession().makeDirectory(name);
    }

    public void delete(String name, boolean directory) throws IOException {
        requireSession().delete(name, directory);
    }

    private FtpClientSession requireSession() throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối FTP Server.");
        }
        return session;
    }

    @Override public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    public record RemoteSnapshot(String path, List<RemoteEntry> entries) {
        public RemoteSnapshot {
            entries = List.copyOf(entries);
        }
    }

    public record RemoteEntry(String name, boolean directory, long size) {
        @Override public String toString() {
            return (directory ? "[Thư mục] " : "") + name + (directory ? "" : "  (" + size + " B)");
        }
    }

    @FunctionalInterface
    public interface TransferProgress {
        void update(long transferred, long total);
    }
}
