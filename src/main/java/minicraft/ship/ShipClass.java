package minicraft.ship;

/**
 * Broad classification of a ship's size and role.
 *
 * Every ShipDefinition belongs to exactly one ShipClass. The class drives
 * default physics tuning (drag, thruster power caps) and UI presentation
 * (icon border colour, stat bar scaling).
 *
 * The Halo Stalwart-class sits at LIGHT_FRIGATE — large enough to feel
 * heavy and colossal, small enough to be driveable solo.
 */
public enum ShipClass {

    /**
     * Tiny personal craft. 1–2 crew. Fast, fragile, very agile.
     * Example: Pelican-scale dropship.
     */
    FIGHTER         ("Fighter",          1,    50_000f,  0.95f),

    /**
     * Small combat vessel. 3–10 crew. Balanced speed and armour.
     * Example: UNSC Prowler.
     */
    CORVETTE        ("Corvette",         3,   200_000f,  0.85f),

    /**
     * Medium warship. Halo Stalwart-class falls here.
     * 10–50 crew. Heavy, sluggish turns, powerful thrust.
     */
    LIGHT_FRIGATE   ("Light Frigate",   10, 1_200_000f,  0.70f),

    /**
     * Full frigate. 50–200 crew. Slow acceleration, massive inertia.
     */
    FRIGATE              ("Frigate",              50,  5_000_000f,  0.35f),

    /**
     * Heavy frigate — Forward Unto Dawn scale.
     * 100–300 crew. Significantly more mass than a standard frigate.
     */
    HEAVY_FRIGATE        ("Heavy Frigate",        100, 12_000_000f,  0.25f),

    /**
     * Super-heavy frigate — oversized, heavily armoured assault vessel.
     * 200–500 crew. Extremely sluggish turns, devastating broadside capacity.
     */
    SUPER_HEAVY_FRIGATE  ("Super Heavy Frigate",  200, 30_000_000f,  0.12f),

    /**
     * Heavy capital ship. 200+ crew. Turns like a small continent.
     */
    DESTROYER            ("Destroyer",            300, 60_000_000f,  0.08f),

    /**
     * Largest class. Carrier / battlecruiser scale.
     * Acceleration is geological. Top speed is surprisingly high once moving.
     */
    CARRIER         ("Carrier",        500,80_000_000f,  0.05f);

    // ── Per-class parameters ──────────────────────────────────────────────

    /** Human-readable display name shown in the ship selection UI. */
    public final String displayName;

    /** Minimum crew the ship needs to be summoned (reserved for future use). */
    public final int minCrew;

    /**
     * Base mass in kilograms used as a starting point before per-block
     * mass is summed. The final mass = baseMass + sum(blockMasses).
     * This ensures even an empty schematic has class-appropriate inertia.
     */
    public final float baseMass;

    /**
     * Base drag coefficient multiplier for this class.
     * Higher = more drag = lower terminal velocity = snappier stops.
     * Physics engine scales this by the ship's cross-sectional area.
     */
    public final float dragMultiplier;

    /**
     * Scaling factor for engine power. 
     * Higher = faster acceleration/turn.
     */
    public final float accelerationMultiplier;

    ShipClass(String displayName, int minCrew, float baseMass, float accelerationMultiplier) {
        this.displayName            = displayName;
        this.minCrew                = minCrew;
        this.baseMass               = baseMass;
        this.accelerationMultiplier = accelerationMultiplier;
        this.dragMultiplier         = 1.0f - accelerationMultiplier; // Derived drag
    }
}
