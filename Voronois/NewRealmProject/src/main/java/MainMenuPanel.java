package newrealm.diagram;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * MainMenuPanel creates a simple user interface that contains the planetary map panel
 * and a slider to adjust the smoothness (smoothing iterations) of the elevation map.
 *
 * You can further extend this panel by adding additional controls (e.g., water threshold,
 * rotation speed, or status displays) to enhance the user experience.
 */
public class MainMenuPanel extends JPanel {

    public MainMenuPanel() {
        // Use BorderLayout to separate the map from the controls.
        setLayout(new BorderLayout());

        // Create the map panel (for example, an 800x800 panel; adjust parameters as needed).
        VoronoiDiagramPanel mapPanel = new VoronoiDiagramPanel(800, 800, 0.001, 0);
        add(mapPanel, BorderLayout.CENTER);

        // Create a control panel for user adjustments.
        JPanel controlPanel = new JPanel(new FlowLayout());

        // Smoothing slider: lets the user adjust the number of smoothing iterations.
        JLabel smoothLabel = new JLabel("Smoothness:");
        JSlider smoothSlider = new JSlider(0, 10, 2);
        smoothSlider.setMajorTickSpacing(2);
        smoothSlider.setMinorTickSpacing(1);
        smoothSlider.setPaintTicks(true);
        smoothSlider.setPaintLabels(true);
        smoothSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = smoothSlider.getValue();
                mapPanel.setSmoothingIterations(value);
            }
        });

        controlPanel.add(smoothLabel);
        controlPanel.add(smoothSlider);

        // (Optional) Add additional controls below. For example:
        // - A slider to adjust water threshold.
        // - Buttons to reset rotation or zoom.
        // - A status label showing current rotation/zoom details.
        // JLabel statusLabel = new JLabel("Status:");
        // controlPanel.add(statusLabel);
        // mapPanel.setStatusLabel(statusLabel);

        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates and displays the main application window.
     */
    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Planetary Map Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MainMenuPanel());
        frame.pack();
        frame.setLocationRelativeTo(null); // Center on screen.
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Schedule a job for the event dispatch thread: creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
