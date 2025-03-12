package newrealm.diagram;

import java.awt.image.BufferedImage;

/**
 * Data class to hold the final rendered map (as a BufferedImage) and the biome map.
 */
public class MapGenerationResult {
    public final BufferedImage image;
    public final int[][] biomeMap;
    
    public MapGenerationResult(BufferedImage image, int[][] biomeMap) {
        this.image = image;
        this.biomeMap = biomeMap;
    }
}
