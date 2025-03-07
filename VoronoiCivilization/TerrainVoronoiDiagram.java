import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class TerrainVoronoiDiagram extends JPanel {
    // Map dimensions (modifiable).
    private int mapWidth = 800;
    private int mapHeight = 800;
    
    // Parameters that can be updated.
    private int numSites = 10;
    private int margin = 50; // Extra margin for SEA along borders

    // Conversion: 1 pixel equals 0.01 km (10 meters per pixel).
    private static final double PIXEL_TO_KM = 0.01;

    // Panning and zooming.
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point lastDragPoint = null;

    // Define biome types.
    public enum Terrain {
        SEA, PLAINS, DESERT, FOREST, MOUNTAINS
    }

    private List<Point> sites;
    private List<Terrain> terrains;
    private JLabel areaLabel;  // Displays the area and biome of the hovered cell

    // For interaction.
    private int hoveredSiteIndex = -1;
    private int selectedSiteIndex = -1;

    // List of river paths (each a list of Points).
    private List<List<Point>> riverPaths = new ArrayList<>();

    public TerrainVoronoiDiagram() {
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        setBackground(Color.WHITE);
        sites = new ArrayList<>();
        terrains = new ArrayList<>();
        generateSitesAndTerrains();
        addInteractionListeners();
    }

    /**
     * Generate random sites and assign each a biome based on its elevation.
     * Then simulate rivers.
     */
    private void generateSitesAndTerrains() {
        sites.clear();
        terrains.clear();
        for (int i = 0; i < numSites; i++) {
            int x = (int) (Math.random() * mapWidth);
            int y = (int) (Math.random() * mapHeight);
            Point site = new Point(x, y);
            sites.add(site);
            terrains.add(getTerrainForSite(site));
        }
        simulateRivers();
    }

    /**
     * A simple noise-based elevation function.
     * Uses a low frequency sine–cosine combination to yield smooth variations.
     * Returns a value in [0,1] where higher values represent higher elevation.
     */
    private double getElevation(double x, double y) {
        double frequency = 0.005; // Adjust for smoothness
        double elev = Math.sin(x * frequency) * Math.cos(y * frequency);
        elev = (elev + 1) / 2; // Normalize to [0,1]
        return elev;
    }

    /**
     * Determine the biome for a given site.
     * - If the site is near the map edge (or its elevation is low), it becomes SEA.
     * - Otherwise, the elevation (modulated by island shape) determines the biome:
     *     elevation < 0.3 -> DESERT,
     *     elevation < 0.5 -> PLAINS,
     *     elevation < 0.7 -> FOREST,
     *     otherwise      -> MOUNTAINS.
     */
    private Terrain getTerrainForSite(Point p) {
        // Compute normalized distance from center for an island effect.
        double cx = mapWidth / 2.0;
        double cy = mapHeight / 2.0;
        double dx = (p.x - cx) / cx;
        double dy = (p.y - cy) / cy;
        double distance = Math.sqrt(dx * dx + dy * dy);  // roughly in [0,√2]
        
        // Force SEA if near the edge (or if elevation is very low).
        double elev = getElevation(p.x, p.y);
        if (distance > 0.8 || elev < 0.3) {
            return Terrain.SEA;
        }
        
        // Refine biome using elevation.
        if (elev < 0.5)
            return Terrain.PLAINS;
        else if (elev < 0.7)
            return Terrain.FOREST;
        else
            return Terrain.MOUNTAINS;
    }

    /**
     * Returns a color for each biome.
     */
    private Color getTerrainColor(Terrain terrain) {
        switch (terrain) {
            case SEA:        return new Color(0, 102, 204);
            case PLAINS:     return new Color(102, 204, 0);
            case DESERT:     return new Color(255, 204, 102);
            case FOREST:     return new Color(34, 139, 34);
            case MOUNTAINS:  return new Color(128, 128, 128);
            default:         return Color.LIGHT_GRAY;
        }
    }

    /**
     * Computes the Voronoi cell (convex polygon) for a given site by clipping the map rectangle.
     */
    private Polygon getVoronoiCell(Point p) {
        List<Point2D.Double> poly = new ArrayList<>();
        poly.add(new Point2D.Double(0, 0));
        poly.add(new Point2D.Double(mapWidth, 0));
        poly.add(new Point2D.Double(mapWidth, mapHeight));
        poly.add(new Point2D.Double(0, mapHeight));

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
     * Clips a polygon with the half-plane defined by A*x + B*y + C >= 0 using the Sutherland–Hodgman algorithm.
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
                if (inter != null) newPoly.add(inter);
            } else if (!currentInside && nextInside) {
                Point2D.Double inter = getIntersection(current, next, A, B, C);
                if (inter != null) newPoly.add(inter);
                newPoly.add(next);
            }
        }
        return newPoly;
    }

    /**
     * Computes the intersection of the segment (p1, p2) with the line A*x + B*y + C = 0.
     */
    private Point2D.Double getIntersection(Point2D.Double p1, Point2D.Double p2, double A, double B, double C) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double denom = A * dx + B * dy;
        if (denom == 0) return null;
        double t = -(A * p1.x + B * p1.y + C) / denom;
        return new Point2D.Double(p1.x + t * dx, p1.y + t * dy);
    }

    /**
     * Computes the area of a polygon using the shoelace formula.
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
     * Sets the JLabel that displays the area and biome.
     */
    public void setAreaLabel(JLabel label) {
        this.areaLabel = label;
    }

    /**
     * Regenerates the diagram.
     */
    public void regenerateDiagram() {
        generateSitesAndTerrains();
        hoveredSiteIndex = -1;
        selectedSiteIndex = -1;
        repaint();
    }

    /**
     * Updates parameters: number of sites, margin, and map dimensions.
     */
    public void updateParameters(int numSites, int margin, int width, int height) {
        this.numSites = numSites;
        this.margin = margin;
        this.mapWidth = width;
        this.mapHeight = height;
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        regenerateDiagram();
    }

    /**
     * Simulate river generation.
     * This method finds one high-elevation site and simulates a river
     * flowing downhill by choosing nearby points with lower elevation.
     */
    private void simulateRivers() {
        riverPaths.clear();
        // Collect candidate sources (sites with elevation above 0.8).
        List<Point> highSites = new ArrayList<>();
        for (Point site : sites) {
            double elev = getElevation(site.x, site.y);
            if (elev > 0.8) {
                highSites.add(site);
            }
        }
        if (highSites.isEmpty()) return;
        // Choose one random source.
        Point source = highSites.get((int)(Math.random() * highSites.size()));
        List<Point> river = new ArrayList<>();
        river.add(source);
        Point current = source;
        int iterations = 0;
        while (iterations < 1000) {
            double currentElev = getElevation(current.x, current.y);
            if (currentElev < 0.3) break; // Reached low elevation (water)
            // Check neighbors (in steps of 5 pixels).
            Point next = null;
            double minElev = currentElev;
            for (int dx = -5; dx <= 5; dx += 5) {
                for (int dy = -5; dy <= 5; dy += 5) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    if (nx < 0 || ny < 0 || nx >= mapWidth || ny >= mapHeight) continue;
                    double neighborElev = getElevation(nx, ny);
                    if (neighborElev < minElev) {
                        minElev = neighborElev;
                        next = new Point(nx, ny);
                    }
                }
            }
            if (next == null) break;
            river.add(next);
            current = next;
            iterations++;
        }
        if (river.size() > 1) {
            riverPaths.add(river);
        }
    }

    /**
     * Adds listeners for panning, zooming, and cell interaction.
     */
    private void addInteractionListeners() {
        // Zoom with mouse wheel.
        addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            double oldScale = scale;
            scale *= Math.pow(1.1, -rotation);
            Point p = e.getPoint();
            offsetX = p.x - ((p.x - offsetX) * (scale / oldScale));
            offsetY = p.y - ((p.y - offsetY) * (scale / oldScale));
            repaint();
        });

        // Pan by dragging.
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
                    double areaPixels = computeArea(cell);
                    double areaKm = areaPixels * (PIXEL_TO_KM * PIXEL_TO_KM);
                    Terrain biome = terrains.get(hoveredSiteIndex);
                    areaLabel.setText(String.format("Area: %.2f km², Biome: %s", areaKm, biome.name()));
                } else if (areaLabel != null) {
                    areaLabel.setText("Area: ");
                }
                repaint();
            }
        });

        // On click, select a cell.
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
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        // Draw Voronoi cells.
        for (int i = 0; i < sites.size(); i++) {
            Polygon cell = getVoronoiCell(sites.get(i));
            Terrain terrain = terrains.get(i);
            Color terrainColor = getTerrainColor(terrain);
            g2d.setColor(terrainColor);
            g2d.fillPolygon(cell);
            g2d.setColor(terrainColor.darker());
            g2d.drawPolygon(cell);
        }

        // Draw river paths.
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        for (List<Point> river : riverPaths) {
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (Point p : river) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else { path.lineTo(p.x, p.y); }
            }
            g2d.draw(path);
        }

        // Highlight hovered cell.
        if (hoveredSiteIndex != -1) {
            Polygon hoveredCell = getVoronoiCell(sites.get(hoveredSiteIndex));
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillPolygon(hoveredCell);
        }

        // Outline selected cell.
        if (selectedSiteIndex != -1) {
            Polygon selectedCell = getVoronoiCell(sites.get(selectedSiteIndex));
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawPolygon(selectedCell);
        }

        // Draw site points.
        g2d.setColor(Color.BLACK);
        for (Point site : sites) {
            g2d.fillOval(site.x - 3, site.y - 3, 6, 6);
        }
        g2d.dispose();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(mapWidth, mapHeight);
    }
}
