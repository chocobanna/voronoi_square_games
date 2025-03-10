package newrealm;

import newrealm.diagram.VoronoiDiagramPanel;
import javax.swing.*;

public class NewRealm {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("New Realm - Voronoi Diagram with Lloyd Relaxation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Define the dimensions of your world map.
            int width = 800;
            int height = 600;
            VoronoiDiagramPanel panel = new VoronoiDiagramPanel(width, height);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
