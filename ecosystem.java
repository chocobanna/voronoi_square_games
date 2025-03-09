import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

/**
 * Main class: creates two windows—one for the simulation view and one for species statistics.
 * It also starts the simulation update in a separate thread.
 */
public class EcosystemSimulationSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create simulation panel and its window.
            SimulationPanel simPanel = new SimulationPanel();
            JFrame simFrame = new JFrame("Ecosystem Simulation");
            simFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            simFrame.add(simPanel);
            simFrame.pack();
            simFrame.setLocation(100, 100);
            simFrame.setVisible(true);

            // Create statistics panel and its window.
            StatisticsPanel statsPanel = new StatisticsPanel();
            JFrame statsFrame = new JFrame("Species Statistics");
            statsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            statsFrame.add(statsPanel);
            statsFrame.pack();
            statsFrame.setLocation(950, 100);
            statsFrame.setVisible(true);

            // Create the simulation engine with references to both panels.
            SimulationEngine engine = new SimulationEngine(simPanel, statsPanel);
            simPanel.setEngine(engine);
            // Start the simulation engine in its own thread.
            Thread simThread = new Thread(engine);
            simThread.start();
        });
    }
}

/**
 * SimulationEngine holds and updates the list of organisms and the terrain.
 * It runs on a background thread and updates the UI via invokeLater.
 */
class SimulationEngine implements Runnable {
    private List<Organism> organisms;
    private Random random;
    private int timeStep;
    private boolean running;
    private SimulationPanel simPanel;
    private StatisticsPanel statsPanel;
    private Terrain terrain;

    public SimulationEngine(SimulationPanel simPanel, StatisticsPanel statsPanel) {
        this.simPanel = simPanel;
        this.statsPanel = statsPanel;
        random = new Random();
        organisms = new ArrayList<>();
        // Initialize with some flora and fauna.
        for (int i = 0; i < 50; i++) {
            organisms.add(new Flora(random, simPanel));
        }
        for (int i = 0; i < 20; i++) {
            organisms.add(new Fauna(random, simPanel));
        }
        timeStep = 0;
        // Create a dynamic terrain (cells of 40 pixels).
        terrain = new Terrain(SimulationPanel.PANEL_WIDTH, SimulationPanel.PANEL_HEIGHT, 40, random);
    }

    // Thread-safe snapshot of organisms.
    public synchronized List<Organism> getOrganismsSnapshot() {
        return new ArrayList<>(organisms);
    }

    public int getTimeStep() {
        return timeStep;
    }

    // Update the simulation state.
    public synchronized void updateSimulation() {
        timeStep++;
        terrain.update(); // update dynamic terrain and biomes
        List<Organism> newOrganisms = new ArrayList<>();
        for (Organism o : organisms) {
            o.update(organisms, newOrganisms);
            if (!o.isDead()) {
                newOrganisms.add(o);
            }
        }
        organisms = newOrganisms;
    }

