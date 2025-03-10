package newrealm.relaxation;

import java.awt.geom.Point2D;

public class LloydRelaxation {
    /**
     * Performs Lloyd relaxation on the provided sites.
     *
     * @param sites      Array of site points.
     * @param width      Width of the world map.
     * @param height     Height of the world map.
     * @param iterations Number of relaxation iterations.
     * @param sampleStep Sampling step to reduce computation.
     */
    public static void relax(Point2D.Double[] sites, int width, int height, int iterations, int sampleStep) {
        int numSites = sites.length;
        for (int iter = 0; iter < iterations; iter++) {
            double[] sumX = new double[numSites];
            double[] sumY = new double[numSites];
            int[] count = new int[numSites];

            // Sample pixels at a given step for performance.
            for (int i = 0; i < width; i += sampleStep) {
                for (int j = 0; j < height; j += sampleStep) {
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
                    sumX[nearest] += i;
                    sumY[nearest] += j;
                    count[nearest]++;
                }
            }

            // Update each site to the centroid of its assigned pixels.
            for (int k = 0; k < numSites; k++) {
                if (count[k] > 0) {
                    sites[k].x = sumX[k] / count[k];
                    sites[k].y = sumY[k] / count[k];
                }
            }
        }
    }
}
