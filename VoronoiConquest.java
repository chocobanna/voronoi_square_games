import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/* Main entry point. */
public class VoronoiConquest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame mainFrame = new JFrame("Voronoi Conquest - Main Menu");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MainMenuPanel menu = new MainMenuPanel(mainFrame);
                mainFrame.getContentPane().add(menu);
                mainFrame.pack();
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
            }
        });
    }
}

/* MainMenuPanel: Handles user input, validation, and improved UI. */
class MainMenuPanel extends JPanel {
    private JTextField widthField;
    private JTextField heightField;
    private JTextField numRegionsField;
    private JSlider numTeamsSlider;
    private JLabel sliderLabel;
    private JComboBox<String> aiTypeCombo;
    private JSlider riskSlider;
    private JLabel riskLabel;
    private JFrame parentFrame;

    public MainMenuPanel(JFrame frame) {
        this.parentFrame = frame;
        // Use a titled border with padding for a better look.
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Voronoi Conquest Settings"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setLayout(new GridLayout(8, 2, 5, 5));
        
        add(new JLabel("Map Width:"));
        widthField = new JTextField("800");
        add(widthField);
        
        add(new JLabel("Map Height:"));
        heightField = new JTextField("600");
        add(heightField);
        
        add(new JLabel("Number of Regions:"));
        numRegionsField = new JTextField("10");
        add(numRegionsField);
        
        add(new JLabel("Number of Teams (2-5):"));
        numTeamsSlider = new JSlider(JSlider.HORIZONTAL, 2, 5, 5);
        numTeamsSlider.setMajorTickSpacing(1);
        numTeamsSlider.setPaintTicks(true);
        numTeamsSlider.setPaintLabels(true);
        add(numTeamsSlider);
        
        sliderLabel = new JLabel("Teams: " + numTeamsSlider.getValue());
        numTeamsSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                sliderLabel.setText("Teams: " + numTeamsSlider.getValue());
            }
        });
        add(sliderLabel);
        
        add(new JLabel("AI Type:"));
        aiTypeCombo = new JComboBox<>(new String[] {"Dumb AI", "Smart AI"});
        add(aiTypeCombo);
        
        add(new JLabel("Smart AI Risk (0 = aggressive, 1 = risk averse):"));
        riskSlider = new JSlider(0, 100, 50);
        riskSlider.setMajorTickSpacing(25);
        riskSlider.setPaintTicks(true);
        riskSlider.setPaintLabels(true);
        add(riskSlider);
        
        riskLabel = new JLabel("Risk: " + (riskSlider.getValue() / 100.0));
        riskSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                riskLabel.setText("Risk: " + (riskSlider.getValue() / 100.0));
            }
        });
        add(riskLabel);
        
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    int numRegions = Integer.parseInt(numRegionsField.getText());
                    if (width <= 0 || height <= 0 || numRegions <= 0) {
                        throw new NumberFormatException("Dimensions and region count must be positive.");
                    }
                    int numTeams = numTeamsSlider.getValue();
                    int aiType = aiTypeCombo.getSelectedIndex(); // 0 = Dumb, 1 = Smart
                    double smartRisk = riskSlider.getValue() / 100.0;
                    
                    // Create game frame with the GamePanel.
                    JFrame gameFrame = new JFrame("Voronoi Conquest - Battle");
                    gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    GamePanel gamePanel = new GamePanel(width, height, numRegions, numTeams, aiType, smartRisk);
                    gameFrame.getContentPane().add(gamePanel);
                    gameFrame.pack();
                    gameFrame.setLocationRelativeTo(null);
                    gameFrame.setVisible(true);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parentFrame, "Invalid input: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        add(startButton);
    }
}

/* GamePanel: Responsible for rendering and mouse interaction. */
class GamePanel extends JPanel {
    private int mapWidth;
    private int mapHeight;
    private int numRegions;
    private int numTeams;
    private int aiType;
    private double smartRisk;
    private GameEngine engine;
    
    public GamePanel(int mapWidth, int mapHeight, int numRegions, int numTeams, int aiType, double smartRisk) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.numRegions = numRegions;
        this.numTeams = numTeams;
        this.aiType = aiType;
        this.smartRisk = smartRisk;
        
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        
        // Initialize game engine.
        engine = new GameEngine(mapWidth, mapHeight, numRegions, numTeams, aiType, smartRisk);
        
