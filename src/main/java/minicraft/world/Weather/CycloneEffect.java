package minicraft.world.weather;

import minicraft.world.IWeatherEntity;
import minicraft.world.IWeatherWorld;

import java.util.*;

/**
 * CycloneEffect — simulates an active cyclone or hurricane.
 *
 * ── Structure of the cyclone ────────────────────────────────────────────
 *
 *   The cyclone has a moving "eye" position (XZ only — Y is handled via
 *   surface height).  The eye drifts slowly across the world using a random
 *   walk, simulating a storm system tracking across the terrain.
 *
 *   Three concentric zones radiate from the eye:
 *
 *     EYE_RADIUS        — the calm centre.  No wind forces here; entities
 *                          inside actually receive a gentle downward press.
 *     WALL_RADIUS       — the eyewall: maximum wind and destruction.
 *                          Entities are pulled violently toward the eye while
 *                          simultaneously being spun tangentially.
 *     OUTER_RADIUS      — outer bands: weaker pull, gradually fading to zero.
 *
 * ── Entity physics ──────────────────────────────────────────────────────
 *
 *   Force on an entity = (radialPull toward eye) + (tangentialSpin ⊥ radial).
 *   The ratio of radial to tangential is controlled by TANGENTIAL_BIAS.
 *   Entities in the eyewall also receive an upward lift component, letting
 *   them be flung into the air when they pass through the strongest zone.
 *
 * ── Block destruction ───────────────────────────────────────────────────
 *
 *   On a configurable tick, a sweep scans for wind-destructible blocks
 *   within the eyewall zone and removes a random subset.  Hurricane is
 *   more destructive than cyclone — controlled by the intensity parameter
 *   passed from WeatherManager.
 *
 * ── Usage ───────────────────────────────────────────────────────────────
 *
 * <pre>
 *   CycloneEffect cyclone = new CycloneEffect(worldSeed);
 *   cyclone.init(playerX, playerZ);   // set starting eye near the player
 *
 *   // Each frame while weather == CYCLONE or HURRICANE:
 *   cyclone.update(dt, world, intensity);
 *
 *   // When storm ends:
 *   cyclone.onEnd();
 * </pre>
 */
public class CycloneEffect {

    // ── Tuning ─────────────────────────────────────────────────────────────

    /** Calm eye radius (blocks). No wind forces inside this zone. */
    private static final float EYE_RADIUS = 12f;

    /** Eyewall outer radius — maximum forces occur between EYE_RADIUS and this. */
    private static final float WALL_RADIUS = 28f;

    /** Outer band radius — forces taper off from WALL_RADIUS to this. */
    private static final float OUTER_RADIUS = 80f;

    /** Peak radial pull force (blocks/s²) applied in the eyewall. */
    private static final float PEAK_PULL_FORCE = 0.55f;

    /**
     * Ratio of tangential (spin) to radial (pull) force.
     * 0 = pure pull toward eye, 1 = pure spin, 0.6 = realistic cyclone.
     */
    private static final float TANGENTIAL_BIAS = 0.60f;

    /** Upward lift applied to entities in the eyewall. */
    private static final float LIFT_FORCE = 0.18f;

    /** How fast the eye position drifts across the world (blocks/s). */
    private static final float EYE_DRIFT_SPEED = 1.8f;

    /** Eye drift direction changes every this many seconds. */
    private static final float DRIFT_CHANGE_INTERVAL = 12f;

    /** How often the block destruction sweep runs (seconds). */
    private static final float DESTROY_TICK_INTERVAL = 1.5f;

    /**
     * At intensity 1.0, this fraction of candidate destructible blocks in
     * the eyewall are destroyed each tick.  Hurricane (1.2) exceeds 1.0.
     */
    private static final float BASE_DESTROY_FRACTION = 0.12f;

    /** Maximum blocks destroyed per tick regardless of intensity. */
    private static final int MAX_DESTROY_PER_TICK = 20;

    // ── State ──────────────────────────────────────────────────────────────

    /** Current eye position (XZ world units). */
    private float eyeX, eyeZ;

    /** Current drift direction (unit vector XZ). */
    private float driftDX = 1f, driftDZ = 0f;

    private float driftTimer    = 0f;
    private float destroyTimer  = 0f;

    private final Random rng;

    // ── Constructor ────────────────────────────────────────────────────────

    public CycloneEffect(long seed) {
        this.rng = new Random(seed ^ 0xDEADBEEFCAFEBABEL);
    }

    // ── Initialisation ─────────────────────────────────────────────────────

