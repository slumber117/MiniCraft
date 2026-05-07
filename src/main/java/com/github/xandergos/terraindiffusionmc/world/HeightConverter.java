package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.pipeline.WorldPipelineModelConfig;

/**
 * HeightConverter — maps raw diffusion-model metre values to Minecraft
 * Y-levels.
 *
 * This is a pure coordinate adapter: it does not generate terrain, it only
 * rescales the diffusion model's output into a range the chunk builder can use.
 *
 * ── What changed vs the original ────────────────────────────────────────
 *
 * OLD: resolution = nativeResolution / clampedScale / 1.8
 * NEW: resolution = nativeResolution / clampedScale (÷1.8 removed)
 *
 * The /1.8 divisor was explicitly described as "stretching terrain
 * vertically for dramatic peaks." In practice it turned every hill into
 * a sheer cliff and compressed ocean floors into almost nothing, making
 * the entire world look like a dripstone mountain range regardless of the
 * biome the diffusion model intended.
 *
 * OLD: GAMMA = 0.92 (nearly linear — peaks barely compressed)
 * NEW: GAMMA = 0.72 (stronger compression — peaks tamed, lowlands wider)
 *
 * A lower gamma means the curve bends harder: very high metre values
 * (mountain peaks) map to only modestly higher Y-levels than mid-range
 * values, while the broad lowland and ocean-floor range gets more Y-space.
 * The result is that the diffusion model's natural biome elevation
 * distribution — flat tundra, coastal plains, mid-height forests,
 * occasional tall peaks — is preserved rather than collapsed into cliffs.
 *
 * OCEAN depth mapping is unchanged (linear negative metres).
 *
 * ── Elevation distribution after the fix ────────────────────────────────
 *
 * Approx. Minecraft Y bands produced for typical diffusion output:
 *
 * Deep ocean floor : 350 – 480 (below SEA_LEVEL = 512)
 * Shallow ocean : 480 – 511
 * Coastline / beach : 512 – 520
 * Plains / tundra : 520 – 560
 * Forest / redwood : 540 – 600
 * Highland : 580 – 660
 * Mountain peaks : 660 – 750 (rare — only genuine high-altitude tiles)
 *
 * ── Tuning guide ────────────────────────────────────────────────────────
 *
 * If terrain is still too flat → raise GAMMA toward 0.85
 * If peaks are still too sharp → lower GAMMA toward 0.60
 * If oceans are too shallow → raise SEA_LEVEL or increase OCEAN_SCALE
 * If oceans are too deep → lower OCEAN_SCALE toward 0.6
 */
public class HeightConverter {

    /** Minecraft Y at which the ocean surface sits. */
    private static final int SEA_LEVEL = 512;

    /**
     * Gamma exponent applied to land heights.
     *
     * < 1.0 → compresses peaks, expands lowlands (flatter overall feel)
     * = 1.0 → linear (no curve)
     * > 1.0 → would amplify peaks (do not use here)
     *
     * 0.72 gives a comfortable range: gentle hills stay gentle,
     * true mountains are still visible but not sheer cliffs.
     */
    private static final float GAMMA = 0.72f;

    /**
     * Offset applied before gamma to avoid a discontinuity at 0 m.
     * Larger C → smoother curve near sea level.
     */
    private static final float C = 25.0f;
    private static final float C_GAMMA = (float) Math.pow(C, GAMMA);

    /**
     * Scale factor for negative (ocean) metre values.
     * 1.0 = linear depth, < 1.0 = shallower oceans, > 1.0 = deeper trenches.
     * 0.75 keeps the ocean floor visible and not excessively deep.
     */
    private static final float OCEAN_SCALE = 0.75f;

    // ── Resolution ────────────────────────────────────────────────────────

    /**
     * Metres-per-Y-level ratio derived from the model's native resolution
     * and the configured world scale.
     *
     * The original code divided by an additional 1.8 here to "stretch"
     * the terrain vertically. That divisor has been removed: dividing
     * resolution by 1.8 makes it smaller, which in turn makes
     * (transformed / resolution) larger — amplifying every height value
     * and causing the cliff/dripstone appearance.
     */
    private static float getResolutionForScale(int configuredScale) {
        return WorldPipelineModelConfig.nativeResolution()
                / (float) WorldScaleManager.clampScale(configuredScale);
        // Note: do NOT divide by 1.8 here. That was the source of the
        // pencil-cliff terrain that buried all biome variety under mountains.
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Converts a raw diffusion-model elevation in metres to a Minecraft
     * Y-level, using the world's current scale setting.
     *
     * @param meters Signed elevation from the diffusion model.
     *               Positive = land above sea level, negative = ocean floor.
     */
    public static int convertToMinecraftHeight(short meters) {
        return convertToMinecraftHeight(meters, WorldScaleManager.getCurrentScale());
    }

    /**
     * Converts a raw metre value at an explicit scale setting.
     * Useful for preview rendering or LOD systems that use a different scale.
     *
     * @param meters          Signed diffusion elevation (metres)
     * @param configuredScale World scale factor (forwarded to WorldScaleManager)
     */
    public static int convertToMinecraftHeight(short meters, int configuredScale) {
        float resolution = getResolutionForScale(configuredScale);
        float transformed;

        if (meters >= 0) {
            // ── Land: gamma compression ────────────────────────────────────
            // Peaks compress toward the ceiling; lowlands spread out.
            // The +C offset keeps the curve smooth across the 0 m boundary.
            transformed = (float) (Math.pow(meters + C, GAMMA) - C_GAMMA);
        } else {
            // ── Ocean: scaled linear mapping ───────────────────────────────
            // Negative values stay negative and scale linearly so ocean
            // floors don't become unreachably deep.
            transformed = meters * OCEAN_SCALE;
        }

        return (int) (transformed / resolution) + SEA_LEVEL;
    }
}