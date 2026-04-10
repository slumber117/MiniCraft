package minicraft.world;

/**
 * WorldGenerator — public entry point for terrain generation.
 *
 * Usage:
 * <pre>
 *   WorldGenerator gen = new WorldGenerator(12345L);
 *
 *   // Query a single tile:
 *   WorldCell cell = gen.generate(worldX, worldZ);
 *
 *   // Query a rectangular region (e.g., a chunk):
 *   WorldCell[][] chunk = gen.generateRegion(chunkOriginX, chunkOriginZ, 16, 16);
 * </pre>
 *
 * All coordinates are world-space integers (or doubles for sub-tile precision).
 * The generator is stateless after construction and fully thread-safe — multiple
 * chunk-loading threads may call it concurrently without synchronisation.
 *
 * ── Architecture overview ────────────────────────────────────────────────────
 *
 *   WorldGenerator
 *       │
 *       ├── ClimateMap          (temperature, humidity, continentalness)
 *       │       └── PerlinNoise ×5  (temp, humid, continent, warp×2)
 *       │
 *       └── ElevationMap        (final heightmap)
 *               └── PerlinNoise ×4  (base, ridge, detail, warp)
 *                   ClimateMap  (shared — passed by reference)
 *
 * The separation means climate and elevation can be queried independently.
 * For example, a biome-colour minimap only needs ClimateMap; a heightmap
 * renderer only needs ElevationMap.
 */
public class WorldGenerator {

    private final ClimateMap  climate;
    private final ElevationMap elevation;

    private final long seed;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Constructs a new world generator with the given seed.
     *
     * @param seed Any long — identical seeds always produce identical worlds.
     */
    public WorldGenerator(long seed) {
        this.seed      = seed;
        this.climate   = new ClimateMap(seed);
        this.elevation = new ElevationMap(seed, climate);
    }

    // ── Single-tile query ─────────────────────────────────────────────────

    /**
     * Generates full world data for a single world-space coordinate.
     *
     * This is the core method. All other generate* methods delegate here.
     *
     * @param x World X coordinate
     * @param z World Z coordinate (depth/south axis)
     * @return  A {@link WorldCell} containing elevation, climate, and biome.
     */
    public WorldCell generate(double x, double z) {
        float temp         = climate.getTemperature(x, z);
        float humid        = climate.getHumidity(x, z);
        float continental  = climate.getContinentalness(x, z);
        float elev         = elevation.getElevation(x, z);

        Biome biome = Biome.classify(temp, humid, elev);

        return new WorldCell(elev, temp, humid, continental, biome);
    }

    /** Integer-coordinate convenience overload. */
    public WorldCell generate(int x, int z) {
        return generate((double) x, (double) z);
    }

    // ── Region query ──────────────────────────────────────────────────────

    /**
     * Generates a rectangular grid of world cells.
     *
     * The returned array is indexed as [x][z] — i.e., result[0][0] corresponds
     * to (originX, originZ) and result[width-1][height-1] to
     * (originX + width - 1, originZ + height - 1).
     *
     * @param originX World X of the top-left corner
     * @param originZ World Z of the top-left corner
     * @param width   Number of tiles along the X axis
     * @param height  Number of tiles along the Z axis
     */
    public WorldCell[][] generateRegion(int originX, int originZ, int width, int height) {
        WorldCell[][] cells = new WorldCell[width][height];
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < height; dz++) {
                cells[dx][dz] = generate(originX + dx, originZ + dz);
            }
        }
        return cells;
    }

    // ── Thin accessors for specialised use ────────────────────────────────

    /**
     * Returns only the elevation at the given coordinate.
     * Cheaper than {@link #generate} when climate data is not needed.
     */
    public float getElevationOnly(double x, double z) {
        return elevation.getElevation(x, z);
    }

    /**
     * Returns only the temperature at the given coordinate.
     */
    public float getTemperatureOnly(double x, double z) {
        return climate.getTemperature(x, z);
    }

    /**
     * Returns only the humidity at the given coordinate.
     */
    public float getHumidityOnly(double x, double z) {
        return climate.getHumidity(x, z);
    }

    /**
     * Returns only the continentalness at the given coordinate.
     */
    public float getContinentalnessOnly(double x, double z) {
        return climate.getContinentalness(x, z);
    }

    // ── Metadata ──────────────────────────────────────────────────────────

    public long getSeed() {
        return seed;
    }

    @Override
    public String toString() {
        return "WorldGenerator[seed=" + seed + "]";
    }
}
