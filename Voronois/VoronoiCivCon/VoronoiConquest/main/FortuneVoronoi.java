package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FortuneVoronoi {
    private int[][] regionAssignment;
    private BufferedImage voronoiImage;
    private List<Point> sites;
    private int mapWidth, mapHeight;

    public FortuneVoronoi(Point[] siteArray, int mapWidth, int mapHeight) {
        this.sites = new ArrayList<>(Arrays.asList(siteArray));
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        computeVoronoi();
    }

    private void computeVoronoi() {
        regionAssignment = new int[mapWidth][mapHeight];
        voronoiImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int nearestSite = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < sites.size(); i++) {
                    double dist = sites.get(i).distance(x, y);
                    if (dist < minDist) {
                        minDist = dist;
                        nearestSite = i;
                    }
                }
                regionAssignment[x][y] = nearestSite;
                float hue = (float) nearestSite / sites.size();
                int rgb = Color.HSBtoRGB(hue, 1.0f, 0.8f);
                voronoiImage.setRGB(x, y, rgb);
            }
        }
    }

    public int[][] getRegionAssignment() {
        return regionAssignment;
    }

    public BufferedImage getVoronoiImage() {
        return voronoiImage;
    }
}
