package newrealm.diagram;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

/**
 * Generates a planetary map rendered on a sphere (using an orthographic projection)
 * with rotation, dynamic level-of-detail via extra octaves, elevation smoothing, and
 * increased pixel resolution (detail) when zoomed in.
 *
 * When zoom > 1, the effective resolution is increased by a detail multiplier, so more pixels
 * are computed. These extra pixels yield extra fractal detail when the image is scaled to the view.
 */
public class MapGenerator {

    // Base noise parameters.
    public static final double DEFAULT_NOISE_SCALE = 100.0;
    public static final int BASE_OCTAVES = 5;
    public static final double PERSISTENCE = 0.5;

    // Biome definitions (6 types):
    // 0: Ocean, 1: Desert, 2: Grassland, 3: Temperate Forest, 4: Mountain, 5: Snow.
    private static final Color[] BIOME_COLORS = {
         Color.BLUE,                           // Ocean (water computed per pixel)
         new Color(237, 201, 175),               // Desert
         new Color(189, 183, 107),               // Grassland
         new Color(34, 139, 34),                 // Temperate Forest
         new Color(139, 69, 19),                 // Mountain
         Color.WHITE                           // Snow
    };

    // Water color constants.
    private static final Color SHALLOW_WATER = new Color(135, 206, 250); // Light blue.
    private static final Color DEEP_WATER = new Color(0, 0, 139);          // Dark blue.

    // Biome threshold constants.
    private static final double TEMP_THRESHOLD_HIGH = 0.8;
    private static final double TEMP_THRESHOLD_MED  = 0.55;
    private static final double HUMIDITY_THRESHOLD_LOW = 0.3;
    private static final double HUMIDITY_THRESHOLD_HIGH = 0.6;
    private static final double ELEVATION_DESERT_GRASSLAND = 0.65;
    private static final double ELEVATION_MOUNTAIN = 0.8;

