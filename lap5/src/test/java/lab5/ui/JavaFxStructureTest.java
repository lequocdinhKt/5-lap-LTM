package lab5.ui;

import javafx.application.Application;
import lab5.client.ClientLauncher;
import lab5.server.ServerLauncher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxStructureTest {
    @Test
    void bothLaunchersAreJavaFxApplications() {
        assertTrue(Application.class.isAssignableFrom(ClientLauncher.class));
        assertTrue(Application.class.isAssignableFrom(ServerLauncher.class));
    }

    @Test
    void designSystemStylesheetIsPackaged() {
        assertNotNull(getClass().getResource("/css/lab5.css"));
    }
}
