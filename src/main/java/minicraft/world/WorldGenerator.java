package minicraft.world;

import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
// Assuming HeightConverter is in this package based on your original file
import com.github.xandergos.terraindiffusionmc.world.HeightConverter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorldGenerator — public entry point for terrain generation.
 *
 * Fuses HuggingFace/ONNX AI Terrain Diffusion with Perlin Noise detailing
 * and Thermal Erosion diffusion for natural settling.
 */
public class WorldGenerator {

    private final long seed;

    // Procedural augmentation tools
    private final PerlinNoise detailNoise;
    private final TerrainProcessor thermalErosion;

    // Thread-safe cache to hold fully processed 256x256 ML tiles
    private final Map<Long, ProcessedTile> tileCache = new ConcurrentHashMap<>();

    // Internal class to hold our pipeline results
    private static class ProcessedTile {
        final float[][] blockHeightmap;
        final HeightmapData rawMlData;

        ProcessedTile(float[][] blockHeightmap, HeightmapData rawMlData) {
            this.blockHeightmap = blockHeightmap;
            this.rawMlData = rawMlData;
        }
    }

    public WorldGenerator(long seed) {
        this.seed = seed;
        LocalTerrainProvider.init(seed);
        com.github.xandergos.terraindiffusionmc.pipeline.ModelAssetManager.ensureAssetsReady();

        // Setup traditional procedural tools to augment the ML Diffusion base
        this.detailNoise = new PerlinNoise(seed);

        // Erosion setup: 1.2 blocks max stable slope, 40% sediment move rate, 5 passes
        this.thermalErosion = new TerrainProcessor(1.2f, 0.4f, 5);
    }

    public WorldCell generate(double x, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        return generateRegion(ix, iz, 1, 1)[0][0];
    }

    public WorldCell generate(int x, int z) {
        return generateRegion(x, z, 1, 1)[0][0];
    }

    /**
     * Fetches the ML Heightmap, applies procedural rock detailing, and simulates
     * thermal erosion. Caches the 256x256 tile to prevent chunk-border seams.
     */
    private ProcessedTile getOrProcessTile(int tileX, int tileZ) {
        long key = ((long) tileX << 32) | (tileZ & 0xFFFFFFFFL);
        return tileCache.computeIfAbsent(key, k -> {

            LocalTerrainProvider provider = LocalTerrainProvider.getInstance();
            HeightmapData data = provider.fetchHeightmap(tileZ, tileX, tileZ + 256, tileX + 256);

            float[][] map = new float[256][256];

            // Step 1: Base ML Terrain + Perlin Micro-Detail
            for (int lx = 0; lx < 256; lx++) {
                for (int lz = 0; lz < 256; lz++) {
                    float meters = data.heightmap[lz][lx];

                    // Convert continuous ML meters into discrete Minecraft Y-levels
                    int baseBlockY = HeightConverter.convertToMinecraftHeight((short) meters);

                    // Add high-frequency fractal noise for natural rockiness
                    double nx = (tileX + lx) * 0.03;
                    double nz = (tileZ + lz) * 0.03;
                    float detail = (float) detailNoise.fractalNoise(nx, 0, nz, 4, 0.45);

                    // Scale detail based on elevation (Mountains = jagged, Plains = smooth)
                    float scale = Math.max(0.5f, (baseBlockY - 60) / 25.0f);
                    float noiseContribution = detail * 5.0f * scale; // up to +/- 5 blocks

                    map[lx][lz] = baseBlockY + noiseContribution;
                }
            }

            // Step 2: Terrain Diffusion / Thermal Erosion
            // This naturally smooths the noise we just added, creating realistic slopes
            thermalErosion.erode(map);

            return new ProcessedTile(map, data);
        });
    }

    /**
     * Generates a rectangular grid of voxel data from the processed ML tiles.
     */
    public WorldCell[][] generateRegion(int originX, int originZ, int width, int height) {
        WorldCell[][] cells = new WorldCell[width][height];

        int lastTileX = -999999, lastTileZ = -999999;
        ProcessedTile currentTile = null;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                int absX = originX + x;
                int absZ = originZ + z;

                int tileX = Math.floorDiv(absX, 256) * 256;
                int tileZ = Math.floorDiv(absZ, 256) * 256;

                // Grab the cached processed tile (or generate it)
                if (tileX != lastTileX || tileZ != lastTileZ || currentTile == null) {
                    currentTile = getOrProcessTile(tileX, tileZ);
                    lastTileX = tileX;
                    lastTileZ = tileZ;
                }

                int lx = absX - tileX;
                int lz = absZ - tileZ;

                // Read the finalized, eroded block height
                float blockY = currentTile.blockHeightmap[lx][lz];

                // Clamp height to safe chunk boundaries
                blockY = Math.max(0, Math.min(255, blockY));

                // Convert back to normalized float for internal WorldCell format
                float elev = blockY / 256.0f; // Assumes Chunk.HEIGHT is 256

                // Map ML metrics
                short mlBiomeId = currentTile.rawMlData.biomeIds[lz][lx];
                Biome biome = mapMlBiome(mlBiomeId);

                float temp = currentTile.rawMlData.temperature[lz][lx];
                float humid = currentTile.rawMlData.humidity[lz][lx];
                float continental = elev / 100f;

                cells[x][z] = new WorldCell(elev, temp, humid, continental, biome);
            }
        }
        return cells;
    }

    private Biome mapMlBiome(short mlId) {
        switch (mlId) {
            case 1:
                /* PLAINS */ return Biome.GRASSLAND;
            case 3:
                /* SNOWY_PLAINS */ return Biome.TUNDRA;
            case 5:
                /* DESERT */ return Biome.DESERT;
            case 6:
                /* SWAMP */ return Biome.JUNGLE;
            case 8:
                /* FOREST */ return Biome.FOREST;
            case 15:
                /* TAIGA */ return Biome.REDWOOD;
            case 16:
                /* SNOWY_TAIGA */ return Biome.SNOWY_FOREST;
            case 17:
                /* SAVANNA */ return Biome.SAVANNA;
            case 19:
                /* WINDSWEPT_HILLS */ return Biome.HIGHLANDS;
            case 23:
                /* JUNGLE */ return Biome.JUNGLE;
            case 26:
                /* BADLANDS */ return Biome.DESERT;
            case 29:
                /* MEADOW */ return Biome.GRASSLAND;
            case 31:
                /* GROVE */ return Biome.FOREST;
            case 32:
                /* SNOWY_SLOPES */ return Biome.SNOWY_PEAKS;
            case 33:
                /* FROZEN_PEAKS */ return Biome.SNOWY_PEAKS;
            case 35:
                /* STONY_PEAKS */ return Biome.MOUNTAINS;
            case 41:
                /* WARM_OCEAN */ return Biome.OCEAN;
            case 44:
                /* OCEAN */ return Biome.OCEAN;
            case 46:
                /* COLD_OCEAN */ return Biome.OCEAN;
            case 48:
                /* FROZEN_OCEAN */ return Biome.FROZEN_OCEAN;
            case 108:
                /* FOREST_SPARSE */ return Biome.FOREST;
            case 115:
                /* TAIGA_SPARSE */ return Biome.REDWOOD;
            case 116:
                /* SNOWY_TAIGA_SPARSE */ return Biome.SNOWY_FOREST;
            default:
                return Biome.GRASSLAND;
        }
    }

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
        return "WorldGenerator[seed=" + seed + ", AI_Driven=true]";
    }
}