package minicraft.entity;

import java.util.Random;

/**
 * Base class for passive animals (Cow, Sheep, Ram, Dog, Cat, Whale).
 * Implements wander + graze + flee AI.
 */
public abstract class PassiveAnimal extends Entity {

    protected static final float WANDER_SPEED = 1.5f;
    protected static final float FLEE_SPEED   = 4.0f;
    protected static final float FLEE_RADIUS  = 8.0f;

    protected float wanderTimer  = 0f;
    protected float wanderDirX   = 0f;
    protected float wanderDirZ   = 0f;
    protected float idleTimer    = 0f;

    protected static final Random RNG = new Random();

    public PassiveAnimal(EntityType type) {
        super(type, type.baseHealth);
        this.width  = type.bodyWidth;
        this.height = type.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, float dt) {
        if (dead) { state = EntityState.DEAD; return; }

        // Check for nearby predators → flee
        boolean threatened = false;
        for (Entity nearby : manager.getNearby(
                position.x, position.y, position.z, FLEE_RADIUS)) {
            if (nearby.type.isPredator()) {
                threatened = true;
                fleeFrom(nearby, dt);
                break;
            }
        }

        if (!threatened) {
            wanderOrIdle(dt);
        }

        applyVelocity(world, dt);
        // Snap yaw to movement direction
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f) {
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
        }
    }

    protected void fleeFrom(Entity threat, float dt) {
        state = EntityState.FLEEING;
        float dx = position.x - threat.position.x;
        float dz = position.z - threat.position.z;
        float len = (float) Math.sqrt(dx*dx + dz*dz);
        if (len > 0) {
            velocity.x += (dx / len) * FLEE_SPEED * dt;
            velocity.z += (dz / len) * FLEE_SPEED * dt;
        }
    }

    protected void wanderOrIdle(float dt) {
        wanderTimer -= dt;
        idleTimer   -= dt;

        if (wanderTimer <= 0) {
            // Alternate between wandering and idling
            if (state == EntityState.IDLE || state == EntityState.EATING) {
                state = EntityState.WANDERING;
                float angle = (float)(RNG.nextDouble() * Math.PI * 2);
                wanderDirX  = (float) Math.sin(angle);
                wanderDirZ  = (float) Math.cos(angle);
                wanderTimer = 2f + RNG.nextFloat() * 4f;
            } else {
                state = RNG.nextFloat() < 0.3f ? EntityState.EATING : EntityState.IDLE;
                wanderTimer  = 1f + RNG.nextFloat() * 3f;
                wanderDirX   = 0;
                wanderDirZ   = 0;
            }
        }

        if (state == EntityState.WANDERING) {
            velocity.x += wanderDirX * WANDER_SPEED * dt;
            velocity.z += wanderDirZ * WANDER_SPEED * dt;
        }
    }
}
