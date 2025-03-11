package newrealm.diagram;

import java.awt.image.BufferedImage;

public class MapGenerationResult {
    public final BufferedImage image;
    public final int[][] biomeMap;
    
    public MapGenerationResult(BufferedImage image, int[][] biomeMap) {
        this.image = image;
        this.biomeMap = biomeMap;
    }
}
