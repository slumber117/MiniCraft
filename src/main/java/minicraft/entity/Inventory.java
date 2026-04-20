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
    public static final int INV_SIZE = 81;

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
        // Normalize empties to null so we never hold a ghost stack
        if (clicked != null && clicked.isEmpty()) clicked = null;
        if (cursorStack != null && cursorStack.isEmpty()) cursorStack = null;

        // Smart merging logic: if items match, combine them
        if (cursorStack != null && clicked != null && cursorStack.getItem().equals(clicked.getItem())) {
            int max = cursorStack.getItem().getMaxStackSize();
            int canAdd = max - clicked.getCount();
            int toAdd = Math.min(canAdd, cursorStack.getCount());
            
            clicked.add(toAdd);
            cursorStack.remove(toAdd);
            
            if (cursorStack.getCount() <= 0) cursorStack = null;
            target[index] = clicked;
            return;
        }

        // Standard Minecraft pick-up / place / swap
        target[index] = cursorStack;   // put what we're holding into the slot (may be null)
        cursorStack   = clicked;       // pick up what was in the slot (may be null)
    }

    public void clickArmorSlot(minicraft.item.ArmorItem.ArmorSlot type) {
        if (cursorStack != null && !cursorStack.isEmpty()) {
            Item item = cursorStack.getItem();
            if (item instanceof minicraft.item.ArmorItem) {
                minicraft.item.ArmorItem armor = (minicraft.item.ArmorItem) item;
                if (armor.getSlot() == type) {
                    // Swap armor
                    minicraft.item.ArmorItem old = null;
                    switch(type) {
                        case HELMET: old = helmet; helmet = armor; break;
                        case CHESTPLATE: old = chestplate; chestplate = armor; break;
                        case LEGGINGS: old = leggings; leggings = armor; break;
                        case BOOTS: old = boots; boots = armor; break;
                    }
                    cursorStack.remove(1);
                    if (cursorStack.isEmpty()) cursorStack = null;
                    
                    if (old != null) {
                        // If we had something equipped, it goes to cursor (Standard MC behavior: if cursor empty)
                        // Actually, for simplicity, if we were holding a stack of 1, we just swapped.
                        // If we were holding a stack > 1, the old one should go to inv or be dropped?
                        // Let's just assume armor is usually stack-size 1.
                        if (cursorStack == null) {
                            cursorStack = new ItemStack(old, 1);
                        } else {
                            add(old, 1); // Fallback: put back in inventory
                        }
                    }
                }
            }
        } else {
            // Pick up from armor slot
            minicraft.item.ArmorItem old = null;
            switch(type) {
                case HELMET: old = helmet; helmet = null; break;
                case CHESTPLATE: old = chestplate; chestplate = null; break;
                case LEGGINGS: old = leggings; leggings = null; break;
                case BOOTS: old = boots; boots = null; break;
            }
            if (old != null) {
                cursorStack = new ItemStack(old, 1);
            }
        }
    }

    public void quickMove(int index, boolean isHotbar) {
        ItemStack[] source = isHotbar ? hotbar : mainInventory;
        ItemStack[] target = isHotbar ? mainInventory : hotbar;
        if (index < 0 || index >= source.length || source[index] == null) return;

        ItemStack stack = source[index];
        
        // 1. Try to stack in target
        for (int i = 0; i < target.length; i++) {
            if (target[i] != null && target[i].getItem().equals(stack.getItem())) {
                target[i].add(stack.getCount());
                source[index] = null;
                return;
            }
        }
        
        // 2. Find empty slot in target
        for (int i = 0; i < target.length; i++) {
            if (target[i] == null) {
                target[i] = stack;
                source[index] = null;
                return;
            }
        }
    }

    public ItemStack[] getHotbar() { return hotbar; }
    public ItemStack[] getMainInventory() { return mainInventory; }
    public ItemStack getCursorStack() { return cursorStack; }
    public void setCursorStack(ItemStack stack) { this.cursorStack = stack; }
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

    /**
     * Returns the name of the tier if a full set is equipped, otherwise null.
     */
    public String getFullSetTier() {
        if (helmet == null || chestplate == null || leggings == null || boots == null) return null;
        String tier = helmet.getTierName();
        if (tier.equals(chestplate.getTierName()) && 
            tier.equals(leggings.getTierName()) && 
            tier.equals(boots.getTierName())) {
            return tier;
        }
        return null;
    }

    public float getSetMultiplier() {
        return (getFullSetTier() != null) ? 1.15f : 1.0f;
    }

    public float getTotalHealthBonus() {
        float sum = 0;
        if (helmet     != null) sum += helmet.getHealthBonus();
        if (chestplate != null) sum += chestplate.getHealthBonus();
        if (leggings   != null) sum += leggings.getHealthBonus();
        if (boots      != null) sum += boots.getHealthBonus();
        return sum * getSetMultiplier();
    }

    public float getTotalSpeedMod() {
        float sum = 0;
        // Speed is additive: 1.0 + sum(offset)
        if (helmet     != null) sum += (helmet.getSpeedModifier() - 1.0f);
        if (chestplate != null) sum += (chestplate.getSpeedModifier() - 1.0f);
        if (leggings   != null) sum += (leggings.getSpeedModifier() - 1.0f);
        if (boots      != null) sum += (boots.getSpeedModifier() - 1.0f);
        
        float total = 1.0f + sum;
        if (getFullSetTier() != null) {
            if (total < 1.0f) total += (1.0f - total) * 0.15f; 
            else              total *= 1.15f;                 
        }
        return Math.max(0.1f, total);
    }

    public float getTotalInsulation() {
        float sum = 0;
        if (helmet     != null) sum += helmet.getInsulation();
        if (chestplate != null) sum += chestplate.getInsulation();
        if (leggings   != null) sum += leggings.getInsulation();
        if (boots      != null) sum += boots.getInsulation();
        return sum * getSetMultiplier();
    }

    public minicraft.math.Vector3f getDominantGlow() {
        // Glow remains a full-set feature or just takes the highest contributing piece?
        // Let's stick to full-set for the "Aura" effect.
        String set = getFullSetTier();
        if (set == null) return null;
        return helmet.getGlowColor();
    }
    public minicraft.item.ArmorItem getHelmet() { return helmet; }
    public minicraft.item.ArmorItem getChestplate() { return chestplate; }
    public minicraft.item.ArmorItem getLeggings() { return leggings; }
    public minicraft.item.ArmorItem getBoots() { return boots; }
    public Item getOffhandItem() { return offhandItem; }
    public void setOffhandItem(Item item) {
        this.offhandItem = item;
    }

    public boolean hasFullSet(String tier) {
        if (helmet == null || chestplate == null || leggings == null || boots == null) return false;
        return helmet.getTierName().equalsIgnoreCase(tier) &&
               chestplate.getTierName().equalsIgnoreCase(tier) &&
               leggings.getTierName().equalsIgnoreCase(tier) &&
               boots.getTierName().equalsIgnoreCase(tier);
    }

    public boolean hasPiece(String tier) {
        if (helmet != null && helmet.getTierName().equalsIgnoreCase(tier)) return true;
        if (chestplate != null && chestplate.getTierName().equalsIgnoreCase(tier)) return true;
        if (leggings != null && leggings.getTierName().equalsIgnoreCase(tier)) return true;
        if (boots != null && boots.getTierName().equalsIgnoreCase(tier)) return true;
        return false;
    }

    public minicraft.math.Vector3f getTotalGlow() {
        if (getFullSetTier() == null) return new minicraft.math.Vector3f(0, 0, 0); // Full set required for radiation
        minicraft.math.Vector3f total = new minicraft.math.Vector3f(0, 0, 0);
        if (helmet != null && helmet.getGlowColor() != null) total.add(helmet.getGlowColor());
        if (chestplate != null && chestplate.getGlowColor() != null) total.add(chestplate.getGlowColor());
        if (leggings != null && leggings.getGlowColor() != null) total.add(leggings.getGlowColor());
        if (boots != null && boots.getGlowColor() != null) total.add(boots.getGlowColor());
        return total;
    }

    public boolean hasTorchEquipped() {
        Item selected = getSelectedItem();
        if (selected != null && selected.getName().equalsIgnoreCase("TORCH")) return true;
        if (offhandItem != null && offhandItem.getName().equalsIgnoreCase("TORCH")) return true;
        return false;
    }
}