    /**
     * Group organisms by scientific name and compute statistics.
     */
    public List<SpeciesStats> computeSpeciesStats() {
        Map<String, SpeciesStats> statsMap = new HashMap<>();
        synchronized (this) {
            for (Organism o : organisms) {
                String name = o.getScientificName();
                SpeciesStats stats = statsMap.get(name);
                if (stats == null) {
                    stats = new SpeciesStats(name, (o instanceof Flora ? "Flora" : "Fauna"));
                    statsMap.put(name, stats);
                }
                stats.count++;
                stats.totalAge += o.age;
                stats.totalLifespan += o.lifespan;
            }
        }
        List<SpeciesStats> list = new ArrayList<>();
        for (SpeciesStats s : statsMap.values()) {
            if (s.count > 0) {
                s.avgAge = s.totalAge / (double) s.count;
                s.avgLifespan = s.totalLifespan / (double) s.count;
            }
            list.add(s);
        }
        return list;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    @Override
    public void run() {
        running = true;
        while (running && timeStep < 1000) {
            updateSimulation();
            simPanel.setTerrain(terrain);
            SwingUtilities.invokeLater(() -> {
                simPanel.repaint();
                statsPanel.updateStats(computeSpeciesStats(), timeStep);
            });
            try {
                Thread.sleep(100); // 100ms per time step.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Terrain simulates a dynamic background divided into cells.
 * Each cell has a fertility value and a biome type.
 */
class Terrain {
    private int width, height, cellSize, cols, rows;
    private double[][] fertility;
    private int[][] biomeGrid; // 0: Forest, 1: Grassland, 2: Desert
    private Random random;
    
    // Base fertility values for each biome.
    private final double[] biomeBase = {1.2, 1.0, 0.7};

    public Terrain(int width, int height, int cellSize, Random random) {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.random = random;
        cols = width / cellSize;
        rows = height / cellSize;
        fertility = new double[cols][rows];
        biomeGrid = new int[cols][rows];
        // Initialize each cell with a random biome and fertility near the biome base.
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                biomeGrid[i][j] = random.nextInt(3);
                fertility[i][j] = biomeBase[biomeGrid[i][j]] + (random.nextDouble() - 0.5) * 0.2;
            }
        }
    }
    
    // Update fertility values (cells drift slowly around their base).
    public void update() {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double drift = (random.nextDouble() - 0.5) * 0.1;
                fertility[i][j] += drift;
                // Clamp fertility within a reasonable range.
                double min = 0.5;
                double max = 1.5;
                if (fertility[i][j] < min) fertility[i][j] = min;
                if (fertility[i][j] > max) fertility[i][j] = max;
            }
        }
    }
    
    // Get fertility at a given coordinate.
    public double getFertilityAt(int x, int y) {
        int col = Math.min(cols - 1, Math.max(0, x / cellSize));
        int row = Math.min(rows - 1, Math.max(0, y / cellSize));
        return fertility[col][row];
    }
    
    // Get biome type at a given coordinate.
    public int getBiomeAt(int x, int y) {
        int col = Math.min(cols - 1, Math.max(0, x / cellSize));
        int row = Math.min(rows - 1, Math.max(0, y / cellSize));
        return biomeGrid[col][row];
    }
    
    // Draw the terrain grid with color based on biome and fertility.
    public void draw(Graphics g) {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                int biome = biomeGrid[i][j];
                double fert = fertility[i][j];
                Color base;
                // Base colors for each biome.
                if (biome == 0) base = new Color(0, 100, 0);         // Forest: dark green
                else if (biome == 1) base = new Color(124, 252, 0);    // Grassland: lawn green
                else base = new Color(210, 180, 140);                  // Desert: tan
                // Adjust brightness by fertility.
                float ratio = (float)((fert - 0.5) / 1.0);
                int r = (int)(base.getRed() * (0.5 + 0.5 * ratio));
                int gr = (int)(base.getGreen() * (0.5 + 0.5 * ratio));
                int b = (int)(base.getBlue() * (0.5 + 0.5 * ratio));
                g.setColor(new Color(r, gr, b));
                g.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);
            }
        }
    }
}

/**
 * SpeciesStats holds statistics for a given species.
 */
class SpeciesStats {
    String scientificName;
    String type;
    int count;
    int totalAge;
    int totalLifespan;
    double avgAge;
    double avgLifespan;

    public SpeciesStats(String scientificName, String type) {
        this.scientificName = scientificName;
        this.type = type;
        this.count = 0;
        this.totalAge = 0;
        this.totalLifespan = 0;
    }
}

/**
 * SimulationPanel displays the simulation. It now supports panning and zooming.
 */
class SimulationPanel extends JPanel {
    private SimulationEngine engine;
    private Terrain terrain;
    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 600;
    
    // Pan/zoom variables.
    private double scale = 1.0;
    private int offsetX = 0, offsetY = 0;
    private Point lastDragPoint;

