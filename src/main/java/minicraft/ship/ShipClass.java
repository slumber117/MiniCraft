package minicraft.ship;

/**
 * Broad classification of a ship's size and role.
 */
public enum ShipClass {

    FIGHTER         ("Fighter",          1,    50_000f,  0.95f),
    CORVETTE        ("Corvette",         3,   200_000f,  0.85f),
    LIGHT_FRIGATE   ("Light Frigate",   10, 1_200_000f,  0.70f),
    FRIGATE         ("Frigate",         50, 5_000_000f,  0.60f),
    DESTROYER       ("Destroyer",      200,20_000_000f,  0.45f),
    CARRIER         ("Carrier",        500,80_000_000f,  0.30f);

    public final String displayName;
    public final int minCrew;
    public final float baseMass;
    public final float dragMultiplier;

    ShipClass(String displayName, int minCrew, float baseMass, float dragMultiplier) {
        this.displayName    = displayName;
        this.minCrew        = minCrew;
        this.baseMass       = baseMass;
        this.dragMultiplier = dragMultiplier;
    }
}
