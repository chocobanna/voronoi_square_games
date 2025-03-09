import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import javax.swing.*;


public class TerrainVoronoiDiagram extends JPanel {
    // Map dimensions (modifiable).
    private int mapWidth = 800;
    private int mapHeight = 800;
    
    // Parameters.
    private int numSites = 10;
    private int margin = 50; // Extra margin for SEA along borders

    // Conversion: 1 pixel equals 0.01 km (i.e. 10 meters per pixel).
    private static final double PIXEL_TO_KM = 0.01;

    // Panning and zooming.
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point lastDragPoint = null;

    // Define biome types.
    public enum Terrain {
        SEA, PLAINS, FARMLAND, FOREST, MOUNTAINS
    }

    private java.util.List<Point> sites;
    private java.util.List<Terrain> terrains;
    private JLabel infoLabel;  // Displays simulation messages/info

    // For interaction.
    private int hoveredSiteIndex = -1;
    private int selectedSiteIndex = -1;

    // Civilization seeds.
    private java.util.List<CivSeed> civSeeds = new java.util.ArrayList<>();
    private boolean placingSeed = false;  // When true, next click places a seed

    // Road & budding thresholds.
    private static final int ROAD_NEIGHBORS = 5; // For graph construction
    private static final int ROAD_DISTANCE_THRESHOLD = 150;
    private static final double GROWTH_THRESHOLD = 5.0;
    private static final int BUDDING_DISTANCE_THRESHOLD = 200;
    private static final double BUDDING_PROBABILITY = 0.2;

    // Resource nodes.
    private java.util.List<ResourceNode> resourceNodes = new java.util.ArrayList<>();

    // Farmland conversion: conversion radius.
    private static final int FARMLAND_CONVERSION_DISTANCE = 80; // pixels

    // ----- Inner Classes -----

    // Civilization seed.
    private class CivSeed {
        Point location;
        int level; // 1: Village, 2: Town, 3: City
        double growthCounter;
        int population;
        int infrastructureLevel; // Starts at 1
        String faction; // "Red" or "Blue"
        public CivSeed(Point loc) {
            this.location = loc;
            this.level = 1;
            this.growthCounter = 0;
            this.population = 100;
            this.infrastructureLevel = 1;
            this.faction = (Math.random() < 0.5) ? "Red" : "Blue";
        }
    }

    // Resource node.
    private class ResourceNode {
        Point location;
        String type; // "Ore", "Lumber", "Fertile"
        int amount;
        public ResourceNode(Point loc, String type, int amount) {
            this.location = loc;
            this.type = type;
            this.amount = amount;
        }
    }

    // For optimal road routing: graph edge.
    private class Edge {
        int to;
        double cost;
        public Edge(int to, double cost) {
            this.to = to;
            this.cost = cost;
        }
    }

    public TerrainVoronoiDiagram() {
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        setBackground(Color.WHITE);
        sites = new java.util.ArrayList<>();
        terrains = new java.util.ArrayList<>();
        generateSitesAndTerrains();
        generateResourceNodes();
        addInteractionListeners();
    }

    // ----- Generation Methods -----

    private void generateSitesAndTerrains() {
        sites.clear();
        terrains.clear();
        for (int i = 0; i < numSites; i++) {
            int x = (int)(Math.random() * mapWidth);
            int y = (int)(Math.random() * mapHeight);
            Point site = new Point(x, y);
            sites.add(site);
            terrains.add(getTerrainForSite(site));
        }
    }

    private void generateResourceNodes() {
        resourceNodes.clear();
        int numNodes = Math.max(1, numSites / 2);
        for (int i = 0; i < numNodes; i++) {
            int x = (int)(Math.random() * mapWidth);
            int y = (int)(Math.random() * mapHeight);
            Point loc = new Point(x, y);
            Terrain t = getTerrainForSite(loc);
            String type;
            int amount;
            if(t == Terrain.MOUNTAINS) {
                type = "Ore";
                amount = 200;
            } else if(t == Terrain.FOREST) {
                type = "Lumber";
                amount = 150;
            } else if(t == Terrain.PLAINS) {
                type = "Fertile";
                amount = 180;
            } else {
                continue;
            }
            resourceNodes.add(new ResourceNode(loc, type, amount));
        }
    }

    // ----- Elevation & Biome Methods -----

    private double getElevation(double x, double y) {
        double frequency = 0.005;
        double elev = Math.sin(x * frequency) * Math.cos(y * frequency);
        return (elev + 1) / 2;
    }

