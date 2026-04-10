package minicraft.world;

/**
 * Generates the three foundational climate maps that drive biome placement:
 *
 *   1. Temperature  — hot/cold axis, large continental blobs
 *   2. Humidity     — wet/dry axis, medium-frequency blobs
 *   3. Continentalness — distance-from-ocean shaping, very low frequency
 *
 * Each map is a separate PerlinNoise field seeded differently so they vary
 * independently. All values are normalised to [0, 1].
 */
public class ClimateMap {

    // ── Noise fields ──────────────────────────────────────────────────────

    /** Controls temperature. Very low frequency → continent-scale blobs. */
    private final PerlinNoise temperatureNoise;

    /** Controls humidity / moisture. Medium frequency. */
    private final PerlinNoise humidityNoise;

    /**
     * Continentalness: lifts land far from the coast, depresses coastal
     * and offshore regions. Very low frequency, 2 octaves.
     * Output shapes the "base" elevation before any mountain noise is added.
     */
    private final PerlinNoise continentalnessNoise;

    /**
     * Domain-warp noise for temperature.
     * Offsets sampling coordinates so climate boundaries look organic rather
     * than perfectly circular.
     */
    private final PerlinNoise tempWarpNoise;

    /**
     * Domain-warp noise for humidity.
     */
    private final PerlinNoise humidWarpNoise;

    // ── Tuning constants ──────────────────────────────────────────────────

    /** How zoomed-out the temperature map is. Larger = bigger climate zones. */
    private static final double TEMP_SCALE        = 0.0008;
    private static final double TEMP_WARP_SCALE   = 0.003;
    private static final double TEMP_WARP_STRENGTH = 80.0;

    private static final double HUMID_SCALE       = 0.0015;
    private static final double HUMID_WARP_SCALE  = 0.004;
    private static final double HUMID_WARP_STRENGTH = 60.0;

    /** Very zoomed-out — one smooth continent per ~3000 blocks. */
    private static final double CONTINENT_SCALE   = 0.0005;

    // ── Constructor ───────────────────────────────────────────────────────

    public ClimateMap(long seed) {
        temperatureNoise    = new PerlinNoise(seed + 100L);
        humidityNoise       = new PerlinNoise(seed + 200L);
        continentalnessNoise = new PerlinNoise(seed + 300L);
        tempWarpNoise       = new PerlinNoise(seed + 401L);
        humidWarpNoise      = new PerlinNoise(seed + 502L);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the temperature at (x, z) in [0, 1].
     * Domain-warped for organic boundaries.
     */
    public float getTemperature(double x, double z) {
        double warpX = tempWarpNoise.noise(x * TEMP_WARP_SCALE, 0, z * TEMP_WARP_SCALE) * TEMP_WARP_STRENGTH;
        double warpZ = tempWarpNoise.noise(x * TEMP_WARP_SCALE + 5.2, 0, z * TEMP_WARP_SCALE + 1.3) * TEMP_WARP_STRENGTH;

        double raw = temperatureNoise.fractalNoise(
            (x + warpX) * TEMP_SCALE, 0,
            (z + warpZ) * TEMP_SCALE,
            3, 0.5
        );
        return normalise(raw);
    }

    /**
     * Returns the humidity at (x, z) in [0, 1].
     * Domain-warped for organic boundaries.
     */
    public float getHumidity(double x, double z) {
        double warpX = humidWarpNoise.noise(x * HUMID_WARP_SCALE + 8.3, 0, z * HUMID_WARP_SCALE + 2.8) * HUMID_WARP_STRENGTH;
        double warpZ = humidWarpNoise.noise(x * HUMID_WARP_SCALE + 3.1, 0, z * HUMID_WARP_SCALE + 7.4) * HUMID_WARP_STRENGTH;

        double raw = humidityNoise.fractalNoise(
            (x + warpX) * HUMID_SCALE, 0,
            (z + warpZ) * HUMID_SCALE,
            4, 0.55
        );
        return normalise(raw);
    }

    /**
     * Returns a continentalness value in [0, 1].
     *
     *   ≈ 0.0 → deep ocean
     *   ≈ 0.3 → coastal / shallow
     *   ≈ 0.6 → inland plains
     *   ≈ 1.0 → deep continental interior
     *
     * This is applied as a base additive to the elevation map so that
     * mountains only appear far inland and oceans naturally ring coastlines.
     */
    public float getContinentalness(double x, double z) {
        double raw = continentalnessNoise.fractalNoise(
            x * CONTINENT_SCALE, 0,
            z * CONTINENT_SCALE,
            2, 0.5
        );
        // Squash so the transition from ocean to land is steeper than land-to-highland
        float n = normalise(raw);
        return (float) applyOceanCurve(n);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Maps noise output [-1, 1] → [0, 1]. */
    private static float normalise(double noiseValue) {
        return (float) Math.max(0.0, Math.min(1.0, (noiseValue + 1.0) * 0.5));
    }

    /**
     * Applies a curve to the continentalness value that:
     *  - Sharpens the ocean/coast boundary (values near 0 stay near 0)
     *  - Gradually lifts the inland plateau
     *  - Keeps the deep interior high and flat before mountain noise is added
     */
    private static double applyOceanCurve(float n) {
        // Smooth step below sea-level threshold to keep oceans wide
        if (n < 0.40f) {
            // Compress the ocean band into [0, 0.18]
            return (n / 0.40) * 0.18;
        } else {
            // Remap [0.40, 1.0] → [0.18, 1.0]
            double t = (n - 0.40) / 0.60;
            // Ease-in so inland regions rise smoothly
            t = t * t * (3.0 - 2.0 * t); // smoothstep
            return 0.18 + t * 0.82;
        }
    }
}