        // Compute the Voronoi diagram (using a placeholder Fortune's algorithm).
        FortuneVoronoi voronoi = new FortuneVoronoi(engine.getSites(), mapWidth, mapHeight);
        engine.setRegionAssignment(voronoi.getRegionAssignment());
        engine.setVoronoiImage(voronoi.getVoronoiImage());
        // Refresh the Voronoi image so every tile is colored with its team color.
        engine.refreshVoronoiImage();
        
        // Setup mouse listeners.
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                engine.handleMouseClick(e);
                repaint();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                engine.handleMouseMove(e);
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        engine.draw((Graphics2D) g);
    }
}

/* GameEngine: Contains game state, rules, troop movement, team management, AI, etc. */
class GameEngine {
    private int mapWidth;
    private int mapHeight;
    private int numRegions;
    private int numTeams;
    private int aiType;
    private double smartRisk;
    
    // Data for regions.
    private Point[] sites;
    private int[][] regionAssignment;
    private BufferedImage voronoiImage;
    private Color[] siteColors;
    private int[] troops;
    private double[] combatPower;
    private int[] regionTeam;
    private boolean[] isBastion;
    private boolean[][] adjacent;
    
    // Turn and team data.
    private int currentTeam = 0;
    private String[] teamNames;
    private float[] teamHues;
    private boolean[] teamIsAI;
    
    // Interaction state.
    private int selectedRegion = -1;
    private int highlightedRegion = -1;
    private int mouseX = 0;
    private int mouseY = 0;
    
    // Last move info for drawing arrow.
    private int lastMoveSource = -1;
    private int lastMoveDest = -1;
    
    // For AI moves.
    private Random rand = new Random();
    
    public GameEngine(int mapWidth, int mapHeight, int numRegions, int numTeams, int aiType, double smartRisk) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.numRegions = numRegions;
        this.numTeams = numTeams;
        this.aiType = aiType;
        this.smartRisk = smartRisk;
        
        // Initialize team names, hues, and AI flags.
        teamNames = new String[numTeams];
        teamHues = new float[numTeams];
        teamIsAI = new boolean[numTeams];
        String[] availableNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
        float[] availableHues = {0.0f, 0.67f, 0.33f, 0.15f, 0.83f};
        for (int i = 0; i < numTeams; i++) {
            teamNames[i] = availableNames[i];
            teamHues[i] = availableHues[i];
            teamIsAI[i] = (i != 0); // Team 0 is human.
        }
        
        // Initialize regions.
        sites = new Point[numRegions];
        siteColors = new Color[numRegions];
        troops = new int[numRegions];
        combatPower = new double[numRegions];
        regionTeam = new int[numRegions];
        isBastion = new boolean[numRegions];
        adjacent = new boolean[numRegions][numRegions];
        
