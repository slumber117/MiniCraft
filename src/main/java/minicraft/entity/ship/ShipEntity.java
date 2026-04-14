package minicraft.entity.ship;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;
import minicraft.ship.ShipClass;

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
    
    // Telemetry Stats
    private float maxShield = 1000f, shield = 1000f;
    private float maxEnergy = 1000f, energy = 1000f;
    private float maxFuel = 1000f, fuel = 1000f;
    
    // Stats (Instance-based, scaled by ShipClass)
    private float acceleration;
    private float rotationSpeed;
    private float driftFriction;
    private final minicraft.ship.ShipDefinition definition;

    public ShipEntity(EntityType type, minicraft.ship.ShipDefinition definition) {
        super(type, type.baseHealth);
        this.definition = definition;
        this.width = 25f; // Larger collision for frigate
        this.height = 12f;

        // Scale physics by class
        ShipClass sc = definition.shipClass;
        this.acceleration = 120.0f * sc.accelerationMultiplier;
        this.rotationSpeed = 35.0f * sc.accelerationMultiplier;
        this.driftFriction = 0.98f + (0.015f * (1.0f - sc.accelerationMultiplier)); 
    }

    public minicraft.ship.ShipDefinition getDefinition() {
        return definition;
    }

    public ShipClass getShipClass() {
        return definition.shipClass;
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
        shipVelocity.x *= driftFriction;
        shipVelocity.y *= driftFriction;
        shipVelocity.z *= driftFriction;

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
            fuel = Math.max(0, fuel - 5.0f * dt); // Fuel consumption
        }
        if (currentThrust < 0) currentThrust = 0;

        // Energy Recharge
        energy = Math.min(maxEnergy, energy + 10.0f * dt);
        shield = Math.min(maxShield, shield + 5.0f * dt);

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
        
        // Prow offset: Shift forward by half-length to clear the hull
        float prowOffset = 65.0f; // Castle-class specific or scaled
        if (definition.id.contains("stalwart")) prowOffset = 45.0f;

        minicraft.math.Vector3f barrelPos = new minicraft.math.Vector3f(
            position.x - sin * prowOffset, 
            position.y + 0.5f, 
            position.z - cos * prowOffset
        );

        minicraft.math.Vector3f dir = new minicraft.math.Vector3f(lookDir.x, lookDir.y, lookDir.z);
        float dist = world.raycast(barrelPos, dir, 800f);
        
        // Visual Beam Effect
        for (float d = 0; d < dist; d += 4.0f) {
            pm.spawnThruster(
                barrelPos.x + dir.x * d,
                barrelPos.y + dir.y * d,
                barrelPos.z + dir.z * d,
                new minicraft.math.Vector3f(0,0,0),
                3.5f,
                new minicraft.math.Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 
                0.15f
            );
        }
        
        // Impact effect
        pm.spawnExplosion(barrelPos.x + dir.x * dist, barrelPos.y + dir.y * dist, barrelPos.z + dir.z * dist, 2.5f);
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
        
        float dist = world.raycast(muzzle, dir, 120f);
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

        // Ground Hit Smoke for PDW
        if (dist < 119f) {
            pm.spawnSmoke(muzzle.x + dir.x * dist, muzzle.y + dir.y * dist, muzzle.z + dir.z * dist);
        }
    }

    private void spawnThrusterPlumes(minicraft.entity.ParticleManager pm, float dt) {
        if (currentThrust <= 0.01f) return;

        float cos = (float) Math.cos(Math.toRadians(yaw));
        float sin = (float) Math.sin(Math.toRadians(yaw));
        
        // Use actual thruster mounts from the schematic
        for (minicraft.ship.ThrusterMount mount : definition.schematic.thrusters) {
            // Only show plumes for rear engines during forward thrust, or lateral for turning
            // For simplicity, we show all major thrusters scaled by currentThrust
            
            float rx = mount.localPosition.x * cos - mount.localPosition.z * sin;
            float rz = mount.localPosition.x * sin + mount.localPosition.z * cos;
            
            float ex = position.x + rx;
            float ey = position.y + mount.localPosition.y;
            float ez = position.z + rz;
            
            // Thrust vector (opposite of thruster's push direction)
            minicraft.math.Vector3f pVel = new minicraft.math.Vector3f(
                (mount.thrustDirection.x * cos - mount.thrustDirection.z * sin) * -10f * currentThrust,
                mount.thrustDirection.y * -10f * currentThrust,
                (mount.thrustDirection.x * sin + mount.thrustDirection.z * cos) * -10f * currentThrust
            );
            
            // DYNAMIC SIZING: Scale radius based on engine force
            // Small fighter (50k N) -> ~1.5 scale
            // Stalwart (8M N) -> ~4.5 scale
            // Castle (28M N) -> ~12.0 scale
            float forceScale = 1.0f + (mount.maxForce / 2_500_000f); 
            float size = forceScale * (0.8f + currentThrust * 0.4f);
            
            // BLUE FUSION COLOR
            minicraft.math.Vector4f color = new minicraft.math.Vector4f(0.2f, 0.5f, 1.0f, 0.85f);
            
            pm.spawnThruster(ex, ey, ez, pVel, size, color, 0.4f + currentThrust * 0.4f);
        }
    }


    public void handleInput(boolean w, boolean s, boolean a, boolean d, boolean space, boolean shift, float dt) {
        float fx = (float) -Math.sin(Math.toRadians(yaw));
        float fz = (float) -Math.cos(Math.toRadians(yaw));

        if (w) {
            shipVelocity.x += fx * acceleration * dt;
            shipVelocity.z += fz * acceleration * dt;
            currentThrust = Math.min(1.0f, currentThrust + dt * 2.0f);
        } else if (s) {
            // Strong active braking / reverse
            shipVelocity.x -= fx * acceleration * 1.5f * dt;
            shipVelocity.z -= fz * acceleration * 1.5f * dt;
            currentThrust = Math.min(0.5f, currentThrust + dt);
        } else {
            // Decay thrust visual
            currentThrust = Math.max(0, currentThrust - dt * 2.0f);
        }

        // Steering (Yaw) - Exact Car-like control
        if (a) yaw += rotationSpeed * dt;
        if (d) yaw -= rotationSpeed * dt;

        // Elevation
        if (space) shipVelocity.y += acceleration * 0.5f * dt;
        if (shift) shipVelocity.y -= acceleration * 0.5f * dt;
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

    public float getShieldPct() { return shield / maxShield; }
    public float getEnergyPct() { return energy / maxEnergy; }
    public float getFuelPct() { return fuel / maxFuel; }
    public float getVelocityKms() { 
        return (float) Math.sqrt(shipVelocity.x*shipVelocity.x + shipVelocity.y*shipVelocity.y + shipVelocity.z*shipVelocity.z); 
    }
}
