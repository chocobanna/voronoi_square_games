// File: main/GameEngine.java
package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameEngine implements AIContext {
    private int mapWidth, mapHeight, numRegions, numTeams;
    double smartRisk;
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

    // ExecutorService for AI computations.
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(2);

    // Game over flag.
    private boolean gameOver = false;

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
            troops[i] = rand.nextInt(41) + 10;
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

    // AI turn initiation now uses a generalized AIManager.
    public void startTurnIfAI() {
        if (gameOver) return;
        if (teamControls[currentTeam] != TeamControl.HOTSEAT) {
            aiExecutor.submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) { }
                mods.AIManager.doMove(this);
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

    public void handleMouseClick(int worldX, int worldY, MouseEvent e) {
        if (gameOver) return;
        if (regionAssignment == null) return;
        int x = worldX;
        int y = worldY;
        if (!isValidCoordinate(x, y)) return;
        int clickedRegion = regionAssignment[x][y];

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
        if (gameOver) return;
        if (regionAssignment == null) return;
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
        if (gameOver) return;
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
        if (checkVictory()) {
            gameOver = true;
            int winningTeam = regionTeam[0];
            JOptionPane.showMessageDialog(null, teamNames[winningTeam] + " wins!");
            System.out.println(teamNames[winningTeam] + " wins!");
            return;
        }
        System.out.println("Turn ended. Current turn: " + teamNames[currentTeam] + " (" + teamControls[currentTeam] + ")");
        if (teamControls[currentTeam] != TeamControl.HOTSEAT) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) { }
                SwingUtilities.invokeLater(() -> {
                    mods.AIManager.doMove(this);
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

    private boolean checkVictory() {
        int firstTeam = regionTeam[0];
        for (int i = 1; i < numRegions; i++) {
            if (regionTeam[i] != firstTeam) return false;
        }
        return true;
    }

    // --- AIContext Getters ---
    public int getNumRegions() { return numRegions; }
    public int[] getRegionTeam() { return regionTeam; }
    public int[] getTroops() { return troops; }
    public boolean[][] getAdjacent() { return adjacent; }
    public double[] getCombatPower() { return combatPower; }
    public String[] getTeamNames() { return teamNames; }
    public int getCurrentTeam() { return currentTeam; }
    public Random getRand() { return rand; }
    public double getSmartRisk() { return smartRisk; }
    
    // New method to return current team's control type.
    public TeamControl getTeamControl() {
        return teamControls[currentTeam];
    }
}
