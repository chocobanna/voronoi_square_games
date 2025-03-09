import javax.swing.*;
import main.MainMenuPanel;

public class Runner {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame("Voronoi Conquest - Main Menu");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MainMenuPanel menu = new MainMenuPanel(mainFrame);
            mainFrame.getContentPane().add(menu);
            mainFrame.pack();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });
    }
}
