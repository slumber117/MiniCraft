package minicraft.entity;

/**
 * Every living entity type in MiniCraft.
 * Stores display name, category, base stats, and dimensions.
 */
public enum EntityType {

    // ── Passive Animals ───────────────────────────────────────────────────
    COW     ("Cow",     Category.PASSIVE,   10f, 0.9f, 1.4f, "Plains / Forest"),
    SHEEP   ("Sheep",   Category.PASSIVE,   8f,  0.9f, 1.3f, "Plains"),
    RAM     ("Ram",     Category.PASSIVE,   12f, 1.0f, 1.4f, "Mountains"),
    DOG     ("Dog",     Category.PASSIVE,   16f, 0.6f, 0.8f, "Forest / Village"),
    CAT     ("Cat",     Category.PASSIVE,   10f, 0.6f, 0.7f, "Village"),
    WHALE   ("Whale",   Category.PASSIVE,   80f, 3.0f, 2.0f, "Ocean"),

    // ── Predators ─────────────────────────────────────────────────────────
    BEAR    ("Bear",    Category.PREDATOR,  40f, 1.4f, 1.8f, "Forest / Mountains"),
    WOLF    ("Wolf",    Category.PREDATOR,  20f, 0.8f, 1.0f, "Forest / Taiga"),
    TIGER   ("Tiger",  Category.PREDATOR,  35f, 1.1f, 1.2f, "Jungle"),
    LION    ("Lion",    Category.PREDATOR,  38f, 1.2f, 1.3f, "Savanna"),
    EAGLE   ("Eagle",   Category.PREDATOR,  14f, 1.0f, 0.9f, "Mountains / Sky"),

    // ── NPCs ──────────────────────────────────────────────────────────────
    FARMER  ("Farmer",  Category.NPC,       20f, 0.6f, 1.8f, "Village"),
    
    // ── Monsters ────────────────────────────────────────────────────────
    ZOMBIE  ("Zombie",  Category.MONSTER,   20f, 0.6f, 1.8f, "Caves / Night"),
    SPIDER  ("Spider",  Category.MONSTER,   15f, 1.2f, 0.8f, "Caves / Forest"),

    // ── Resources ──────────────────────────────────────────────────────────
    ITEM    ("Item",    Category.NPC,       1f,  0.3f, 0.3f, "Anywhere"),

    // ── Megastructures ────────────────────────────────────────────────────
    SHIP          ("Ship",          Category.NPC,       5000f, 15f,  8f,   "Sky"),
    STALWART_SHIP ("Stalwart Ship", Category.NPC,       5000f, 10f,  5f,   "Sky"),
    SHIP_MISSILE  ("Archer Missile", Category.NPC, 10f, 0.4f, 0.4f, "Weapon"),

    // ── Meta ──────────────────────────────────────────────────────────────
    PLAYER  ("Player",  Category.NPC,       100f, 0.6f, 1.8f, "Spawn");

    public enum Category { PASSIVE, PREDATOR, NPC, MONSTER }

    public final String   displayName;
    public final Category category;
    /** Base max health */
    public final float    baseHealth;
    /** AABB half-width */
    public final float    bodyWidth;
    /** AABB full height */
    public final float    bodyHeight;
    /** Natural spawn biome description */
    public final String   biome;

    EntityType(String displayName, Category category,
               float baseHealth, float bodyWidth, float bodyHeight, String biome) {
        this.displayName = displayName;
        this.category    = category;
        this.baseHealth  = baseHealth;
        this.bodyWidth   = bodyWidth;
        this.bodyHeight  = bodyHeight;
        this.biome       = biome;
    }

    public boolean isPassive()  { return category == Category.PASSIVE; }
    public boolean isPredator() { return category == Category.PREDATOR; }
    public boolean isNPC()      { return category == Category.NPC; }
}
