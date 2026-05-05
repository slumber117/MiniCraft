package minicraft.renderer.ui;

import minicraft.math.Vector4f;

/**
 * Shared color palette and UI constants.
 */
public class UIPalette {
    public static final Vector4f HEALTH_COLOR = new Vector4f(0.82f, 0.18f, 0.18f, 1.0f);
    public static final Vector4f HEALTH_COLOR_DARK = new Vector4f(0.50f, 0.08f, 0.08f, 1.0f);
    public static final Vector4f HUNGER_COLOR = new Vector4f(0.85f, 0.45f, 0.12f, 1.0f);
    public static final Vector4f HUNGER_COLOR_DARK = new Vector4f(0.55f, 0.25f, 0.06f, 1.0f);

    // Rustic Palette
    public static final Vector4f RUSTIC_BG = new Vector4f(0.14f, 0.13f, 0.12f, 0.98f);
    public static final Vector4f RUSTIC_BORDER = new Vector4f(0.38f, 0.22f, 0.12f, 1.0f);
    public static final Vector4f RUSTIC_LIGHT_WOOD = new Vector4f(0.52f, 0.35f, 0.22f, 1.0f);
    public static final Vector4f RUSTIC_STONE = new Vector4f(0.45f, 0.45f, 0.48f, 1.0f);
    public static final Vector4f RUSTIC_PARCHMENT = new Vector4f(0.88f, 0.82f, 0.68f, 1.0f);

    public static final Vector4f TEXT_COLOR = new Vector4f(0.92f, 0.88f, 0.82f, 1.0f);
    public static final Vector4f CROSSHAIR_COLOR = new Vector4f(0.92f, 0.88f, 0.82f, 0.85f);

    public static final Vector4f TACT_ORANGE = new Vector4f(0.72f, 0.38f, 0.12f, 1.0f);
    public static final Vector4f TACT_BLUE = new Vector4f(0.35f, 0.48f, 0.62f, 1.0f);
    public static final Vector4f TACT_GREEN = new Vector4f(0.28f, 0.48f, 0.22f, 1.0f);
    public static final Vector4f TACT_DIM = new Vector4f(0.08f, 0.08f, 0.06f, 0.75f);

    public static final Vector4f BAR_SHINE = new Vector4f(1.00f, 1.00f, 1.00f, 0.05f);
    public static final Vector4f SLOT_HOVER = new Vector4f(0.52f, 0.35f, 0.22f, 0.25f);

    // ── Medieval Health Palette ───────────────────────────────────────────

    /** Deep crimson fill for a full heart. */
    public static final Vector4f HEART_FULL = new Vector4f(0.78f, 0.08f, 0.08f, 1.0f);

    /** Brighter highlight on the upper-left of a full heart (faux sheen). */
    public static final Vector4f HEART_SHINE = new Vector4f(0.96f, 0.34f, 0.34f, 1.0f);

    /** Almost-black shadow rim around each heart. */
    public static final Vector4f HEART_SHADOW = new Vector4f(0.10f, 0.03f, 0.03f, 1.0f);

    /** Half-heart fill — dark amber, feels like a cracked gemstone. */
    public static final Vector4f HEART_HALF = new Vector4f(0.55f, 0.10f, 0.10f, 1.0f);

    /** Empty heart — dark stone hollow. */
    public static final Vector4f HEART_EMPTY = new Vector4f(0.18f, 0.14f, 0.14f, 1.0f);

    /** Dark, slightly warm border for the heart tray frame. */
    public static final Vector4f HEART_FRAME_BG = new Vector4f(0.11f, 0.09f, 0.08f, 0.92f);

    /** Outer stone border of the heart tray. */
    public static final Vector4f HEART_FRAME_BORDER = new Vector4f(0.42f, 0.28f, 0.18f, 1.0f);

    /** Warm stone colour for battlements / notches. */
    public static final Vector4f HEART_FRAME_STONE = new Vector4f(0.32f, 0.25f, 0.18f, 1.0f);

    /** Parchment-gold for the "VITALITY" label. */
    public static final Vector4f HEART_LABEL = new Vector4f(0.85f, 0.72f, 0.42f, 1.0f);

    /** Dim gold for the label when health is critical. */
    public static final Vector4f HEART_LABEL_CRITICAL = new Vector4f(0.90f, 0.20f, 0.10f, 1.0f);
}