package minicraft.world;

/**
 * Generates the final heightmap for the world.
 *
 * The elevation of any point is built from four stacked layers:
 *
 *   1. Continentalness base  — very low frequency. Lifts inland regions,
 *                              depresses coastal/ocean zones.
 *
 *   2. Ridged mountain noise — creates the colossal, gradual mountain ranges
 *                              with sharp peaks and broad bases. Only contributes
 *                              meaningfully in high-continentalness regions.
 *
 *   3. Detail fBm            — adds realistic small-scale roughness: hills,
 *                              ravines, local variation.
 *
 *   4. Biome shaping curve   — each biome remaps the blended raw height into
 *                              its own characteristic range, so tundra stays
 *                              flat while redwood forests roll gently and
 *                              deserts produce sandy dunes rather than cliffs.
 *
 * Biome transitions are smooth: we sample climate at a radius of surrounding
 * points and blend the biome parameters before applying the shaping curve.
 */
public class ElevationMap {

    // ── Noise fields ──────────────────────────────────────────────────────

    /** Base terrain — gentle rolling hills and valleys. */
    private final PerlinNoise baseNoise;

    /**
     * Ridge noise — used to carve sharp mountain ridgelines from broad bases.
     * Sampled with the "ridged" technique: value = 1 - |noise()|
     */
    private final PerlinNoise ridgeNoise;

    /** High-frequency detail — small rocks, dunes, undergrowth bumps. */
    private final PerlinNoise detailNoise;

    /**
     * Domain warp for the base terrain.
     * Prevents terrain from looking too grid-aligned.
     */
    private final PerlinNoise terrainWarpNoise;

    // ── Tuning constants ──────────────────────────────────────────────────

    private static final double BASE_SCALE        = 0.003;
    private static final double RIDGE_SCALE       = 0.0018;
    private static final double DETAIL_SCALE      = 0.012;
    private static final double WARP_SCALE        = 0.005;
    private static final double WARP_STRENGTH     = 40.0;

    /**
     * How many octaves the ridge noise runs.
     * More octaves = more fractal detail on the mountainsides.
     */
    private static final int    RIDGE_OCTAVES     = 6;
    private static final double RIDGE_PERSISTENCE = 0.55;

    private static final int    BASE_OCTAVES      = 5;
    private static final double BASE_PERSISTENCE  = 0.5;

    private static final int    DETAIL_OCTAVES    = 4;
    private static final double DETAIL_PERSISTENCE = 0.5;

    /**
     * Radius (in world units) over which biome parameters are blended to
     * produce smooth biome transitions. Larger = wider transition zone.
     */
    private static final int BIOME_BLEND_RADIUS   = 64;
    private static final int BIOME_BLEND_SAMPLES  = 5; // per axis → 5×5 = 25 samples

    // ── Dependencies ──────────────────────────────────────────────────────

    private final ClimateMap climate;

    // ── Constructor ───────────────────────────────────────────────────────

