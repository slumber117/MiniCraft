package minicraft.world.cave;

import minicraft.world.PerlinNoise;

/**
 * UnderwaterCaveCarver — carves cave systems beneath ocean and lake floors.
 *
 * These caves are always fully flooded. They connect to the ocean floor
 * via openings and extend laterally beneath the seabed, often creating
 * blue-holes, underwater arches, and drowned tunnels.
 *
 * Detection: this carver is only active when the surface directly above
 * the voxel is below sea level (i.e., the terrain is underwater terrain).
 *
 * The technique is the same as CavernCarver but with:
 *   – Different threshold (denser network, more passages)
 *   – A strong horizontal bias (caves extend sideways, not down)
 *   – Shallower depth limit (ocean caves don't go as deep as land caves)
 *   – No magma zone (deep ocean is cold, not volcanic — unless you want that)
 */
public class UnderwaterCaveCarver {

    private static final double NOISE_SCALE_H  = 0.028; // horizontal
    private static final double NOISE_SCALE_V  = 0.045; // vertical (tighter = flatter caves)
    private static final double THRESHOLD      = -0.12; // lower = less carved

    /** Only carve within this fraction below the ocean floor. */
    private static final float MAX_DEPTH_FRAC  = 0.50f;
    private static final float MIN_DEPTH_FRAC  = 0.05f;

    private final PerlinNoise noiseA;
    private final PerlinNoise noiseB;

    public UnderwaterCaveCarver(long seed) {
        noiseA = new PerlinNoise(seed + 6001L);
        noiseB = new PerlinNoise(seed + 6002L);
    }

    /**
     * Tests whether a voxel beneath an underwater surface is carved.
     *
     * @param x          World X
     * @param y          World Y
     * @param z          World Z
     * @param seaFloorY  The terrain height (ocean/lake floor) above this voxel
     * @param isSurface  Whether the voxel's column is actually underwater surface
     * @return CaveType.UNDERWATER or CaveType.NONE
     */
    public CaveType query(int x, int y, int z, int seaFloorY, boolean isSurface) {
        if (!isSurface) return CaveType.NONE;
        if (y >= seaFloorY) return CaveType.NONE; // above floor

        float depthFrac = 1.0f - ((float) y / (float) Math.max(1, seaFloorY));
        if (depthFrac < MIN_DEPTH_FRAC || depthFrac > MAX_DEPTH_FRAC) return CaveType.NONE;

        double depthEnv = Math.sin(
            Math.PI * (depthFrac - MIN_DEPTH_FRAC) / (MAX_DEPTH_FRAC - MIN_DEPTH_FRAC)
        );

        double a = noiseA.noise(x * NOISE_SCALE_H, y * NOISE_SCALE_V, z * NOISE_SCALE_H);
        double b = noiseB.noise(x * NOISE_SCALE_H + 7.3, y * NOISE_SCALE_V + 2.1, z * NOISE_SCALE_H + 14.6);

        double t = THRESHOLD + depthEnv * 0.15;

        if (a < t && b < t) return CaveType.UNDERWATER;
        return CaveType.NONE;
    }
}
