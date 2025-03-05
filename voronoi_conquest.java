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
    private static final int NUM_SITES = 10;
    
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
    private int[] troops;
    private int[] regionTeam; // Team affiliation for each region.
    
    // Voronoi diagram data.
    private int[][] regionAssignment;
    private boolean[][] borders;
    private BufferedImage voronoiImage;
    private boolean[][] adjacent;
    
    // Interaction variables.
    private int selectedSource = -1;  // Selected source region (human move).
    private int lastMoveSource = -1, lastMoveDest = -1; // For drawing the move arrow.
    private int highlightedSite = -1;
    private int mouseX = 0, mouseY = 0;
    
    private Random rand = new Random();
    
    public VoronoiSquares() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        sites = new Point[NUM_SITES];
        siteColors = new Color[NUM_SITES];
        troops = new int[NUM_SITES];
        regionTeam = new int[NUM_SITES];
        regionAssignment = new int[WIDTH][HEIGHT];
        borders = new boolean[WIDTH][HEIGHT];
        adjacent = new boolean[NUM_SITES][NUM_SITES];
        
        // By default, let team 0 be human and all others be AI (if AI is enabled).
        for (int i = 0; i < numTeams; i++) {
            teamIsAI[i] = (i != 0); // Only team 0 is human by default.
        }
        
        // Initialize regions with random troop counts (between 10 and 50) and assign teams round-robin.
        for (int i = 0; i < NUM_SITES; i++) {
            int troopCount = rand.nextInt(41) + 10; // Start with a moderate number.
            troops[i] = troopCount;
            int team = i % numTeams;
            regionTeam[i] = team;
            float brightness = computeBrightness(troopCount);
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
        
        // Compute borders (a pixel is a border if any 4-neighbor belongs to a different region).
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
        
        // Mouse listeners for human moves.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                highlightedSite = -1;
                repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // If it's not a human turn, ignore mouse clicks.
                if (teamIsAI[currentTeam] && aiEnabled) return;
                
                int mx = e.getX();
                int my = e.getY();
                if (mx < 0 || mx >= WIDTH || my < 0 || my >= HEIGHT) return;
                int clickedRegion = regionAssignment[mx][my];
                
                // Selection: only allow human to select their own region.
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
                    // A source is already selected. Check if the clicked region is adjacent.
                    if (clickedRegion == selectedSource) {
                        selectedSource = -1;
                        repaint();
                        return;
                    }
                    if (!adjacent[selectedSource][clickedRegion]) {
                        System.out.println("Region " + clickedRegion + " is not adjacent.");
                        return;
                    }
                    
                    // Execute troop movement.
                    executeMove(selectedSource, clickedRegion);
                    selectedSource = -1;
                    endTurn();
                }
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
    
    // Compute brightness based on troop count.
    private float computeBrightness(int troopCount) {
        // Start at 0.8 for 10 troops, and decrease gradually with a minimum brightness of 0.3.
        float brightness = 0.8f - (troopCount - 10) * 0.004f;
        return Math.max(0.3f, brightness);
    }
    
    // Execute a move from source to destination.
    private void executeMove(int source, int dest) {
        int sourceTroops = troops[source];
        int destTroops = troops[dest];
        Color sourceColor = siteColors[source];
        Color destColor = siteColors[dest];
        
        System.out.println("Moving troops from region " + source + " to region " + dest);
        lastMoveSource = source;
        lastMoveDest = dest;
        
        if (sourceColor.equals(destColor)) {
            // Friendly move: merge troops.
            troops[dest] = destTroops + sourceTroops;
            troops[source] = 0;
        } else {
            // Enemy move: combat resolution.
            if (sourceTroops > destTroops) {
                // Attacker wins: convert the region.
                troops[dest] = sourceTroops - destTroops;
                siteColors[dest] = sourceColor;
                regionTeam[dest] = regionTeam[source];
                troops[source] = 0;
            } else {
                // Defender holds.
                troops[dest] = destTroops - sourceTroops;
                troops[source] = 0;
            }
        }
        
        updateRegionColor(source);
        updateRegionColor(dest);
        updateVoronoiImage();
        repaint();
    }
    
    // Compute which regions are adjacent by scanning neighboring pixels.
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
    
    // Update a region's color based on its troop count and team affiliation.
    private void updateRegionColor(int regionIndex) {
        int troopCount = troops[regionIndex];
        float brightness = computeBrightness(troopCount);
        int team = regionTeam[regionIndex];
        siteColors[regionIndex] = Color.getHSBColor(teamHues[team], 1.0f, brightness);
    }
    
    // Rebuild the Voronoi image based on current site colors.
    private void updateVoronoiImage() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int region = regionAssignment[x][y];
                voronoiImage.setRGB(x, y, siteColors[region].getRGB());
            }
        }
    }
    
    // End turn: add 5 troops to every region, skip turn if current team has no tiles, and if AI is enabled, auto-move.
    public void endTurn() {
        // Reinforcement: add 5 troops to every region.
        for (int i = 0; i < NUM_SITES; i++) {
            troops[i] += 5;
            updateRegionColor(i);
        }
        updateVoronoiImage();
        selectedSource = -1;
        
        // Advance turn.
        currentTeam = (currentTeam + 1) % numTeams;
        // Skip teams with no tiles.
        while (!teamHasTiles(currentTeam)) {
            System.out.println("Skipping turn for " + teamNames[currentTeam] + " (no tiles).");
            currentTeam = (currentTeam + 1) % numTeams;
        }
        
        System.out.println("Turn ended. Current turn: " + teamNames[currentTeam]);
        repaint();
        
        // If AI is enabled and the current team is AI, schedule an AI move.
        if (aiEnabled && teamIsAI[currentTeam]) {
            // Use a Swing Timer for a short delay.
            new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doAIMove();
                    ((Timer)e.getSource()).stop();
                }
            }).start();
        }
    }
    
    // Check if a given team has any regions.
    private boolean teamHasTiles(int team) {
        for (int i = 0; i < NUM_SITES; i++) {
            if (regionTeam[i] == team) return true;
        }
        return false;
    }
    
    // AI move: choose a random valid move for the current team.
    private void doAIMove() {
        List<int[]> moves = new ArrayList<>();
        // For each region belonging to currentTeam with troops, check adjacent regions.
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
        // Pick a random move.
        int[] move = moves.get(rand.nextInt(moves.size()));
        System.out.println("AI (" + teamNames[currentTeam] + ") moves from region " + move[0] + " to region " + move[1]);
        executeMove(move[0], move[1]);
        endTurn();
    }
    
    // Utility to draw an arrow from (x1, y1) to (x2, y2)
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
        // Draw the Voronoi diagram.
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
        
        // Highlight selected source region.
        if (selectedSource != -1) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(0, 0, 0, 100));
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (regionAssignment[x][y] == selectedSource) {
                        g2d.fillRect(x, y, 1, 1);
                    }
                }
            }
            g2d.dispose();
        }
        
        // Draw move arrow if applicable.
        if (lastMoveSource != -1 && lastMoveDest != -1) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.MAGENTA);
            Point from = sites[lastMoveSource];
            Point to = sites[lastMoveDest];
            drawArrow(g2d, from.x, from.y, to.x, to.y);
            g2d.dispose();
        }
        
        // Display troop count on hover.
        if (highlightedSite != -1) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.BLACK);
            String text = "Troops: " + troops[highlightedSite];
            g2d.drawString(text, mouseX + 10, mouseY + 10);
            g2d.dispose();
        }
        
        // Display current turn info with team name.
        g.setColor(Color.BLACK);
        g.drawString("Current Turn: " + teamNames[currentTeam], 10, 20);
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Turn-Based Voronoi Combat Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        VoronoiSquares panel = new VoronoiSquares();
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        
        // Add a control panel for ending turn and toggling AI.
        JPanel controlPanel = new JPanel();
        JButton endTurnButton = new JButton("End Turn");
        endTurnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Only allow manual end-turn if it's human's turn.
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
                // Update teamIsAI: keep team 0 human; all others become AI if enabled.
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
