package minicraft.item;

import minicraft.world.Block;

/**
 * Base class for all collectible items.
 */
public class Item {
    private final String name;
    private final Block blockRepresentation;

    public Item(String name, Block block) {
        this.name = name;
        this.blockRepresentation = block;
    }

    public Item(String name) {
        this(name, null);
    }

    public String getName() { return name; }
    public Block getBlock() { return blockRepresentation; }
    public boolean isBlock() { return blockRepresentation != null; }

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
