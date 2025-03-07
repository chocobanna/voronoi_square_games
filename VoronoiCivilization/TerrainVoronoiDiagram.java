import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class TerrainVoronoiDiagram extends JPanel {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    // Parameters that can be updated via the setup dialog.
    private int numSites = 10;
    private int margin = 50; // Margin for SEA terrain near borders

    // Panning and zooming variables.
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point lastDragPoint = null;

    // Define terrain types.
    private enum Terrain {
        SEA, PLAINS, DESERT, FOREST, MOUNTAINS, SWAMP, TUNDRA
    }

    private List<Point> sites;
    private List<Terrain> terrains;
    private JLabel areaLabel;  // Label to display the area of the hovered cell

    // Track the hovered and selected site indices.
    private int hoveredSiteIndex = -1;
    private int selectedSiteIndex = -1;

    public TerrainVoronoiDiagram() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        sites = new ArrayList<>();
        terrains = new ArrayList<>();
        generateSitesAndTerrains();
        addInteractionListeners();
    }

    /**
     * Generate random sites and assign each a terrain type based on its position.
     */
    private void generateSitesAndTerrains() {
        sites.clear();
        terrains.clear();
        for (int i = 0; i < numSites; i++) {
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            Point site = new Point(x, y);
            sites.add(site);
            terrains.add(getTerrainForSite(site));
        }
    }

    /**
     * Determine the terrain for a given site based on its position.
     * - If the site is near any border (within margin), return SEA.
     * - Otherwise, use normalized coordinates to divide the interior.
     */
    private Terrain getTerrainForSite(Point p) {
        if (p.x < margin || p.x > WIDTH - margin || p.y < margin || p.y > HEIGHT - margin) {
            return Terrain.SEA;
        }
        double normX = p.x / (double) WIDTH;
        double normY = p.y / (double) HEIGHT;
        
        if (normX < 0.33) {
            if (normY < 0.33)
                return Terrain.FOREST;
            else if (normY < 0.66)
                return Terrain.MOUNTAINS;
            else
                return Terrain.TUNDRA;
        } else if (normX < 0.66) {
            if (normY < 0.33)
                return Terrain.PLAINS;
            else if (normY < 0.66)
                return Terrain.SWAMP;
            else
                return Terrain.DESERT;
        } else {
            if (normY < 0.33)
                return Terrain.DESERT;
            else if (normY < 0.66)
                return Terrain.PLAINS;
            else
                return Terrain.FOREST;
        }
    }

    /**
     * Map each terrain type to a specific color.
     */
    private Color getTerrainColor(Terrain terrain) {
        switch (terrain) {
            case SEA:        return new Color(0, 102, 204);
            case PLAINS:     return new Color(102, 204, 0);
            case DESERT:     return new Color(255, 204, 102);
            case FOREST:     return new Color(34, 139, 34);
            case MOUNTAINS:  return new Color(128, 128, 128);
            case SWAMP:      return new Color(102, 153, 102);
            case TUNDRA:     return new Color(220, 220, 220);
            default:         return Color.LIGHT_GRAY;
        }
    }

    /**
     * Compute the exact Voronoi cell (a convex polygon) for a given site.
     * The cell is determined by starting with the bounding rectangle and
     * clipping it with the perpendicular bisectors between the target and all other sites.
     */
    private Polygon getVoronoiCell(Point p) {
        List<Point2D.Double> poly = new ArrayList<>();
        poly.add(new Point2D.Double(0, 0));
        poly.add(new Point2D.Double(WIDTH, 0));
        poly.add(new Point2D.Double(WIDTH, HEIGHT));
        poly.add(new Point2D.Double(0, HEIGHT));

        for (Point q : sites) {
            if (q.equals(p)) continue;
            double A = p.x - q.x;
            double B = p.y - q.y;
            double midX = (p.x + q.x) / 2.0;
            double midY = (p.y + q.y) / 2.0;
            double C = -(A * midX + B * midY);
            poly = clipPolygon(poly, A, B, C);
            if (poly.isEmpty()) break;
        }

        Polygon awtPoly = new Polygon();
        for (Point2D.Double pt : poly) {
            awtPoly.addPoint((int) Math.round(pt.x), (int) Math.round(pt.y));
        }
        return awtPoly;
    }

    /**
     * Clip a polygon with the half-plane defined by A*x + B*y + C >= 0.
     */
    private List<Point2D.Double> clipPolygon(List<Point2D.Double> poly, double A, double B, double C) {
        List<Point2D.Double> newPoly = new ArrayList<>();
        int size = poly.size();
        for (int i = 0; i < size; i++) {
            Point2D.Double current = poly.get(i);
            Point2D.Double next = poly.get((i + 1) % size);
            boolean currentInside = (A * current.x + B * current.y + C >= 0);
            boolean nextInside = (A * next.x + B * next.y + C >= 0);

            if (currentInside && nextInside) {
                newPoly.add(next);
            } else if (currentInside && !nextInside) {
                Point2D.Double inter = getIntersection(current, next, A, B, C);
                if (inter != null) {
                    newPoly.add(inter);
                }
            } else if (!currentInside && nextInside) {
                Point2D.Double inter = getIntersection(current, next, A, B, C);
                if (inter != null) {
                    newPoly.add(inter);
                }
                newPoly.add(next);
            }
        }
        return newPoly;
    }

    /**
     * Compute the intersection of the segment (p1, p2) with the line A*x + B*y + C = 0.
     */
    private Point2D.Double getIntersection(Point2D.Double p1, Point2D.Double p2, double A, double B, double C) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double denom = A * dx + B * dy;
        if (denom == 0) return null; // Lines are parallel.
        double t = -(A * p1.x + B * p1.y + C) / denom;
        return new Point2D.Double(p1.x + t * dx, p1.y + t * dy);
    }

    /**
     * Compute the area of a polygon using the shoelace formula.
     */
    private double computeArea(Polygon poly) {
        double area = 0;
        int n = poly.npoints;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += poly.xpoints[i] * poly.ypoints[j] - poly.xpoints[j] * poly.ypoints[i];
        }
        return Math.abs(area) / 2.0;
    }

    /**
     * Set the JLabel that displays the area.
     */
    public void setAreaLabel(JLabel label) {
        this.areaLabel = label;
    }

    /**
     * Regenerate the Voronoi diagram.
     */
    public void regenerateDiagram() {
        generateSitesAndTerrains();
        hoveredSiteIndex = -1;
        selectedSiteIndex = -1;
        repaint();
    }

    /**
     * Update parameters (numSites and margin) and regenerate the diagram.
     */
    public void updateParameters(int numSites, int margin) {
        this.numSites = numSites;
        this.margin = margin;
        regenerateDiagram();
    }

    /**
     * Add listeners for panning, zooming, and cell interaction.
     */
    private void addInteractionListeners() {
        // Mouse wheel for zooming.
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                double oldScale = scale;
                scale *= Math.pow(1.1, -rotation);
                // Adjust offset so that the zoom is centered on the mouse pointer.
                Point p = e.getPoint();
                offsetX = p.x - ((p.x - offsetX) * (scale / oldScale));
                offsetY = p.y - ((p.y - offsetY) * (scale / oldScale));
                repaint();
            }
        });

        // Mouse dragging for panning.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragPoint = e.getPoint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = e.getPoint();
                if (lastDragPoint != null) {
                    offsetX += current.x - lastDragPoint.x;
                    offsetY += current.y - lastDragPoint.y;
                    lastDragPoint = current;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Convert the mouse point to diagram coordinates.
                int diagramX = (int) ((e.getX() - offsetX) / scale);
                int diagramY = (int) ((e.getY() - offsetY) / scale);
                double minDistSquared = Double.MAX_VALUE;
                int nearestIndex = -1;
                for (int i = 0; i < sites.size(); i++) {
                    Point site = sites.get(i);
                    int dx = diagramX - site.x;
                    int dy = diagramY - site.y;
                    double d2 = dx * dx + dy * dy;
                    if (d2 < minDistSquared) {
                        minDistSquared = d2;
                        nearestIndex = i;
                    }
                }
                hoveredSiteIndex = nearestIndex;
                if (areaLabel != null && hoveredSiteIndex != -1) {
                    Polygon cell = getVoronoiCell(sites.get(hoveredSiteIndex));
                    double area = computeArea(cell);
                    areaLabel.setText(String.format("Area: %.2f pixels²", area));
                } else if (areaLabel != null) {
                    areaLabel.setText("Area: ");
                }
                repaint();
            }
        });

        // Mouse click to select a cell.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedSiteIndex = hoveredSiteIndex;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        // Apply pan and zoom transform.
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        // Draw each Voronoi cell.
        for (int i = 0; i < sites.size(); i++) {
            Polygon cell = getVoronoiCell(sites.get(i));
            Terrain terrain = terrains.get(i);
            Color terrainColor = getTerrainColor(terrain);
            g2d.setColor(terrainColor);
            g2d.fillPolygon(cell);
            g2d.setColor(terrainColor.darker());
            g2d.drawPolygon(cell);
        }

        // Highlight the hovered cell.
        if (hoveredSiteIndex != -1) {
            Polygon hoveredCell = getVoronoiCell(sites.get(hoveredSiteIndex));
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillPolygon(hoveredCell);
        }

        // Outline the selected cell.
        if (selectedSiteIndex != -1) {
            Polygon selectedCell = getVoronoiCell(sites.get(selectedSiteIndex));
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawPolygon(selectedCell);
        }

        // Draw each site as a small black circle.
        g2d.setColor(Color.BLACK);
        for (Point site : sites) {
            g2d.fillOval(site.x - 3, site.y - 3, 6, 6);
        }
        g2d.dispose();
    }
}
