package minicraft.world;

/**
 * IWeatherEntity — the slice of your entity API that weather effects need.
 *
 * Implement this on your base Entity / LivingEntity class (or wrap it).
 * Weather effects only need position, velocity, and the ability to deal
 * damage — keeping the coupling surface very small.
 */
public interface IWeatherEntity {

    // ── Position ───────────────────────────────────────────────────────────

    float getX();

    float getY();

    float getZ();

    // ── Velocity ───────────────────────────────────────────────────────────

    float getVX();

    float getVY();

    float getVZ();

    /**
     * Adds an impulse to the entity's current velocity.
     * The physics engine is responsible for clamping terminal velocity.
     *
     * @param dvx Delta velocity in world X
     * @param dvy Delta velocity in world Y (positive = up)
     * @param dvz Delta velocity in world Z
     */
    void addVelocity(float dvx, float dvy, float dvz);

    /**
     * Multiplies the entity's current velocity by the given factor (per axis).
     * Used by blizzard slow: call with (0.55f, 1f, 0.55f) to halve XZ speed.
     */
    void scaleVelocity(float sx, float sy, float sz);

    // ── Damage ─────────────────────────────────────────────────────────────

    /**
     * Applies damage to the entity from a weather source.
     *
     * @param amount Raw damage points (before armour / resistance)
     * @param source Human-readable cause shown in death/log messages
     */
    void applyWeatherDamage(float amount, String source);

    // ── Status effects ─────────────────────────────────────────────────────

    /**
     * Applies a slow status to this entity for the given duration (seconds).
     * If the entity already has a slow effect, extend or replace as appropriate.
     */
    void applySlow(float durationSeconds, float speedMultiplier);

    /** Returns true if this entity is a player character. */
    boolean isPlayer();
}