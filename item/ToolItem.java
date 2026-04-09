package minicraft.item;

import minicraft.world.Block;

/**
 * Specialized items for harvesting resources.
 */
public class ToolItem extends Item {

    public enum ToolType { PICKAXE, AXE, SHOVEL, SWORD }

    private final ToolType toolType;
    private final int harvestLevel;
    private final float efficiency;
    private final String textureName;

    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName) {
        super(name, null); // Tools aren't placeable blocks
        this.toolType = type;
        this.harvestLevel = level;
        this.efficiency = efficiency;
        this.textureName = textureName;
    }

    public ToolType getToolType() { return toolType; }
    public int getHarvestLevel() { return harvestLevel; }
    public float getEfficiency() { return efficiency; }
    public String getTextureName() { return textureName; }
}
