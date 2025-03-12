package newrealm.diagram;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Panel that displays the planetary map rendered on a sphere.
 * The sphere is centered in the window on a black background.
 * Use arrow keys (and/or mouse dragging) to spin the sphere.
 * The mouse wheel zooms infinitely, and extra detail is added by increasing octaves.
 * The smoothing level can be adjusted from the main menu.
 */
public class VoronoiDiagramPanel extends JPanel {
    private int width;
    private int height;
    private BufferedImage finalImage;
    
    // Rotation parameters (in radians).
    private double rotAzimuth = 0;
    private double rotElevation = 0;
    
    // Zoom factor controlling display size and LOD.
    private double scale = 1.0;
    
    // Map generation parameters.
    private double pointDensity;
    private int lloydIterations;
    private double waterThreshold = 0.5;
    
    // Smoothing iterations for elevation.
    private int smoothingIterations = 2;

    // Last computed biome map for lookup.
    private int[][] lastBiomeMap;

    // Status label for displaying info.
    private JLabel statusLabel;
    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }
    
    public VoronoiDiagramPanel(int width, int height, double pointDensity, int lloydIterations) {
        this.width = width;
        this.height = height;
        this.pointDensity = pointDensity;
        this.lloydIterations = lloydIterations;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        // Mouse wheel for zooming.
        addMouseWheelListener(e -> {
            double delta = e.getPreciseWheelRotation();
            scale *= (delta > 0) ? 0.9 : 1.1;
            new DiagramWorker().execute();
        });

        // Key bindings for arrow keys.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "rotateLeft");
        getActionMap().put("rotateLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rotAzimuth -= 0.05;
                new DiagramWorker().execute();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "rotateRight");
        getActionMap().put("rotateRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rotAzimuth += 0.05;
                new DiagramWorker().execute();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "rotateUp");
        getActionMap().put("rotateUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rotElevation -= 0.05;
                rotElevation = Math.max(-Math.PI/2, rotElevation);
                new DiagramWorker().execute();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "rotateDown");
        getActionMap().put("rotateDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rotElevation += 0.05;
                rotElevation = Math.min(Math.PI/2, rotElevation);
                new DiagramWorker().execute();
            }
        });
        
        // Optional mouse dragging.
        MouseAdapter ma = new MouseAdapter() {
            private int lastDragX, lastDragY;
            private double initAzimuth, initElevation;
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
                initAzimuth = rotAzimuth;
                initElevation = rotElevation;
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastDragX;
                int dy = e.getY() - lastDragY;
                rotAzimuth = initAzimuth + dx * 0.005;
                rotElevation = initElevation + dy * 0.005;
                rotElevation = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, rotElevation));
                new DiagramWorker().execute();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        
        new DiagramWorker().execute();
    }
    
    public void setWaterThreshold(double waterThreshold) {
        this.waterThreshold = waterThreshold;
        new DiagramWorker().execute();
    }
    
    public void setSmoothingIterations(int iterations) {
        this.smoothingIterations = iterations;
        new DiagramWorker().execute();
    }

    private class DiagramWorker extends SwingWorker<BufferedImage, Void> {
        @Override
        protected BufferedImage doInBackground() {
            MapGenerationResult result = MapGenerator.generateMap(
                    width, height, pointDensity, lloydIterations,
                    waterThreshold, rotAzimuth, rotElevation, scale, smoothingIterations);
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
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Fill background black.
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (finalImage != null) {
            int drawWidth = (int) (finalImage.getWidth() * scale);
            int drawHeight = (int) (finalImage.getHeight() * scale);
            int x = (getWidth() - drawWidth) / 2;
            int y = (getHeight() - drawHeight) / 2;
            g.drawImage(finalImage, x, y, drawWidth, drawHeight, null);
        } else {
            g.setColor(Color.WHITE);
            g.drawString("Loading Map...", getWidth() / 2 - 40, getHeight() / 2);
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
