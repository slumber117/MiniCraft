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
    STONE(true, 0, 1.5f, 0.5f, "stone"),
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
    IRON_ORE(true, 1, 3.5f, 5.0f, "iron_ore"),
    COPPER_ORE(true, 1, 3.0f, 3.0f, "copper_ore"),
    TIN_ORE(true, 1, 3.0f, 3.0f, "tin_ore"),

    GOLD_ORE(true, 2, 5.0f, 10.0f, "gold_ore"),
    TANZANITE_ORE(true, 2, 5.0f, 15.0f, "tanzanite_ore"),
    SILVER_ORE(true, 2, 5.0f, 8.0f, "silver_ore"),
    NICKEL_ORE(true, 2, 5.0f, 7.0f, "nickel_ore"),
    PLATINUM_ORE(true, 2, 8.0f, 12.0f, "platinum_ore"),

    DIAMOND_ORE(true, 3, 30.0f, 20.0f, "diamond_ore"),
    EMERALD_ORE(true, 3, 25.0f, 18.0f, "emerald_ore"),
    RUBY_ORE(true, 3, 25.0f, 18.0f, "ruby_ore"),
    TOPAZ_ORE(true, 3, 20.0f, 15.0f, "topaz_ore"),
    AQUAMARINE_ORE(true, 3, 20.0f, 15.0f, "aquamarine_ore"),
    PERIDOT_ORE(true, 3, 20.0f, 15.0f, "peridot_ore"),
    LAPIS_ORE(true, 1, 3.0f, 5.0f, "lapis_ore"),
    SAPPHIRE_ORE(true, 3, 15.0f, 15.0f, "sapphire_ore"),
    AMETHYST_ORE(true, 2, 12.0f, 12.0f, "amethyst_ore"),
    JADE_ORE(true, 2, 10.0f, 10.0f, "jade_ore"),
    ONYX_ORE(true, 5, 60.0f, 55.0f, "onyx_ore"), // Stronger than Adamantine
    GARNET_ORE(true, 3, 25.0f, 20.0f, "garnet_ore"),
    TOURMALINE_ORE(true, 3, 25.0f, 20.0f, "tourmaline_ore"),
    AGATE_ORE(true, 3, 20.0f, 15.0f, "agate_ore"),
    PAINITE_ORE(true, 6, 80.0f, 100.0f, "painite_ore"),
    TAAFFEITE_ORE(true, 6, 90.0f, 150.0f, "taaffeite_ore"),
    GRANDIDIERITE_ORE(true, 6, 95.0f, 200.0f, "grandidierite_ore"),
    SERENDIBITE_ORE(true, 6, 100.0f, 300.0f, "serendibite_ore"),
    ALEXANDRITE_ORE(true, 5, 80.0f, 150.0f, "alexandrite_ore"),
    MUSGRAVITE_ORE(true, 6, 110.0f, 400.0f, "musgravite_ore"),
    OPAL_ORE(true, 3, 15.0f, 25.0f, "opal_ore"),

    QUARTZ_ORE(true, 1, 4.0f, 3.0f, "quartz_ore"),
    PYRITE_ORE(true, 1, 4.0f, 4.0f, "pyrite_ore"),
    MITHRIL_ORE(true, 10, 150.0f, 500.0f, "mithril_ore"),
    ADAMANTINE_ORE(true, 5, 50.0f, 40.0f, "adamantine_ore"),
    ORICHALCUM_ORE(true, 4, 35.0f, 35.0f, "orichalcum_ore"),
    PLUTONIUM_ORE(true, 4, 50.0f, 50.0f, "plutonium_ore"),
    URANIUM_ORE(true, 4, 45.0f, 45.0f, "uranium_ore"),
    NEPTUNIUM_ORE(true, 4, 45.0f, 45.0f, "neptunium_ore"),

    TUNGSTEN_ORE(true, 2, 10.0f, 12.0f, "tungsten_ore"),
    TITANIUM_ORE(true, 2, 10.0f, 12.0f, "titanium_ore"),

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
    CHEST(true, 0, 2.5f, 5.0f, "chest", Block.MeshType.CUBE, new minicraft.world.behavior.ChestBlock()),

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
