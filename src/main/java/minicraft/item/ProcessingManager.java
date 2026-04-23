package minicraft.item;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages processing recipes for the Furnace and Cooker.
 */
public class ProcessingManager {

    private final Map<String, Recipe> furnaceRecipes = new HashMap<>();
    private final Map<String, Recipe> cookerRecipes = new HashMap<>();
    private final Map<String, Float> fuelValues = new HashMap<>();

    public ProcessingManager() {
        initialize();
    }

    private void initialize() {
        // --- 1. Fuel Registry ---
        fuelValues.put("COAL", 30.0f);        
        fuelValues.put("COAL_ORE", 30.0f);    
        fuelValues.put("COAL_BLOCK", 300.0f); 
        fuelValues.put("CHARCOAL", 25.0f);
        fuelValues.put("LOG", 15.0f);
        fuelValues.put("STICK", 5.0f);
        fuelValues.put("WOOD", 15.0f);
        fuelValues.put("PLANK", 10.0f);
        
        // Wood Variants (Handling block name suffixes)
        fuelValues.put("OAK_WOOD", 15.0f);
        fuelValues.put("REDWOOD_WOOD", 20.0f);
        fuelValues.put("JUNGLE_WOOD", 18.0f);
        fuelValues.put("MANGO_WOOD", 15.0f);
        fuelValues.put("APPLE_WOOD", 15.0f);
        fuelValues.put("PEAR_WOOD", 15.0f);

        // --- 2. Furnace Registry (Ores -> Ingots/Gems) ---
        addFurnaceRecipe("IRON_ORE", "IRON_INGOT", 5.0f);
        addFurnaceRecipe("IRON_BLOCK", "IRON_INGOT", 5.0f); // Alias
        addFurnaceRecipe("COPPER_ORE", "COPPER_INGOT", 4.0f);
        addFurnaceRecipe("TIN_ORE", "TIN_INGOT", 4.0f);
        addFurnaceRecipe("GOLD_ORE", "GOLD_INGOT", 6.0f);
        addFurnaceRecipe("SILVER_ORE", "SILVER_INGOT", 6.0f);
        addFurnaceRecipe("NICKEL_ORE", "NICKEL_INGOT", 6.0f);
        addFurnaceRecipe("PLATINUM_ORE", "PLATINUM_INGOT", 12.0f);
        addFurnaceRecipe("TITANIUM_ORE", "TITANIUM_INGOT", 10.0f);
        addFurnaceRecipe("TUNGSTEN_ORE", "TUNGSTEN_INGOT", 12.0f);
        
        // Gems & Crystals
        addFurnaceRecipe("DIAMOND_ORE", "DIAMOND", 12.0f);
        addFurnaceRecipe("EMERALD_ORE", "EMERALD", 10.0f);
        addFurnaceRecipe("RUBY_ORE", "RUBY", 10.0f);
        addFurnaceRecipe("SAPPHIRE_ORE", "SAPPHIRE", 10.0f);
        addFurnaceRecipe("TOPAZ_ORE", "TOPAZ", 8.0f);
        addFurnaceRecipe("AQUAMARINE_ORE", "AQUAMARINE", 8.0f);
        addFurnaceRecipe("AMETHYST_ORE", "AMETHYST", 8.0f);
        addFurnaceRecipe("QUARTZ_ORE", "QUARTZ", 5.0f);
        addFurnaceRecipe("LAPIS_ORE", "LAPIS", 4.0f);
        addFurnaceRecipe("TANZANITE_ORE", "TANZANITE", 10.0f);
        
        // Advanced/Atomic/Legendary
        addFurnaceRecipe("URANIUM_ORE", "URANIUM_INGOT", 15.0f);
        addFurnaceRecipe("PLUTONIUM_ORE", "PLUTONIUM_INGOT", 20.0f);
        addFurnaceRecipe("ADAMANTINE_ORE", "ADAMANTINE_INGOT", 30.0f);
        addFurnaceRecipe("MITHRIL_ORE", "MITHRIL_INGOT", 25.0f);
        addFurnaceRecipe("NEPTUNIUM_ORE", "NEPTUNIUM_INGOT", 25.0f);

        // Legendary Gems (Industrial Refining)
        addFurnaceRecipe("AGATE_ORE", "AGATE", 15.0f);
        addFurnaceRecipe("GARNET_ORE", "GARNET", 15.0f);
        addFurnaceRecipe("TOURMALINE_ORE", "TOURMALINE", 18.0f);
        addFurnaceRecipe("OPAL_ORE", "OPAL", 20.0f);
        addFurnaceRecipe("ALEXANDRITE_ORE", "ALEXANDRITE", 25.0f);
        addFurnaceRecipe("ONYX_ORE", "ONYX", 35.0f);

        // Absolute Rare Minerals (Fusion Level Refining)
        addFurnaceRecipe("PAINITE_ORE", "PAINITE", 50.0f);
        addFurnaceRecipe("MUSGRAVITE_ORE", "MUSGRAVITE", 60.0f);
        addFurnaceRecipe("TAAFFEITE_ORE", "TAAFFEITE", 70.0f);
        addFurnaceRecipe("GRANDIDIERITE_ORE", "GRANDIDIERITE", 80.0f);
        addFurnaceRecipe("SERENDIBITE_ORE", "SERENDIBITE", 100.0f);

        // --- 3. Cooker Registry (Raw -> Cooked) ---
        addCookerRecipe("RAW_MEAT", "COOKED_MEAT", 4.0f);
        addCookerRecipe("RAW_FISH", "COOKED_FISH", 3.0f);
        addCookerRecipe("RAW_CHICKEN", "COOKED_CHICKEN", 4.0f);
    }

