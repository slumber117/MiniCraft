package minicraft.world.cave.geode;

/**
 * GemType — enumerates every gem variety that can appear inside a geode.
 *
 * Each constant carries:
 * - {@link #minDepthFraction} Minimum normalised depth [0,1] before this gem
 * spawns.
 * - {@link #peakDepthFraction} Depth at which the gem is most likely.
 * - {@link #rarityWeight} Relative weight used for gem-face selection
 * inside a single geode. Lower = rarer.
 *
 * Depth fraction convention (matches CaveCell / CaveCarver):
 * 0.0 = surface, 1.0 = deepest possible cave layer.
 */
public enum GemType {

    // ── Gem definitions ───────────────────────────────────────────────────
    // minDepth peakDepth rarityWeight displayName

    /** Lilac-purple crystals. Appears from mid-depth downward. */
    AMETHYST(0.35f, 0.55f, 100, "Amethyst"),

    /**
     * Brilliant white octahedral crystals. Extremely rare; only in the
     * deepest layers, just above the bedrock zone.
     */
    DIAMOND(0.78f, 0.90f, 8, "Diamond"),

    /** Vivid green hexagonal prisms. Rare; prefers mid-to-deep layers. */
    EMERALD(0.55f, 0.72f, 18, "Emerald"),

    /**
     * Deep red / crimson rhombohedral crystals.
     * Common relative to emerald; appears from moderate depth.
     */
    RUBY(0.42f, 0.62f, 45, "Ruby"),

    /**
     * Violet-blue triclinic crystals. Rarer than amethyst; found only
     * in the deep zone approaching magma-chamber territory.
     * Named after Tanzania where the only known deposit exists.
     */
    TANZANITE(0.72f, 0.88f, 15, "Tanzanite"),

    // ── High-Tier Legendary Gems ───────────────────────────────────────────
    AGATE(0.40f, 0.60f, 55, "Agate"),
    GARNET(0.45f, 0.65f, 90, "Garnet"),
    TOURMALINE(0.50f, 0.70f, 50, "Tourmaline"),
    OPAL(0.55f, 0.75f, 20, "Opal"),
    ALEXANDRITE(0.65f, 0.80f, 15, "Alexandrite"),
    ONYX(0.75f, 0.90f, 7, "Onyx"), // Stronger than Adamantium

    // ── Deepest Rarest Minerals ────────────────────────────────────────────
    PAINITE(0.85f, 0.95f, 5, "Painite"),
    MUSGRAVITE(0.88f, 0.96f, 4, "Musgravite"),
    TAAFFEITE(0.90f, 0.97f, 3, "Taaffeite"),
    GRANDIDIERITE(0.92f, 0.98f, 2, "Grandidierite"),
    SERENDIBITE(0.94f, 0.99f, 1, "Serendibite");

    // ── Fields ────────────────────────────────────────────────────────────

    /** Normalised depth at which this gem first appears. */
    public final float minDepthFraction;

    /** Normalised depth at which spawn probability peaks. */
    public final float peakDepthFraction;

    /**
     * Relative spawn weight compared to other gem types that are eligible
     * at the same depth. Used by GeodeCarver when choosing crystal faces.
     */
    public final int rarityWeight;

    /** Human-readable name for display and serialisation. */
    public final String displayName;

    // ── Constructor ───────────────────────────────────────────────────────

    GemType(float minDepthFraction, float peakDepthFraction,
            int rarityWeight, String displayName) {
        this.minDepthFraction = minDepthFraction;
        this.peakDepthFraction = peakDepthFraction;
        this.rarityWeight = rarityWeight;
        this.displayName = displayName;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Returns the probability multiplier [0, 1] for this gem at the given
     * depth fraction. Uses a smooth tent function identical to CavernCarver's
     * depth envelope so behaviour stays consistent with the rest of the cave
     * system.
     *
     * @param depthFraction Normalised depth, 0 = surface, 1 = bedrock.
     * @return 0.0 if the gem cannot appear here; up to 1.0 at peak depth.
     */
    public float depthMultiplier(float depthFraction) {
        if (depthFraction < minDepthFraction)
            return 0f;

        // Ramp up from minDepth to peakDepth
        if (depthFraction <= peakDepthFraction) {
            float t = (depthFraction - minDepthFraction)
                    / (peakDepthFraction - minDepthFraction);
            return smoothStep(t);
        }

        // Shallow ramp-down from peakDepth toward bedrock (gems thin out but
        // don't disappear entirely — even at maximum depth a trace can appear)
        float t = 1.0f - 0.4f * ((depthFraction - peakDepthFraction)
                / (1.0f - peakDepthFraction));
        return Math.max(0f, smoothStep(t));
    }

    /** Classic cubic smoothstep. */
    private static float smoothStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
