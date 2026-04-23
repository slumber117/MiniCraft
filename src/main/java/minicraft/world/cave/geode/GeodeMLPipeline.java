package minicraft.world.cave.geode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

/**
 * GeodeMLPipeline — High-performance Java inference for the Geode NCA.
 *
 * This class runs a 3D Neural Cellular Automata to 'grow' a geode from noise.
 * It loads weights exported by the Python ML core.
 */
public class GeodeMLPipeline {
    private static final String WEIGHTS_PATH = "/ml/geode_nca_weights.json";
    private static final int GRID_SIZE = 13;
    private static final int STEPS = 10;
    
    private final float[][] w1, w2;
    private final float[] b1, b2;
    private final int channels, hidden;

    public GeodeMLPipeline() {
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(WEIGHTS_PATH)),
                StandardCharsets.UTF_8)) {
            
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonObject weights = root.getAsJsonObject("weights");
            
            this.channels = root.get("channels").getAsInt();
            this.w1 = gson.fromJson(weights.get("w1"), float[][].class);
            this.w2 = gson.fromJson(weights.get("w2"), float[][].class);
            this.b1 = gson.fromJson(weights.get("bias1"), float[].class);
            this.b2 = gson.fromJson(weights.get("bias2"), float[].class);
            this.hidden = b1.length;
            
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Geode ML weights from " + WEIGHTS_PATH, e);
        }
    }

    /**
     * Generates a 3D geode blueprint for a specific seed and depth.
     * Returns a 3D array: [grid_size][grid_size][grid_size] where values are
     * from GeodeCell.Layer ordinal.
     */
    public byte[][][] generateBlueprint(long seed, float depthFraction) {
        Random rng = new Random(seed);
        float[][][][] grid = new float[GRID_SIZE][GRID_SIZE][GRID_SIZE][channels];
        
        // 1. Initialise with seed noise in the center
        int mid = GRID_SIZE / 2;
        for (int i = mid-1; i <= mid+1; i++) {
            for (int j = mid-1; j <= mid+1; j++) {
                for (int k = mid-1; k <= mid+1; k++) {
                    grid[i][j][k][2] = 0.5f + rng.nextFloat() * 0.5f; // Hollow potential
                    for (int c = 3; c < channels; c++) {
                        grid[i][j][k][c] = rng.nextFloat() * 2 - 1; // Latent noise
                    }
                }
            }
        }

        // 2. NCA Growth steps
        float[][][][] nextGrid = new float[GRID_SIZE][GRID_SIZE][GRID_SIZE][channels];
        for (int s = 0; s < STEPS; s++) {
            runNCAStep(grid, nextGrid, rng);
            // Swap grids
            float[][][][] tmp = grid;
            grid = nextGrid;
            nextGrid = tmp;
        }

        // 3. Map to Geode layers
        byte[][][] blueprint = new byte[GRID_SIZE][GRID_SIZE][GRID_SIZE];
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    float shell = grid[x][y][z][0];
                    float crystal = grid[x][y][z][1];
                    float hollow = grid[x][y][z][2];
                    
                    if (crystal > 0.4f && crystal > shell) {
                        blueprint[x][y][z] = (byte) GeodeCell.Layer.CRYSTAL.ordinal();
                    } else if (shell > 0.3f || hollow > 0.6f) {
                        if (hollow > 0.8f) {
                            blueprint[x][y][z] = (byte) GeodeCell.Layer.HOLLOW.ordinal();
                        } else {
                            blueprint[x][y][z] = (byte) GeodeCell.Layer.SHELL.ordinal();
                        }
                    } else {
                        blueprint[x][y][z] = (byte) GeodeCell.Layer.OUTSIDE.ordinal();
                    }
                }
            }
        }
        return blueprint;
    }

    private void runNCAStep(float[][][][] in, float[][][][] out, Random rng) {
        // Implementation of NCA perception + MLP update
        // (dx, dy, dz, identity) convolution
        for (int x = 1; x < GRID_SIZE - 1; x++) {
            for (int y = 1; y < GRID_SIZE - 1; y++) {
                for (int z = 1; z < GRID_SIZE - 1; z++) {
                    
                    float[] perception = new float[channels * 4];
                    for (int c = 0; c < channels; c++) {
                        perception[c] = in[x][y][z][c]; // Identity
                        perception[channels + c] = in[x+1][y][z][c] - in[x-1][y][z][c]; // dx
                        perception[2*channels + c] = in[x][y+1][z][c] - in[x][y-1][z][c]; // dy
                        perception[3*channels + c] = in[x][y][z+1][c] - in[x][y][z-1][c]; // dz
                    }

                    // MLP Layer 1
                    float[] h = new float[hidden];
                    for (int i = 0; i < hidden; i++) {
                        float sum = b1[i];
                        for (int j = 0; j < perception.length; j++) {
                            sum += perception[j] * w1[j][i];
                        }
                        h[i] = Math.max(0, sum); // ReLU
                    }

                    // MLP Layer 2
                    for (int c = 0; c < channels; c++) {
                        float ds = b2[c];
                        for (int i = 0; i < hidden; i++) {
                            ds += h[i] * w2[i][c];
                        }
                        
                        // Stochastic update
                        if (rng.nextFloat() < 0.5f) {
                            out[x][y][z][c] = in[x][y][z][c] + ds * 0.1f;
                        } else {
                            out[x][y][z][c] = in[x][y][z][c];
                        }
                    }
                }
            }
        }
    }
}