    public SimulationPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        // Enable panning and zooming.
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragPoint = e.getPoint();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                offsetX += p.x - lastDragPoint.x;
                offsetY += p.y - lastDragPoint.y;
                lastDragPoint = p;
                repaint();
            }
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                // Adjust scale factor.
                scale *= (notches > 0 ? 0.9 : 1.1);
                repaint();
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }
    
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }
    
    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }
    
    // Helper: get fertility at a point.
    public double getTerrainFertilityAt(int x, int y) {
        return (terrain != null) ? terrain.getFertilityAt(x, y) : 1.0;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Use Graphics2D for pan/zoom.
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);
        
        // Draw terrain.
        if (terrain != null) {
            terrain.draw(g2d);
        }
        if (engine == null)
            return;
        List<Organism> organisms = engine.getOrganismsSnapshot();
        // Draw simulation stats.
        g2d.setColor(Color.WHITE);
        g2d.drawString("Time Step: " + engine.getTimeStep(), 10, 20);
        g2d.drawString("Total Organisms: " + organisms.size(), 10, 35);
        int floraCount = 0, faunaCount = 0;
        for (Organism o : organisms) {
            if (o instanceof Flora)
                floraCount++;
            else if (o instanceof Fauna)
                faunaCount++;
        }
        g2d.drawString("Flora: " + floraCount, 10, 50);
        g2d.drawString("Fauna: " + faunaCount, 10, 65);
        
        for (Organism o : organisms) {
            o.draw(g2d);
        }
        g2d.setTransform(oldTransform);
    }
}

/**
 * StatisticsPanel displays species statistics in a JTable.
 */
class StatisticsPanel extends JPanel {
    private JTable table;
    private SpeciesStatsTableModel model;
    private JLabel timeLabel;

    public StatisticsPanel() {
        setLayout(new BorderLayout());
        model = new SpeciesStatsTableModel();
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
        timeLabel = new JLabel("Time Step: 0");
        add(timeLabel, BorderLayout.NORTH);
        setPreferredSize(new Dimension(400, 600));
    }
    
    public void updateStats(List<SpeciesStats> stats, int timeStep) {
        model.setData(stats);
        timeLabel.setText("Time Step: " + timeStep);
    }
}

/**
 * Custom table model for species statistics.
 */
class SpeciesStatsTableModel extends AbstractTableModel {
    private List<SpeciesStats> data = new ArrayList<>();
    private String[] columnNames = {"Species", "Type", "Count", "Avg Age", "Avg Lifespan"};
    
    public void setData(List<SpeciesStats> data) {
        this.data = data;
        fireTableDataChanged();
    }
    
    @Override
    public int getRowCount() {
        return data.size();
    }
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    @Override
    public Object getValueAt(int row, int col) {
        SpeciesStats s = data.get(row);
        switch (col) {
            case 0: return s.scientificName;
            case 1: return s.type;
            case 2: return s.count;
            case 3: return String.format("%.1f", s.avgAge);
            case 4: return String.format("%.1f", s.avgLifespan);
        }
        return null;
    }
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }
}

/**
 * Abstract class for organisms.
 */
abstract class Organism {
    protected int age;
    protected int lifespan;
    protected double mutationRate;
    protected Random random;
    protected int x, y;
    protected String genus;
    protected String species;
    protected SimulationPanel panel;
    
    public Organism(Random random, SimulationPanel panel) {
        this.random = random;
        this.panel = panel;
        this.age = 0;
        this.lifespan = 50 + random.nextInt(50); // default lifespan
        this.mutationRate = 0.1; // default mutation rate
        this.x = random.nextInt(SimulationPanel.PANEL_WIDTH);
        this.y = random.nextInt(SimulationPanel.PANEL_HEIGHT);
    }
    
    public abstract void update(List<Organism> currentOrganisms, List<Organism> newOrganisms);
    public abstract void draw(Graphics g);
    
    public boolean isDead() {
        return age >= lifespan;
    }
    
    public String getScientificName() {
        return genus + " | " + species;
    }
    
    /**
     * Mutate species with moderate chance.
     */
    protected String maybeMutateSpecies(String species) {
        if (random.nextDouble() < 0.1) {
            int index = random.nextInt(species.length());
            char newChar = (char) ('a' + random.nextInt(26));
            char[] chars = species.toCharArray();
            chars[index] = newChar;
            return new String(chars);
        }
        return species;
    }
    
