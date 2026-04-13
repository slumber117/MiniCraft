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

        Map<Item, Integer> stoneAxeIng = new HasMap<>();
        stoneAxeIng.put(new Item("STONE", Block.STONE), 3);
        recipes.add(new Recipe("Stone Axe", Recipe.Category.TOOLS, stoneAxeIng,
                new ToolItem("Stone Axe", ToolItem.ToolType.AXE, 1, 4.0f, "item_axe_stone"), 1));

        // 2. Iron Pickaxe (Level 2)
        Map<Item, Integer> ironPickIng = new HashMap<>();
        ironPickIng.put(new Item("IRON_ORE", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Pickaxe", Recipe.Category.TOOLS, ironPickIng,
                new ToolItem("Iron Pick", ToolItem.ToolType.PICKAXE, 2, 8.0f, "item_pick_iron"), 1));

        Map<Item, Integer> ironAxeIng = new HasMap<>();
        ironAxeIng.put(new Item("IRON", Block.IRON_ORE), 3);
        recipes.add(new Recipe("Iron Axe", Recipe.Category.TOOLS, ironAxeIng,
                new ToolItem("Iron Axe", ToolItem.ToolType.AXE, 1, 4.0f, "item_axe_iron"), 1));

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
        addArmorSet("Leather", "LEATHER", 0.05f, 5); // 5% per piece
        addArmorSet("Iron", "IRON_ORE", 0.10f, 3); // 10% per piece
        addArmorSet("Bronze", "BRONZE_BLOCK", 0.12f, 2);
        addArmorSet("Gold", "GOLD_ORE", 0.15f, 4);
        addArmorSet("Diamond", "DIAMOND_ORE", 0.20f, 2);
        addArmorSet("Obsidian", "OBSIDIAN", 0.25f, 3);

    }

    private void addArmorSet(String tierName, String matName, float protPerPiece, int cost) {
        // Material Item (Placeholder block/item)
        Item mat = new Item(matName);

        // Helmet
        Map<Item, Integer> hIng = new HashMap<>();
        hIng.put(mat, cost);
        recipes.add(new Recipe(tierName + " Helmet", Recipe.Category.ARMOR, hIng,
                new ArmorItem(tierName + " Helmet", ArmorItem.ArmorSlot.HELMET, protPerPiece,
                        "armor_helm_" + tierName.toLowerCase()),
                1));

        // Chestplate
        Map<Item, Integer> cIng = new HashMap<>();
        cIng.put(mat, cost + 2);
        recipes.add(new Recipe(tierName + " Chestplate", Recipe.Category.ARMOR, cIng,
                new ArmorItem(tierName + " Chestplate", ArmorItem.ArmorSlot.CHESTPLATE, protPerPiece + 0.05f,
                        "armor_chest_" + tierName.toLowerCase()),
                1));

        // Leggings
        Map<Item, Integer> lIng = new HashMap<>();
        lIng.put(mat, cost + 1);
        recipes.add(new Recipe(tierName + " Leggings", Recipe.Category.ARMOR, lIng,
                new ArmorItem(tierName + " Leggings", ArmorItem.ArmorSlot.LEGGINGS, protPerPiece + 0.02f,
                        "armor_legs_" + tierName.toLowerCase()),
                1));

        // Boots
        Map<Item, Integer> bIng = new HashMap<>();
        bIng.put(mat, cost);
        recipes.add(new Recipe(tierName + " Boots", Recipe.Category.ARMOR, bIng,
                new ArmorItem(tierName + " Boots", ArmorItem.ArmorSlot.BOOTS, protPerPiece,
                        "armor_boots_" + tierName.toLowerCase()),
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
