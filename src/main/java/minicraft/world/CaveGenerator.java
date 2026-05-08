package minicraft.world;

import minicraft.world.cave.CaveType;

/**
 * Handles true 3D procedural cave generation.
 * Uses density functions and ridged noise to create Minecraft-style
 * interconnected tunnels, ravines, and large cavern pockets.
 */
public class CaveGenerator {

    private final PerlinNoise noiseGen;
    
    // Offsets to prevent all cave types from generating in the exact same spot
    private final double tunnelOffset = 10000.0;
    private final double ravineOffset = 20000.0;
    private final double cheeseOffset = 30000.0;

    public CaveGenerator(long seed) {
        // Use a different seed from the surface generator for variety
        this.noiseGen = new PerlinNoise(seed * 31); 
    }

    /**
     * Determines the type of cave at a specific block coordinate.
     * 
     * @param worldX   Absolute X coordinate
     * @param worldY   Absolute Y coordinate
     * @param worldZ   Absolute Z coordinate
     * @param surfaceY The Y-level of the terrain surface
     * @return The CaveType at this location, or CaveType.NONE if solid
     */
    public CaveType getCaveType(int worldX, int worldY, int worldZ, float surfaceY) {
        
        // 1. Safety Crust: Never carve caves too close to the surface,
        // or through the bottom of the world.
        if (worldY > surfaceY - 8 || worldY <= 2) {
            return CaveType.NONE;
        }

        // Depth modifier: Caves get slightly larger/more common the deeper you go
        float depthModifier = 1.0f - (worldY / surfaceY);

        // ==========================================
        // TYPE 1: Mineshaft-Style Tunnels (3D Tubes)
        // ==========================================
        // A single Math.abs(noise) < threshold creates massive 2D sheets/caverns.
        // To create 1D "tubes" (tunnels), we intersect two different noise functions.
        // We stretch the Y noise (0.012) compared to X/Z (0.025) to make tunnels 
        // 4-5 blocks wide and ~8 blocks tall on average.
        
        double n1X = (worldX + tunnelOffset) * 0.018;
        double n1Y = worldY * 0.006;  // Smoother vertical descent
        double n1Z = (worldZ + tunnelOffset) * 0.018;
        
        double n2X = (worldX + tunnelOffset + 5000) * 0.018;
        double n2Y = (worldY + 5000) * 0.006;
        double n2Z = (worldZ + tunnelOffset + 5000) * 0.018;
        
        double noise1 = noiseGen.fractalNoise(n1X, n1Y, n1Z, 2, 0.5);
        double noise2 = noiseGen.fractalNoise(n2X, n2Y, n2Z, 2, 0.5);
        
        // Sum of squares creates a tubular distance field
        double tubeDensity = (noise1 * noise1) + (noise2 * noise2);
        
        // Threshold creates tubes. 0.01 noise-space radius = 4 blocks wide (0.01 / 0.025 * 10ish)
        float tubeThreshold = 0.008f + (0.01f * depthModifier); 
        
        if (tubeDensity < tubeThreshold) {
            return CaveType.TUNNEL;
        }

        // ==========================================
        // TYPE 2: Ravines (Deep Vertical Cracks)
        // ==========================================
        double rNoiseX = (worldX + ravineOffset) * 0.01;
        double rNoiseY = worldY * 0.002;
        double rNoiseZ = (worldZ + ravineOffset) * 0.01;
        
        double ravineDensity = Math.abs(noiseGen.fractalNoise(rNoiseX, rNoiseY, rNoiseZ, 2, 0.5));
        
        double pinchNoise = noiseGen.noise(worldX * 0.005, 0, worldZ * 0.005);
        if (ravineDensity < 0.008f && pinchNoise > 0.0) {
            return CaveType.RAVINE;
        }

        // ==========================================
        // TYPE 3: Cheese Caves (Massive Caverns)
        // ==========================================
        double cNoiseX = (worldX + cheeseOffset) * 0.008;
        double cNoiseY = worldY * 0.01;
        double cNoiseZ = (worldZ + cheeseOffset) * 0.008;
        
        double cavernDensity = noiseGen.fractalNoise(cNoiseX, cNoiseY, cNoiseZ, 3, 0.5);
        
        // Disable giant caverns completely to stop "drop after drop" generation
        // Tunnels and ravines will be the only cave types.
        if (cavernDensity > 1.5f) { 
            return CaveType.CAVERN;
        }

        return CaveType.NONE;
    }
}