    public ElevationMap(long seed, ClimateMap climate) {
        this.climate        = climate;
        baseNoise           = new PerlinNoise(seed + 10L);
        ridgeNoise          = new PerlinNoise(seed + 20L);
        detailNoise         = new PerlinNoise(seed + 30L);
        terrainWarpNoise    = new PerlinNoise(seed + 40L);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the normalised elevation at (x, z) in [0, 1].
     *
     * @param x World X coordinate
     * @param z World Z coordinate
     * @return  Elevation in [0, 1] where 0.18 is roughly sea level.
     */
    public float getElevation(double x, double z) {
        float raw = getRawElevation(x, z);
        return applyBiomeShaping(x, z, raw);
    }

    /**
     * Returns the raw, unshaped elevation level.
     * This is the "pure" geological height before biomes warp it into
     * specific profiles (like flat deserts or rolling hills).
     */
    public float getRawElevation(double x, double z) {
        // ── 1. Domain warp the coordinates ────────────────────────────────
        double warpX = terrainWarpNoise.noise(x * WARP_SCALE,         0, z * WARP_SCALE        ) * WARP_STRENGTH;
        double warpZ = terrainWarpNoise.noise(x * WARP_SCALE + 3.7,   0, z * WARP_SCALE + 9.2  ) * WARP_STRENGTH;
        double wx = x + warpX;
        double wz = z + warpZ;

        // ── 2. Continentalness — the foundation ───────────────────────────
        float continentalness = climate.getContinentalness(x, z);

        // ── 3. Base rolling terrain ───────────────────────────────────────
        double base = baseNoise.fractalNoise(wx * BASE_SCALE, 0, wz * BASE_SCALE, BASE_OCTAVES, BASE_PERSISTENCE);
        base = (base + 1.0) * 0.5; // → [0, 1]

        // ── 4. Ridged mountain noise ──────────────────────────────────────
        double ridge = ridgedFractal(wx * RIDGE_SCALE, wz * RIDGE_SCALE);

        // Mountains only rise when continentalness is high (deep inland).
        float mountainFactor = mountainInfluence(continentalness);
        double mountainContrib = ridge * mountainFactor;

        // ── 5. Blend layers into a raw elevation ──────────────────────────
        double rawElevation = continentalness * 0.45
                            + base            * 0.25
                            + mountainContrib * 0.30;

        return (float) Math.max(0.0, Math.min(1.0, rawElevation));
    }

    /**
     * Takes a possibly eroded raw elevation and applies biome-specific
     * shaping curves and high-frequency detail.
     */
    public float applyBiomeShaping(double x, double z, float rawElevation) {
        // ── 1. Domain warp for detail sampling ────────────────────────────
        double warpX = terrainWarpNoise.noise(x * WARP_SCALE,         0, z * WARP_SCALE        ) * WARP_STRENGTH;
        double warpZ = terrainWarpNoise.noise(x * WARP_SCALE + 3.7,   0, z * WARP_SCALE + 9.2  ) * WARP_STRENGTH;
        double wx = x + warpX;
        double wz = z + warpZ;

        // ── 2. Biome-blended shaping ──────────────────────────────────────
        BlendedBiomeParams params = sampleBlendedBiomeParams(x, z, rawElevation);

        // ── 3. Apply biome elevation curve ────────────────────────────────
        double shaped = applyBiomeCurve(rawElevation, params.baseHeight, params.maxHeight);

        // ── 4. Add biome-gated detail roughness ───────────────────────────
        double detail = detailNoise.fractalNoise(wx * DETAIL_SCALE, 0, wz * DETAIL_SCALE, DETAIL_OCTAVES, DETAIL_PERSISTENCE);
        detail = (detail + 1.0) * 0.5; // → [0, 1]
        
        shaped += detail * params.roughness * 0.12;

        return (float) Math.max(0.0, Math.min(1.0, shaped));
    }

    // ── Ridged multifractal ───────────────────────────────────────────────

    /**
     * Ridged noise produces sharp ridgelines and broad, rounded bases —
     * the characteristic silhouette of real mountain ranges.
     *
     * At each octave we flip the noise: value = 1 - |noise|.
     * This inverts valleys into peaks. The result is then squared to
     * concentrate energy at the ridgelines and flatten the surrounding terrain.
     *
     * @return value in [0, 1]
     */
    private double ridgedFractal(double x, double z) {
        double total     = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue  = 0;
        double weight    = 1; // carries high-value regions forward (makes ridges cascade)

        for (int i = 0; i < RIDGE_OCTAVES; i++) {
            double n = ridgeNoise.noise(x * frequency, 0, z * frequency);
            // Fold: turn valleys into peaks
            n = 1.0 - Math.abs(n);
            // Square to sharpen the ridgeline
            n = n * n;
            // Weight by previous octave so ridges compound
            n *= weight;
            weight = Math.min(1.0, n * 2.0);

            total    += n * amplitude;
            maxValue += amplitude;

            amplitude *= RIDGE_PERSISTENCE;
            frequency *= 2.0;
        }

        return total / maxValue; // → [0, 1]
    }

    // ── Mountain influence curve ──────────────────────────────────────────

    /**
     * Converts a continentalness value into a mountain contribution scalar.
     *
     * Ocean/coastal regions (c < 0.45) → zero mountain contribution.
     * Inland plains (0.45 – 0.65)      → mountain noise ramps in gradually.
     * Deep interior (0.65+)            → full mountain contribution.
     *
     * This is the key mechanism that makes mountains gradual: they don't
     * appear until you're well inland, and they scale up smoothly from zero.
     */
    private static float mountainInfluence(float continentalness) {
        final float start = 0.45f;
        final float full  = 0.65f;
        if (continentalness <= start) return 0f;
        if (continentalness >= full)  return 1f;
        float t = (continentalness - start) / (full - start);
        // Ease-in: t² so the first foothills are very gentle
        return t * t;
    }

    // ── Biome-blended parameters ──────────────────────────────────────────

    /**
     * Samples a grid of points around (x, z) within BIOME_BLEND_RADIUS and
     * returns biome parameters averaged by distance weight (Gaussian falloff).
     * This is what makes the boundary between, say, grassland and jungle
     * a gradual transition rather than a hard cut.
     */
    private BlendedBiomeParams sampleBlendedBiomeParams(double x, double z, float rawElevation) {
        double totalWeight  = 0;
        double blendBase    = 0;
        double blendMax     = 0;
        double blendRough   = 0;

        int step = (2 * BIOME_BLEND_RADIUS) / (BIOME_BLEND_SAMPLES - 1);

        for (int sx = 0; sx < BIOME_BLEND_SAMPLES; sx++) {
            for (int sz = 0; sz < BIOME_BLEND_SAMPLES; sz++) {
                double sampleX = x + (-BIOME_BLEND_RADIUS + sx * step);
                double sampleZ = z + (-BIOME_BLEND_RADIUS + sz * step);

                float temp    = climate.getTemperature(sampleX, sampleZ);
                float humid   = climate.getHumidity(sampleX, sampleZ);
                float elev    = climate.getContinentalness(sampleX, sampleZ); // use continentalness as rough proxy
                Biome biome   = Biome.classify(temp, humid, rawElevation);    // use actual raw elevation for biome lookup

                double dx = sampleX - x;
                double dz = sampleZ - z;
                double dist2 = dx * dx + dz * dz;

                // Gaussian weight: centre of the kernel has highest influence
                double sigma = BIOME_BLEND_RADIUS * 0.5;
                double weight = Math.exp(-dist2 / (2.0 * sigma * sigma));

                blendBase  += biome.baseHeight * weight;
                blendMax   += biome.maxHeight  * weight;
                blendRough += biome.roughness  * weight;
                totalWeight += weight;
            }
        }

        return new BlendedBiomeParams(
            (float) (blendBase  / totalWeight),
            (float) (blendMax   / totalWeight),
            (float) (blendRough / totalWeight)
        );
    }

    // ── Biome elevation curve ─────────────────────────────────────────────

    /**
     * Remaps a raw elevation value into the biome's characteristic height range.
     *
     * The raw value [0, 1] is treated as a "t" parameter and linearly
     * mapped into [biome.baseHeight, biome.maxHeight].
     *
     * Because mountains already produce raw values near 1.0 via the ridged
     * noise, and oceans produce values near 0.0 via continentalness, the
     * biome's range acts as a final clamp + rescale that ensures biome
     * identity is preserved even across blended boundaries.
     */
    private static double applyBiomeCurve(double rawElevation, float baseHeight, float maxHeight) {
        // Smooth step within the biome's height band
        double t = rawElevation; // already [0, 1]
        t = t * t * (3.0 - 2.0 * t); // smoothstep
        return baseHeight + t * (maxHeight - baseHeight);
    }

    // ── Inner helpers ─────────────────────────────────────────────────────

    private static class BlendedBiomeParams {
        final float baseHeight;
        final float maxHeight;
        final float roughness;

        BlendedBiomeParams(float baseHeight, float maxHeight, float roughness) {
            this.baseHeight = baseHeight;
            this.maxHeight  = maxHeight;
            this.roughness  = roughness;
        }
    }
}
