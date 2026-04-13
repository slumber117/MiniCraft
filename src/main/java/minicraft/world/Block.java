package minicraft.world;

/**
 * All placeable block types in MiniCraft.
 */
public enum Block {

    // ── Environment ───────────────────────────────────────────────────────
    AIR       (false, 0, 0f, ""),
    BEDROCK   (true,  9999, -1f, "stone"),
    GRASS     (true,  0, 0.6f, "grass_top", "dirt", "grass"),
    DIRT      (true,  0, 0.5f, "dirt"),
    STONE     (true,  1, 1.5f, "stone"),
    SAND      (true,  0, 0.5f, "sand"),
    RED_SAND  (true,  0, 0.5f, "red_sand"),
    WATER     (false, 0, 0f,   "water"),
    WOOD      (true,  0, 2.0f, "wood"),
    LEAVES    (true,  0, 0.2f, "leaves"),
    SNOW      (true,  0, 0.1f, "snow"),
    ICE       (true,  1, 2.0f, "ice"),
    PODZOL    (true,  0, 0.6f, "podzol_top", "dirt", "podzol_side"),

    // ── Construction ──────────────────────────────────────────────────────
    SAND_BRICKS  (true, 0, 2.0f, "sand_bricks"),
    STONE_BRICKS (true, 0, 3.0f, "stone_bricks"),
    WOOD_PLANKS  (true, 0, 1.5f, "wood_planks"),
    OBSIDIAN     (true, 4, 40.0f, "obsidian"),

    // ── Vegetation Variants ────────────────────────────────────────────────
    OAK_WOOD        (true, 0, 2.0f, "wood"),
    OAK_LEAVES      (true, 0, 0.2f, "leaves"),
    REDWOOD_WOOD    (true, 0, 3.0f, "redwood_wood"),
    REDWOOD_LEAVES  (true, 0, 0.2f, "redwood_leaves"),
    MANGO_WOOD      (true, 0, 2.0f, "mango_wood"),
    MANGO_LEAVES    (true, 0, 0.2f, "fruit_leaves_mango"),
    APPLE_WOOD      (true, 0, 2.0f, "apple_wood"),
    APPLE_LEAVES    (true, 0, 0.2f, "fruit_leaves_apple"),
    PEAR_WOOD       (true,  0, 2.0f, "pear_wood"),
    PEAR_LEAVES     (true,  0, 0.2f, "fruit_leaves_pear"),
    JUNGLE_WOOD     (true,  0, 2.5f, "jungle_wood"),
    JUNGLE_LEAVES   (true,  0, 0.2f, "jungle_leaves"),

    // ── Basic Ores ────────────────────────────────────────────────────────
    COAL_ORE      (true, 1, 3.0f,  "coal_ore"),
    IRON_ORE      (true, 1, 3.5f,  "iron_ore"),
    COPPER_ORE    (true, 1, 3.0f,  "copper_ore"),
    TIN_ORE       (true, 1, 3.0f,  "tin_ore"),

    GOLD_ORE      (true, 2, 5.0f,  "gold_ore"),
    SILVER_ORE    (true, 2, 5.0f,  "silver_ore"),
    NICKEL_ORE    (true, 2, 5.0f,  "nickel_ore"),

    DIAMOND_ORE   (true, 3, 30.0f, "diamond_ore"),
    EMERALD_ORE   (true, 3, 25.0f, "emerald_ore"),
    RUBY_ORE      (true, 3, 25.0f, "ruby_ore"),
    TOPAZ_ORE     (true, 3, 20.0f, "topaz_ore"),
    AQUAMARINE_ORE(true, 3, 20.0f, "aquamarine_ore"),
    PERIDOT_ORE   (true, 3, 20.0f, "peridot_ore"),

    TUNGSTEN_ORE  (true, 2, 10.0f, "tungsten_ore"),
    TITANIUM_ORE  (true, 2, 10.0f, "titanium_ore"),

