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
            minicraft.item.Recipe res = isCooker 
                ? pm.getCookerResult(input.getItem().getName()) 
                : pm.getFurnaceResult(input.getItem().getName());

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
                        }
                    }

                    if (fac.remainingFuelTime > 0) {
                        fac.isActive = true;
                        // Progress is (Time Elapsed / Total Time Required for this specific item)
                        fac.processProgress += dt / pm.getProcessTime(input.getItem().getName());

                        if (fac.processProgress >= 1.0f) {
                            // Consume input
                            input.add(-1);
                            if (input.getCount() <= 0) fac.setSlot(0, null);

                            // Add result
                            if (output == null) {
                                fac.setSlot(2, new minicraft.item.ItemStack(res.getResult(), res.getResultCount()));
                            } else {
                                output.add(res.getResultCount());
                            }
                            fac.processProgress = 0;
                        }
                    } else {
                        fac.isActive = false;
                    }
                } else {
                    fac.isActive = false;
                }
            } else {
                fac.isActive = false;
                fac.processProgress = 0;
            }
        } else {
            fac.isActive = false;
            if (fac.remainingFuelTime > 0) fac.remainingFuelTime -= dt * 0.1f;
        }

        // 3. VISUAL SYNC
        if (wasActive != fac.isActive) {
            world.markChunkDirty(Math.floorDiv(fx, 16), Math.floorDiv(fz, 16));
        }
    }
}
