package minicraft.world;

/**
 * Handles morphological processing of terrain heightmaps.
 * Implements Thermal Erosion (Diffusion) to simulate geological settling.
 */
public class TerrainProcessor {

    private final float talusThreshold;
    private final float erosionRate;
    private final int iterations;

    /**
     * @param talusThreshold The maximum stable slope (height difference between tiles).
     * @param erosionRate    How much sediment is moved per pass (0.0 to 1.0).
     * @param iterations     Number of erosion passes (higher = more fidelity).
     */
    public TerrainProcessor(float talusThreshold, float erosionRate, int iterations) {
        this.talusThreshold = talusThreshold;
        this.erosionRate    = erosionRate;
        this.iterations     = iterations;
    }

    /**
     * Applies thermal erosion to a 2D heightmap in-place.
     * Uses a multi-pass approach to propagate sediment across the grid.
     */
    public void erode(float[][] grid) {
        int width = grid.length;
        int height = grid[0].length;

        for (int iter = 0; iter < iterations; iter++) {
            // We use a simple 4-neighbor check. 
            // In each pass, we check slopes and move height from high to low.
            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < height - 1; z++) {
                    float h = grid[x][z];
                    
                    // Check neighbors: North, South, East, West
                    // We look for the neighbor with the lowest height
                    int lowX = x, lowZ = z;
                    float minH = h;

                    if (grid[x+1][z] < minH) { minH = grid[x+1][z]; lowX = x+1; lowZ = z; }
                    if (grid[x-1][z] < minH) { minH = grid[x-1][z]; lowX = x-1; lowZ = z; }
                    if (grid[x][z+1] < minH) { minH = grid[x][z+1]; lowX = x;   lowZ = z+1; }
                    if (grid[x][z-1] < minH) { minH = grid[x][z-1]; lowX = x;   lowZ = z-1; }

                    float diff = h - minH;
                    if (diff > talusThreshold) {
                        float move = (diff - talusThreshold) * erosionRate;
                        grid[x][z] -= move;
                        grid[lowX][lowZ] += move;
                    }
                }
            }
        }
    }
}
