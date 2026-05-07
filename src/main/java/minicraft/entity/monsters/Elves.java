package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

/**
 * Forest-dwelling Elves — peaceful by default.
 * They roam the forest passively and only become hostile
 * when attacked by the player (retaliatory AI).
 */
public class Elves extends Entity {
    private float aggroRange = 32.0f;
    private float attackRange = 3.5f;
    private float moveSpeed = 2.5f;
    private float damageValue = 50.0f;
    private float attackCooldown = 2.0f;
    private float attackTimer = 0f;

    // Retaliation state
    private boolean provoked = false;
    private float provokedTimer = 0f;
    private static final float PROVOKED_DURATION = 30.0f;

    // Roaming
    private float roamTimer = 0f;
    private float roamDirX = 0f;
    private float roamDirZ = 0f;

    public Elves() {
        super(EntityType.ELVES, EntityType.ELVES.baseHealth);
        this.width = EntityType.ELVES.bodyWidth;
        this.height = EntityType.ELVES.bodyHeight;
        pickNewRoamDirection();
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) { state = EntityState.DEAD; return; }

        if (attackTimer > 0) attackTimer -= dt;

        // Decay provocation over time
        if (provoked) {
            provokedTimer -= dt;
            if (provokedTimer <= 0) {
                provoked = false;
                state = EntityState.IDLE;
            }
        }

        Player p = findNearbyPlayer(manager);

        if (provoked && p != null) {
            // Hostile: chase and attack
            float dSq = distanceSq(p.position.x, p.position.y, p.position.z);
            if (dSq < aggroRange * aggroRange) {
                moveTo(p.position, dt);
                if (dSq < attackRange * attackRange) tryAttack(p, dt);
            } else {
                roam(world, dt);
            }
        } else {
            // Passive: wander the forest
            roam(world, dt);
        }

        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
    }

    @Override
    public void damage(float amount, Entity attacker) {
        super.damage(amount, attacker);
        if (attacker instanceof Player) {
            provoked = true;
            provokedTimer = PROVOKED_DURATION;
        }
    }

    private void roam(World world, float dt) {
        state = EntityState.WANDERING;
        roamTimer -= dt;
        if (roamTimer <= 0) {
            pickNewRoamDirection();
        }
        float roamSpeed = moveSpeed * 0.3f;
        velocity.x += roamDirX * roamSpeed * dt;
        velocity.z += roamDirZ * roamSpeed * dt;
        velocity.x *= 0.92f;
        velocity.z *= 0.92f;
    }

    private void pickNewRoamDirection() {
        float angle = rng.nextFloat() * (float)(Math.PI * 2);
        roamDirX = (float) Math.cos(angle);
        roamDirZ = (float) Math.sin(angle);
        roamTimer = 3.0f + rng.nextFloat() * 5.0f;
    }

    private Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    private void moveTo(Vector3f target, float dt) {
        state = EntityState.WANDERING;
        float dx = target.x - position.x;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
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