        for (int i = 0; i < numRegions; i++) {
            troops[i] = rand.nextInt(41) + 10; // 10 to 50 troops.
            isBastion[i] = false;
            combatPower[i] = troops[i] * 1.0;
            regionTeam[i] = i % numTeams;
            float brightness = computeBrightness(troops[i]);
            sites[i] = new Point(rand.nextInt(mapWidth), rand.nextInt(mapHeight));
            siteColors[i] = Color.getHSBColor(teamHues[regionTeam[i]], 1.0f, brightness);
        }
    }
    
    public Point[] getSites() {
        return sites;
    }
    
    public void setRegionAssignment(int[][] assignment) {
        this.regionAssignment = assignment;
        computeAdjacency();
    }
    
    public void setVoronoiImage(BufferedImage image) {
        this.voronoiImage = image;
    }
    
    // Call this method to refresh the Voronoi image using team colors.
    public void refreshVoronoiImage() {
        updateVoronoiImage();
    }
    
    private float computeBrightness(int troopCount) {
        float brightness = 0.8f - (troopCount - 10) * 0.004f;
        return Math.max(0.3f, brightness);
    }
    
    private void computeAdjacency() {
        if (regionAssignment == null) return;
        int width = regionAssignment.length;
        int height = regionAssignment[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int region = regionAssignment[x][y];
                if (x < width - 1) {
                    int neighbor = regionAssignment[x + 1][y];
                    if (region != neighbor) {
                        adjacent[region][neighbor] = true;
                        adjacent[neighbor][region] = true;
                    }
                }
                if (y < height - 1) {
                    int neighbor = regionAssignment[x][y + 1];
                    if (region != neighbor) {
                        adjacent[region][neighbor] = true;
                        adjacent[neighbor][region] = true;
                    }
                }
            }
        }
    }
    
    /* --- Mouse Interaction --- */
    public void handleMouseClick(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (!isValidCoordinate(x, y)) return;
        int clickedRegion = regionAssignment[x][y];
        
        // Right-click: reinforcement move on self (if valid).
        if (SwingUtilities.isRightMouseButton(e)) {
            if (regionTeam[clickedRegion] == currentTeam && troops[clickedRegion] >= 10) {
                executeReinforce(clickedRegion, clickedRegion);
                System.out.println("Reinforced region " + clickedRegion + " with 100% bonus.");
            }
            return;
        }
        
        // Double-click: toggle region into a bastion.
        if (e.getClickCount() >= 2) {
            if (!isBastion[clickedRegion]) {
                isBastion[clickedRegion] = true;
                combatPower[clickedRegion] = troops[clickedRegion] * 1.5;
                System.out.println("Region " + clickedRegion + " turned into a bastion.");
                updateRegionStats(clickedRegion);
                updateVoronoiImage();
            }
            return;
        }
        
        // Left-click: selection and movement.
        if (teamIsAI[currentTeam]) return; // ignore if it's AI turn.
        
        if (selectedRegion == -1) {
            if (regionTeam[clickedRegion] != currentTeam) {
                System.out.println("Not your region. Current turn: " + teamNames[currentTeam]);
                return;
            }
            if (troops[clickedRegion] <= 0) {
                System.out.println("Region " + clickedRegion + " has no troops.");
                return;
            }
            selectedRegion = clickedRegion;
            System.out.println("Selected source region: " + selectedRegion);
        } else {
            if (clickedRegion == selectedRegion) {
                selectedRegion = -1; // Deselect.
                return;
            }
            if (!adjacent[selectedRegion][clickedRegion]) {
                System.out.println("Region " + clickedRegion + " is not adjacent to region " + selectedRegion);
                return;
            }
            // If moving troops within your own team, reinforce.
            if (regionTeam[selectedRegion] == regionTeam[clickedRegion]) {
                executeReinforce(selectedRegion, clickedRegion);
                selectedRegion = -1;
            } else { // Otherwise, attack.
                executeMove(selectedRegion, clickedRegion);
                selectedRegion = -1;
                endTurn();
            }
        }
    }
    
    public void handleMouseMove(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (isValidCoordinate(mouseX, mouseY)) {
            highlightedRegion = regionAssignment[mouseX][mouseY];
        } else {
            highlightedRegion = -1;
        }
    }
    
    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < mapWidth && y >= 0 && y < mapHeight;
    }
    
    /* --- Drawing --- */
    public void draw(Graphics2D g2d) {
        if (voronoiImage != null) {
            g2d.drawImage(voronoiImage, 0, 0, null);
        }
        // Draw team turn indicator.
        g2d.setColor(Color.BLACK);
        g2d.drawString("Current Turn: " + teamNames[currentTeam], 10, 20);
        // If hovering, display region stats.
        if (highlightedRegion != -1) {
            String text = "Troops: " + troops[highlightedRegion] + " | Power: " + String.format("%.1f", combatPower[highlightedRegion]);
            g2d.drawString(text, mouseX + 10, mouseY + 10);
        }
        // Draw black outline around the selected region.
        if (selectedRegion != -1) {
            g2d.setColor(Color.BLACK);
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (regionAssignment[x][y] == selectedRegion) {
                        // Check if it's a boundary pixel.
                        boolean isBoundary = false;
                        if (x == 0 || regionAssignment[x - 1][y] != selectedRegion) isBoundary = true;
                        if (x == mapWidth - 1 || regionAssignment[x + 1][y] != selectedRegion) isBoundary = true;
                        if (y == 0 || regionAssignment[x][y - 1] != selectedRegion) isBoundary = true;
                        if (y == mapHeight - 1 || regionAssignment[x][y + 1] != selectedRegion) isBoundary = true;
                        if (isBoundary) {
                            g2d.drawRect(x, y, 1, 1);
                        }
                    }
                }
            }
        }
        // Draw an arrow if a move was made.
        if (lastMoveSource != -1 && lastMoveDest != -1) {
            g2d.setColor(Color.MAGENTA);
            Point from = sites[lastMoveSource];
            Point to = sites[lastMoveDest];
            drawArrow(g2d, from.x, from.y, to.x, to.y);
        }
    }
    
    // Draws an arrow from (x1,y1) to (x2,y2).
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
    
    /* --- Troop Movement and Reinforcement --- */
    public void executeMove(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        double sourcePower = combatPower[source];
        double destPower = combatPower[dest];
        System.out.println("Moving troops from region " + source + " to region " + dest);
        // Update last move info for arrow drawing.
        lastMoveSource = source;
        lastMoveDest = dest;
        // If friendly move, simply add the troops.
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
    }
    
    public void executeReinforce(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        // Update last move info for arrow drawing.
        lastMoveSource = source;
        lastMoveDest = dest;
        troops[dest] = destTroops + 2 * sourceTroops;
        troops[source] = 0;
        updateRegionStats(source);
        updateRegionStats(dest);
        updateVoronoiImage();
        System.out.println("Reinforced region " + dest + " with " + (2 * sourceTroops) + " troops (100% bonus).");
    }
    
    private void updateRegionStats(int regionIndex) {
        float brightness = computeBrightness(troops[regionIndex]);
        int team = regionTeam[regionIndex];
        siteColors[regionIndex] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        double baseMultiplier = isBastion[regionIndex] ? 1.5 : 1.0;
        combatPower[regionIndex] = troops[regionIndex] * baseMultiplier;
    }
    
    // Now public so it can be called from GamePanel after initialization.
    public void updateVoronoiImage() {
        if (voronoiImage == null || regionAssignment == null) return;
        int width = regionAssignment.length;
        int height = regionAssignment[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int region = regionAssignment[x][y];
                voronoiImage.setRGB(x, y, siteColors[region].getRGB());
            }
        }
    }
    
    /* --- Turn Management and AI --- */
    public void endTurn() {
        // Add 5 troops to every region.
        for (int i = 0; i < numRegions; i++) {
            troops[i] += 5;
            updateRegionStats(i);
        }
        updateVoronoiImage();
        selectedRegion = -1;
        // Cycle to next team.
        currentTeam = (currentTeam + 1) % numTeams;
        while (!teamHasTiles(currentTeam)) {
            System.out.println("Skipping turn for " + teamNames[currentTeam] + " (no tiles).");
            currentTeam = (currentTeam + 1) % numTeams;
        }
        System.out.println("Turn ended. Current turn: " + teamNames[currentTeam]);
        
        // If it's an AI team, schedule an AI move.
        if (teamIsAI[currentTeam]) {
            new javax.swing.Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (aiType == 0) {
                        doDumbAIMove();
                    } else {
                        doSmartAIMove();
                    }
                    ((javax.swing.Timer)e.getSource()).stop();
                }
            }).start();
        }
    }
    
    private boolean teamHasTiles(int team) {
        for (int i = 0; i < numRegions; i++) {
            if (regionTeam[i] == team) return true;
        }
        return false;
    }
    
    private void doDumbAIMove() {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < numRegions; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numRegions; j++) {
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
            endTurn(); // End turn after reinforcement.
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
        for (int i = 0; i < numRegions; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numRegions; j++) {
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
            endTurn(); // End turn after reinforcement.
        } else {
            System.out.println("Smart AI (" + teamNames[currentTeam] + ") attacks from region " + bestMove[0] + " to region " + bestMove[1] + " with score " + bestScore);
            executeMove(bestMove[0], bestMove[1]);
            endTurn();
        }
    }
}

