package newrealm.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.stream.IntStream;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import newrealm.config.Config;
import newrealm.relaxation.LloydRelaxation;

public class VoronoiDiagramPanel extends JPanel {
    private int width;
    private int height;
    private BufferedImage finalImage;  // Final image with voronoi cells colored by noise majority.
    private double scale = 1.0;        // Zoom factor.
    private double offsetX = 0;        // Pan offset in x.
    private double offsetY = 0;        // Pan offset in y.

    // Variables for smooth (inertial) scrolling.
    private int lastDragX, lastDragY;
    private double velocityX = 0;
    private double velocityY = 0;
    private Timer inertiaTimer;

    // Noise parameters.
    private final int octaves = 5;
    private final double persistence = 0.5;
    private final double noiseScale = 100.0;

    // Terrain thresholds and colors:
    // 0: ocean, 1: land, 2: mountain, 3: snow.
    private final double[] thresholds = {0.4, 0.6, 0.8};
    private final Color[] terrainColors = {
         Color.BLUE,
         new Color(34, 139, 34),
         new Color(139, 69, 19),
         Color.WHITE
    };

    // These parameters now govern the Voronoi diagram.
    private double pointDensity;
    private int lloydIterations;

    public VoronoiDiagramPanel(int width, int height, double pointDensity, int lloydIterations) {
        this.width = width;
        this.height = height;
        this.pointDensity = pointDensity;
        this.lloydIterations = lloydIterations;
        updatePreferredSize();

        // Mouse wheel listener for zooming.
        addMouseWheelListener(e -> {
            double delta = e.getPreciseWheelRotation();
            double oldScale = scale;
            double newScale = (delta > 0) ? scale / 1.1 : scale * 1.1;
            newScale = Math.max(0.5, Math.min(5.0, newScale));
            double mouseX = e.getX();
            double mouseY = e.getY();
            double worldX = (mouseX - offsetX) / oldScale;
            double worldY = (mouseY - offsetY) / oldScale;
            scale = newScale;
            offsetX = mouseX - worldX * scale;
            offsetY = mouseY - worldY * scale;
            updatePreferredSize();
            revalidate();
            repaint();
        });

        // Mouse listeners for dragging (panning) with inertia.
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
                if (inertiaTimer != null && inertiaTimer.isRunning()) {
                    inertiaTimer.stop();
                }
                velocityX = 0;
                velocityY = 0;
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastDragX;
                int dy = e.getY() - lastDragY;
                offsetX += dx;
                offsetY += dy;
                lastDragX = e.getX();
                lastDragY = e.getY();
                velocityX = dx;
                velocityY = dy;
                updatePreferredSize();
                revalidate();
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (Math.abs(velocityX) > 1 || Math.abs(velocityY) > 1) {
                    inertiaTimer = new Timer(20, ae -> {
                        offsetX += velocityX;
                        offsetY += velocityY;
                        velocityX *= 0.9;
                        velocityY *= 0.9;
                        if (Math.abs(velocityX) < 0.5 && Math.abs(velocityY) < 0.5) {
                            inertiaTimer.stop();
                        }
                        updatePreferredSize();
                        revalidate();
                        repaint();
                    });
                    inertiaTimer.start();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // Start the background computation.
        new DiagramWorker().execute();
    }

    // Update the panel's preferred size based on current scale and offset.
    private void updatePreferredSize() {
        int left = (int) Math.min(0, offsetX);
        int top = (int) Math.min(0, offsetY);
        int right = (int) Math.max(width * scale, offsetX + width * scale);
        int bottom = (int) Math.max(height * scale, offsetY + height * scale);
        Dimension natural = new Dimension(right - left, bottom - top);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int prefWidth = Math.min(natural.width, screen.width);
        int prefHeight = Math.min(natural.height, screen.height);
        setPreferredSize(new Dimension(prefWidth, prefHeight));
    }

    // SwingWorker to generate the Voronoi diagram image.
    private class DiagramWorker extends SwingWorker<BufferedImage, Void> {
        @Override
        protected BufferedImage doInBackground() {
            // Step 1: Generate a noise-based terrain map.
            int[][] terrainMap = new int[width][height];
            IntStream.range(0, width).parallel().forEach(i -> {
                for (int j = 0; j < height; j++) {
                    double n = fractalNoise(i / noiseScale, j / noiseScale, octaves, persistence);
                    int type;
                    if (n < thresholds[0]) type = 0;          // Ocean.
                    else if (n < thresholds[1]) type = 1;     // Land.
                    else if (n < thresholds[2]) type = 2;     // Mountain.
                    else type = 3;                           // Snow.
                    terrainMap[i][j] = type;
                }
            });

            // Step 2: Generate random Voronoi sites.
            int numSites = (int) (width * height * pointDensity);
            Point2D.Double[] sites = new Point2D.Double[numSites];
            Random rand = new Random();
            for (int i = 0; i < numSites; i++) {
                double x = rand.nextDouble() * width;
                double y = rand.nextDouble() * height;
                sites[i] = new Point2D.Double(x, y);
            }
            // Optional: Apply Lloyd relaxation.
            if (lloydIterations > 0) {
                LloydRelaxation.relax(sites, width, height, lloydIterations, Config.SAMPLE_STEP);
            }

            // Step 3: For each pixel, assign it to the nearest site.
            int[][] cellIndices = new int[width][height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int nearest = 0;
                    double minDistSq = Double.MAX_VALUE;
                    for (int k = 0; k < numSites; k++) {
                        double dx = sites[k].x - i;
                        double dy = sites[k].y - j;
                        double distSq = dx * dx + dy * dy;
                        if (distSq < minDistSq) {
                            minDistSq = distSq;
                            nearest = k;
                        }
                    }
                    cellIndices[i][j] = nearest;
                }
            }

            // Step 4: For each site, determine the majority terrain type from its pixels.
            int[] majorityTerrain = new int[numSites];
            int[][] counts = new int[numSites][4]; // 4 terrain types.
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int site = cellIndices[i][j];
                    int terrain = terrainMap[i][j];
                    counts[site][terrain]++;
                }
            }
            for (int s = 0; s < numSites; s++) {
                int maxCount = -1;
                int majority = 0;
                for (int t = 0; t < 4; t++) {
                    if (counts[s][t] > maxCount) {
                        maxCount = counts[s][t];
                        majority = t;
                    }
                }
                majorityTerrain[s] = majority;
            }

