package minicraft.renderer;

import minicraft.math.Vector3f;
import minicraft.world.WeatherManager;
import java.util.Random;

/**
 * Renders localized weather particles (Rain/Snow) around the player.
 */
public class WeatherRenderer {

    private static final int PARTICLE_COUNT = 1000;
    private final float[] particlePositions = new float[PARTICLE_COUNT * 3];
    private Mesh particleMesh;
    private float fallSpeed = 20.0f;
    private Random random = new Random();

    public WeatherRenderer() {
        // Create a single 0.1x0.1 quad mesh for all particles (using instancing or batching)
        // For simplicity in this engine, we'll use a simple batching approach or multiple draw calls.
        // Actually, let's just make one large mesh that we rebuild? No, too slow.
        // We'll use a simple Cross mesh like blocks.
        float[] positions = new float[] {
            -0.05f, 0.5f, 0,  0.05f, 0.5f, 0,  0.05f, -0.5f, 0, -0.05f, -0.5f, 0
        };
        float[] uvs = new float[] { 0,1, 1,1, 1,0, 0,0 };
        int[] indices = new int[] { 0,1,2, 2,3,0 };
        particleMesh = new Mesh(positions, uvs, null, indices, null);

        // Initialize particles in a 30x30x30 volume
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i);
            particlePositions[i*3 + 1] = random.nextFloat() * 30.0f; // Randomize initial height
        }
    }

    private void resetParticle(int i) {
        particlePositions[i*3]     = (random.nextFloat() - 0.5f) * 40.0f;
        particlePositions[i*3 + 1] = 20.0f; // Start high
        particlePositions[i*3 + 2] = (random.nextFloat() - 0.5f) * 40.0f;
    }

    public void render(Vector3f playerPos, WeatherManager weather, ShaderProgram shader, float dt) {
        WeatherManager.WeatherType type = weather.getCurrentType();
        if (type == WeatherManager.WeatherType.CLEAR) return;

        float intensity = weather.getIntensity();
        int count = (int)(PARTICLE_COUNT * Math.min(1.0f, intensity));
        
        minicraft.math.Vector4f color;
        if (type == WeatherManager.WeatherType.SNOW || type == WeatherManager.WeatherType.BLIZZARD) {
            color = new minicraft.math.Vector4f(1.0f, 1.0f, 1.0f, 0.8f);
            fallSpeed = 5.0f;
        } else {
            color = new minicraft.math.Vector4f(0.5f, 0.5f, 1.0f, 0.4f);
            fallSpeed = 25.0f;
        }

        shader.setUniform("colorTint", color);
        shader.setUniform("useLighting", 0.0f); // Particles don't receive lighting usually

        for (int i = 0; i < count; i++) {
            particlePositions[i*3 + 1] -= fallSpeed * dt;
            
            // If it falls below "ground" relative to player, reset it
            if (particlePositions[i*3 + 1] < -10.0f) {
                resetParticle(i);
            }

            minicraft.math.Matrix4f model = new minicraft.math.Matrix4f()
                .identity()
                .translate(playerPos.x + particlePositions[i*3], 
                           playerPos.y + particlePositions[i*3 + 1], 
                           playerPos.z + particlePositions[i*3 + 2])
                .scale(1.0f, 1.0f, 1.0f);
            
            shader.setUniform("modelMatrix", model);
            particleMesh.render(null); // Render as white/colored quad
        }
        
        shader.setUniform("useLighting", 1.0f);
    }

    public void cleanup() {
        if (particleMesh != null) particleMesh.cleanup();
    }
}
