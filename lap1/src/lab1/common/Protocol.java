package lab1.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cac hang so va dinh dang goi tin dung chung giua client va server. */
public final class Protocol {
    public static final int DEFAULT_TCP_PORT = 5000;
    public static final int DEFAULT_UDP_FILE_PORT = 5001;
    public static final int DEFAULT_UDP_TEXT_PORT = 5002;
    public static final int DEFAULT_UDP_PORT = DEFAULT_UDP_FILE_PORT;
    public static final int MAX_CLIENTS = 10;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MAX_TEXT_LENGTH = 4_000;
    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    public static final int UDP_CHUNK_SIZE = 4 * 1024;
    public static final int UDP_ACK_TIMEOUT_MS = 500;
    public static final int UDP_MAX_RETRIES = 8;

    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_ERROR = "LOGIN_ERROR";
    public static final String USER_LIST = "USER_LIST";
    public static final String TEXT = "TEXT";
    public static final String SYSTEM = "SYSTEM";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String QUIT = "QUIT";
    public static final String CREATE_GROUP = "CREATE_GROUP";
    public static final String UPDATE_GROUP = "UPDATE_GROUP";
    public static final String DELETE_GROUP = "DELETE_GROUP";
    public static final String LEAVE_GROUP = "LEAVE_GROUP";
    public static final String REQUEST_GROUP_MEMBERS = "REQUEST_GROUP_MEMBERS";
    public static final String GROUP_CATALOG = "GROUP_CATALOG";
    public static final String GROUP_MEMBERS = "GROUP_MEMBERS";
    public static final String GROUP_REMOVED = "GROUP_REMOVED";
    public static final String GROUP_TEXT = "GROUP_TEXT";
    public static final String UDP_PORT = "UDP_PORT";
    public static final String FILE_OFFER = "FILE_OFFER";
    public static final String GROUP_FILE_OFFER = "GROUP_FILE_OFFER";
    public static final String FILE_READY = "FILE_READY";
    public static final String FILE_INCOMING = "FILE_INCOMING";
    public static final String FILE_DONE = "FILE_DONE";
    public static final String FILE_FAIL = "FILE_FAIL";

    public static final short UDP_MAGIC_CHUNK = (short) 0x1A01;
    public static final short UDP_MAGIC_ACK = (short) 0x1A02;
    public static final int UDP_TEXT_MAGIC = 0x4C315458; // L1TX
    public static final byte UDP_TEXT_VERSION = 1;
    public static final byte UDP_TEXT_MULTICAST = 1;
    public static final byte UDP_TEXT_BROADCAST = 2;
    public static final int MAX_UDP_TEXT_PACKET = 8 * 1024;

    private Protocol() {
    }

    public static void writeStringList(DataOutputStream output, List<String> values) throws IOException {
        output.writeInt(values.size());
        for (String value : values) {
            output.writeUTF(value);
        }
    }

    public static List<String> readStringList(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_CLIENTS) {
            throw new IOException("So phan tu khong hop le: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(input.readUTF());
        }
        return values;
    }

    public static byte[] encodeUdpText(UdpTextPacket packet) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(UDP_TEXT_MAGIC);
            output.writeByte(UDP_TEXT_VERSION);
            output.writeByte(packet.type());
            output.writeLong(packet.messageId());
            output.writeLong(packet.timestamp());
            writeUtf8(output, packet.groupId());
            writeUtf8(output, packet.sender());
            writeUtf8(output, packet.text());
        }
        byte[] result = bytes.toByteArray();
        if (result.length > MAX_UDP_TEXT_PACKET) {
            throw new IOException("Tin nhan UDP vuot qua " + MAX_UDP_TEXT_PACKET + " byte.");
        }
        return result;
    }

    public static UdpTextPacket decodeUdpText(byte[] data, int length) throws IOException {
        if (length <= 0 || length > MAX_UDP_TEXT_PACKET) {
            throw new IOException("Kich thuoc goi UDP text khong hop le.");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data, 0, length))) {
            if (input.readInt() != UDP_TEXT_MAGIC || input.readByte() != UDP_TEXT_VERSION) {
                throw new IOException("Sai magic/version UDP text.");
            }
            byte type = input.readByte();
            if (type != UDP_TEXT_MULTICAST && type != UDP_TEXT_BROADCAST) {
                throw new IOException("Sai loai UDP text.");
            }
            UdpTextPacket packet = new UdpTextPacket(type, input.readLong(), input.readLong(),
                    readUtf8(input), readUtf8(input), readUtf8(input));
            if (input.available() != 0 || packet.text().length() > MAX_TEXT_LENGTH) {
                throw new IOException("Noi dung UDP text khong hop le.");
            }
            return packet;
        }
    }

    private static void writeUtf8(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readUtf8(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_UDP_TEXT_PACKET) {
            throw new IOException("Chuoi UDP khong hop le.");
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Goi UDP text bi thieu du lieu.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record UdpTextPacket(byte type, long messageId, long timestamp,
                                String groupId, String sender, String text) {
    }
}
