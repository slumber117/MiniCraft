package minicraft.entity.npcs;

import minicraft.entity.*;

import java.util.Random;

/**
 * Farmer NPC – wanders around a village area during the day.
 * Will flee from predators. Placeholder for trading/dialogue in future.
 */
public class Farmer extends Entity {

    private static final float WANDER_SPEED = 1.2f;
    private static final float FLEE_SPEED   = 3.5f;
    private static final float FLEE_RADIUS  = 10f;
    private static final Random RNG = new Random();

    private float wanderTimer = 0f;
    private float wanderDirX  = 0f;
    private float wanderDirZ  = 0f;

    // Home position — farmer wanders near this point
    private final float homeX, homeZ;
    private static final float HOME_RADIUS = 12f;

    public Farmer() {
        super(EntityType.FARMER, EntityType.FARMER.baseHealth);
        this.homeX = position.x;
        this.homeZ = position.z;
        this.width  = EntityType.FARMER.bodyWidth;
        this.height = EntityType.FARMER.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, float dt) {
        if (dead) { state = EntityState.DEAD; return; }

        // Flee from predators
        boolean threatened = false;
        for (Entity nearby : manager.getNearby(position.x, position.y, position.z, FLEE_RADIUS)) {
            if (nearby.type.isPredator()) {
                threatened = true;
                float dx = position.x - nearby.position.x;
                float dz = position.z - nearby.position.z;
                float len = (float) Math.sqrt(dx*dx + dz*dz);
                if (len > 0) {
                    velocity.x += (dx / len) * FLEE_SPEED * dt;
                    velocity.z += (dz / len) * FLEE_SPEED * dt;
                }
                state = EntityState.FLEEING;
                break;
            }
        }

        if (!threatened) {
            wanderNearHome(dt);
        }

        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f) {
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
        }
    }

    private void wanderNearHome(float dt) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            if (state == EntityState.WANDERING) {
                state = EntityState.IDLE;
                wanderDirX = 0; wanderDirZ = 0;
                wanderTimer = 1.5f + RNG.nextFloat() * 2f;
            } else {
                // Bias wander direction back toward home if too far
                float dxHome = homeX - position.x;
                float dzHome = homeZ - position.z;
                float distHome = (float) Math.sqrt(dxHome*dxHome + dzHome*dzHome);

                float angle;
                if (distHome > HOME_RADIUS) {
                    angle = (float) Math.atan2(dzHome, dxHome)
                            + (float)((RNG.nextDouble() - 0.5) * Math.PI * 0.5);
                } else {
                    angle = (float)(RNG.nextDouble() * Math.PI * 2);
                }
                wanderDirX  = (float) Math.sin(angle);
                wanderDirZ  = (float) Math.cos(angle);
                state       = EntityState.WANDERING;
                wanderTimer = 2f + RNG.nextFloat() * 3f;
            }
        }
        if (state == EntityState.WANDERING) {
            velocity.x += wanderDirX * WANDER_SPEED * dt;
            velocity.z += wanderDirZ * WANDER_SPEED * dt;
        }
    }
}
