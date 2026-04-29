package minicraft.item;

import minicraft.entity.Inventory;
import minicraft.world.Block;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all game recipes and the crafting process.
 */
public class CraftingManager {

    private final List<Recipe> recipes = new ArrayList<>();

    public CraftingManager() {
        // --- 1. WOOD TIER (Basic Survival) ---
        addToolSet("Wood", "WOOD", 0, 2.0f);

        Map<Item, Integer> tableIng = new HashMap<>();
        tableIng.put(new Item("WOOD", Block.WOOD), 4);
        recipes.add(new Recipe("Crafting Table", Recipe.Category.BLOCKS, tableIng,
                new Item("CRAFTING_TABLE", Block.CRAFTING_TABLE), 1));

        Map<Item, Integer> torchIng = new HashMap<>();
        torchIng.put(new Item("WOOD", Block.WOOD), 1);
        recipes.add(new Recipe("Primitive Torch", Recipe.Category.SURVIVAL, torchIng,
                new Item("TORCH", Block.TORCH), 2));
        
        recipes.add(new Recipe("Tin Torch", Recipe.Category.SURVIVAL, Map.of(new Item("TIN_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("TIN_TORCH", Block.TIN_TORCH), 4));

        recipes.add(new Recipe("Iron Torch", Recipe.Category.SURVIVAL, Map.of(new Item("IRON_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("IRON_TORCH", Block.IRON_TORCH), 4));

        recipes.add(new Recipe("Gold Torch", Recipe.Category.SURVIVAL, Map.of(new Item("GOLD_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("GOLD_TORCH", Block.GOLD_TORCH), 8));

        recipes.add(new Recipe("Copper Torch", Recipe.Category.SURVIVAL, Map.of(new Item("COPPER_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("COPPER_TORCH", Block.COPPER_TORCH), 4));

        recipes.add(new Recipe("Nickel Torch", Recipe.Category.SURVIVAL, Map.of(new Item("NICKEL_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("NICKEL_TORCH", Block.NICKEL_TORCH), 4));

        recipes.add(new Recipe("Uranium Torch", Recipe.Category.SURVIVAL, Map.of(new Item("URANIUM_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("URANIUM_TORCH", Block.URANIUM_TORCH), 4));

        recipes.add(new Recipe("Plutonium Torch", Recipe.Category.SURVIVAL, Map.of(new Item("PLUTONIUM_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("PLUTONIUM_TORCH", Block.PLUTONIUM_TORCH), 4));

        // --- 2. STONE TIER ---
        addToolSet("Stone", "STONE", 1, 4.0f);

        Map<Item, Integer> furnaceIng = new HashMap<>();
        furnaceIng.put(new Item("STONE", Block.STONE), 8);
        recipes.add(new Recipe("Industrial Smelter", Recipe.Category.BLOCKS, furnaceIng,
                new Item("FURNACE", Block.FURNACE), 1));

        // --- 3. IRON TIER (Refinement Required) ---
        addArmorSet("Iron", "IRON_INGOT", 0.12f, 3, 0.3f, 0.85f, 0.2f, null);
        addToolSet("Iron", "IRON_INGOT", 2, 8.0f);

        // --- 4. GOLD TIER (Precision & Speed) ---
        addArmorSet("Gold", "GOLD_INGOT", 0.15f, 4, 0.2f, 1.15f, 0.3f, null); // Lightweight & Aerodynamic
        addToolSet("Gold", "GOLD_INGOT", 2, 15.0f); // Fast mining speed

        // --- 5. TITANIUM TIER (High Industry) ---
        addArmorSet("Titanium", "TITANIUM_INGOT", 0.20f, 3, 0.6f, 0.95f, 0.3f, null);
        addToolSet("Titanium", "TITANIUM_INGOT", 3, 12.0f);

        // --- 6. GEM & SPECIAL TIERS ---
        addArmorSet("Leather", "LEATHER", 0.05f, 5, 0.0f, 1.0f, 0.5f, null);
        addArmorSet("Bronze", "BRONZE_BLOCK", 0.14f, 2, 0.2f, 0.85f, 0.2f, null);
        addArmorSet("Diamond", "DIAMOND_ORE", 0.25f, 2, 1.0f, 1.0f, 0.4f, null);
        addToolSet("Diamond", "DIAMOND_ORE", 4, 16.0f);

        addArmorSet("Ruby", "RUBY_ORE", 0.22f, 3, 1.5f, 1.0f, 0.4f, null);
        addToolSet("Ruby", "RUBY_ORE", 5, 18.0f);

        addArmorSet("Quartz", "QUARTZ_ORE", 0.20f, 2, -0.1f, 0.65f, 0.5f, null); // Heavy Crystal Armor
        addToolSet("Quartz", "QUARTZ_ORE", 3, 11.0f); // Tier 3 Mining Capability

        addArmorSet("Tanzanite", "TANZANITE_ORE", 0.24f, 4, 0.8f, 1.0f, 0.3f, null); // Thorns Set
        addToolSet("Tanzanite", "TANZANITE_ORE", 4, 16.0f);

        addArmorSet("Sapphire", "SAPPHIRE_ORE", 0.22f, 3, 2.0f, 1.0f, 0.4f, null);
        addToolSet("Sapphire", "SAPPHIRE_ORE", 5, 22.0f);

        addArmorSet("Emerald", "EMERALD_ORE", 0.22f, 3, 0.5f, 1.1f, 0.4f, null);
        addToolSet("Emerald", "EMERALD_ORE", 4, 14.0f);

        addArmorSet("Topaz", "TOPAZ_ORE", 0.22f, 3, 0.8f, 1.0f, 0.4f, null);
        addToolSet("Topaz", "TOPAZ_ORE", 4, 15.0f);

        addArmorSet("Amethyst", "AMETHYST_ORE", 0.18f, 3, 0.2f, 1.0f, 0.3f, null);
        addToolSet("Amethyst", "AMETHYST_ORE", 3, 10.0f);

        addArmorSet("Aquamarine", "AQUAMARINE_ORE", 0.18f, 3, 0.4f, 1.05f, 0.2f, null);
        addToolSet("Aquamarine", "AQUAMARINE_ORE", 3, 11.0f);

        addArmorSet("Obsidian", "OBSIDIAN", 0.30f, 3, 0.8f, 0.85f, 0.6f, null);

        // Atomic Tiers
        addArmorSet("Uranium", "URANIUM_ORE", 0.25f, 4, 0.8f, 1.0f, 0.5f,
                new minicraft.math.Vector3f(0.2f, 1.0f, 0.2f)); // Green Glow
        addToolSet("Uranium", "URANIUM_ORE", 5, 20.0f);
        addArmorSet("Plutonium", "PLUTONIUM_ORE", 0.28f, 4, 1.0f, 1.0f, 0.5f,
                new minicraft.math.Vector3f(1.0f, 0.45f, 0.05f)); // Orange Glow
        addToolSet("Plutonium", "PLUTONIUM_ORE", 6, 25.0f);

        // --- 8. LEGENDARY GEM TIERS (Zenith Industry) ---
        addArmorSet("Agate", "AGATE_ORE", 0.20f, 3, 0.5f, 1.05f, 0.3f, null);
        addToolSet("Agate", "AGATE_ORE", 4, 14.0f);

        addArmorSet("Garnet", "GARNET_ORE", 0.22f, 3, 1.0f, 1.0f, 0.4f, null);
        addToolSet("Garnet", "GARNET_ORE", 4, 15.0f);

        addArmorSet("Tourmaline", "TOURMALINE_ORE", 0.22f, 3, 0.5f, 1.15f, 0.4f, null);
        addToolSet("Tourmaline", "TOURMALINE_ORE", 4, 16.0f);

        addArmorSet("Opal", "OPAL_ORE", 0.24f, 4, 2.0f, 1.0f, 0.5f, null);
        addToolSet("Opal", "OPAL_ORE", 5, 18.0f);

        addArmorSet("Alexandrite", "ALEXANDRITE_ORE", 0.26f, 4, 1.5f, 1.1f, 0.4f, null);
        addToolSet("Alexandrite", "ALEXANDRITE_ORE", 5, 20.0f);

        addArmorSet("Onyx", "ONYX_ORE", 0.35f, 5, 3.0f, 0.9f, 0.7f, null); // Heavier than Adamantine
        addToolSet("Onyx", "ONYX_ORE", 6, 22.0f);

        // --- 9. RAREST MINERAL TIERS (The Absolute Limit) ---
        addArmorSet("Painite", "PAINITE_ORE", 0.40f, 6, 5.0f, 1.0f, 0.8f, null);
        addToolSet("Painite", "PAINITE_ORE", 7, 30.0f);

        addArmorSet("Musgravite", "MUSGRAVITE_ORE", 0.42f, 6, 6.0f, 1.0f, 0.8f, null);
        addToolSet("Musgravite", "MUSGRAVITE_ORE", 7, 35.0f);

        addArmorSet("Taaffeite", "TAAFFEITE_ORE", 0.45f, 7, 7.0f, 1.0f, 0.9f, null);
        addToolSet("Taaffeite", "TAAFFEITE_ORE", 8, 40.0f);

        addArmorSet("Grandidierite", "GRANDIDIERITE_ORE", 0.48f, 7, 8.0f, 1.1f, 0.9f, null);
        addToolSet("Grandidierite", "GRANDIDIERITE_ORE", 8, 45.0f);

        addArmorSet("Serendibite", "SERENDIBITE_ORE", 0.55f, 8, 10.0f, 1.2f, 1.0f, 
                new minicraft.math.Vector3f(0.1f, 0.1f, 0.5f)); // Cosmic Glow
        addToolSet("Serendibite", "SERENDIBITE_ORE", 9, 60.0f);

        // --- 10. ULTIMATE MITHRIL TIER (God-like Industry) ---
        addArmorSet("Mithril", "MITHRIL_ORE", 0.75f, 12, 25.0f, 1.5f, 2.0f,
                new minicraft.math.Vector3f(0.6f, 0.8f, 1.0f)); // Celestial Blue Glow
        addToolSet("Mithril", "MITHRIL_ORE", 10, 100.0f);

        // --- 7. ADVANCED TECHNOLOGY ---
        Map<Item, Integer> consoleIng = new HashMap<>();
        consoleIng.put(new Item("IRON_INGOT", null), 10);
        consoleIng.put(new Item("GOLD_INGOT", null), 2);
        recipes.add(new Recipe("Ship Console", Recipe.Category.BLOCKS, consoleIng,
                new Item("SHIP_CONSOLE", Block.SHIP_CONSOLE), 1));

        Map<Item, Integer> blacksmithIng = new HashMap<>();
        blacksmithIng.put(new Item("STONE", Block.STONE), 10);
        blacksmithIng.put(new Item("IRON_INGOT", null), 5);
        recipes.add(new Recipe("Blacksmith Station", Recipe.Category.BLOCKS, blacksmithIng,
                new Item("BLACKSMITH", Block.BLACKSMITH), 1));

        // --- SPECIALISED WEAPONS (Blacksmith Station) ---
        Map<Item, Integer> cotdIng = new HashMap<>();
        cotdIng.put(new Item("IRON_INGOT", null), 5);
        cotdIng.put(new Item("DIAMOND_ORE", null), 3);
        cotdIng.put(new Item("RUBY_ORE", null), 2);
        cotdIng.put(new Item("URANIUM_ORE", null), 5);
        recipes.add(new Recipe("Call of the Depths", Recipe.Category.BLACKSMITH, cotdIng,
                new ToolItem("Call of the Depths", ToolItem.ToolType.SWORD, 10, 50.0f, "item_sword_call_of_the_depths", null, 0.8f, false, 0.8f), 1));

        Map<Item, Integer> eorIng = new HashMap<>();
        eorIng.put(new Item("TITANIUM_INGOT", null), 8);
        eorIng.put(new Item("DIAMOND_ORE", null), 5);
        eorIng.put(new Item("RUBY_ORE", null), 5);
        eorIng.put(new Item("URANIUM_ORE", null), 5);
        eorIng.put(new Item("PLUTONIUM_ORE", null), 5);
        eorIng.put(new Item("MITHRIL_ORE", null), 5);
        recipes.add(new Recipe("Echo of Regret", Recipe.Category.BLACKSMITH, eorIng,
                new ToolItem("Echo of Regret", ToolItem.ToolType.SWORD, 15, 100.0f, "item_sword_echo_of_regret", null, 1.0f, true, 0.5f), 1));
    }

    private Item createMaterialItem(String mat) {
        String tex = null;

        // Mapping internal IDs to the .png filenames in /textures/
        if (mat.equals("IRON_INGOT"))
            tex = "item_ingot_iron_standalone";
        else if (mat.equals("GOLD_INGOT"))
            tex = "item_ingot_gold_standalone";
        else if (mat.equals("TITANIUM_INGOT"))
            tex = "item_ingot_titanium_standalone";

        // Gems
        else if (mat.equals("RUBY_ORE"))
            tex = "item_gem_ruby_standalone_v2";
        else if (mat.equals("SAPPHIRE_ORE"))
            tex = "item_ingot_sapphire_standalone"; // Rendered as bar per request
        else if (mat.equals("DIAMOND_ORE"))
            tex = "item_gem_diamond_standalone_v2";
        else if (mat.equals("EMERALD_ORE"))
            tex = "item_gem_emerald_standalone_v2";
        else if (mat.equals("TOPAZ_ORE"))
            tex = "item_gem_topaz_standalone";
        else if (mat.equals("AMETHYST_ORE"))
            tex = "item_gem_amethyst_standalone";
        else if (mat.equals("AQUAMARINE_ORE"))
            tex = "item_gem_aquamarine_standalone";

        // Atomic/Special
        else if (mat.equals("URANIUM_INGOT") || mat.equals("URANIUM_ORE"))
            tex = "item_pick_uranium"; 
        else if (mat.equals("PLUTONIUM_INGOT") || mat.equals("PLUTONIUM_ORE"))
            tex = "item_ingot_plutonium";
        else if (mat.equals("MITHRIL_INGOT") || mat.equals("MITHRIL_ORE"))
            tex = "item_ingot_mithril";
        else if (mat.equals("TAAFFEITE_ORE"))
            tex = "item_gem_taaffeite";
        else if (mat.equals("BLACKSMITH"))
            tex = "blacksmith_top";
        else if (mat.equals("COPPER_INGOT"))
            tex = "item_ingot_gold_standalone";
        else if (mat.equals("NICKEL_INGOT"))
            tex = "item_ingot_iron_standalone";
        else if (mat.equals("TIN_INGOT"))
            tex = "item_ingot_titanium_standalone";
        else if (mat.equals("LEATHER"))
            tex = "item_leather";
        else if (mat.equals("OBSIDIAN"))
            tex = "block_obsidian";
        
        // Food
        else if (mat.contains("MEAT")) tex = "item_meat_cooked";
        else if (mat.contains("FISH")) tex = "item_fish_cooked";
        else if (mat.contains("CHICKEN")) tex = "item_chicken_cooked";
        else if (mat.contains("APPLE")) tex = "item_apple";
        else if (mat.contains("MANGO")) tex = "item_mango";
        else if (mat.contains("PEAR")) tex = "item_pear";
        else if (mat.contains("BREAD")) tex = "item_bread";
        
        // Legendary Fallbacks (using ore textures until standalone gems are generated)
        else if (mat.endsWith("_ORE"))
            tex = mat.toLowerCase();

        return new Item(mat, null, tex, 64);
    }

    private void addToolSet(String tier, String mat, int level, float speed) {
        Item m = createMaterialItem(mat);
        String name = tier + " ";
        String low = tier.toLowerCase();

        // Pickaxe
        Map<Item, Integer> pI = new HashMap<>();
        pI.put(m, 3);
        recipes.add(new Recipe(name + "Pickaxe", Recipe.Category.TOOLS, pI,
                new ToolItem(name + "Pick", ToolItem.ToolType.PICKAXE, level, speed, "item_pick_" + low), 1));
        // Axe
        Map<Item, Integer> aI = new HashMap<>();
        aI.put(m, 3);
        recipes.add(new Recipe(name + "Axe", Recipe.Category.TOOLS, aI,
                new ToolItem(name + "Axe", ToolItem.ToolType.AXE, level, speed, "item_axe_" + low), 1));
        // Shovel
        Map<Item, Integer> sI = new HashMap<>();
        sI.put(m, 3);
        recipes.add(new Recipe(name + "Shovel", Recipe.Category.TOOLS, sI,
                new ToolItem(name + "Shovel", ToolItem.ToolType.SHOVEL, level, speed, "item_shovel_" + low), 1));
        // Sword
        Map<Item, Integer> swI = new HashMap<>();
        swI.put(m, 3);
        recipes.add(new Recipe(name + "Sword", Recipe.Category.TOOLS, swI,
                new ToolItem(name + "Sword", ToolItem.ToolType.SWORD, level, speed, "item_sword_" + low), 1));
    }

    private void addArmorSet(String tierName, String matName, float prot, int cost,
            float healthBonus, float speedMod, float insulation, minicraft.math.Vector3f glow) {
        Item mat = createMaterialItem(matName);
        String low = tierName.toLowerCase();

        // Weighted Distribution: Helmet 20%, Chest 40%, Legs 25%, Boots 15%
        recipes.add(new Recipe(tierName + " Helmet", Recipe.Category.ARMOR, Map.of(mat, cost),
                new ArmorItem(tierName + " Helmet", ArmorItem.ArmorSlot.HELMET, prot * 0.20f, "armor_helm_" + low, tierName,
                        healthBonus * 0.20f, 1.0f + (speedMod - 1.0f) * 0.20f, insulation * 0.20f, glow),
                1));

        recipes.add(new Recipe(tierName + " Chestplate", Recipe.Category.ARMOR, Map.of(mat, cost + 2),
                new ArmorItem(tierName + " Chestplate", ArmorItem.ArmorSlot.CHESTPLATE, prot * 0.40f,
                        "armor_chest_" + low, tierName, healthBonus * 0.40f, 1.0f + (speedMod - 1.0f) * 0.40f, insulation * 0.40f, glow),
                1));

        recipes.add(new Recipe(tierName + " Leggings", Recipe.Category.ARMOR, Map.of(mat, cost + 1),
                new ArmorItem(tierName + " Leggings", ArmorItem.ArmorSlot.LEGGINGS, prot * 0.25f, "armor_legs_" + low,
                        tierName, healthBonus * 0.25f, 1.0f + (speedMod - 1.0f) * 0.25f, insulation * 0.25f, glow),
                1));

        recipes.add(new Recipe(tierName + " Boots", Recipe.Category.ARMOR, Map.of(mat, cost),
                new ArmorItem(tierName + " Boots", ArmorItem.ArmorSlot.BOOTS, prot * 0.15f, "armor_boots_" + low, tierName,
                        healthBonus * 0.15f, 1.0f + (speedMod - 1.0f) * 0.15f, insulation * 0.15f, glow),
                1));
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public boolean craft(Recipe recipe, Inventory inventory) {
        // Check ingredients
        for (Map.Entry<Item, Integer> entry : recipe.getIngredients().entrySet()) {
            if (inventory.getCount(entry.getKey()) < entry.getValue()) {
                return false; // Not enough ingredients
            }
        }

        // Consume ingredients
        for (Map.Entry<Item, Integer> entry : recipe.getIngredients().entrySet()) {
            inventory.remove(entry.getKey(), entry.getValue());
        }

        // Add result
        inventory.add(recipe.getResult(), recipe.getResultCount());
        return true;
    }
}
