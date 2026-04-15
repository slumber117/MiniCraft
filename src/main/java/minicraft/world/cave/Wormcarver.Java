package minicraft.world.cave;

import minicraft.world.PerlinNoise;

import java.util.Random;

/**
 * WormCarver — generates sinuous cave tunnels using the "drunken worm" algorithm.
 *
 * A worm is a 3D walker that starts at a seed point and takes steps guided
 * by noise-driven direction vectors. At each step it carves a sphere of
 * configurable radius. The result is a smooth, branching, organically curved
 * tunnel that looks nothing like a box or a straight shaft.
 *
 * Multiple worm families are used:
 *   – Standard worms  (TUNNEL)   : mid-radius, mid-frequency direction noise
 *   – Noodle worms    (NOODLE)   : thin, high-frequency turns, very branchy
 *   – Spaghetti worms (SPAGHETTI): longer, looser curves that reconnect
 *
 * Worms are seeded deterministically from chunk coordinates so the same
 * world always produces the same caves, and adjacent chunks connect correctly.
 *
 * ── How to query ─────────────────────────────────────────────────────────
 *
 * Rather than pre-carving a volume, this class builds a set of carved spheres
 * per-chunk and exposes an isCarved(x, y, z) test. This keeps memory flat —
 * only the sphere list is stored, not every voxel.
 */
public class WormCarver {

    // ── Tuning ────────────────────────────────────────────────────────────

    /** How many worm seeds to attempt per 16×16 chunk column. */
    private static final int TUNNEL_WORMS_PER_CHUNK   = 4;
    private static final int NOODLE_WORMS_PER_CHUNK   = 8;
    private static final int SPAGHETTI_WORMS_PER_CHUNK = 2;

    /** Maximum steps a worm walks before dying. */
    private static final int TUNNEL_MAX_STEPS    = 140;
    private static final int NOODLE_MAX_STEPS    = 100;
    private static final int SPAGHETTI_MAX_STEPS = 220;

    /** Base radius of the carved sphere at each worm step. */
    private static final float TUNNEL_BASE_RADIUS    = 2.1f;
    private static final float NOODLE_BASE_RADIUS    = 1.0f;
    private static final float SPAGHETTI_BASE_RADIUS = 1.6f;

    /** How much the radius varies along the worm's length. */
    private static final float RADIUS_VARIANCE = 0.6f;

    /** Depth band: worms only live between these world-Y fractions [0=surface, 1=bedrock]. */
    private static final float TUNNEL_MIN_DEPTH    = 0.10f;
    private static final float TUNNEL_MAX_DEPTH    = 0.75f;
    private static final float NOODLE_MIN_DEPTH    = 0.05f;
    private static final float NOODLE_MAX_DEPTH    = 0.45f;
    private static final float SPAGHETTI_MIN_DEPTH = 0.20f;
    private static final float SPAGHETTI_MAX_DEPTH = 0.80f;

    /** Direction noise scale — larger = slower turns (more sinuous worms). */
    private static final double TUNNEL_DIR_SCALE    = 0.08;
    private static final double NOODLE_DIR_SCALE    = 0.18;
    private static final double SPAGHETTI_DIR_SCALE = 0.05;

    // ── State ─────────────────────────────────────────────────────────────

    /**
     * Voxel occupancy map for the current chunk.
     * Index = (lx * 400 * 16) + (ly * 16) + lz
     */
    private final CaveType[] occupancyMap = new CaveType[16 * 400 * 16];

    /** Noise used to steer worm direction. */
    private final PerlinNoise directionNoise;

    /** Noise used to modulate worm radius along its length. */
    private final PerlinNoise radiusNoise;

    // ── Constructor ───────────────────────────────────────────────────────

