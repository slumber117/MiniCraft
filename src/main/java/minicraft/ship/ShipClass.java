package minicraft.ship;

/**
 * Broad classification of a ship's size and role.
 */
public enum ShipClass {

    FIGHTER("Fighter", 1, 50_000f, 0.95f),
    TRANSPORT("Transport", 1, 75_000f, 0.85f),
    CORVETTE("Corvette", 3, 200_000f, 0.85f),
    LIGHT_FRIGATE("Light Frigate", 10, 1_200_000f, 0.70f),
    HEAVY_FRIGATE("Heavy Frigate", 25, 4_500_000f, 0.65f),
    SUPER_HEAVY_FRIGATE("Super Heavy Frigate", 50, 10_000_000f, 0.60f),
    FRIGATE("Frigate", 50, 5_000_000f, 0.60f),
    DESTROYER("Destroyer", 200, 20_000_000f, 0.45f),
    HEAVY_DESTROYER("Heavy Destroyer", 300, 30_000_000f, 0.40f),
    CARRIER("Carrier", 500, 80_000_000f, 0.30f),
    HEAVY_CARRIER("Heavy Carrier", 700, 100_000_000f, 0.25f),
    SUPER_HEAVY_CARRIER("Super Heavy Carrier", 1000, 120_000_000f, 0.20f);

    public final String displayName;
    public final int minCrew;
    public final float baseMass;
    public final float dragMultiplier;

    ShipClass(String displayName, int minCrew, float baseMass, float dragMultiplier) {
        this.displayName = displayName;
        this.minCrew = minCrew;
        this.baseMass = baseMass;
        this.dragMultiplier = dragMultiplier;
    }
}
