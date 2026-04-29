package minicraft.world;

import minicraft.world.weather.BlizzardEffect;
import minicraft.world.weather.CycloneEffect;
import minicraft.world.weather.LightningBolt;
import minicraft.world.weather.LightningSystem;

import java.util.List;

/**
 * WeatherManager — global weather coordinator.
 *
 * Owns one instance of each weather-effect subsystem and routes per-frame
 * updates to the right system(s) based on the current weather type.
 *
 * ── Effect subsystems ───────────────────────────────────────────────────
 *
 * {@link LightningSystem} — bolt spawning, damage, fire.
 * Active during: THUNDERSTORM, HURRICANE.
 *
 * {@link BlizzardEffect} — water freezing, entity slow.
 * Active during: BLIZZARD.
 * Calls onEnd() on weather transition.
 *
 * {@link CycloneEffect} — wandering eye, entity pull, block destruction.
 * Active during: CYCLONE, HURRICANE.
 * Eye is initialised near the world centre when
 * the storm begins.
 *
 * ── Integration ─────────────────────────────────────────────────────────
 *
 * <pre>
 * WeatherManager weather = new WeatherManager(worldSeed);
 *
 * // Each game frame — pass world reference and player area for strike
 * // targeting:
 * weather.update(dt, world, playerX, playerZ, strikeRadius);
 *
 * // Renderer reads bolt list:
 * for (LightningBolt bolt : weather.getActiveBolts()) {
 *     renderer.drawLightningBolt(bolt);
 * }
 * </pre>
 *
 * Thread safety: not thread-safe — call from the main game loop thread.
 */
public class WeatherManager {

    // ── Weather types ──────────────────────────────────────────────────────

    public enum WeatherType {
        CLEAR,
        RAIN,
        SNOW,
        THUNDERSTORM,
        BLIZZARD,
        HURRICANE,
        CYCLONE,
        TORRENTIAL_RAIN
    }

    // ── Transition table ───────────────────────────────────────────────────

    private WeatherType currentType = WeatherType.CLEAR;
    private WeatherType previousType = WeatherType.CLEAR;
    private float weatherTime = 0f;
    private float nextChange = 12000f; // ticks

    // ── Subsystems ─────────────────────────────────────────────────────────

    private final LightningSystem lightningSystem;
    private final BlizzardEffect blizzardEffect;
    private final CycloneEffect cycloneEffect;

    private final long worldSeed;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * @param worldSeed Used to seed the cyclone random walk deterministically.
     */
    public WeatherManager(long worldSeed) {
        this.worldSeed = worldSeed;
        this.lightningSystem = new LightningSystem();
        this.blizzardEffect = new BlizzardEffect();
        this.cycloneEffect = new CycloneEffect(worldSeed);
    }

    /**
     * Convenience constructor for worlds that don't use a fixed seed.
     */
    public WeatherManager() {
        this(System.currentTimeMillis());
    }

    // ── Main update ────────────────────────────────────────────────────────

    /**
     * Call once per frame.
     *
     * @param dt           Delta time in seconds
     * @param world        World reference — may be null if no effects needed
     *                     (e.g. during loading). Effect calls are skipped when
     *                     null.
     * @param playerX      World X centre for strike/cyclone area targeting
     * @param playerZ      World Z centre for strike/cyclone area targeting
     * @param strikeRadius Radius (blocks) around player where lightning can land
     */
    public void update(float dt, IWeatherWorld world,
            float playerX, float playerZ, float strikeRadius) {

        // ── Weather state machine ──────────────────────────────────────────
        weatherTime += dt * 60f;
        if (weatherTime >= nextChange) {
            weatherTime = 0f;
            transitionWeather(world, playerX, playerZ);
        }

        if (world == null)
            return;

        // ── Dispatch to subsystems ─────────────────────────────────────────
        switch (currentType) {

            case THUNDERSTORM:
                lightningSystem.update(dt, world,
                        playerX, playerZ, strikeRadius, getIntensity());
                break;

            case HURRICANE:
                // Hurricane: lightning + cyclone at the same time
                lightningSystem.update(dt, world,
                        playerX, playerZ, strikeRadius * 1.5f, getIntensity());
                cycloneEffect.update(dt, world, getIntensity());
                break;

            case CYCLONE:
                cycloneEffect.update(dt, world, getIntensity());
                break;

            case BLIZZARD:
                blizzardEffect.update(dt, world, getIntensity());
                break;

            default:
                // CLEAR, RAIN, SNOW, TORRENTIAL_RAIN — particle/visual only.
                break;
        }
    }

