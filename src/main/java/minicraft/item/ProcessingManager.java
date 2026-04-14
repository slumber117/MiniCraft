package minicraft.item;

import minicraft.world.Block;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for smelting and cooking recipes.
 */
public class ProcessingManager {

    private final Map<String, Recipe> furnaceRecipes = new HashMap<>();
    private final Map<String, Recipe> cookerRecipes = new HashMap<>();
    private final Map<String, Float> fuelValues = new HashMap<>();

    public ProcessingManager() {
        initialize();
    }

    private void initialize() {
        // --- Fuel Values (Time in seconds) ---
        fuelValues.put("COAL_ORE", 30.0f); // Furnace base
        fuelValues.put("WOOD", 10.0f);
        fuelValues.put("WOOD_PLANKS", 15.0f);

        // --- Furnace Recipes (Ores -> Ingots) ---
        addFurnaceRecipe("IRON_ORE", "IRON_INGOT", 5.0f);
        addFurnaceRecipe("GOLD_ORE", "GOLD_INGOT", 7.0f);
        addFurnaceRecipe("COPPER_ORE", "COPPER_INGOT", 4.0f);
        addFurnaceRecipe("TIN_ORE", "TIN_INGOT", 4.0f);
        addFurnaceRecipe("SILVER_ORE", "SILVER_INGOT", 6.0f);
        addFurnaceRecipe("TITANIUM_ORE", "TITANIUM_INGOT", 12.0f);

        // --- Cooker Recipes (Raw -> Cooked) ---
        addCookerRecipe("RAW_MEAT", "COOKED_MEAT", 6.0f);
        addCookerRecipe("RAW_FISH", "COOKED_FISH", 4.0f);
    }

    private void addFurnaceRecipe(String input, String output, float time) {
        furnaceRecipes.put(input, new Recipe(output, Recipe.Category.BLOCKS, null, new Item(output), 1));
    }

    private void addCookerRecipe(String input, String output, float time) {
        cookerRecipes.put(input, new Recipe(output, Recipe.Category.SURVIVAL, null, new Item(output), 1));
    }

    public Recipe getFurnaceResult(String inputName) { return furnaceRecipes.get(inputName); }
    public Recipe getCookerResult(String inputName) { return cookerRecipes.get(inputName); }
    
    public float getFuelTime(String name, boolean isCooker) {
        Float val = fuelValues.get(name);
        if (val == null) return 0;
        // User requested: "cooker uses coal too but lasts longer, 60 seconds"
        // Furnace is 30s. So cooker gets 2x efficiency for coal.
        return isCooker ? val * 2.0f : val;
    }

    public float getProcessTime(String inputName) {
        // Standard process time
        return 5.0f; 
    }
}
