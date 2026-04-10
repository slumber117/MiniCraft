package minicraft.world.cave;

import minicraft.world.PerlinNoise;

/**
 * CavernCarver — carves large open chambers using 3D noise intersection.
 *
 * Instead of walking a worm, this samples two independent 3D noise fields.
 * Where both fields are simultaneously below a threshold, the voxel is
 * carved. This produces irregular, interconnected chambers of varying size
 * rather than tubes — think Lechuguilla or Carlsbad Caverns rather than
 * a subway tunnel.
 *
 * A second pass identifies MAGMA_CHAMBER regions: caverns found below a
 * critical depth threshold get reclassified so downstream systems can
 * fill them with lava and hostile content.
 *
 * GROTTO detection: isolated small cavern pockets near the surface are
 * tagged separately so decorators can place water, moss, and light shafts.
 *
 * ── How it works ────────────────────────────────────────────────────────
 *
 * Carve if:
 * noiseA(x, y, z) < THRESHOLD_A
 * AND
 * noiseB(x, y, z) < THRESHOLD_B
 *
 * The two noise fields are at different frequencies so their intersection
 * produces chambers of varying scale — big open halls where the overlap
 * is large, narrow connectors where it's small.
 *
 * An additional vertical squeeze is applied: the thresholds shrink near
 * the surface (fewer caverns poking out) and widen at mid-depth (most
 * caverns), then shrink again near bedrock (compressed rock).
 */
public class CavernCarver {

    // ── Tuning ────────────────────────────────────────────────────────────

    /** Primary noise scale — controls chamber size. Smaller = bigger chambers. */
    private static final double NOISE_A_SCALE = 0.022;

    /** Secondary noise scale — slightly higher freq for irregular walls. */
    private static final double NOISE_B_SCALE = 0.034;

    /**
     * Carve threshold for each noise field.
     * Value in [-1, 1]. Intersection of both < threshold defines carved space.
     * Higher threshold = more carved, larger caverns.
     */
    private static final double THRESHOLD_A = -0.10;
    private static final double THRESHOLD_B = -0.15;

    /** Depth fraction at which caverns start appearing (below surface). */
    private static final float CAVERN_START_DEPTH = 0.15f;

    /** Depth fraction at which caverns are most common. */
    private static final float CAVERN_PEAK_DEPTH = 0.45f;

    /**
     * Depth fraction below which caverns are suppressed again (solid bedrock zone).
     */
    private static final float CAVERN_END_DEPTH = 0.88f;

    /** Below this depth fraction, caverns become MAGMA_CHAMBERs. */
    private static final float MAGMA_DEPTH = 0.72f;

    /** Above this depth fraction and small, caverns become GROTTOs. */
    private static final float GROTTO_MAX_DEPTH = 0.25f;

    // ── Noise fields ──────────────────────────────────────────────────────

    private final PerlinNoise noiseA;
    private final PerlinNoise noiseB;

    /** Domain warp to make cavern walls less blob-like. */
    private final PerlinNoise warpNoise;

    // ── Constructor ───────────────────────────────────────────────────────

    public CavernCarver(long seed) {
        noiseA = new PerlinNoise(seed + 8001L);
        noiseB = new PerlinNoise(seed + 8002L);
        warpNoise = new PerlinNoise(seed + 8003L);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Tests whether a voxel is carved by the cavern system.
     *
     * @param x         World X
     * @param y         World Y (0 = bedrock, surfaceY = ground level)
     * @param z         World Z
     * @param surfaceY  The terrain height directly above this voxel, in world units
     * @param worldMaxY Maximum world height in world units
     * @return CaveType indicating what kind of cavern this is, or NONE if solid.
     */
    public CaveType query(int x, int y, int z, int surfaceY, int worldMaxY) {

        // ── 1. Depth fraction ──────────────────────────────────────────────
        float depthFraction = 1.0f - ((float) y / (float) Math.max(1, surfaceY));
        depthFraction = Math.max(0f, Math.min(1f, depthFraction));

        // ── 2. Early-out: outside the carving band ─────────────────────────
        if (depthFraction < CAVERN_START_DEPTH || depthFraction > CAVERN_END_DEPTH) {
            return CaveType.NONE;
        }

        // ── 3. Depth-based threshold modifier ─────────────────────────────
        // Peaks at CAVERN_PEAK_DEPTH, falls off toward start and end.
        double depthGain = depthEnvelope(depthFraction);

        double threshA = THRESHOLD_A + depthGain * 0.20;
        double threshB = THRESHOLD_B + depthGain * 0.20;

        // ── 4. Domain warp ─────────────────────────────────────────────────
        double warpScale = 0.015;
        double warpStrength = 3.5;
        double wx = x + warpNoise.noise(x * warpScale, y * warpScale, z * warpScale) * warpStrength;
        double wy = y + warpNoise.noise(x * warpScale + 11.3, y * warpScale + 5.7, z * warpScale + 2.1) * warpStrength;
        double wz = z
                + warpNoise.noise(x * warpScale + 23.8, y * warpScale + 17.4, z * warpScale + 31.6) * warpStrength;

        // ── 5. Sample noise ────────────────────────────────────────────────
        double a = noiseA.noise(wx * NOISE_A_SCALE, wy * NOISE_A_SCALE * 0.7, wz * NOISE_A_SCALE);
        double b = noiseB.noise(wx * NOISE_B_SCALE, wy * NOISE_B_SCALE * 0.7, wz * NOISE_B_SCALE);

        // ── 6. Intersection test ───────────────────────────────────────────
        if (a > threshA || b > threshB) {
            return CaveType.NONE;
        }

        // ── 7. Classify carved voxel ───────────────────────────────────────
        if (depthFraction >= MAGMA_DEPTH) {
            return CaveType.MAGMA_CHAMBER;
        }
        if (depthFraction <= GROTTO_MAX_DEPTH) {
            return CaveType.GROTTO;
        }
        return CaveType.CAVERN;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Smooth envelope that peaks at 1.0 at CAVERN_PEAK_DEPTH and falls to
     * 0.0 at CAVERN_START_DEPTH and CAVERN_END_DEPTH.
     */
    private static double depthEnvelope(float depthFraction) {
        if (depthFraction <= CAVERN_START_DEPTH)
            return 0.0;
        if (depthFraction >= CAVERN_END_DEPTH)
            return 0.0;

        // Remap to [0, 1] within the carving band
        double t = (depthFraction - CAVERN_START_DEPTH) / (CAVERN_END_DEPTH - CAVERN_START_DEPTH);

        // Tent function: ramps up to peak then back down
        double peak = (CAVERN_PEAK_DEPTH - CAVERN_START_DEPTH) / (CAVERN_END_DEPTH - CAVERN_START_DEPTH);
        double envelope;
        if (t <= peak) {
            envelope = t / peak;
        } else {
            envelope = 1.0 - (t - peak) / (1.0 - peak);
        }

        // Smooth it
        return envelope * envelope * (3.0 - 2.0 * envelope);
    }
}