    // ── Weather transition ─────────────────────────────────────────────────

    private void transitionWeather(IWeatherWorld world,
            float playerX, float playerZ) {
        previousType = currentType;
        double r = Math.random();

        if (r < 0.50)
            currentType = WeatherType.CLEAR;
        else if (r < 0.65)
            currentType = WeatherType.RAIN;
        else if (r < 0.75)
            currentType = WeatherType.SNOW;
        else if (r < 0.82)
            currentType = WeatherType.THUNDERSTORM;
        else if (r < 0.88)
            currentType = WeatherType.TORRENTIAL_RAIN;
        else if (r < 0.93)
            currentType = WeatherType.BLIZZARD;
        else if (r < 0.97)
            currentType = WeatherType.CYCLONE;
        else
            currentType = WeatherType.HURRICANE;

        nextChange = 6000f + (float) Math.random() * 15000f;

        System.out.printf("Weather: %s → %s%n", previousType, currentType);

        // ── Tear-down old effects ──────────────────────────────────────────
        boolean hadLightning = previousType == WeatherType.THUNDERSTORM
                || previousType == WeatherType.HURRICANE;
        boolean hasLightning = currentType == WeatherType.THUNDERSTORM
                || currentType == WeatherType.HURRICANE;
        if (hadLightning && !hasLightning) {
            lightningSystem.clear();
        }

        boolean hadBlizzard = previousType == WeatherType.BLIZZARD;
        boolean hasBlizzard = currentType == WeatherType.BLIZZARD;
        if (hadBlizzard && !hasBlizzard && world != null) {
            blizzardEffect.onEnd(world);
        }

        boolean hadCyclone = previousType == WeatherType.CYCLONE
                || previousType == WeatherType.HURRICANE;
        boolean hasCyclone = currentType == WeatherType.CYCLONE
                || currentType == WeatherType.HURRICANE;
        if (hadCyclone && !hasCyclone) {
            cycloneEffect.onEnd();
        }

        // ── Initialise new effects ─────────────────────────────────────────
        if (!hadCyclone && hasCyclone) {
            // Spawn eye 40–80 blocks away from the player in a random direction
            double angle = Math.random() * Math.PI * 2.0;
            float offset = 40f + (float) Math.random() * 40f;
            cycloneEffect.init(
                    playerX + (float) Math.cos(angle) * offset,
                    playerZ + (float) Math.sin(angle) * offset);
        }
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** Current weather type. */
    public WeatherType getCurrentType() {
        return currentType;
    }

    /**
     * All active lightning bolts — pass to your renderer each frame.
     * The list is owned by {@link LightningSystem}; do not modify it.
     */
    public List<LightningBolt> getActiveBolts() {
        return lightningSystem.getActiveBolts();
    }

    /**
     * Eye position X for cyclone rendering (particle vortex, UI overlay, etc.).
     * Only meaningful during CYCLONE / HURRICANE.
     */
    public float getCycloneEyeX() {
        return cycloneEffect.getEyeX();
    }

    /** Eye position Z. Only meaningful during CYCLONE / HURRICANE. */
    public float getCycloneEyeZ() {
        return cycloneEffect.getEyeZ();
    }

    // ── Intensity / rendering hints ────────────────────────────────────────

    public float getIntensity() {
        switch (currentType) {
            case CLEAR:
                return 0f;
            case RAIN:
                return 0.3f;
            case SNOW:
                return 0.3f;
            case THUNDERSTORM:
                return 0.6f;
            case TORRENTIAL_RAIN:
                return 0.8f;
            case BLIZZARD:
                return 0.8f;
            case CYCLONE:
                return 1.0f;
            case HURRICANE:
                return 1.2f;
            default:
                return 0f;
        }
    }

    public float getRainIntensity() {
        if (currentType == WeatherType.SNOW
                || currentType == WeatherType.BLIZZARD
                || currentType == WeatherType.CLEAR)
            return 0f;
        return getIntensity();
    }

    public float getSunBrightness() {
        return Math.max(0.2f, 1.0f - getIntensity() * 0.5f);
    }
}