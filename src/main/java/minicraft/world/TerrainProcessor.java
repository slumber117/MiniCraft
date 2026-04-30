package minicraft.world;

/**
 * Handles morphological processing of terrain heightmaps.
 * Implements Thermal Erosion (Diffusion) to simulate geological settling
 * directly on the AI-generated grids.
 */
public class TerrainProcessor {

    private final float talusThreshold;
    private final float erosionRate;
    private final int iterations;

    /**
     * @param talusThreshold The maximum stable slope (height difference between
     *                       tiles).
     * @param erosionRate    How much sediment is moved per pass (0.0 to 1.0).
     * @param iterations     Number of erosion passes (higher = more fidelity).
     */
    public TerrainProcessor(float talusThreshold, float erosionRate, int iterations) {
        this.talusThreshold = talusThreshold;
        this.erosionRate = erosionRate;
        this.iterations = iterations;
    }

    /**
     * Applies thermal erosion to a 2D heightmap in-place.
     * Uses a multi-pass approach to propagate sediment across the grid.
     */
    public void erode(float[][] grid) {
        int width = grid.length;
        int height = grid[0].length;
        float[][] nextGrid = new float[width][height];

        for (int iter = 0; iter < iterations; iter++) {
            // Copy current state
            for (int x = 0; x < width; x++) {
                System.arraycopy(grid[x], 0, nextGrid[x], 0, height);
            }

            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < height - 1; z++) {
                    float h = grid[x][z];

                    float dN = h - grid[x][z - 1];
                    float dS = h - grid[x][z + 1];
                    float dE = h - grid[x + 1][z];
                    float dW = h - grid[x - 1][z];

                    float maxDiff = Math.max(Math.max(dN, dS), Math.max(dE, dW));

                    if (maxDiff > talusThreshold) {
                        float slopes = 0;
                        if (dN > talusThreshold)
                            slopes += dN;
                        if (dS > talusThreshold)
                            slopes += dS;
                        if (dE > talusThreshold)
                            slopes += dE;
                        if (dW > talusThreshold)
                            slopes += dW;

                        if (slopes == 0)
                            continue; // Protection against math errors

                        // Total amount to move is proportional to the max slope over threshold
                        // Bounded by half the max diff to avoid oscillating peaks
                        float totalMove = (maxDiff - talusThreshold) * erosionRate;
                        if (totalMove > maxDiff * 0.5f) {
                            totalMove = maxDiff * 0.5f;
                        }

                        // Diffuse the terrain to lower neighbors
                        nextGrid[x][z] -= totalMove;
                        if (dN > talusThreshold)
                            nextGrid[x][z - 1] += totalMove * (dN / slopes);
                        if (dS > talusThreshold)
                            nextGrid[x][z + 1] += totalMove * (dS / slopes);
                        if (dE > talusThreshold)
                            nextGrid[x + 1][z] += totalMove * (dE / slopes);
                        if (dW > talusThreshold)
                            nextGrid[x - 1][z] += totalMove * (dW / slopes);
                    }
                }
            }

            // Sync state for the next diffusion pass
            for (int x = 0; x < width; x++) {
                System.arraycopy(nextGrid[x], 0, grid[x], 0, height);
            }
        }
    }
}