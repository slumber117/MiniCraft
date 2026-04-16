package minicraft.renderer;

import com.google.gson.JsonObject; // You'll need the GSON library, or use a simple parser
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.FileReader;

public class TextureRegistry {
    private final Map<String, TextureRegion> regions = new HashMap<>(); 
    private final Map<String, Texture> standaloneTextures = new HashMap<>(); // Standalone files
    private final String basePath;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TextureRegistry(String basePath) {
        this.basePath = basePath;
        initAtlases();
    }

    private void initAtlases() {
        // 1. Nature Atlas Mapping
        String natureJson = "{\"grass_top\":{\"x\":0,\"y\":0},\"grass\":{\"x\":16,\"y\":0},\"dirt\":{\"x\":32,\"y\":0},\"stone\":{\"x\":48,\"y\":0},\"sand\":{\"x\":0,\"y\":16},\"red_sand\":{\"x\":16,\"y\":16},\"water\":{\"x\":32,\"y\":16},\"lava\":{\"x\":48,\"y\":16},\"magma\":{\"x\":0,\"y\":32},\"snow\":{\"x\":16,\"y\":32},\"ice\":{\"x\":32,\"y\":32},\"wood\":{\"x\":48,\"y\":32},\"leaves\":{\"x\":0,\"y\":48},\"podzol_top\":{\"x\":16,\"y\":48},\"podzol_side\":{\"x\":32,\"y\":48}}";
        loadAtlas("nature_atlas", natureJson);

        // 2. Ore Atlas Mapping
        String oreJson = "{\"coal_ore\":{\"x\":0,\"y\":0},\"iron_ore\":{\"x\":16,\"y\":0},\"copper_ore\":{\"x\":32,\"y\":0},\"tin_ore\":{\"x\":48,\"y\":0},\"gold_ore\":{\"x\":64,\"y\":0},\"silver_ore\":{\"x\":80,\"y\":0},\"platinum_ore\":{\"x\":0,\"y\":16},\"nickel_ore\":{\"x\":16,\"y\":16},\"tanzanite_ore\":{\"x\":32,\"y\":16},\"diamond_ore\":{\"x\":48,\"y\":16},\"emerald_ore\":{\"x\":64,\"y\":16},\"ruby_ore\":{\"x\":80,\"y\":16},\"topaz_ore\":{\"x\":0,\"y\":32},\"aquamarine_ore\":{\"x\":16,\"y\":32},\"peridot_ore\":{\"x\":32,\"y\":32},\"lapis_ore\":{\"x\":48,\"y\":32},\"sapphire_ore\":{\"x\":64,\"y\":32},\"amethyst_ore\":{\"x\":80,\"y\":32},\"jade_ore\":{\"x\":0,\"y\":48},\"opal_ore\":{\"x\":16,\"y\":48},\"quartz_ore\":{\"x\":32,\"y\":48},\"pyrite_ore\":{\"x\":48,\"y\":48},\"mithril_ore\":{\"x\":64,\"y\":48},\"adamantine_ore\":{\"x\":80,\"y\":48},\"orichalcum_ore\":{\"x\":0,\"y\":64},\"plutonium_ore\":{\"x\":16,\"y\":64},\"uranium_ore\":{\"x\":32,\"y\":64},\"neptunium_ore\":{\"x\":48,\"y\":64},\"tungsten_ore\":{\"x\":64,\"y\":64},\"titanium_ore\":{\"x\":80,\"y\":64},\"dragon_scale_ore\":{\"x\":0,\"y\":80}}";
        loadAtlas("ore_atlas", oreJson);

        // 3. Industrial Atlas Mapping
        String industrialJson = "{\"sand_bricks\":{\"x\":0,\"y\":0},\"stone_bricks\":{\"x\":16,\"y\":0},\"wood_planks\":{\"x\":32,\"y\":0},\"iron_plating\":{\"x\":48,\"y\":0},\"obsidian\":{\"x\":64,\"y\":0},\"glass\":{\"x\":0,\"y\":16},\"bronze_block\":{\"x\":16,\"y\":16},\"crafting_table\":{\"x\":32,\"y\":16},\"furnace\":{\"x\":48,\"y\":16},\"furnace_lit\":{\"x\":64,\"y\":16},\"alloy_forge\":{\"x\":0,\"y\":32},\"chest\":{\"x\":16,\"y\":32},\"alloy_plate\":{\"x\":32,\"y\":32},\"transmat_pad\":{\"x\":48,\"y\":32},\"ship_console\":{\"x\":64,\"y\":32},\"cooker_top\":{\"x\":0,\"y\":48},\"cooker_side\":{\"x\":16,\"y\":48},\"cooker_top_lit\":{\"x\":32,\"y\":48},\"cooker_side_lit\":{\"x\":48,\"y\":48},\"stone_dark\":{\"x\":64,\"y\":48}}";
        loadAtlas("industrial_atlas", industrialJson);

        // 4. Decoration Atlas Mapping
        String decorationJson = "{\"tall_grass\":{\"x\":0,\"y\":0},\"flower_red\":{\"x\":16,\"y\":0},\"flower_blue\":{\"x\":32,\"y\":0},\"mushroom\":{\"x\":48,\"y\":0},\"torch\":{\"x\":64,\"y\":0},\"cactus\":{\"x\":0,\"y\":16},\"sea_weed\":{\"x\":16,\"y\":16},\"coral\":{\"x\":32,\"y\":16}}";
        loadAtlas("decoration_atlas", decorationJson);
    }

    private void loadAtlas(String fileName, String json) {
        try {
            Texture atlasTex = new Texture(basePath + fileName + ".png");
            float aw = atlasTex.getWidth(), ah = atlasTex.getHeight();

            com.google.gson.reflect.TypeToken<Map<String, Map<String, Integer>>> typeToken = new com.google.gson.reflect.TypeToken<Map<String, Map<String, Integer>>>(){};
            Map<String, Map<String, Integer>> map = gson.fromJson(json, typeToken.getType());

            for (Map.Entry<String, Map<String, Integer>> entry : map.entrySet()) {
                String key = entry.getKey();
                int px = entry.getValue().get("x");
                int py = entry.getValue().get("y");

                float u1 = px / aw, v1 = py / ah;
                float u2 = (px + 16) / aw, v2 = (py + 16) / ah;

                regions.put(key, new TextureRegion(atlasTex, u1, v1, u2, v2));
            }
        } catch (Exception e) {
            System.err.println("Failed to load atlas: " + fileName + " error: " + e.getMessage());
        }
    }

    public TextureRegion get(String name) {
        if (name == null || name.isEmpty()) return null;

        // 1. Check Atlas Regions
        if (regions.containsKey(name)) return regions.get(name);

        // 2. Fallback: Standalone Files
        if (standaloneTextures.containsKey(name)) {
            Texture t = standaloneTextures.get(name);
            return new TextureRegion(t, 0, 0, 1, 1);
        }

        try {
            Texture t = new Texture(basePath + name + ".png");
            standaloneTextures.put(name, t);
            return new TextureRegion(t, 0, 0, 1, 1);
        } catch (Exception e) {
            // Ultimate Fallback
            if (name.equals("stone")) return null;
            return get("stone");
        }
    }

    public void cleanup() {
        standaloneTextures.values().forEach(t -> { if (t != null) t.cleanup(); });
        // Cleanup atlas parents (we only need to cleanup the unique parent textures)
        regions.values().stream().map(TextureRegion::getTexture).distinct().forEach(t -> { if (t != null) t.cleanup(); });
        standaloneTextures.clear();
        regions.clear();
    }
}
