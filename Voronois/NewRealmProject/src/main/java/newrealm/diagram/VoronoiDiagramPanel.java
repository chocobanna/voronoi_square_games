package newrealm.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;


public class VoronoiDiagramPanel extends JPanel {
    private int width;
    private int height;
    private BufferedImage finalImage;
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // For smooth panning.
    private int lastDragX, lastDragY;
    private double velocityX = 0, velocityY = 0;
    private Timer inertiaTimer;

    // Map generation parameters.
    private double pointDensity;
    private int lloydIterations;
    private double waterThreshold = 0.5;

    // Last computed biome map for lookup.
    private int[][] lastBiomeMap;

    // Optional status label (bottombar) to display biome under mouse.
    private JLabel statusLabel;
    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }

    public VoronoiDiagramPanel(int width, int height, double pointDensity, int lloydIterations) {
        this.width = width;
        this.height = height;
        this.pointDensity = pointDensity;
        this.lloydIterations = lloydIterations;
        updatePreferredSize();

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

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
                if (inertiaTimer != null && inertiaTimer.isRunning()) inertiaTimer.stop();
                velocityX = velocityY = 0;
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
                        if (Math.abs(velocityX) < 0.5 && Math.abs(velocityY) < 0.5)
                            inertiaTimer.stop();
                        updatePreferredSize();
                        revalidate();
                        repaint();
                    });
                    inertiaTimer.start();
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                if (statusLabel != null && lastBiomeMap != null) {
                    int x = (int) ((e.getX() - offsetX) / scale);
                    int y = (int) ((e.getY() - offsetY) / scale);
                    if (x >= 0 && x < width && y >= 0 && y < height)
                        statusLabel.setText("Biome: " + getBiomeName(lastBiomeMap[x][y]));
                    else
                        statusLabel.setText("Biome: Out of bounds");
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                ma.mouseMoved(e);
            }
        });

        // Start background generation.
        new DiagramWorker().execute();
    }

    public void setWaterThreshold(double waterThreshold) {
        this.waterThreshold = waterThreshold;
        new DiagramWorker().execute();
    }

    private void updatePreferredSize() {
        int left = (int) Math.min(0, offsetX);
        int top = (int) Math.min(0, offsetY);
        int right = (int) Math.max(width * scale, offsetX + width * scale);
        int bottom = (int) Math.max(height * scale, offsetY + height * scale);
        setPreferredSize(new Dimension(right - left, bottom - top));
    }

    private class DiagramWorker extends SwingWorker<BufferedImage, Void> {
        @Override
        protected BufferedImage doInBackground() {
            MapGenerationResult result = MapGenerator.generateMap(width, height, pointDensity, lloydIterations, waterThreshold);
            lastBiomeMap = result.biomeMap;
            return result.image;
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

    private String getBiomeName(int biome) {
        switch(biome) {
            case 0: return "Ocean";
            case 1: return "Desert";
            case 2: return "Grassland";
            case 3: return "Temperate Forest";
            case 4: return "Mountain";
            case 5: return "Snow";
            default: return "Unknown";
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        if (finalImage != null)
            g2.drawImage(finalImage, 0, 0, null);
        else {
            g2.setColor(Color.BLACK);
            g2.drawString("Loading Map...", width/2 - 40, height/2);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        int left = (int) Math.min(0, offsetX);
        int top = (int) Math.min(0, offsetY);
        int right = (int) Math.max(width * scale, offsetX + width * scale);
        int bottom = (int) Math.max(height * scale, offsetY + height * scale);
        return new Dimension(right - left, bottom - top);
    }
}
