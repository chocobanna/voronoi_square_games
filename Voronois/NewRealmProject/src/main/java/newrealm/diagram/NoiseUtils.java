package newrealm.diagram;

/**
 * Helper functions for generating noise and smoothing noise maps.
 */
public class NoiseUtils {

    /**
     * Linear interpolation between values a and b.
     */
    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Fade function as defined by Ken Perlin.
     */
    public static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /**
     * A simple hash function that returns a pseudo-random value in [0, 1].
     */
    public static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }

    /**
     * Generates noise based on coordinates.
     */
    public static double noise(double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double sx = fade(x - x0);
        double sy = fade(y - y0);
        double n0 = hash(x0, y0);
        double n1 = hash(x1, y0);
        double ix0 = lerp(n0, n1, sx);
        n0 = hash(x0, y1);
        n1 = hash(x1, y1);
        double ix1 = lerp(n0, n1, sx);
        return lerp(ix0, ix1, sy);
    }

    /**
     * Generates fractal noise by summing multiple octaves.
     */
    public static double fractalNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        return total / maxValue;
    }

    /**
     * Applies smoothing to a noise map by averaging each cell with its neighbors.
     */
    public static double[][] smoothMap(double[][] map, int width, int height, int iterations) {
        double[][] result = new double[width][height];
        // Copy original map into result.
        for (int i = 0; i < width; i++) {
            System.arraycopy(map[i], 0, result[i], 0, height);
        }
        for (int iter = 0; iter < iterations; iter++) {
            double[][] temp = new double[width][height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    double sum = 0;
                    int count = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < width && nj >= 0 && nj < height) {
                                sum += result[ni][nj];
                                count++;
                            }
                        }
                    }
                    temp[i][j] = sum / count;
                }
            }
            result = temp;
        }
        return result;
    }
}
