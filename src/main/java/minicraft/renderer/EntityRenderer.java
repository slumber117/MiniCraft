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
                    if (e.getHealth() < e.getMaxHealth() || e.damageFlashTimer > 0) renderHealthBar(e, shader, viewMatrix);
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

            // Render Health Bar for damaged entities
            if (e.getHealth() < e.getMaxHealth() || e.isDead()) {
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
        if (healthRatio <= 0 && !e.isDead()) return;

        // 1. BILLBOARD CALCULATION
        // Extract Camera's Right and Up vectors from the view matrix (Rows 0 and 1)
        float rx = viewMatrix.m[0], ry = viewMatrix.m[4], rz = viewMatrix.m[8];
        float ux = viewMatrix.m[1], uy = viewMatrix.m[5], uz = viewMatrix.m[9];
        
        // 2. Final Model Matrix
        Matrix4f model = new Matrix4f().identity();
        
        // Translation (Center above entity head)
        model.m[12] = e.position.x;
        model.m[13] = e.position.y + e.height + 0.35f;
        model.m[14] = e.position.z;
        
        // Inverse Rotation (Billboard logic)
        // We set the model's orientation axes to match the camera's screen-plane axes
        model.m[0] = rx; model.m[1] = ry; model.m[2] = rz;
        model.m[4] = ux; model.m[5] = uy; model.m[6] = uz;
        // Facing vector (matches camera depth axis)
        model.m[8] = viewMatrix.m[2]; model.m[9] = viewMatrix.m[6]; model.m[10] = viewMatrix.m[10];

        // 3. BACKGROUND BAR (Shadow)
        shader.setUniform("colorTint", new Vector4f(0.05f, 0.05f, 0.05f, 0.85f));
        shader.setUniform("modelMatrix", new Matrix4f(model).scale(0.45f, 0.04f, 1.0f));
        cubeMesh.render(null);

        // 4. HEALTH FILL (Ruby to Emerald)
        Vector4f hColor = (healthRatio > 0.45f) ? new Vector4f(0.3f, 0.9f, 0.3f, 1.0f) : new Vector4f(0.95f, 0.15f, 0.15f, 1.0f);
        shader.setUniform("colorTint", hColor);
        
        // Apply scaling and a tiny offset forward (Z) to prevent flickering against bg
        shader.setUniform("modelMatrix", new Matrix4f(model)
            .scale(0.43f * healthRatio, 0.035f, 1.0f)
            .translate(0, 0, 0.01f)); 
        cubeMesh.render(null);
    }

    public void cleanup() {
        if (cubeMesh != null) cubeMesh.cleanup();
    }
}
