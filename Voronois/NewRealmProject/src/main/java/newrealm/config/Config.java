package newrealm.config;

public class Config {
    // Density factor: points per pixel.
    // For example, a density of 0.00005 produces about 24 sites for an 800x600 map.
    public static final double POINT_DENSITY = 0.00005;
    
    // Number of iterations for Lloyd relaxation.
    public static final int LLOYD_ITERATIONS = 5;
    
    // Sampling step to reduce computation during relaxation.
    public static final int SAMPLE_STEP = 4;
}
