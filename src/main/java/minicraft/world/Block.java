package minicraft.world;

import minicraft.Main;
import minicraft.world.behavior.*;

/**
 * All placeable block types in MiniCraft.
 */
public enum Block {

    // ── Environment ───────────────────────────────────────────────────────
    AIR(false, 0, 0f, 0f, ""),
    BEDROCK(true, 9999, -1f, 0f, "obsidian"),
    GRASS(true, -1, 0.6f, 0.1f, "grass_top", "dirt", "grass"),
    DIRT(true, -1, 0.5f, 0.1f, "dirt"),
    STONE(true, 1, 1.5f, 0.5f, "stone"),
    SAND(true, -1, 0.5f, 0.1f, "sand"),
    RED_SAND(true, -1, 0.5f, 0.1f, "red_sand"),
    WATER(false, 0, 0f, 0f, "water"),
    WOOD(true, -1, 2.0f, 1.0f, "wood"),
    LEAVES(true, -1, 0.2f, 0.05f, "leaves"),
    SNOW(true, -1, 0.1f, 0.05f, "snow"),
    ICE(true, 1, 2.0f, 0.2f, "ice"),
    PODZOL(true, -1, 0.6f, 0.1f, "podzol_top", "dirt", "podzol_side"),

    // ── Construction ──────────────────────────────────────────────────────
    SAND_BRICKS(true, 0, 2.0f, 1.0f, "sand_bricks"),
    STONE_BRICKS(true, 0, 3.0f, 1.5f, "stone_bricks"),
    WOOD_PLANKS(true, 0, 1.5f, 0.5f, "wood_planks"),
    IRON_PLATE(true, 0, 2.0f, 1.0f, "iron_plating"),
    OBSIDIAN(true, 4, 40.0f, 50.0f, "obsidian"),
    FORTRESS_WALL(true, 5, 50.0f, 60.0f, "fortress_obsidian_brick"),
    FORTRESS_FLOOR(true, 5, 45.0f, 55.0f, "fortress_obsidian_floor"),
    FORTRESS_CEILING(true, 5, 45.0f, 55.0f, "fortress_obsidian_ceiling"),
    GLASS(true, 0, 1.0f, 0.5f, "glass"),

    // ── Vegetation Variants ────────────────────────────────────────────────
    OAK_WOOD(true, -1, 2.0f, 1.0f, "wood"),
    OAK_LEAVES(true, -1, 0.2f, 0.05f, "leaves"),
    REDWOOD_WOOD(true, -1, 3.0f, 2.0f, "redwood_wood"),
    REDWOOD_LEAVES(true, -1, 0.2f, 0.05f, "redwood_leaves"),
    MANGO_WOOD(true, -1, 2.0f, 1.0f, "mango_wood"),
    MANGO_LEAVES(true, -1, 0.2f, 0.05f, "fruit_leaves_mango"),
    APPLE_WOOD(true, -1, 2.0f, 1.0f, "apple_wood"),
    APPLE_LEAVES(true, -1, 0.2f, 0.05f, "fruit_leaves_apple"),
    PEAR_WOOD(true, -1, 2.0f, 1.0f, "pear_wood"),
    PEAR_LEAVES(true, -1, 0.2f, 0.05f, "fruit_leaves_pear"),
    JUNGLE_WOOD(true, -1, 2.5f, 1.5f, "jungle_wood"),
    JUNGLE_LEAVES(true, -1, 0.2f, 0.05f, "jungle_leaves"),

    // ── Basic Ores ────────────────────────────────────────────────────────
    COAL_ORE(true, 1, 3.0f, 2.0f, "coal_ore"),
    IRON_ORE(true, 2, 3.5f, 5.0f, "iron_ore"),
    COPPER_ORE(true, 1, 3.0f, 3.0f, "copper_ore"),
    TIN_ORE(true, 1, 3.0f, 3.0f, "tin_ore"),

    GOLD_ORE(true, 3, 15.0f, 10.0f, "gold_ore"),
    TANZANITE_ORE(true, 3, 10.0f, 30.0f, "tanzanite_ore"),
    SILVER_ORE(true, 3, 5.0f, 8.0f, "silver_ore"),
    NICKEL_ORE(true, 3, 5.0f, 7.0f, "nickel_ore"),
    PLATINUM_ORE(true, 3, 8.0f, 12.0f, "platinum_ore"),