    private String normalize(String name) {
        if (name == null) return null;
        return name.toUpperCase().trim().replace(" ", "_");
    }

    private void addFurnaceRecipe(String input, String output, float time) {
        String tex = null;
        String normalizedInput = normalize(input);

        // High-fidelity texture mapping - Ores and Gems
        if (output.contains("IRON")) tex = "item_ingot_iron_standalone";
        else if (output.contains("GOLD")) tex = "item_ingot_gold_standalone";
        else if (output.contains("TITANIUM")) tex = "item_ingot_titanium_standalone";
        else if (output.contains("TIN")) tex = "item_ingot_titanium_standalone"; // Shared icon for now
        else if (output.contains("COPPER")) tex = "item_ingot_gold_standalone"; // Shared icon for now
        else if (output.contains("SILVER")) tex = "item_ingot_iron_standalone"; // Shared icon for now
        else if (output.contains("URANIUM")) tex = "item_pick_uranium"; 
        else if (output.contains("PLUTONIUM")) tex = "item_pick_uranium";
        else if (output.contains("DIAMOND")) tex = "item_gem_diamond_standalone_v2";
        else if (output.contains("RUBY")) tex = "item_gem_ruby_standalone_v2";
        else if (output.contains("SAPPHIRE")) tex = "item_ingot_sapphire_standalone";
        else if (output.contains("EMERALD")) tex = "item_gem_emerald_standalone_v2";
        else if (output.contains("TOPAZ")) tex = "item_gem_topaz_standalone";
        else if (output.contains("AMETHYST")) tex = "item_gem_amethyst_standalone";
        else if (output.contains("AQUAMARINE")) tex = "item_gem_aquamarine_standalone";
        else if (output.contains("TANZANITE")) tex = "item_gem_amethyst_standalone";
        else if (output.contains("QUARTZ")) tex = "item_quartz_shard";
        else if (output.contains("MITHRIL")) tex = "item_ingot_sapphire_standalone";
        else if (output.contains("ADAMANTINE")) tex = "item_ingot_titanium_standalone";
        
        // Rare Gem Fallbacks
        else if (output.contains("ONYX")) tex = "item_ingot_titanium_standalone";
        else if (output.contains("ALEXANDRITE")) tex = "item_gem_amethyst_standalone";
        else if (output.contains("OPAL")) tex = "item_gem_aquamarine_standalone";
        else if (output.contains("PAINITE") || output.contains("GARNET")) tex = "item_gem_ruby_standalone_v2";
        else if (output.contains("TOURMALINE")) tex = "item_gem_emerald_standalone_v2";
        else if (output.contains("SERENDIBITE")) tex = "item_ingot_sapphire_standalone";
        else tex = "item_gem_topaz_standalone"; // Default legendary fallback

        furnaceRecipes.put(normalizedInput, new Recipe(output, Recipe.Category.BLOCKS, null, new Item(output, null, tex, 64), 1, time));
    }

    private void addCookerRecipe(String input, String output, float time) {
        cookerRecipes.put(normalize(input), new Recipe(output, Recipe.Category.SURVIVAL, null, new Item(output, null, "item_food_cooked", 64), 1, time));
    }

    public Recipe getFurnaceResult(String inputName) {
        return furnaceRecipes.get(normalize(inputName));
    }

    public Recipe getCookerResult(String inputName) {
        return cookerRecipes.get(normalize(inputName));
    }

    public float getFuelTime(String name, boolean isCooker) {
        String normalizedName = normalize(name);
        if (normalizedName == null) return 0;
        
        Float val = fuelValues.get(normalizedName);
        if (val == null) return 0;
        
        // Efficiency: Cooker gets 2x duration from fuel sources
        return isCooker ? val * 2.0f : val;
    }

    public float getProcessTime(String inputName) {
        Recipe r = getFurnaceResult(inputName);
        if (r == null) r = getCookerResult(inputName);
        return (r != null) ? r.getProcessTime() : 5.0f;
    }
}
