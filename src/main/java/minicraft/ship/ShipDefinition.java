package minicraft.ship;

/**
 * ShipDefinition — immutable data record describing one ship type.
 *
 * This is the "what is this ship" record. It contains everything the
 * UI needs to display the ship and everything the spawner needs to
 * place it. No game logic lives here.
 *
 * Instances are created in ShipSchematics and registered in ShipRegistry
 * at startup. They are flyweight objects — one instance shared everywhere.
 *
 * ── Fields ────────────────────────────────────────────────────────────
 *
 *   id          → Unique string key used by the registry and save system.
 *   displayName → Shown in the ship selection UI.
 *   description → Flavour text / lore blurb.
 *   shipClass   → Drives default physics tuning and UI stat scaling.
 *   schematic   → The actual voxel blueprint.
 *   thumbnailId → Texture registry key for the UI preview sprite.
 *                 (Can be a pre-rendered side-view sprite or a placeholder.)
 *   driveable   → Whether a player can pilot this ship. Some ships are
 *                 purely decorative (docked mega-structures, etc.)
 */
public final class ShipDefinition {

    public final String        id;
    public final String        displayName;
    public final String        description;
    public final ShipClass     shipClass;
    public final ShipSchematic schematic;
    public final String        thumbnailId;

    /**
     * Optional 3D mesh ID for cinematic/high-fidelity render modes.
     * Null means the ship is rendered purely from its voxel schematic.
     * When non-null, the renderer may substitute a pre-built mesh for the
     * exterior visual while the schematic still drives physics and hitboxes.
     */
    public final String        meshId;

    public final boolean       driveable;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Full constructor — use when a separate 3D mesh is available. */
    public ShipDefinition(String id, String displayName, String description,
                          ShipClass shipClass, ShipSchematic schematic,
                          String thumbnailId, String meshId, boolean driveable) {
        this.id          = id;
        this.displayName = displayName;
        this.description = description;
        this.shipClass   = shipClass;
        this.schematic   = schematic;
        this.thumbnailId = thumbnailId;
        this.meshId      = meshId;       // may be null
        this.driveable   = driveable;
    }

    /** Convenience constructor — no separate 3D mesh (voxel-only rendering). */
    public ShipDefinition(String id, String displayName, String description,
                          ShipClass shipClass, ShipSchematic schematic,
                          String thumbnailId, boolean driveable) {
        this(id, displayName, description, shipClass, schematic, thumbnailId, null, driveable);
    }

    /** Returns true if this ship has a separate high-fidelity 3D mesh. */
    public boolean hasMesh() { return meshId != null; }

    /**
     * Computes the total block count of this ship's schematic.
     * Convenience accessor for the UI stat panel.
     */
    public int getBlockCount() {
        return schematic.blockCount;
    }

    /**
     * Returns the approximate dimensions of this ship in blocks.
     * Used in the UI stat panel: "Length: 60 | Width: 30 | Height: 12"
     */
    public String getDimensionsString() {
        return String.format("L:%d  W:%d  H:%d",
            schematic.getDepth(),
            schematic.getWidth(),
            schematic.getHeight());
    }

    @Override
    public String toString() {
        return String.format("ShipDefinition[id=%s, class=%s, blocks=%d]",
            id, shipClass.displayName, schematic.blockCount);
    }
}
