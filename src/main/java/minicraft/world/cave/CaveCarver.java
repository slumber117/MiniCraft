package minicraft.world.cave;

import minicraft.world.CaveGenerator;
import minicraft.world.WorldCell;
import minicraft.world.cave.geode.*;

/**
 * CaveCarver — master coordinator for all underground carving systems.
 *
 * This is the single class your chunk builder talks to. It holds one instance
 * of each sub-carver and routes queries through all of them, returning the
 * final CaveCell for any voxel.
 *
 * ── Priority order (first match wins) ────────────────────────────────────
 *
 *   1. GemGeode           — constructive crystal structures
 *   2. CaveGenerator      — 3D noise (Tunnels, Ravines, Caverns)
 *   3. UnderwaterCaveCarver — flooded systems under ocean/lake floor
 *
 */
public class CaveCarver {

    // ── Constants ─────────────────────────────────────────────────────────

    /** World Y at which solid bedrock begins. No carving below this. */
    public static final int BEDROCK_Y = 3;

    /** Chunk width and depth in world units. */
    private static final int CHUNK_W = 16;
    private static final int CHUNK_D = 16;

    // ── Sub-carvers ───────────────────────────────────────────────────────

    private final CaveGenerator      caveGen;
    private final UnderwaterCaveCarver underwaterCarver;
    private final GemGeode           gemGeode;

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
        this.caveGen         = new CaveGenerator(worldSeed);
        this.underwaterCarver = new UnderwaterCaveCarver(worldSeed);
        this.gemGeode        = new GemGeode(worldSeed);
    }

    // ── Chunk preparation ─────────────────────────────────────────────────

    /**
     * Prepares all chunk-local state for the given chunk. 
     *
     * @param chunkX    Chunk grid X (= worldX / 16)
     * @param chunkZ    Chunk grid Z (= worldZ / 16)
     * @param surfaceY  Approximate terrain height at the chunk centre (world units)
     * @param worldMaxY Maximum Y extent of the world
     */
    public void prepareChunk(int chunkX, int chunkZ, int surfaceY, int worldMaxY) {
        // CaveGenerator is query-based; no per-chunk preparation needed
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

        // ── 1. Gem Geodes (Constructive) ───────────────────────────────────
        GeodeCell geode = gemGeode.query(x, y, z, surfaceY, isUnderwater);
        if (geode.layer != GeodeCell.Layer.OUTSIDE) {
            if (geode.layer == GeodeCell.Layer.SHELL)
                return new CaveCell(false, CaveType.GEODE_SHELL, geode.depth);
            if (geode.layer == GeodeCell.Layer.CRYSTAL)
                return new CaveCell(false, CaveType.GEODE_CRYSTAL, geode.depth, geode.gemType);
            if (geode.layer == GeodeCell.Layer.HOLLOW)
                return new CaveCell(true, CaveType.GEODE_HOLLOW, geode.depth);
        }

        // ── 2. Master Cave Generator (3D Noise) ────────────────────────────
        CaveType type = caveGen.getCaveType(x, y, z, surfaceY);
        if (type != CaveType.NONE) {
            return new CaveCell(true, type, depthFraction);
        }

        // ── 3. Underwater cave ─────────────────────────────────────────────
        if (isUnderwater) {
            CaveType uwType = underwaterCarver.query(x, y, z, surfaceY, true);
            if (uwType != CaveType.NONE) {
                return new CaveCell(true, uwType, depthFraction);
            }
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
