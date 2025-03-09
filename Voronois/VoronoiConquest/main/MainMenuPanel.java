package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MainMenuPanel extends JPanel {
    private JTextField widthField;
    private JTextField heightField;
    private JTextField numRegionsField;
    private JSlider numTeamsSlider;
    private JLabel teamsLabel;
    private JPanel teamConfigPanel;
    private List<JComboBox<String>> teamComboBoxes;
    private JFrame parentFrame;

    private final String[] availableNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
    private final String[] controlOptions = {"Hotseat", "Dumb AI", "Smart AI"};

    public MainMenuPanel(JFrame frame) {
        this.parentFrame = frame;
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Voronoi Conquest Settings"),
            new EmptyBorder(10, 10, 10, 10)
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Basic game settings.
        JPanel settingsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        settingsPanel.add(new JLabel("Map Width:"));
        widthField = new JTextField("800");
        settingsPanel.add(widthField);
        settingsPanel.add(new JLabel("Map Height:"));
        heightField = new JTextField("600");
        settingsPanel.add(heightField);
        settingsPanel.add(new JLabel("Number of Regions:"));
        numRegionsField = new JTextField("10");
        settingsPanel.add(numRegionsField);
        add(settingsPanel);

        // Team count configuration.
        JPanel teamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        teamsPanel.add(new JLabel("Number of Teams (2-5):"));
        numTeamsSlider = new JSlider(JSlider.HORIZONTAL, 2, 5, 2);
        numTeamsSlider.setMajorTickSpacing(1);
        numTeamsSlider.setPaintTicks(true);
        numTeamsSlider.setPaintLabels(true);
        teamsPanel.add(numTeamsSlider);
        teamsLabel = new JLabel("Teams: " + numTeamsSlider.getValue());
        teamsPanel.add(teamsLabel);
        add(teamsPanel);

        // Panel for team configuration.
        teamConfigPanel = new JPanel();
        teamConfigPanel.setLayout(new GridLayout(numTeamsSlider.getValue(), 2, 5, 5));
        teamComboBoxes = new ArrayList<>();
        for (int i = 0; i < numTeamsSlider.getValue(); i++) {
            String teamName = availableNames[i];
            teamConfigPanel.add(new JLabel("Team " + (i + 1) + " (" + teamName + "):"));
            JComboBox<String> combo = new JComboBox<>(controlOptions);
            combo.setSelectedIndex(i == 0 ? 0 : 1);
            teamComboBoxes.add(combo);
            teamConfigPanel.add(combo);
        }
        add(teamConfigPanel);

        numTeamsSlider.addChangeListener(e -> {
            int numTeams = numTeamsSlider.getValue();
            teamsLabel.setText("Teams: " + numTeams);
            updateTeamConfigPanel(numTeams);
        });

        // Start game button.
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                int numRegions = Integer.parseInt(numRegionsField.getText());
                if (width <= 0 || height <= 0 || numRegions <= 0) {
                    throw new NumberFormatException("Dimensions and region count must be positive.");
                }
                int numTeams = numTeamsSlider.getValue();
                TeamControl[] teamControls = new TeamControl[numTeams];
                for (int i = 0; i < numTeams; i++) {
                    int sel = teamComboBoxes.get(i).getSelectedIndex();
                    switch (sel) {
                        case 0:
                            teamControls[i] = TeamControl.HOTSEAT;
                            break;
                        case 1:
                            teamControls[i] = TeamControl.DUMB;
                            break;
                        case 2:
                            teamControls[i] = TeamControl.SMART;
                            break;
                        default:
                            teamControls[i] = TeamControl.DUMB;
                            break;
                    }
                }
                
                // Instead of creating a bare JFrame, create a GameWindow that contains a menu bar
                GameWindow gameWindow = new GameWindow(width, height, numRegions, numTeams, teamControls, 0.5);
                gameWindow.setLocationRelativeTo(null);
                gameWindow.setVisible(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Invalid input: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        add(startButton);
    }

    private void updateTeamConfigPanel(int numTeams) {
        teamConfigPanel.removeAll();
        teamComboBoxes.clear();
        teamConfigPanel.setLayout(new GridLayout(numTeams, 2, 5, 5));
        for (int i = 0; i < numTeams; i++) {
            String teamName = availableNames[i];
            teamConfigPanel.add(new JLabel("Team " + (i + 1) + " (" + teamName + "):"));
            JComboBox<String> combo = new JComboBox<>(controlOptions);
            combo.setSelectedIndex(i == 0 ? 0 : 1);
            teamComboBoxes.add(combo);
            teamConfigPanel.add(combo);
        }
        teamConfigPanel.revalidate();
        teamConfigPanel.repaint();
        parentFrame.pack();
    }
}
