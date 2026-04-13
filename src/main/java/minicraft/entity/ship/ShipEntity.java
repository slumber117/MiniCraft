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

    public enum WeaponSystem {
        MAC("Magnetic Accelerator Cannon"),
        MISSILES("M42 Archer Missiles"),
        PDW("Point Defense Turrets");

        public final String displayName;
        WeaponSystem(String name) { this.displayName = name; }
    }

    private Player passenger = null;
    private WeaponSystem activeWeapon = WeaponSystem.MAC;
    
    // Flight Physics
    private float shipPitch = 0, shipYaw = 0, shipRoll = 0;
    private final Vector3f shipVelocity = new Vector3f();
    private float currentThrust = 0f; // 0.0 to 1.0
    
    // Stats
    private static final float MAX_ACCEL = 40.0f;
    private static final float DRIFT_FRICTION = 0.995f; // Very low friction for "weight"
    private static final float ROT_SPEED = 25f; // Heavier rotation

    public ShipEntity(EntityType type) {
        super(type, type.baseHealth);
        this.width = 15f; 
        this.height = 8f;
    }

    private float fireCooldown = 0f;

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        if (fireCooldown > 0) fireCooldown -= dt;
        
        // 1. Move the ship
        position.x += shipVelocity.x * dt;
        position.y += shipVelocity.y * dt;
        position.z += shipVelocity.z * dt;
        
        // 2. Apply Drift Friction (Newtonian-lite)
        shipVelocity.x *= DRIFT_FRICTION;
        shipVelocity.y *= DRIFT_FRICTION;
        shipVelocity.z *= DRIFT_FRICTION;

        // 3. Keep passenger in sync
        if (passenger != null) {
            passenger.position.set(position.x, position.y + 4.0f, position.z);
            passenger.velocity.set(0, 0, 0); 
            passenger.yaw = this.yaw;
        }

        // 4. Update Thrust Visual State & Spawn Particles
        if (currentThrust > 0) {
            spawnThrusterPlumes(particleManager, dt);
            currentThrust -= dt * 0.5f; 
        }
        if (currentThrust < 0) currentThrust = 0;

        // Clamp world bounds
        if (position.y < 0) position.y = 0;
        if (position.y > 400) position.y = 400;
    }

    public void fireActiveWeapon(EntityManager manager, World world, minicraft.entity.ParticleManager pm, org.joml.Vector3f lookDir) {
        if (fireCooldown > 0) return;

        switch (activeWeapon) {
            case MAC:
                fireMAC(world, pm, lookDir);
                fireCooldown = 3.0f; // Long reload
                break;
            case MISSILES:
                fireMissile(manager, lookDir);
                fireCooldown = 0.8f;
                break;
            case PDW:
                firePDW(world, pm, lookDir);
                fireCooldown = 0.1f; // High rate of fire
                break;
        }
    }

    private void fireMAC(World world, minicraft.entity.ParticleManager pm, org.joml.Vector3f lookDir) {
        // Find MAC barrel tip (front of ship)
        float cos = (float) Math.cos(Math.toRadians(yaw));
        float sin = (float) Math.sin(Math.toRadians(yaw));
        
        minicraft.math.Vector3f barrelPos = new minicraft.math.Vector3f(
            position.x - sin * 40f, 
            position.y + 1f, 
            position.z - cos * 40f
        );

        minicraft.math.Vector3f dir = new minicraft.math.Vector3f(lookDir.x, lookDir.y, lookDir.z);
        float dist = world.raycast(barrelPos, dir, 500f);
        
        // Visual Beam Effect
        for (float d = 0; d < dist; d += 2.0f) {
            pm.spawnThruster(
                barrelPos.x + dir.x * d,
                barrelPos.y + dir.y * d,
                barrelPos.z + dir.z * d,
                new minicraft.math.Vector3f(0,0,0),
                2.5f,
                new minicraft.math.Vector4f(1.0f, 1.0f, 1.0f, 1.0f), // White hot beam
                0.2f
            );
        }
        
        // Impact effect
        pm.spawnSmoke(barrelPos.x + dir.x * dist, barrelPos.y + dir.y * dist, barrelPos.z + dir.z * dist);
        System.out.println("MAC HIT: " + dist + "m");
    }

    private void fireMissile(EntityManager manager, org.joml.Vector3f lookDir) {
        // Launch from top silos
        minicraft.math.Vector3f siloPos = new minicraft.math.Vector3f(position.x, position.y + 5f, position.z);
        minicraft.math.Vector3f fwd = new minicraft.math.Vector3f(lookDir.x, lookDir.y, lookDir.z);
        manager.spawn(new minicraft.entity.weapons.Missile(this, siloPos, fwd));
    }

    private void firePDW(World world, minicraft.entity.ParticleManager pm, org.joml.Vector3f lookDir) {
        // Rapid tracers
        minicraft.math.Vector3f muzzle = new minicraft.math.Vector3f(position.x, position.y + 2f, position.z);
        minicraft.math.Vector3f dir = new minicraft.math.Vector3f(lookDir.x, lookDir.y, lookDir.z);
        
        float dist = world.raycast(muzzle, dir, 80f);
        // Small tracer point
        pm.spawnThruster(
            muzzle.x + dir.x * dist,
            muzzle.y + dir.y * dist,
            muzzle.z + dir.z * dist,
            new minicraft.math.Vector3f(0,0,0),
            0.5f,
            new minicraft.math.Vector4f(1.0f, 0.8f, 0.2f, 1.0f),
            0.05f
        );
    }

    private void spawnThrusterPlumes(minicraft.entity.ParticleManager pm, float dt) {
        // Find "Engine" positions relative to ship (rear)
        // For the Stalwart, engines are roughly at z = 30-40 (depending on origin)
        // Let's assume origin 0,0,0 is mid-ship.
        
        float cos = (float) Math.cos(Math.toRadians(yaw));
        float sin = (float) Math.sin(Math.toRadians(yaw));
        
        // Tail exhaust positions
        float[][] engines = {
            {-5, 0, 35}, {5, 0, 35}, // Main engines
            {-8, 4, 32}, {8, 4, 32}  // Nacelles
        };
        
        for (float[] engine : engines) {
            // Rotate local offset to world space
            float rx = engine[0] * cos - engine[2] * sin;
            float rz = engine[0] * sin + engine[2] * cos;
            
            float ex = position.x + rx;
            float ey = position.y + engine[1];
            float ez = position.z + rz;
            
            // Thrust vector (opposite of forward)
            minicraft.math.Vector3f pVel = new minicraft.math.Vector3f(sin * 10f, 0, -cos * 10f);
            
            float size = 1.0f + currentThrust * 2.5f;
            minicraft.math.Vector4f color = new minicraft.math.Vector4f(0.3f, 0.6f, 1.0f, 0.8f); // Blue Afterburner
            
            pm.spawnThruster(ex, ey, ez, pVel, size, color, 0.5f + currentThrust * 0.5f);
        }
    }


    public void handleInput(boolean w, boolean s, boolean a, boolean d, boolean space, boolean shift, float dt) {
        float fx = (float) -Math.sin(Math.toRadians(yaw));
        float fz = (float) -Math.cos(Math.toRadians(yaw));

        if (w) {
            shipVelocity.x += fx * MAX_ACCEL * dt;
            shipVelocity.z += fz * MAX_ACCEL * dt;
            currentThrust = Math.min(1.0f, currentThrust + dt * 2.0f);
        }
        if (s) {
            // Reverse / Braking thrust
            shipVelocity.x -= fx * MAX_ACCEL * 0.6f * dt;
            shipVelocity.z -= fz * MAX_ACCEL * 0.6f * dt;
            currentThrust = Math.min(1.0f, currentThrust + dt * 1.5f);
        }

        // Rotation (Yaw) with heavier feel
        if (a) yaw += ROT_SPEED * dt;
        if (d) yaw -= ROT_SPEED * dt;

        // Elevation
        if (space) shipVelocity.y += MAX_ACCEL * 0.5f * dt;
        if (shift) shipVelocity.y -= MAX_ACCEL * 0.5f * dt;
    }

    public void nextWeapon() {
        int next = (activeWeapon.ordinal() + 1) % WeaponSystem.values().length;
        activeWeapon = WeaponSystem.values()[next];
    }

    public void prevWeapon() {
        int prev = (activeWeapon.ordinal() - 1 + WeaponSystem.values().length) % WeaponSystem.values().length;
        activeWeapon = WeaponSystem.values()[prev];
    }

    public WeaponSystem getActiveWeapon() { return activeWeapon; }
    public float getThrustLevel() { return currentThrust; }

    public void setPassenger(Player p) { this.passenger = p; }
    public Player getPassenger() { return passenger; }
    public boolean hasPassenger() { return passenger != null; }
}
