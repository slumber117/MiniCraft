package minicraft.world.cave.geode;

/**
 * GeodeCell — immutable snapshot of geode data for a single voxel.
 *
 * A voxel inside a geode is in one of three states:
 *
 *   OUTSIDE  — not part of any geode (solid terrain as normal)
 *   SHELL    — the outer calcite / basalt rind of the geode
 *   HOLLOW   — the air pocket interior of the geode
 *   CRYSTAL  — a gem crystal node on the inner wall of the geode
 *
 * The {@link #gemType} field is non-null only when {@link #layer} is CRYSTAL.
 */
public final class GeodeCell {

    // ── Layer enumeration ─────────────────────────────────────────────────

    public enum Layer {
        /** Voxel is outside all geodes entirely. */
        OUTSIDE,
        /** Outer hard shell — rendered as smooth calcite or basalt. */
        SHELL,
        /** Interior air pocket — empty space inside the geode. */
        HOLLOW,
        /** Gem crystal growing inward from the shell wall. */
        CRYSTAL
    }

    // ── Singleton for the common case ─────────────────────────────────────

    /** Returned for any voxel that is not part of a geode. */
    public static final GeodeCell OUTSIDE = new GeodeCell(Layer.OUTSIDE, null, 0f);

    // ── Fields ────────────────────────────────────────────────────────────

    /** Which structural layer of the geode this voxel occupies. */
    public final Layer layer;

    /**
     * The gem type for CRYSTAL voxels; {@code null} for all other layers.
     * Downstream systems use this to choose block type, colour, and loot.
     */
    public final GemType gemType;

    /**
     * Normalised depth of this voxel [0, 1] — matches the convention from
     * {@link CaveCell#depth}.  Useful for tinting / atmosphere.
     */
    public final float depth;

    // ── Constructor ───────────────────────────────────────────────────────

    public GeodeCell(Layer layer, GemType gemType, float depth) {
        this.layer   = layer;
        this.gemType = gemType;
        this.depth   = depth;
    }

    // ── Convenience predicates ────────────────────────────────────────────

    /** True if this voxel should be solid (shell or crystal). */
    public boolean isSolid() {
        return layer == Layer.SHELL || layer == Layer.CRYSTAL;
    }

    /** True if this voxel is open air (inside the geode hollow). */
    public boolean isHollow() {
        return layer == Layer.HOLLOW;
    }

    /** True if this voxel carries a gem crystal. */
    public boolean isCrystal() {
        return layer == Layer.CRYSTAL;
    }

    @Override
    public String toString() {
        return String.format("GeodeCell[layer=%s, gem=%s, depth=%.2f]",
                layer, gemType, depth);
    }
}
