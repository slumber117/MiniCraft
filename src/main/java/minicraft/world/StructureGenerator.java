package minicraft.world;

import java.util.Random;

/**
 * Procedural Architect for Pre-built Villages, Castles, and Fortresses.
 */
public class StructureGenerator {

    private final Random random = new Random();

    public void generateVillage(Chunk chunk, int x, int y, int z, Biome biome) {
        // Small Wooden Hut (5x5x4)
        Block mat = Block.WOOD;
        if (biome == Biome.JUNGLE) mat = Block.JUNGLE_WOOD;
        else if (biome == Biome.REDWOOD || biome == Biome.TUNDRA) mat = Block.REDWOOD_WOOD;
        
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy < 4; dy++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3) {
                        chunk.setBlock(x + dx, y + dy, z + dz, mat);
                    } else {
                        chunk.setBlock(x + dx, y + dy, z + dz, Block.AIR);
                    }
                }
            }
        }
        chunk.setBlock(x + 2, y + 1, z, Block.AIR); // Door Gap
    }

    public void generateCastle(Chunk chunk, int x, int y, int z, Biome biome) {
        // High-Fidelity Stone Castle (7x7x8) with biome variants
        Block mat = Block.STONE;
        if (biome == Biome.FROZEN_OCEAN || biome == Biome.ARCTIC) mat = Block.ICE;
        else if (biome == Biome.DESERT) mat = Block.OBSIDIAN;

        for (int dy = 0; dy < 8; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean wall = (Math.abs(dx) == 3 || Math.abs(dz) == 3);
                    if (wall || dy == 0 || dy == 7) {
                        chunk.setBlock(x + dx, y + dy, z + dz, mat);
                    } else {
                        chunk.setBlock(x + dx, y + dy, z + dz, Block.AIR);
                    }
                }
            }
        }
        chunk.setBlock(x, y + 1, z, Block.CHEST); // Contains valuable Iron-age loot
    }

    public void generateFortress(Chunk chunk, int x, int y, int z, Biome biome) {
        // Elite Fortress (9x9x10) for Rugged Mountains
        Block mat = Block.OBSIDIAN;
        if (biome == Biome.SNOWY_PEAKS) mat = Block.STONE_BRICKS;

        for (int dy = 0; dy < 10; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    boolean ext = (Math.abs(dx) == 4 || Math.abs(dz) == 4);
                    if (ext || dy == 0) {
                        // Material foundation for elite durability
                        chunk.setBlock(x + dx, y + dy, z + dz, (dy < 4) ? mat : Block.STONE);
                    } else {
                        chunk.setBlock(x + dx, y + dy, z + dz, Block.AIR);
                    }
                }
            }
        }
        chunk.setBlock(x, y + 1, z, Block.CHEST); // Top-tier rewards
    }

    public void generateFloatingFactory(Chunk chunk, int x, int y, int z) {
        // Floating Alloy Platform (16x16) at Y=180
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                chunk.setBlock(x + dx, y, z + dz, Block.ALLOY_PLATE);
            }
        }
        
        // Command Console
        chunk.setBlock(x + 8, y + 1, z + 8, Block.SHIP_CONSOLE);
        
        // Docking Pylons (Structural supports hanging down)
        for (int dy = -10; dy < 0; dy++) {
            chunk.setBlock(x + 2, y + dy, z + 2, Block.ALLOY_PLATE);
            chunk.setBlock(x + 13, y + dy, z + 13, Block.ALLOY_PLATE);
        }
    }

    public void generateEncouragementShip(Chunk chunk, int x, int y, int z) {
        // 80-block megastructure hull (Simplified profile for construction bay)
        // This is a placeholder structure representing the "Build" state.
        for (int dx = 0; dx < 80; dx++) {
            for (int dy = 0; dy < 8; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    // Tapered Stalwart silhouette
                    int width = (dx < 20) ? 2 : (dx > 60 ? 3 : 5);
                    if (Math.abs(dz) <= width) {
                        chunk.setBlock(x + dx, y + dy, z + dz, Block.ALLOY_PLATE);
                    }
                }
            }
        }
    }
}
