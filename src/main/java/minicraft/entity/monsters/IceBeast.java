package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

public class IceBeast extends Entity {
    private float aggroRange = 40.0f;
    private float attackRange = 3.5f;
    private float moveSpeed = 1.6f;
    private float damageValue = 45.0f;
    private float attackCooldown = 1.8f;
    private float attackTimer = 0f;

    public IceBeast() {
        super(EntityType.ICE_BEAST, EntityType.ICE_BEAST.baseHealth);
        this.width = EntityType.ICE_BEAST.bodyWidth;
        this.height = EntityType.ICE_BEAST.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) { state = EntityState.DEAD; return; }
        
        Player p = findNearbyPlayer(manager);
        if (p != null) {
            float dSq = distanceSq(p.position.x, p.position.y, p.position.z);
            if (dSq < aggroRange * aggroRange) {
                moveTo(p.position, dt);
                if (dSq < attackRange * attackRange) tryAttack(p, dt);
            } else {
                state = EntityState.IDLE;
                velocity.x *= 0.9f; velocity.z *= 0.9f;
            }
        }
        
        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
            
        if (attackTimer > 0) attackTimer -= dt;
        
        // Blizzard effect around the beast
        if (rng.nextFloat() < 0.3f) {
            float ox = (rng.nextFloat() * 4 - 2);
            float oz = (rng.nextFloat() * 4 - 2);
            particleManager.spawnSmoke(position.x + ox, position.y + height/2, position.z + oz);
        }
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

    private void tryAttack(Player player, float dt) {
        if (attackTimer <= 0) {
            player.damage(damageValue, this);
            attackTimer = attackCooldown;
            // Heavy Knockback
            velocity.y = 2.5f;
            player.velocity.y = 5.0f;
            player.velocity.x += (player.position.x - position.x) * 2.0f;
            player.velocity.z += (player.position.z - position.z) * 2.0f;
        }
    }
}
