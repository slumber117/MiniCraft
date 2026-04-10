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
            Texture woodTexture = textureRegistry.get("wood");
            Mesh pickaxe = OBJLoader.loadModel("/models/pickaxe_wooden.obj", woodTexture);
            models.put("pickaxe_wooden", pickaxe);

            // 2. Load the high-fidelity zombie
            Texture zombieTexture = textureRegistry.get("zombie_hd");
            Mesh zombie = OBJLoader.loadModel("/models/zombie.obj", zombieTexture);
            models.put("zombie", zombie);

            System.out.println("Model Registry: Loaded high-fidelity assets (Pickaxe & Zombie).");
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