    private Terrain getTerrainForSite(Point p) {
        double cx = mapWidth / 2.0, cy = mapHeight / 2.0;
        double dx = (p.x - cx) / cx, dy = (p.y - cy) / cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double elev = getElevation(p.x, p.y);
        if (distance > 0.8 || elev < 0.3) {
            return Terrain.SEA;
        }
        if (elev < 0.5)
            return Terrain.PLAINS;
        else if (elev < 0.7)
            return Terrain.FOREST;
        else
            return Terrain.MOUNTAINS;
    }

    private Color getTerrainColor(Terrain terrain) {
        switch (terrain) {
            case SEA:        return new Color(0, 102, 204);
            case PLAINS:     return new Color(102, 204, 0);
            case FARMLAND:   return new Color(210, 180, 140);
            case FOREST:     return new Color(34, 139, 34);
            case MOUNTAINS:  return new Color(128, 128, 128);
            default:         return Color.LIGHT_GRAY;
        }
    }

    // ----- Voronoi Cell Computation -----

    private Polygon getVoronoiCell(Point p) {
        java.util.List<Point2D.Double> poly = new java.util.ArrayList<>();
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
            awtPoly.addPoint((int)Math.round(pt.x), (int)Math.round(pt.y));
        }
        return awtPoly;
    }

    private java.util.List<Point2D.Double> clipPolygon(java.util.List<Point2D.Double> poly, double A, double B, double C) {
        java.util.List<Point2D.Double> newPoly = new java.util.ArrayList<>();
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

    private Point2D.Double getIntersection(Point2D.Double p1, Point2D.Double p2, double A, double B, double C) {
        double dx = p2.x - p1.x, dy = p2.y - p1.y;
        double denom = A * dx + B * dy;
        if (denom == 0) return null;
        double t = -(A * p1.x + B * p1.y + C) / denom;
        return new Point2D.Double(p1.x + t * dx, p1.y + t * dy);
    }

    private double computeArea(Polygon poly) {
        double area = 0;
        int n = poly.npoints;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += poly.xpoints[i] * poly.ypoints[j] - poly.xpoints[j] * poly.ypoints[i];
        }
        return Math.abs(area) / 2.0;
    }

    // ----- Civilization Seed Helper Methods -----

    private int getCellIndexForPoint(Point p) {
        double minDist = Double.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < sites.size(); i++) {
            double d = p.distance(sites.get(i));
            if (d < minDist) {
                minDist = d;
                index = i;
            }
        }
        return index;
    }

    private boolean cellOccupied(int cellIndex) {
        for (CivSeed seed : civSeeds) {
            if (getCellIndexForPoint(seed.location) == cellIndex) {
                return true;
            }
        }
        return false;
    }

    public void setAreaLabel(JLabel label) {
        this.infoLabel = label;
    }

    public void regenerateDiagram() {
        generateSitesAndTerrains();
        generateResourceNodes();
        hoveredSiteIndex = -1;
        selectedSiteIndex = -1;
        repaint();
    }

    public void updateParameters(int numSites, int margin, int width, int height) {
        this.numSites = numSites;
        this.margin = margin;
        this.mapWidth = width;
        this.mapHeight = height;
        setPreferredSize(new Dimension(mapWidth, mapHeight));
        regenerateDiagram();
    }

    // Prevent seeds from being placed in ocean.
    public void setPlacingSeed(boolean placing) {
        placingSeed = placing;
        if (infoLabel != null && placing) {
            infoLabel.setText("Click on a cell (non-ocean) to place a civilization seed (village).");
        }
    }

    public void addCivilizationSeed(Point p) {
        int cellIndex = getCellIndexForPoint(p);
        if (getTerrainForSite(sites.get(cellIndex)) == Terrain.SEA) {
            if (infoLabel != null) {
                infoLabel.setText("Cannot place seed in ocean tile.");
            }
            return;
        }
        if (cellOccupied(cellIndex)) {
            if (infoLabel != null) {
                infoLabel.setText("That cell already has a civilization seed.");
            }
            return;
        }
        civSeeds.add(new CivSeed(sites.get(cellIndex)));
        repaint();
    }

    // ----- Optimal Road Routing via Dual Graph (Dijkstra) -----

    private java.util.List<java.util.List<Edge>> buildGraph() {
        int n = sites.size();
        int k = Math.min(ROAD_NEIGHBORS, n - 1);
        java.util.List<java.util.List<Edge>> graph = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new java.util.ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            java.util.List<int[]> candidates = new java.util.ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double dist = sites.get(i).distance(sites.get(j));
                candidates.add(new int[]{j, (int)(dist * 1000)});
            }
            candidates.sort(Comparator.comparingInt(a -> a[1]));
            for (int m = 0; m < k && m < candidates.size(); m++) {
                int j = candidates.get(m)[0];
                double cost = sites.get(i).distance(sites.get(j));
                graph.get(i).add(new Edge(j, cost));
                graph.get(j).add(new Edge(i, cost));
            }
        }
        return graph;
    }

    private java.util.List<Integer> computeShortestPath(int start, int end) {
        int n = sites.size();
        double[] dist = new double[n];
        int[] prev = new int[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[start] = 0;
        
        PriorityQueue<int[]> queue = new PriorityQueue<>(Comparator.comparingDouble((int[] a) -> a[1]));
        queue.add(new int[]{start, 0});
        
        java.util.List<java.util.List<Edge>> graph = buildGraph();
        
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int u = cur[0];
            double d = cur[1];
            if (d > dist[u]) continue;
            if (u == end) break;
            for (Edge edge : graph.get(u)) {
                int v = edge.to;
                double nd = dist[u] + edge.cost;
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = u;
                    queue.add(new int[]{v, (int)(nd * 1000)});
                }
            }
        }
        if (dist[end] == Double.MAX_VALUE) return null;
        java.util.List<Integer> path = new java.util.ArrayList<>();
        for (int at = end; at != -1; at = prev[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }


    private java.util.List<Point> getOptimalRoadPath(CivSeed seed1, CivSeed seed2) {
        int startIndex = getCellIndexForPoint(seed1.location);
        int endIndex = getCellIndexForPoint(seed2.location);
        java.util.List<Integer> indicesPath = computeShortestPath(startIndex, endIndex);
        if (indicesPath == null || indicesPath.size() < 2) return null;
        java.util.List<Point> path = new java.util.ArrayList<>();
        for (int idx : indicesPath) {
            path.add(sites.get(idx));
        }
        return path;
    }

    // ----- Advance Time: Population, Infrastructure, Budding, Farmland Conversion -----

    public void advanceTime() {
        java.util.List<CivSeed> newSeeds = new java.util.ArrayList<>();
        for (CivSeed seed : civSeeds) {
            double growthIncrement = 1.0;
            for (ResourceNode rn : resourceNodes) {
                if (seed.location.distance(rn.location) < 100) {
                    if (rn.type.equals("Ore") || rn.type.equals("Lumber"))
                        growthIncrement += 0.5;
                    else if (rn.type.equals("Fertile"))
                        growthIncrement += 0.8;
                }
            }
            int friendlyConnections = 0;
            for (CivSeed other : civSeeds) {
                if (other == seed) continue;
                if (seed.faction.equals(other.faction) && seed.location.distance(other.location) < ROAD_DISTANCE_THRESHOLD)
                    friendlyConnections++;
            }
            growthIncrement += 0.2 * seed.infrastructureLevel * friendlyConnections;
            seed.growthCounter += growthIncrement;
            if (seed.level == 1)
                seed.population += 20;
            else if (seed.level == 2)
                seed.population += 40;
            else
                seed.population += 60;
            if (seed.growthCounter >= GROWTH_THRESHOLD && seed.level < 3) {
                seed.level++;
                seed.growthCounter = 0;
            }
            if (seed.population > 1000 * seed.infrastructureLevel) {
                seed.infrastructureLevel++;
            }
            if (seed.level == 3 && Math.random() < BUDDING_PROBABILITY) {
                int parentCell = getCellIndexForPoint(seed.location);
                for (int i = 0; i < sites.size(); i++) {
                    if (i == parentCell) continue;
                    if (seed.location.distance(sites.get(i)) < BUDDING_DISTANCE_THRESHOLD
                        && !cellOccupied(i)
                        && getTerrainForSite(sites.get(i)) != Terrain.SEA) {
                        newSeeds.add(new CivSeed(sites.get(i)));
                        break;
                    }
                }
            }
        }
        civSeeds.addAll(newSeeds);
        for (CivSeed seed : civSeeds) {
            for (int i = 0; i < sites.size(); i++) {
                if (seed.location.distance(sites.get(i)) < FARMLAND_CONVERSION_DISTANCE) {
                    Terrain current = terrains.get(i);
                    if (current == Terrain.PLAINS || current == Terrain.FOREST) {
                        terrains.set(i, Terrain.FARMLAND);
                    }
                }
            }
        }
        repaint();
    }

    // ----- Interaction Listeners -----

    private void addInteractionListeners() {
        addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            double oldScale = scale;
            scale *= Math.pow(1.1, -rotation);
            Point p = e.getPoint();
            offsetX = p.x - ((p.x - offsetX) * (scale / oldScale));
            offsetY = p.y - ((p.y - offsetY) * (scale / oldScale));
            repaint();
        });
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
                int diagramX = (int)((e.getX() - offsetX) / scale);
                int diagramY = (int)((e.getY() - offsetY) / scale);
                double minDist = Double.MAX_VALUE;
                int nearestIndex = -1;
                for (int i = 0; i < sites.size(); i++) {
                    Point site = sites.get(i);
                    double d2 = Math.pow(diagramX - site.x, 2) + Math.pow(diagramY - site.y, 2);
                    if (d2 < minDist) {
                        minDist = d2;
                        nearestIndex = i;
                    }
                }
                hoveredSiteIndex = nearestIndex;
                if (infoLabel != null && hoveredSiteIndex != -1) {
                    Polygon cell = getVoronoiCell(sites.get(hoveredSiteIndex));
                    double areaPixels = computeArea(cell);
                    double areaKm = areaPixels * (PIXEL_TO_KM * PIXEL_TO_KM);
                    Terrain biome = getTerrainForSite(sites.get(hoveredSiteIndex));
                    infoLabel.setText(String.format("Area: %.2f km², Biome: %s", areaKm, biome.name()));
                } else if (infoLabel != null) {
                    infoLabel.setText("Area: ");
                }
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (placingSeed) {
                    int diagramX = (int)((e.getX() - offsetX) / scale);
                    int diagramY = (int)((e.getY() - offsetY) / scale);
                    addCivilizationSeed(new Point(diagramX, diagramY));
                    placingSeed = false;
                    if (infoLabel != null) {
                        infoLabel.setText("Civilization seed placed.");
                    }
                } else {
                    selectedSiteIndex = hoveredSiteIndex;
                }
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
            Terrain terrain = getTerrainForSite(sites.get(i));
            Color terrainColor = getTerrainColor(terrain);
            g2d.setColor(terrainColor);
            g2d.fillPolygon(cell);
            g2d.setColor(terrainColor.darker());
            g2d.drawPolygon(cell);
        }
        // Draw resource nodes.
        for (ResourceNode rn : resourceNodes) {
            if (rn.type.equals("Ore"))
                g2d.setColor(Color.GRAY);
            else if (rn.type.equals("Lumber"))
                g2d.setColor(new Color(34,139,34));
            else if (rn.type.equals("Fertile"))
                g2d.setColor(Color.YELLOW);
            else
                g2d.setColor(Color.WHITE);
            g2d.fillOval(rn.location.x - 4, rn.location.y - 4, 8, 8);
        }
        // Draw roads along the optimal path.
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < civSeeds.size(); i++) {
            CivSeed seed1 = civSeeds.get(i);
            if (seed1.level < 2) continue;
            for (int j = i + 1; j < civSeeds.size(); j++) {
                CivSeed seed2 = civSeeds.get(j);
                if (seed2.level < 2) continue;
                java.util.List<Point> roadPath = getOptimalRoadPath(seed1, seed2);
                if (roadPath == null || roadPath.size() < 2) continue;
                // Draw each segment with a curve.
                for (int k = 0; k < roadPath.size() - 1; k++) {
                    Point p1 = roadPath.get(k);
                    Point p2 = roadPath.get(k+1);
                    int mx = (p1.x + p2.x) / 2;
                    int my = (p1.y + p2.y) / 2;
                    int dx = p2.x - p1.x;
                    int dy = p2.y - p1.y;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    double offsetAmount = len * 0.2;
                    double px = -dy / len;
                    double py = dx / len;
                    int cx = mx + (int)(px * offsetAmount);
                    int cy = my + (int)(py * offsetAmount);
                    QuadCurve2D.Double curve = new QuadCurve2D.Double(
                        p1.x, p1.y,
                        cx, cy,
                        p2.x, p2.y
                    );
                    Color roadColor = seed1.faction.equals(seed2.faction) ? Color.GREEN : Color.RED;
                    g2d.setColor(roadColor);
                    g2d.draw(curve);
                }
            }
        }
        // Draw civilization seeds.
        for (CivSeed seed : civSeeds) {
            int size = 10 + (seed.level - 1) * 5;
            Color fill = (seed.level == 1) ? Color.RED : (seed.level == 2 ? Color.ORANGE : Color.MAGENTA);
            g2d.setColor(fill);
            g2d.fillOval(seed.location.x - size/2, seed.location.y - size/2, size, size);
            g2d.setColor(Color.BLACK);
            String label = (seed.level == 1) ? "Village" : (seed.level == 2 ? "Town" : "City");
            g2d.drawString(label + " (" + seed.faction + ") Pop:" + seed.population + " Infra:" + seed.infrastructureLevel,
                           seed.location.x + size, seed.location.y);
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