    /**
     * Mutate genus with very low probability.
     */
    protected String maybeMutateGenus(String genus) {
        if (random.nextDouble() < 0.005) {
            int index = random.nextInt(genus.length());
            char newChar = (char) ('A' + random.nextInt(26));
            char[] chars = genus.toCharArray();
            chars[index] = newChar;
            return new String(chars);
        }
        return genus;
    }
    
    /**
     * Inherit a gene value from the parent with a small mutation.
     */
    protected double inheritGene(double gene) {
        return gene * (1 + (random.nextDouble() - 0.5) * mutationRate);
    }
    
    /**
     * Set child's position near the parent.
     */
    protected void setChildPosition(Organism parent) {
        int dx = random.nextInt(21) - 10;
        int dy = random.nextInt(21) - 10;
        this.x = clamp(parent.x + dx, 0, SimulationPanel.PANEL_WIDTH);
        this.y = clamp(parent.y + dy, 0, SimulationPanel.PANEL_HEIGHT);
    }
    
    protected int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}

/**
 * Flora: plants that absorb light and use terrain fertility.
 * They have many extra parameters that affect growth.
 */
class Flora extends Organism {
    protected double reproductionChance;
    protected int size; // visual size in pixels
    protected double hardiness;
    // Extra parameters (values between 0.5 and 1.5)
    protected double waterAbsorption;
    protected double nutrientAbsorption;
    protected double photosynthesisEfficiency;
    protected double growthRate;
    protected double droughtResistance;
    protected double temperatureTolerance;
    protected double windResistance;
    protected double seedDispersal;
    protected double leafArea;
    protected double rootDepth;
    
    public Flora(Random random, SimulationPanel panel) {
        super(random, panel);
        reproductionChance = 0.05 + random.nextDouble() * 0.15;
        genus = "Plantae";
        species = "viridis";
        size = 8;
        lifespan = 80 + random.nextInt(50);
        mutationRate = 0.05;
        hardiness = 0.5;
        waterAbsorption = 0.5 + random.nextDouble();
        nutrientAbsorption = 0.5 + random.nextDouble();
        photosynthesisEfficiency = 0.5 + random.nextDouble();
        growthRate = 0.5 + random.nextDouble();
        droughtResistance = 0.5 + random.nextDouble();
        temperatureTolerance = 0.5 + random.nextDouble();
        windResistance = 0.5 + random.nextDouble();
        seedDispersal = 0.5 + random.nextDouble();
        leafArea = 0.5 + random.nextDouble();
        rootDepth = 0.5 + random.nextDouble();
    }
    
    @Override
    public void update(List<Organism> currentOrganisms, List<Organism> newOrganisms) {
        age++;
        double fert = panel.getTerrainFertilityAt(x, y);
        // Compute light intensity based on vertical position (top has full light).
        double light = 1.0 - ((double) y / SimulationPanel.PANEL_HEIGHT);
        // Extra growth factor from parameters.
        double extraGrowth = (waterAbsorption + nutrientAbsorption + photosynthesisEfficiency + growthRate) / 4.0;
        if (random.nextDouble() < reproductionChance * fert * light * (1 + hardiness) * extraGrowth) {
            size += (int)(2 * fert * extraGrowth);
            species = maybeMutateSpecies(species);
            genus = maybeMutateGenus(genus);
        }
        // Bud off a new plant if large enough.
        if (size >= 30 && random.nextDouble() < 0.5) {
            Flora bud = new Flora(random, panel);
            bud.size = 8;
            bud.setChildPosition(this);
            bud.species = maybeMutateSpecies(this.species);
            bud.genus = this.genus;
            newOrganisms.add(bud);
            size -= 10;
            if (size < 8) size = 8;
        }
    }
    
    @Override
    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillOval(x, y, size, size);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.ITALIC, 10));
        g.drawString(getScientificName(), x + size + 2, y + size + 2);
    }
}

