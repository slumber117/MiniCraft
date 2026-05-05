package minicraft.renderer;

import java.util.HashMap;
import java.util.Map;
import minicraft.renderer.Texture;
import minicraft.renderer.TextureRegistry;

public class ModelRegistry {

    private static final Map<String, Mesh> models = new HashMap<>();

    public static void init(TextureRegistry textureRegistry) {
        try {
            // 1. Load the high-fidelity wooden pickaxe
            TextureRegion woodRegion = textureRegistry.get("material_wood_realistic");
            Texture woodTexture = woodRegion != null ? woodRegion.getTexture() : null;
            Mesh pickaxe = OBJLoader.loadModel("/models/pickaxe_wooden.obj", woodTexture);
            models.put("pickaxe_wooden", pickaxe);

            // 2. Load the high-fidelity stone pickaxe
            TextureRegion stoneRegion = textureRegistry.get("material_stone_realistic");
            Texture stoneTexture = stoneRegion != null ? stoneRegion.getTexture() : null;
            Mesh stonePick = OBJLoader.loadModel("/models/pickaxe_stone.obj", stoneTexture);
            models.put("pickaxe_stone", stonePick);

            // 3. Load the high-fidelity iron pickaxe
            TextureRegion ironRegion = textureRegistry.get("material_iron_realistic");
            Texture ironTexture = ironRegion != null ? ironRegion.getTexture() : null;
            Mesh ironPick = OBJLoader.loadModel("/models/pickaxe_iron.obj", ironTexture);
            models.put("pickaxe_iron", ironPick);

            // 4. Load the high-fidelity gold pickaxe
            TextureRegion goldRegion = textureRegistry.get("material_gold_realistic");
            Texture goldTexture = goldRegion != null ? goldRegion.getTexture() : null;
            Mesh goldPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", goldTexture);
            models.put("pickaxe_gold", goldPick);

            // 5. Load the high-fidelity diamond pickaxe
            TextureRegion diamondRegion = textureRegistry.get("alloy_plate");
            Texture diamondTexture = diamondRegion != null ? diamondRegion.getTexture() : null;
            Mesh diamondPick = OBJLoader.loadModel("/models/pickaxe_diamond.obj", diamondTexture);
            models.put("pickaxe_diamond", diamondPick);

            // 6. Neodymium Pickaxe
            TextureRegion neoRegion = textureRegistry.get("item_pick_neodymium");
            Texture neoTexture = neoRegion != null ? neoRegion.getTexture() : null;
            Mesh neoPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", neoTexture);
            models.put("pickaxe_neodymium", neoPick);

            // 7. Musgravite Pickaxe
            TextureRegion musRegion = textureRegistry.get("item_pick_musgravite");
            Texture musTexture = musRegion != null ? musRegion.getTexture() : null;
            Mesh musPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", musTexture);
            models.put("pickaxe_musgravite", musPick);

            // 8. Painite Pickaxe
            TextureRegion painiteRegion = textureRegistry.get("item_pick_painite");
            Texture painiteTexture = painiteRegion != null ? painiteRegion.getTexture() : null;
            Mesh painitePick = OBJLoader.loadModel("/models/pickaxe_gold.obj", painiteTexture);
            models.put("pickaxe_painite", painitePick);

            // 9. Uranium Pickaxe
            TextureRegion uraniumRegion = textureRegistry.get("item_pick_uranium");
            Texture uraniumTexture = uraniumRegion != null ? uraniumRegion.getTexture() : null;
            Mesh uraniumPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", uraniumTexture);
            models.put("pickaxe_uranium", uraniumPick);

            // 10. Praseodymium Pickaxe
            TextureRegion praseRegion = textureRegistry.get("item_pick_praseodymium");
            Texture praseTexture = praseRegion != null ? praseRegion.getTexture() : null;
            Mesh prasePick = OBJLoader.loadModel("/models/pickaxe_gold.obj", praseTexture);
            models.put("pickaxe_praseodymium", prasePick);

            // 11. Dysprosium Pickaxe
            TextureRegion dysRegion = textureRegistry.get("item_pick_dysprosium");
            Texture dysTexture = dysRegion != null ? dysRegion.getTexture() : null;
            Mesh dysPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", dysTexture);
            models.put("pickaxe_dysprosium", dysPick);

            // 12. Erbium Pickaxe
            TextureRegion erbRegion = textureRegistry.get("item_pick_erbium");
            Texture erbTexture = erbRegion != null ? erbRegion.getTexture() : null;
            Mesh erbPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", erbTexture);
            models.put("pickaxe_erbium", erbPick);

            // 13. Lutetium Pickaxe
            TextureRegion lutRegion = textureRegistry.get("item_pick_lutetium");
            Texture lutTexture = lutRegion != null ? lutRegion.getTexture() : null;
            Mesh lutPick = OBJLoader.loadModel("/models/pickaxe_gold.obj", lutTexture);
            models.put("pickaxe_lutetium", lutPick);

            // 8. Load the high-fidelity zombie (Humanoid mesh)
            TextureRegion zombieRegion = textureRegistry.get("zombie_hd");
            Texture zombieTexture = zombieRegion != null ? zombieRegion.getTexture() : null;
            Mesh zombie = OBJLoader.loadModel("/models/humanoid.obj", zombieTexture);
            models.put("zombie", zombie);

            // 10. Leviathan (Sea Monster)
            TextureRegion leviRegion = textureRegistry.get("painite_ore");
            Texture leviTexture = leviRegion != null ? leviRegion.getTexture() : null;
            Mesh leviathan = OBJLoader.loadModel("/models/leviathan.obj", leviTexture);
            models.put("leviathan", leviathan);

            // 11. Orc (High-Fidelity)
            TextureRegion orcRegion = textureRegistry.get("stone");
            Texture orcTexture = orcRegion != null ? orcRegion.getTexture() : null;
            Mesh orc = OBJLoader.loadModel("/models/orc.obj", orcTexture);
            models.put("orc", orc);

            // 9. Load the player model (First-Person Optimized)
            TextureRegion playerRegion = textureRegistry.get("char_farmer");
            Texture playerTexture = playerRegion != null ? playerRegion.getTexture() : null;
            Mesh playerBody = OBJLoader.loadModel("/models/player_body.obj", playerTexture);
            models.put("player_body", playerBody);

            // 10. Load the full character model (For Inventory UI)
            Mesh playerFull = OBJLoader.loadModel("/models/humanoid.obj", playerTexture);
            models.put("character_full", playerFull);

            // 10. Load the player's custom Stalwart model
            // This is a 54MB high-fidelity asset!
            TextureRegion shipRegion = textureRegistry.get("alloy_plate");
            Texture shipTexture = shipRegion != null ? shipRegion.getTexture() : null;
            Mesh shipModel = OBJLoader.loadModel("/models/sof_.obj", shipTexture);
            models.put("ship_stalwart", shipModel);
            
            // 11. Primitives
            models.put("primitive_cube", createCubeMesh());

            System.out.println("Model Registry: Loaded high-fidelity assets (Pickaxes, Zombie & Stalwart).");
        } catch (Exception e) {
            System.err.println("Failed to load 3D models: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Mesh createCubeMesh() {
        float[] positions = {
            // Front face
            -0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f, -0.5f, -0.5f,
            // Top face
            -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f,
            // Bottom face
            -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,
            // Right face
             0.5f, -0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f,  0.5f,
            // Left face
            -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f, -0.5f,
        };
        float[] uvs = {
            0,0, 1,0, 1,1, 0,1,
            0,0, 0,1, 1,1, 1,0,
            0,1, 0,0, 1,0, 1,1,
            0,0, 1,0, 1,1, 0,1,
            0,0, 0,1, 1,1, 1,0,
            1,0, 0,0, 0,1, 1,1,
        };
        int[] indices = {
            0,1,2, 0,2,3,
            4,5,6, 4,6,7,
            8,9,10, 8,10,11,
            12,13,14, 12,14,15,
            16,17,18, 16,18,19,
            20,21,22, 20,22,23
        };
        return new Mesh(positions, uvs, indices, null);
    }

    public static Mesh getModel(String name) {
        return models.get(name);
    }

    public static void cleanup() {
        for (Mesh mesh : models.values()) {
            if (mesh != null) mesh.cleanup();
        }
        models.clear();
    }
}
