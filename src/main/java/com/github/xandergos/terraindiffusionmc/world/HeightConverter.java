package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.pipeline.WorldPipelineModelConfig;

public class HeightConverter {
    private static final int SEA_LEVEL = 102;
    private static final float GAMMA = 0.92f;
    private static final float C = 30.0f;
    private static final float C_GAMMA = (float) Math.pow(C, GAMMA);

    private static float getResolutionForScale(int configuredScale) {
        // We divide by 1.8 to "stretch" the terrain vertically for more dramatic peaks
        return (WorldPipelineModelConfig.nativeResolution() / WorldScaleManager.clampScale(configuredScale)) / 1.8f;
    }

    public static int convertToMinecraftHeight(short meters) {
        return convertToMinecraftHeight(meters, WorldScaleManager.getCurrentScale());
    }

    public static int convertToMinecraftHeight(short meters, int configuredScale) {
        float resolution = getResolutionForScale(configuredScale);
        float transformed;

        if (meters >= 0) {
            // High-fidelity gamma compression: keeps detail in lowlands, prevents clipping at peaks
            transformed = (float) (Math.pow(meters + C, GAMMA) - C_GAMMA);
        } else {
            // Linear ocean mapping for realistic deep trenches
            transformed = meters;
        }

        return (int) (transformed / resolution) + SEA_LEVEL;
    }

}
