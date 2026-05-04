package minicraft.world.fortress;

/**
 * FortressCell — immutable snapshot of fortress data for a single voxel.
 *
 * ── Layer meanings ───────────────────────────────────────────────────────
 *
 *   OUTSIDE   Not part of any fortress. Terrain generates as normal.
 *
 *   WALL      Structural stone/brick — the outer shell of a room or the
 *             sides of a corridor. Renderer should place fortress stone here.
 *
 *   FLOOR     The bottom solid layer of a room or corridor (y == anchorY).
 *             Renderer places flagstone, cracked stone bricks, etc.
 *
 *   CEILING   The top solid layer (y == anchorY + WALL_HEIGHT). Same
 *             material as wall typically.
 *
 *   INTERIOR  Open air inside the fortress. The voxel should be air/empty.
 *             Decorators use {@link #roomType} to decide what to put here.
 *
 * ── Room type ────────────────────────────────────────────────────────────
 *
 *   Non-null for FLOOR, INTERIOR.  Null for WALL, CEILING, OUTSIDE.
 *   Use this to drive chest placement (CHEST_ROOM), mob spawners, etc.
 *
 * ── Usage example ────────────────────────────────────────────────────────
 *
 * <pre>
 *   FortressCell cell = fortressCarver.query(x, y, z, surfaceY);
 *
 *   switch (cell.layer) {
 *       case OUTSIDE:   // do nothing — normal terrain
 *       case WALL:
 *       case CEILING:   placeBlock(x, y, z, STONE_BRICKS); break;
 *       case FLOOR:     placeBlock(x, y, z, CRACKED_STONE_BRICKS); break;
 *       case INTERIOR:
 *           placeBlock(x, y, z, AIR);
 *           if (cell.roomType == FortressRoomType.CHEST_ROOM) {
 *               // schedule chest placement for this room
 *           }
 *           break;
 *   }
 * </pre>
 */
public final class FortressCell {

    // ── Layer ──────────────────────────────────────────────────────────────

    public enum Layer {
        OUTSIDE,
        WALL,
        FLOOR,
        CEILING,
        INTERIOR
    }

    // ── Singleton for the common case ──────────────────────────────────────

    /** Returned for every voxel not part of any fortress. */
    public static final FortressCell OUTSIDE =
            new FortressCell(Layer.OUTSIDE, null);

    // ── Fields ─────────────────────────────────────────────────────────────

    /** Structural layer this voxel occupies. */
    public final Layer layer;

    /**
     * Type of room this voxel belongs to.
     * Non-null only for FLOOR and INTERIOR layers.
     * Null for WALL, CEILING, OUTSIDE.
     */
    public final FortressRoomType roomType;

    // ── Constructor ────────────────────────────────────────────────────────

    public FortressCell(Layer layer, FortressRoomType roomType) {
        this.layer    = layer;
        this.roomType = roomType;
    }

    // ── Convenience predicates ─────────────────────────────────────────────

    /** True if this voxel should be solid (wall or ceiling). */
    public boolean isSolid() {
        return layer == Layer.WALL || layer == Layer.CEILING || layer == Layer.FLOOR;
    }

    /** True if this voxel is open air inside the fortress. */
    public boolean isInterior() {
        return layer == Layer.INTERIOR;
    }

    /** True if this voxel is part of a chest room (floor or interior). */
    public boolean isChestRoom() {
        return roomType == FortressRoomType.CHEST_ROOM;
    }

    @Override
    public String toString() {
        return String.format("FortressCell[layer=%s, room=%s]", layer, roomType);
    }
}