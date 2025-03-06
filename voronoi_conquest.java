import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoronoiSquares extends JPanel {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int NUM_SITES = 20;
    
    // Turn-based and team settings.
    private int numTeams = 5;
    private int currentTeam = 0;
    // Define distinct hues for each team.
    private float[] teamHues = {0.0f, 0.67f, 0.33f, 0.15f, 0.83f};
    private String[] teamNames = {"Red", "Blue", "Green", "Yellow", "Purple"};
    // Which teams are controlled by AI. By default, team 0 is human; others may be AI.
    private boolean[] teamIsAI = new boolean[numTeams];
    private boolean aiEnabled = false; // Toggleable via a checkbox.
    
    // Region data.
    private Point[] sites;
    private Color[] siteColors;
    private int[] troops;             // raw troop count
    private double[] combatPower;     // effective strength (troops * multiplier)
    private int[] regionTeam;         // team affiliation for each region
    private boolean[] isBastion;      // if true, multiplier = 1.5, else 1.0
    
    // Voronoi diagram data.
    private int[][] regionAssignment;
    private boolean[][] borders;
    private BufferedImage voronoiImage;
    private boolean[][] adjacent;
    
    // Interaction variables.
    private int selectedSource = -1;  // Selected source region (for human moves).
    private int lastMoveSource = -1, lastMoveDest = -1; // For drawing the move arrow.
    private int highlightedSite = -1;
    private int mouseX = 0, mouseY = 0;
    
    private Random rand = new Random();
    
    public VoronoiSquares() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        sites = new Point[NUM_SITES];
        siteColors = new Color[NUM_SITES];
        troops = new int[NUM_SITES];
        combatPower = new double[NUM_SITES];
        regionTeam = new int[NUM_SITES];
        isBastion = new boolean[NUM_SITES];
        regionAssignment = new int[WIDTH][HEIGHT];
        borders = new boolean[WIDTH][HEIGHT];
        adjacent = new boolean[NUM_SITES][NUM_SITES];
        
        // By default, let team 0 be human and all others be AI (if AI is enabled).
        for (int i = 0; i < numTeams; i++) {
            teamIsAI[i] = (i != 0);
        }
        
        // Initialize regions with random troop counts (10-50) and assign teams round-robin.
        for (int i = 0; i < NUM_SITES; i++) {
            int troopCount = rand.nextInt(41) + 10; // 10 to 50
            troops[i] = troopCount;
            isBastion[i] = false;
            combatPower[i] = troopCount * 1.0;  // normal multiplier 1.0
            int team = i % numTeams;
            regionTeam[i] = team;
            float brightness = computeBrightness(troops[i]);
            sites[i] = new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT));
            siteColors[i] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        }
        
        // Precompute the Voronoi diagram.
        voronoiImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int closestIndex = 0;
                int minDistSq = Integer.MAX_VALUE;
                for (int i = 0; i < NUM_SITES; i++) {
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
        
        // Compute borders: a pixel is a border if any 4-neighbor is in a different region.
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int cur = regionAssignment[x][y];
                boolean isBorder = false;
                if (x > 0 && regionAssignment[x - 1][y] != cur) isBorder = true;
                else if (x < WIDTH - 1 && regionAssignment[x + 1][y] != cur) isBorder = true;
                else if (y > 0 && regionAssignment[x][y - 1] != cur) isBorder = true;
                else if (y < HEIGHT - 1 && regionAssignment[x][y + 1] != cur) isBorder = true;
                borders[x][y] = isBorder;
            }
        }
        
        // Compute adjacency between regions.
        computeAdjacency();
        
        // Remove rivers: no river initialization.
        
        // Mouse listeners for human moves and double-click bastion creation.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Double-click: turn region into bastion.
                if (e.getClickCount() >= 2) {
                    int mx = e.getX();
                    int my = e.getY();
                    if (mx < 0 || mx >= WIDTH || my < 0 || my >= HEIGHT) return;
                    int clickedRegion = regionAssignment[mx][my];
                    if (!isBastion[clickedRegion]) {
                        isBastion[clickedRegion] = true;
                        combatPower[clickedRegion] = troops[clickedRegion] * 1.5;
                        System.out.println("Region " + clickedRegion + " turned into a bastion.");
                        repaint();
                    }
                    return;
                }
                
                // Ignore clicks if it's an AI turn.
                if (teamIsAI[currentTeam] && aiEnabled) return;
                
                int mx = e.getX();
                int my = e.getY();
                if (mx < 0 || mx >= WIDTH || my < 0 || my >= HEIGHT) return;
                int clickedRegion = regionAssignment[mx][my];
                
                // Selection: human can only select their own region.
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
                    // A source is selected; check if the clicked region is adjacent.
                    if (clickedRegion == selectedSource) {
                        selectedSource = -1;
                        repaint();
                        return;
                    }
                    if (!adjacent[selectedSource][clickedRegion]) {
                        System.out.println("Region " + clickedRegion + " is not adjacent.");
                        return;
                    }
                    
                    // Execute move.
                    executeMove(selectedSource, clickedRegion);
                    selectedSource = -1;
                    endTurn();
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
                if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
                    highlightedSite = regionAssignment[mouseX][mouseY];
                } else {
                    highlightedSite = -1;
                }
                repaint();
            }
        });
    }
    
    // Compute brightness based on raw troop count.
    private float computeBrightness(int troopCount) {
        float brightness = 0.8f - (troopCount - 10) * 0.004f;
        return Math.max(0.3f, brightness);
    }
    
    // Update a region's stats: its color and combat power.
    private void updateRegionStats(int regionIndex) {
        float brightness = computeBrightness(troops[regionIndex]);
        int team = regionTeam[regionIndex];
        siteColors[regionIndex] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
        combatPower[regionIndex] = troops[regionIndex] * (isBastion[regionIndex] ? 1.5 : 1.0);
    }
    
    // Execute a move from source to destination.
    private void executeMove(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        double sourcePower = combatPower[source];
        double destPower = combatPower[dest];
        System.out.println("Moving troops from region " + source + " to region " + dest);
        lastMoveSource = source;
        lastMoveDest = dest;
        
        // Friendly move: merge raw troops.
        if (siteColors[source].equals(siteColors[dest])) {
            troops[dest] = destTroops + sourceTroops;
            troops[source] = 0;
        } else {
            // Enemy move: resolve combat using effective power.
            if (sourcePower > destPower) {
                double multiplier = isBastion[source] ? 1.5 : 1.0;
                int newTroops = (int)Math.floor((sourcePower - destPower) / multiplier);
                troops[dest] = newTroops;
                regionTeam[dest] = regionTeam[source];
                siteColors[dest] = siteColors[source];
                isBastion[dest] = isBastion[source];
                troops[source] = 0;
            } else {
                double multiplier = isBastion[dest] ? 1.5 : 1.0;
                int newTroops = (int)Math.floor((destPower - sourcePower) / multiplier);
                troops[dest] = newTroops;
                troops[source] = 0;
            }
        }
        
        updateRegionStats(source);
        updateRegionStats(dest);
        updateVoronoiImage();
        repaint();
    }
    
    // Compute adjacent regions by scanning neighboring pixels.
    private void computeAdjacency() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int cur = regionAssignment[x][y];
                if (x < WIDTH - 1) {
                    int neighbor = regionAssignment[x + 1][y];
                    if (cur != neighbor) {
                        adjacent[cur][neighbor] = true;
                        adjacent[neighbor][cur] = true;
                    }
                }
                if (y < HEIGHT - 1) {
                    int neighbor = regionAssignment[x][y + 1];
                    if (cur != neighbor) {
                        adjacent[cur][neighbor] = true;
                        adjacent[neighbor][cur] = true;
                    }
                }
            }
        }
    }
    
    // Rebuild the Voronoi image based on current region colors.
    private void updateVoronoiImage() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int region = regionAssignment[x][y];
                voronoiImage.setRGB(x, y, siteColors[region].getRGB());
            }
        }
    }
    
    // End turn: add 5 troops (no cap) to every region, skip teams with no regions, and handle AI.
    public void endTurn() {
        for (int i = 0; i < NUM_SITES; i++) {
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
                    doAIMove();
                    ((Timer)e.getSource()).stop();
                }
            }).start();
        }
    }
    
    // Check if a given team controls any regions.
    private boolean teamHasTiles(int team) {
        for (int i = 0; i < NUM_SITES; i++) {
            if (regionTeam[i] == team) return true;
        }
        return false;
    }
    
    // AI move: choose a random valid move for the current team.
    private void doAIMove() {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < NUM_SITES; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < NUM_SITES; j++) {
                    if (adjacent[i][j]) {
                        moves.add(new int[]{i, j});
                    }
                }
            }
        }
        if (moves.isEmpty()) {
            System.out.println("AI (" + teamNames[currentTeam] + ") has no valid moves. Skipping turn.");
            endTurn();
            return;
        }
        int[] move = moves.get(rand.nextInt(moves.size()));
        System.out.println("AI (" + teamNames[currentTeam] + ") moves from region " + move[0] + " to region " + move[1]);
        executeMove(move[0], move[1]);
        endTurn();
    }
    
    // Utility to draw an arrow from (x1, y1) to (x2, y2).
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
        // Draw the base Voronoi diagram.
        g.drawImage(voronoiImage, 0, 0, null);
        
        // Draw borders.
        g.setColor(Color.BLACK);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (borders[x][y]) {
                    g.drawLine(x, y, x, y);
                }
            }
        }
        
        // Draw site points.
        g.setColor(Color.BLACK);
        for (Point p : sites) {
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
        
        // Rivers removed.
        
        // Highlight selected source region.
        if (selectedSource != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, 100));
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (regionAssignment[x][y] == selectedSource) {
                        g2.fillRect(x, y, 1, 1);
                    }
                }
            }
            g2.dispose();
        }
        
        // Draw move arrow if applicable.
        if (lastMoveSource != -1 && lastMoveDest != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.MAGENTA);
            Point from = sites[lastMoveSource];
            Point to = sites[lastMoveDest];
            drawArrow(g2, from.x, from.y, to.x, to.y);
            g2.dispose();
        }
        
        // Display troop count and combat power on hover.
        if (highlightedSite != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            String text = "Troops: " + troops[highlightedSite] + " | Power: " + String.format("%.1f", combatPower[highlightedSite]);
            g2.drawString(text, mouseX + 10, mouseY + 10);
            g2.dispose();
        }
        
        // Display current turn info.
        g.setColor(Color.BLACK);
        g.drawString("Current Turn: " + teamNames[currentTeam], 10, 20);
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Voronoi Combat Simulation without Rivers");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        VoronoiSquares panel = new VoronoiSquares();
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel();
        JButton endTurnButton = new JButton("End Turn");
        endTurnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(panel.aiEnabled && panel.teamIsAI[panel.currentTeam])) {
                    panel.endTurn();
                }
            }
        });
        controlPanel.add(endTurnButton);
        
        JCheckBox aiCheckBox = new JCheckBox("Enable AI", false);
        aiCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.aiEnabled = aiCheckBox.isSelected();
                for (int i = 0; i < panel.numTeams; i++) {
                    panel.teamIsAI[i] = (i != 0) && panel.aiEnabled;
                }
                System.out.println("AI enabled: " + panel.aiEnabled);
            }
        });
        controlPanel.add(aiCheckBox);
        
        frame.add(controlPanel, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
