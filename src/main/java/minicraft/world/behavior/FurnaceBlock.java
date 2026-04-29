package minicraft.world.behavior;

import minicraft.Main;
import minicraft.world.World;
import minicraft.world.BlockInteraction;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles interaction logic for industrial smelting facilities.
 */
public class FurnaceBlock implements BlockInteraction {

    private final String label;
    private final String facilityType;

    public FurnaceBlock(String label, String facilityType) {
        this.label = label;
        this.facilityType = facilityType;
    }

    @Override
    public void onInteract(Main main, World world, int gx, int gy, int gz) {
        main.activeFacility = world.getFacility(gx, gy, gz);
        main.activeFacX = gx;
        main.activeFacY = gy;
        main.activeFacZ = gz;
        
        switch (facilityType) {
            case "FURNACE":
                main.furnaceOpen = true;
                break;
            case "COOKER":
                main.cookerOpen = true;
                break;
            case "ALLOY_FORGE":
                main.shipConsoleOpen = false; // Ensure no conflict
                // Note: ALLOY_FORGE currently uses furnace UI but could be branched here
                main.furnaceOpen = true; 
                break;
        }
        
        main.setStatusMessage(label + " LINKED");
        glfwSetInputMode(main.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    /**
     * Handles all mouse interaction for the facility screen.
     * Migrated from Main.java to keep industrial logic encapsulated.
     */
    public static void handleClick(minicraft.entity.ProcessingFacility fac, minicraft.entity.Player player, Main main, float x, float y, int fbW, int fbH) {
        float panelW = 680f, panelH = 580f;
        float sx = (fbW - panelW) / 2f, sy = (fbH - panelH) / 2f;
        float cx = sx + panelW / 2f, cy = sy + 180;
        float slotSize = 72f;

        // ── 1. Facility Slots ──
        // Input
        if (x >= cx - 170 && x <= cx - 170 + slotSize && y >= cy - 36 && y <= cy - 36 + slotSize) {
            minicraft.item.ItemStack clicked = fac.getSlot(0);
            fac.setSlot(0, player.inventory.getCursorStack());
            player.inventory.setCursorStack(clicked);
            if (fac.getSlot(0) != null)
                main.setStatusMessage(fac.getSlot(0).getItem().getDisplayName());
            return;
        }
        // Fuel
        if (x >= cx - 36 && x <= cx - 36 + slotSize && y >= cy + 52 && y <= cy + 52 + slotSize) {
            minicraft.item.ItemStack clicked = fac.getSlot(1);
            fac.setSlot(1, player.inventory.getCursorStack());
            player.inventory.setCursorStack(clicked);
            if (fac.getSlot(1) != null)
                main.setStatusMessage(fac.getSlot(1).getItem().getDisplayName());
            return;
        }
        // Output
        if (x >= cx + 98 && x <= cx + 98 + slotSize && y >= cy - 36 && y <= cy - 36 + slotSize) {
            minicraft.item.ItemStack clicked = fac.getSlot(2);
            if (clicked != null && !clicked.isEmpty()
                    && (player.inventory.getCursorStack() == null || player.inventory.getCursorStack().isEmpty())) {
                // Quick Move to inventory if cursor is empty
                for (int i = 0; i < 27; i++) {
                    if (player.inventory.getMainInventory()[i] == null
                            || player.inventory.getMainInventory()[i].isEmpty()) {
                        player.inventory.getMainInventory()[i] = clicked;
                        fac.setSlot(2, null);
                        return;
                    }
                }
            }
            // Standard swap
            fac.setSlot(2, player.inventory.getCursorStack());
            player.inventory.setCursorStack(clicked);
            if (clicked != null)
                main.setStatusMessage(clicked.getItem().getDisplayName());
            return;
        }

        // ── 2. Player Inventory Grid inside Facility Screen ──
        final float ISLOT = 54f, IGAP = 6f;
        float invGridW = 9 * ISLOT + 8 * IGAP;
        float invStartX = sx + (panelW - invGridW) / 2f;
        float invStartY = sy + 300f;

        // Main 3x9
        for (int i = 0; i < 27; i++) {
            int r = i / 9, c = i % 9;
            float slotX = invStartX + c * (ISLOT + IGAP);
            float slotY = invStartY + r * (ISLOT + IGAP);
            if (x >= slotX && x <= slotX + ISLOT && y >= slotY && y <= slotY + ISLOT) {
                player.inventory.clickSlot(i, false);
                minicraft.item.ItemStack s = player.inventory.getMainInventory()[i];
                if (s != null && !s.isEmpty())
                    main.setStatusMessage(s.getItem().getDisplayName());
                return;
            }
        }

        // Hotbar 1x9
        float hotStartY = invStartY + 3 * (ISLOT + IGAP) + 18f;
        for (int i = 0; i < 9; i++) {
            float slotX = invStartX + i * (ISLOT + IGAP);
            if (x >= slotX && x <= slotX + ISLOT && y >= hotStartY && y <= hotStartY + ISLOT) {
                player.inventory.clickSlot(i, true);
                minicraft.item.ItemStack s = player.inventory.getHotbar()[i];
                if (s != null && !s.isEmpty())
                    main.setStatusMessage(s.getItem().getDisplayName());
                return;
            }
        }
    }

    public static void tick(minicraft.entity.ProcessingFacility fac, World world, int fx, int fy, int fz, float dt, minicraft.item.ProcessingManager pm) {
        minicraft.world.Block b = world.getBlock(fx, fy, fz);
        if (b != minicraft.world.Block.FURNACE && b != minicraft.world.Block.COOKER && b != minicraft.world.Block.ALLOY_FORGE) {
            fac.isActive = false;
            return;
        }
        
        boolean wasActive = fac.isActive;
        boolean isCooker = (b == minicraft.world.Block.COOKER);
        
        // 1. FUEL LOGIC
        if (fac.remainingFuelTime > 0) {
            fac.remainingFuelTime -= dt;
            if (fac.remainingFuelTime < 0) fac.remainingFuelTime = 0;
        }

        // 2. PROCESSING LOGIC
        minicraft.item.ItemStack input = fac.getSlot(0);
        minicraft.item.ItemStack fuel = fac.getSlot(1);
        minicraft.item.ItemStack output = fac.getSlot(2);

        if (input != null) {
            String inputName = input.getItem().getName();
            minicraft.item.Recipe res = isCooker 
                ? pm.getCookerResult(inputName) 
                : pm.getFurnaceResult(inputName);

            if (res != null) {
                // Check if output slot is compatible
                if (output == null || (output.getItem().equals(res.getResult()) && output.getCount() + res.getResultCount() <= 64)) {
                    
                    // Consume fuel if needed
                    if (fac.remainingFuelTime <= 0 && fuel != null) {
                        float fuelVal = pm.getFuelTime(fuel.getItem().getName(), isCooker);
                        if (fuelVal > 0) {
                            fac.maxFuelTime = fac.remainingFuelTime = fuelVal;
                            fuel.add(-1);
                            if (fuel.getCount() <= 0) fac.setSlot(1, null);
                            System.out.println("LOG: Facility (" + fx + "," + fy + "," + fz + ") consumed fuel. Total: " + fuelVal + "s");
                        }
                    }

                    if (fac.remainingFuelTime > 0) {
                        fac.isActive = true;
                        fac.processProgress += dt / pm.getProcessTime(inputName);

                        if (fac.processProgress >= 1.0f) {
                            System.out.println("LOG: Processing complete at (" + fx + "," + fy + "," + fz + "): " + inputName);
                            // Add result
                            minicraft.item.ItemStack currentOutput = fac.getSlot(2);
                            if (currentOutput == null) {
                                fac.setSlot(2, new minicraft.item.ItemStack(res.getResult(), res.getResultCount()));
                            } else if (currentOutput.getItem().equals(res.getResult())) {
                                currentOutput.add(res.getResultCount());
                            }
                            
                            // Consume input
                            input.add(-1);
                            if (input.getCount() <= 0) fac.setSlot(0, null);
                            fac.processProgress = 0;
                        }
                    } else {
                        fac.isActive = false;
                    }
                } else {
                    // Output slot full/incompatible - don't reset progress, just stop
                    fac.isActive = false; 
                }
            } else {
                // No recipe for this input - RESET progress
                fac.isActive = false; 
                fac.processProgress = 0;
            }
        } else {
            // No input - RESET progress and drain fuel slowly
            fac.isActive = false;
            if (fac.remainingFuelTime > 0) fac.remainingFuelTime -= dt * 0.15f; // Faster drain when idling
            fac.processProgress = 0;
        }

        // 3. VISUAL SYNC
        if (wasActive != fac.isActive) {
            world.markChunkDirty(Math.floorDiv(fx, 16), Math.floorDiv(fz, 16));
        }
    }
}
