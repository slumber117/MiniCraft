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
        Vector4f color = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        float scale = 0.1f;
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
        p.scale = 0.1f + rand.nextFloat() * 0.1f;
        p.color = new Vector4f(0.4f, 0.4f, 0.4f, 0.6f);
        particles.add(p);
    }

    public void spawnExplosion(float x, float y, float z, float intensity) {
        int count = (int) (20 * intensity);
        for (int i = 0; i < count; i++) {
            // Thermal Flash (Fire)
            Vector3f fireballVel = new Vector3f(
                (rand.nextFloat() - 0.5f) * 12f * intensity,
                (rand.nextFloat() - 0.5f) * 12f * intensity,
                (rand.nextFloat() - 0.5f) * 12f * intensity
            );
            spawnThruster(x, y, z, fireballVel, 2.5f * intensity, 
                new Vector4f(1.0f, 0.6f, 0.0f, 1.0f), 0.5f + rand.nextFloat() * 0.5f);
            
            // Plume (Smoke)
            spawnSmoke(x + (rand.nextFloat()-0.5f) * 2f, y + (rand.nextFloat()-0.5f) * 2f, z + (rand.nextFloat()-0.5f) * 2f);
        }
    }

    public void spawnThruster(float x, float y, float z, Vector3f velocity, float size, Vector4f color, float life) {
        Particle p = new Particle();
        p.pos = new Vector3f(x, y, z);
        p.vel = new Vector3f(velocity);
        // Add slight jitter
        p.vel.x += (rand.nextFloat() - 0.5f) * 0.5f;
        p.vel.y += (rand.nextFloat() - 0.5f) * 0.5f;
        p.vel.z += (rand.nextFloat() - 0.5f) * 0.5f;
        
        p.maxLife = life;
        p.life = life;
        p.scale = size;
        p.color = new Vector4f(color);
        particles.add(p);
    }

    public void spawnLevelUp(float x, float y, float z) {
        int count = 60;
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float speed = 2.0f + (float) Math.random() * 3.0f;
            Vector3f vel = new Vector3f(
                (float) Math.cos(angle) * speed,
                (float) Math.random() * 5.0f,
                (float) Math.sin(angle) * speed
            );
            // Gold color
            Vector4f color = new Vector4f(1.0f, 0.84f, 0.0f, 1.0f);
            spawnThruster(x, y, z, vel, 0.3f, color, 1.5f + (float) Math.random());
        }
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
        if (particles.size() > 2000) particles.remove(0); // Increased cap for ship battle
    }

    public void render(ShaderProgram shader, minicraft.renderer.TextureRegistry registry, Matrix4f view, Matrix4f proj) {
        shader.setUniform("useLighting", 0.0f);
        minicraft.renderer.Texture particleTex = (registry != null) ? registry.get("alloy_plate") : null;
        
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            float currentScale = p.scale * (0.5f + alpha * 0.5f);
            
            Matrix4f model = new Matrix4f()
                .identity()
                .translate(p.pos.x, p.pos.y, p.pos.z)
                .scale(currentScale, currentScale, currentScale);
                
            // Billboard rotation (keep facing camera)
            shader.setUniform("modelMatrix", model);
            
            Vector4f drawColor = new Vector4f(p.color);
            drawColor.w *= alpha; // Fade out
            shader.setUniform("colorTint", drawColor);
            quad.render(particleTex);
        }
        shader.setUniform("useLighting", 1.0f);
    }
}
