package minicraft.world;

/**
 * Immutable snapshot of all world-generation data for a single tile.
 *
 * Anything downstream (chunk builders, renderers, mob spawners, surface
 * placers) should consume this and never re-run the noise themselves.
 */
public final class WorldCell {

    /** Normalised elevation in [0, 1]. Sea level ≈ 0.18. */
    public final float elevation;

    /** Normalised temperature in [0, 1]. */
    public final float temperature;

    /** Normalised humidity in [0, 1]. */
    public final float humidity;

    /** Continentalness — proximity to ocean vs. deep interior [0, 1]. */
    public final float continentalness;

    /** The dominant biome at this tile after blending. */
    public final Biome biome;

    /**
     * Whether this tile is under water (elevation < sea level).
     * Convenience flag so callers don't need to know the sea level constant.
     */
    public final boolean isWater;

    /** Sea level threshold — elevation below this is ocean/lake. */
    public static final float SEA_LEVEL = 0.18f;

    public WorldCell(float elevation, float temperature, float humidity,
                     float continentalness, Biome biome) {
        this.elevation       = elevation;
        this.temperature     = temperature;
        this.humidity        = humidity;
        this.continentalness = continentalness;
        this.biome           = biome;
        this.isWater         = elevation < SEA_LEVEL;
    }

    @Override
    public String toString() {
        return String.format("WorldCell[biome=%s, elev=%.3f, temp=%.2f, humid=%.2f, water=%b]",
            biome.displayName, elevation, temperature, humidity, isWater);
    }
}
