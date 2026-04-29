package minicraft.item;

/**
 * Represents an edible item that restores health and hunger.
 */
public class FoodItem extends Item {

    private final float healthRestored;
    private final float hungerRestored;

    public FoodItem(String name, String textureName, float healthRestored, float hungerRestored) {
        super(name, null, textureName, 64);
        this.healthRestored = healthRestored;
        this.hungerRestored = hungerRestored;
    }

    public float getHealthRestored() { return healthRestored; }
    public float getHungerRestored() { return hungerRestored; }
}