    BRONZE_BLOCK  (true, 0, 5.0f, "bronze_block"),
    CRAFTING_TABLE(true, 0, 2.5f, "crafting_table"),
    FURNACE       (true, 0, 3.5f, "furnace"),
    ALLOY_FORGE   (true, 0, 4.0f, "alloy_forge"),
    CHEST         (true, 0, 2.5f, "chest"),

    // ── Vegetation & Undersea ─────────────────────────────────────────────
    TALL_GRASS   (false, 0, 0f,   "tall_grass", MeshType.CROSS),
    FLOWER_RED   (false, 0, 0f,   "flower_red", MeshType.CROSS),
    FLOWER_BLUE  (false, 0, 0f,   "flower_blue", MeshType.CROSS),
    MUSHROOM     (false, 0, 0f,   "mushroom", MeshType.CROSS),
    TORCH        (false, 0, 0f,   "torch", MeshType.CROSS),
    CACTUS       (true,  0, 0.5f, "cactus"),
    SEA_WEED     (false, 0, 0f,   "sea_weed", MeshType.CROSS),
    CORAL        (true,  0, 0.3f, "coral"),
    
    // ── Advanced Scientific blocks ───────────────────────────────────────
    ALLOY_PLATE  (true, 1, 10.0f, "alloy_plate"),
    SHIP_CONSOLE (true, 0, 2.5f,  "ship_console");

    // ── Mesh Types ──
    public enum MeshType { CUBE, CROSS }

    public final boolean solid;
    public final String  topTexture;
    public final String  bottomTexture;
    public final String  sideTexture;
    public final float   paddingTop;
    public final float   paddingSide;
    public final int     requiredHarvestLevel;
    public final float   hardness;
    public final MeshType meshType;

    Block(boolean solid, int requiredHarvestLevel, float hardness, String textureName) {
        this(solid, requiredHarvestLevel, hardness, textureName, MeshType.CUBE);
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, String textureName, MeshType meshType) {
        this.solid                = solid;
        this.requiredHarvestLevel = requiredHarvestLevel;
        this.hardness             = hardness;
        this.meshType             = meshType;
        this.topTexture           = textureName;
        this.bottomTexture        = textureName;
        this.sideTexture          = textureName;
        this.paddingTop           = solid ? 1.0f : 0.0f;
        this.paddingSide          = solid ? 1.0f : 0.0f;
    }

    Block(boolean solid, int requiredHarvestLevel, float hardness, String top, String bottom, String side) {
        this.solid                = solid;
        this.requiredHarvestLevel = requiredHarvestLevel;
        this.hardness             = hardness;
        this.meshType             = MeshType.CUBE;
        this.topTexture           = top;
        this.bottomTexture        = bottom;
        this.sideTexture          = side;
        this.paddingTop           = solid ? 1.0f : 0.0f;
        this.paddingSide          = solid ? 1.0f : 0.0f;
    }

    public String getTextureForFace(Face face) {
        switch (face) {
            case TOP:    return topTexture;
            case BOTTOM: return bottomTexture;
            case SIDE:   return sideTexture;
            default:     return sideTexture;
        }
    }

    public float getPaddingForFace(Face face) {
        switch (face) {
            case TOP:    return paddingTop;
            case BOTTOM: return paddingTop;
            case SIDE:   return paddingSide;
            default:     return paddingSide;
        }
    }

    public boolean isAir() { return this == AIR; }
    
    public boolean isOpaque() {
        if (!solid) return false;
        // Transparent solids
        if (this == LEAVES || this == WATER || this == SNOW || this == SEA_WEED || this == CORAL || this == ICE) return false;
        if (this == OAK_LEAVES || this == REDWOOD_LEAVES || this == MANGO_LEAVES || this == APPLE_LEAVES || this == PEAR_LEAVES || this == JUNGLE_LEAVES) return false;
        return true;
    }
}
