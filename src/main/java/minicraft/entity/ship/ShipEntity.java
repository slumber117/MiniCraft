package minicraft.entity.ship;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

/**
 * ShipEntity — A massive, driveable megastructure.
 * Supports flying, rotation, and passenger mounting.
 */
public class ShipEntity extends Entity {

    private Player passenger = null;
    
    // Flight Physics
    private float shipPitch = 0, shipYaw = 0, shipRoll = 0;
    private final Vector3f shipVelocity = new Vector3f();
    
    // Stats
    private static final float ACCEL = 15.0f;
    private static final float FRICTION = 0.98f;
    private static final float ROT_SPEED = 45f; // Deg per sec

    public ShipEntity(EntityType type) {
        super(type, type.baseHealth);
        this.width = 10f; // Simplified collision box
        this.height = 5f;
    }

    @Override
    public void tick(EntityManager manager, World world, float dt) {
        // 1. Move the ship
        position.x += shipVelocity.x * dt;
        position.y += shipVelocity.y * dt;
        position.z += shipVelocity.z * dt;
        
        // 2. Apply Friction
        shipVelocity.x *= FRICTION;
        shipVelocity.y *= FRICTION;
        shipVelocity.z *= FRICTION;

        // 3. Keep passenger in sync
        if (passenger != null) {
            // Anchor player to "cockpit" (relative offset)
            passenger.position.set(position.x, position.y + 2.0f, position.z);
            passenger.velocity.set(0, 0, 0); // Player doesn't move relative to ship
            passenger.yaw = this.yaw;
        }

        // Clamp world bounds
        if (position.y < 0) position.y = 0;
        if (position.y > 400) position.y = 400;
    }

    public void handleInput(boolean w, boolean s, boolean a, boolean d, boolean space, boolean shift, float dt) {
        // Forward / Backward based on Yaw
        float fx = (float) -Math.sin(Math.toRadians(yaw));
        float fz = (float) -Math.cos(Math.toRadians(yaw));

        if (w) {
            shipVelocity.x += fx * ACCEL * dt;
            shipVelocity.z += fz * ACCEL * dt;
        }
        if (s) {
            shipVelocity.x -= fx * ACCEL * dt;
            shipVelocity.z -= fz * ACCEL * dt;
        }

        // Rotation (Yaw)
        if (a) yaw += ROT_SPEED * dt;
        if (d) yaw -= ROT_SPEED * dt;

        // Elevation
        if (space) shipVelocity.y += ACCEL * 0.8f * dt;
        if (shift) shipVelocity.y -= ACCEL * 0.8f * dt;
    }

    public void setPassenger(Player p) {
        this.passenger = p;
    }

    public Player getPassenger() {
        return passenger;
    }

    public boolean hasPassenger() {
        return passenger != null;
    }
}
