import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

enum TeamControl {
    HOTSEAT, DUMB, SMART, LOSING
}

/* Main entry point. */
public class VoronoiConquest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame("Voronoi Conquest - Main Menu");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MainMenuPanel menu = new MainMenuPanel(mainFrame);
            mainFrame.getContentPane().add(menu);
            mainFrame.pack();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });
    }
}

/* MainMenuPanel: Handles user input and team configuration. */
class MainMenuPanel extends JPanel {
    private JTextField widthField;
    private JTextField heightField;
    private JTextField numRegionsField;
    private JSlider numTeamsSlider;
    private JLabel teamsLabel;
    private JPanel teamConfigPanel;
    private List<JComboBox<String>> teamComboBoxes;
    private JFrame parentFrame;
    
    private final String[] availableNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
    private final String[] controlOptions = {"Hotseat", "Dumb AI", "Smart AI", "Losing is Fun"};
    
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
                        case 0: teamControls[i] = TeamControl.HOTSEAT; break;
                        case 1: teamControls[i] = TeamControl.DUMB; break;
                        case 2: teamControls[i] = TeamControl.SMART; break;
                        case 3: teamControls[i] = TeamControl.LOSING; break;
                        default: teamControls[i] = TeamControl.DUMB; break;
                    }
                }
                
                JFrame gameFrame = new JFrame("Voronoi Conquest - Battle");
                gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                double smartRisk = 0.5;
                GamePanel gamePanel = new GamePanel(width, height, numRegions, numTeams, teamControls, smartRisk);
                gameFrame.getContentPane().add(gamePanel);
                gameFrame.pack();
                gameFrame.setLocationRelativeTo(null);
                gameFrame.setVisible(true);
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

/* GamePanel: Renders the game, applies zoom/pan transformations, and manages mouse interactions.
   The Voronoi diagram is computed in a background thread.
   Zooming is controlled via the scrollwheel and panning via middle-mouse dragging.
*/
class GamePanel extends JPanel {
    private GameEngine engine;
    private int mapWidth, mapHeight;
    
    // Zoom and pan state.
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private Point dragStart; // For panning
    
    public GamePanel(int mapWidth, int mapHeight, int numRegions, int numTeams, TeamControl[] teamControls, double smartRisk) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        engine = new GameEngine(mapWidth, mapHeight, numRegions, numTeams, teamControls, smartRisk);
        
        // Compute Voronoi diagram off the EDT.
        new Thread(() -> {
            FortuneVoronoi voronoi = new FortuneVoronoi(engine.getSites(), mapWidth, mapHeight);
            SwingUtilities.invokeLater(() -> {
                engine.setRegionAssignment(voronoi.getRegionAssignment());
                engine.setVoronoiImage(voronoi.getVoronoiImage());
                engine.refreshVoronoiImage();
                engine.startTurnIfAI();
                repaint();
            });
        }).start();
        
        // Mouse listener for clicks and panning.
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Only process left/right clicks for game actions.
                if (!SwingUtilities.isMiddleMouseButton(e)) {
                    int worldX = (int) ((e.getX() - translateX) / scale);
                    int worldY = (int) ((e.getY() - translateY) / scale);
                    engine.handleMouseClick(worldX, worldY, e);
                    repaint();
                }
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    dragStart = e.getPoint();
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int worldX = (int) ((e.getX() - translateX) / scale);
                int worldY = (int) ((e.getY() - translateY) / scale);
                engine.handleMouseMove(worldX, worldY, e);
                repaint();
            }
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    Point dragEnd = e.getPoint();
                    translateX += dragEnd.x - dragStart.x;
                    translateY += dragEnd.y - dragStart.y;
                    dragStart = dragEnd;
                    repaint();
                }
            }
        });
        
        // Mouse wheel listener for zooming.
        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            double oldScale = scale;
            if (notches < 0) {
                scale *= 1.1; // Zoom in.
            } else {
                scale /= 1.1; // Zoom out.
            }
            // Adjust translation so that zoom centers on the mouse pointer.
            double mouseX = e.getX();
            double mouseY = e.getY();
            translateX = mouseX - ((mouseX - translateX) * (scale / oldScale));
            translateY = mouseY - ((mouseY - translateY) * (scale / oldScale));
            repaint();
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Apply translation and scaling.
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform original = g2d.getTransform();
        g2d.translate(translateX, translateY);
        g2d.scale(scale, scale);
        engine.draw(g2d);
        g2d.setTransform(original);
    }
}