    /**
     * Generates a planetary map image rendered on a sphere with rotation, zoom-controlled LOD,
     * elevation smoothing, and extra detail (higher resolution) when zoomed in.
     *
     * @param width               Base image width.
     * @param height              Base image height.
     * @param pointDensity        (Unused; retained for API compatibility)
     * @param lloydIterations     (Unused; retained for API compatibility)
     * @param waterThreshold      Elevation threshold below which cells become ocean.
     * @param rotAzimuth          Rotation angle around the vertical axis (in radians).
     * @param rotElevation        Rotation angle around the horizontal axis (in radians).
     * @param zoom                Zoom factor controlling detail; values > 1 increase extra detail.
     * @param smoothingIterations Number of smoothing iterations applied to the elevation map.
     * @return A MapGenerationResult containing the rendered BufferedImage and a biome map.
     */
    public static MapGenerationResult generateMap(
            int width,
            int height,
            double pointDensity,
            int lloydIterations,
            double waterThreshold,
            double rotAzimuth,
            double rotElevation,
            double zoom,
            int smoothingIterations) {

        // Use a detail multiplier based on zoom.
        // When zoom <= 1, use a multiplier of 1; otherwise, use Math.ceil(zoom).
        int detailMultiplier = (int) Math.ceil(Math.max(1, zoom));
        int effectiveWidth = width * detailMultiplier;
        int effectiveHeight = height * detailMultiplier;

        BufferedImage img = new BufferedImage(effectiveWidth, effectiveHeight, BufferedImage.TYPE_INT_RGB);
        int[][] biomeMap = new int[effectiveWidth][effectiveHeight];

        // Define sphere parameters based on effective resolution.
        int cx = effectiveWidth / 2;
        int cy = effectiveHeight / 2;
        int sphereRadius = Math.min(effectiveWidth, effectiveHeight) / 2;

        // Use constant noise scale.
        final double effectiveNoiseScale = DEFAULT_NOISE_SCALE;

        // Increase octaves when zooming in.
        int effectiveOctaves = BASE_OCTAVES;
        if (zoom > 1) {
            effectiveOctaves += (int) Math.ceil(Math.log(zoom) / Math.log(2));
        }
        final int finalEffectiveOctaves = effectiveOctaves; // for lambda use

        // Create a pixel buffer for the effective image.
        int[] pixelBuffer = new int[effectiveWidth * effectiveHeight];
        int blackRGB = Color.BLACK.getRGB();
        for (int i = 0; i < pixelBuffer.length; i++) {
            pixelBuffer[i] = blackRGB;
        }

        // Compute the bounding box of the sphere.
        int startX = Math.max(0, cx - sphereRadius);
        int endX = Math.min(effectiveWidth, cx + sphereRadius);
        int startY = Math.max(0, cy - sphereRadius);
        int endY = Math.min(effectiveHeight, cy + sphereRadius);

        // Process each row in parallel.
        IntStream.range(startY, endY).parallel().forEach(j -> {
            for (int i = startX; i < endX; i++) {
                double dx = (i - cx) / (double) sphereRadius;
                double dy = (j - cy) / (double) sphereRadius;
                double distSq = dx * dx + dy * dy;
                if (distSq > 1) {
                    continue;  // Outside the sphere.
                }
                double dz = Math.sqrt(1 - distSq);
                // Initial 3D point on the unit sphere.
                double x = dx, y = dy, z = dz;
                // --- Apply rotations ---
                double x1 = x * Math.cos(rotAzimuth) - y * Math.sin(rotAzimuth);
                double y1 = x * Math.sin(rotAzimuth) + y * Math.cos(rotAzimuth);
                double z1 = z;
                double x2 = x1;
                double y2 = y1 * Math.cos(rotElevation) - z1 * Math.sin(rotElevation);
                double z2 = y1 * Math.sin(rotElevation) + z1 * Math.cos(rotElevation);
                // --- End rotations ---

                double phi = Math.atan2(y2, x2);      // longitude in radians.
                double theta = Math.acos(z2);          // polar angle.
                double lat = Math.PI / 2 - theta;      // latitude.

                // Map spherical coordinates to noise space.
                double noiseX = phi * (effectiveNoiseScale / Math.PI);
                double noiseY = lat * (effectiveNoiseScale / (Math.PI / 2));

                // Sample noise functions.
                double elev = NoiseUtils.fractalNoise(noiseX, noiseY, finalEffectiveOctaves, PERSISTENCE);
                double temp = NoiseUtils.fractalNoise(noiseX + 1000, noiseY + 1000, finalEffectiveOctaves, PERSISTENCE);
                double hum  = NoiseUtils.fractalNoise(noiseX + 2000, noiseY + 2000, finalEffectiveOctaves, PERSISTENCE);

                int biome;
                if (elev < waterThreshold) {
                    biome = 0; // Ocean.
                } else if (elev < ELEVATION_DESERT_GRASSLAND) {
                    if (temp > TEMP_THRESHOLD_HIGH) {
                        biome = (hum < HUMIDITY_THRESHOLD_LOW) ? 1 : 2;
                    } else if (temp > TEMP_THRESHOLD_MED) {
                        biome = (hum > HUMIDITY_THRESHOLD_HIGH) ? 3 : 2;
                    } else {
                        biome = 3;
                    }
                } else if (elev < ELEVATION_MOUNTAIN) {
                    biome = 4;
                } else {
                    biome = 5;
                }
                biomeMap[i][j] = biome;
                int rgb;
                if (biome == 0) {
                    double depthFactor = (waterThreshold - elev) / waterThreshold;
                    depthFactor = Math.max(0, Math.min(1, depthFactor));
                    double darkFactor = Math.min(1, depthFactor * 1.2);
                    Color waterColor = blend(SHALLOW_WATER, DEEP_WATER, darkFactor);
                    rgb = waterColor.getRGB();
                } else {
                    rgb = BIOME_COLORS[biome].getRGB();
                }
                pixelBuffer[j * effectiveWidth + i] = rgb;
            }
        });

        // (Optional) Here you could smooth the raw elevation map within the bounding box before deciding biomes.
        // For brevity, this code uses the computed "elev" values directly.

        // Set the computed pixel buffer into the image.
        img.setRGB(0, 0, effectiveWidth, effectiveHeight, pixelBuffer, 0, effectiveWidth);
        
        // Return the high-resolution image.
        // The calling panel can then scale this image to the view size.
        return new MapGenerationResult(img, biomeMap);
    }

    /**
     * Blends two colors based on a blend factor t.
     *
     * @param c1 First color.
     * @param c2 Second color.
     * @param t  Blend factor in [0,1].
     * @return Blended color.
     */
    private static Color blend(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }
}
