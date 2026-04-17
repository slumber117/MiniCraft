package minicraft.item;

import minicraft.world.Block;

/**
 * Base class for all collectible items.
 */
public class Item {
    private final String name;
    private final Block blockRepresentation;
    private final String textureName;
    private final int maxStackSize;

    public Item(String name, Block block, String textureName, int maxStackSize) {
        this.name = name;
        this.blockRepresentation = block;
        this.textureName = textureName;
        this.maxStackSize = maxStackSize;
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
        return sb.toString();
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
