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
        // TYPE 1: Mineshaft-Style Tunnels
        // ==========================================
        // Lower noise multipliers mean the noise changes much slower, creating 
        // extremely long, continuous stretches of tunnels instead of fragmented pockets.
        double tNoiseX = (worldX + tunnelOffset) * 0.006;
        double tNoiseY = worldY * 0.008;  // Moderate vertical variation for smooth descents
        double tNoiseZ = (worldZ + tunnelOffset) * 0.006;
        
        double tunnelDensity = Math.abs(noiseGen.fractalNoise(tNoiseX, tNoiseY, tNoiseZ, 3, 0.4));
        
        // Wider threshold ensures tunnels are traversable and don't pinch off easily
        float tunnelThreshold = 0.022f + (0.01f * depthModifier); 
        if (tunnelDensity < tunnelThreshold) {
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
        
        if (cavernDensity > 0.75f - (0.02f * depthModifier)) { // Rarer caverns, less depth influence
            return CaveType.CAVERN;
        }

        return CaveType.NONE;
    }
}
