package minicraft.entity.weapons;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.ParticleManager;
import minicraft.entity.Projectile;
import minicraft.math.Vector3f;
import minicraft.math.Vector4f;
import minicraft.world.World;

public class Missile extends Projectile {

    private Vector3f forward;
    private float acceleration = 35.0f;
    private float smokeTimer = 0f;

    public Missile(Entity owner, Vector3f startPos, Vector3f forward) {
        super(EntityType.SHIP_MISSILE, owner, 15.0f);
        this.position.set(startPos);
        this.forward = new Vector3f(forward).normalize();
        this.velocity.set(forward).mul(15.0f); // Launch velocity
        this.life = 6.0f;
    }

    @Override
    public void tick(EntityManager manager, World world, ParticleManager pm, float dt) {
        // Accelerate along forward path
        velocity.x += forward.x * acceleration * dt;
        velocity.y += forward.y * acceleration * dt;
        velocity.z += forward.z * acceleration * dt;
        
        super.tick(manager, world, pm, dt);
        
        // Spawn smoke trail
        smokeTimer += dt;
        if (smokeTimer > 0.05f) {
            pm.spawnSmoke(position.x, position.y, position.z);
            smokeTimer = 0;
            
            // Thrust glow
            pm.spawnThruster(position.x, position.y, position.z, 
                new Vector3f(velocity).mul(-0.2f), 0.4f, 
                new Vector4f(1.0f, 0.4f, 0.1f, 0.8f), 0.2f);
        }
    }

    @Override
    protected void onImpact(World world, ParticleManager pm) {
        // Create explosion effect
        for (int i = 0; i < 20; i++) {
            pm.spawnSmoke(position.x, position.y, position.z);
            pm.spawnThruster(position.x, position.y, position.z,
                new Vector3f((float)Math.random()-0.5f, (float)Math.random()-0.5f, (float)Math.random()-0.5f).mul(5f),
                1.5f, new Vector4f(1.0f, 0.5f, 0.0f, 1.0f), 0.8f);
        }
        System.out.println("MISSILE IMPACT at " + position);
    }
}