    DIAMOND_ORE(true, 4, 30.0f, 100.0f, "diamond_ore"),
    EMERALD_ORE(true, 5, 25.0f, 308.0f, "emerald_ore"),
    RUBY_ORE(true, 5, 25.0f, 272.0f, "ruby_ore"),
    TOPAZ_ORE(true, 5, 20.0f, 900.0f, "topaz_ore"),
    AQUAMARINE_ORE(true, 5, 20.0f, 845.0f, "aquamarine_ore"),
    PERIDOT_ORE(true, 5, 20.0f, 1030.0f, "peridot_ore"),
    LAPIS_ORE(true, 6, 3.0f, 1000.0f, "lapis_ore"),
    SAPPHIRE_ORE(true, 5, 15.0f, 45.0f, "sapphire_ore"),
    AMETHYST_ORE(true, 4, 12.0f, 12.0f, "amethyst_ore"),
    JADE_ORE(true, 5, 10.0f, 700.0f, "jade_ore"),
    ONYX_ORE(true, 10, 120.0f, 7.5f, "onyx_ore"),
    GARNET_ORE(true, 8, 90.0f, 120.0f, "garnet_ore"),
    TOURMALINE_ORE(true, 5, 25.0f, 40.0f, "tourmaline_ore"),
    AGATE_ORE(true, 8, 20.0f, 33000.0f, "agate_ore"),
    PAINITE_ORE(true, 9, 80.0f, 345000.0f, "painite_ore"), // High-fidelity deep red crystalline ore
    TAAFFEITE_ORE(true, 7, 90.0f, 1285.0f, "tanzanite_ore"), // Solid lilac crystal pattern
    GRANDIDIERITE_ORE(true, 8, 95.0f, 600.0f, "grandidierite_ore"),
    SERENDIBITE_ORE(true, 8, 100.0f, 2000.0f, "serendibite_ore"),
    ALEXANDRITE_ORE(true, 7, 80.0f, 925.0f, "alexandrite_ore"),
    MUSGRAVITE_ORE(true, 8, 110.0f, 400.0f, "musgravite_ore"),
    OPAL_ORE(true, 5, 15.0f, 50.0f, "opal_ore"),

    QUARTZ_ORE(true, 4, 4.0f, 400.0f, "quartz_ore"),
    PYRITE_ORE(true, 8, 45.0f, 1000.0f, "pyrite_ore"),
    MITHRIL_ORE(true, 10, 150.0f, 7.5f, "mithril_ore"),
    ADAMANTINE_ORE(true, 7, 50.0f, 1400.0f, "adamantine_ore"),
    ORICHALCUM_ORE(true, 6, 35.0f, 1500.0f, "orichalcum_ore"),
    PLUTONIUM_ORE(true, 6, 50.0f, 1225.0f, "plutonium_ore"),
    URANIUM_ORE(true, 6, 45.0f, 1000.0f, "uranium_ore"),
    NEPTUNIUM_ORE(true, 6, 45.0f, 1500.0f, "neptunium_ore"),

    TUNGSTEN_ORE(true, 4, 10.0f, 12.0f, "tungsten_ore"),
    TITANIUM_ORE(true, 3, 10.0f, 12.0f, "titanium_ore"),

