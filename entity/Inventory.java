package minicraft.entity;

import minicraft.world.Block;
import minicraft.item.Item;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores collected blocks for a player.
 */
public class Inventory {

    private final Map<Item, Integer> items = new LinkedHashMap<>();
    private final Map<minicraft.world.Block, Item> blockItemCache = new LinkedHashMap<>();
    private int selectedIndex = 0;
    private Item offhandItem = null;

    // Armor Slots
    private minicraft.item.ArmorItem helmet = null;
    private minicraft.item.ArmorItem chestplate = null;
    private minicraft.item.ArmorItem leggings = null;
    private minicraft.item.ArmorItem boots = null;

    public void equip(minicraft.item.ArmorItem armor) {
        if (armor == null) return;
        switch (armor.getSlot()) {
            case HELMET:     helmet = armor; break;
            case CHESTPLATE: chestplate = armor; break;
            case LEGGINGS:   leggings = armor; break;
            case BOOTS:      boots = armor; break;
        }
    }

    public float getTotalDefense() {
        float total = 0;
        if (helmet != null) total += helmet.getProtection();
        if (chestplate != null) total += chestplate.getProtection();
        if (leggings != null) total += leggings.getProtection();
        if (boots != null) total += boots.getProtection();
        return Math.min(0.95f, total); // Max 95% protection
    }

    public minicraft.item.ArmorItem getHelmet() { return helmet; }
    public minicraft.item.ArmorItem getChestplate() { return chestplate; }
    public minicraft.item.ArmorItem getLeggings() { return leggings; }
    public minicraft.item.ArmorItem getBoots() { return boots; }

    public void add(Block block, int count) {
        if (block == null || block == Block.AIR) return;
        add(getBlockItem(block), count);
    }

    public void add(Item item, int count) {
        if (item == null) return;
        items.put(item, items.getOrDefault(item, 0) + count);
    }

    public boolean has(Block block, int count) {
        return has(getBlockItem(block), count);
    }

    public boolean has(Item item, int count) {
        return items.getOrDefault(item, 0) >= count;
    }

    public void remove(Block block, int count) {
        remove(getBlockItem(block), count);
    }

    public void remove(Item item, int count) {
        int current = items.getOrDefault(item, 0);
        if (current >= count) {
            items.put(item, current - count);
            if (items.get(item) <= 0) {
                items.remove(item);
            }
        }
    }

    public Item getSelectedItem() {
        if (items.isEmpty()) return null;
        List<Item> keys = new ArrayList<>(items.keySet());
        if (selectedIndex < 0) selectedIndex = keys.size() - 1;
        if (selectedIndex >= keys.size()) selectedIndex = 0;
        return keys.get(selectedIndex);
    }

    public Block getSelectedBlock() {
        Item selected = getSelectedItem();
        if (selected != null && selected.isBlock()) {
            return selected.getBlock();
        }
        return null;
    }

    public void changeSelection(int delta) {
        if (items.isEmpty()) return;
        selectedIndex += delta;
        List<Item> keys = new ArrayList<>(items.keySet());
        if (selectedIndex < 0) selectedIndex = keys.size() - 1;
        if (selectedIndex >= keys.size()) selectedIndex = 0;
    }

    private Item getBlockItem(Block b) {
        return blockItemCache.computeIfAbsent(b, block -> new Item(block.name(), block));
    }

    public Map<Item, Integer> getItems() {
        return items;
    }

    public int getCount(Item item) {
        return items.getOrDefault(item, 0);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public Item getOffhandItem() {
        return offhandItem;
    }

    public void setOffhandItem(Item item) {
        this.offhandItem = item;
    }

    public boolean hasTorchEquipped() {
        // If the offhand item is a torch, or if the main item is a torch
        if (offhandItem != null && offhandItem.getName().equalsIgnoreCase("TORCH")) return true;
        Item main = getSelectedItem();
        return main != null && main.getName().equalsIgnoreCase("TORCH");
    }
}
