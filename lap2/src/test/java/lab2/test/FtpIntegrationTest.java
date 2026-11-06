package lab2.test;

import lab2.service.FtpService;

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

        try (FtpService client = new FtpService(System.out::println)) {
            client.connect(host, port, "alice", "123456");
            client.upload(source, remoteName, null);
            boolean listed = client.browse().entries().stream()
                    .anyMatch(entry -> entry.name().equals(remoteName));
            require(listed, "LIST không thấy file vừa upload");
            client.download(remoteName, downloaded, expected.length, null);
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
