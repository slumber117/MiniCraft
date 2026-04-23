package minicraft.world;

import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;

/**
 * WorldGenerator — public entry point for terrain generation.
 *
 * Modified to utilize HuggingFace/ONNX AI Terrain Diffusion via LocalTerrainProvider.
 */
public class WorldGenerator {

    private final long seed;

    public WorldGenerator(long seed) {
        this.seed = seed;
        LocalTerrainProvider.init(seed);
        com.github.xandergos.terraindiffusionmc.pipeline.ModelAssetManager.ensureAssetsReady();
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
     * Generates a rectangular grid using ML inference.
     */
    public WorldCell[][] generateRegion(int originX, int originZ, int width, int height) {
        LocalTerrainProvider provider = LocalTerrainProvider.getInstance();
        HeightmapData data = provider.fetchHeightmap(originZ, originX, originZ + height, originX + width);

        WorldCell[][] cells = new WorldCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                float meters = data.heightmap[z][x];
                int targetY = com.github.xandergos.terraindiffusionmc.world.HeightConverter.convertToMinecraftHeight((short)meters);
                float elev = (float)targetY / minicraft.world.Chunk.HEIGHT;

                short mlBiomeId = data.biomeIds[z][x];
                Biome biome = mapMlBiome(mlBiomeId);

                // Climate values from real AI inference
                float temp = data.temperature[z][x];
                float humid = data.humidity[z][x];
                float continental = elev / 100f; // Scale it down for the engine's internal checks

                cells[x][z] = new WorldCell(elev, temp, humid, continental, biome);
            }
        }
        return cells;
    }

    private Biome mapMlBiome(short mlId) {
        switch (mlId) {
            case 1:  /* PLAINS */ return Biome.GRASSLAND;
            case 3:  /* SNOWY_PLAINS */ return Biome.TUNDRA;
            case 5:  /* DESERT */ return Biome.DESERT;
            case 6:  /* SWAMP */ return Biome.JUNGLE;
            case 8:  /* FOREST */ return Biome.FOREST;
            case 15: /* TAIGA */ return Biome.REDWOOD;
            case 16: /* SNOWY_TAIGA */ return Biome.SNOWY_FOREST;
            case 17: /* SAVANNA */ return Biome.SAVANNA;
            case 19: /* WINDSWEPT_HILLS */ return Biome.HIGHLANDS;
            case 23: /* JUNGLE */ return Biome.JUNGLE;
            case 26: /* BADLANDS */ return Biome.DESERT;
            case 29: /* MEADOW */ return Biome.GRASSLAND;
            case 31: /* GROVE */ return Biome.FOREST;
            case 32: /* SNOWY_SLOPES */ return Biome.SNOWY_PEAKS;
            case 33: /* FROZEN_PEAKS */ return Biome.SNOWY_PEAKS;
            case 35: /* STONY_PEAKS */ return Biome.MOUNTAINS;
            case 41: /* WARM_OCEAN */ return Biome.OCEAN;
            case 44: /* OCEAN */ return Biome.OCEAN;
            case 46: /* COLD_OCEAN */ return Biome.OCEAN;
            case 48: /* FROZEN_OCEAN */ return Biome.FROZEN_OCEAN;
            case 108: /* FOREST_SPARSE */ return Biome.FOREST;
            case 115: /* TAIGA_SPARSE */ return Biome.REDWOOD;
            case 116: /* SNOWY_TAIGA_SPARSE */ return Biome.SNOWY_FOREST;
            default: return Biome.GRASSLAND;
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
        return "WorldGenerator[seed=" + seed + "]";
    }
}
