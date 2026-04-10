package minicraft.world;

import java.util.Random;

/**
 * Classic improved Perlin Noise implementation (Ken Perlin, 2002).
 * Used for generating smooth, natural-looking terrain heightmaps.
 */
public class PerlinNoise {

    private final int[] p = new int[512];

    /**
     * Constructor
     * 
     * @param seed The seed for the random number generator to ensure reproducible
     *             noise.
     */
    public PerlinNoise(long seed) {
        // Initialize the permutation array with values 0-255
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        // Shuffle the array using the seed
        Random rand = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        // Duplicate the array to 512 to avoid overflow during bitwise operations
        for (int i = 0; i < 256; i++) {
            p[i] = permutation[i];
            p[i + 256] = permutation[i];
        }
    }

    /**
     * Quintic interpolant function (The "Fade" function)
     * 6t^5 - 15t^4 + 10t^3
     */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /**
     * Linear interpolation
     */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /**
     * Calculates the gradient for a specific pseudo-random hash
     */
    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /**
     * The core Perlin Noise algorithm for a single point.
     * 
     * @return A value between -1.0 and 1.0
     */
    public double noise(double x, double y, double z) {
        // Find the unit cube that contains the point
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        // Find relative x, y, z of point in cube
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        // Compute fade curves for x, y, z
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        // Hash coordinates of the 8 cube corners
        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        // Blend the results from the 8 corners
        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),
                grad(p[BA], x - 1, y, z)),
                lerp(u, grad(p[AB], x, y - 1, z),
                        grad(p[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),
                        grad(p[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
                                grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    /**
     * Fractal Noise (Fractal Brownian Motion)
     * Combies multiple "octaves" of noise to create natural, jagged textures.
     * 
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param z           Z coordinate
     * @param octaves     Number of layers of noise (higher = more detail)
     * @param persistence How much each octave contributes (0.5 is standard)
     * @return A value between roughly -1.0 and 1.0
     */
    public double fractalNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0; // Used for normalizing the result

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;

            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }
}
