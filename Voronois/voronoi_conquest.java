import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoronoiSimulation {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowMainMenu());
    }
    
    private static void createAndShowMainMenu() {
        JFrame frame = new JFrame("Voronoi Simulation - Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MainMenuPanel menu = new MainMenuPanel(frame);
        frame.getContentPane().add(menu);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class MainMenuPanel extends JPanel {
    private JTextField widthField;
    private JTextField heightField;
    private JTextField numSquaresField;
    private JSlider numColorsSlider;
    private JLabel sliderLabel;
    private JComboBox<String> aiTypeCombo;
    private JSlider riskSlider;
    private JLabel riskLabel;
    private JFrame parentFrame;
    
    public MainMenuPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridLayout(7, 2, 5, 5));
        
        add(new JLabel("Map Width:"));
        widthField = new JTextField("800");
        add(widthField);
        
        add(new JLabel("Map Height:"));
        heightField = new JTextField("600");
        add(heightField);
        
        add(new JLabel("Number of Voronoi Squares:"));
        numSquaresField = new JTextField("10");
        add(numSquaresField);
        
        add(new JLabel("Number of Colors:"));
        numColorsSlider = new JSlider(JSlider.HORIZONTAL, 2, 5, 5);
        numColorsSlider.setMajorTickSpacing(1);
        numColorsSlider.setPaintTicks(true);
        numColorsSlider.setPaintLabels(true);
        add(numColorsSlider);
        
        sliderLabel = new JLabel("Colors: " + numColorsSlider.getValue());
        numColorsSlider.addChangeListener(e -> sliderLabel.setText("Colors: " + numColorsSlider.getValue()));
        add(sliderLabel);
        
        add(new JLabel("AI Type:"));
        aiTypeCombo = new JComboBox<>(new String[] { "Dumb AI", "Smart AI" });
        add(aiTypeCombo);
        
        add(new JLabel("Smart AI Risk (0 = aggressive, 1 = risk averse):"));
        riskSlider = new JSlider(0, 100, 50);
        riskSlider.setMajorTickSpacing(25);
        riskSlider.setPaintTicks(true);
        riskSlider.setPaintLabels(true);
        add(riskSlider);
        
        riskLabel = new JLabel("Risk: " + (riskSlider.getValue() / 100.0));
        riskSlider.addChangeListener(e -> riskLabel.setText("Risk: " + (riskSlider.getValue() / 100.0)));
        add(riskLabel);
        
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                int numSquares = Integer.parseInt(numSquaresField.getText());
                int numColors = numColorsSlider.getValue();
                int aiType = aiTypeCombo.getSelectedIndex(); // 0 for dumb, 1 for smart
                double smartRisk = riskSlider.getValue() / 100.0;
                
                // Open a new simulation window with the specified map dimensions.
                JFrame simFrame = new JFrame("Voronoi Combat Simulation");
                simFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                VoronoiSquares simulation = new VoronoiSquares(width, height, numSquares, numColors, aiType, smartRisk);
                simFrame.getContentPane().add(simulation);
                simFrame.pack();
                simFrame.setLocationRelativeTo(null);
                simFrame.setVisible(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Invalid input. Please enter valid numbers.");
            }
        });
        add(startButton);
    }
}

class VoronoiSquares extends JPanel {
    private int mapWidth, mapHeight, numSites, numTeams;
    // Turn-based/team settings.
    private int currentTeam = 0;
    private float[] teamHues;
    private String[] teamNames;
    private boolean[] teamIsAI;
    private boolean aiEnabled = true;
    private int aiType; // 0 = Dumb AI, 1 = Smart AI
    private double smartRisk;
    // Region data.
    private Point[] sites;
    private Color[] siteColors;
    private int[] troops;             // raw troop count
    private double[] combatPower;     // effective strength (troops * multiplier)
    private int[] regionTeam;         // team affiliation for each region
    private boolean[] isBastion;      // if true, multiplier = 1.5
    // Voronoi diagram.
    private int[][] regionAssignment;
    private boolean[][] borders;
    private BufferedImage voronoiImage;
    private boolean[][] adjacent;
    // Interaction variables.
    private int selectedSource = -1;  // Selected source region.
    private int lastMoveSource = -1, lastMoveDest = -1; // For drawing the move arrow.
    private int highlightedSite = -1;
    private int mouseX = 0, mouseY = 0;
    private Random rand = new Random();
    
    // Constructor with AI parameters.
    public VoronoiSquares(int mapWidth, int mapHeight, int numSites, int numTeams, int aiType, double smartRisk) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.numSites = numSites;
        this.numTeams = numTeams;
        this.aiType = aiType;
        this.smartRisk = smartRisk;
        