    /**
     * Sets the initial eye position.  Call once when the cyclone begins.
     * Typically placed at some offset from the nearest player so the storm
     * approaches rather than starting on top of them.
     *
     * @param startX  Suggested eye X (world units)
     * @param startZ  Suggested eye Z (world units)
     */
    public void init(float startX, float startZ) {
        this.eyeX = startX;
        this.eyeZ = startZ;
        randomiseDrift();
        System.out.printf("[Cyclone] Eye initialised at (%.1f, %.1f)%n", eyeX, eyeZ);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    /**
     * Main update — call every frame while weather == CYCLONE or HURRICANE.
     *
     * @param dt        Delta time (seconds)
     * @param world     World reference
     * @param intensity Weather intensity from WeatherManager [0, 1.2]
     */
    public void update(float dt, IWeatherWorld world, float intensity) {
        moveEye(dt);
        applyWindForces(dt, world, intensity);

        destroyTimer += dt;
        if (destroyTimer >= DESTROY_TICK_INTERVAL) {
            destroyTimer = 0f;
            runDestructionSweep(world, intensity);
        }
    }

    /** Call when the cyclone/hurricane ends. Resets timers. */
    public void onEnd() {
        destroyTimer = 0f;
        driftTimer   = 0f;
        System.out.println("[Cyclone] Storm dissipated.");
    }

    // ── Accessors for rendering ─────────────────────────────────────────────

    /** Current eye X (world units). */
    public float getEyeX() { return eyeX; }

    /** Current eye Z (world units). */
    public float getEyeZ() { return eyeZ; }

    /** Outer radius (for renderer debug overlays or particle systems). */
    public float getOuterRadius() { return OUTER_RADIUS; }

    /** Eyewall outer radius. */
    public float getWallRadius() { return WALL_RADIUS; }

    // ── Eye movement ───────────────────────────────────────────────────────

    private void moveEye(float dt) {
        eyeX += driftDX * EYE_DRIFT_SPEED * dt;
        eyeZ += driftDZ * EYE_DRIFT_SPEED * dt;

        driftTimer += dt;
        if (driftTimer >= DRIFT_CHANGE_INTERVAL) {
            driftTimer = 0f;
            randomiseDrift();
        }
    }

    private void randomiseDrift() {
        // Gradually nudge the drift angle so it curves rather than snapping
        float angle = (float) (Math.atan2(driftDZ, driftDX)
                             + (rng.nextFloat() - 0.5f) * Math.PI * 0.8f);
        driftDX = (float) Math.cos(angle);
        driftDZ = (float) Math.sin(angle);
    }

    // ── Wind forces ────────────────────────────────────────────────────────

    private void applyWindForces(float dt, IWeatherWorld world, float intensity) {
        // Clamp the search to the outer band + a margin
        List<IWeatherEntity> entities = world.getEntitiesInRadius(
                eyeX, world.getMaxY() * 0.5f, eyeZ, OUTER_RADIUS + 8f);

        for (IWeatherEntity e : entities) {
            float dx = eyeX - e.getX();
            float dz = eyeZ - e.getZ();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.01f) continue;

            float forceScale = windForceScale(dist) * intensity;
            if (forceScale <= 0f) continue;

            // Radial unit vector (toward eye)
            float rx = dx / dist;
            float rz = dz / dist;

            // Tangential unit vector (90° rotation for counter-clockwise spin)
            float tx = -rz;
            float tz =  rx;

            float radial     = forceScale * PEAK_PULL_FORCE * (1f - TANGENTIAL_BIAS);
            float tangential = forceScale * PEAK_PULL_FORCE * TANGENTIAL_BIAS;

            float dvx = (rx * radial + tx * tangential) * dt;
            float dvz = (rz * radial + tz * tangential) * dt;
            float dvy = 0f;

            // Lift in the eyewall
            if (dist >= EYE_RADIUS && dist <= WALL_RADIUS) {
                dvy = LIFT_FORCE * forceScale * dt;
            }

            e.addVelocity(dvx, dvy, dvz);
        }
    }

    /**
     * Returns a [0,1] force scale based on distance from the eye.
     *
     *   Inside eye:      0.0  (calm)
     *   Eye→wall:        ramps 0 → 1
     *   At wall:         1.0  (maximum)
     *   Wall→outer:      falls 1 → 0
     *   Beyond outer:    0.0
     */
    private float windForceScale(float dist) {
        if (dist < EYE_RADIUS)   return 0f;
        if (dist > OUTER_RADIUS)  return 0f;

        if (dist <= WALL_RADIUS) {
            // Ramp from eye edge to eyewall — smoothstep
            float t = (dist - EYE_RADIUS) / (WALL_RADIUS - EYE_RADIUS);
            return smoothStep(t);
        } else {
            // Fall from eyewall out to outer band
            float t = 1f - (dist - WALL_RADIUS) / (OUTER_RADIUS - WALL_RADIUS);
            return smoothStep(t) * 0.5f; // outer bands are half-strength max
        }
    }

    // ── Block destruction ──────────────────────────────────────────────────

    private void runDestructionSweep(IWeatherWorld world, float intensity) {
        int eyeSurfY = world.getSurfaceY((int) eyeX, (int) eyeZ);
        int r        = (int) WALL_RADIUS;
        float r2wall = WALL_RADIUS * WALL_RADIUS;

        List<int[]> candidates = new ArrayList<>();

        // Scan a 2D horizontal band at and slightly above surface level
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                float distSq = dx * dx + dz * dz;
                if (distSq > r2wall) continue;
                float dist = (float) Math.sqrt(distSq);
                if (dist < EYE_RADIUS) continue; // eye is calm

                int bx = (int) eyeX + dx;
                int bz = (int) eyeZ + dz;
                int by = world.getSurfaceY(bx, bz);

                // Check the block at surface and a few above
                for (int dy = 0; dy <= 4; dy++) {
                    if (world.isWindDestructible(bx, by + dy, bz)) {
                        candidates.add(new int[]{bx, by + dy, bz});
                    }
                }
            }
        }

        if (candidates.isEmpty()) return;

        // Destroy a fraction proportional to intensity
        Collections.shuffle(candidates, rng);
        int destroyCount = Math.min(MAX_DESTROY_PER_TICK,
                (int) (candidates.size() * BASE_DESTROY_FRACTION * intensity));

        for (int i = 0; i < destroyCount; i++) {
            int[] b = candidates.get(i);
            world.destroyBlock(b[0], b[1], b[2], true);
        }

        if (destroyCount > 0) {
            System.out.printf("[Cyclone] Destroyed %d blocks near eye (%.1f, %.1f)%n",
                    destroyCount, eyeX, eyeZ);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static float smoothStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}