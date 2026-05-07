package minicraft.item;

import minicraft.world.Block;

/**
 * Base class for all collectible items.
 */
public class Item {

    /**
     * Item quality tiers, granted exclusively as quest rewards.
     * Each tier carries an efficiency multiplier for ToolItems and a display colour.
     */
    public enum QualityTier {
        RAMSHACKLE  ("Ramshackle",  0.80f, 0x888888),
        APPRENTICE  ("Apprentice",  1.00f, 0xEEEEEE),
        JOURNEYMAN  ("Journeyman",  1.30f, 0x22EE44),
        MASTERCRAFT ("MasterCraft", 1.60f, 0x1166FF),
        LEGENDARY   ("Legendary",   2.20f, 0xAA00FF),
        HEIRLOOM    ("Heirloom",    3.00f, 0xFFD700); // Animated gold

        public final String displayName;
        public final float  efficiencyMult;
        public final int    colour; // packed RGB for UI

        QualityTier(String displayName, float efficiencyMult, int colour) {
            this.displayName    = displayName;
            this.efficiencyMult = efficiencyMult;
            this.colour         = colour;
        }
    }

    private final String name;
    private final Block blockRepresentation;
    private final String textureName;
    private final int maxStackSize;
    private final QualityTier quality; // null = plain item (no quality tag)
    private int levelRequirement = 1;

    public Item(String name, Block block, String textureName, int maxStackSize) {
        this(name, block, textureName, maxStackSize, null);
    }

    public Item(String name, Block block, String textureName, int maxStackSize, QualityTier quality) {
        this.name = name;
        this.blockRepresentation = block;
        this.textureName = textureName;
        this.maxStackSize = maxStackSize;
        this.quality = quality;
    }

    public Item(String name, Block block) {
        this(name, block, null, 64);
    }

    public Item(String name) {
        this(name, null, null, 64);
    }

    public String getName() { return name; }
    public Block getBlock() { return blockRepresentation; }
    public boolean isBlock() { return blockRepresentation != null; }
    public String getTextureName() { return textureName; }
    public int getMaxStackSize() { return maxStackSize; }
    public QualityTier getQuality() { return quality; }
    public boolean hasQuality() { return quality != null; }
    
    public int getLevelRequirement() { return levelRequirement; }
    public void setLevelRequirement(int level) { this.levelRequirement = level; }

    public String getDisplayName() {
        if (name == null || name.isEmpty()) return "Unknown Item";
        
        // Handle underscore to space and title case
        String[] parts = name.replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
        }
        // Prepend quality prefix
        if (quality != null) {
            return quality.displayName + " " + sb.toString();
        }
        return sb.toString();
    }

    /**
     * Returns extra information about the item's power level (Tier, Level, etc.)
     * Returns null if no tier information is applicable.
     */
    public String getTierInfo() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return name.equals(item.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
