package minicraft.item;

/**
 * Represents a piece of wearable equipment.
 */
public class ArmorItem extends Item {

    public enum ArmorSlot { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    private final ArmorSlot slot;
    private final float protection; // 0.0 to 1.0 (percent)
    private final String textureName;

    public ArmorItem(String name, ArmorSlot slot, float protection, String textureName) {
        super(name);
        this.slot = slot;
        this.protection = protection;
        this.textureName = textureName;
    }

    public ArmorSlot getSlot() {
        return slot;
    }

    public float getProtection() {
        return protection;
    }

    public String getTextureName() {
        return textureName;
    }

    @Override
    public boolean isBlock() {
        return false;
    }
}
