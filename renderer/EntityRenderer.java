package minicraft.renderer;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.ItemEntity;
import minicraft.math.Matrix4f;
import minicraft.math.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders all physical entities in the world as colored 3D bounding boxes.
 */
public class EntityRenderer {

    private Mesh cubeMesh;
    private final Map<String, String> entityTextures = new HashMap<>();

    public EntityRenderer(TextureRegistry textures) {
        float[] positions = new float[] {
            // Front face (-Z)
            -1,  1,-1,   1,  1,-1,   1, 0,-1,  -1, 0,-1,
            // Back face (+Z)
             1,  1, 1,  -1,  1, 1,  -1, 0, 1,   1, 0, 1,
            // Top face (+Y)
            -1,  1, 1,   1,  1, 1,   1, 1,-1,  -1, 1,-1,
            // Bottom face (-Y)
            -1,  0, -1,  1,  0, -1,  1, 0, 1,  -1, 0, 1,
            // Right face (+X)
             1,  1,-1,   1,  1, 1,   1, 0, 1,   1, 0,-1,
            // Left face (-X)
            -1,  1, 1,  -1,  1,-1,  -1, 0,-1,  -1, 0, 1
        };

        float[] uvs = new float[] {
            // Front face (-Z) - THE ACTUAL FORWARD
            0.125f, 0.25f,  0.25f, 0.25f,  0.25f, 0.5f,   0.125f, 0.5f,
            // Back face (+Z)
            0.375f, 0.25f,  0.5f,  0.25f,  0.5f,  0.5f,   0.375f, 0.5f,
            // Top face (+Y)
            0.125f, 0.0f,   0.25f, 0.0f,   0.25f, 0.25f,  0.125f, 0.25f,
            // Bottom face (-Y)
            0.25f,  0.0f,   0.375f, 0.0f,  0.375f, 0.25f, 0.25f,  0.25f,
            // Right face (+X)
            0,      0.25f,  0.125f, 0.25f, 0.125f, 0.5f,  0,      0.5f,
            // Left face (-X)
            0.25f,  0.25f,  0.375f, 0.25f, 0.375f, 0.5f,  0.25f,  0.5f
        };

        int[] indices = new int[] {
            0,1,3, 3,1,2,       // Front
            4,5,7, 7,5,6,       // Back
            8,9,11, 11,9,10,    // Top
            12,13,15, 15,13,14, // Bottom
            16,17,19, 19,17,18, // Right
            20,21,23, 23,21,22  // Left
        };

        cubeMesh = new Mesh(positions, uvs, indices, textures.get("stone"));

        // Setup entity textures
        entityTextures.put("COW",    "char_cow");
        entityTextures.put("SHEEP",  "char_sheep");
        entityTextures.put("RAM",    "char_ram");
        entityTextures.put("DOG",    "char_dog");
        entityTextures.put("CAT",    "char_cat");
        entityTextures.put("WHALE",  "char_whale");
        entityTextures.put("BEAR",   "char_bear");
        entityTextures.put("WOLF",   "char_wolf");
        entityTextures.put("TIGER",  "char_tiger");
        entityTextures.put("LION",   "char_lion");
        entityTextures.put("EAGLE",  "char_eagle");
        entityTextures.put("ZOMBIE", "char_zombie");
        entityTextures.put("SPIDER", "char_spider");
    }

    public void render(EntityManager manager, ShaderProgram shader, TextureRegistry textures, Matrix4f viewMatrix) {
        for (Entity e : manager.getAll()) {
            if (e.isDead() || e.type == minicraft.entity.EntityType.PLAYER) continue;

            String typeName = e.type.name();
            Vector4f color = new Vector4f(1, 1, 1, 1);
            if (e.damageFlashTimer > 0) color = new Vector4f(1, 0.5f, 0.5f, 1);

            // --- HIGH-FIDELITY 3D NPC OVERRIDE ---
            if (typeName.equalsIgnoreCase("ZOMBIE")) {
                Mesh zombieMesh = ModelRegistry.getModel("zombie");
                if (zombieMesh != null) {
                    render3DNPC(e, zombieMesh, shader, color);
                    if (e.getHealth() < e.getMaxHealth()) renderHealthBar(e, shader, viewMatrix);
                    continue;
                }
            }

            String texName = entityTextures.getOrDefault(typeName, "stone");
            
            if (e instanceof minicraft.entity.ItemEntity) {
                texName = ((minicraft.entity.ItemEntity) e).block.sideTexture;
            }
            
            float yPos = e.position.y;
            float scaleX = e.width;
            float scaleY = e.height;

            if (e instanceof minicraft.entity.ItemEntity) {
                yPos += ((minicraft.entity.ItemEntity) e).getBobbingOffset();
                scaleX = 0.25f;
                scaleY = 0.25f;
            }

            Matrix4f model = new Matrix4f()
                .identity()
                .translate(e.position.x, yPos, e.position.z)
                .rotateY((float) Math.toRadians(e.yaw))
                .scale(scaleX, scaleY, scaleX);

            shader.setUniform("colorTint", color);
            shader.setUniform("modelMatrix", model);
            shader.setUniform("uTime", (float)(System.currentTimeMillis() % 100000) / 1000.0f);

            cubeMesh.render(textures.get(texName));

            // Render Health Bar for hit monsters
            if (e.type.category == minicraft.entity.EntityType.Category.MONSTER && e.getHealth() < e.getMaxHealth()) {
                renderHealthBar(e, shader, viewMatrix);
            }
        }
    }

    private void render3DNPC(Entity e, Mesh mesh, ShaderProgram shader, Vector4f color) {
        // High-fidelity transform for humanoid models
        Matrix4f model = new Matrix4f()
            .identity()
            .translate(e.position.x, e.position.y, e.position.z)
            .rotateY((float) Math.toRadians(-e.yaw + 180)) // Compressing Blender vs Engine heading
            .scale(0.8f, 0.8f, 0.8f); // High-fidelity scale (slightly taller than cube)

        shader.setUniform("colorTint", color);
        shader.setUniform("modelMatrix", model);
        mesh.render(null); // Uses internal high-fidelity texture
    }

    private void renderHealthBar(Entity e, ShaderProgram shader, Matrix4f viewMatrix) {
        float healthRatio = e.getHealth() / e.getMaxHealth();
        float width = 1.0f;
        float height = 0.1f;
        
        // Billboard rotation (facing camera)
        Matrix4f model = new Matrix4f()
            .identity()
            .translate(e.position.x, e.position.y + e.height + 0.5f, e.position.z);
        
        // Match camera yaw for billboarding
        model.rotateY((float) Math.toRadians(e.yaw)); // Simple approach for now

        // Background (Grey)
        shader.setUniform("colorTint", new Vector4f(0.2f, 0.2f, 0.2f, 1.0f));
        shader.setUniform("modelMatrix", new Matrix4f(model).scale(width, height, 0.1f));
        cubeMesh.render(null);

        // Health Fill (Green to Red)
        Vector4f hColor = new Vector4f(1.0f - healthRatio, healthRatio, 0.0f, 1.0f);
        shader.setUniform("colorTint", hColor);
        shader.setUniform("modelMatrix", new Matrix4f(model).scale(width * healthRatio, height, 0.11f));
        cubeMesh.render(null);
    }

    public void cleanup() {
        if (cubeMesh != null) cubeMesh.cleanup();
    }
}
