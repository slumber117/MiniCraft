package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityType;
import minicraft.entity.ParticleManager;
import minicraft.entity.Projectile;
import minicraft.world.World;

public class OnyxProjectile extends Projectile {
    private float damage = 40.0f;

    public OnyxProjectile(Entity owner, float vx, float vy, float vz) {
        super(EntityType.ONYX_PROJECTILE, owner, 0);
        this.velocity.set(vx, vy, vz);
        this.width = 0.6f;
        this.height = 0.6f;
        this.life = 10.0f;
    }

    @Override
    protected void onImpact(World world, ParticleManager pm) {
        // Impact particles
    }

    @Override
    public void tick(minicraft.entity.EntityManager manager, World world, ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        for (Entity e : manager.getNearby(position.x, position.y, position.z, 1.2f)) {
            if (e != owner && e.getType() == EntityType.PLAYER) {
                e.damage(damage, owner);
                dead = true;
                break;
            }
        }
    }
}
