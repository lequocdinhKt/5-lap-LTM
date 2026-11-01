package lab4.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Chỉ đọc tài nguyên nằm thật sự bên trong thư mục www. */
public final class FileService {
    private final Path webRoot;

    public FileService(Path webRoot) throws IOException {
        this.webRoot = webRoot.toAbsolutePath().normalize().toRealPath();
        if (!Files.isDirectory(this.webRoot)) {
            throw new IOException("Web root không phải thư mục: " + this.webRoot);
        }
    }

    public Resource load(String decodedPath)
            throws IOException, HttpRequest.BadRequestException {
        String path = "/".equals(decodedPath) ? "/index.html" : decodedPath;
        if (path == null || !path.startsWith("/")) {
            throw new HttpRequest.BadRequestException("Đường dẫn tài nguyên không hợp lệ");
        }

        try {
            String relative = path.substring(1);
            Path candidate = webRoot.resolve(relative).normalize();
            if (!candidate.startsWith(webRoot)) {
                throw new HttpRequest.BadRequestException("Từ chối truy cập ngoài thư mục www");
            }
            if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                throw new NoSuchFileException(path);
            }

            // toRealPath ngăn trường hợp symlink bên trong www trỏ ra ngoài.
            Path realFile = candidate.toRealPath();
            if (!realFile.startsWith(webRoot)) {
                throw new HttpRequest.BadRequestException("Từ chối symbolic link ra ngoài www");
            }
            return new Resource(Files.readAllBytes(realFile), MimeTypes.forPath(realFile));
        } catch (InvalidPathException exception) {
            throw new HttpRequest.BadRequestException("Đường dẫn tài nguyên không hợp lệ");
        }
    }

    public record Resource(byte[] bytes, String contentType) {
        public Resource {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
