package minicraft.entity;

import minicraft.world.Block;
import minicraft.item.Item;
import minicraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores collected blocks and items in fixed slots.
 */
public class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int INV_SIZE = 27;

    private final ItemStack[] hotbar = new ItemStack[HOTBAR_SIZE];
    private final ItemStack[] mainInventory = new ItemStack[INV_SIZE];
    
    private int selectedIndex = 0;
    private Item offhandItem = null;
    private ItemStack cursorStack = null; // Item currently being dragged

    // Armor Slots
    private minicraft.item.ArmorItem helmet = null;
    private minicraft.item.ArmorItem chestplate = null;
    private minicraft.item.ArmorItem leggings = null;
    private minicraft.item.ArmorItem boots = null;

    public Inventory() {
        for (int i = 0; i < HOTBAR_SIZE; i++) hotbar[i] = null;
        for (int i = 0; i < INV_SIZE; i++) mainInventory[i] = null;
    }

    public void add(Block block, int count) {
        if (block == null || block == Block.AIR) return;
        add(new Item(block.name(), block), count);
    }

    public void add(Item item, int count) {
        if (item == null) return;
        
        // 1. Try to stack in hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i] != null && hotbar[i].getItem().equals(item)) {
                hotbar[i].add(count);
                return;
            }
        }
        // 2. Try to stack in main inv
        for (int i = 0; i < INV_SIZE; i++) {
            if (mainInventory[i] != null && mainInventory[i].getItem().equals(item)) {
                mainInventory[i].add(count);
                return;
            }
        }
        // 3. Find empty hotbar slot
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i] == null) {
                hotbar[i] = new ItemStack(item, count);
                return;
            }
        }
        // 4. Find empty main inv slot
        for (int i = 0; i < INV_SIZE; i++) {
            if (mainInventory[i] == null) {
                mainInventory[i] = new ItemStack(item, count);
                return;
            }
        }
    }

    public int getCount(Item item) {
        int total = 0;
        for (ItemStack s : hotbar) if (s != null && s.getItem().equals(item)) total += s.getCount();
        for (ItemStack s : mainInventory) if (s != null && s.getItem().equals(item)) total += s.getCount();
        return total;
    }

    public boolean has(Item item, int count) {
        return getCount(item) >= count;
    }

    public void remove(Item item, int count) {
        int remaining = count;
        // Search main inv first
        for (int i = 0; i < INV_SIZE && remaining > 0; i++) {
            if (mainInventory[i] != null && mainInventory[i].getItem().equals(item)) {
                int take = Math.min(remaining, mainInventory[i].getCount());
                mainInventory[i].setCount(mainInventory[i].getCount() - take);
                remaining -= take;
                if (mainInventory[i].getCount() <= 0) mainInventory[i] = null;
            }
        }
        // Then hotbar
        for (int i = 0; i < HOTBAR_SIZE && remaining > 0; i++) {
            if (hotbar[i] != null && hotbar[i].getItem().equals(item)) {
                int take = Math.min(remaining, hotbar[i].getCount());
                hotbar[i].setCount(hotbar[i].getCount() - take);
                remaining -= take;
                if (hotbar[i].getCount() <= 0) hotbar[i] = null;
            }
        }
    }

    public Item getSelectedItem() {
        ItemStack s = hotbar[selectedIndex];
        return (s == null) ? null : s.getItem();
    }

    public void changeSelection(int delta) {
        selectedIndex = (selectedIndex + delta) % HOTBAR_SIZE;
        if (selectedIndex < 0) selectedIndex += HOTBAR_SIZE;
    }

    public Block getSelectedBlock() {
        Item item = getSelectedItem();
        return (item == null) ? null : item.getBlock();
    }

    public void remove(Block block, int count) {
        if (block == null) return;
        remove(new Item(block.name(), block), count);
    }

    public void clickSlot(int index, boolean isHotbar) {
        ItemStack[] target = isHotbar ? hotbar : mainInventory;
        if (index < 0 || index >= target.length) return;

        ItemStack clicked = target[index];
        
        // SWAP LOGIC
        target[index] = cursorStack;
        cursorStack = clicked;
    }

    public ItemStack[] getHotbar() { return hotbar; }
    public ItemStack[] getMainInventory() { return mainInventory; }
    public ItemStack getCursorStack() { return cursorStack; }
    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < HOTBAR_SIZE) this.selectedIndex = index;
    }

    // Armor & Defense (Unchanged Logic, just using ItemStack helper if needed in future)
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
        return Math.min(0.95f, total);
    }
    public minicraft.item.ArmorItem getHelmet() { return helmet; }
    public minicraft.item.ArmorItem getChestplate() { return chestplate; }
    public minicraft.item.ArmorItem getLeggings() { return leggings; }
    public minicraft.item.ArmorItem getBoots() { return boots; }
    public Item getOffhandItem() { return offhandItem; }
    public void setOffhandItem(Item item) { this.offhandItem = item; }

    public boolean hasTorchEquipped() {
        Item selected = getSelectedItem();
        if (selected != null && selected.getName().equalsIgnoreCase("TORCH")) return true;
        if (offhandItem != null && offhandItem.getName().equalsIgnoreCase("TORCH")) return true;
        return false;
    }
}
