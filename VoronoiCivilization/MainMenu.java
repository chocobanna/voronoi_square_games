import java.awt.*;
import javax.swing.*;

public class MainMenu extends JFrame {
    private TerrainVoronoiDiagram voronoiPanel;

    public MainMenu() {
        super("Voronoi Diagram Setup");

        voronoiPanel = new TerrainVoronoiDiagram();
        
        // Create a label to display the area.
        JLabel areaLabel = new JLabel("Area: ");
        voronoiPanel.setAreaLabel(areaLabel);

        // Set up the frame layout.
        setLayout(new BorderLayout());
        // Place the diagram panel in a scroll pane (optional – panning/zooming are handled internally).
        add(new JScrollPane(voronoiPanel), BorderLayout.CENTER);
        add(areaLabel, BorderLayout.SOUTH);

        // Create menu bar with a Setup option.
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem setupItem = new JMenuItem("Setup Voronoi");
        setupItem.addActionListener(e -> openSetupDialog());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(setupItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Open a dialog to adjust parameters.
     */
    private void openSetupDialog() {
        JDialog dialog = new JDialog(this, "Setup Parameters", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JLabel sitesLabel = new JLabel("Number of Sites:");
        JTextField sitesField = new JTextField(String.valueOf(10)); // default value
        JLabel marginLabel = new JLabel("Margin:");
        JTextField marginField = new JTextField(String.valueOf(50)); // default value

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            try {
                int newSites = Integer.parseInt(sitesField.getText().trim());
                int newMargin = Integer.parseInt(marginField.getText().trim());
                voronoiPanel.updateParameters(newSites, newMargin);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(sitesLabel);
        dialog.add(sitesField);
        dialog.add(marginLabel);
        dialog.add(marginField);
        dialog.add(applyButton);
        dialog.add(cancelButton);

        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}
