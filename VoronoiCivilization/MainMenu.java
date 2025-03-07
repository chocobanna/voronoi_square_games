import java.awt.*;
import javax.swing.*;

public class MainMenu extends JFrame {
    private TerrainVoronoiDiagram voronoiPanel;

    public MainMenu() {
        super("Voronoi Diagram Setup");

        voronoiPanel = new TerrainVoronoiDiagram();
        
        // Label to display area and biome.
        JLabel areaLabel = new JLabel("Area: ");
        voronoiPanel.setAreaLabel(areaLabel);

        setLayout(new BorderLayout());
        add(new JScrollPane(voronoiPanel), BorderLayout.CENTER);
        add(areaLabel, BorderLayout.SOUTH);

        // Menu bar with a Setup option.
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
     * Opens a dialog to adjust parameters: number of sites, margin, map width, and map height.
     */
    private void openSetupDialog() {
        JDialog dialog = new JDialog(this, "Setup Parameters", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setSize(300, 250);
        dialog.setLocationRelativeTo(this);

        JLabel sitesLabel = new JLabel("Number of Sites:");
        JTextField sitesField = new JTextField(String.valueOf(10));
        JLabel marginLabel = new JLabel("Margin:");
        JTextField marginField = new JTextField(String.valueOf(50));
        JLabel widthLabel = new JLabel("Map Width:");
        JTextField widthField = new JTextField(String.valueOf(800));
        JLabel heightLabel = new JLabel("Map Height:");
        JTextField heightField = new JTextField(String.valueOf(800));

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            try {
                int newSites = Integer.parseInt(sitesField.getText().trim());
                int newMargin = Integer.parseInt(marginField.getText().trim());
                int newWidth = Integer.parseInt(widthField.getText().trim());
                int newHeight = Integer.parseInt(heightField.getText().trim());
                voronoiPanel.updateParameters(newSites, newMargin, newWidth, newHeight);
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
        dialog.add(widthLabel);
        dialog.add(widthField);
        dialog.add(heightLabel);
        dialog.add(heightField);
        dialog.add(applyButton);
        dialog.add(cancelButton);

        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}
