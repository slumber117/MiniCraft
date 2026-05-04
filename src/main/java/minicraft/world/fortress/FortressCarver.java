package minicraft.world.fortress;

import minicraft.world.IWeatherWorld;

import java.util.LinkedHashMap;
import java.util.Map;


public class FortressCarver {

    // ── Placement tuning ──────────────────────────────────────────────────

    /**
     * Side length (blocks) of each grid cell.
     * Controls minimum fortress spacing.  Max footprint is ~80 blocks so
     * 256 gives comfortable breathing room between fortresses.
     */
    private static final int GRID_CELL_SIZE = 256;

    /**
     * Probability [0, 1] that a given grid cell contains a fortress.
     * At 0.06, roughly 1 in 17 cells has a fortress — about one every
     * 256–512 blocks in open terrain.
     */
    private static final double FORTRESS_CHANCE = 0.06;

    /**
     * How many grid cells away from the query position to search for
     * fortress centres.  Ceil( maxFortressRadius / GRID_CELL_SIZE ) + 1.
     * With a max footprint of ~80 blocks this is always 1.
     */
    private static final int SEARCH_RADIUS_CELLS = 1;

    // ── LRU cache ─────────────────────────────────────────────────────────

    private static final int CACHE_SIZE = 16;

    @SuppressWarnings("serial")
    private final Map<Long, FortressLayout> layoutCache =
        new LinkedHashMap<Long, FortressLayout>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, FortressLayout> e) {
                return size() > CACHE_SIZE;
            }
        };

    // ── World seed ────────────────────────────────────────────────────────

    private final long worldSeed;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param worldSeed  Must match the world generator seed so fortresses
     *                   appear at the same positions regardless of query order.
     */
    public FortressCarver(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Classifies a single voxel's fortress state.
     *
     * @param x       World X
     * @param y       World Y
     * @param z       World Z
     * @param world   World reference — used to look up surface height at the
     *                fortress centre for its anchor Y.
     * @return {@link FortressCell} describing the voxel's structural role.
     *         Returns {@link FortressCell#OUTSIDE} if not part of any fortress.
     */
    public FortressCell query(int x, int y, int z, IWeatherWorld world) {
        int cellX = Math.floorDiv(x, GRID_CELL_SIZE);
        int cellZ = Math.floorDiv(z, GRID_CELL_SIZE);

        FortressCell best = FortressCell.OUTSIDE;

        for (int dcx = -SEARCH_RADIUS_CELLS; dcx <= SEARCH_RADIUS_CELLS; dcx++) {
            for (int dcz = -SEARCH_RADIUS_CELLS; dcz <= SEARCH_RADIUS_CELLS; dcz++) {
                int gcx = cellX + dcx;
                int gcz = cellZ + dcz;

                FortressLayout layout = getLayout(gcx, gcz, world);
                if (layout == null) continue;

                FortressCell cell = classifyVoxel(x, y, z, layout);
                if (cell.layer != FortressCell.Layer.OUTSIDE) {
                    // Prefer more interior classifications (INTERIOR > WALL > FLOOR > CEILING)
                    if (best.layer == FortressCell.Layer.OUTSIDE
                            || layerPriority(cell.layer) > layerPriority(best.layer)) {
                        best = cell;
                    }
                }
            }
        }

        return best;
    }

    // ── Layout lookup ─────────────────────────────────────────────────────

    /**
     * Returns the FortressLayout for a grid cell, generating and caching it
     * on first access.  Returns null if this cell has no fortress.
     */
    private FortressLayout getLayout(int gcx, int gcz, IWeatherWorld world) {
        long key = packCell(gcx, gcz);
        if (layoutCache.containsKey(key)) {
            return layoutCache.get(key); // may be null (cached absence)
        }

        FortressLayout layout = generateLayout(gcx, gcz, world);
        layoutCache.put(key, layout); // null is stored for cells with no fortress
        return layout;
    }

    /**
     * Deterministically decides whether this grid cell has a fortress,
     * and if so generates and returns its layout.  Returns null otherwise.
     */
    private FortressLayout generateLayout(int gcx, int gcz, IWeatherWorld world) {
        long cellSeed = cellHash(gcx, gcz);
        java.util.Random rng = new java.util.Random(cellSeed);

        if (rng.nextDouble() >= FORTRESS_CHANCE) return null;

        // Place the centre inside the grid cell (not at the exact grid corner)
        int margin = GRID_CELL_SIZE / 6; // stay away from cell edges
        int cx = gcx * GRID_CELL_SIZE + margin + rng.nextInt(GRID_CELL_SIZE - margin * 2);
        int cz = gcz * GRID_CELL_SIZE + margin + rng.nextInt(GRID_CELL_SIZE - margin * 2);

        // Surface height at the centre determines floor Y
        int anchorY = world.getSurfaceY(cx, cz);

        // Per-fortress layout seed — different from the cell placement seed
        long layoutSeed = cellSeed ^ 0xFEEDFACEDEADBEEFL;

        System.out.printf("[FortressCarver] Generating fortress at (%d, %d, %d)%n",
                cx, anchorY, cz);

        return new FortressLayout(cx, anchorY, cz, layoutSeed);
    }

    // ── Voxel classification ──────────────────────────────────────────────

    /**
     * Given a confirmed layout, determines what layer (if any) voxel
     * (x, y, z) falls into.
     *
     * Classification priority:
     *   INTERIOR > WALL > FLOOR/CEILING > OUTSIDE
     *
     * INTERIOR uses a union of all cells' inner footprints — this is what
     * carves doorways automatically where corridors meet rooms.
     */
    private FortressCell classifyVoxel(int x, int y, int z, FortressLayout layout) {

        // ── Quick Y reject ─────────────────────────────────────────────────
        if (!layout.isInYRange(y)) return FortressCell.OUTSIDE;

        int anchorY    = layout.anchorY;
        int ceilingY   = anchorY + FortressLayout.WALL_HEIGHT;
        boolean isFloor   = (y == anchorY);
        boolean isCeiling = (y == ceilingY);
        boolean isMid     = (!isFloor && !isCeiling); // interior Y band

        // For mid-Y: precompute whether (x,z) is inside ANY cell's inner footprint.
        // This is the doorway union rule.
        boolean insideAnyInner = false;
        FortressRoomType innerRoomType = null;

        // Track whether (x,z) is inside any outer footprint (for floor/ceiling/wall)
        boolean insideAnyOuter = false;
        FortressRoomType outerRoomType = null;

        for (FortressLayout.Cell c : layout.getCells()) {
            if (c.containsOuter(x, z)) {
                insideAnyOuter = true;
                if (outerRoomType == null
                        || roomTypePriority(c.type) > roomTypePriority(outerRoomType)) {
                    outerRoomType = c.type;
                }

                if (c.containsInner(x, z)) {
                    insideAnyInner = true;
                    if (innerRoomType == null
                            || roomTypePriority(c.type) > roomTypePriority(innerRoomType)) {
                        innerRoomType = c.type;
                    }
                }
            }
        }

        if (!insideAnyOuter) return FortressCell.OUTSIDE;

        // ── Floor ──────────────────────────────────────────────────────────
        if (isFloor) {
            // Floor is solid — return FLOOR with room type for decorators
            FortressRoomType rt = insideAnyInner ? innerRoomType : outerRoomType;
            return new FortressCell(FortressCell.Layer.FLOOR, rt);
        }

        // ── Ceiling ────────────────────────────────────────────────────────
        if (isCeiling) {
            return new FortressCell(FortressCell.Layer.CEILING, null);
        }

        // ── Mid Y-band (interior vs wall) ──────────────────────────────────
        if (insideAnyInner) {
            // Open air — use the room type of the inner cell for content tagging
            return new FortressCell(FortressCell.Layer.INTERIOR, innerRoomType);
        } else {
            // In the wall band of some cell but not in any inner footprint
            return new FortressCell(FortressCell.Layer.WALL, null);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Returns a numeric priority for layer ordering — higher wins when two
     * fortresses (edge case) both claim the same voxel.
     */
    private static int layerPriority(FortressCell.Layer l) {
        switch (l) {
            case INTERIOR: return 4;
            case FLOOR:    return 3;
            case CEILING:  return 2;
            case WALL:     return 1;
            default:       return 0;
        }
    }

    /**
     * Room type priority for resolving which room a shared voxel belongs to.
     * CHEST_ROOM > ENTRANCE > GUARD_ROOM > CHAMBER > CORRIDOR.
     */
    private static int roomTypePriority(FortressRoomType t) {
        switch (t) {
            case CHEST_ROOM:  return 5;
            case ENTRANCE:    return 4;
            case GUARD_ROOM:  return 3;
            case CHAMBER:     return 2;
            case CORRIDOR:    return 1;
            default:          return 0;
        }
    }

    /** Packs two grid-cell ints into one long for use as a map key. */
    private static long packCell(int gcx, int gcz) {
        return ((long) gcx << 32) | (gcz & 0xFFFFFFFFL);
    }

    /**
     * Deterministic hash of grid cell coordinates — same mixing as GeodeCarver
     * to avoid any correlation between the two placement systems.
     */
    private long cellHash(int gcx, int gcz) {
        long h = worldSeed;
        h ^= (long) gcx * 0xB7E151628AED2A6BL;
        h ^= (long) gcz * 0x6C62272E07BB0142L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }
}