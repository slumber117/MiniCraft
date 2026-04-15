package minicraft.world.cave;

import minicraft.world.PerlinNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RavineCarver — carves deep vertical cracks (ravines) into the terrain.
 *
 * A ravine is essentially a very flat worm that walks horizontally while
 * carving a tall, thin vertical slice. The result looks like a tectonic
 * crack or canyon — narrow at the top, sometimes widening at the base,
 * always dramatically deep.
 *
 * Properties of a well-formed ravine:
 *   – Width: 2–8 blocks across at surface level
 *   – Depth: reaches 60–90 % of the way to bedrock
 *   – Length: 40–160 blocks horizontally
 *   – Shape: tapers — wider mid-depth, pinched at top and bottom
 *   – Direction: gently curves, never perfectly straight
 *
 * Ravines are sparse — roughly 1–2 per 64×64 chunk region.
 * They are much rarer than tunnels but visually dramatic.
 */
public class RavineCarver {

    // ── Tuning ────────────────────────────────────────────────────────────

    /** Probability a given chunk spawns a ravine (0–1). */
    private static final float RAVINE_CHANCE         = 0.06f;

    /** Horizontal half-width at the widest point of the ravine. */
    private static final float RAVINE_HALF_WIDTH      = 2.2f;

    /** Depth below surface at which the ravine bottom is placed. */
    private static final float RAVINE_MIN_DEPTH_FRAC  = 0.55f;
    private static final float RAVINE_MAX_DEPTH_FRAC  = 0.88f;

    /** Maximum number of horizontal steps a ravine walks. */
    private static final int   RAVINE_MAX_STEPS       = 120;

    /** How fast the ravine's horizontal direction turns. */
    private static final double RAVINE_TURN_SCALE     = 0.06;

    // ── State ─────────────────────────────────────────────────────────────

    /**
     * Ravine segments stored as float[5]: {cx, topY, cz, bottomY, halfWidth}.
     * Each segment is a vertical box centred at (cx, cz) stretching from bottomY to topY.
     */
    private final List<float[]> segments = new ArrayList<>();

    private final PerlinNoise dirNoise;

    // ── Constructor ───────────────────────────────────────────────────────

    public RavineCarver(long seed) {
        dirNoise = new PerlinNoise(seed + 9001L);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Generates ravines for a given chunk. Call once per chunk.
     *
     * @param chunkX    Chunk grid X
     * @param chunkZ    Chunk grid Z
     * @param worldSeed Base world seed
     * @param surfaceY  Approximate surface height at the chunk centre
     */
    public void generateForChunk(int chunkX, int chunkZ, long worldSeed, int surfaceY, int chunkW, int chunkD) {
        segments.clear();

        long chunkSeed = worldSeed ^ ((long) chunkX * 987654321L) ^ ((long) chunkZ * 123456789L) ^ 0xDEADBEEFL;
        Random rng = new Random(chunkSeed);

        if (rng.nextFloat() > RAVINE_CHANCE) return; // sparse

        int originX = chunkX * chunkW;
        int originZ = chunkZ * chunkD;

        int startX = originX + rng.nextInt(chunkW);
        int startZ = originZ + rng.nextInt(chunkD);

        // Bottom of the ravine (lower Y value = deeper)
        int bottomY = (int) (surfaceY * (RAVINE_MIN_DEPTH_FRAC + rng.nextFloat() * (RAVINE_MAX_DEPTH_FRAC - RAVINE_MIN_DEPTH_FRAC)));
        bottomY = Math.max(2, bottomY);

        // Initial walking direction
        double yaw = rng.nextDouble() * Math.PI * 2;
        double tOffset = rng.nextDouble() * 500.0;

        double cx = startX;
        double cz = startZ;

        for (int step = 0; step < RAVINE_MAX_STEPS; step++) {
            double t = tOffset + step * 0.08;

            // Gently curve the ravine
            double turn = dirNoise.noise(t * RAVINE_TURN_SCALE, 0.0, 0.0);
            yaw += turn * 0.25;

            cx += Math.cos(yaw);
            cz += Math.sin(yaw);

            // Width tapers toward ends — widest in the middle of the walk
            float progress = (float) step / RAVINE_MAX_STEPS;
            float taper = (float) Math.sin(progress * Math.PI); // 0→1→0
            float hw = RAVINE_HALF_WIDTH * taper + 0.8f;

            // Slight width variation from noise
            float hwVariance = (float) (dirNoise.noise(cx * 0.1, 0, cz * 0.1) * 0.8);
            hw = Math.max(0.6f, hw + hwVariance);

            // The ravine carves from bottomY up to just below the surface
            // leaving a thin cap so it doesn't punch all the way out (unless dramatic)
            int topY = surfaceY - 2;

            segments.add(new float[]{ (float) cx, topY, (float) cz, bottomY, hw });
        }
    }

    /**
     * Tests whether a voxel is inside a ravine.
     */
    public boolean isCarved(int x, int y, int z) {
        for (float[] seg : segments) {
            float cx  = seg[0];
            float topY   = seg[1];
            float cz  = seg[2];
            float botY   = seg[3];
            float hw  = seg[4];

            // Horizontal Bounding Box check (Optimization)
            if (Math.abs(x - cx) > hw || Math.abs(z - cz) > hw) continue;

            // Must be in the vertical band
            if (y < botY || y > topY) continue;

            // Horizontal distance test — elliptical cross-section
            float dx = x - cx;
            float dz = z - cz;

            if (dx * dx + dz * dz <= hw * hw) return true;
        }
        return false;
    }

    public CaveType getType() {
        return CaveType.RAVINE;
    }
}
