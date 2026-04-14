package minicraft.ship;

import java.util.*;

/**
 * ShipRegistry — the single source of truth for all ship types.
 *
 * Loaded once at game startup via {@link #initialize()}. After that,
 * all access is read-only and fully thread-safe — any thread can call
 * {@link #get(String)} or {@link #getAll()} without synchronisation.
 *
 * ── Adding a new ship ────────────────────────────────────────────────
 *
 *   1. Create a schematic class (see StalwartSchematic for the pattern).
 *   2. Call register() inside initialize() below.
 *   3. Add a thumbnail texture to your texture atlas with the same key.
 *   Done. The UI and spawner pick it up automatically.
 */
public final class ShipRegistry {

    // ── Singleton ─────────────────────────────────────────────────────────

    private static ShipRegistry INSTANCE;

    public static ShipRegistry getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("ShipRegistry not initialised. Call initialize() first.");
        return INSTANCE;
    }

    /**
     * Builds all ship definitions and initialises the registry.
     * Call once during game startup, before any UI or world code runs.
     */
    public static void initialize() {
        if (INSTANCE != null) return; // idempotent
        INSTANCE = new ShipRegistry();
        INSTANCE.registerAll();
    }

    // ── Storage ───────────────────────────────────────────────────────────

    /** Insertion-ordered so the UI displays ships in a consistent order. */
    private final LinkedHashMap<String, ShipDefinition> registry = new LinkedHashMap<>();

    private ShipRegistry() {}

    // ── Registration ──────────────────────────────────────────────────────

    /**
     * Registers all ship definitions. Add new ships here.
     */
    private void registerAll() {
        // ── Stalwart-class Light Frigate ──────────────────────────────────
        register(new ShipDefinition(
            "stalwart",
            "Stalwart-class Light Frigate",
            "A reliable UNSC light frigate. Heavy layered alloy plating, twin engine nacelles, " +
            "forward observation bridge. Slow to turn but devastatingly powerful once at speed.",
            ShipClass.LIGHT_FRIGATE,
            StalwartSchematic.build(),
            "ship_thumb_stalwart",
            true
        ));

        // ── Castle-class Super Heavy Frigate ──────────────────────────────
        register(new ShipDefinition(
            "castle",
            "Castle-class Super Heavy Frigate",
            "A UNSC super heavy frigate built for prolonged deep-space engagement. " +
            "Blunt slab hull, layered dorsal superstructure, quad engine cluster, MAC gun. " +
            "Turns like a continent. Given enough time, reaches terrifying speed.",
            ShipClass.SUPER_HEAVY_FRIGATE,
            CastleSchematic.build(),
            "ship_thumb_castle",
            true
        ));

        // ── Forward Unto Dawn (heavy frigate, mesh-linked) ────────────────
        register(new ShipDefinition(
            "dawn",
            "UNSC Forward Unto Dawn",
            "The legendary Charon-class light frigate. Sleeker than the Castle-class " +
            "but still massively armoured. Famous for its role at Installation 00.",
            ShipClass.HEAVY_FRIGATE,
            StalwartSchematic.build(),   // reuses Stalwart voxels for physics/hitbox
            "ship_thumb_dawn",
            "ship_stalwart",             // links to 3D mesh when renderer supports it
            true
        ));
    }

    private void register(ShipDefinition def) {
        if (registry.containsKey(def.id)) {
            throw new IllegalArgumentException("Duplicate ship ID: " + def.id);
        }
        registry.put(def.id, def);
    }

    // ── Query API ─────────────────────────────────────────────────────────

    /**
     * Returns the ShipDefinition for the given ID, or null if not found.
     */
    public ShipDefinition get(String id) {
        return registry.get(id);
    }

    /**
     * Returns all registered ships in registration order.
     * The returned list is unmodifiable.
     */
    public List<ShipDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    /** Returns the number of registered ship types. */
    public int count() {
        return registry.size();
    }
}
