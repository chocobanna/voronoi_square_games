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

import newrealm.config.Config;
import newrealm.relaxation.LloydRelaxation;

public class VoronoiDiagramPanel extends JPanel {
    private int width;
    private int height;
    private BufferedImage voronoiImage;
    private double scale = 1.0;  // Zoom factor
    private double offsetX = 0;  // Pan offset in x
    private double offsetY = 0;  // Pan offset in y

    // Variables to support dragging (panning).
    private int lastDragX, lastDragY;

    public VoronoiDiagramPanel(int width, int height, double pointDensity, int lloydIterations) {
        this.width = width;
        this.height = height;
        updatePreferredSize();

        // Mouse wheel for zooming (keeps mouse pointer fixed).
        addMouseWheelListener(e -> {
            double delta = e.getPreciseWheelRotation();
            double oldScale = scale;
            double newScale = (delta > 0) ? scale / 1.1 : scale * 1.1;
            newScale = Math.max(0.5, Math.min(5.0, newScale));
            // Mouse position relative to the component.
            double mouseX = e.getX();
            double mouseY = e.getY();
            // Convert to world coordinates (before zoom).
            double worldX = (mouseX - offsetX) / oldScale;
            double worldY = (mouseY - offsetY) / oldScale;
            scale = newScale;
            // Adjust offset so that the world coordinate under the mouse stays constant.
            offsetX = mouseX - worldX * scale;
            offsetY = mouseY - worldY * scale;
            updatePreferredSize();
            revalidate();
            repaint();
        });

        // Mouse listener for dragging (panning).
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastDragX;
                int dy = e.getY() - lastDragY;
                offsetX += dx;
                offsetY += dy;
                lastDragX = e.getX();
                lastDragY = e.getY();
                updatePreferredSize();
                revalidate();
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // Start background computation of the Voronoi diagram.
        new DiagramWorker(pointDensity, lloydIterations).execute();
    }

    // Updates the preferred size based on current scale and pan offset,
    // clamping it to the screen dimensions if necessary.
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

    // SwingWorker to generate the Voronoi diagram off the EDT.
    private class DiagramWorker extends SwingWorker<BufferedImage, Void> {
        private double pointDensity;
        private int lloydIterations;

        public DiagramWorker(double pointDensity, int lloydIterations) {
            this.pointDensity = pointDensity;
            this.lloydIterations = lloydIterations;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            int numSites = (int) (width * height * pointDensity);
            Point2D.Double[] sites = new Point2D.Double[numSites];
            Color[] siteColors = new Color[numSites];
            Random rand = new Random();

            // Create random sites and assign pastel colors.
            for (int i = 0; i < numSites; i++) {
                double x = rand.nextDouble() * width;
                double y = rand.nextDouble() * height;
                sites[i] = new Point2D.Double(x, y);
                siteColors[i] = new Color(
                        rand.nextInt(128) + 127,
                        rand.nextInt(128) + 127,
                        rand.nextInt(128) + 127
                );
            }

            // Perform Lloyd relaxation.
            LloydRelaxation.relax(sites, width, height, lloydIterations, Config.SAMPLE_STEP);

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int[][] cellIndices = new int[width][height];

            // Performance optimization: Parallelize per-column computation of nearest site.
            IntStream.range(0, width).parallel().forEach(i -> {
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
            });

            // Color pixels and draw boundaries.
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    boolean boundary = false;
                    int current = cellIndices[i][j];
                    if (i > 0 && cellIndices[i - 1][j] != current) boundary = true;
                    else if (i < width - 1 && cellIndices[i + 1][j] != current) boundary = true;
                    else if (j > 0 && cellIndices[i][j - 1] != current) boundary = true;
                    else if (j < height - 1 && cellIndices[i][j + 1] != current) boundary = true;
                    img.setRGB(i, j, boundary ? Color.BLACK.getRGB() : siteColors[current].getRGB());
                }
            }

            // Draw small circles at each site.
            Graphics2D g = img.createGraphics();
            for (int k = 0; k < numSites; k++) {
                int x = (int) sites[k].x;
                int y = (int) sites[k].y;
                g.setColor(Color.WHITE);
                g.fillOval(x - 3, y - 3, 6, 6);
                g.setColor(Color.BLACK);
                g.drawOval(x - 3, y - 3, 6, 6);
            }
            g.dispose();

            return img;
        }

        @Override
        protected void done() {
            try {
                voronoiImage = get();
                repaint();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // Apply panning (translation) then zoom.
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        if (voronoiImage != null) {
            g2.drawImage(voronoiImage, 0, 0, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.drawString("Loading Voronoi Diagram...", width / 2 - 50, height / 2);
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
