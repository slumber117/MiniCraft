package minicraft.world;

import minicraft.world.cave.CaveType;
import java.util.*;

/**
 * Advanced Physics-Motivated Cave Generator.
 * 
 * Implements biome-conditioned topology planning using Poisson-disk seed points,
 * Relative Neighbourhood Graph (RNG) connections, and Signed Distance Function (SDF) 
 * carving with smooth-union (smin) blending for organic chambers.
 */
public class CaveGenerator {

    private final PerlinNoise noiseGen;
    private final long seed;

    public CaveGenerator(long seed) {
        this.seed = seed;
        this.noiseGen = new PerlinNoise(seed);
    }

    /**
     * Determines the cave type at a specific voxel using topology-aware SDF carving.
     */
    public CaveType getCaveType(int x, int y, int z, float surfaceY, Biome biome) {
        // 1. Safety constraints
        if (y > surfaceY - 10 || y <= 3) return CaveType.NONE;

        // 2. Select Specialized Cave Logic based on Biome
        CaveCategory cat = getCategory(biome);
        
        // 3. Topology Query: Find nearby seed points and their connections
        // We look at the current chunk and its neighbors (3x3 grid)
        int cx = x >> 4;
        int cz = z >> 4;
        
        double minDist = 1e9;
        CaveType resultType = CaveType.NONE;

        // Iterating over a 3x3 chunk neighborhood to find all relevant cave segments
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                List<CaveSeed> seeds = getSeedsForChunk(cx + dx, cz + dz);
                for (CaveSeed s1 : seeds) {
                    // Check distance to the seed point itself (forming a chamber/node)
                    double dNode = distSq(x, y, z, s1.x, s1.y, s1.z);
                    double nodeRadius = s1.radius * (0.8 + 0.4 * noiseGen.noise(x * 0.1, y * 0.1, z * 0.1));
                    
                    minDist = smin(minDist, Math.sqrt(dNode) - nodeRadius, 4.0);

                    // Check connections (RNG)
                    for (CaveSeed s2 : s1.connections) {
                        double dSeg = distToSegment(x, y, z, s1.x, s1.y, s1.z, s2.x, s2.y, s2.z);
                        
                        // Modulate passage radius based on biome category
                        double passageRadius = cat.baseRadius * (0.7 + 0.6 * noiseGen.noise(x * 0.05, y * 0.05, z * 0.05));
                        if (cat == CaveCategory.FRACTURE) {
                            // Fractures are jagged and sharp (angular SDF)
                            passageRadius *= (0.5 + 0.5 * Math.abs(noiseGen.noise(x * 0.2, y * 0.2, z * 0.2)));
                        }

                        minDist = smin(minDist, dSeg - passageRadius, cat.smoothness);
                    }
                }
            }
        }

        // 4. Final Carving Decision
        if (minDist < 0) {
            return cat.type;
        }

        return CaveType.NONE;
    }

    // ── Topology Planning ──────────────────────────────────────────────────

    private static class CaveSeed {
        double x, y, z;
        double radius;
        List<CaveSeed> connections = new ArrayList<>();
        
        CaveSeed(double x, double y, double z, double r) {
            this.x = x; this.y = y; this.z = z; this.radius = r;
        }
    }

    /**
     * Deterministically generates 1-2 seed points per chunk.
     */
    private List<CaveSeed> getSeedsForChunk(int cx, int cz) {
        long cSeed = seed ^ ((long) cx << 32) ^ cz;
        Random rand = new Random(cSeed);
        List<CaveSeed> seeds = new ArrayList<>();
        
        // Poisson-disk approximation: jittered grid points
        int numPoints = 1 + rand.nextInt(2);
        for (int i = 0; i < numPoints; i++) {
            double px = cx * 16 + rand.nextDouble() * 16;
            double pz = cz * 16 + rand.nextDouble() * 16;
            // Utilize full depth up to near surface
            double py = 10 + rand.nextDouble() * 900; 
            double pr = 3 + rand.nextDouble() * 8;
            
            seeds.add(new CaveSeed(px, py, pz, pr));
        }

        // Connect to seeds in immediate neighbor chunks using RNG logic
        // (Simplified: connect to the single closest seed in each adjacent chunk)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                List<CaveSeed> neighbors = getSeedsForChunkStatic(cx + dx, cz + dz, seed);
                for (CaveSeed s : seeds) {
                    CaveSeed closest = null;
                    double dMin = 1e9;
                    for (CaveSeed n : neighbors) {
                        double d = distSq(s.x, s.y, s.z, n.x, n.y, n.z);
                        if (d < dMin) { dMin = d; closest = n; }
                    }
                    if (closest != null && dMin < 60 * 60) { // Increased connection length for deep world
                        s.connections.add(closest);
                    }
                }
            }
        }

        return seeds;
    }

    // Static version to avoid recursion loops
    private List<CaveSeed> getSeedsForChunkStatic(int cx, int cz, long worldSeed) {
        long cSeed = worldSeed ^ ((long) cx << 32) ^ cz;
        Random rand = new Random(cSeed);
        List<CaveSeed> seeds = new ArrayList<>();
        int numPoints = 1 + rand.nextInt(2);
        for (int i = 0; i < numPoints; i++) {
            double px = cx * 16 + rand.nextDouble() * 16;
            double pz = cz * 16 + rand.nextDouble() * 16;
            double py = 10 + rand.nextDouble() * 900;
            seeds.add(new CaveSeed(px, py, pz, 3 + rand.nextDouble() * 8));
        }
        return seeds;
    }

    // ── Math Utilities ─────────────────────────────────────────────────────

    /** Smooth-min for organic blending of SDF shapes. */
    private double smin(double a, double b, double k) {
        double h = Math.max(k - Math.abs(a - b), 0.0) / k;
        return Math.min(a, b) - h * h * k * 0.25;
    }

    private double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private double distToSegment(double x, double y, double z, double x1, double y1, double z1, double x2, double y2, double z2) {
        double l2 = distSq(x1, y1, z1, x2, y2, z2);
        if (l2 == 0) return Math.sqrt(distSq(x, y, z, x1, y1, z1));
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1) + (z - z1) * (z2 - z1)) / l2;
        t = Math.max(0, Math.min(1, t));
        
        // Agent-based erosion approximation: Add "sag" to the path so it follows gravity
        double sag = (1.0 - Math.abs(t - 0.5) * 2.0) * 5.0; // Max 5 block drop in the middle
        
        return Math.sqrt(distSq(x, y, z, x1 + t * (x2 - x1), y1 + t * (y2 - y1) - sag, z1 + t * (z2 - z1)));
    }

    // ── Biome Categories ───────────────────────────────────────────────────

    private enum CaveCategory {
        KARST(CaveType.KARST, 6.0, 8.0),      // Large, smooth chambers
        LAVA_TUBE(CaveType.LAVA_TUBE, 3.5, 2.0), // Smooth, cylindrical
        FRACTURE(CaveType.FRACTURE, 2.0, 0.5),  // Sharp, narrow, jagged
        GENERIC(CaveType.TUNNEL, 3.0, 4.0);

        final CaveType type;
        final double baseRadius;
        final double smoothness;
        CaveCategory(CaveType t, double r, double s) { 
            type = t; baseRadius = r; smoothness = s; 
        }
    }

    private CaveCategory getCategory(Biome biome) {
        if (biome == null) return CaveCategory.GENERIC;
        switch (biome) {
            case JUNGLE: case FOREST: case REDWOOD: case GRASSLAND: return CaveCategory.KARST;
            case DESERT: case MOUNTAINS: case HIGHLANDS: return CaveCategory.LAVA_TUBE;
            case ARCTIC: case TUNDRA: case SNOWY_PEAKS: case SNOWY_FOREST: return CaveCategory.FRACTURE;
            default: return CaveCategory.GENERIC;
        }
    }
}
