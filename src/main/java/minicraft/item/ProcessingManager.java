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
        fuelValues.put("COAL", 40.0f);        
        fuelValues.put("COAL_ORE", 40.0f);    
        fuelValues.put("COAL_BLOCK", 400.0f); 
        fuelValues.put("URANIUM_ORE", 80.0f);
        fuelValues.put("PLUTONIUM_ORE", 150.0f);
        fuelValues.put("PROMETHIUM_ORE", 500.0f); // Legendary fuel source
        fuelValues.put("PROMETHIUM", 1000.0f); // Purest energy source
        fuelValues.put("CHARCOAL", 30.0f);
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
        addFurnaceRecipe("IRON_BLOCK", "IRON_INGOT", 5.0f); 
        addFurnaceRecipe("COPPER_ORE", "COPPER_INGOT", 4.0f);
        addFurnaceRecipe("TIN_ORE", "TIN_INGOT", 4.0f);
        addFurnaceRecipe("GOLD_ORE", "GOLD_INGOT", 6.0f);
        addFurnaceRecipe("SILVER_ORE", "SILVER_INGOT", 6.0f);
        addFurnaceRecipe("NICKEL_ORE", "NICKEL_INGOT", 6.0f);
        addFurnaceRecipe("PLATINUM_ORE", "PLATINUM_INGOT", 12.0f);
        addFurnaceRecipe("TITANIUM_ORE", "TITANIUM_INGOT", 10.0f); // Slower
        addFurnaceRecipe("TUNGSTEN_ORE", "TUNGSTEN_INGOT", 12.0f); // Slower

        // Rare Earth Refining (Exotic Tiers)
        addFurnaceRecipe("XANTHIOSITE_ORE", "XANTHIOSITE", 5.0f);
        addFurnaceRecipe("MONAZITE_ORE", "MONAZITE", 25.0f);
        addFurnaceRecipe("BASTNAESITE_ORE", "BASTNAESITE", 30.0f);
        addFurnaceRecipe("XENOTIME_ORE", "XENOTIME", 35.0f);
        addFurnaceRecipe("LOPARITE_ORE", "LOPARITE", 40.0f);
        addFurnaceRecipe("TANTALITE_ORE", "TANTALITE", 45.0f);
        addFurnaceRecipe("VANADINITE_ORE", "VANADINITE", 50.0f);
        addFurnaceRecipe("GADOLINIUM_ORE", "GADOLINIUM", 55.0f);
        addFurnaceRecipe("TERBIUM_ORE", "TERBIUM", 60.0f);
        addFurnaceRecipe("DYSPROSIUM_ORE", "DYSPROSIUM", 65.0f);
        addFurnaceRecipe("HOLMIUM_ORE", "HOLMIUM", 70.0f);
        addFurnaceRecipe("ERBIUM_ORE", "ERBIUM", 75.0f);
        addFurnaceRecipe("YTTRIUM_ORE", "YTTRIUM", 80.0f);
        addFurnaceRecipe("LUTETIUM_ORE", "LUTETIUM", 85.0f);
        addFurnaceRecipe("SAMARIUM_ORE", "SAMARIUM", 90.0f);
        addFurnaceRecipe("NEODYMIUM_ORE", "NEODYMIUM", 95.0f);
        addFurnaceRecipe("PRASEODYMIUM_ORE", "PRASEODYMIUM", 100.0f);
        addFurnaceRecipe("CERIUM_ORE", "CERIUM", 110.0f);
        addFurnaceRecipe("LANTHANUM_ORE", "LANTHANUM", 120.0f);
        addFurnaceRecipe("PROMETHIUM_ORE", "PROMETHIUM", 150.0f); // Epic smelting time
        
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
        addFurnaceRecipe("URANIUM_ORE", "URANIUM_INGOT", 18.0f);
        addFurnaceRecipe("PLUTONIUM_ORE", "PLUTONIUM_INGOT", 25.0f);
        addFurnaceRecipe("ADAMANTINE_ORE", "ADAMANTINE", 35.0f);
        addFurnaceRecipe("MITHRIL_ORE", "MITHRIL_INGOT", 40.0f); // Much slower
        addFurnaceRecipe("NEPTUNIUM_ORE", "NEPTUNIUM_INGOT", 45.0f);

        // Absolute Rare Minerals (Fusion Level Refining)
        addFurnaceRecipe("ONYX_ORE", "ONYX", 15.0f);
        addFurnaceRecipe("PAINITE_ORE", "PAINITE", 60.0f);
        addFurnaceRecipe("MUSGRAVITE_ORE", "MUSGRAVITE", 75.0f);
        addFurnaceRecipe("TAAFFEITE_ORE", "TAAFFEITE", 90.0f);
        addFurnaceRecipe("GRANDIDIERITE_ORE", "GRANDIDIERITE", 110.0f);
        addFurnaceRecipe("SERENDIBITE_ORE", "SERENDIBITE", 150.0f); // Extremely slow

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
        else if (output.contains("PLUTONIUM")) tex = "item_ingot_plutonium";
        else if (output.contains("TAAFFEITE")) tex = "item_gem_taaffeite";
        else if (output.contains("DIAMOND")) tex = "item_gem_diamond_standalone_v2";
        else if (output.contains("RUBY")) tex = "item_gem_ruby_standalone_v2";
        else if (output.contains("SAPPHIRE")) tex = "item_ingot_sapphire_standalone";
        else if (output.contains("EMERALD")) tex = "item_gem_emerald_standalone_v2";
        else if (output.contains("TOPAZ")) tex = "item_gem_topaz_standalone";
        else if (output.contains("AMETHYST")) tex = "item_gem_amethyst_standalone";
        else if (output.contains("AQUAMARINE")) tex = "item_gem_aquamarine_standalone";
        else if (output.contains("TANZANITE")) tex = "item_gem_amethyst_standalone";
        else if (output.contains("QUARTZ")) tex = "item_quartz_shard";
        else if (output.contains("MITHRIL")) tex = "item_ingot_mithril";
        else if (output.contains("ADAMANTINE")) tex = "item_ingot_adamantine";
        
        // Rare Earth & Exotic Metal Textures
        else if (output.equals("XANTHIOSITE")) tex = "item_ingot_xanthiosite";
        else if (output.equals("PLATINUM_INGOT")) tex = "item_ingot_silver";
        else if (output.equals("MONAZITE")) tex = "item_ingot_monazite";
        else if (output.equals("BASTNAESITE")) tex = "item_ingot_bastnaesite";
        else if (output.equals("XENOTIME")) tex = "item_ingot_xenotime";
        else if (output.equals("LOPARITE")) tex = "item_ingot_loparite";
        else if (output.equals("TANTALITE") || output.equals("VANADINITE")) tex = "item_ingot_titanium_standalone";
        else if (output.equals("GADOLINIUM") || output.equals("TERBIUM") || output.equals("DYSPROSIUM") || output.equals("HOLMIUM")) tex = "item_ingot_mithril";
        else if (output.equals("ERBIUM") || output.equals("YTTRIUM") || output.equals("LUTETIUM") || output.equals("SAMARIUM")) tex = "item_ingot_mithril";
        else if (output.equals("NEODYMIUM") || output.equals("PRASEODYMIUM") || output.equals("CERIUM") || output.equals("LANTHANUM")) tex = "item_ingot_mithril";
        else if (output.equals("PROMETHIUM")) tex = "item_ingot_promethium";

        // Rare Gem Fallbacks
        else if (output.equals("ONYX")) tex = "item_gem_onyx";
        else if (output.equals("GRANDIDIERITE")) tex = "item_gem_grandidierite";
        else if (output.contains("ONYX")) tex = "item_gem_onyx";
        else if (output.contains("ALEXANDRITE")) tex = "item_gem_alexandrite";
        else if (output.contains("OPAL")) tex = "item_gem_aquamarine_standalone";
        else if (output.contains("PAINITE") || output.contains("GARNET")) tex = "item_gem_ruby_standalone_v2";
        else if (output.contains("TOURMALINE")) tex = "item_gem_emerald_standalone_v2";
        else if (output.contains("SERENDIBITE")) tex = "item_ingot_sapphire_standalone";

        // Food Fallbacks
        else if (output.contains("MEAT")) tex = "item_meat_cooked";
        else if (output.contains("FISH")) tex = "item_fish_cooked";
        else if (output.contains("CHICKEN")) tex = "item_chicken_cooked";
        else if (output.contains("APPLE")) tex = "item_apple";
        else if (output.contains("MANGO")) tex = "item_mango";
        else if (output.contains("PEAR")) tex = "item_pear";
        else if (output.contains("BREAD")) tex = "item_bread";
        else tex = "item_gem_topaz_standalone"; // Default legendary fallback

        furnaceRecipes.put(normalizedInput, new Recipe(output, Recipe.Category.BLOCKS, null, new Item(output, null, tex, 64), 1, time));
    }

    private void addCookerRecipe(String input, String output, float time) {
        String tex = "item_food_cooked";
        float heal = 20f;
        float hunger = 30f;

        if (output.contains("COOKED_MEAT")) { tex = "item_meat_cooked"; heal = 25f; hunger = 40f; }
        else if (output.contains("COOKED_FISH")) { tex = "item_fish_cooked"; heal = 15f; hunger = 25f; }
        else if (output.contains("COOKED_CHICKEN")) { tex = "item_chicken_cooked"; heal = 20f; hunger = 35f; }
        else if (output.contains("BREAD")) { tex = "item_bread"; heal = 10f; hunger = 20f; }

        cookerRecipes.put(normalize(input), new Recipe(output, Recipe.Category.SURVIVAL, null, new FoodItem(output, tex, heal, hunger), 1, time));
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

    public float getFuelEfficiency(String name) {
        String normalizedName = normalize(name);
        if (normalizedName == null) return 1.0f;
        
        if (normalizedName.equals("URANIUM_ORE")) return 1.25f;
        if (normalizedName.equals("PLUTONIUM_ORE")) return 1.875f;
        
        return 1.0f; // Standard efficiency
    }

    public float getProcessTime(String inputName) {
        Recipe r = getFurnaceResult(inputName);
        if (r == null) r = getCookerResult(inputName);
        return (r != null) ? r.getProcessTime() : 5.0f;
    }
}
