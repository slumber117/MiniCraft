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
        
        Map<Item, Integer> tinTorchIng = new HashMap<>();
        tinTorchIng.put(new Item("TIN_ORE", Block.TIN_ORE), 1);
        tinTorchIng.put(new Item("WOOD", Block.WOOD), 1);
        recipes.add(new Recipe("Tin Torch", Recipe.Category.SURVIVAL, tinTorchIng,
                new Item("TIN_TORCH", Block.TIN_TORCH), 4));

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

        // --- 7. ADVANCED TECHNOLOGY ---
        Map<Item, Integer> consoleIng = new HashMap<>();
        consoleIng.put(new Item("IRON_INGOT", null), 10);
        consoleIng.put(new Item("GOLD_INGOT", null), 2);
        recipes.add(new Recipe("Ship Console", Recipe.Category.BLOCKS, consoleIng,
                new Item("SHIP_CONSOLE", Block.SHIP_CONSOLE), 1));
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
            tex = "item_gem_ruby";
        else if (mat.equals("SAPPHIRE_ORE"))
            tex = "item_ingot_sapphire"; // Rendered as bar per request
        else if (mat.equals("DIAMOND_ORE"))
            tex = "item_gem_diamond";
        else if (mat.equals("EMERALD_ORE"))
            tex = "item_gem_emerald";
        else if (mat.equals("TOPAZ_ORE"))
            tex = "item_gem_topaz";
        else if (mat.equals("AMETHYST_ORE"))
            tex = "item_gem_amethyst";
        else if (mat.equals("AQUAMARINE_ORE"))
            tex = "item_gem_aquamarine";

        // Atomic/Special
        else if (mat.equals("URANIUM_ORE"))
            tex = "item_ingot_uranium"; // New texture
        else if (mat.equals("PLUTONIUM_ORE"))
            tex = "item_ingot_plutonium";
        else if (mat.equals("LEATHER"))
            tex = "item_leather";
        else if (mat.equals("OBSIDIAN"))
            tex = "block_obsidian";

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
