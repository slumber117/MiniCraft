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
        // Initialize basic recipes

        // 1. Stone Pickaxe (Level 1)
        Map<Item, Integer> stonePickIng = new HashMap<>();
        stonePickIng.put(new Item("STONE", Block.STONE), 3);
        recipes.add(new Recipe("Stone Pickaxe", Recipe.Category.TOOLS, stonePickIng,
                new ToolItem("Stone Pick", ToolItem.ToolType.PICKAXE, 1, 4.0f, "item_pick_stone"), 1));

        Map<Item, Integer> stoneAxeIng = new HashMap<>();
        stoneAxeIng.put(new Item("STONE", Block.STONE), 3);
        recipes.add(new Recipe("Stone Axe", Recipe.Category.TOOLS, stoneAxeIng,
                new ToolItem("Stone Axe", ToolItem.ToolType.AXE, 1, 4.0f, "item_axe_stone"), 1));

        // 2. Iron Pickaxe (Level 2)
        Map<Item, Integer> ironPickIng = new HashMap<>();
        ironPickIng.put(new Item("IRON_ORE", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Pickaxe", Recipe.Category.TOOLS, ironPickIng,
                new ToolItem("Iron Pick", ToolItem.ToolType.PICKAXE, 2, 8.0f, "item_pick_iron"), 1));

        Map<Item, Integer> ironAxeIng = new HashMap<>();
        ironAxeIng.put(new Item("IRON", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Axe", Recipe.Category.TOOLS, ironAxeIng,
                new ToolItem("Iron Axe", ToolItem.ToolType.AXE, 1, 4.0f, "item_axe_iron"), 1));

        // 2b. Gold Pickaxe (Level 2.5 — between Iron and Titanium)
        Map<Item, Integer> goldPickIng = new HashMap<>();
        goldPickIng.put(new Item("GOLD_ORE", Block.GOLD_ORE), 3);
        recipes.add(new Recipe("Gold Pickaxe", Recipe.Category.TOOLS, goldPickIng,
                new ToolItem("Gold Pick", ToolItem.ToolType.PICKAXE, 2, 10.0f, "item_pick_gold"), 1));

        Map<Item, Integer> goldAxeIng = new HashMap<>();
        goldAxeIng.put(new Item("GOLD_ORE", Block.GOLD_ORE), 3);
        recipes.add(new Recipe("Gold Axe", Recipe.Category.TOOLS, goldAxeIng,
                new ToolItem("Gold Axe", ToolItem.ToolType.AXE, 2, 10.0f, "item_axe_gold"), 1));

        Map<Item, Integer> goldShovelIng = new HashMap<>();
        goldShovelIng.put(new Item("GOLD_ORE", Block.GOLD_ORE), 3);
        recipes.add(new Recipe("Gold Shovel", Recipe.Category.TOOLS, goldShovelIng,
                new ToolItem("Gold Shovel", ToolItem.ToolType.SHOVEL, 2, 10.0f, "item_shovel_gold"), 1));

        Map<Item, Integer> goldSwordIng = new HashMap<>();
        goldSwordIng.put(new Item("GOLD_ORE", Block.GOLD_ORE), 3);
        recipes.add(new Recipe("Gold Sword", Recipe.Category.TOOLS, goldSwordIng,
                new ToolItem("Gold Sword", ToolItem.ToolType.SWORD, 2, 10.0f, "item_sword_gold"), 1));

        // 3. Titanium Pickaxe (Level 3)
        Map<Item, Integer> titPickIng = new HashMap<>();
        titPickIng.put(new Item("TITANIUM_ORE", Block.TITANIUM_ORE), 3);
        recipes.add(new Recipe("Titanium Pick", Recipe.Category.TOOLS, titPickIng,
                new ToolItem("Titanium Pick", ToolItem.ToolType.PICKAXE, 3, 12.0f, "item_pick_titanium"), 1));

        Map<Item, Integer> titAxeIng = new HashMap<>();
        titAxeIng.put(new Item("TITANIUM_ORE", Block.TITANIUM_ORE), 3);
        recipes.add(new Recipe("Titanium Axe", Recipe.Category.TOOLS, titAxeIng,
                new ToolItem("Titanium Axe", ToolItem.ToolType.AXE, 3, 12.0f, "item_axe_titanium"), 1));

        // 4. Diamond Pickaxe (Level 4)
        Map<Item, Integer> diaPickIng = new HashMap<>();
        diaPickIng.put(new Item("DIAMOND_ORE", Block.DIAMOND_ORE), 3);
        recipes.add(new Recipe("Diamond Pick", Recipe.Category.TOOLS, diaPickIng,
                new ToolItem("Diamond Pick", ToolItem.ToolType.PICKAXE, 4, 16.0f, "item_pick_diamond"), 1));

        Map<Item, Integer> diaAxeIng = new HashMap<>();
        diaAxeIng.put(new Item("DIAMOND_ORE", Block.DIAMOND_ORE), 3);
        recipes.add(new Recipe("Diamond Axe", Recipe.Category.TOOLS, diaAxeIng,
                new ToolItem("Diamond Axe", ToolItem.ToolType.AXE, 4, 16.0f, "item_axe_diamond"), 1));
        // 3. Crafting Table (from Wood)
        Map<Item, Integer> tableIng = new HashMap<>();
        tableIng.put(new Item("WOOD", Block.WOOD), 4);
        recipes.add(new Recipe("Crafting Table", Recipe.Category.BLOCKS, tableIng,
                new Item("CRAFTING_TABLE", Block.CRAFTING_TABLE), 1));

        // 5. Ship Console (Advanced)
        Map<Item, Integer> consoleIng = new HashMap<>();
        consoleIng.put(new Item("IRON_ORE", Block.IRON_ORE), 10);
        consoleIng.put(new Item("GOLD_ORE", Block.GOLD_ORE), 2);
        recipes.add(new Recipe("Ship Console", Recipe.Category.BLOCKS, consoleIng,
                new Item("SHIP_CONSOLE", Block.SHIP_CONSOLE), 1));

        // 4. Primitive Torch (from Wood)
        Map<Item, Integer> torchIng = new HashMap<>();
        torchIng.put(new Item("WOOD", Block.WOOD), 1);
        recipes.add(new Recipe("Primitive Torch", Recipe.Category.SURVIVAL, torchIng,
                new Item("TORCH", Block.TORCH), 2));

        // 9. Stone Shovel
        Map<Item, Integer> stoneShovelIng = new HashMap<>();
        stoneShovelIng.put(new Item("STONE", Block.STONE), 3);
        recipes.add(new Recipe("Stone Shovel", Recipe.Category.TOOLS, stoneShovelIng,
                new ToolItem("Stone Shovel", ToolItem.ToolType.SHOVEL, 1, 4.0f, "item_shovel_stone"), 1));

        // 10. Iron Shovel
        Map<Item, Integer> ironShovelIng = new HashMap<>();
        ironShovelIng.put(new Item("IRON_ORE", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Shovel", Recipe.Category.TOOLS, ironShovelIng,
                new ToolItem("Iron Shovel", ToolItem.ToolType.SHOVEL, 2, 8.0f, "item_shovel_iron"), 1));

        // 11. Titanium Shovel
        Map<Item, Integer> titShovelIng = new HashMap<>();
        titShovelIng.put(new Item("TITANIUM_ORE", Block.TITANIUM_ORE), 3);
        recipes.add(new Recipe("Titanium Shovel", Recipe.Category.TOOLS, titShovelIng,
                new ToolItem("Titanium Shovel", ToolItem.ToolType.SHOVEL, 3, 12.0f, "item_shovel_titanium"), 1));

        // 12. Diamond Shovel
        Map<Item, Integer> diaShovelIng = new HashMap<>();
        diaShovelIng.put(new Item("DIAMOND_ORE", Block.DIAMOND_ORE), 3);
        recipes.add(new Recipe("Diamond Shovel", Recipe.Category.TOOLS, diaShovelIng,
                new ToolItem("Diamond Shovel", ToolItem.ToolType.SHOVEL, 4, 16.0f, "item_shovel_diamond"), 1));
        // 13. Stone Sword
        Map<Item, Integer> stoneSwordIng = new HashMap<>();
        stoneSwordIng.put(new Item("STONE", Block.STONE), 3);
        recipes.add(new Recipe("Stone Sword", Recipe.Category.TOOLS, stoneSwordIng,
                new ToolItem("Stone Sword", ToolItem.ToolType.SWORD, 1, 4.0f, "item_sword_stone"), 1));

        // 14. Iron Sword
        Map<Item, Integer> ironSwordIng = new HashMap<>();
        ironSwordIng.put(new Item("IRON_ORE", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Sword", Recipe.Category.TOOLS, ironSwordIng,
                new ToolItem("Iron Sword", ToolItem.ToolType.SWORD, 2, 8.0f, "item_sword_iron"), 1));

        // 15. Titanium Sword
        Map<Item, Integer> titSwordIng = new HashMap<>();
        titSwordIng.put(new Item("TITANIUM_ORE", Block.TITANIUM_ORE), 3);
        recipes.add(new Recipe("Titanium Sword", Recipe.Category.TOOLS, titSwordIng,
                new ToolItem("Titanium Sword", ToolItem.ToolType.SWORD, 3, 12.0f, "item_sword_titanium"), 1));

        // 16. Diamond Sword
        Map<Item, Integer> diaSwordIng = new HashMap<>();
        diaSwordIng.put(new Item("DIAMOND_ORE", Block.DIAMOND_ORE), 3);
        recipes.add(new Recipe("Diamond Sword", Recipe.Category.TOOLS, diaSwordIng,
                new ToolItem("Diamond Sword", ToolItem.ToolType.SWORD, 4, 16.0f, "item_sword_diamond"), 1));

        // --- NEW ARMOR RECIPES ---
        addArmorSet("Leather", "LEATHER",    0.05f, 5, 0.0f, 1.0f,  0.5f, null); 
        addArmorSet("Iron",    "IRON_ORE",   0.12f, 3, 0.3f, 0.7f,  0.2f, null); 
        addArmorSet("Bronze",  "BRONZE_BLOCK",0.14f, 2, 0.2f, 0.85f, 0.2f, null);
        addArmorSet("Gold",    "GOLD_ORE",   0.16f, 4, 0.2f, 1.0f,  0.3f, null);
        addArmorSet("Titanium","TITANIUM_ORE",0.20f, 3, 0.6f, 0.95f, 0.3f, null);
        addArmorSet("Diamond", "DIAMOND_ORE",0.25f, 2, 1.0f, 1.0f,  0.4f, null);
        
        // Gem Tiers
        addArmorSet("Ruby",    "RUBY_ORE",   0.22f, 3, 1.5f, 1.0f, 0.4f, null);
        addArmorSet("Emerald", "EMERALD_ORE",0.22f, 3, 0.5f, 1.1f, 0.4f, null);
        addArmorSet("Topaz",   "TOPAZ_ORE",  0.22f, 3, 0.8f, 1.0f, 0.4f, null);
        addArmorSet("Sapphire","SAPPHIRE_ORE",0.22f, 3, 2.0f, 1.0f, 0.4f, null);
        addArmorSet("Obsidian","OBSIDIAN",   0.30f, 3, 0.8f, 0.85f,0.6f, null);

        // Atomic Tiers
        addArmorSet("Uranium", "URANIUM_ORE",  0.25f, 4, 0.8f, 1.0f, 0.5f, new minicraft.math.Vector3f(0.2f, 1.0f, 0.2f));
        addArmorSet("Plutonium","PLUTONIUM_ORE",0.28f, 4, 1.0f, 1.0f, 0.5f, new minicraft.math.Vector3f(1.0f, 0.5f, 0.1f));

        // --- NEW TOOL RECIPES ---
        addToolSet("Iron",    "IRON_ORE",    2, 8.0f);
        addToolSet("Gold",    "GOLD_ORE",    2, 10.0f);
        addToolSet("Titanium","TITANIUM_ORE", 3, 12.0f);
        addToolSet("Diamond", "DIAMOND_ORE",  4, 16.0f);
        addToolSet("Ruby",    "RUBY_ORE",     4, 18.0f);
        addToolSet("Sapphire","SAPPHIRE_ORE",  5, 22.0f);
        addToolSet("Uranium", "URANIUM_ORE",   5, 20.0f);
    }

    private void addToolSet(String tier, String mat, int level, float speed) {
        Item m = new Item(mat);
        String name = tier + " ";
        String low = tier.toLowerCase();
        
        // Pickaxe
        Map<Item, Integer> pI = new HashMap<>(); pI.put(m, 3);
        recipes.add(new Recipe(name+"Pickaxe", Recipe.Category.TOOLS, pI, new ToolItem(name+"Pick", ToolItem.ToolType.PICKAXE, level, speed, "item_pick_"+low), 1));
        // Axe
        Map<Item, Integer> aI = new HashMap<>(); aI.put(m, 3);
        recipes.add(new Recipe(name+"Axe", Recipe.Category.TOOLS, aI, new ToolItem(name+"Axe", ToolItem.ToolType.AXE, level, speed, "item_axe_"+low), 1));
        // Shovel
        Map<Item, Integer> sI = new HashMap<>(); sI.put(m, 3);
        recipes.add(new Recipe(name+"Shovel", Recipe.Category.TOOLS, sI, new ToolItem(name+"Shovel", ToolItem.ToolType.SHOVEL, level, speed, "item_shovel_"+low), 1));
        // Sword
        Map<Item, Integer> swI = new HashMap<>(); swI.put(m, 3);
        recipes.add(new Recipe(name+"Sword", Recipe.Category.TOOLS, swI, new ToolItem(name+"Sword", ToolItem.ToolType.SWORD, level, speed, "item_sword_"+low), 1));
    }

    private void addArmorSet(String tierName, String matName, float prot, int cost, 
                             float healthBonus, float speedMod, float insulation, minicraft.math.Vector3f glow) {
        Item mat = new Item(matName);
        String low = tierName.toLowerCase();

        recipes.add(new Recipe(tierName + " Helmet", Recipe.Category.ARMOR, Map.of(mat, cost),
                new ArmorItem(tierName+" Helmet", ArmorItem.ArmorSlot.HELMET, prot, "armor_helm_"+low, tierName, healthBonus, speedMod, insulation, glow), 1));

        recipes.add(new Recipe(tierName + " Chestplate", Recipe.Category.ARMOR, Map.of(mat, cost+2),
                new ArmorItem(tierName+" Chestplate", ArmorItem.ArmorSlot.CHESTPLATE, prot+0.05f, "armor_chest_"+low, tierName, healthBonus, speedMod, insulation, glow), 1));

        recipes.add(new Recipe(tierName + " Leggings", Recipe.Category.ARMOR, Map.of(mat, cost+1),
                new ArmorItem(tierName+" Leggings", ArmorItem.ArmorSlot.LEGGINGS, prot+0.02f, "armor_legs_"+low, tierName, healthBonus, speedMod, insulation, glow), 1));

        recipes.add(new Recipe(tierName + " Boots", Recipe.Category.ARMOR, Map.of(mat, cost),
                new ArmorItem(tierName+" Boots", ArmorItem.ArmorSlot.BOOTS, prot, "armor_boots_"+low, tierName, healthBonus, speedMod, insulation, glow), 1));
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