            // Step 5: Create the final image.
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int site = cellIndices[i][j];
                    boolean boundary = false;
                    if (i > 0 && cellIndices[i - 1][j] != site) boundary = true;
                    else if (i < width - 1 && cellIndices[i + 1][j] != site) boundary = true;
                    else if (j > 0 && cellIndices[i][j - 1] != site) boundary = true;
                    else if (j < height - 1 && cellIndices[i][j + 1] != site) boundary = true;
                    if (boundary) {
                        img.setRGB(i, j, Color.BLACK.getRGB());
                    } else {
                        int terrainType = majorityTerrain[site];
                        img.setRGB(i, j, terrainColors[terrainType].getRGB());
                    }
                }
            }
            return img;
        }

        @Override
        protected void done() {
            try {
                finalImage = get();
                repaint();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // --- Noise helper functions ---
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    private double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }
    private double noise(double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double sx = fade(x - x0);
        double sy = fade(y - y0);
        double n0 = hash(x0, y0);
        double n1 = hash(x1, y0);
        double ix0 = lerp(n0, n1, sx);
        n0 = hash(x0, y1);
        n1 = hash(x1, y1);
        double ix1 = lerp(n0, n1, sx);
        return lerp(ix0, ix1, sy);
    }
    private double fractalNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        return total / maxValue;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // Apply pan (translation) then zoom (scale).
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        if (finalImage != null) {
            g2.drawImage(finalImage, 0, 0, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.drawString("Loading Map...", width / 2 - 50, height / 2);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        int left = (int) Math.min(0, offsetX);
        int top = (int) Math.min(0, offsetY);
        int right = (int) Math.max(width * scale, offsetX + width * scale);
        int bottom = (int) Math.max(height * scale, offsetY + height * scale);
        Dimension natural = new Dimension(right - left, bottom - top);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int prefWidth = Math.min(natural.width, screen.width);
        int prefHeight = Math.min(natural.height, screen.height);
        return new Dimension(prefWidth, prefHeight);
    }
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
