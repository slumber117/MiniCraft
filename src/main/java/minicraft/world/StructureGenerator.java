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
}