    public WormCarver(long seed) {
        directionNoise = new PerlinNoise(seed + 7001L);
        radiusNoise    = new PerlinNoise(seed + 7002L);
        // Initialize occupancy map
        java.util.Arrays.fill(occupancyMap, CaveType.NONE);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Generates all worms for a given chunk and caches their carved spheres.
     * Must be called once per chunk before querying isCarved().
     *
     * @param chunkX    Chunk grid X (world X / chunkWidth)
     * @param chunkZ    Chunk grid Z (world Z / chunkDepth)
     * @param worldSeed Base world seed — combined with chunk coords to derive per-chunk seeds
     * @param maxY      The surface height in world units at the chunk centre (used to scale depth)
     * @param chunkW    Chunk width  in world units (typically 16)
     * @param chunkD    Chunk depth  in world units (typically 16)
     * @param worldMaxY Maximum possible world height in world units
     */
    public void generateForChunk(int chunkX, int chunkZ, long worldSeed,
                                 int maxY, int chunkW, int chunkD, int worldMaxY) {
        java.util.Arrays.fill(occupancyMap, CaveType.NONE);

        // Deterministic per-chunk seed
        long chunkSeed = worldSeed ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L);
        Random rng = new Random(chunkSeed);

        int originX = chunkX * chunkW;
        int originZ = chunkZ * chunkD;

        // ── Spawn tunnel worms ─────────────────────────────────────────────
        for (int i = 0; i < TUNNEL_WORMS_PER_CHUNK; i++) {
            int startX = originX + rng.nextInt(chunkW);
            int startZ = originZ + rng.nextInt(chunkD);
            int minY   = (int) (maxY * TUNNEL_MIN_DEPTH);
            int maxYW  = (int) (maxY * TUNNEL_MAX_DEPTH);
            int startY = minY + rng.nextInt(Math.max(1, maxYW - minY));
            walkWorm(startX, startY, startZ, TUNNEL_MAX_STEPS,
                     TUNNEL_BASE_RADIUS, TUNNEL_DIR_SCALE, CaveType.TUNNEL, rng, worldMaxY);
        }

        // ── Spawn noodle worms ────────────────────────────────────────────
        for (int i = 0; i < NOODLE_WORMS_PER_CHUNK; i++) {
            int startX = originX + rng.nextInt(chunkW);
            int startZ = originZ + rng.nextInt(chunkD);
            int minY   = (int) (maxY * NOODLE_MIN_DEPTH);
            int maxYW  = (int) (maxY * NOODLE_MAX_DEPTH);
            int startY = minY + rng.nextInt(Math.max(1, maxYW - minY));
            walkWorm(startX, startY, startZ, NOODLE_MAX_STEPS,
                     NOODLE_BASE_RADIUS, NOODLE_DIR_SCALE, CaveType.NOODLE, rng, worldMaxY);
        }

        // ── Spawn spaghetti worms ─────────────────────────────────────────
        for (int i = 0; i < SPAGHETTI_WORMS_PER_CHUNK; i++) {
            int startX = originX + rng.nextInt(chunkW);
            int startZ = originZ + rng.nextInt(chunkD);
            int minY   = (int) (maxY * SPAGHETTI_MIN_DEPTH);
            int maxYW  = (int) (maxY * SPAGHETTI_MAX_DEPTH);
            int startY = minY + rng.nextInt(Math.max(1, maxYW - minY));
            walkWorm(startX, startY, startZ, SPAGHETTI_MAX_STEPS,
                     SPAGHETTI_BASE_RADIUS, SPAGHETTI_DIR_SCALE, CaveType.SPAGHETTI, rng, worldMaxY);
        }
    }

    /**
     * Returns true if the voxel at (x, y, z) has been carved by any worm.
     * Call generateForChunk() for the relevant chunk first.
     */
    public boolean isCarved(int x, int y, int z) {
        int lx = x & 15;
        int lz = z & 15;
        if (y < 0 || y >= 400) return false;
        return occupancyMap[(lx * 400 * 16) + (y * 16) + lz] != CaveType.NONE;
    }