/**
 * Fauna: animals that now have more behavioral complexity.
 * They target nearby flora when energy is low.
 */
class Fauna extends Organism {
    private double energy;
    private double metabolism;
    private double reproductionThreshold;
    // Extra parameters for fauna.
    private double speed;
    private double agility;
    private double strength;
    private double camouflage;
    private double vision;
    private double hearing;
    private double stamina;
    private double aggression;
    private double intelligence;
    private double sociality;
    
    public Fauna(Random random, SimulationPanel panel) {
        super(random, panel);
        energy = 20 + random.nextDouble() * 20;
        metabolism = 0.5 + random.nextDouble();
        reproductionThreshold = 30 + random.nextDouble() * 20;
        genus = "Animalia";
        species = "sapiens";
        lifespan = 70 + random.nextInt(30);
        mutationRate = 0.05;
        speed = 0.5 + random.nextDouble();
        agility = 0.5 + random.nextDouble();
        strength = 0.5 + random.nextDouble();
        camouflage = 0.5 + random.nextDouble();
        vision = 0.5 + random.nextDouble();
        hearing = 0.5 + random.nextDouble();
        stamina = 0.5 + random.nextDouble();
        aggression = 0.5 + random.nextDouble();
        intelligence = 0.5 + random.nextDouble();
        sociality = 0.5 + random.nextDouble();
    }
    
    @Override
    public void update(List<Organism> currentOrganisms, List<Organism> newOrganisms) {
        age++;
        // If energy is low, search for the nearest flora.
        Flora target = null;
        if (energy < reproductionThreshold) {
            double closestDist = Double.MAX_VALUE;
            for (Organism o : currentOrganisms) {
                if (o instanceof Flora) {
                    int dx = o.x - this.x;
                    int dy = o.y - this.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 100 && dist < closestDist) {  // search radius of 100
                        closestDist = dist;
                        target = (Flora) o;
                    }
                }
            }
        }
        // Movement: if a target is found, move toward it; otherwise, random wandering.
        if (target != null) {
            int dx = target.x - this.x;
            int dy = target.y - this.y;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length != 0) {
                dx = (int) ((dx / length) * speed * 5);
                dy = (int) ((dy / length) * agility * 5);
            }
            x = clamp(x + dx, 0, SimulationPanel.PANEL_WIDTH);
            y = clamp(y + dy, 0, SimulationPanel.PANEL_HEIGHT);
        } else {
            int dx = random.nextInt((int)(speed * 10)) - (int)(speed * 5);
            int dy = random.nextInt((int)(agility * 10)) - (int)(agility * 5);
            x = clamp(x + dx, 0, SimulationPanel.PANEL_WIDTH);
            y = clamp(y + dy, 0, SimulationPanel.PANEL_HEIGHT);
        }
        energy -= metabolism;
        // Foraging: if near a flora, consume it.
        if (energy < reproductionThreshold) {
            for (Organism o : currentOrganisms) {
                if (o instanceof Flora && Math.abs(o.x - this.x) < 20 && Math.abs(o.y - this.y) < 20) {
                    energy += 10 * ((stamina + vision) / 2.0);
                    o.age = o.lifespan;  // Mark the plant as dead.
                    break;
                }
            }
        }
        // Reproduction: if energy is high, produce a new fauna.
        if (energy > reproductionThreshold && random.nextDouble() < 0.3) {
            Fauna child = new Fauna(random, panel);
            child.metabolism = metabolism * (1 + (random.nextDouble() - 0.5) * mutationRate);
            child.reproductionThreshold = reproductionThreshold * (1 + (random.nextDouble() - 0.5) * mutationRate);
            child.energy = energy / 2;
            energy /= 2;
            child.species = maybeMutateSpecies(this.species);
            child.genus = maybeMutateGenus(this.genus);
            child.setChildPosition(this);
            newOrganisms.add(child);
        }
    }
    
    @Override
    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(x, y, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.ITALIC, 10));
        g.drawString(getScientificName(), x + 12, y + 12);
    }
}
