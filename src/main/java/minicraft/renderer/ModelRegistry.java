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

            // 4. Load the high-fidelity diamond pickaxe
            TextureRegion diamondRegion = textureRegistry.get("alloy_plate");
            Texture diamondTexture = diamondRegion != null ? diamondRegion.getTexture() : null;
            Mesh diamondPick = OBJLoader.loadModel("/models/pickaxe_diamond.obj", diamondTexture);
            models.put("pickaxe_diamond", diamondPick);

            // 5. Load the high-fidelity zombie (Humanoid mesh)
            TextureRegion zombieRegion = textureRegistry.get("zombie_hd");
            Texture zombieTexture = zombieRegion != null ? zombieRegion.getTexture() : null;
            Mesh zombie = OBJLoader.loadModel("/models/humanoid.obj", zombieTexture);
            models.put("zombie", zombie);

            // 6. Load the player model (Humanoid mesh)
            TextureRegion playerRegion = textureRegistry.get("char_farmer");
            Texture playerTexture = playerRegion != null ? playerRegion.getTexture() : null;
            Mesh playerModel = OBJLoader.loadModel("/models/humanoid.obj", playerTexture);
            models.put("player_body", playerModel);

            // 6. Load the player's custom Stalwart model
            // This is a 54MB high-fidelity asset!
            TextureRegion shipRegion = textureRegistry.get("alloy_plate");
            Texture shipTexture = shipRegion != null ? shipRegion.getTexture() : null;
            Mesh shipModel = OBJLoader.loadModel("/models/sof_.obj", shipTexture);
            models.put("ship_stalwart", shipModel);
            
            // 7. Primitives
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
