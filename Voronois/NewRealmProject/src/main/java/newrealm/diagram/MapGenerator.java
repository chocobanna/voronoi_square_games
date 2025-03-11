package newrealm.diagram;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class MapGenerator {

    // Noise parameters.
    public static final double DEFAULT_NOISE_SCALE = 100.0;
    public static final int OCTAVES = 5;
    public static final double PERSISTENCE = 0.5;

    // Biome grouping (6 types):
    // 0: Ocean, 1: Desert, 2: Grassland, 3: Temperate Forest, 4: Mountain, 5: Snow.
    private static final Color[] BIOME_COLORS = {
         Color.BLUE,                           // Ocean (base; water color computed separately)
         new Color(237, 201, 175),               // Desert
         new Color(189, 183, 107),               // Grassland
         new Color(34, 139, 34),                 // Temperate Forest
         new Color(139, 69, 19),                 // Mountain
         Color.WHITE                           // Snow
    };

    // Water colors.
    private static final Color SHALLOW_WATER = new Color(135, 206, 250); // Light blue.
    private static final Color DEEP_WATER = new Color(0, 0, 139);          // Dark blue.

    /**
     * Generates a map given the parameters.
     *
     * @param width          the width of the map
     * @param height         the height of the map
     * @param pointDensity   the density for Voronoi sites
     * @param lloydIterations number of relaxation iterations (if used)
     * @param waterThreshold elevation below which becomes ocean
     * @return a MapGenerationResult containing the final image and biome map
     */
    public static MapGenerationResult generateMap(
            int width,
            int height,
            double pointDensity,
            int lloydIterations,
            double waterThreshold) {

        // --- Step 1: Generate noise maps ---
        double[][] elevMap = new double[width][height];
        double[][] tempMap = new double[width][height];
        double[][] humMap  = new double[width][height];
        double[][] windMap = new double[width][height];

        double tempScale = 200.0;
        double humScale  = 200.0;
        double windScale = 150.0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                elevMap[i][j] = NoiseUtils.fractalNoise(i / DEFAULT_NOISE_SCALE, j / DEFAULT_NOISE_SCALE, OCTAVES, PERSISTENCE);
                tempMap[i][j] = NoiseUtils.fractalNoise((i + 1000) / tempScale, (j + 1000) / tempScale, OCTAVES, PERSISTENCE);
                humMap[i][j]  = NoiseUtils.fractalNoise((i + 2000) / humScale, (j + 2000) / humScale, OCTAVES, PERSISTENCE);
                windMap[i][j] = NoiseUtils.fractalNoise((i + 3000) / windScale, (j + 3000) / windScale, OCTAVES, PERSISTENCE);
            }
        }
        // Smooth elevation for better slopes.
        elevMap = NoiseUtils.smoothMap(elevMap, width, height, 2);

        // --- Step 2: Generate biome map using refined logic ---
        int[][] biomeMap = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double elev = elevMap[i][j];
                double temp = tempMap[i][j];
                double hum  = humMap[i][j];
                int biome;
                if (elev < waterThreshold) {
                    biome = 0; // Ocean.
                } else if (elev < 0.65) {
                    if (temp > 0.8) {
                        if (hum < 0.3) {
                            biome = 1; // Desert.
                        } else {
                            biome = 2; // Grassland.
                        }
                    } else if (temp > 0.55) {
                        if (hum > 0.6) {
                            biome = 3; // Temperate Forest.
                        } else {
                            biome = 2; // Grassland.
                        }
                    } else {
                        biome = 3; // Temperate Forest.
                    }
                } else if (elev < 0.8) {
                    biome = 4; // Mountain.
                } else {
                    biome = 5; // Snow.
                }
                biomeMap[i][j] = biome;
            }
        }

        // --- Step 3: Generate Voronoi sites ---
        int numSites = (int) (width * height * pointDensity);
        Point2D.Double[] sites = new Point2D.Double[numSites];
        Random rand = new Random();
        for (int i = 0; i < numSites; i++) {
            double x = rand.nextDouble() * width;
            double y = rand.nextDouble() * height;
            sites[i] = new Point2D.Double(x, y);
        }
        // Optionally, you could call LloydRelaxation.relax(sites, width, height, lloydIterations, Config.SAMPLE_STEP);

        // --- Step 4: Compute nearest-site assignment ---
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

        // --- Step 5: Determine majority biome per Voronoi cell ---
        int[] majorityBiome = new int[numSites];
        int[][] counts = new int[numSites][6]; // 6 biomes.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int site = cellIndices[i][j];
                int biome = biomeMap[i][j];
                counts[site][biome]++;
            }
        }
        for (int s = 0; s < numSites; s++) {
            int maxCount = -1;
            int majority = 0;
            for (int b = 0; b < 6; b++) {
                if (counts[s][b] > maxCount) {
                    maxCount = counts[s][b];
                    majority = b;
                }
            }
            majorityBiome[s] = majority;
        }

        // --- Step 6: Create final image ---
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // For water, sample at lower resolution.
        int waterResFactor = 4;
        int wBlocks = (width + waterResFactor - 1) / waterResFactor;
        int hBlocks = (height + waterResFactor - 1) / waterResFactor;
        Color[][] waterColorMap = new Color[wBlocks][hBlocks];
        for (int bx = 0; bx < wBlocks; bx++) {
            for (int by = 0; by < hBlocks; by++) {
                int sampleX = bx * waterResFactor;
                int sampleY = by * waterResFactor;
                if (sampleX >= width) sampleX = width - 1;
                if (sampleY >= height) sampleY = height - 1;
                double elev = elevMap[sampleX][sampleY];
                double depthFactor = (waterThreshold - elev) / waterThreshold;
                depthFactor = Math.max(0, Math.min(1, depthFactor));
                // Trend more toward darker water.
                double darkFactor = Math.min(1, depthFactor * 1.2);
                waterColorMap[bx][by] = blend(SHALLOW_WATER, DEEP_WATER, darkFactor);
            }
        }
        // For each pixel, assign color.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int site = cellIndices[i][j];
                int biome = majorityBiome[site];
                if (biome == 0) { // Ocean.
                    int bx = i / waterResFactor;
                    int by = j / waterResFactor;
                    Color waterColor = waterColorMap[bx][by];
                    img.setRGB(i, j, waterColor.getRGB());
                } else {
                    // Remove land color ramping; simply use the base color.
                    img.setRGB(i, j, BIOME_COLORS[biome].getRGB());
                }
            }
        }
        return new MapGenerationResult(img, biomeMap);
    }

    // Helper method to blend two colors.
    private static Color blend(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }
}
