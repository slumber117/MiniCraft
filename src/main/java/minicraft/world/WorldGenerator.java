package minicraft.world;

import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import com.github.xandergos.terraindiffusionmc.pipeline.ModelAssetManager;
import com.github.xandergos.terraindiffusionmc.world.HeightConverter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorldGenerator — pure terrain-diffusion with spatial smoothing.
 *
 * ── Pipeline (per 256×256 tile) ─────────────────────────────────────────
 *
 * 1. LocalTerrainProvider — ONNX diffusion model → raw HeightmapData
 * (metres, biome IDs, temperature, humidity)
 *
 * 2. HeightConverter — metres → approximate Minecraft Y
 * (coordinate adapter, no generation)
 *
 * 3. Tanh remap — bounds the height range regardless of how
 * extreme the model's metre values are:
 *
 * deviation = blockY − SEA_LEVEL
 * compressed = tanh(deviation / HEIGHT_SPREAD)
 * finalY = SEA_LEVEL + compressed × HEIGHT_AMPLITUDE
 *
 * A 200 m hill and a 5000 m peak both map to
 * sensible Minecraft Y values; neither clips
 * the world ceiling or falls through bedrock.
 *
 * 4. Gaussian blur — separable two-pass blur (horizontal + vertical)
 * eliminates pixel-to-pixel height jumps from
 * high-frequency diffusion noise. BLUR_RADIUS 10
 * smooths across ~20 blocks — enough to convert
 * sheer pencil-cliff drops into walkable slopes
 * while keeping large biome shapes intact.
 *
 * ── Why the old approach produced jagged mountains ───────────────────────
 *
 * Two compounding problems in the previous code:
 *
 * a) HeightConverter divided resolution by 1.8, artificially amplifying
 * every height value — turning gentle hills into dramatic cliffs.
 *
 * b) No spatial smoothing was applied, so any high-frequency noise in
 * the diffusion output was expressed directly as 1-block sheer drops.
 *
 * The tanh remap (step 3) fixes (a) in a model-agnostic way.
 * The Gaussian blur (step 4) fixes (b).
 *
 * ── Tuning guide ────────────────────────────────────────────────────────
 *
 * Still jagged → increase BLUR_RADIUS / BLUR_SIGMA
 * Terrain too flat → increase HEIGHT_AMPLITUDE
 * Peaks still too tall → decrease HEIGHT_SPREAD
 * Oceans too shallow → increase OCEAN_DEPTH_SCALE
 * Tile-edge seams → increase BLUR_RADIUS (blends edges softer)
 */
public class WorldGenerator {

    // ── Tanh remap constants ──────────────────────────────────────────────

    private static final float SEA_LEVEL = 512f;
    private static final float HEIGHT_SPREAD = 180f; // tanh saturation point
    private static final float HEIGHT_AMPLITUDE = 200f; // max Y deviation from sea level
    private static final float OCEAN_DEPTH_SCALE = 0.8f; // extra depth for negative values

    // ── Gaussian blur constants ───────────────────────────────────────────

    private static final int BLUR_RADIUS = 10; // kernel half-width in blocks
    private static final float BLUR_SIGMA = 5.0f; // Gaussian standard deviation

    // ── State ─────────────────────────────────────────────────────────────

    private final long seed;
    private final Map<Long, ProcessedTile> tileCache = new ConcurrentHashMap<>();
    private final float[] gaussianKernel;

    // ── Internal tile record ───────────────────────────────────────────────

    private static final class ProcessedTile {
        final float[][] blockHeightmap;
        final HeightmapData rawMlData;

        ProcessedTile(float[][] blockHeightmap, HeightmapData rawMlData) {
            this.blockHeightmap = blockHeightmap;
            this.rawMlData = rawMlData;
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    public WorldGenerator(long seed) {
        this.seed = seed;
        LocalTerrainProvider.init(seed);
        ModelAssetManager.ensureAssetsReady();
        this.gaussianKernel = buildGaussianKernel(BLUR_RADIUS, BLUR_SIGMA);
    }

    // ── Public generation API ─────────────────────────────────────────────

    public WorldCell generate(double x, double z) {
        return generateRegion((int) Math.floor(x), (int) Math.floor(z), 1, 1)[0][0];
    }

    public WorldCell generate(int x, int z) {
        return generateRegion(x, z, 1, 1)[0][0];
    }

    public WorldCell[][] generateRegion(int originX, int originZ, int width, int height) {
        WorldCell[][] cells = new WorldCell[width][height];

        int lastTileX = Integer.MIN_VALUE;
        int lastTileZ = Integer.MIN_VALUE;
        ProcessedTile currentTile = null;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                int absX = originX + x;
                int absZ = originZ + z;

                int tileX = Math.floorDiv(absX, 256) * 256;
                int tileZ = Math.floorDiv(absZ, 256) * 256;

                if (tileX != lastTileX || tileZ != lastTileZ || currentTile == null) {
                    currentTile = getOrProcessTile(tileX, tileZ);
                    lastTileX = tileX;
                    lastTileZ = tileZ;
                }

                int lx = absX - tileX;
                int lz = absZ - tileZ;

                float blockY = currentTile.blockHeightmap[lx][lz];
                float elev = blockY / (float) Chunk.HEIGHT;

                short mlBiomeId = currentTile.rawMlData.biomeIds[lz][lx];
                Biome biome = mapMlBiome(mlBiomeId);
                float temp = currentTile.rawMlData.temperature[lz][lx];
                float humid = currentTile.rawMlData.humidity[lz][lx];
                float continental = elev;

                cells[x][z] = new WorldCell(elev, temp, humid, continental, biome);
            }
        }

        return cells;
    }

