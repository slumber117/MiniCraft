package minicraft.world.cave.geode;

import minicraft.world.PerlinNoise;

public class GemGeode {
    // ── Tuning ─────────────────────────────────────────────────────────────

    /**
     * Minimum normalised depth at which any geode can spawn.
     * 0 = surface, 1 = bedrock. Matches GemType.minDepthFraction lower bound.
     */
    private static final float GEODE_MIN_DEPTH_FRACTION = 0.35f;

    /**
     * Probability [0, 1] that a given chunk attempts to place a geode at all.
     * Tune this to control overall geode frequency.
     * Geodes should be noticeably rarer than caves.
     */
    private static final double CHUNK_GEODE_CHANCE = 0.04; // ~1 in 25 chunks attempts

    /**
     * Minimum block distance between any two geode centres.
     * Prevents geodes from overlapping even across different chunk attempts.
     */
    private static final int MIN_SEPARATION_BLOCKS = 48;

    /**
     * Grid cell size (in blocks) used for the separation hash-grid.
     * Must be >= MIN_SEPARATION_BLOCKS to guarantee the invariant.
     */
    private static final int GRID_CELL_SIZE = 64;

    // ── Geode geometry ─────────────────────────────────────────────────────

    /** Outer radius of the geode shell (voxels). */
    private static final float OUTER_RADIUS = 5.5f;

    /** Inner radius of the hollow air pocket (voxels). */
    private static final float INNER_RADIUS = 3.8f;

    /**
     * Crystal growth depth: how far (in voxels) crystal nodes protrude
     * inward from the shell into the hollow.
     */
    private static final float CRYSTAL_DEPTH = 1.4f;

    /** Domain warp strength applied to the distance field (voxels). */
    private static final float WARP_STRENGTH = 1.8f;

    /** Scale of the domain warp noise. */
    private static final double WARP_SCALE = 0.09;

    /**
     * Fraction of shell-facing voxels that grow crystal nodes.
     * 1.0 = every wall voxel is a crystal; 0.0 = no crystals.
     * A value around 0.55 gives an attractive, not-too-dense coverage.
     */
    private static final double CRYSTAL_COVERAGE_THRESHOLD = 0.0; // noise > this → crystal

    // ── Noise ──────────────────────────────────────────────────────────────

    /** Domain warp fields (X/Y/Z offsets). */
    private final PerlinNoise warpX;
    private final PerlinNoise warpY;
    private final PerlinNoise warpZ;

    /** Mask noise: controls which shell-adjacent voxels grow crystals. */
    private final PerlinNoise crystalMask;

