package minicraft.ship;

/**
 * ShipDefinition — immutable data record describing one ship type.
 */
public final class ShipDefinition {

    public final String        id;
    public final String        displayName;
    public final String        description;
    public final ShipClass     shipClass;
    public final ShipSchematic schematic;
    public final String        thumbnailId;
    public final String        modelId;
    public final boolean       driveable;

    public ShipDefinition(String id, String displayName, String description,
                          ShipClass shipClass, ShipSchematic schematic,
                          String thumbnailId, String modelId, boolean driveable) {
        this.id          = id;
        this.displayName = displayName;
        this.description = description;
        this.shipClass   = shipClass;
        this.schematic   = schematic;
        this.thumbnailId = thumbnailId;
        this.modelId     = modelId;
        this.driveable   = driveable;
    }

    public int getBlockCount() {
        return schematic.blockCount;
    }

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
