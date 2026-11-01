import java.util.List;

import models.Mail;
import network.LocalMailServer;
import network.MailProtocol;
import network.MailServerType;

public class LocalMailIntegrationTest {
    public static void main(String[] args) throws Exception {
        try (LocalMailServer server = new LocalMailServer()) {
            server.start();

            Mail sent = new Mail(
                    LocalMailServer.USER_1,
                    LocalMailServer.USER_2,
                    "Kiểm tra localhost",
                    "SMTP và POP3 hoạt động."
            );
            MailProtocol.sendMail(MailServerType.LOCALHOST, sent, LocalMailServer.PASSWORD);

            List<Mail> inbox = MailProtocol.readMails(
                    MailServerType.LOCALHOST,
                    LocalMailServer.USER_2,
                    LocalMailServer.PASSWORD
            );

            if (inbox.size() != 1 || !"Kiểm tra localhost".equals(inbox.get(0).subject)) {
                throw new AssertionError("Không nhận đúng thư qua POP3 localhost.");
            }
            System.out.println("PASS: SMTP và POP3 localhost hoạt động.");
        }
    }
}