    /**
     * Returns the CaveType of the first sphere that contains (x, y, z),
     * or CaveType.NONE if the voxel is solid.
     */
    public CaveType getType(int x, int y, int z) {
        int lx = x & 15;
        int lz = z & 15;
        if (y < 0 || y >= 400) return CaveType.NONE;
        return occupancyMap[(lx * 400 * 16) + (y * 16) + lz];
    }

    // ── Worm walker ───────────────────────────────────────────────────────

    /**
     * Walks a single worm from its start position for up to maxSteps steps,
     * carving a sphere at each step. Direction is driven by direction noise
     * sampled at the current position scaled by dirScale.
     */
    private void walkWorm(int startX, int startY, int startZ,
                          int maxSteps, float baseRadius, double dirScale,
                          CaveType type, Random rng, int worldMaxY) {

        double px = startX;
        double py = startY;
        double pz = startZ;

        // Initial direction — randomised
        double yaw   = rng.nextDouble() * Math.PI * 2;
        double pitch = (rng.nextDouble() - 0.5) * Math.PI * 0.3;

        // A noise-based "time" offset so each worm samples different noise regions
        double tOffset = rng.nextDouble() * 1000.0;

        for (int step = 0; step < maxSteps; step++) {
            double t = tOffset + step * 0.1;

            // Sample direction noise — each axis uses a different noise slice
            double noiseYaw   = directionNoise.noise(t * dirScale, 0.0, 0.0);
            double noisePitch = directionNoise.noise(0.0, t * dirScale + 50.0, 0.0);

            // Smoothly bend the direction
            yaw   += noiseYaw   * 0.4;
            pitch += noisePitch * 0.2;
            // Keep pitch from going fully vertical — caves are mostly horizontal
            pitch = Math.max(-0.6, Math.min(0.6, pitch));

            // Convert direction to a step vector
            double cosP = Math.cos(pitch);
            double dx   = Math.cos(yaw) * cosP;
            double dy   = Math.sin(pitch);
            double dz   = Math.sin(yaw) * cosP;

            px += dx;
            py += dy;
            pz += dz;

            // Kill the worm if it escapes the world vertically
            if (py < 1 || py > worldMaxY - 1) break;

            // Modulate radius — breathes wider and narrower naturally
            double radiusMod = radiusNoise.noise(px * 0.05, py * 0.05, pz * 0.05);
            float radius = baseRadius + (float) (radiusMod * RADIUS_VARIANCE);
            radius = Math.max(0.5f, radius);

            // Carve into the map immediately
            carveSphere((float) px, (float) py, (float) pz, radius, type, startX & ~15, startZ & ~15);
        }
    }

    private void carveSphere(float worldX, float worldY, float worldZ, float radius, CaveType type, int chunkStartX, int chunkStartZ) {
        int minX = Math.max(chunkStartX, (int) (worldX - radius));
        int maxX = Math.min(chunkStartX + 15, (int) (worldX + radius));
        int minY = Math.max(0, (int) (worldY - radius));
        int maxY = Math.min(399, (int) (worldY + radius));
        int minZ = Math.max(chunkStartZ, (int) (worldZ - radius));
        int maxZ = Math.min(chunkStartZ + 15, (int) (worldZ + radius));

        float r2 = radius * radius;

        for (int x = minX; x <= maxX; x++) {
            float dx = x - worldX;
            for (int z = minZ; z <= maxZ; z++) {
                float dz = z - worldZ;
                float dxdz2 = dx * dx + dz * dz;
                if (dxdz2 > r2) continue;
                
                int lx = x - chunkStartX;
                int lz = z - chunkStartZ;
                
                for (int y = minY; y <= maxY; y++) {
                    float dy = y - worldY;
                    if (dxdz2 + dy * dy <= r2) {
                        occupancyMap[(lx * 400 * 16) + (y * 16) + lz] = type;
                    }
                }
            }
        }
    }
}