/* FortuneVoronoi: Computes the Voronoi diagram.
   NOTE: This is a placeholder implementation. A full Fortune's algorithm would
   involve event queues and a beach line. Here we use a simple nearest-neighbor approach. */
class FortuneVoronoi {
    private int[][] regionAssignment;
    private BufferedImage voronoiImage;
    private List<Point> sites;
    private int mapWidth;
    private int mapHeight;
    
    public FortuneVoronoi(Point[] siteArray, int mapWidth, int mapHeight) {
        this.sites = new ArrayList<Point>(Arrays.asList(siteArray));
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        computeVoronoi();
    }
    
    private void computeVoronoi() {
        regionAssignment = new int[mapWidth][mapHeight];
        voronoiImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int nearestSite = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < sites.size(); i++) {
                    double dist = sites.get(i).distance(x, y);
                    if (dist < minDist) {
                        minDist = dist;
                        nearestSite = i;
                    }
                }
                regionAssignment[x][y] = nearestSite;
                // The initial color here is just a placeholder.
                float hue = (float) nearestSite / sites.size();
                int rgb = Color.HSBtoRGB(hue, 1.0f, 0.8f);
                voronoiImage.setRGB(x, y, rgb);
            }
        }
    }
    
    public int[][] getRegionAssignment() {
        return regionAssignment;
    }
    
    public BufferedImage getVoronoiImage() {
        return voronoiImage;
    }
}
