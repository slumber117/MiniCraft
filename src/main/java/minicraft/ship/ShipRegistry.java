package minicraft.ship;

import java.util.*;

/**
 * ShipRegistry — the single source of truth for all ship types.
 */
public final class ShipRegistry {

    private static ShipRegistry INSTANCE;

    public static ShipRegistry getInstance() {
        if (INSTANCE == null)
            throw new IllegalStateException("ShipRegistry not initialised. Call initialize() first.");
        return INSTANCE;
    }

    public static void initialize() {
        if (INSTANCE != null)
            return;
        INSTANCE = new ShipRegistry();
        INSTANCE.registerAll();
    }

    private final LinkedHashMap<String, ShipDefinition> registry = new LinkedHashMap<>();

    private ShipRegistry() {
    }

    private void registerAll() {
        register(new ShipDefinition(
                "stalwart",
                "Stalwart-class Light Frigate",
                "A reliable UNSC light frigate. Heavy layered alloy plating, twin engine nacelles, forward observation bridge.",
                ShipClass.LIGHT_FRIGATE,
                StalwartSchematic.build(),
                "ship_thumb_stalwart",
                null, // No 3D mesh (voxel based)
                true));

        register(new ShipDefinition(
                "dawn",
                "UNSC Forward Unto Dawn",
                "The legendary Charcoal-class heavy frigate. Features high-fidelity armor plating and refined cinematic geometry. Built for deep space engagement.",
                ShipClass.HEAVY_FRIGATE,
                StalwartSchematic.build(), // Still use voxel for mass/hitbox
                "ship_thumb_dawn",
                "ship_stalwart", // Link to the 54MB mesh
                true));

        register(new ShipDefinition(
                "Castle", "Castle-class Heavy Frigate",
                "Heavy plated, vessel designed for combat and heavy duty transport.",
                ShipClass.SUPER_HEAVY_FRIGATE,
                StalwartSchematic.build(),
                "ship_thumb_Castle",
                "ship_stalwart",
                false));
    }

    private void register(ShipDefinition def) {
        if (registry.containsKey(def.id)) {
            throw new IllegalArgumentException("Duplicate ship ID: " + def.id);
        }
        registry.put(def.id, def);
    }

    public ShipDefinition get(String id) {
        return registry.get(id);
    }

    public List<ShipDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    public int count() {
        return registry.size();
    }
}
