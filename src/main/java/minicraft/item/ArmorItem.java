package minicraft.item;

/**
 * Represents a piece of wearable equipment.
 */
public class ArmorItem extends Item {

    public enum ArmorSlot { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    private final ArmorSlot slot;
    private final float protection; // 0.0 to 1.0 (percent)
    private final String textureName;
    private final String tierName;
    private final float healthBonus;    // 0.0 = none, 1.0 = +100%
    private final float speedModifier;  // 1.0 = normal, 0.8 = 20% slower
    private final float insulation;     // Resistance to temperature stress
    private final minicraft.math.Vector3f glowColor; // For radiation effects (nullable)

    public ArmorItem(String name, ArmorSlot slot, float protection, String textureName,
                     String tierName, float healthBonus, float speedModifier, 
                     float insulation, minicraft.math.Vector3f glowColor) {
        super(name);
        this.slot = slot;
        this.protection = protection;
        this.textureName = textureName;
        this.tierName = tierName;
        this.healthBonus = healthBonus;
        this.speedModifier = speedModifier;
        this.insulation = insulation;
        this.glowColor = glowColor;
    }

    public ArmorSlot getSlot() { return slot; }
    public float getProtection() { return protection; }
    public String getTextureName() { return textureName; }
    public String getTierName() { return tierName; }
    public float getHealthBonus() { return healthBonus; }
    public float getSpeedModifier() { return speedModifier; }
    public float getInsulation() { return insulation; }
    public minicraft.math.Vector3f getGlowColor() { return glowColor; }

    @Override
    public boolean isBlock() {
        return false;
    }

    @Override
    public String getTierInfo() {
        return tierName + " Tier (" + (int)(protection * 100) + "% Prot)";
    }
}