    // ── Rare Earth Ores (Tier 1, 12-30) ──────────────────────────────────
    XANTHIOSITE_ORE(true, 12, 4.0f, 10.0f, "xanthiosite_ore"),
    MONAZITE_ORE(true, 13, 180.0f, 11.5f, "monazite_ore"),
    BASTNAESITE_ORE(true, 14, 200.0f, 13.2f, "bastnaesite_ore"),
    XENOTIME_ORE(true, 15, 220.0f, 15.2f, "xenotime_ore"),
    LOPARITE_ORE(true, 16, 240.0f, 17.4f, "loparite_ore"),
    TANTALITE_ORE(true, 17, 260.0f, 20.1f, "tantalite_ore"),
    VANADINITE_ORE(true, 18, 280.0f, 23.1f, "vanadinite_ore"),
    GADOLINIUM_ORE(true, 19, 300.0f, 26.5f, "gadolinium_ore"),
    TERBIUM_ORE(true, 20, 320.0f, 30.5f, "terbium_ore"),
    DYSPROSIUM_ORE(true, 21, 340.0f, 35.1f, "dysprosium_ore"),
    HOLMIUM_ORE(true, 11, 360.0f, 8.7f, "holmium_ore"),
    ERBIUM_ORE(true, 22, 380.0f, 40.3f, "erbium_ore"),
    YTTRIUM_ORE(true, 23, 400.0f, 46.4f, "yttrium_ore"),
    LUTETIUM_ORE(true, 24, 420.0f, 53.3f, "lutetium_ore"),
    SAMARIUM_ORE(true, 25, 440.0f, 61.3f, "samarium_ore"),
    NEODYMIUM_ORE(true, 9, 460.0f, 1600.0f, "neodymium_ore"),
    PRASEODYMIUM_ORE(true, 26, 480.0f, 70.5f, "praseodymium_ore"),
    CERIUM_ORE(true, 27, 500.0f, 81.1f, "cerium_ore"),
    LANTHANUM_ORE(true, 28, 520.0f, 93.3f, "lanthanum_ore"),
    PROMETHIUM_ORE(true, 30, 600.0f, 123.4f, "promethium_ore"),

    // --- 13. VERDANT TIER (Tier 31-35) ---
    RUTHENIUM_ORE(true, 31, 650.0f, 141.9f, "ruthenium_ore"),
    RHENIUM_ORE(true, 32, 700.0f, 163.2f, "rhenium_ore"),
    IRIDIUM_ORE(true, 33, 750.0f, 187.6f, "iridium_ore"),
    OSMIUM_ORE(true, 34, 800.0f, 215.8f, "osmium_ore"),
    RHODIUM_ORE(true, 35, 850.0f, 248.2f, "rhodium_ore"),

    // --- 14. VANGUARD TIER (Tier 50-100) ---
    ANTIMATTER_ORE(true, 50, 2000.0f, 4038.4f, "antimatter_ore"),
    DARKMATTER_ORE(true, 60, 3000.0f, 8168.9f, "darkmatter_ore"),
    GAMMA_RAY_ORE(true, 80, 5000.0f, 133697.3f, "gamma_ray_ore"),
    NEBULA_ORE(true, 100, 10000.0f, 2188161.7f, "nebula_ore"),

    // Arena Blocks
    BEDROCK_WALL(true, 0, 999999f, 999999f, "stone"), // Unbreakable
    BOSS_GATE(true, 0, 999999f, 999999f, "iron_block"), // Only passable if Level 25+
    GOLD_BLOCK(true, 0, 50.0f, 100.0f, "gold_block"),
    ONYX_BLOCK(true, 0, 500.0f, 1000.0f, "onyx_ore"),

    // Ores dropped from bosses
    DRAGON_SCALE_ORE(false, 1, 3, 100.0f, "dragon_scale_ore"),

    BRONZE_BLOCK(true, 0, 5.0f, 2.0f, "bronze_block"),
    CRAFTING_TABLE(true, 0, 2.5f, 5.0f, "blacksmith_top", "stone", "blacksmith_side",
            new minicraft.world.behavior.CraftingTableBlock()),
    BLACKSMITH(true, 0, 4.0f, 10.0f, "blacksmith_top", "stone", "blacksmith_side",
            new minicraft.world.behavior.BlacksmithBlock()),
    FURNACE(true, 0, 3.5f, 10.0f, "furnace", Block.MeshType.CUBE,
            new minicraft.world.behavior.FurnaceBlock("INDUSTRIAL SMELTER", "FURNACE")),
    ALLOY_FORGE(true, 0, 4.0f, 15.0f, "alloy_forge", Block.MeshType.CUBE,
            new minicraft.world.behavior.FurnaceBlock("FUSION FORGE", "ALLOY_FORGE")),
    CHEST(true, 0, 50.0f, 15.0f, "boss_chest_texture", Block.MeshType.CUBE, new minicraft.world.behavior.ChestBlock()),
    GOLDEN_CHEST(true, 0, 50.0f, 15.0f, "golden_boss_chest_texture", Block.MeshType.CUBE,
            new minicraft.world.behavior.ChestBlock()),

