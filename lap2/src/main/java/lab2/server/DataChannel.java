package lab2.server;

import lab2.common.FtpProtocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public final class DataChannel implements Closeable {
    private static final AtomicInteger NEXT_PASSIVE_PORT =
            new AtomicInteger(FtpProtocol.PASSIVE_PORT_MIN);

    private ServerSocket passiveSocket;
    private InetSocketAddress activeAddress;

    public int enterPassive(InetAddress bindAddress) throws IOException {
        close();
        int portCount = FtpProtocol.PASSIVE_PORT_MAX - FtpProtocol.PASSIVE_PORT_MIN + 1;
        IOException lastError = null;
        for (int attempt = 0; attempt < portCount; attempt++) {
            int port = nextPassivePort();
            ServerSocket candidate = new ServerSocket();
            try {
                candidate.bind(new InetSocketAddress(bindAddress, port));
                candidate.setSoTimeout(15_000);
                passiveSocket = candidate;
                return port;
            } catch (IOException error) {
                lastError = error;
                candidate.close();
            }
        }
        throw new IOException("Không còn cổng PASV trống trong dải "
                + FtpProtocol.PASSIVE_PORT_MIN + "-" + FtpProtocol.PASSIVE_PORT_MAX, lastError);
    }

    private static int nextPassivePort() {
        return NEXT_PASSIVE_PORT.getAndUpdate(current ->
                current >= FtpProtocol.PASSIVE_PORT_MAX
                        ? FtpProtocol.PASSIVE_PORT_MIN
                        : current + 1);
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
