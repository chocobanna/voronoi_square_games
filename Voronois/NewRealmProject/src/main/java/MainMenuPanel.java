package newrealm;

import newrealm.diagram.VoronoiDiagramPanel;
import newrealm.config.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainMenuPanel extends JPanel {
    private JTextField widthField;
    private JTextField heightField;
    private JTextField densityField;
    private JTextField iterationsField;

    public MainMenuPanel(JFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Map Width
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Map Width:"), gbc);
        widthField = new JTextField("800", 10);
        gbc.gridx = 1;
        add(widthField, gbc);

        // Map Height
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Map Height:"), gbc);
        heightField = new JTextField("600", 10);
        gbc.gridx = 1;
        add(heightField, gbc);

        // Point Density
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Point Density:"), gbc);
        densityField = new JTextField(String.valueOf(Config.POINT_DENSITY), 10);
        gbc.gridx = 1;
        add(densityField, gbc);

        // Lloyd Iterations
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Lloyd Iterations:"), gbc);
        iterationsField = new JTextField(String.valueOf(Config.LLOYD_ITERATIONS), 10);
        gbc.gridx = 1;
        add(iterationsField, gbc);

        // Start Button
        JButton startButton = new JButton("Start");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(startButton, gbc);

        startButton.addActionListener((ActionEvent e) -> {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                double density = Double.parseDouble(densityField.getText());
                int iterations = Integer.parseInt(iterationsField.getText());

                // Instantiate the Voronoi diagram panel using the specified options.
                VoronoiDiagramPanel diagramPanel = new VoronoiDiagramPanel(width, height, density, iterations);

                // Replace the content pane with the diagram panel.
                frame.getContentPane().removeAll();
                frame.getContentPane().add(diagramPanel);
                frame.revalidate();
                frame.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numeric values.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
