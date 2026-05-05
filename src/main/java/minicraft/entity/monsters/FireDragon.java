package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

import java.util.Random;

public class FireDragon extends Entity {
    private float aggroRange = 64.0f;
    private float fireRange = 40.0f;
    private float moveSpeed = 8.0f;
    private float fireCooldown = 3.0f;
    private float fireTimer = 0f;
    private Random rng = new Random();

    public FireDragon() {
        super(EntityType.FIRE_DRAGON, EntityType.FIRE_DRAGON.baseHealth);
        this.width = EntityType.FIRE_DRAGON.bodyWidth;
        this.height = EntityType.FIRE_DRAGON.bodyHeight;
        this.state = EntityState.FLYING;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) { state = EntityState.DEAD; return; }
        
        Player p = findNearbyPlayer(manager);
        if (p != null) {
            float dSq = distanceSq(p.position.x, p.position.y, p.position.z);
            if (dSq < aggroRange * aggroRange) {
                // Flying AI: Hover above and circle
                Vector3f targetPos = new Vector3f(p.position.x, p.position.y + 15.0f, p.position.z);
                moveTo(targetPos, dt);
                
                if (dSq < fireRange * fireRange) {
                    tryFire(p, manager, dt);
                }
            } else {
                state = EntityState.IDLE;
                velocity.x *= 0.95f; velocity.z *= 0.95f; velocity.y *= 0.95f;
            }
        } else {
            // Passive flight
            velocity.y += (float) Math.sin(System.currentTimeMillis() / 1000.0) * 0.1f;
        }

        // Apply movement
        position.x += velocity.x * dt;
        position.y += velocity.y * dt;
        position.z += velocity.z * dt;
        
        // Simple friction
        velocity.x *= 0.98f;
        velocity.y *= 0.98f;
        velocity.z *= 0.98f;

        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
            
        if (fireTimer > 0) fireTimer -= dt;
    }

    private Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    private void moveTo(Vector3f target, float dt) {
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 1.0f) {
            velocity.x += (dx / len) * moveSpeed * dt;
            velocity.y += (dy / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }

    private void tryFire(Player player, EntityManager manager, float dt) {
        if (fireTimer <= 0) {
            float dx = player.position.x - position.x;
            float dy = (player.position.y + 1.0f) - position.y;
            float dz = player.position.z - position.z;
            float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            float projectileSpeed = 20.0f;
            if (len > 0) {
                Fireball fireball = new Fireball(this, (dx / len) * projectileSpeed, (dy / len) * projectileSpeed, (dz / len) * projectileSpeed);
                fireball.setPosition(position.x, position.y, position.z);
                manager.spawn(fireball);
                fireTimer = fireCooldown + rng.nextFloat();
            }
        }
    }
}
