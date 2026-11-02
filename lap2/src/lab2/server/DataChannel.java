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
