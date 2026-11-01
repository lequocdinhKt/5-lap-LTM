package lab3;

import java.util.List;

import lab3.model.Mail;
import lab3.network.LocalMailServer;
import lab3.network.MailProtocol;
import lab3.network.MailServerType;

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