    // ── Vegetation & Undersea ─────────────────────────────────────────────
    TALL_GRASS(false, 0, 0f, 0f, "tall_grass", MeshType.CROSS),
    FLOWER_RED(false, 0, 0f, 0f, "flower_red", MeshType.CROSS),
    FLOWER_BLUE(false, 0, 0f, 0f, "flower_blue", MeshType.CROSS),
    MUSHROOM(false, 0, 0f, 0f, "mushroom", MeshType.CROSS),
    TORCH(false, 0, 0f, 1.0f, "torch", MeshType.CROSS),
    TIN_TORCH(false, 0, 0f, 1.1f, "tin_torch", MeshType.CROSS),
    IRON_TORCH(false, 0, 0f, 1.2f, "iron_torch", MeshType.CROSS),
    GOLD_TORCH(false, 0, 0f, 1.5f, "gold_torch", MeshType.CROSS),
    COPPER_TORCH(false, 0, 0f, 1.1f, "copper_torch", MeshType.CROSS),
    NICKEL_TORCH(false, 0, 0f, 1.2f, "nickel_torch", MeshType.CROSS),
    URANIUM_TORCH(false, 0, 0f, 1.3f, "uranium_torch", MeshType.CROSS),
    PLUTONIUM_TORCH(false, 0, 0f, 1.6f, "plutonium_torch", MeshType.CROSS),
    CACTUS(true, 0, 0.5f, 0.5f, "cactus"),
    SEA_WEED(false, 0, 0f, 0f, "sea_weed", MeshType.CROSS),
    CORAL(true, 0, 0.3f, 1.0f, "coral"),
    FIBRE_BUSH(false, 0, 0.1f, 0.5f, "fibre_bush", MeshType.CROSS),

    // ── Advanced Scientific blocks ───────────────────────────────────────
    ALLOY_PLATE(true, 1, 10.0f, 2.0f, "alloy_plate"),
    TRANSMAT_PAD(true, 1, 10.0f, 5.0f, "transmat_pad"),
    SHIP_CONSOLE(true, 0, 2.5f, 10.0f, "ship_console", Block.MeshType.CUBE,
            new minicraft.world.behavior.ConsoleBlock()),
    COOKER(true, 0, 3.0f, 10.0f, "cooker_top", "stone", "cooker_side",
            new minicraft.world.behavior.FurnaceBlock("COOKING UNIT", "COOKER")), // High-efficiency food preparation
                                                                                  // unit
    STONE_DARK(true, 0, 5.0f, 1.0f, "stone_dark"), // Deep layer industrial stone
    LAVA(false, 0, 0f, 0f, "lava"), // High-intensity surface liquid hazard
    MAGMA(true, 0, 1.0f, 5.0f, "magma"), // Glowing underground solid hazard
    FIRE(false, 0, 0f, 0.5f, "fire", MeshType.CROSS);

    // ── Mesh Types ──
    public enum MeshType {
        CUBE, CROSS
    }

    public final boolean solid;
    public final String topTexture;
    public final String bottomTexture;
    public final String sideTexture;
    public final float paddingTop;
    public final float paddingSide;
    public final int requiredHarvestLevel;
    public final float hardness;
    public final float xpValue;
    public final MeshType meshType;
    public final BlockInteraction interaction;

    Block(boolean solid, int requiredHarvestLevel, float hardness, float xpValue, String textureName) {
        this(solid, requiredHarvestLevel, hardness, xpValue, textureName, MeshType.CUBE);
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, float xpValue, String textureName,
            MeshType meshType) {
        this(solid, requiredHarvestLevel, hardness, xpValue, textureName, meshType, null);
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, float xpValue, String textureName, MeshType meshType,
            BlockInteraction interaction) {
        this.solid = solid;
        this.requiredHarvestLevel = requiredHarvestLevel;
        this.hardness = hardness;
        this.xpValue = xpValue;
        this.meshType = meshType;
        this.topTexture = textureName;
        this.bottomTexture = textureName;
        this.sideTexture = textureName;
        this.paddingTop = solid ? 1.0f : 0.0f;
        this.paddingSide = solid ? 1.0f : 0.0f;
        this.interaction = interaction;
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, float xpValue, String top, String bottom,
            String side) {
        this(solid, requiredHarvestLevel, hardness, xpValue, top, bottom, side, null);
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, float xpValue, String top, String bottom,
            String side, BlockInteraction interaction) {
        this.solid = solid;
        this.requiredHarvestLevel = requiredHarvestLevel;
        this.hardness = hardness;
        this.xpValue = xpValue;
        this.meshType = MeshType.CUBE;
        this.topTexture = top;
        this.bottomTexture = bottom;
        this.sideTexture = side;
        this.paddingTop = solid ? 1.0f : 0.0f;
        this.paddingSide = solid ? 1.0f : 0.0f;
        this.interaction = interaction;
    }

