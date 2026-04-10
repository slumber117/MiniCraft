package minicraft.world.cave;

import minicraft.world.WorldCell;

/**
 * CaveCarver — master coordinator for all underground carving systems.
 *
 * This is the single class your chunk builder talks to. It holds one instance
 * of each sub-carver and routes queries through all of them, returning the
 * final CaveCell for any voxel.
 *
 * ── Priority order (first match wins) ────────────────────────────────────
 *
 *   1. RavineCarver       — dramatic surface cracks (rarest)
 *   2. CavernCarver       — large noise-intersection chambers
 *   3. UnderwaterCaveCarver — flooded systems under ocean/lake floor
 *   4. WormCarver         — tunnels, noodles, spaghetti passages (most common)
 *
 * ── Usage ─────────────────────────────────────────────────────────────────
 *
 * <pre>
 *   CaveCarver carver = new CaveCarver(worldSeed);
 *
 *   // Called once per chunk before querying voxels in that chunk:
 *   carver.prepareChunk(chunkX, chunkZ, surfaceHeightAtCentre, worldMaxY);
 *
 *   // Per-voxel query:
 *   CaveCell cell = carver.query(worldX, worldY, worldZ, surfaceY, isUnderwater);
 * </pre>
 *
 * Thread safety: CaveCarver is NOT thread-safe — chunk-loading threads should
 * each create their own instance. All sub-carver noise fields are read-only
 * after construction; only the worm and ravine chunk state is mutable.
 */
public class CaveCarver {

    // ── Constants ─────────────────────────────────────────────────────────

    /** World Y at which solid bedrock begins. No carving below this. */
    public static final int BEDROCK_Y = 3;

    /** Chunk width and depth in world units. */
    private static final int CHUNK_W = 16;
    private static final int CHUNK_D = 16;

    // ── Sub-carvers ───────────────────────────────────────────────────────

    private final CavernCarver       cavernCarver;
    private final WormCarver         wormCarver;
    private final RavineCarver       ravineCarver;
    private final UnderwaterCaveCarver underwaterCarver;

    private final long worldSeed;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Constructs the cave carver system. Inexpensive — actual generation
     * happens lazily per chunk via {@link #prepareChunk}.
     *
     * @param worldSeed The world seed. Must match the world generator seed.
     */
    public CaveCarver(long worldSeed) {
        this.worldSeed       = worldSeed;
        this.cavernCarver    = new CavernCarver(worldSeed);
        this.wormCarver      = new WormCarver(worldSeed);
        this.ravineCarver    = new RavineCarver(worldSeed);
        this.underwaterCarver = new UnderwaterCaveCarver(worldSeed);
    }

    // ── Chunk preparation ─────────────────────────────────────────────────

    /**
     * Prepares all chunk-local state (worm paths, ravine segments) for the
     * given chunk. Must be called before querying any voxel in this chunk.
     *
     * For chunk-based engines: call this at the start of chunk generation.
     * For voxel-streaming engines: call this whenever the active chunk changes.
     *
     * @param chunkX    Chunk grid X (= worldX / 16)
     * @param chunkZ    Chunk grid Z (= worldZ / 16)
     * @param surfaceY  Approximate terrain height at the chunk centre (world units)
     * @param worldMaxY Maximum Y extent of the world
     */
    public void prepareChunk(int chunkX, int chunkZ, int surfaceY, int worldMaxY) {
        wormCarver.generateForChunk(chunkX, chunkZ, worldSeed, surfaceY, CHUNK_W, CHUNK_D, worldMaxY);
        ravineCarver.generateForChunk(chunkX, chunkZ, worldSeed, surfaceY, CHUNK_W, CHUNK_D);
    }

    // ── Voxel query ───────────────────────────────────────────────────────

    /**
     * Determines whether a voxel is carved and what type of cave it is.
     *
     * @param x           World X coordinate
     * @param y           World Y coordinate (0 = bedrock level)
     * @param z           World Z coordinate
     * @param surfaceY    The terrain height directly above this voxel column
     * @param isUnderwater Whether the surface of this column is below sea level
     * @return A {@link CaveCell} describing the carved state of this voxel.
     */
    public CaveCell query(int x, int y, int z, int surfaceY, boolean isUnderwater) {

        // ── Absolute floor — bedrock is always solid ───────────────────────
        if (y <= BEDROCK_Y) return CaveCell.SOLID;

        // ── Don't carve above the surface ─────────────────────────────────
        if (y >= surfaceY) return CaveCell.SOLID;

        float depthFraction = 1.0f - ((float) y / (float) Math.max(1, surfaceY));

        // ── 1. Ravine (surface crack) ──────────────────────────────────────
        if (ravineCarver.isCarved(x, y, z)) {
            return new CaveCell(true, CaveType.RAVINE, depthFraction);
        }

        // ── 2. Large cavern (noise intersection) ───────────────────────────
        CaveType cavernType = cavernCarver.query(x, y, z, surfaceY, surfaceY);
        if (cavernType != CaveType.NONE) {
            return new CaveCell(true, cavernType, depthFraction);
        }

        // ── 3. Underwater cave ─────────────────────────────────────────────
        if (isUnderwater) {
            CaveType uwType = underwaterCarver.query(x, y, z, surfaceY, true);
            if (uwType != CaveType.NONE) {
                return new CaveCell(true, uwType, depthFraction);
            }
        }

        // ── 4. Worm tunnels (most common) ──────────────────────────────────
        if (wormCarver.isCarved(x, y, z)) {
            CaveType wormType = wormCarver.getType(x, y, z);
            return new CaveCell(true, wormType, depthFraction);
        }

        return CaveCell.SOLID;
    }

    /**
     * Convenience overload that accepts a {@link WorldCell} from the terrain
     * generator so the caller doesn't need to manually extract surface info.
     *
     * @param x         World X
     * @param y         World Y
     * @param z         World Z
     * @param surface   The WorldCell at (x, z) from WorldGenerator
     * @param surfaceY  The world-unit height corresponding to surface.elevation
     */
    public CaveCell query(int x, int y, int z, WorldCell surface, int surfaceY) {
        return query(x, y, z, surfaceY, surface.isWater);
    }
}