        // Set fixed color names and hues for up to 5 teams.
        String[] availableNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
        float[] availableHues = {0.0f, 0.67f, 0.33f, 0.15f, 0.83f};
        teamNames = new String[numTeams];
        teamHues = new float[numTeams];
        teamIsAI = new boolean[numTeams];
        for (int i = 0; i < numTeams; i++) {
            teamNames[i] = availableNames[i];
            teamHues[i] = availableHues[i];
            teamIsAI[i] = (i != 0); // Team 0 is human.
        }
        
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        
        // Initialize region arrays.
        sites = new Point[numSites];
        siteColors = new Color[numSites];
        troops = new int[numSites];
        combatPower = new double[numSites];
        regionTeam = new int[numSites];
        isBastion = new boolean[numSites];
        
        regionAssignment = new int[mapWidth][mapHeight];
        borders = new boolean[mapWidth][mapHeight];
        adjacent = new boolean[numSites][numSites];
        
        // Initialize regions with random troop counts (10-50) and assign teams round-robin.
        for (int i = 0; i < numSites; i++) {
            int troopCount = rand.nextInt(41) + 10;
            troops[i] = troopCount;
            isBastion[i] = false;
            combatPower[i] = troopCount * 1.0;
            int team = i % numTeams;
            regionTeam[i] = team;
            float brightness = computeBrightness(troops[i]);
            sites[i] = new Point(rand.nextInt(mapWidth), rand.nextInt(mapHeight));
            siteColors[i] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        }
        
