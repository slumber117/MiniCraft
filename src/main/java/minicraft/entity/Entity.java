package minicraft.entity;

import minicraft.math.Vector3f;

/**
 * Base class for every living thing in MiniCraft.
 * Handles position, velocity, health and a tick-based update loop.
 */
public abstract class Entity implements minicraft.world.IWeatherEntity {

    // ── Identity ──────────────────────────────────────────────────────────
    public final String id;
    public final EntityType type;

    // ── Spatial ───────────────────────────────────────────────────────────
    public final Vector3f position = new Vector3f();
    public final Vector3f velocity = new Vector3f();
    /** Yaw in degrees (0 = +Z direction) */
    public float yaw;

    // ── Physical bounds (AABB half-extents) ───────────────────────────────
    public float width  = 0.6f;
    public float height = 1.8f;

    // ── Health ────────────────────────────────────────────────────────────
    protected float maxHealth;
    protected float health;
    protected boolean dead = false;
    public float damageFlashTimer = 0f;

    // ── State ─────────────────────────────────────────────────────────────
    public EntityState state = EntityState.IDLE;
    public float radiationTimer = 0f;
    
    public void applyRadiation(float duration) {
        this.radiationTimer = Math.max(this.radiationTimer, duration);
    }

    // ── Counter ───────────────────────────────────────────────────────────
    private static int idCounter = 0;
    private final int uid = idCounter++;

    // ─────────────────────────────────────────────────────────────────────

    public Entity(EntityType type, float maxHealth) {
        this.type      = type;
        this.id        = type.name().toLowerCase() + "_" + uid;
        this.maxHealth = maxHealth;
        this.health    = maxHealth;
    }


    // ── Abstract API ──────────────────────────────────────────────────────

    /** Called once per game tick (~20 times/sec). Override to implement AI. */
    public void tick(EntityManager manager, minicraft.world.World world, ParticleManager particleManager, float dt) {
        if (damageFlashTimer > 0) damageFlashTimer -= dt;
        if (radiationTimer > 0) {
            radiationTimer -= dt;
            health -= 15.0f * dt; // 15 Damage per second DoT
            if (health <= 0 && !dead) {
                health = 0;
                dead = true;
            }
        }
    }

    // ── Update helpers ────────────────────────────────────────────────────

    public void onDeath(EntityManager manager, minicraft.world.World world) {
        // Override to implement drops, etc.
    }

    /** Apply velocity to position with basic terrain collision. */
    protected void applyVelocity(minicraft.world.World world, float dt) {
        // Apply gravity if not flying/swimming
        if (state != EntityState.FLYING && state != EntityState.SWIMMING) {
            velocity.y -= 15.0f * dt; // Gravity
        }

        // Test X
        float nextX = position.x + velocity.x * dt;
        if (!isSolid(world, (int)Math.floor(nextX), (int)Math.floor(position.y), (int)Math.floor(position.z)) &&
            !isSolid(world, (int)Math.floor(nextX), (int)Math.floor(position.y + 1f), (int)Math.floor(position.z))) {
            position.x = nextX;
        } else {
            velocity.x = 0;
        }

        // Test Z
        float nextZ = position.z + velocity.z * dt;
        if (!isSolid(world, (int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(nextZ)) &&
            !isSolid(world, (int)Math.floor(position.x), (int)Math.floor(position.y + 1f), (int)Math.floor(nextZ))) {
            position.z = nextZ;
        } else {
            velocity.z = 0;
        }

        // Test Y (Floor and Ceiling)
        float nextY = position.y + velocity.y * dt;
        if (velocity.y <= 0) {
            // Check floor
            if (!isSolid(world, (int)Math.floor(position.x), (int)Math.floor(nextY), (int)Math.floor(position.z))) {
                position.y = nextY;
            } else {
                position.y = (float) Math.floor(position.y); // Snap to ground
                velocity.y = 0;
            }
        } else {
            // Check ceiling
            if (!isSolid(world, (int)Math.floor(position.x), (int)Math.floor(nextY + height), (int)Math.floor(position.z))) {
                position.y = nextY;
            } else {
                velocity.y = 0;
            }
        }

        // Friction
        velocity.x *= 0.85f;
        velocity.z *= 0.85f;
    }

    protected boolean isSolid(minicraft.world.World world, int x, int y, int z) {
        minicraft.world.Block b = world.getBlock(x, y, z);
        if (b == minicraft.world.Block.BOSS_GATE) {
            if (this instanceof Player) {
                return ((Player)this).level < 25;
            }
            return true; // Mobs can't pass
        }
        return b.solid;
    }

    // ── Health ────────────────────────────────────────────────────────────

    public void damage(float amount, Entity attacker) {
        if (dead) return;
        health -= amount;
        damageFlashTimer = 0.35f;
        if (health <= 0) {
            health = 0;
            dead = true;
            // No direct manager access here, will handle via EntityManager if needed
            // or just rely on subclasses having state.
        }
    }

    public void applyKnockback(float dx, float dy, float dz) {
        this.velocity.x += dx;
        this.velocity.y += dy;
        this.velocity.z += dz;
    }

    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }

    // ── Spatial ───────────────────────────────────────────────────────────

    public Vector3f getPosition() { return position; }
    public void setPosition(float x, float y, float z) { position.set(x, y, z); }
    public float getYaw() { return yaw; }

    /** Squared distance to another entity. */
    public float distanceSq(Entity other) {
        return position.distanceSquared(other.position);
    }

    /** Squared distance to a world position. */
    public float distanceSq(float x, float y, float z) {
        float dx = position.x - x, dy = position.y - y, dz = position.z - z;
        return dx*dx + dy*dy + dz*dz;
    }

    // ── IWeatherEntity ───────────────────────────────────────────────────
    @Override public float getX() { return position.x; }
    @Override public float getY() { return position.y; }
    @Override public float getZ() { return position.z; }
    @Override public float getVX() { return velocity.x; }
    @Override public float getVY() { return velocity.y; }
    @Override public float getVZ() { return velocity.z; }
    @Override public void applyWeatherDamage(float amount, String type) { damage(amount, (Entity)null); }
    @Override public void addVelocity(float dx, float dy, float dz) { applyKnockback(dx, dy, dz); }
    @Override public void scaleVelocity(float sx, float sy, float sz) { 
        velocity.x *= sx; velocity.y *= sy; velocity.z *= sz; 
    }
    @Override public void applySlow(float duration, float multiplier) { /* TODO: Slow effect logic */ }
    @Override public boolean isPlayer() { return false; }

    // ── Getters ───────────────────────────────────────────────────────────

    public float getHealth()    { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead()     { return dead; }
    public EntityState getState() { return state; }
    public EntityType getType()   { return type; }

    @Override
    public String toString() {
        return type.displayName + " [" + id + "] @ ("
            + String.format("%.1f,%.1f,%.1f", position.x, position.y, position.z) + ")";
    }
}
