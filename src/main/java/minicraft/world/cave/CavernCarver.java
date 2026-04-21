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
 * ── Tuning changes vs original ──────────────────────────────────────────
 *
 * • NOISE_A_SCALE / NOISE_B_SCALE lowered → bigger individual chambers.
 * • THRESHOLD_A / THRESHOLD_B raised → more voxels pass the carve test
 * (larger, more frequent caverns).
 * • CAVERN_START_DEPTH lowered → caverns begin forming earlier
 * below the surface.
 * • depthGain coefficient raised (0.12 → 0.18) → the envelope gives a
 * stronger boost at peak depth, creating extra-large deep chambers.
 * • CAVERN_PEAK_DEPTH shifted downward → the richest zone is now deeper,
 * which helps geodes stay nestled in solid rock at shallower depths.
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

    /**
     * Primary noise scale — controls chamber size.
     * ↓ Smaller value = larger individual chambers.
     * Original: 0.045 / 0.065 → New: 0.032 / 0.048
     */
    private static final double NOISE_A_SCALE = 0.032; // was 0.045
    private static final double NOISE_B_SCALE = 0.048; // was 0.065

    /**
     * Carve threshold for each noise field.
     * Value in [-1, 1]. Intersection of both < threshold defines carved space.
     * ↑ Higher threshold = more carved, larger and more frequent caverns.
     * Original: -0.22 / -0.28 → New: -0.12 / -0.16
     */
    private static final double THRESHOLD_A = -0.12; // was -0.22
    private static final double THRESHOLD_B = -0.16; // was -0.28

    /**
     * Depth fraction at which caverns start appearing (below surface).
     * ↓ Lowered so caves begin forming closer to the surface.
     * Original: 0.15 → New: 0.10
     */
    private static final float CAVERN_START_DEPTH = 0.10f; // was 0.15

    /**
     * Depth fraction at which caverns are most common.
     * Shifted slightly deeper to leave shallow layers for geodes to stand out.
     * Original: 0.45 → New: 0.50
     */
    private static final float CAVERN_PEAK_DEPTH = 0.50f; // was 0.45

    /**
     * Depth fraction below which caverns are suppressed again (solid bedrock zone).
     */
    private static final float CAVERN_END_DEPTH = 0.98f;

    /** Below this depth fraction, caverns become MAGMA_CHAMBERs. */
    private static final float MAGMA_DEPTH = 0.85f;

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

        // Raised from 0.12 to 0.18 — the envelope now opens thresholds wider
        // at peak depth, yielding larger deep chambers.
        double threshA = THRESHOLD_A + depthGain * 0.18; // was 0.12
        double threshB = THRESHOLD_B + depthGain * 0.18; // was 0.12

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