        // Build the Voronoi diagram.
        voronoiImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int closestIndex = 0;
                int minDistSq = Integer.MAX_VALUE;
                for (int i = 0; i < numSites; i++) {
                    int dx = x - sites[i].x;
                    int dy = y - sites[i].y;
                    int distSq = dx * dx + dy * dy;
                    if (distSq < minDistSq) {
                        minDistSq = distSq;
                        closestIndex = i;
                    }
                }
                regionAssignment[x][y] = closestIndex;
                voronoiImage.setRGB(x, y, siteColors[closestIndex].getRGB());
            }
        }
        
        // Compute borders (a pixel is a border if any 4-neighbor is in a different region).
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int cur = regionAssignment[x][y];
                boolean isBorder = false;
                if (x > 0 && regionAssignment[x-1][y] != cur) isBorder = true;
                else if (x < mapWidth-1 && regionAssignment[x+1][y] != cur) isBorder = true;
                else if (y > 0 && regionAssignment[x][y-1] != cur) isBorder = true;
                else if (y < mapHeight-1 && regionAssignment[x][y+1] != cur) isBorder = true;
                borders[x][y] = isBorder;
            }
        }
        
        computeAdjacency();
        
        // Set up mouse listeners.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Right-click: reinforcement move.
                if (SwingUtilities.isRightMouseButton(e)) {
                    int mx = e.getX();
                    int my = e.getY();
                    if (mx < 0 || mx >= mapWidth || my < 0 || my >= mapHeight) return;
                    int clickedRegion = regionAssignment[mx][my];
                    // Reinforce only if the region belongs to the current team and has at least 10 troops.
                    if (regionTeam[clickedRegion] == currentTeam && troops[clickedRegion] >= 10) {
                        executeReinforce(clickedRegion, clickedRegion);
                        System.out.println("Reinforced region " + clickedRegion + " with 100% bonus.");
                    }
                    return;
                }
                // Double-click: turn region into a bastion.
                if (e.getClickCount() >= 2) {
                    int mx = e.getX();
                    int my = e.getY();
                    if (mx < 0 || mx >= mapWidth || my < 0 || my >= mapHeight) return;
                    int clickedRegion = regionAssignment[mx][my];
                    if (!isBastion[clickedRegion]) {
                        isBastion[clickedRegion] = true;
                        combatPower[clickedRegion] = troops[clickedRegion] * 1.5;
                        System.out.println("Region " + clickedRegion + " turned into a bastion.");
                        repaint();
                    }
                    return;
                }
                // Left-click.
                if (teamIsAI[currentTeam] && aiEnabled) return;
                int mx = e.getX();
                int my = e.getY();
                if (mx < 0 || mx >= mapWidth || my < 0 || my >= mapHeight) return;
                int clickedRegion = regionAssignment[mx][my];
                if (selectedSource == -1) {
                    if (regionTeam[clickedRegion] != currentTeam) {
                        System.out.println("Not your region. Current turn: " + teamNames[currentTeam]);
                        return;
                    }
                    if (troops[clickedRegion] <= 0) {
                        System.out.println("Region " + clickedRegion + " has no troops.");
                        return;
                    }
                    selectedSource = clickedRegion;
                    System.out.println("Selected source region: " + selectedSource);
                    repaint();
                } else {
                    if (clickedRegion == selectedSource) {
                        selectedSource = -1;
                        repaint();
                        return;
                    }
                    if (!adjacent[selectedSource][clickedRegion]) {
                        System.out.println("Region " + clickedRegion + " is not adjacent.");
                        return;
                    }
                    // If reinforcing (friendly move), do reinforcement and do NOT end the turn.
                    if (regionTeam[selectedSource] == regionTeam[clickedRegion]) {
                        executeReinforce(selectedSource, clickedRegion);
                        selectedSource = -1;
                        return;
                    } else {
                        executeMove(selectedSource, clickedRegion);
                        selectedSource = -1;
                        endTurn();
                    }
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                highlightedSite = -1;
                repaint();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                if (mouseX >= 0 && mouseX < mapWidth && mouseY >= 0 && mouseY < mapHeight) {
                    highlightedSite = regionAssignment[mouseX][mouseY];
                } else {
                    highlightedSite = -1;
                }
                repaint();
            }
        });
        
        // Add control panel.
        setLayout(new BorderLayout());
        JPanel controlPanel = new SimulationControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private class SimulationControlPanel extends JPanel {
        public SimulationControlPanel() {
            JButton endTurnButton = new JButton("End Turn");
            endTurnButton.addActionListener(e -> endTurn());
            add(endTurnButton);
            
            JCheckBox aiCheckBox = new JCheckBox("Enable AI", true);
            aiCheckBox.addActionListener(e -> {
                aiEnabled = aiCheckBox.isSelected();
                System.out.println("AI enabled: " + aiEnabled);
            });
            add(aiCheckBox);
        }
    }
    
    private float computeBrightness(int troopCount) {
        float brightness = 0.8f - (troopCount - 10) * 0.004f;
        return Math.max(0.3f, brightness);
    }
    
    private void updateRegionStats(int regionIndex) {
        float brightness = computeBrightness(troops[regionIndex]);
        int team = regionTeam[regionIndex];
        siteColors[regionIndex] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        double baseMultiplier = isBastion[regionIndex] ? 1.5 : 1.0;
        combatPower[regionIndex] = troops[regionIndex] * baseMultiplier;
    }
    
    private void executeMove(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        double sourcePower = combatPower[source];
        double destPower = combatPower[dest];
        System.out.println("Moving troops from region " + source + " to region " + dest);
        lastMoveSource = source;
        lastMoveDest = dest;
        if (siteColors[source].equals(siteColors[dest])) {
            troops[dest] = destTroops + sourceTroops;
            troops[source] = 0;
        } else {
            if (sourcePower > destPower) {
                double multiplier = isBastion[source] ? 1.5 : 1.0;
                int newTroops = (int) Math.floor((sourcePower - destPower) / multiplier);
                troops[dest] = newTroops;
                regionTeam[dest] = regionTeam[source];
                siteColors[dest] = siteColors[source];
                isBastion[dest] = isBastion[source];
                troops[source] = 0;
            } else {
                double multiplier = isBastion[dest] ? 1.5 : 1.0;
                int newTroops = (int) Math.floor((destPower - sourcePower) / multiplier);
                troops[dest] = newTroops;
                troops[source] = 0;
            }
        }
        updateRegionStats(source);
        updateRegionStats(dest);
        updateVoronoiImage();
        repaint();
    }
    
    private void computeAdjacency() {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int cur = regionAssignment[x][y];
                if (x < mapWidth - 1) {
                    int neighbor = regionAssignment[x+1][y];
                    if (cur != neighbor) {
                        adjacent[cur][neighbor] = true;
                        adjacent[neighbor][cur] = true;
                    }
                }
                if (y < mapHeight - 1) {
                    int neighbor = regionAssignment[x][y+1];
                    if (cur != neighbor) {
                        adjacent[cur][neighbor] = true;
                        adjacent[neighbor][cur] = true;
                    }
                }
            }
        }
    }
    
    private void updateVoronoiImage() {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int region = regionAssignment[x][y];
                voronoiImage.setRGB(x, y, siteColors[region].getRGB());
            }
        }
    }
    
    public void endTurn() {
        for (int i = 0; i < numSites; i++) {
            troops[i] += 5;
            updateRegionStats(i);
        }
        updateVoronoiImage();
        selectedSource = -1;
        currentTeam = (currentTeam + 1) % numTeams;
        while (!teamHasTiles(currentTeam)) {
            System.out.println("Skipping turn for " + teamNames[currentTeam] + " (no tiles).");
            currentTeam = (currentTeam + 1) % numTeams;
        }
        System.out.println("Turn ended. Current turn: " + teamNames[currentTeam]);
        repaint();
        if (aiEnabled && teamIsAI[currentTeam]) {
            new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (aiType == 0) {
                        doDumbAIMove();
                    } else {
                        doSmartAIMove();
                    }
                    ((Timer)e.getSource()).stop();
                }
            }).start();
        }
    }
    
    private boolean teamHasTiles(int team) {
        for (int i = 0; i < numSites; i++) {
            if (regionTeam[i] == team)
                return true;
        }
        return false;
    }
    
    private void doDumbAIMove() {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < numSites; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numSites; j++) {
                    if (adjacent[i][j]) {
                        moves.add(new int[]{i, j});
                    }
                }
            }
        }
        if (moves.isEmpty()) {
            System.out.println("Dumb AI (" + teamNames[currentTeam] + ") has no valid moves. Skipping turn.");
            endTurn();
            return;
        }
        int[] move = moves.get(rand.nextInt(moves.size()));
        if (regionTeam[move[0]] == regionTeam[move[1]]) {
            System.out.println("Dumb AI (" + teamNames[currentTeam] + ") reinforces region " + move[1]);
            executeReinforce(move[0], move[1]);
            return;
        } else {
            System.out.println("Dumb AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1]);
            executeMove(move[0], move[1]);
            endTurn();
        }
    }
    
    private void doSmartAIMove() {
        double bestScore = Double.NEGATIVE_INFINITY;
        int[] bestMove = null;
        boolean isFriendly = false;
        for (int i = 0; i < numSites; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numSites; j++) {
                    if (adjacent[i][j]) {
                        double score = 0.0;
                        if (regionTeam[i] == regionTeam[j]) {
                            score = 2 * troops[i];
                        } else {
                            double sourcePower = combatPower[i];
                            double destPower = combatPower[j];
                            if (sourcePower > destPower) {
                                score = (sourcePower - destPower) * (1 - smartRisk);
                            } else {
                                score = -1000;
                            }
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = new int[]{i, j};
                            isFriendly = (regionTeam[i] == regionTeam[j]);
                        }
                    }
                }
            }
        }
        if (bestMove == null || bestScore <= 0) {
            System.out.println("Smart AI (" + teamNames[currentTeam] + ") found no advantageous moves. Skipping turn.");
            endTurn();
            return;
        }
        if (isFriendly) {
            System.out.println("Smart AI (" + teamNames[currentTeam] + ") reinforces from region " + bestMove[0] + " to region " + bestMove[1] + " with score " + bestScore);
            executeReinforce(bestMove[0], bestMove[1]);
            return;
        } else {
            System.out.println("Smart AI (" + teamNames[currentTeam] + ") attacks from region " + bestMove[0] + " to region " + bestMove[1] + " with score " + bestScore);
            executeMove(bestMove[0], bestMove[1]);
            endTurn();
        }
    }
    
    private void executeReinforce(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        troops[dest] = destTroops + 2 * sourceTroops;
        troops[source] = 0;
        updateRegionStats(source);
        updateRegionStats(dest);
        updateVoronoiImage();
        repaint();
        System.out.println("Reinforced region " + dest + " with " + (2 * sourceTroops) + " troops (100% bonus).");
    }
    
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowHeadLength = 10;
        double arrowAngle = Math.toRadians(20);
        int xArrow1 = (int)(x2 - arrowHeadLength * Math.cos(angle - arrowAngle));
        int yArrow1 = (int)(y2 - arrowHeadLength * Math.sin(angle - arrowAngle));
        int xArrow2 = (int)(x2 - arrowHeadLength * Math.cos(angle + arrowAngle));
        int yArrow2 = (int)(y2 - arrowHeadLength * Math.sin(angle + arrowAngle));
        g2d.drawLine(x2, y2, xArrow1, yArrow1);
        g2d.drawLine(x2, y2, xArrow2, yArrow2);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(voronoiImage, 0, 0, null);
        g.setColor(Color.BLACK);
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (borders[x][y]) {
                    g.drawLine(x, y, x, y);
                }
            }
        }
        g.setColor(Color.BLACK);
        for (Point p : sites) {
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
        if (selectedSource != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, 100));
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (regionAssignment[x][y] == selectedSource) {
                        g2.fillRect(x, y, 1, 1);
                    }
                }
            }
            g2.dispose();
        }
        if (lastMoveSource != -1 && lastMoveDest != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.MAGENTA);
            Point from = sites[lastMoveSource];
            Point to = sites[lastMoveDest];
            drawArrow(g2, from.x, from.y, to.x, to.y);
            g2.dispose();
        }
        if (highlightedSite != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            String text = "Troops: " + troops[highlightedSite] + " | Power: " + String.format("%.1f", combatPower[highlightedSite]);
            g2.drawString(text, mouseX + 10, mouseY + 10);
            g2.dispose();
        }
        g.setColor(Color.BLACK);
        g.drawString("Current Turn: " + teamNames[currentTeam], 10, 20);
    }
}