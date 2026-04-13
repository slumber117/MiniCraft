package minicraft.entity;

import minicraft.math.Vector3f;
import minicraft.world.World;

/**
 * Projectile — Base class for all high-velocity ship-launched objects.
 */
public abstract class Projectile extends Entity {
    
    protected float life = 5.0f; // Seconds before cleanup
    protected Entity owner;

    public Projectile(EntityType type, Entity owner, float speed) {
        super(type, 1.0f);
        this.owner = owner;
        this.velocity.set(0, 0, 0); // Speed usually set by subclass firing logic
    }

    @Override
    public void tick(EntityManager manager, World world, ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        life -= dt;
        if (life <= 0) dead = true;
        
        // Move with collision
        position.x += velocity.x * dt;
        position.y += velocity.y * dt;
        position.z += velocity.z * dt;
        
        // Check for impact
        int bx = (int) Math.floor(position.x);
        int by = (int) Math.floor(position.y);
        int bz = (int) Math.floor(position.z);
        
        if (world.getBlock(bx, by, bz).solid) {
            onImpact(world, pm);
            dead = true;
        }
    }

    protected abstract void onImpact(World world, ParticleManager pm);
}
