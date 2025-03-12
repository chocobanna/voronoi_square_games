import java.awt.*;
import javax.swing.*;

public class MainMenu extends JFrame {
    private TerrainVoronoiDiagram voronoiPanel;
    private JTextArea civInfoArea;

    public MainMenu() {
        super("Voronoi Civilization Setup");

        voronoiPanel = new TerrainVoronoiDiagram();
        
        // Label for simulation messages.
        JLabel infoLabel = new JLabel("Welcome to Voronoi Civilization.");
        voronoiPanel.setAreaLabel(infoLabel);

        // Top toolbar.
        JToolBar toolBar = new JToolBar();
        JButton seedButton = new JButton("Place Civilization Seed");
        seedButton.addActionListener(e -> voronoiPanel.setPlacingSeed(true));
        toolBar.add(seedButton);

        // Bottom panel with simulation controls.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton timeButton = new JButton("Advance Time");
        timeButton.addActionListener(e -> {
            voronoiPanel.advanceTime();
            updateCivInfo();
        });
        bottomPanel.add(infoLabel, BorderLayout.WEST);
        bottomPanel.add(timeButton, BorderLayout.EAST);

        // Civilization info area.
        civInfoArea = new JTextArea(5, 20);
        civInfoArea.setEditable(false);
        JScrollPane civScroll = new JScrollPane(civInfoArea);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(voronoiPanel), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(civScroll, BorderLayout.EAST);

        // Menu bar.
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

    private void updateCivInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Civilization Seeds:\n");
        for (String info : getCivSeedInfo()) {
            sb.append(info).append("\n");
        }
        civInfoArea.setText(sb.toString());
    }
    
    private java.util.List<String> getCivSeedInfo() {
        java.util.List<String> list = new java.util.ArrayList<>();
        try {
            java.lang.reflect.Field field = voronoiPanel.getClass().getDeclaredField("civSeeds");
            field.setAccessible(true);
            java.util.List<?> seeds = (java.util.List<?>) field.get(voronoiPanel);
            for (Object obj : seeds) {
                java.lang.reflect.Field locField = obj.getClass().getDeclaredField("location");
                locField.setAccessible(true);
                Point loc = (Point) locField.get(obj);
                java.lang.reflect.Field levelField = obj.getClass().getDeclaredField("level");
                levelField.setAccessible(true);
                int level = levelField.getInt(obj);
                java.lang.reflect.Field popField = obj.getClass().getDeclaredField("population");
                popField.setAccessible(true);
                int pop = popField.getInt(obj);
                java.lang.reflect.Field factionField = obj.getClass().getDeclaredField("faction");
                factionField.setAccessible(true);
                String faction = (String) factionField.get(obj);
                java.lang.reflect.Field infraField = obj.getClass().getDeclaredField("infrastructureLevel");
                infraField.setAccessible(true);
                int infra = infraField.getInt(obj);
                String label = (level == 1) ? "Village" : (level == 2 ? "Town" : "City");
                list.add(label + " (" + faction + ") at (" + loc.x + "," + loc.y + ") Pop:" + pop + " Infra:" + infra);
            }
        } catch(Exception ex) {
            list.add("Error retrieving civilization info.");
        }
        return list;
    }

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
        dialog.add(sitesLabel); dialog.add(sitesField);
        dialog.add(marginLabel); dialog.add(marginField);
        dialog.add(widthLabel); dialog.add(widthField);
        dialog.add(heightLabel); dialog.add(heightField);
        dialog.add(applyButton); dialog.add(cancelButton);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}