    public void onInteract(Main main, World world, int x, int y, int z) {
        if (interaction != null) {
            interaction.onInteract(main, world, x, y, z);
        }
    }

    public String getInteractionLabel() {
        if (this == FURNACE)
            return "INDUSTRIAL SMELTER";
        if (this == ALLOY_FORGE)
            return "FUSION FORGE";
        if (this == COOKER)
            return "HIGH-EFFICIENCY COOKER";
        if (this == CRAFTING_TABLE)
            return "CRAFTING FORGE";
        if (this == BLACKSMITH)
            return "BLACKSMITH STATION";
        return "";
    }

    public String getTextureForFace(Face face) {
        return getTextureForFace(face, false);
    }

    public String getTextureForFace(Face face, boolean lit) {
        String base = "";
        switch (face) {
            case TOP:
                base = topTexture;
                break;
            case BOTTOM:
                base = bottomTexture;
                break;
            case SIDE:
                base = sideTexture;
                break;
            default:
                base = sideTexture;
                break;
        }

        if (lit) {
            // Check if a _lit version exists by convention
            if (this == FURNACE || this == COOKER || this == ALLOY_FORGE) {
                return base + "_lit";
            }
        }
        return base;
    }

    public float getPaddingForFace(Face face) {
        switch (face) {
            case TOP:
                return paddingTop;
            case BOTTOM:
                return paddingTop;
            case SIDE:
                return paddingSide;
            default:
                return paddingSide;
        }
    }

    /**
     * Returns true if this block is a gem-type ore (precious/semi-precious stones).
     * Used by the Emerald armor set bonus for gem mining speed.
     */
    public boolean isGemOre() {
        return this == DIAMOND_ORE || this == EMERALD_ORE || this == RUBY_ORE
                || this == TOPAZ_ORE || this == AQUAMARINE_ORE || this == PERIDOT_ORE
                || this == SAPPHIRE_ORE || this == AMETHYST_ORE || this == JADE_ORE
                || this == ONYX_ORE || this == GARNET_ORE || this == TOURMALINE_ORE
                || this == AGATE_ORE || this == PAINITE_ORE || this == OPAL_ORE
                || this == TANZANITE_ORE || this == LAPIS_ORE
                || this == TAAFFEITE_ORE || this == GRANDIDIERITE_ORE
                || this == SERENDIBITE_ORE || this == ALEXANDRITE_ORE
                || this == MUSGRAVITE_ORE;
    }

    public boolean isAir() {
        return this == AIR;
    }

    public boolean isOpaque() {
        if (!solid)
            return false;
        // Transparent solids
        if (this == LEAVES || this == WATER || this == SNOW || this == SEA_WEED || this == CORAL || this == ICE
                || this == GLASS || this == TRANSMAT_PAD)
            return false;
        if (this == OAK_LEAVES || this == REDWOOD_LEAVES || this == MANGO_LEAVES || this == APPLE_LEAVES
                || this == PEAR_LEAVES || this == JUNGLE_LEAVES)
            return false;
        return true;
    }

    public String getFriendlyName() {
        if (this == AIR)
            return "";
        String n = name();
        // Special case overrides
        if (this == FURNACE)
            return "Industrial Smelter";
        if (this == ALLOY_FORGE)
            return "Fusion Forge";
        if (this == SHIP_CONSOLE)
            return "Neural Link Console";

        // General underscore conversion to title case
        String[] parts = n.replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1)
                sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
