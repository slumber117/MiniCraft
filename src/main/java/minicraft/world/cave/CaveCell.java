package minicraft.world.cave;

/**
 * Immutable snapshot of cave carver data for a single voxel.
 *
 * The cave system operates in 3D — X/Y/Z — where Y is depth below the
 * surface. Every voxel in the world volume can be queried independently,
 * making this compatible with chunk-by-chunk loading.
 */
public final class CaveCell {

    /**
     * Whether this voxel has been carved out (is air/void inside the cave).
     * If false, the voxel is solid rock/terrain.
     */
    public final boolean isCarved;

    /**
     * The type of cave feature at this voxel — allows downstream systems
     * (renderers, mob spawners, loot placers) to respond differently to
     * different cave environments.
     */
    public final CaveType type;

    /**
     * Normalised depth of this voxel below the surface, [0, 1].
     * 0 = at the surface, 1 = at max cave depth.
     * Useful for placing depth-specific ores, mobs, or atmosphere.
     */
    public final float depth;

    public CaveCell(boolean isCarved, CaveType type, float depth) {
        this.isCarved = isCarved;
        this.type     = type;
        this.depth    = depth;
    }

    public static final CaveCell SOLID = new CaveCell(false, CaveType.NONE, 0f);

    @Override
    public String toString() {
        return String.format("CaveCell[carved=%b, type=%s, depth=%.2f]", isCarved, type, depth);
    }
}
