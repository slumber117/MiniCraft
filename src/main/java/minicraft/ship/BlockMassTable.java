package minicraft.ship;

import minicraft.world.Block;

/**
 * BlockMassTable — maps every Block to a mass in kilograms per block.
 */
public final class BlockMassTable {

    private BlockMassTable() {} 

    public static float getMass(Block block) {
        if (block == null) return 0f;
        switch (block) {
            case AIR:        return 0f;
            case WATER:      return 0f;
            case TALL_GRASS:
            case FLOWER_RED:
            case FLOWER_BLUE:
            case MUSHROOM:
            case TORCH:
            case SEA_WEED:   return 0f;

            case LEAVES:
            case OAK_LEAVES:
            case JUNGLE_LEAVES:
            case REDWOOD_LEAVES:
            case MANGO_LEAVES:
            case APPLE_LEAVES:
            case PEAR_LEAVES: return 10f;

            case SNOW:       return 50f;
            case GLASS:      return 150f;
            case SAND:
            case RED_SAND:   return 200f;

            case WOOD:
            case OAK_WOOD:
            case APPLE_WOOD:
            case PEAR_WOOD:
            case MANGO_WOOD: return 400f;
            case JUNGLE_WOOD: return 500f;
            case REDWOOD_WOOD: return 600f;
            case WOOD_PLANKS: return 350f;

            case DIRT:
            case PODZOL:
            case GRASS:      return 800f;
            case SAND_BRICKS: return 900f;
            case STONE:
            case STONE_BRICKS: return 1_500f;
            case ICE:        return 500f;
            case CORAL:      return 300f;
            case CACTUS:     return 100f;

            case COAL_ORE:   return 1_600f;
            case COPPER_ORE:
            case TIN_ORE:    return 2_000f;
            case IRON_ORE:   return 2_500f;
            case NICKEL_ORE:
            case SILVER_ORE: return 3_000f;
            case GOLD_ORE:   return 3_500f;
            case TUNGSTEN_ORE:
            case TITANIUM_ORE: return 4_000f;
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case RUBY_ORE:
            case TOPAZ_ORE:
            case AQUAMARINE_ORE:
            case PERIDOT_ORE: return 5_000f;

            case BRONZE_BLOCK: return 4_500f;

            case ALLOY_PLATE:   return 8_000f;
            case OBSIDIAN:      return 12_000f;
            case TRANSMAT_PAD:  return 2_000f;
            case SHIP_CONSOLE:  return 1_500f;
            case CRAFTING_TABLE: return 500f;
            case FURNACE:        return 2_000f;
            case ALLOY_FORGE:    return 3_000f;
            case CHEST:          return 400f;

            case BEDROCK:    return 50_000f;

            default:         return 1_000f;
        }
    }
}
