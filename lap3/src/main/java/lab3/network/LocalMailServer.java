package lab3.network;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class LocalMailServer implements AutoCloseable {
    public static final String USER_1 = "user1@localhost";
    public static final String USER_2 = "user2@localhost";
    public static final String PASSWORD = "123456";
    public static final int SMTP_PORT = 3025;
    public static final int POP3_PORT = 3110;

    private final GreenMail greenMail;

    public LocalMailServer() {
        this(setting("LAB3_LOCAL_MAIL_BIND", "127.0.0.1"));
    }

    public LocalMailServer(String bindAddress) {
        ServerSetup smtp = new ServerSetup(SMTP_PORT, bindAddress, ServerSetup.PROTOCOL_SMTP);
        ServerSetup pop3 = new ServerSetup(POP3_PORT, bindAddress, ServerSetup.PROTOCOL_POP3);
        greenMail = new GreenMail(new ServerSetup[]{smtp, pop3});
    }

    public void start() {
        greenMail.start();
        greenMail.setUser(USER_1, USER_1, PASSWORD);
        greenMail.setUser(USER_2, USER_2, PASSWORD);
    }

    @Override
    public void close() {
        greenMail.stop();
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
