package minicraft.entity;

import minicraft.renderer.Mesh;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Matrix4f;
import minicraft.math.Vector3f;
import minicraft.math.Vector4f;
import java.util.*;

public class ParticleManager {
    private static class Particle {
        Vector3f pos;
        Vector3f vel;
        float life;
        float maxLife;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Mesh quad;
    private final Random rand = new Random();

    public ParticleManager() {
        // Create a small vertical 1x1 quad mesh for particles
        float[] pos = { -0.5f, 0, 0,  0.5f, 0, 0,  0.5f, 1, 0,  -0.5f, 1, 0 };
        float[] uv  = { 0, 1, 1, 1, 1, 0, 0, 0 };
        int[]   idx = { 0, 1, 2, 2, 3, 0 };
        float[] lit = { 1, 1, 1, 1, 1, 1, 1, 1 }; 
        this.quad = new Mesh(pos, uv, lit, idx, null);
    }

    public void spawnSmoke(float x, float y, float z) {
        Particle p = new Particle();
        p.pos = new Vector3f(x + (rand.nextFloat() - 0.5f) * 0.1f, y + 0.6f, z + (rand.nextFloat() - 0.5f) * 0.1f);
        p.vel = new Vector3f((rand.nextFloat() - 0.5f) * 0.1f, 0.4f + rand.nextFloat() * 0.2f, (rand.nextFloat() - 0.5f) * 0.1f);
        p.maxLife = 1.0f + rand.nextFloat() * 1.5f;
        p.life = p.maxLife;
        particles.add(p);
    }

    public void update(float dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.pos.x += p.vel.x * dt;
            p.pos.y += p.vel.y * dt;
            p.pos.z += p.vel.z * dt;
            p.life -= dt;
            if (p.life <= 0) it.remove();
        }
        if (particles.size() > 500) particles.remove(0);
    }

    public void render(ShaderProgram shader, Matrix4f view, Matrix4f proj) {
        shader.setUniform("useLighting", 0.0f);
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            float scale = 0.05f + (1.0f - alpha) * 0.1f;
            
            Matrix4f model = new Matrix4f()
                .identity()
                .translate(p.pos.x, p.pos.y, p.pos.z)
                .scale(scale, scale, scale);
                
            // Billboard rotation (reset orientation to match camera view)
            model.m[0] = scale; model.m[1] = 0; model.m[2] = 0;
            model.m[4] = 0; model.m[5] = scale; model.m[6] = 0;
            model.m[8] = 0; model.m[9] = 0; model.m[10] = scale;

            shader.setUniform("modelMatrix", model);
            shader.setUniform("colorTint", new Vector4f(0.5f, 0.5f, 0.5f, alpha * 0.6f));
            quad.render(null);
        }
        shader.setUniform("useLighting", 1.0f);
    }
}