    private final long worldSeed;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * @param worldSeed  Must match the world generator seed for consistent placement.
     */
    public GemGeode(long worldSeed) {
        this.worldSeed  = worldSeed;
        this.warpX      = new PerlinNoise(worldSeed + 9101L);
        this.warpY      = new PerlinNoise(worldSeed + 9102L);
        this.warpZ      = new PerlinNoise(worldSeed + 9103L);
        this.crystalMask = new PerlinNoise(worldSeed + 9104L);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Queries the geode state of a single voxel.
     *
     * The method inspects every geode candidate that could possibly overlap
     * with (x, y, z), which is at most a small fixed set of nearby grid cells.
     * Performance is O(1) per voxel.
     *
     * @param x            World X
     * @param y            World Y (0 = bedrock level)
     * @param z            World Z
     * @param surfaceY     Terrain height directly above this voxel column
     * @param isUnderwater Whether this column's surface is below sea level
     * @return A {@link GeodeCell} describing this voxel's geode state.
     *         Returns {@link GeodeCell#OUTSIDE} if not part of any geode.
     */
    public GeodeCell query(int x, int y, int z, int surfaceY, boolean isUnderwater) {

        // Depth fraction — used for both the global depth gate and gem selection
        float depthFraction = computeDepthFraction(y, surfaceY);

        // Global depth gate: never place geodes near the surface
        if (depthFraction < GEODE_MIN_DEPTH_FRACTION) {
            return GeodeCell.OUTSIDE;
        }

        // Search nearby grid cells for geode centres that could reach this voxel
        int searchRadius = (int) Math.ceil((OUTER_RADIUS + WARP_STRENGTH) / GRID_CELL_SIZE) + 1;

        int cellX = Math.floorDiv(x, GRID_CELL_SIZE);
        int cellY = Math.floorDiv(y, GRID_CELL_SIZE);
        int cellZ = Math.floorDiv(z, GRID_CELL_SIZE);

        GeodeCell nearest = GeodeCell.OUTSIDE;
        float nearestDist = Float.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    int gcx = cellX + dx;
                    int gcy = cellY + dy;
                    int gcz = cellZ + dz;

                    GeodeCandidate candidate = getCandidate(gcx, gcy, gcz, surfaceY);
                    if (candidate == null)
                        continue;

                    GeodeCell cell = testVoxel(x, y, z, candidate, depthFraction);
                    if (cell.layer != GeodeCell.Layer.OUTSIDE) {
                        // Prefer the innermost layer when two geodes overlap (rare edge case)
                        float d = distToCenter(x, y, z, candidate);
                        if (d < nearestDist) {
                            nearestDist = d;
                            nearest = cell;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    // ── Internal: candidate lookup ─────────────────────────────────────────

    /**
     * Determines whether a grid cell contains a geode and, if so, returns its
     * centre and gem type. Returns null if this grid cell has no geode.
     *
     * All decisions are derived deterministically from the seed so results are
     * consistent regardless of the query order or thread.
     */
    private GeodeCandidate getCandidate(int gcx, int gcy, int gcz, int surfaceY) {
        long cellSeed = cellHash(gcx, gcy, gcz);
        java.util.Random rng = new java.util.Random(cellSeed);

        // Reject with high probability to keep geodes rare
        if (rng.nextDouble() >= CHUNK_GEODE_CHANCE)
            return null;

        // Place centre somewhere inside this grid cell
        int cx = gcx * GRID_CELL_SIZE + rng.nextInt(GRID_CELL_SIZE);
        int cy = gcy * GRID_CELL_SIZE + rng.nextInt(GRID_CELL_SIZE);
        int cz = gcz * GRID_CELL_SIZE + rng.nextInt(GRID_CELL_SIZE);

        // Depth check on the candidate centre
        float depthFraction = computeDepthFraction(cy, surfaceY);
        if (depthFraction < GEODE_MIN_DEPTH_FRACTION)
            return null;

        // Pick gem type by weighted random, filtered to depth-eligible gems
        GemType gem = pickGemType(rng, depthFraction);
        if (gem == null)
            return null;

        return new GeodeCandidate(cx, cy, cz, gem, depthFraction);
    }

    // ── Internal: voxel test ───────────────────────────────────────────────

    /**
     * Given a confirmed geode candidate, determines what layer (if any) the
     * voxel (x, y, z) falls into.
     */
    private GeodeCell testVoxel(int x, int y, int z,
            GeodeCandidate c, float depthFraction) {

        // Offset from geode centre (float for warp)
        double ox = x - c.cx;
        double oy = y - c.cy;
        double oz = z - c.cz;

        // Domain warp: nudge the sampling point so the shell is lumpy
        double wx = ox + warpX.noise(ox * WARP_SCALE, oy * WARP_SCALE, oz * WARP_SCALE) * WARP_STRENGTH;
        double wy = oy
                + warpY.noise(ox * WARP_SCALE + 7.3, oy * WARP_SCALE + 3.1, oz * WARP_SCALE + 5.9) * WARP_STRENGTH;
        double wz = oz
                + warpZ.noise(ox * WARP_SCALE + 13.7, oy * WARP_SCALE + 19.2, oz * WARP_SCALE + 2.4) * WARP_STRENGTH;

        double dist = Math.sqrt(wx * wx + wy * wy + wz * wz);

        // ── Outside the geode entirely ─────────────────────────────────────
        if (dist > OUTER_RADIUS)
            return GeodeCell.OUTSIDE;

        // ── Shell layer ────────────────────────────────────────────────────
        if (dist >= INNER_RADIUS) {
            return new GeodeCell(GeodeCell.Layer.SHELL, null, depthFraction);
        }

        // ── Crystal layer: inner face of the shell ─────────────────────────
        // Voxels just inside INNER_RADIUS can grow crystals if the mask allows.
        if (dist >= INNER_RADIUS - CRYSTAL_DEPTH) {
            // Crystal mask noise: high-frequency, anisotropic
            double maskScale = 0.22;
            double mask = crystalMask.noise(
                    x * maskScale, y * maskScale * 1.4, z * maskScale);
            if (mask > CRYSTAL_COVERAGE_THRESHOLD) {
                return new GeodeCell(GeodeCell.Layer.CRYSTAL, c.gem, depthFraction);
            }
            // Failed mask → part of the hollow
        }

        // ── Hollow interior ────────────────────────────────────────────────
        return new GeodeCell(GeodeCell.Layer.HOLLOW, null, depthFraction);
    }

    // ── Internal: gem selection ────────────────────────────────────────────

    /**
     * Picks a gem type by weighted random from among those eligible at the
     * given depth. Returns null only if no gem is eligible (shouldn't happen
     * below GEODE_MIN_DEPTH_FRACTION given current gem definitions).
     */
    private static GemType pickGemType(java.util.Random rng, float depthFraction) {
        // Compute effective weights, zeroing out ineligible gems
        int totalWeight = 0;
        int[] weights = new int[GemType.values().length];
        for (int i = 0; i < GemType.values().length; i++) {
            GemType g = GemType.values()[i];
            float mult = g.depthMultiplier(depthFraction);
            if (mult > 0f) {
                int w = Math.max(1, Math.round(g.rarityWeight * mult));
                weights[i] = w;
                totalWeight += w;
            }
        }
        if (totalWeight == 0)
            return null;

        int roll = rng.nextInt(totalWeight);
        int cum = 0;
        for (int i = 0; i < weights.length; i++) {
            cum += weights[i];
            if (roll < cum)
                return GemType.values()[i];
        }
        return GemType.values()[0]; // fallback, unreachable
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static float computeDepthFraction(int y, int surfaceY) {
        float df = 1.0f - ((float) y / (float) Math.max(1, surfaceY));
        return Math.max(0f, Math.min(1f, df));
    }

    private static float distToCenter(int x, int y, int z, GeodeCandidate c) {
        float dx = x - c.cx, dy = y - c.cy, dz = z - c.cz;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Deterministic hash for a 3-D grid cell coordinate pair.
     * Uses a series of large primes to spread the bit pattern.
     */
    private long cellHash(int gcx, int gcy, int gcz) {
        long h = worldSeed;
        h ^= (long) gcx * 0x9E3779B97F4A7C15L;
        h ^= (long) gcy * 0x6C62272E07BB0142L;
        h ^= (long) gcz * 0xD2A98B26625EEE7BL;
        // Finalise with a few mixing steps (similar to MurmurHash3 finaliser)
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    // ── Inner data holder ──────────────────────────────────────────────────

    /** Lightweight struct holding a confirmed geode centre. */
    private static final class GeodeCandidate {
        final int cx, cy, cz;
        final GemType gem;
        final float depthFraction;

        GeodeCandidate(int cx, int cy, int cz, GemType gem, float depthFraction) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.gem = gem;
            this.depthFraction = depthFraction;
        }
    }
}
