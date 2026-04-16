package minicraft.renderer;

import java.util.HashMap;
import java.util.Map;
import minicraft.renderer.Texture;
import minicraft.renderer.TextureRegistry;

public class ModelRegistry {

    private static final Map<String, Mesh> models = new HashMap<>();

    public static void init(TextureRegistry textureRegistry) {
        try {
            // 1. Load the high-fidelity pickaxe
            TextureRegion woodRegion = textureRegistry.get("wood");
            Texture woodTexture = woodRegion != null ? woodRegion.getTexture() : null;
            Mesh pickaxe = OBJLoader.loadModel("/models/pickaxe_wooden.obj", woodTexture);
            models.put("pickaxe_wooden", pickaxe);

            // 2. Load the high-fidelity stone pickaxe
            TextureRegion stoneRegion = textureRegistry.get("stone");
            Texture stoneTexture = stoneRegion != null ? stoneRegion.getTexture() : null;
            Mesh stonePick = OBJLoader.loadModel("/models/pickaxe_stone.obj", stoneTexture);
            models.put("pickaxe_stone", stonePick);

            // 3. Load the high-fidelity iron pickaxe
            TextureRegion ironRegion = textureRegistry.get("alloy_plate");
            Texture ironTexture = ironRegion != null ? ironRegion.getTexture() : null;
            Mesh ironPick = OBJLoader.loadModel("/models/pickaxe_iron.obj", ironTexture);
            models.put("pickaxe_iron", ironPick);

            // 4. Load the high-fidelity zombie
            TextureRegion zombieRegion = textureRegistry.get("zombie_hd");
            Texture zombieTexture = zombieRegion != null ? zombieRegion.getTexture() : null;
            Mesh zombie = OBJLoader.loadModel("/models/zombie.obj", zombieTexture);
            models.put("zombie", zombie);

            // 3. Load the player's custom Stalwart model
            // This is a 54MB high-fidelity asset!
            TextureRegion shipRegion = textureRegistry.get("alloy_plate");
            Texture shipTexture = shipRegion != null ? shipRegion.getTexture() : null;
            Mesh shipModel = OBJLoader.loadModel("/models/sof_.obj", shipTexture);
            models.put("ship_stalwart", shipModel);

            System.out.println("Model Registry: Loaded high-fidelity assets (Pickaxe, Zombie & Stalwart).");
        } catch (Exception e) {
            System.err.println("Failed to load 3D models: " + e.getMessage());
            e.printStackTrace();
        }
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
