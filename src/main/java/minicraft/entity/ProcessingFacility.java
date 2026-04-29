package minicraft.entity;

import minicraft.item.ItemStack;
import java.io.Serializable;

/**
 * Stores the state of a functional facility like a Furnace or Cooker.
 * 
 * Contains three slots:
 * 0: Input (to be processed)
 * 1: Fuel (energy source)
 * 2: Output (result)
 */
public class ProcessingFacility implements Serializable {

    private final ItemStack[] slots = new ItemStack[3]; // 0:In, 1:Fuel, 2:Out
    
    public float remainingFuelTime = 0f;
    public float maxFuelTime = 1f;
    public float fuelEfficiency = 1.0f;
    public float processProgress = 0f;
    public boolean isActive = false;

    public ProcessingFacility() {
        for (int i = 0; i < 3; i++) slots[i] = null;
    }

    public ItemStack getSlot(int i) {
        if (i < 0 || i >= 3) return null;
        return slots[i];
    }

    public void setSlot(int i, ItemStack stack) {
        if (i >= 0 && i < 3) slots[i] = stack;
    }

    /**
     * Swaps or places an item into the slot.
     */
    public ItemStack clickSlot(int i, ItemStack cursor) {
        if (i < 0 || i >= 3) return cursor;
        ItemStack clicked = slots[i];
        slots[i] = cursor;
        return clicked;
    }

    public boolean hasFuel() { return remainingFuelTime > 0; }
    public float getProgress() { return processProgress; }
    public float getFuelRatio() { return remainingFuelTime / maxFuelTime; }
}
