package minicraft.item;

import minicraft.world.Block;

/**
 * Specialized items for harvesting resources.
 */
public class ToolItem extends Item {

    public enum ToolType { PICKAXE, AXE, SHOVEL, SWORD }

    private final ToolType toolType;
    private final int harvestLevel;
    private final float baseEfficiency;
    private final String textureName;
    private final float percentDamage;
    private final boolean isInstaKill;
    private final float bossPercentDamage;

    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName) {
        this(name, type, level, efficiency, textureName, null, 0.0f, false, 0.0f);
    }

    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality) {
        this(name, type, level, efficiency, textureName, quality, 0.0f, false, 0.0f);
    }

    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality,
                    float percentDamage, boolean isInstaKill, float bossPercentDamage) {
        super(name, null, textureName, 1, quality);
        this.toolType = type;
        this.harvestLevel = level;
        this.baseEfficiency = efficiency;
        this.textureName = textureName;
        this.percentDamage = percentDamage;
        this.isInstaKill = isInstaKill;
        this.bossPercentDamage = bossPercentDamage;
    }

    public ToolType getToolType() { return toolType; }
    public int getHarvestLevel()  { return harvestLevel; }
    public float getPercentDamage() { return percentDamage; }
    public boolean isInstaKill() { return isInstaKill; }
    public float getBossPercentDamage() { return bossPercentDamage; }

    /** Returns efficiency scaled by quality tier multiplier (if any). */
    public float getEfficiency() {
        QualityTier q = getQuality();
        return q != null ? baseEfficiency * q.efficiencyMult : baseEfficiency;
    }

    public String getTextureName() { return textureName; }
}
