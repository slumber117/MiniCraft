package minicraft.world;

/**
 * Defines all biome types and their terrain shaping parameters.
 *
 * Biomes are determined by the intersection of temperature and humidity,
 * following a simplified Whittaker diagram. Each biome carries its own
 * elevation curve, surface roughness, and base height — so the same
 * underlying noise value produces very different landscapes depending on
 * which biome it falls into.
 */
public enum Biome {

    // ── Aquatic ────────────────────────────────────────────────────────────
    OCEAN        (0.00f, 0.12f, 0.05f, "Ocean"),
    FROZEN_OCEAN (0.00f, 0.08f, 0.03f, "Frozen Ocean"),

    // ── Cold / Arctic ──────────────────────────────────────────────────────
    ARCTIC       (0.05f, 0.10f, 0.06f, "Arctic"),
    TUNDRA       (0.10f, 0.16f, 0.10f, "Tundra"),
    SNOWY_FOREST (0.15f, 0.22f, 0.12f, "Snowy Forest"),

    // ── Temperate ──────────────────────────────────────────────────────────
    GRASSLAND    (0.20f, 0.28f, 0.08f, "Grassland"),
    FOREST       (0.22f, 0.32f, 0.10f, "Forest"),
    REDWOOD      (0.25f, 0.38f, 0.13f, "Redwood Forest"),

    // ── Warm / Tropical ────────────────────────────────────────────────────
    JUNGLE       (0.28f, 0.45f, 0.18f, "Dense Jungle"),
    SAVANNA      (0.22f, 0.30f, 0.09f, "Savanna"),

    // ── Arid ───────────────────────────────────────────────────────────────
    DESERT       (0.18f, 0.20f, 0.07f, "Desert"),

    // ── Highland / Mountain ────────────────────────────────────────────────
    HIGHLANDS    (0.35f, 0.55f, 0.22f, "Highlands"),
    MOUNTAINS    (0.55f, 0.90f, 0.30f, "Mountains"),
    SNOWY_PEAKS  (0.60f, 0.95f, 0.25f, "Snowy Peaks");

    // ── Per-biome terrain parameters ───────────────────────────────────────

    /**
     * The minimum normalised height [0..1] this biome sits at.
     * Ocean biomes cluster near 0; mountains near 0.8+.
     */
    public final float baseHeight;

    /**
     * The maximum normalised height [0..1] terrain in this biome can reach.
     */
    public final float maxHeight;

    /**
     * How much fine-detail noise (high-frequency fBm) is allowed to
     * contribute on top of the base shape. Low = smooth flatlands.
     * High = jagged ridges and rocky surfaces.
     */
    public final float roughness;

    /** Human-readable display name. */
    public final String displayName;

    Biome(float baseHeight, float maxHeight, float roughness, String displayName) {
        this.baseHeight  = baseHeight;
        this.maxHeight   = maxHeight;
        this.roughness   = roughness;
        this.displayName = displayName;
    }

    // ── Biome lookup ───────────────────────────────────────────────────────

    /**
     * Derives a biome from normalised temperature, humidity, and elevation.
     *
     * @param temperature 0 = arctic cold, 1 = scorching hot
     * @param humidity    0 = bone dry,    1 = saturated
     * @param elevation   0 = sea floor,   1 = highest peak (normalised)
     */
    public static Biome classify(float temperature, float humidity, float elevation) {

        // ── Water bodies ──────────────────────────────────────────────────
        if (elevation < 0.18f) {
            return temperature < 0.25f ? FROZEN_OCEAN : OCEAN;
        }

        // ── High-altitude: override all climate rules ──────────────────────
        if (elevation > 0.60f) {
            return temperature < 0.30f ? SNOWY_PEAKS : MOUNTAINS;
        }
        if (elevation > 0.45f) {
            return temperature < 0.35f ? SNOWY_PEAKS : HIGHLANDS;
        }

        // ── Cold band (temperature 0.0 – 0.35) ───────────────────────────
        if (temperature < 0.20f) {
            return humidity < 0.35f ? ARCTIC : TUNDRA;
        }
        if (temperature < 0.35f) {
            return humidity < 0.40f ? TUNDRA : SNOWY_FOREST;
        }

        // ── Temperate band (temperature 0.35 – 0.60) ──────────────────────
        if (temperature < 0.60f) {
            if (humidity < 0.30f) return GRASSLAND;
            if (humidity < 0.55f) return FOREST;
            return REDWOOD;
        }

        // ── Warm / tropical band (temperature 0.60 – 0.80) ───────────────
        if (temperature < 0.80f) {
            if (humidity < 0.35f) return SAVANNA;
            if (humidity < 0.65f) return GRASSLAND;
            return JUNGLE;
        }

        // ── Hot band (temperature 0.80+) ──────────────────────────────────
        return humidity < 0.40f ? DESERT : JUNGLE;
    }
}