/* GameEngine: Contains game state, rules, and turn management.
   Mouse interactions now use world coordinates.
   AI moves are offloaded via an ExecutorService to keep the UI responsive.
*/
class GameEngine {
    private int mapWidth, mapHeight, numRegions, numTeams;
    private double smartRisk;
    private TeamControl[] teamControls;
    
    // Region data.
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
    
    // Interaction state.
    private int selectedRegion = -1;
    private int highlightedRegion = -1;
    private int mouseX = 0, mouseY = 0;
    private int lastMoveSource = -1, lastMoveDest = -1;
    
    private Random rand = new Random();
    
    // ExecutorService for offloading AI computations.
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(2);
    
    public GameEngine(int mapWidth, int mapHeight, int numRegions, int numTeams, TeamControl[] teamControls, double smartRisk) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.numRegions = numRegions;
        this.numTeams = numTeams;
        this.teamControls = teamControls;
        this.smartRisk = smartRisk;
        initTeams();
        initRegions();
    }
    
    private void initTeams() {
        teamNames = new String[numTeams];
        teamHues = new float[numTeams];
        String[] availableNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
        float[] availableHues = {0.0f, 0.67f, 0.33f, 0.15f, 0.83f};
        for (int i = 0; i < numTeams; i++) {
            teamNames[i] = availableNames[i];
            teamHues[i] = availableHues[i];
        }
    }
    
    private void initRegions() {
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
            combatPower[i] = troops[i];
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
    
    public void refreshVoronoiImage() {
        updateVoronoiImage();
    }
    
    // If current team is AI-controlled, start its turn in a background thread.
    public void startTurnIfAI() {
        if (teamControls[currentTeam] != TeamControl.HOTSEAT) {
            aiExecutor.submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) { }
                switch (teamControls[currentTeam]) {
                    case DUMB:
                        doDumbAIMove();
                        break;
                    case SMART:
                        doSmartAIMove();
                        break;
                    case LOSING:
                        doLosingIsFunAIMove();
                        break;
                    default:
                        break;
                }
            });
        }
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
    
    // Mouse interactions using world coordinates.
    public void handleMouseClick(int worldX, int worldY, MouseEvent e) {
        int x = worldX;
        int y = worldY;
        if (!isValidCoordinate(x, y)) return;
        int clickedRegion = regionAssignment[x][y];
        
        // Right-click: reinforcement move.
        if (SwingUtilities.isRightMouseButton(e)) {
            if (regionTeam[clickedRegion] == currentTeam && troops[clickedRegion] >= 10) {
                executeReinforce(clickedRegion, clickedRegion);
                System.out.println("Player reinforced region " + clickedRegion + " with 100% bonus.");
                if (teamControls[currentTeam] == TeamControl.HOTSEAT) {
                    endTurn();
                }
            }
            return;
        }
        
        // Double-click: toggle bastion.
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
        
        // Left-click: process player move.
        if (teamControls[currentTeam] != TeamControl.HOTSEAT) return;
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
                selectedRegion = -1;
                return;
            }
            if (!adjacent[selectedRegion][clickedRegion]) {
                System.out.println("Region " + clickedRegion + " is not adjacent to region " + selectedRegion);
                return;
            }
            // Friendly move: reinforcement ends turn.
            if (regionTeam[selectedRegion] == regionTeam[clickedRegion]) {
                executeReinforce(selectedRegion, clickedRegion);
                selectedRegion = -1;
                System.out.println("Player reinforced region " + clickedRegion);
                endTurn();
            } else {
                executeMove(selectedRegion, clickedRegion);
                selectedRegion = -1;
                endTurn();
            }
        }
    }
    
    public void handleMouseMove(int worldX, int worldY, MouseEvent e) {
        mouseX = worldX;
        mouseY = worldY;
        if (isValidCoordinate(mouseX, mouseY)) {
            highlightedRegion = regionAssignment[mouseX][mouseY];
        } else {
            highlightedRegion = -1;
        }
    }
    
    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < mapWidth && y >= 0 && y < mapHeight;
    }
    
    public void draw(Graphics2D g2d) {
        if (voronoiImage != null) {
            g2d.drawImage(voronoiImage, 0, 0, null);
        }
        g2d.setColor(Color.BLACK);
        g2d.drawString("Current Turn: " + teamNames[currentTeam] + " (" + teamControls[currentTeam] + ")", 10, 20);
        if (highlightedRegion != -1) {
            String text = "Troops: " + troops[highlightedRegion] + " | Power: " + String.format("%.1f", combatPower[highlightedRegion]);
            Font originalFont = g2d.getFont();
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(mouseX + 10, mouseY + 10 - fm.getAscent(), textWidth, textHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, mouseX + 10, mouseY + 10);
            g2d.setFont(originalFont);
        }
        if (selectedRegion != -1) {
            g2d.setColor(Color.BLACK);
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (regionAssignment[x][y] == selectedRegion) {
                        boolean isBoundary = (x == 0 || regionAssignment[x - 1][y] != selectedRegion) ||
                                             (x == mapWidth - 1 || regionAssignment[x + 1][y] != selectedRegion) ||
                                             (y == 0 || regionAssignment[x][y - 1] != selectedRegion) ||
                                             (y == mapHeight - 1 || regionAssignment[x][y + 1] != selectedRegion);
                        if (isBoundary) {
                            g2d.drawRect(x, y, 1, 1);
                        }
                    }
                }
            }
        }
        if (lastMoveSource != -1 && lastMoveDest != -1) {
            g2d.setColor(Color.MAGENTA);
            Point from = sites[lastMoveSource];
            Point to = sites[lastMoveDest];
            drawArrow(g2d, from.x, from.y, to.x, to.y);
        }
    }
    
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowHeadLength = 10;
        double arrowAngle = Math.toRadians(20);
        int xArrow1 = (int) (x2 - arrowHeadLength * Math.cos(angle - arrowAngle));
        int yArrow1 = (int) (y2 - arrowHeadLength * Math.sin(angle - arrowAngle));
        int xArrow2 = (int) (x2 - arrowHeadLength * Math.cos(angle + arrowAngle));
        int yArrow2 = (int) (y2 - arrowHeadLength * Math.sin(angle + arrowAngle));
        g2d.drawLine(x2, y2, xArrow1, yArrow1);
        g2d.drawLine(x2, y2, xArrow2, yArrow2);
    }
    
    public void executeMove(int source, int dest) {
        System.out.println("Moving troops from region " + source + " to region " + dest);
        lastMoveSource = source;
        lastMoveDest = dest;
        if (siteColors[source].equals(siteColors[dest])) {
            troops[dest] += troops[source];
            troops[source] = 0;
        } else {
            double sourcePower = combatPower[source];
            double destPower = combatPower[dest];
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
        lastMoveSource = source;
        lastMoveDest = dest;
        troops[dest] += 2 * troops[source];
        troops[source] = 0;
        updateRegionStats(source);
        updateRegionStats(dest);
        updateVoronoiImage();
        System.out.println("Reinforced region " + dest + " with a 100% bonus.");
    }
    
    private void updateRegionStats(int regionIndex) {
        float brightness = computeBrightness(troops[regionIndex]);
        int team = regionTeam[regionIndex];
        siteColors[regionIndex] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        double baseMultiplier = isBastion[regionIndex] ? 1.5 : 1.0;
        combatPower[regionIndex] = troops[regionIndex] * baseMultiplier;
    }
    
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
    
    public void endTurn() {
        for (int i = 0; i < numRegions; i++) {
            troops[i] += 5;
            updateRegionStats(i);
        }
        updateVoronoiImage();
        selectedRegion = -1;
        currentTeam = (currentTeam + 1) % numTeams;
        while (!teamHasTiles(currentTeam)) {
            System.out.println("Skipping turn for " + teamNames[currentTeam] + " (no tiles).");
            currentTeam = (currentTeam + 1) % numTeams;
        }
        System.out.println("Turn ended. Current turn: " + teamNames[currentTeam] + " (" + teamControls[currentTeam] + ")");
        if (teamControls[currentTeam] != TeamControl.HOTSEAT) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) { }
                SwingUtilities.invokeLater(() -> {
                    switch (teamControls[currentTeam]) {
                        case DUMB:
                            doDumbAIMove();
                            break;
                        case SMART:
                            doSmartAIMove();
                            break;
                        case LOSING:
                            doLosingIsFunAIMove();
                            break;
                        default:
                            break;
                    }
                });
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
        aiExecutor.submit(() -> {
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
                SwingUtilities.invokeLater(this::endTurn);
                return;
            }
            int[] move = moves.get(rand.nextInt(moves.size()));
            SwingUtilities.invokeLater(() -> {
                if (regionTeam[move[0]] == regionTeam[move[1]]) {
                    System.out.println("Dumb AI (" + teamNames[currentTeam] + ") reinforces region " + move[1]);
                    executeReinforce(move[0], move[1]);
                } else {
                    System.out.println("Dumb AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1]);
                    executeMove(move[0], move[1]);
                }
                endTurn();
            });
        });
    }
    
    private void doSmartAIMove() {
        aiExecutor.submit(() -> {
            double bestScore = Double.NEGATIVE_INFINITY;
            int[] bestMove = null;
            boolean isFriendly = false;
            for (int i = 0; i < numRegions; i++) {
                if (regionTeam[i] == currentTeam && troops[i] > 0) {
                    for (int j = 0; j < numRegions; j++) {
                        if (adjacent[i][j]) {
                            double score;
                            if (regionTeam[i] == regionTeam[j]) {
                                score = 2 * troops[i];
                            } else {
                                double sourcePower = combatPower[i];
                                double destPower = combatPower[j];
                                score = (sourcePower > destPower) ? (sourcePower - destPower) * (1 - smartRisk) : -1000;
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
            final int[] move = bestMove;
            final double score = bestScore;
            final boolean friendly = isFriendly;
            SwingUtilities.invokeLater(() -> {
                if (move == null || score <= 0) {
                    System.out.println("Smart AI (" + teamNames[currentTeam] + ") found no advantageous moves. Skipping turn.");
                    endTurn();
                    return;
                }
                if (friendly) {
                    System.out.println("Smart AI (" + teamNames[currentTeam] + ") reinforces from region " + move[0] + " to region " + move[1] + " with score " + score);
                    executeReinforce(move[0], move[1]);
                } else {
                    System.out.println("Smart AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1] + " with score " + score);
                    executeMove(move[0], move[1]);
                }
                endTurn();
            });
        });
    }
    
    private void doLosingIsFunAIMove() {
        aiExecutor.submit(() -> {
            double worstScore = Double.POSITIVE_INFINITY;
            int[] worstMove = null;
            boolean isFriendly = false;
            for (int i = 0; i < numRegions; i++) {
                if (regionTeam[i] == currentTeam && troops[i] > 0) {
                    for (int j = 0; j < numRegions; j++) {
                        if (adjacent[i][j]) {
                            double score;
                            if (regionTeam[i] == regionTeam[j]) {
                                score = 2 * troops[i];
                            } else {
                                double sourcePower = combatPower[i];
                                double destPower = combatPower[j];
                                score = (sourcePower - destPower) * (1 - smartRisk);
                            }
                            if (score < worstScore) {
                                worstScore = score;
                                worstMove = new int[]{i, j};
                                isFriendly = (regionTeam[i] == regionTeam[j]);
                            }
                        }
                    }
                }
            }
            final int[] move = worstMove;
            final double score = worstScore;
            final boolean friendly = isFriendly;
            SwingUtilities.invokeLater(() -> {
                if (move == null) {
                    System.out.println("Losing is Fun AI (" + teamNames[currentTeam] + ") has no valid moves. Skipping turn.");
                    endTurn();
                    return;
                }
                if (friendly) {
                    System.out.println("Losing is Fun AI (" + teamNames[currentTeam] + ") reinforces from region " + move[0] + " to region " + move[1] + " with score " + score);
                    executeReinforce(move[0], move[1]);
                } else {
                    System.out.println("Losing is Fun AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1] + " with score " + score);
                    executeMove(move[0], move[1]);
                }
                endTurn();
            });
        });
    }
}

/* FortuneVoronoi: Computes a simple Voronoi diagram via a nearest-neighbor approach.
   This class is independent of the UI.
*/
class FortuneVoronoi {
    private int[][] regionAssignment;
    private BufferedImage voronoiImage;
    private List<Point> sites;
    private int mapWidth, mapHeight;
    
    public FortuneVoronoi(Point[] siteArray, int mapWidth, int mapHeight) {
        this.sites = new ArrayList<>(Arrays.asList(siteArray));
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
