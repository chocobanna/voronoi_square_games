package main;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;

public class GamePanel extends JPanel {
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
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform original = g2d.getTransform();
        g2d.translate(translateX, translateY);
        g2d.scale(scale, scale);
        engine.draw(g2d);
        g2d.setTransform(original);
    }
}
