package minicraft.world.weather;

import minicraft.world.IWeatherEntity;
import minicraft.world.IWeatherWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * LightningSystem — manages all active lightning bolts for the current weather.
 *
 * ── Responsibilities ────────────────────────────────────────────────────
 *
 *   • Accumulates time and fires new bolts at random intervals determined
 *     by the current weather intensity (THUNDERSTORM vs HURRICANE).
 *   • Maintains the active bolt list that the renderer reads each frame.
 *   • On the frame a bolt transitions to STRUCK, fires all gameplay effects:
 *       - Entity damage + shockwave impulse (radius-based)
 *       - Block ignition for flammable blocks near the strike point
 *
 * ── Integration ─────────────────────────────────────────────────────────
 *
 * <pre>
 *   // In your WeatherManager.update():
 *   if (currentType == THUNDERSTORM || currentType == HURRICANE) {
 *       lightningSystem.update(dt, world, strikeAreaCentreX, strikeAreaCentreZ,
 *                              strikeAreaRadius, intensity);
 *   } else {
 *       lightningSystem.clear();
 *   }
 *
 *   // In your renderer:
 *   for (LightningBolt bolt : lightningSystem.getActiveBolts()) {
 *       renderBolt(bolt);
 *   }
 * </pre>
 *
 * Thread safety: not thread-safe — call from your main game loop thread.
 */
public class LightningSystem {

    // ── Tuning ─────────────────────────────────────────────────────────────

    /**
     * At intensity 0.6 (THUNDERSTORM) a bolt fires every 8–18 seconds.
     * At intensity 1.2 (HURRICANE) the interval shrinks proportionally.
     * minInterval = BASE_MIN_INTERVAL / intensity
     */
    private static final float BASE_MIN_INTERVAL = 5f;   // seconds at intensity 1.0
    private static final float BASE_MAX_INTERVAL = 18f;  // seconds at intensity 1.0

    /** Maximum bolts alive at any one time (prevents visual overload). */
    private static final int MAX_ACTIVE_BOLTS = 6;

    // ── State ──────────────────────────────────────────────────────────────

    private final List<LightningBolt> activeBolts = new ArrayList<>();
    private float timeUntilNextBolt = 3f; // first strike 3s after storm begins
    private long boltCounter = 0;
    private final Random rng = new Random();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Main update — call once per frame while a storm is active.
     *
     * @param dt            Delta time (seconds)
     * @param world         World reference for block and entity queries
     * @param areaCentreX   Centre of the area where bolts can strike
     * @param areaCentreZ   Centre of the area where bolts can strike
     * @param strikeRadius  Radius (blocks) around centre where bolts land
     * @param intensity     Weather intensity [0,1+] from WeatherManager
     */
    public void update(float dt, IWeatherWorld world,
                       float areaCentreX, float areaCentreZ,
                       float strikeRadius, float intensity) {

        // ── Advance existing bolts ─────────────────────────────────────────
        Iterator<LightningBolt> it = activeBolts.iterator();
        while (it.hasNext()) {
            LightningBolt bolt = it.next();
            boolean justStruck = bolt.update(dt);
            if (justStruck) {
                applyStrikeEffects(bolt, world);
            }
            if (bolt.isDead()) it.remove();
        }

        // ── Spawn new bolt? ────────────────────────────────────────────────
        timeUntilNextBolt -= dt;
        if (timeUntilNextBolt <= 0f && activeBolts.size() < MAX_ACTIVE_BOLTS) {
            spawnBolt(world, areaCentreX, areaCentreZ, strikeRadius);
            scheduleNextBolt(intensity);
        }
    }

    /** Returns the live bolt list for the renderer. Do not modify. */
    public List<LightningBolt> getActiveBolts() {
        return activeBolts;
    }

    /** Clears all active bolts — call when the storm ends. */
    public void clear() {
        activeBolts.clear();
        timeUntilNextBolt = 3f;
    }

