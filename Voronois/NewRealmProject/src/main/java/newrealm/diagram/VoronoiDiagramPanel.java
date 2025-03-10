package newrealm.diagram;

import newrealm.config.Config;
import newrealm.relaxation.LloydRelaxation;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class VoronoiDiagramPanel extends JPanel {
    private int width;
    private int height;
    private Point2D.Double[] sites;
    private BufferedImage voronoiImage;
    private Color[] siteColors;

    public VoronoiDiagramPanel(int width, int height) {
        this.width = width;
        this.height = height;

        // Scale the number of sites to the map size using the density constant.
        int numSites = (int) (width * height * Config.POINT_DENSITY);
        sites = new Point2D.Double[numSites];
        siteColors = new Color[numSites];
        Random rand = new Random();

        // Generate random sites and assign each a pastel color.
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

        // Apply Lloyd relaxation to the sites.
        LloydRelaxation.relax(sites, width, height, Config.LLOYD_ITERATIONS, Config.SAMPLE_STEP);

        // Generate the Voronoi diagram image.
        voronoiImage = generateVoronoiImage();
    }

    private BufferedImage generateVoronoiImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int numSites = sites.length;
        int[][] cellIndices = new int[width][height];

        // Assign each pixel to the nearest site.
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

        // Color each pixel with the corresponding site color.
        // Draw boundaries (black) where adjacent pixels belong to different sites.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                boolean boundary = false;
                int current = cellIndices[i][j];
                if (i > 0 && cellIndices[i - 1][j] != current) boundary = true;
                else if (i < width - 1 && cellIndices[i + 1][j] != current) boundary = true;
                else if (j > 0 && cellIndices[i][j - 1] != current) boundary = true;
                else if (j < height - 1 && cellIndices[i][j + 1] != current) boundary = true;

                if (boundary) {
                    img.setRGB(i, j, Color.BLACK.getRGB());
                } else {
                    img.setRGB(i, j, siteColors[current].getRGB());
                }
            }
        }

        // Optionally, draw small circles at each site.
        Graphics2D g = img.createGraphics();
        for (int k = 0; k < sites.length; k++) {
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(voronoiImage, 0, 0, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