    // ── Tile processing ───────────────────────────────────────────────────

    private ProcessedTile getOrProcessTile(int tileX, int tileZ) {
        long key = ((long) tileX << 32) | (tileZ & 0xFFFFFFFFL);

        return tileCache.computeIfAbsent(key, k -> {

            // Step 1 — fetch raw diffusion output
            LocalTerrainProvider provider = LocalTerrainProvider.getInstance();
            HeightmapData data = provider.fetchHeightmap(
                    tileZ, tileX, tileZ + 256, tileX + 256);

            float[][] raw = new float[256][256];

            // Step 2 — HeightConverter + tanh remap
            for (int lx = 0; lx < 256; lx++) {
                for (int lz = 0; lz < 256; lz++) {
                    short metres = (short) data.heightmap[lz][lx];

                    // Coordinate adapter: metres → approximate Minecraft Y
                    float blockY = HeightConverter.convertToMinecraftHeight(metres);

                    // Tanh remap: bounds the result regardless of model output range.
                    // Ocean floors get a slight extra scale so they stay visibly deep
                    // rather than being compressed to near sea level by tanh symmetry.
                    float deviation = blockY - SEA_LEVEL;
                    if (deviation < 0f)
                        deviation *= OCEAN_DEPTH_SCALE;

                    float compressed = (float) Math.tanh(deviation / HEIGHT_SPREAD);
                    blockY = SEA_LEVEL + compressed * HEIGHT_AMPLITUDE;

                    raw[lx][lz] = Math.max(1f, Math.min(Chunk.HEIGHT - 1f, blockY));
                }
            }

            // Step 3 — Gaussian blur: removes high-frequency jaggedness
            float[][] smoothed = gaussianBlur(raw);

            return new ProcessedTile(smoothed, data);
        });
    }

    // ── Gaussian blur ─────────────────────────────────────────────────────

    /**
     * Separable two-pass Gaussian blur on a 256×256 float array.
     *
     * Cost: O(256 × 256 × 2 × (2·BLUR_RADIUS+1)) — roughly 2.7 M multiply-
     * adds per tile, negligible compared to the ONNX inference call.
     *
     * Edge handling: clamp (nearest-neighbour) — avoids the dark-border
     * artifact that zero-padding would introduce at tile seams.
     */
    private float[][] gaussianBlur(float[][] map) {
        final int SIZE = 256;
        float[][] temp = new float[SIZE][SIZE];
        float[][] out = new float[SIZE][SIZE];

        // Horizontal pass (X axis)
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                float sum = 0f, weightSum = 0f;
                for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                    int sx = Math.max(0, Math.min(SIZE - 1, x + k));
                    float w = gaussianKernel[k + BLUR_RADIUS];
                    sum += map[sx][z] * w;
                    weightSum += w;
                }
                temp[x][z] = sum / weightSum;
            }
        }

        // Vertical pass (Z axis)
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                float sum = 0f, weightSum = 0f;
                for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                    int sz = Math.max(0, Math.min(SIZE - 1, z + k));
                    float w = gaussianKernel[k + BLUR_RADIUS];
                    sum += temp[x][sz] * w;
                    weightSum += w;
                }
                out[x][z] = sum / weightSum;
            }
        }

        return out;
    }

    /**
     * Builds a normalised 1-D Gaussian kernel.
     * Computed once at construction, shared across all tile generations.
     */
    private static float[] buildGaussianKernel(int radius, float sigma) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sum = 0f;
        for (int i = -radius; i <= radius; i++) {
            float v = (float) Math.exp(-(i * i) / (2f * sigma * sigma));
            kernel[i + radius] = v;
            sum += v;
        }
        for (int i = 0; i < size; i++)
            kernel[i] /= sum;
        return kernel;
    }

    // ── Biome mapping ─────────────────────────────────────────────────────

    private Biome mapMlBiome(short mlId) {
        switch (mlId) {
            case 1:
                return Biome.GRASSLAND;
            case 3:
                return Biome.TUNDRA;
            case 5:
                return Biome.DESERT;
            case 6:
                return Biome.JUNGLE;
            case 8:
                return Biome.FOREST;
            case 15:
                return Biome.REDWOOD;
            case 16:
                return Biome.SNOWY_FOREST;
            case 17:
                return Biome.SAVANNA;
            case 19:
                return Biome.HIGHLANDS;
            case 23:
                return Biome.JUNGLE;
            case 26:
                return Biome.DESERT;
            case 29:
                return Biome.GRASSLAND;
            case 31:
                return Biome.FOREST;
            case 32:
                return Biome.SNOWY_PEAKS;
            case 33:
                return Biome.SNOWY_PEAKS;
            case 35:
                return Biome.MOUNTAINS;
            case 41:
                return Biome.OCEAN;
            case 44:
                return Biome.OCEAN;
            case 46:
                return Biome.OCEAN;
            case 48:
                return Biome.FROZEN_OCEAN;
            case 108:
                return Biome.FOREST;
            case 115:
                return Biome.REDWOOD;
            case 116:
                return Biome.SNOWY_FOREST;
            default:
                return Biome.GRASSLAND;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public float getElevationOnly(double x, double z) {
        return generate(x, z).elevation;
    }

    public float getTemperatureOnly(double x, double z) {
        return generate(x, z).temperature;
    }

    public float getHumidityOnly(double x, double z) {
        return generate(x, z).humidity;
    }

    public float getContinentalnessOnly(double x, double z) {
        return generate(x, z).continentalness;
    }

    public long getSeed() {
        return seed;
    }

    @Override
    public String toString() {
        return "WorldGenerator[seed=" + seed + ", mode=pure_diffusion]";
    }
}