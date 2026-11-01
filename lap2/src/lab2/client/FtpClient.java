package lab2.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class FtpClient {
    private FtpClient() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FileZillaFrame().setVisible(true);
        });
    }
}
