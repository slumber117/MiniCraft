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

        recipes.add(new Recipe("Tin Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("TIN_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("TIN_TORCH", Block.TIN_TORCH), 4));

        recipes.add(new Recipe("Iron Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("IRON_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("IRON_TORCH", Block.IRON_TORCH), 4));

        recipes.add(new Recipe("Gold Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("GOLD_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("GOLD_TORCH", Block.GOLD_TORCH), 8));

        recipes.add(new Recipe("Copper Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("COPPER_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("COPPER_TORCH", Block.COPPER_TORCH), 4));

        recipes.add(new Recipe("Nickel Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("NICKEL_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("NICKEL_TORCH", Block.NICKEL_TORCH), 4));

        recipes.add(new Recipe("Uranium Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("URANIUM_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("URANIUM_TORCH", Block.URANIUM_TORCH), 4));

        recipes.add(new Recipe("Plutonium Torch", Recipe.Category.SURVIVAL,
                Map.of(new Item("PLUTONIUM_INGOT", null), 1, new Item("WOOD", Block.WOOD), 1),
                new Item("PLUTONIUM_TORCH", Block.PLUTONIUM_TORCH), 4));

        // --- 1. CLOTH TIER ---
        addArmorSet("Cloth", "FIBRE", 0.05f, 2, 0.0f, 1.10f, 0.8f, null);

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
        addArmorSet("Gold", "GOLD_INGOT", 0.22f, 4, 0.2f, 0.95f, 0.6f,
                new minicraft.math.Vector3f(1.0f, 0.85f, 0.2f)); // Precious & Radiant
        addToolSet("Gold", "GOLD_INGOT", 3, 15.0f); // Fast mining speed

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
        addToolSet("Quartz", "QUARTZ_ORE", 4, 11.0f); // Tier 4 Mining Capability

        addArmorSet("Tanzanite", "TANZANITE_ORE", 0.24f, 4, 0.8f, 1.0f, 0.3f, null); // Thorns Set
        addToolSet("Tanzanite", "TANZANITE_ORE", 4, 16.0f);

        addArmorSet("Sapphire", "SAPPHIRE_ORE", 0.22f, 3, 2.0f, 1.0f, 0.4f, null);
        addToolSet("Sapphire", "SAPPHIRE_ORE", 5, 22.0f);

        addArmorSet("Emerald", "EMERALD_ORE", 0.22f, 3, 0.5f, 1.1f, 0.4f, null);
        addToolSet("Emerald", "EMERALD_ORE", 5, 14.0f);

        addArmorSet("Topaz", "TOPAZ_ORE", 0.35f, 6, 2.5f, 1.15f, 0.7f,
                new minicraft.math.Vector3f(1.0f, 0.8f, 0.0f)); // Tier 5 Amber Glow
        addToolSet("Topaz", "TOPAZ_ORE", 5, 25.0f, new minicraft.math.Vector3f(1.0f, 0.8f, 0.0f), 0.15f); // Tier 5

        addArmorSet("Amethyst", "AMETHYST_ORE", 0.18f, 3, 0.2f, 1.0f, 0.3f, null);
        addToolSet("Amethyst", "AMETHYST_ORE", 3, 10.0f);

        addArmorSet("Aquamarine", "AQUAMARINE_ORE", 0.10f, 3, 0.4f, 1.05f, 0.2f,
                new minicraft.math.Vector3f(0.15f, 0.35f, 0.7f)); // Subtle Light Blue Glow
        addToolSet("Aquamarine", "AQUAMARINE_ORE", 5, 100.0f); // Tier 5 Aquamarine (100 * 4 = 400 damage)

        addArmorSet("Adamantine", "ADAMANTINE", 0.38f, 3, 1.8f, 1.0f, 0.4f,
                new minicraft.math.Vector3f(0.5f, 0.05f, 0.05f)); // Subtle Dark Red Glow
        addToolSet("Adamantine", "ADAMANTINE", 7, 112.5f); // Tier 7 Adamantine (112.5 * 4 = 450 damage)

        addArmorSet("Agate", "AGATE_ORE", 0.45f, 3, 0.5f, 1.05f, 0.3f,
                new minicraft.math.Vector3f(0.8f, 0.0f, 0.0f)); // Deep Red Glow
        addToolSet("Agate", "AGATE_ORE", 7, 112.5f); // Tier 7 Agate (112.5 * 4 = 450 damage)

        addArmorSet("Obsidian", "OBSIDIAN", 0.45f, 3, 0.8f, 0.85f, 0.6f,
                new minicraft.math.Vector3f(0.4f, 0.05f, 0.8f)); // Deep Purple Glow
        addToolSet("Obsidian", "OBSIDIAN", 8, 125.0f); // Tier 8 Obsidian Tools (125 * 4 = 500 damage)

        // Atomic Tiers
        addArmorSet("Uranium", "URANIUM_ORE", 0.25f, 4, 0.8f, 1.0f, 0.5f,
                new minicraft.math.Vector3f(0.2f, 1.0f, 0.2f)); // Green Glow
        addToolSet("Uranium", "URANIUM_ORE", 6, 20.0f);
        addArmorSet("Plutonium", "PLUTONIUM_ORE", 0.28f, 4, 1.0f, 1.0f, 0.5f,
                new minicraft.math.Vector3f(1.0f, 0.45f, 0.05f)); // Orange Glow
        addToolSet("Plutonium", "PLUTONIUM_ORE", 6, 25.0f);

        addArmorSet("Neptunium", "NEPTUNIUM_ORE", 0.30f, 5, 1.2f, 1.0f, 0.6f,
                new minicraft.math.Vector3f(0.0f, 0.8f, 1.0f)); // Cyan/Blue Glow
        addToolSet("Neptunium", "NEPTUNIUM_ORE", 6, 30.0f);

        addArmorSet("Orichalcum", "ORICHALCUM_ORE", 0.35f, 6, 1.5f, 1.1f, 0.4f,
                new minicraft.math.Vector3f(1.0f, 0.8f, 0.2f)); // Golden/Yellow Glow
        addToolSet("Orichalcum", "ORICHALCUM_ORE", 6, 35.0f);

        // --- 8. LEGENDARY GEM TIERS (Zenith Industry) ---
        addArmorSet("Tourmaline", "TOURMALINE_ORE", 0.22f, 3, 0.5f, 1.15f, 0.4f, null);
        addToolSet("Tourmaline", "TOURMALINE_ORE", 4, 16.0f);

        addArmorSet("Opal", "OPAL_ORE", 0.24f, 4, 2.0f, 1.0f, 0.5f, null);
        addToolSet("Opal", "OPAL_ORE", 5, 18.0f);

        addArmorSet("Alexandrite", "ALEXANDRITE_ORE", 0.26f, 4, 1.5f, 1.1f, 0.4f,
                new minicraft.math.Vector3f(1.0f, 0.1f, 0.1f)); // Bright Red Glow
        addToolSet("Alexandrite", "ALEXANDRITE_ORE", 7, 20.0f);

        // --- 9. RAREST MINERAL TIERS (The Absolute Limit - Tier 8 & 9) ---
        addArmorSet("Garnet", "GARNET_ORE", 0.50f, 8, 9.0f, 1.2f, 0.9f, null);
        addToolSet("Garnet", "GARNET_ORE", 8, 55.0f);

        addArmorSet("Painite", "PAINITE_ORE", 0.40f, 6, 5.0f, 1.0f, 10.0f,
                new minicraft.math.Vector3f(1.5f, 0.15f, 0.1f)); // Shiny Red Glow — brighter than Plutonium
        addToolSet("Painite", "PAINITE_ORE", 9, 30.0f, new minicraft.math.Vector3f(1.5f, 0.15f, 0.1f), 0.25f);

        addArmorSet("Musgravite", "MUSGRAVITE_ORE", 0.42f, 6, 6.0f, 1.0f, 0.8f, null);
        addToolSet("Musgravite", "MUSGRAVITE_ORE", 8, 35.0f);

        addArmorSet("Taaffeite", "TAAFFEITE_ORE", 0.45f, 7, 7.0f, 1.0f, 0.9f, null);
        addToolSet("Taaffeite", "TAAFFEITE_ORE", 7, 40.0f);

        addArmorSet("Grandidierite", "GRANDIDIERITE_ORE", 0.48f, 7, 8.0f, 1.1f, 0.9f, null);
        addToolSet("Grandidierite", "GRANDIDIERITE_ORE", 8, 45.0f);

        addArmorSet("Serendibite", "SERENDIBITE_ORE", 0.55f, 8, 10.0f, 1.2f, 1.0f,
                new minicraft.math.Vector3f(0.1f, 0.1f, 0.5f)); // Cosmic Glow
        addToolSet("Serendibite", "SERENDIBITE_ORE", 8, 60.0f);

        // --- 10. ULTIMATE TIERS (Tier 10 Dominus Industry) ---
        addArmorSet("Onyx", "ONYX_ORE", 0.85f, 15, 30.0f, 1.7f, 2.5f,
                new minicraft.math.Vector3f(0.1f, 0.0f, 0.2f)); // Dark Violet Glow
        addToolSet("Onyx", "ONYX_ORE", 10, 150.0f);

        addArmorSet("Mithril", "MITHRIL_ORE", 0.75f, 12, 25.0f, 1.5f, 2.0f,
                new minicraft.math.Vector3f(0.6f, 0.8f, 1.0f)); // Celestial Blue Glow
        addToolSet("Mithril", "MITHRIL_ORE", 10, 100.0f);

        // --- 11. RARE EARTH & EXOTIC TIERS (Zenith Tier 11) ---
        addToolSet("Xanthiosite", "XANTHIOSITE", 12, 105.0f);
        addToolSet("Platinum", "PLATINUM", 3, 10.5f);
        addArmorSet("Platinum", "PLATINUM", 0.22f, 4, 1.2f, 1.1f, 0.3f, null);

        addToolSet("Monazite", "MONAZITE", 13, 120.0f);
        addArmorSet("Monazite", "MONAZITE", 0.80f, 14, 30.0f, 1.6f, 2.2f,
                new minicraft.math.Vector3f(0.8f, 0.4f, 0.1f));

        addToolSet("Bastnaesite", "BASTNAESITE", 40, 250.0f, new minicraft.math.Vector3f(1.0f, 0.5f, 0.0f), 0.0f); // Apex
                                                                                                                   // Tier
        addArmorSet("Bastnaesite", "BASTNAESITE", 1.20f, 40, 200.0f, 1.3f, 10.0f,
                new minicraft.math.Vector3f(1.0f, 0.5f, 0.0f));

        addToolSet("Xenotime", "XENOTIME", 35, 200.0f, new minicraft.math.Vector3f(0.0f, 1.0f, 0.2f), 0.0f); // Zenith
                                                                                                             // Tier
        addArmorSet("Xenotime", "XENOTIME", 1.10f, 30, 150.0f, 1.4f, 5.0f,
                new minicraft.math.Vector3f(0.0f, 1.0f, 0.2f));

        addToolSet("Loparite", "LOPARITE", 16, 165.0f);
        addArmorSet("Loparite", "LOPARITE", 0.95f, 17, 45.0f, 1.9f, 2.8f,
                new minicraft.math.Vector3f(0.3f, 0.3f, 0.3f));

        addToolSet("Tantalite", "TANTALITE", 17, 180.0f);
        addArmorSet("Tantalite", "TANTALITE", 1.00f, 18, 50.0f, 2.0f, 3.0f,
                new minicraft.math.Vector3f(0.2f, 0.6f, 0.8f));

        addToolSet("Vanadinite", "VANADINITE", 18, 200.0f);
        addArmorSet("Vanadinite", "VANADINITE", 1.10f, 20, 60.0f, 2.1f, 3.2f,
                new minicraft.math.Vector3f(1.0f, 0.1f, 0.1f));

        addToolSet("Gadolinium", "GADOLINIUM", 19, 220.0f);
        addArmorSet("Gadolinium", "GADOLINIUM", 1.20f, 22, 70.0f, 2.2f, 3.4f,
                new minicraft.math.Vector3f(0.9f, 0.9f, 1.0f));

        addToolSet("Terbium", "TERBIUM", 20, 240.0f);
        addArmorSet("Terbium", "TERBIUM", 1.30f, 24, 80.0f, 2.3f, 3.6f, new minicraft.math.Vector3f(0.5f, 1.0f, 0.5f));

        addToolSet("Dysprosium", "DYSPROSIUM", 21, 260.0f);
        addArmorSet("Dysprosium", "DYSPROSIUM", 1.40f, 26, 90.0f, 2.4f, 3.8f,
                new minicraft.math.Vector3f(1.0f, 0.8f, 0.0f));

        addToolSet("Holmium", "HOLMIUM", 11, 280.0f);
        addArmorSet("Holmium", "HOLMIUM", 1.50f, 28, 100.0f, 2.5f, 4.0f, new minicraft.math.Vector3f(1.0f, 0.5f, 0.0f));

        addToolSet("Erbium", "ERBIUM", 22, 300.0f);
        addArmorSet("Erbium", "ERBIUM", 1.60f, 30, 110.0f, 2.6f, 4.2f, new minicraft.math.Vector3f(1.0f, 0.2f, 0.6f));

        addToolSet("Yttrium", "YTTRIUM", 23, 320.0f);
        addArmorSet("Yttrium", "YTTRIUM", 1.70f, 32, 120.0f, 2.7f, 4.4f, new minicraft.math.Vector3f(0.8f, 0.8f, 0.8f));

        addToolSet("Lutetium", "LUTETIUM", 24, 340.0f);
        addArmorSet("Lutetium", "LUTETIUM", 1.80f, 34, 130.0f, 2.8f, 4.6f,
                new minicraft.math.Vector3f(1.0f, 1.0f, 1.0f));

        addToolSet("Samarium", "SAMARIUM", 25, 360.0f);
        addArmorSet("Samarium", "SAMARIUM", 1.90f, 36, 140.0f, 2.9f, 4.8f,
                new minicraft.math.Vector3f(1.0f, 1.0f, 0.5f));

        addToolSet("Neodymium", "NEODYMIUM", 9, 380.0f, new minicraft.math.Vector3f(0.5f, 0.0f, 0.5f), 1.10f);
        addArmorSet("Neodymium", "NEODYMIUM", 2.00f, 38, 150.0f, 3.0f, 5.0f,
                new minicraft.math.Vector3f(0.5f, 0.0f, 0.5f));

        addToolSet("Praseodymium", "PRASEODYMIUM", 26, 400.0f);
        addArmorSet("Praseodymium", "PRASEODYMIUM", 2.10f, 40, 160.0f, 3.1f, 5.2f,
                new minicraft.math.Vector3f(0.2f, 0.8f, 0.2f));

        addToolSet("Cerium", "CERIUM", 27, 420.0f);
        addArmorSet("Cerium", "CERIUM", 2.20f, 42, 170.0f, 3.2f, 5.4f, new minicraft.math.Vector3f(1.0f, 1.0f, 0.8f));

        addToolSet("Lanthanum", "LANTHANUM", 28, 440.0f);
        addArmorSet("Lanthanum", "LANTHANUM", 2.30f, 44, 180.0f, 3.3f, 5.6f,
                new minicraft.math.Vector3f(0.9f, 0.9f, 0.9f));

        addToolSet("Promethium", "PROMETHIUM", 30, 500.0f);
        addArmorSet("Promethium", "PROMETHIUM", 3.00f, 50, 250.0f, 4.0f, 10.0f,
                new minicraft.math.Vector3f(0.0f, 1.0f, 1.0f)); // Final Tier Cyan Glow

        // --- 13. VERDANT TIER (Nature/Rare Earth Mix) ---
        addArmorSet("Ruthenium", "RUTHENIUM_ORE", 0.45f, 15, 35.0f, 1.25f, 2.0f,
                new minicraft.math.Vector3f(0.5f, 1.0f, 0.3f));
        addToolSet("Ruthenium", "RUTHENIUM_ORE", 20, 150.0f);

        addArmorSet("Rhenium", "RHENIUM_ORE", 0.50f, 18, 45.0f, 1.30f, 2.5f,
                new minicraft.math.Vector3f(0.4f, 0.9f, 0.4f));
        addToolSet("Rhenium", "RHENIUM_ORE", 22, 180.0f);

        addArmorSet("Iridium", "IRIDIUM_ORE", 0.55f, 22, 55.0f, 1.35f, 3.0f,
                new minicraft.math.Vector3f(0.3f, 0.8f, 0.5f));
        addToolSet("Iridium", "IRIDIUM_ORE", 25, 220.0f);

        addArmorSet("Osmium", "OSMIUM_ORE", 0.60f, 28, 70.0f, 1.40f, 4.0f,
                new minicraft.math.Vector3f(0.2f, 0.7f, 0.6f));
        addToolSet("Osmium", "OSMIUM_ORE", 30, 280.0f);

        addArmorSet("Rhodium", "RHODIUM_ORE", 0.70f, 35, 90.0f, 1.50f, 5.0f,
                new minicraft.math.Vector3f(0.1f, 0.6f, 0.7f));
        addToolSet("Rhodium", "RHODIUM_ORE", 35, 350.0f);

        // --- 14. VANGUARD TIER (Ultimate End-Game) ---
        // Antimatter: 90% reduction, glows blue/green
        addArmorSet("Antimatter", "ANTIMATTER_ORE", 0.90f, 100, 200.0f, 1.50f, 10.0f,
                new minicraft.math.Vector3f(0.0f, 1.0f, 0.5f));
        addToolSet("Antimatter", "ANTIMATTER_ORE", 50, 20000.0f, new minicraft.math.Vector3f(0.0f, 1.0f, 0.5f), 0.35f);
        // Special: Citadel Sword (10 antimatter)
        Map<Item, Integer> citadelIng = new HashMap<>();
        citadelIng.put(new Item("ANTIMATTER_ORE", null), 10);
        recipes.add(new Recipe("Citadel", Recipe.Category.TOOLS, citadelIng,
                new ToolItem("Citadel", ToolItem.ToolType.SWORD, 100, 20000.0f, "item_sword_antimatter"), 1));
        recipes.add(new Recipe("Citadel Battle Axe", Recipe.Category.TOOLS, citadelIng,
                new ToolItem("Citadel Battle Axe", ToolItem.ToolType.BATTLE_AXE, 100, 20000.0f,
                        "item_battleaxe_antimatter"),
                1));

        // Darkmatter: 85% reduction, glows black/purple
        addArmorSet("Darkmatter", "DARKMATTER_ORE", 0.85f, 120, 250.0f, 1.40f, 10.0f,
                new minicraft.math.Vector3f(0.3f, 0.0f, 0.5f));
        addToolSet("Darkmatter", "DARKMATTER_ORE", 60, 15000.0f, new minicraft.math.Vector3f(0.3f, 0.0f, 0.5f), 0.50f);

        // Gamma Ray: 80% reduction, white/orange
        addArmorSet("Gamma Ray", "GAMMA_RAY_ORE", 0.80f, 150, 300.0f, 1.25f, 10.0f,
                new minicraft.math.Vector3f(1.0f, 0.6f, 0.1f));
        addToolSet("Gamma Ray", "GAMMA_RAY_ORE", 80, 175000.0f, new minicraft.math.Vector3f(1.0f, 0.6f, 0.1f), 0.25f);

        // Nebula: Strongest, 95% reduction
        addArmorSet("Nebula", "NEBULA_ORE", 0.95f, 200, 500.0f, 1.75f, 10.0f,
                new minicraft.math.Vector3f(0.6f, 0.1f, 1.0f));
        addToolSet("Nebula", "NEBULA_ORE", 100, 200000.0f, new minicraft.math.Vector3f(0.6f, 0.1f, 1.0f), 0.15f);

        // --- 12. MasterCraft Tier ---

        // Rogue Armor (Silver + Leather)
        Map<Item, Integer> rogueArmorIng = new HashMap<>();
        rogueArmorIng.put(new Item("SILVER_ORE", null), 5);
        rogueArmorIng.put(new Item("LEATHER", null), 5);

        recipes.add(new Recipe("Rogue Helmet", Recipe.Category.ARMOR, rogueArmorIng,
                new ArmorItem("Rogue Helmet", ArmorItem.ArmorSlot.HELMET, 0.05f, "armor_helm_rogue", "Rogue", 1.0f,
                        1.25f, 2.0f, null),
                1));
        recipes.add(new Recipe("Rogue Chestplate", Recipe.Category.ARMOR, rogueArmorIng,
                new ArmorItem("Rogue Chestplate", ArmorItem.ArmorSlot.CHESTPLATE, 0.10f, "armor_chest_rogue", "Rogue",
                        2.0f, 1.25f, 4.0f, null),
                1));
        recipes.add(new Recipe("Rogue Leggings", Recipe.Category.ARMOR, rogueArmorIng,
                new ArmorItem("Rogue Leggings", ArmorItem.ArmorSlot.LEGGINGS, 0.08f, "armor_legs_rogue", "Rogue", 1.5f,
                        1.25f, 3.0f, null),
                1));
        recipes.add(new Recipe("Rogue Boots", Recipe.Category.ARMOR, rogueArmorIng,
                new ArmorItem("Rogue Boots", ArmorItem.ArmorSlot.BOOTS, 0.05f, "armor_boots_rogue", "Rogue", 1.0f,
                        1.30f, 2.0f, null),
                1));

        // MasterCraft Pickaxe (10 Uranium + 5 Plutonium)
        Map<Item, Integer> mcPickIng = new HashMap<>();
        mcPickIng.put(new Item("URANIUM_ORE", null), 10);
        mcPickIng.put(new Item("PLUTONIUM_ORE", null), 5);

        // Tier 11, efficiency 40, light-blue aura, 5x radioactive speed
        ToolItem mcPick = new ToolItem("MasterCraft Pickaxe", ToolItem.ToolType.PICKAXE, 11, 40.0f,
                "item_pick_mastercraft", null, 0f, false, 0f,
                new minicraft.math.Vector3f(0.4f, 0.8f, 1.0f), 5.0f, 1.0f);
        recipes.add(new Recipe("MasterCraft Pickaxe", Recipe.Category.TOOLS, mcPickIng, mcPick, 1));

        // Rogue Dagger (Silver)
        Map<Item, Integer> daggerIng = new HashMap<>();
        daggerIng.put(new Item("SILVER_ORE", null), 4);
        // Tier 8, 1.8x attack speed multiplier
        ToolItem dagger = new ToolItem("Rogue Dagger", ToolItem.ToolType.SWORD, 8, 20.0f, "item_sword_rogue", null, 0f,
                false, 0f,
                null, 1.0f, 1.8f);
        recipes.add(new Recipe("Rogue Dagger", Recipe.Category.TOOLS, daggerIng, dagger, 1));
        recipes.add(new Recipe("Rogue Battle Axe", Recipe.Category.TOOLS, daggerIng,
                new ToolItem("Rogue Battle Axe", ToolItem.ToolType.BATTLE_AXE, 8, 20.0f, "item_battleaxe_rogue", null,
                        0f, false, 0f, null, 1.0f, 1.8f),
                1));

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
                new ToolItem("Call of the Depths", ToolItem.ToolType.SWORD, 10, 50.0f, "item_sword_call_of_the_depths",
                        null, 0.8f, false, 0.8f),
                1));
        recipes.add(new Recipe("Battle Axe of the Depths", Recipe.Category.BLACKSMITH, cotdIng,
                new ToolItem("Battle Axe of the Depths", ToolItem.ToolType.BATTLE_AXE, 10, 50.0f,
                        "item_battleaxe_call_of_the_depths",
                        null, 0.8f, false, 0.8f),
                1));

        Map<Item, Integer> eorIng = new HashMap<>();
        eorIng.put(new Item("TITANIUM_INGOT", null), 8);
        eorIng.put(new Item("DIAMOND_ORE", null), 5);
        eorIng.put(new Item("RUBY_ORE", null), 5);
        eorIng.put(new Item("URANIUM_ORE", null), 5);
        eorIng.put(new Item("PLUTONIUM_ORE", null), 5);
        eorIng.put(new Item("MITHRIL_ORE", null), 5);
        recipes.add(new Recipe("Echo of Regret", Recipe.Category.BLACKSMITH, eorIng,
                new ToolItem("Echo of Regret", ToolItem.ToolType.SWORD, 15, 100.0f, "item_sword_echo_of_regret", null,
                        1.0f, true, 0.5f),
                1));
        recipes.add(new Recipe("Echo of Regret Battle Axe", Recipe.Category.BLACKSMITH, eorIng,
                new ToolItem("Echo of Regret Battle Axe", ToolItem.ToolType.BATTLE_AXE, 15, 100.0f,
                        "item_battleaxe_echo_of_regret", null,
                        1.0f, true, 0.5f),
                1));

        Map<Item, Integer> radBladeIng = new HashMap<>();
        radBladeIng.put(new Item("Onyx Sword", null), 1);
        radBladeIng.put(new Item("Plutonium Sword", null), 1);
        recipes.add(new Recipe("Radiation Blade", Recipe.Category.BLACKSMITH, radBladeIng,
                new ToolItem("Radiation Blade", ToolItem.ToolType.SWORD, 10, 120.0f, "item_sword_onyx", null,
                        0.0f, false, 0.0f),
                1));
        recipes.add(new Recipe("Radiation Greataxe", Recipe.Category.BLACKSMITH, radBladeIng,
                new ToolItem("Radiation Greataxe", ToolItem.ToolType.BATTLE_AXE, 10, 120.0f, "item_battleaxe_onyx",
                        null,
                        0.0f, false, 0.0f),
                1));

    }

    private Item createMaterialItem(String mat) {
        String tex = null;

        // Rare Earth / High Tier Mappings
        if (mat.equals("MONAZITE"))
            tex = "item_ingot_monazite";
        else if (mat.equals("ADAMANTINE"))
            tex = "item_ingot_adamantine";
        else if (mat.equals("BASTNAESITE"))
            tex = "item_ingot_bastnaesite";
        else if (mat.equals("XENOTIME"))
            tex = "item_ingot_xenotime";
        else if (mat.equals("LOPARITE"))
            tex = "item_ingot_loparite";
        else if (mat.equals("TANTALITE") || mat.equals("VANADINITE"))
            tex = "item_ingot_titanium_standalone"; // Use high-tier ingot placeholder
        else if (mat.equals("TERBIUM"))
            tex = "item_ingot_terbium";
        else if (mat.equals("DYSPROSIUM"))
            tex = "item_ingot_dysprosium";
        else if (mat.equals("HOLMIUM"))
            tex = "item_ingot_holmium";
        else if (mat.equals("ERBIUM"))
            tex = "item_ingot_erbium";
        else if (mat.equals("LUTETIUM"))
            tex = "item_ingot_lutetium";
        else if (mat.equals("SAMARIUM"))
            tex = "item_ingot_samarium";
        else if (mat.equals("NEODYMIUM"))
            tex = "item_ingot_neodymium";
        else if (mat.equals("PRASEODYMIUM"))
            tex = "item_ingot_praseodymium";
        else if (mat.equals("CERIUM"))
            tex = "item_ingot_cerium";
        else if (mat.equals("GADOLINIUM") || mat.equals("YTTRIUM") || mat.equals("LANTHANUM"))
            tex = "item_ingot_mithril"; // General fallback
        else if (mat.equals("PROMETHIUM"))
            tex = "item_ingot_promethium";
        else if (mat.equals("XANTHIOSITE"))
            tex = "item_ingot_xanthiosite";
        else if (mat.equals("PLATINUM"))
            tex = "item_ingot_silver";

        // Mapping internal IDs to the .png filenames in /textures/
        else if (mat.equals("IRON_INGOT"))
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
            tex = "item_ingot_uranium";
        else if (mat.equals("PLUTONIUM_INGOT") || mat.equals("PLUTONIUM_ORE"))
            tex = "item_ingot_plutonium";
        else if (mat.equals("SILVER_INGOT") || mat.equals("SILVER_ORE"))
            tex = "item_ingot_silver";
        else if (mat.equals("OPAL_ORE"))
            tex = "item_gem_opal";
        else if (mat.equals("ALEXANDRITE_ORE"))
            tex = "item_gem_alexandrite";
        else if (mat.equals("GRANDIDIERITE_ORE"))
            tex = "item_gem_grandidierite";
        else if (mat.equals("MUSGRAVITE_ORE"))
            tex = "item_gem_musgravite";
        else if (mat.equals("MITHRIL_INGOT") || mat.equals("MITHRIL_ORE"))
            tex = "item_ingot_mithril";
        else if (mat.equals("TAAFFEITE_ORE"))
            tex = "item_gem_taaffeite";
        else if (mat.equals("ONYX_ORE"))
            tex = "item_gem_onyx";
        else if (mat.equals("SERENDIBITE_ORE"))
            tex = "item_gem_serendibite";
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
        else if (mat.contains("RAW_MEAT"))
            tex = "item_meat_raw";
        else if (mat.contains("MEAT"))
            tex = "item_meat_cooked";
        else if (mat.contains("RAW_FISH"))
            tex = "item_fish_raw";
        else if (mat.contains("FISH"))
            tex = "item_fish_cooked";
        else if (mat.contains("RAW_CHICKEN"))
            tex = "item_chicken_raw";
        else if (mat.contains("CHICKEN"))
            tex = "item_chicken_cooked";
        else if (mat.contains("APPLE"))
            tex = "item_apple";
        else if (mat.contains("MANGO"))
            tex = "item_mango";
        else if (mat.contains("PEAR"))
            tex = "item_pear";
        else if (mat.contains("BREAD"))
            tex = "item_bread";

        // Legendary Fallbacks (using ore textures until standalone gems are generated)
        else if (mat.endsWith("_ORE"))
            tex = mat.toLowerCase();

        return new Item(mat, null, tex, 64);
    }

    private int getReqLevel(int tier) {
        if (tier <= 1)
            return 1;
        if (tier == 2)
            return 5;
        if (tier == 3)
            return 10;
        if (tier == 4)
            return 20;
        if (tier == 5)
            return 30;
        if (tier == 6)
            return 40;
        if (tier == 7)
            return 50;
        if (tier == 8)
            return 65;
        if (tier == 9)
            return 80;
        if (tier == 10)
            return 100;
        return 100 + (tier - 10) * 10;
    }

    private void addToolSet(String tier, String mat, int level, float speed) {
        addToolSet(tier, mat, level, speed, null, 0.0f);
    }

    private void addToolSet(String tier, String mat, int level, float speed, minicraft.math.Vector3f aura,
            float chance) {
        Item m = createMaterialItem(mat);
        String name = tier + " ";
        String low = tier.toLowerCase();

        float radBonus = 1.0f;
        if (tier.equals("Uranium"))
            radBonus = 1.5f;
        if (tier.equals("Plutonium"))
            radBonus = 2.5f;
        if (tier.equals("Neptunium"))
            radBonus = 3.0f;
        if (tier.equals("Xenotime"))
            radBonus = 5.0f;

        // Pickaxe
        Map<Item, Integer> pI = new HashMap<>();
        pI.put(m, 3);
        ToolItem pick = new ToolItem(name + "Pick", ToolItem.ToolType.PICKAXE, level, speed, "item_pick_" + low, null,
                0f, false, 0f, aura, radBonus, 1.0f, chance);
        pick.setLevelRequirement(getReqLevel(level));
        recipes.add(new Recipe(name + "Pickaxe", Recipe.Category.TOOLS, pI, pick, 1));

        // Axe
        Map<Item, Integer> aI = new HashMap<>();
        aI.put(m, 3);
        ToolItem axe = new ToolItem(name + "Axe", ToolItem.ToolType.AXE, level, speed, "item_axe_" + low, null, 0f,
                false, 0f, aura, radBonus, 1.0f, 0f);
        axe.setLevelRequirement(getReqLevel(level));
        recipes.add(new Recipe(name + "Axe", Recipe.Category.TOOLS, aI, axe, 1));

        // Shovel
        Map<Item, Integer> sI = new HashMap<>();
        sI.put(m, 3);
        ToolItem shovel = new ToolItem(name + "Shovel", ToolItem.ToolType.SHOVEL, level, speed, "item_shovel_" + low,
                null, 0f, false, 0f, aura, radBonus, 1.0f, 0f);
        shovel.setLevelRequirement(getReqLevel(level));
        recipes.add(new Recipe(name + "Shovel", Recipe.Category.TOOLS, sI, shovel, 1));
        // Sword
        Map<Item, Integer> swI = new HashMap<>();
        swI.put(m, 3);

        // Attack Speed Scaling: Higher level = Faster swing (Lower latency)
        // Tier 0 (Wood): 0.7x, Tier 2 (Iron): 1.0x, Tier 10 (Mithril): 2.5x
        float attackSpeed = 0.7f + (level * 0.18f);
        if (tier.equals("Gold"))
            attackSpeed += 0.5f; // Gold specialty is speed
        if (tier.equals("Obsidian"))
            attackSpeed = 0.5f; // 0.8s cooldown: 0.4 / 0.5 = 0.8
        if (tier.equals("Aquamarine"))
            attackSpeed = 0.444f; // 0.9s cooldown: 0.4 / 0.444 = 0.9
        if (tier.equals("Agate"))
            attackSpeed = 0.533f; // 0.75s cooldown: 0.4 / 0.533 = 0.75

        ToolItem sword = new ToolItem(name + "Sword", ToolItem.ToolType.SWORD, level, speed, "item_sword_" + low, null,
                0f, false, 0f, aura, radBonus, attackSpeed, 0f);
        sword.setLevelRequirement(getReqLevel(level));
        recipes.add(new Recipe(name + "Sword", Recipe.Category.TOOLS, swI, sword, 1));

        // Battle Axe
        Map<Item, Integer> baI = new HashMap<>();
        baI.put(m, 5); // Heavier weapon, more material
        ToolItem battleAxe = new ToolItem(name + "Battle Axe", ToolItem.ToolType.BATTLE_AXE, level, speed,
                "item_battleaxe_" + low, null, 0f, false, 0f, aura, radBonus, attackSpeed, 0f);
        battleAxe.setLevelRequirement(getReqLevel(level));
        recipes.add(new Recipe(name + "Battle Axe", Recipe.Category.TOOLS, baI, battleAxe, 1));
    }

    private void addArmorSet(String tierName, String matName, float prot, int cost,
            float healthBonus, float speedMod, float insulation, minicraft.math.Vector3f glow) {
        Item mat = createMaterialItem(matName);
        String low = tierName.toLowerCase();

        // Determine Requirement Level from Tier Name/Context
        int req = 1;
        if (tierName.equals("Iron") || tierName.equals("Silver"))
            req = 5;
        else if (tierName.equals("Gold") || tierName.equals("Nickel") || tierName.equals("Amethyst"))
            req = 10;
        else if (tierName.equals("Diamond") || tierName.equals("Titanium") || tierName.equals("Tanzanite"))
            req = 20;
        else if (tierName.equals("Aquamarine") || tierName.equals("Emerald") || tierName.equals("Topaz")
                || tierName.equals("Opal") || tierName.equals("Alexandrite") || tierName.equals("Tourmaline"))
            req = 30;
        else if (tierName.equals("Uranium") || tierName.equals("Plutonium") || tierName.equals("Neptunium")
                || tierName.equals("Orichalcum"))
            req = 40;
        else if (tierName.equals("Adamantine") || tierName.equals("Agate") || tierName.equals("Taaffeite"))
            req = 50;
        else if (tierName.equals("Painite") || tierName.equals("Garnet") || tierName.equals("Serendibite"))
            req = 80;
        else if (tierName.equals("Ruthenium") || tierName.equals("Rhenium"))
            req = 85;
        else if (tierName.equals("Iridium") || tierName.equals("Osmium"))
            req = 90;
        else if (tierName.equals("Rhodium"))
            req = 95;
        else if (tierName.equals("Onyx") || tierName.equals("Mithril") || tierName.equals("Antimatter")
                || tierName.equals("Darkmatter") || tierName.equals("Gamma Ray") || tierName.equals("Nebula")
                || tierName.equals("Citadel"))
            req = 100;

        // Helmet
        ArmorItem helm = new ArmorItem(tierName + " Helmet", ArmorItem.ArmorSlot.HELMET, prot * 0.20f,
                "armor_helm_" + low,
                tierName, healthBonus * 0.20f, 1.0f + (speedMod - 1.0f) * 0.20f, insulation * 0.20f, glow);
        helm.setLevelRequirement(req);
        recipes.add(new Recipe(tierName + " Helmet", Recipe.Category.ARMOR, Map.of(mat, cost), helm, 1));

        // Chestplate
        ArmorItem chest = new ArmorItem(tierName + " Chestplate", ArmorItem.ArmorSlot.CHESTPLATE, prot * 0.40f,
                "armor_chest_" + low, tierName, healthBonus * 0.40f, 1.0f + (speedMod - 1.0f) * 0.40f,
                insulation * 0.40f, glow);
        chest.setLevelRequirement(req);
        recipes.add(new Recipe(tierName + " Chestplate", Recipe.Category.ARMOR, Map.of(mat, cost + 2), chest, 1));

        // Leggings
        ArmorItem legs = new ArmorItem(tierName + " Leggings", ArmorItem.ArmorSlot.LEGGINGS, prot * 0.25f,
                "armor_legs_" + low,
                tierName, healthBonus * 0.25f, 1.0f + (speedMod - 1.0f) * 0.25f, insulation * 0.25f, glow);
        legs.setLevelRequirement(req);
        recipes.add(new Recipe(tierName + " Leggings", Recipe.Category.ARMOR, Map.of(mat, cost + 1), legs, 1));

        // Boots
        ArmorItem boots = new ArmorItem(tierName + " Boots", ArmorItem.ArmorSlot.BOOTS, prot * 0.15f,
                "armor_boots_" + low,
                tierName, healthBonus * 0.15f, 1.0f + (speedMod - 1.0f) * 0.15f, insulation * 0.15f, glow);
        boots.setLevelRequirement(req);
        recipes.add(new Recipe(tierName + " Boots", Recipe.Category.ARMOR, Map.of(mat, cost), boots, 1));
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
