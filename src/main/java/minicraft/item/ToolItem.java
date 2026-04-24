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

    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName) {
        this(name, type, level, efficiency, textureName, null);
    }

    /** Quality-aware constructor used by QuestReward. */
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality) {
        super(name, null, textureName, 1, quality);
        this.toolType       = type;
        this.harvestLevel   = level;
        this.baseEfficiency = efficiency;
        this.textureName    = textureName;
    }

    public ToolType getToolType() { return toolType; }
    public int getHarvestLevel()  { return harvestLevel; }

    /** Returns efficiency scaled by quality tier multiplier (if any). */
    public float getEfficiency() {
        QualityTier q = getQuality();
        return q != null ? baseEfficiency * q.efficiencyMult : baseEfficiency;
    }

    public String getTextureName() { return textureName; }
}
