package com.github.xandergos.terraindiffusionmc.world;

/**
 * Stripped version of WorldScaleManager for standalone MiniCraft.
 * Hardcoded to DEFAULT_SCALE.
 */
public final class WorldScaleManager {
    public static final int DEFAULT_SCALE = 2;
    private static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 6;

    private static int currentScale = DEFAULT_SCALE;

    private WorldScaleManager() {
    }

    public static int getCurrentScale() {
        return currentScale;
    }

    public static void setCurrentScale(int configuredScale) {
        currentScale = clampScale(configuredScale);
    }

    public static int clampScale(int configuredScale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, configuredScale));
    }
}
