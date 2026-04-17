package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

public class Spider extends Entity {
    private float aggroRange = 18.0f;
    private float moveSpeed = 4.0f; // Fast!
    private float damageValue = 8.0f;
    private float attackTimer = 0f;

    public Spider() {
        super(EntityType.SPIDER, EntityType.SPIDER.baseHealth);
        this.width = EntityType.SPIDER.bodyWidth;
        this.height = EntityType.SPIDER.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) { state = EntityState.DEAD; return; }
        Player p = findNearbyPlayer(manager);
        if (p != null) {
            float distSq = distanceSq(p.position.x, p.position.y, p.position.z);
            if (distSq < aggroRange * aggroRange) {
                moveTo(p.position, dt);
                if (distSq < 2.5f * 2.5f && attackTimer <= 0) {
                    leapAttack(p);
                }
            } else {
                state = EntityState.IDLE;
                velocity.x *= 0.8f; velocity.z *= 0.8f;
            }
        }
        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
        if (attackTimer > 0) attackTimer -= dt;
    }

    private Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    private void moveTo(Vector3f target, float dt) {
        state = EntityState.WANDERING; 
        float dx = target.x - position.x;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dz*dz);
        if (len > 0.1f) {
            velocity.x += (dx / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }

    private void leapAttack(Player player) {
        attackTimer = 2.0f;
        velocity.y = 4.0f; // Leap up
        // Dash toward player
        float dx = player.position.x - position.x;
        float dz = player.position.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dz*dz);
        if (len > 0) {
            velocity.x = (dx / len) * 10f;
            velocity.z = (dz / len) * 10f;
        }
        player.damage(damageValue, this);
    }
}
