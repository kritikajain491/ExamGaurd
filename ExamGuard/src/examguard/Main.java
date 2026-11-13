package examguard;

import examguard.service.DataStore;
import examguard.ui.LoginFrame;
import examguard.ui.Theme;
import javax.swing.SwingUtilities;

/** Entry point of ExamGuard. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            DataStore.get().load();
            new LoginFrame().setVisible(true);
        });
    }
}
