package minicraft.world.weather;

import minicraft.world.IWeatherEntity;
import minicraft.world.IWeatherWorld;

import java.util.*;

/**
 * BlizzardEffect — implements the active gameplay effects of a blizzard.
 *
 * ── What it does ────────────────────────────────────────────────────────
 *
 *   FREEZING: On a configurable tick interval, the system scans water blocks
 *   near each player and converts them to ice.  Frozen blocks are tracked
 *   so they can be melted back to water when the blizzard ends.
 *
 *   SLOWING: Each update, all entities in the world receive a persistent
 *   slow status effect.  The slow decays naturally if the entity leaves the
 *   blizzard area, but is continuously refreshed while the storm persists.
 *
 * ── Lifecycle ───────────────────────────────────────────────────────────
 *
 *   Call {@link #update} every frame while weather == BLIZZARD.
 *   Call {@link #onEnd} exactly once when the blizzard finishes — this
 *   melts all tracked ice blocks back to water.
 *
 * ── Tuning ──────────────────────────────────────────────────────────────
 *
 *   FREEZE_RADIUS:        How far from each player water can freeze.
 *   FREEZE_TICK_INTERVAL: Seconds between each freeze scan pass.
 *   SLOW_MULTIPLIER:      Fraction of normal XZ speed during blizzard.
 *   SLOW_REFRESH_PERIOD:  How often the slow effect is reapplied (seconds).
 *   MAX_FROZEN_BLOCKS:    Cap to prevent unlimited world mutation.
 */
public class BlizzardEffect {

    // ── Tuning ─────────────────────────────────────────────────────────────

    /** Radius around each player in which water blocks can be frozen. */
    private static final float FREEZE_RADIUS = 18f;

    /** How often (seconds) the freeze scan runs. */
    private static final float FREEZE_TICK_INTERVAL = 2.5f;

    /** XZ speed multiplier applied to all entities during the blizzard. */
    private static final float SLOW_MULTIPLIER = 0.45f;

    /** Duration (seconds) of each slow application — reapplied regularly. */
    private static final float SLOW_DURATION = 4f;

    /** How often (seconds) the slow effect is refreshed on entities. */
    private static final float SLOW_REFRESH_PERIOD = 2f;

    /**
     * Hard cap on the number of blocks this blizzard can freeze.
     * Prevents runaway mutation on large flat worlds.
     */
    private static final int MAX_FROZEN_BLOCKS = 512;

    // ── State ──────────────────────────────────────────────────────────────

    /** Set of block positions frozen by this blizzard, stored as packed longs. */
    private final Set<Long> frozenBlocks = new LinkedHashSet<>();

    private float freezeTimer = 0f;
    private float slowTimer   = 0f;
    private final Random rng  = new Random();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Main update — call every frame while weather == BLIZZARD.
     *
     * @param dt        Delta time (seconds)
     * @param world     World reference
     * @param intensity Weather intensity from WeatherManager [0,1+]
     */
    public void update(float dt, IWeatherWorld world, float intensity) {
        freezeTimer += dt;
        slowTimer   += dt;

        // Freeze pass — runs every FREEZE_TICK_INTERVAL seconds
        if (freezeTimer >= FREEZE_TICK_INTERVAL) {
            freezeTimer = 0f;
            if (frozenBlocks.size() < MAX_FROZEN_BLOCKS) {
                runFreezePass(world, intensity);
            }
        }

        // Slow refresh — continuously reapply slow to all entities
        if (slowTimer >= SLOW_REFRESH_PERIOD) {
            slowTimer = 0f;
            applySlowToAll(world);
        }
    }

    /**
     * Call exactly once when the blizzard ends.
     * Melts all frozen blocks back to water and resets internal state.
     *
     * @param world World reference (same one passed to {@link #update})
     */
    public void onEnd(IWeatherWorld world) {
        for (long packed : frozenBlocks) {
            int bx = unpackX(packed);
            int by = unpackY(packed);
            int bz = unpackZ(packed);
            world.meltIce(bx, by, bz);
        }
        frozenBlocks.clear();
        freezeTimer = 0f;
        slowTimer   = 0f;
        System.out.println("[Blizzard] Thaw complete — all ice melted.");
    }

    /** Number of blocks currently frozen by this blizzard instance. */
    public int getFrozenBlockCount() {
        return frozenBlocks.size();
    }

    // ── Freeze logic ───────────────────────────────────────────────────────

    private void runFreezePass(IWeatherWorld world, float intensity) {
        // Find all players to use as freeze centres
        List<IWeatherEntity> players = new ArrayList<>();
        for (IWeatherEntity e : world.getAllEntities()) {
            if (e.isPlayer()) players.add(e);
        }
        if (players.isEmpty()) return;

        // How many blocks to attempt to freeze this tick (scales with intensity)
        int attempts = Math.min(12, (int) (6 * intensity) + 3);

        for (int i = 0; i < attempts; i++) {
            if (frozenBlocks.size() >= MAX_FROZEN_BLOCKS) break;

            // Pick a random player as the freeze centre
            IWeatherEntity player = players.get(rng.nextInt(players.size()));

            // Pick a random offset within FREEZE_RADIUS on XZ (flat disk)
            float angle = rng.nextFloat() * (float) (Math.PI * 2.0);
            float r     = (float) Math.sqrt(rng.nextFloat()) * FREEZE_RADIUS;
            int cx = (int) (player.getX() + Math.cos(angle) * r);
            int cz = (int) (player.getZ() + Math.sin(angle) * r);

            // Walk downward from surface to find a water block
            int surface = world.getSurfaceY(cx, cz);
            for (int by = surface; by >= surface - 3; by--) {
                if (world.isWater(cx, by, cz)) {
                    long packed = packCoord(cx, by, cz);
                    if (!frozenBlocks.contains(packed)) {
                        if (world.setIce(cx, by, cz)) {
                            frozenBlocks.add(packed);
                        }
                    }
                    break;
                }
            }
        }
    }

    // ── Slow logic ─────────────────────────────────────────────────────────

    private void applySlowToAll(IWeatherWorld world) {
        for (IWeatherEntity e : world.getAllEntities()) {
            e.applySlow(SLOW_DURATION, SLOW_MULTIPLIER);
        }
    }

    // ── Coordinate packing (fits Y in 12 bits, X/Z in 26 bits each) ───────

    private static long packCoord(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
             | ((long) (y & 0xFFF)     << 26)
             |  (long) (z & 0x3FFFFFF);
    }

    private static int unpackX(long p) {
        int v = (int) (p >> 38) & 0x3FFFFFF;
        return (v << 6) >> 6; // sign extend
    }

    private static int unpackY(long p) {
        return (int) (p >> 26) & 0xFFF;
    }

    private static int unpackZ(long p) {
        int v = (int) p & 0x3FFFFFF;
        return (v << 6) >> 6; // sign extend
    }
}