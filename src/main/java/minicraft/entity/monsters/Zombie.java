package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

public class Zombie extends Entity {
    private float aggroRange = 16.0f;
    private float attackRange = 1.5f;
    private float moveSpeed = 1.25f;
    private float damageValue = 10.0f;
    private float attackCooldown = 1.5f;
    private float attackTimer = 0f;

    public Zombie() {
        super(EntityType.ZOMBIE, EntityType.ZOMBIE.baseHealth);
        this.width = EntityType.ZOMBIE.bodyWidth;
        this.height = EntityType.ZOMBIE.bodyHeight;
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
            velocity.y = 1.5f; 
        }
    }
}
