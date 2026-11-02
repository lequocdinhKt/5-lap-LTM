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
