package newrealm;

import newrealm.diagram.VoronoiDiagramPanel;
import javax.swing.*;

public class NewRealm {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("New Realm");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Start with the main menu panel.
            MainMenuPanel mainMenu = new MainMenuPanel(frame);
            frame.getContentPane().add(mainMenu);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
