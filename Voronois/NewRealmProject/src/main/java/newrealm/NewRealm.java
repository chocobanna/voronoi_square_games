package newrealm;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import newrealm.diagram.MainMenuPanel;

public class NewRealm {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("New Realm");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Use the no-argument constructor for MainMenuPanel.
            MainMenuPanel mainMenu = new MainMenuPanel();
            frame.getContentPane().add(mainMenu);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
