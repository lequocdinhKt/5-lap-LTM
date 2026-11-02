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
