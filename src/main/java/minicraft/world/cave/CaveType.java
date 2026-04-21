package minicraft.world.cave;

/**
 * Classifies the type of cave or underground feature at a voxel.
 *
 * Downstream systems (mob spawners, decorators, loot tables) use this
 * to vary content by cave type — e.g. lava pools only in MAGMA_CHAMBER,
 * bioluminescent fungi only in GROTTO.
 */
public enum CaveType {

    /** Not a cave — solid terrain. */
    NONE,

    /**
     * Standard worm tunnel — narrow, sinuous, 2–5 blocks wide.
     * The most common cave type. Winds through mid-depth terrain.
     */
    TUNNEL,

    /**
     * Large open cavern — wide chambers carved by 3D noise intersection.
     * 10–40+ blocks across. Can contain lakes, stalactites, stalagmites.
     */
    CAVERN,

    /**
     * Small scenic grotto — a single widened pocket, often near the surface.
     * Good candidate for water pools, mossy walls, and natural light shafts.
     */
    GROTTO,

    /**
     * Noodle cave — ultra-narrow, branching, high-frequency worm passages.
     * Dense network at shallow depth, difficult to navigate.
     */
    NOODLE,

    /**
     * Deep magma chamber — wide caves found only at great depth.
     * Floor may be lava. Walls are blackened/obsidian. Hostile spawns.
     */
    MAGMA_CHAMBER,

    /**
     * Underwater cave — carved below a body of water.
     * Fully flooded; connects to ocean/lake floor openings.
     */
    UNDERWATER,

    /**
     * Spaghetti cave — long, tangled mid-frequency tunnels that fork and
     * reconnect frequently, forming a web-like network across a region.
     */
    SPAGHETTI,

    /**
     * Canyon / ravine — near-vertical slice from surface downward.
     * Essentially a surface crack rather than an underground void.
     */
    RAVINE,

    /** Outer hard shell of a gem geode. */
    GEODE_SHELL,
    /** Gem crystal growing inside a geode. */
    GEODE_CRYSTAL,
    /** Air pocket interior of a geode. */
    GEODE_HOLLOW
}
