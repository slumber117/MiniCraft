package minicraft.item;

import minicraft.world.Block;

/**
 * Specialized items for harvesting resources.
 */
public class ToolItem extends Item {

    public enum ToolType { PICKAXE, AXE, SHOVEL, SWORD, BATTLE_AXE }

    private final ToolType toolType;
    private final int harvestLevel;
    private final float baseEfficiency;
    private final String textureName;
    private final float percentDamage;
    private final boolean isInstaKill;
    private final float bossPercentDamage;
    
    public minicraft.math.Vector3f auraColor = null;
    public float radioactiveBonus = 1.0f;
    public float attackSpeedMultiplier = 1.0f;
    public float doubleDropChance = 0.0f;
 
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName) {
        this(name, type, level, efficiency, textureName, null, 0.0f, false, 0.0f, null, 1.0f, 1.0f, 0.0f);
    }
 
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality) {
        this(name, type, level, efficiency, textureName, quality, 0.0f, false, 0.0f, null, 1.0f, 1.0f, 0.0f);
    }
 
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality,
                    float percentDamage, boolean isInstaKill, float bossPercentDamage) {
        this(name, type, level, efficiency, textureName, quality, percentDamage, isInstaKill, bossPercentDamage, null, 1.0f, 1.0f, 0.0f);
    }
 
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality,
                    float percentDamage, boolean isInstaKill, float bossPercentDamage,
                    minicraft.math.Vector3f auraColor, float radioactiveBonus, float attackSpeedMultiplier) {
        this(name, type, level, efficiency, textureName, quality, percentDamage, isInstaKill, bossPercentDamage, auraColor, radioactiveBonus, attackSpeedMultiplier, 0.0f);
    }
 
    public ToolItem(String name, ToolType type, int level, float efficiency, String textureName, QualityTier quality,
                    float percentDamage, boolean isInstaKill, float bossPercentDamage,
                    minicraft.math.Vector3f auraColor, float radioactiveBonus, float attackSpeedMultiplier, float doubleDropChance) {
        super(name, null, textureName, 1, quality);
        this.toolType = type;
        this.harvestLevel = level;
        this.baseEfficiency = efficiency;
        this.textureName = textureName;
        this.percentDamage = percentDamage;
        this.isInstaKill = isInstaKill;
        this.bossPercentDamage = bossPercentDamage;
        this.auraColor = auraColor;
        this.radioactiveBonus = radioactiveBonus;
        this.attackSpeedMultiplier = attackSpeedMultiplier;
        this.doubleDropChance = doubleDropChance;
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
    
    @Override
    public String getTierInfo() {
        return "TIER " + harvestLevel;
    }
}
