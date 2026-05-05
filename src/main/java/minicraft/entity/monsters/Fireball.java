package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityType;
import minicraft.entity.ParticleManager;
import minicraft.entity.Projectile;
import minicraft.world.World;

public class Fireball extends Projectile {
    private float damage = 25.0f;

    public Fireball(Entity owner, float vx, float vy, float vz) {
        super(EntityType.FIREBALL, owner, 0);
        this.velocity.set(vx, vy, vz);
        this.width = 0.8f;
        this.height = 0.8f;
        this.life = 8.0f;
    }

    @Override
    protected void onImpact(World world, ParticleManager pm) {
        // Explosion effects could go here
    }

    @Override
    public void tick(minicraft.entity.EntityManager manager, World world, ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        
        // Check for player hit
        for (Entity e : manager.getNearby(position.x, position.y, position.z, 1.5f)) {
            if (e != owner && e.getType() == EntityType.PLAYER) {
                e.damage(damage, owner);
                dead = true;
                break;
            }
        }
    }
}
