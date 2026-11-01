package lab3.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lab3.model.Mail;

public final class MailProtocol {
    private MailProtocol() {
    }

    public static void sendMail(MailServerType serverType, Mail mail, String password) throws Exception {
        boolean gmail = serverType == MailServerType.GMAIL;
        String host = gmail ? "smtp.gmail.com" : localMailHost();
        int port = gmail ? 587 : LocalMailServer.SMTP_PORT;

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", String.valueOf(gmail));

        Session session = Session.getInstance(properties);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mail.sender));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.receiver));
        message.setSubject(mail.subject, "UTF-8");
        message.setText(mail.content, "UTF-8");

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(host, port, mail.sender, password);
            transport.sendMessage(message, message.getAllRecipients());
        }
    }

    public static List<Mail> readMails(MailServerType serverType, String email, String password) throws Exception {
        boolean gmail = serverType == MailServerType.GMAIL;
        String protocol = gmail ? "pop3s" : "pop3";
        String host = gmail ? "pop.gmail.com" : localMailHost();
        int port = gmail ? 995 : LocalMailServer.POP3_PORT;

        Properties properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        if (gmail) {
            properties.put("mail.pop3s.ssl.enable", "true");
        }

        Store store = null;
        Folder inbox = null;
        List<Mail> mails = new ArrayList<>();
        try {
            store = Session.getInstance(properties).getStore(protocol);
            store.connect(host, port, email, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            for (Message message : inbox.getMessages()) {
                mails.add(new Mail(
                        InternetAddress.toString(message.getFrom()),
                        email,
                        message.getSubject() == null ? "" : message.getSubject(),
                        readContent(message)
                ));
            }
            return mails;
        } finally {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }

    private static String readContent(Part part) throws Exception {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            return String.valueOf(part.getContent());
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String content = readContent(multipart.getBodyPart(i));
                if (!content.isBlank()) {
                    return content;
                }
            }
        }
        return "";
    }

    private static String localMailHost() {
        String value = System.getenv("LAB3_LOCAL_MAIL_HOST");
        return value == null || value.isBlank() ? "127.0.0.1" : value.trim();
    }
}