    // ── Bolt spawning ──────────────────────────────────────────────────────

    private void spawnBolt(IWeatherWorld world,
                           float cx, float cz, float radius) {
        // Pick a random XZ inside the strike area
        float angle  = rng.nextFloat() * (float) (Math.PI * 2.0);
        float dist   = (float) Math.sqrt(rng.nextFloat()) * radius; // uniform disk
        float sx     = cx + (float) Math.cos(angle) * dist;
        float sz     = cz + (float) Math.sin(angle) * dist;
        int   sy     = world.getSurfaceY((int) sx, (int) sz);
        long  seed   = (boltCounter++ * 0x9E3779B97F4A7C15L) ^ rng.nextLong();

        activeBolts.add(new LightningBolt(sx, sy + 1f, sz, seed));
    }

    private void scheduleNextBolt(float intensity) {
        float clampedI = Math.max(0.1f, intensity);
        float min = BASE_MIN_INTERVAL / clampedI;
        float max = BASE_MAX_INTERVAL / clampedI;
        timeUntilNextBolt = min + rng.nextFloat() * (max - min);
    }

    // ── Gameplay effects on strike ─────────────────────────────────────────

    /**
     * Fires all gameplay effects for a bolt that just reached the ground.
     * Called exactly once per bolt, on the frame it transitions to STRUCK.
     */
    private void applyStrikeEffects(LightningBolt bolt, IWeatherWorld world) {
        float sx = bolt.strikeX;
        float sy = bolt.strikeY;
        float sz = bolt.strikeZ;

        applyEntityEffects(world, sx, sy, sz);
        applyFireEffects(world, (int) sx, (int) sy, (int) sz);

        System.out.printf("[Lightning] Strike at (%.1f, %.1f, %.1f)%n", sx, sy, sz);
    }

    /** Damage + shockwave to nearby entities. */
    private void applyEntityEffects(IWeatherWorld world,
                                    float sx, float sy, float sz) {
        List<IWeatherEntity> nearby = world.getEntitiesInRadius(
                sx, sy, sz, LightningBolt.SHOCKWAVE_RADIUS);

        for (IWeatherEntity e : nearby) {
            float dx = e.getX() - sx;
            float dy = e.getY() - sy;
            float dz = e.getZ() - sz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.01f) dist = 0.01f;

            // Direct damage (falls off with distance, zero beyond DAMAGE_RADIUS)
            if (dist <= LightningBolt.DAMAGE_RADIUS) {
                float falloff = 1f - (dist / LightningBolt.DAMAGE_RADIUS);
                float dmg = LightningBolt.LIGHTNING_DAMAGE * falloff * falloff;
                e.applyWeatherDamage(dmg, "lightning");
            }

            // Kinetic shockwave — outward impulse, inverse-square falloff
            float strength = LightningBolt.SHOCKWAVE_STRENGTH
                           * (1f - dist / LightningBolt.SHOCKWAVE_RADIUS);
            strength = Math.max(0f, strength);
            float nx = dx / dist, ny = dy / dist, nz = dz / dist;
            // Bias upward so entities are knocked into the air, not the ground
            ny = Math.max(ny + 0.35f, 0.2f);
            e.addVelocity(nx * strength, ny * strength, nz * strength);
        }
    }

    /** Set fire to flammable blocks around the strike point. */
    private void applyFireEffects(IWeatherWorld world, int sx, int sy, int sz) {
        int r = (int) Math.ceil(LightningBolt.FIRE_RADIUS);
        float r2 = LightningBolt.FIRE_RADIUS * LightningBolt.FIRE_RADIUS;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= r; dy++) {  // check slightly below strike too
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    int bx = sx + dx, by = sy + dy, bz = sz + dz;
                    // Only ignite if there's air above (so fire has room)
                    if (world.isFlammable(bx, by, bz) && world.isAir(bx, by + 1, bz)) {
                        world.setFire(bx, by + 1, bz);
                    }
                }
            }
        }
    }
}