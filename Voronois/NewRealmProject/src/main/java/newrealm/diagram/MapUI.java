package newrealm.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public class MapUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Main frame for the map and controls.
            JFrame frame = new JFrame("World Map Generator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            
            // --- Top Bar: Parameter Controls ---
            JPanel topPanel = new JPanel(new FlowLayout());
            topPanel.setBackground(java.awt.Color.LIGHT_GRAY);
            topPanel.setPreferredSize(new Dimension(1000, 50));
            
            JLabel waterLabel = new JLabel("Water Level:");
            JSlider waterSlider = new JSlider(0, 100, 50); // 0->100 maps to waterThreshold 0.0-1.0
            waterSlider.setMajorTickSpacing(10);
            waterSlider.setPaintTicks(true);
            waterSlider.setPaintLabels(true);
            topPanel.add(waterLabel);
            topPanel.add(waterSlider);
            
            JLabel densityLabel = new JLabel("Point Density:");
            JSlider densitySlider = new JSlider(1, 100, 20); // e.g., 20 -> 0.0002 when scaled
            densitySlider.setMajorTickSpacing(10);
            densitySlider.setPaintTicks(true);
            densitySlider.setPaintLabels(true);
            topPanel.add(densityLabel);
            topPanel.add(densitySlider);
            
            JLabel lloydLabel = new JLabel("Lloyd Iterations:");
            JSlider lloydSlider = new JSlider(0, 10, 3);
            lloydSlider.setMajorTickSpacing(2);
            lloydSlider.setPaintTicks(true);
            lloydSlider.setPaintLabels(true);
            topPanel.add(lloydLabel);
            topPanel.add(lloydSlider);
            
            JButton regenerateButton = new JButton("Regenerate Map");
            topPanel.add(regenerateButton);
            
            // --- Bottom Bar: Status Bar ---
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottomPanel.setBackground(java.awt.Color.LIGHT_GRAY);
            bottomPanel.setPreferredSize(new Dimension(1000, 30));
            JLabel statusLabel = new JLabel("Biome: ");
            bottomPanel.add(statusLabel);
            
            // --- Center: Map Panel ---
            int mapWidth = 800;
            int mapHeight = 600;
            double defaultWaterThreshold = waterSlider.getValue() / 100.0;
            double defaultPointDensity = densitySlider.getValue() / 100000.0; // e.g., 20 -> 0.0002
            int defaultLloydIterations = lloydSlider.getValue();
            
            VoronoiDiagramPanel mapPanel = new VoronoiDiagramPanel(mapWidth, mapHeight, defaultPointDensity, defaultLloydIterations);
            mapPanel.setWaterThreshold(defaultWaterThreshold);
            mapPanel.setStatusLabel(statusLabel);
            JScrollPane scrollPane = new JScrollPane(mapPanel);
            scrollPane.setPreferredSize(new Dimension(mapWidth, mapHeight));
            
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);
            frame.setSize(1000, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            // --- Separate Civilization Window ---
            JFrame civFrame = new JFrame("Civilization Simulation");
            civFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            CivilizationOverlay civOverlay = new CivilizationOverlay(mapWidth, mapHeight);
            civFrame.add(civOverlay);
            civFrame.pack();
            // Position the civilization window to the right of the main frame.
            civFrame.setLocation(frame.getX() + frame.getWidth(), frame.getY());
            civFrame.setAlwaysOnTop(true);
            civFrame.setVisible(true);
            
            // --- Regenerate Action: update parameters and reset civilization simulation ---
            ActionListener regenerateAction = e -> {
                double newWaterThreshold = waterSlider.getValue() / 100.0;
                double newPointDensity = densitySlider.getValue() / 100000.0;
                int newLloydIterations = lloydSlider.getValue();
                mapPanel.setWaterThreshold(newWaterThreshold);
                // (For a complete implementation, add setters to update pointDensity and lloydIterations in mapPanel.)
                // Reset the civilization simulation in its own window.
                civFrame.getContentPane().removeAll();
                CivilizationOverlay newCivOverlay = new CivilizationOverlay(mapWidth, mapHeight);
                civFrame.add(newCivOverlay);
                civFrame.revalidate();
                civFrame.repaint();
            };
            regenerateButton.addActionListener(regenerateAction);
            waterSlider.addChangeListener(e -> regenerateAction.actionPerformed(null));
            densitySlider.addChangeListener(e -> regenerateAction.actionPerformed(null));
            lloydSlider.addChangeListener(e -> regenerateAction.actionPerformed(null));
        });
    }
}
