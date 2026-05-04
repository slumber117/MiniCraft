package minicraft.world.fortress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * FortressLayout — generates and stores the complete structural layout for
 * one medium-sized surface fortress with a dungeon feel.
 *
 * ── Physical structure ───────────────────────────────────────────────────
 *
 *   The fortress sits on the surface.  All rooms share the same floor Y
 *   (the anchor Y passed at construction — the terrain height at the
 *   fortress centre).  Walls rise WALL_HEIGHT blocks above the floor.
 *
 *   Interior height:  WALL_HEIGHT - 1  (headroom inside rooms/corridors)
 *   Wall thickness:   ROOM_WALL  = 2 blocks for rooms
 *                     CORR_WALL  = 1 block for corridors
 *
 *   A corridor slightly overlaps the rooms it connects so that the shared
 *   wall voxels become INTERIOR — creating natural doorways without any
 *   explicit door list.
 *
 * ── Layout algorithm ────────────────────────────────────────────────────
 *
 *   1. Place the ENTRANCE room (16×16) at the anchor centre.
 *   2. Optionally flank it with two GUARD_ROOMs on E/W.
 *   3. From the entrance, branch in each cardinal direction with 80%
 *      probability, placing a CORRIDOR then a CHAMBER at its end.
 *   4. Mark ~30% of CHAMBERs as CHEST_ROOMs.
 *   5. From primary CHAMBERs, with 50% probability, branch one extra
 *      secondary corridor + a smaller CHAMBER or CHEST_ROOM.
 *
 *   Rooms are recorded as XZ AABBs in world space.  The query in
 *   {@link FortressCarver} classifies each voxel by testing these AABBs.
 *
 * ── Doorways ────────────────────────────────────────────────────────────
 *
 *   The per-voxel classifier in FortressCarver uses a union rule:
 *   any voxel in the INTERIOR Y band that falls inside *any* cell's inner
 *   footprint becomes INTERIOR.  Because corridors are extended by
 *   {@link #DOORWAY_OVERLAP} blocks into the room's outer shell, the
 *   shared wall voxels are automatically carved open as doorways.
 *
 * ── Overlap prevention ───────────────────────────────────────────────────
 *
 *   Before placing each room, the generator checks that its outer footprint
 *   does not overlap (beyond the doorway margin) any already-placed room.
 *   If a collision is detected, that branch is skipped.
 */
public class FortressLayout {

    // ── Geometry constants ────────────────────────────────────────────────

    /** Total wall height in blocks (floor block + interior + ceiling block). */
    public static final int WALL_HEIGHT = 8;

    /** Wall thickness for rooms (blocks). */
    public static final int ROOM_WALL = 2;

    /** Wall thickness for corridors (blocks). */
    public static final int CORR_WALL = 1;

    /**
     * How far a corridor extends INTO a room's outer shell to carve a doorway.
     * Must be <= ROOM_WALL.
     */
    private static final int DOORWAY_OVERLAP = 2;

    // ── Room size ranges ──────────────────────────────────────────────────

    /** Entrance room outer dimensions (square, fixed). */
    private static final int ENTRANCE_SIZE = 16;

    /** Primary chamber outer size range. */
    private static final int CHAMBER_MIN = 9;
    private static final int CHAMBER_MAX = 13;

    /** Secondary chamber outer size range. */
    private static final int SECONDARY_MIN = 7;
    private static final int SECONDARY_MAX = 10;

    /** Guard room outer size. */
    private static final int GUARD_SIZE = 8;

    /** Corridor width (outer). Inner = outer - 2 * CORR_WALL. */
    private static final int CORRIDOR_WIDTH = 5;

    /** Primary corridor length range (not counting overlap at both ends). */
    private static final int CORRIDOR_MIN = 8;
    private static final int CORRIDOR_MAX = 16;

    // ── Data ──────────────────────────────────────────────────────────────

    /**
     * One entry per room or corridor in the layout.
     * Each cell is an XZ AABB in world space plus its room type.
     */
    public static final class Cell {
        /** Outer XZ bounds (inclusive). */
        public final int x1, z1, x2, z2;
        /** Wall thickness for this cell type. */
        public final int wallThick;
        /** Room category. */
        public final FortressRoomType type;

        Cell(int x1, int z1, int x2, int z2,
             int wallThick, FortressRoomType type) {
            this.x1        = x1;
            this.z1        = z1;
            this.x2        = x2;
            this.z2        = z2;
            this.wallThick = wallThick;
            this.type      = type;
        }

        /** Inner footprint (the carved-out or air region). */
        public int innerX1() { return x1 + wallThick; }
        public int innerZ1() { return z1 + wallThick; }
        public int innerX2() { return x2 - wallThick; }
        public int innerZ2() { return z2 - wallThick; }

        /** True if (x, z) is inside the outer footprint. */
        public boolean containsOuter(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }

        /** True if (x, z) is inside the inner footprint. */
        public boolean containsInner(int x, int z) {
            return x >= innerX1() && x <= innerX2()
                && z >= innerZ1() && z <= innerZ2();
        }
    }

    // ── Instance state ────────────────────────────────────────────────────

    /** Centre of this fortress in world space (X, Z). */
    public final int centreX, centreZ;

    /** Floor Y — the Y of the floor block (== terrain height at centre). */
    public final int anchorY;

    /** All cells (rooms + corridors) in world space. */
    private final List<Cell> cells = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Generates the complete fortress layout.
     *
     * @param centreX  World X of the fortress centre
     * @param anchorY  World Y of the floor (surface height at centre)
     * @param centreZ  World Z of the fortress centre
     * @param seed     Per-fortress seed for reproducible layout
     */
    public FortressLayout(int centreX, int anchorY, int centreZ, long seed) {
        this.centreX = centreX;
        this.anchorY = anchorY;
        this.centreZ = centreZ;
        generate(new Random(seed));
    }

    // ── Public accessors ──────────────────────────────────────────────────

    /** Returns all layout cells (rooms and corridors). Read-only. */
    public List<Cell> getCells() {
        return Collections.unmodifiableList(cells);
    }

    /**
     * Quick Y-range test: returns false if y is entirely outside this
     * fortress's vertical extent.  Use as an early-out before XZ checks.
     */
    public boolean isInYRange(int y) {
        return y >= anchorY && y <= anchorY + WALL_HEIGHT;
    }

    // ── Layout generation ─────────────────────────────────────────────────

    private void generate(Random rng) {
        int half = ENTRANCE_SIZE / 2;

        // ── 1. Central entrance room ───────────────────────────────────────
        Cell entrance = addRoom(
                centreX - half, centreZ - half,
                centreX + half, centreZ + half,
                ROOM_WALL, FortressRoomType.ENTRANCE);

        // ── 2. Guard rooms (E and W of entrance, 70% chance each) ─────────
        if (rng.nextDouble() < 0.70) {
            tryAddAdjacentRoom(entrance, Direction.EAST,
                    GUARD_SIZE, GUARD_SIZE, ROOM_WALL,
                    FortressRoomType.GUARD_ROOM, 0, rng);
        }
        if (rng.nextDouble() < 0.70) {
            tryAddAdjacentRoom(entrance, Direction.WEST,
                    GUARD_SIZE, GUARD_SIZE, ROOM_WALL,
                    FortressRoomType.GUARD_ROOM, 0, rng);
        }

        // ── 3. Primary branches (N / S / E / W) ───────────────────────────
        List<Cell> primaryChambers = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (rng.nextDouble() > 0.80) continue; // 80% to attempt each branch

            int corrLen = CORRIDOR_MIN + rng.nextInt(CORRIDOR_MAX - CORRIDOR_MIN + 1);
            Cell corridor = tryAddCorridor(entrance, dir, corrLen, rng);
            if (corridor == null) continue;

            int size = CHAMBER_MIN + rng.nextInt(CHAMBER_MAX - CHAMBER_MIN + 1);
            Cell chamber = tryAddAdjacentRoom(corridor, dir,
                    size, size, ROOM_WALL,
                    FortressRoomType.CHAMBER, 0, rng);
            if (chamber != null) {
                primaryChambers.add(chamber);
            }
        }

        // ── 4. Mark some primary chambers as chest rooms ───────────────────
        // Shuffle so chest rooms are spread, not always the first-placed.
        Collections.shuffle(primaryChambers, rng);
        int chestCount = Math.max(1, (int) Math.round(primaryChambers.size() * 0.30));
        for (int i = 0; i < chestCount && i < primaryChambers.size(); i++) {
            promoteCellToChestRoom(primaryChambers.get(i));
        }

        // ── 5. Secondary branches (from primary chambers) ─────────────────
        List<Cell> allPrimary = new ArrayList<>(primaryChambers);
        for (Cell source : allPrimary) {
            if (rng.nextDouble() > 0.50) continue;

            Direction secDir = Direction.values()[rng.nextInt(4)];
            int secLen = CORRIDOR_MIN + rng.nextInt(4); // shorter secondary corridors
            Cell secCorr = tryAddCorridor(source, secDir, secLen, rng);
            if (secCorr == null) continue;

            int secSize = SECONDARY_MIN + rng.nextInt(SECONDARY_MAX - SECONDARY_MIN + 1);
            FortressRoomType secType = (rng.nextDouble() < 0.40)
                    ? FortressRoomType.CHEST_ROOM
                    : FortressRoomType.CHAMBER;
            tryAddAdjacentRoom(secCorr, secDir, secSize, secSize,
                    ROOM_WALL, secType, 0, rng);
        }
    }

    // ── Builder helpers ───────────────────────────────────────────────────

    /**
     * Attempts to add a corridor extending from one face of {@code source}
     * in {@code dir}.  Returns null if placement fails (overlap).
     *
     * @param source  The room/corridor to extend from
     * @param dir     Cardinal direction
     * @param length  Corridor outer length (not counting doorway overlaps)
     * @param rng     Seeded RNG
     */
    private Cell tryAddCorridor(Cell source, Direction dir, int length, Random rng) {
        int cw = CORRIDOR_WIDTH;

        // Centre the corridor on the face mid-point of the source cell
        int x1, z1, x2, z2;

        switch (dir) {
            case NORTH: {
                int midX  = (source.x1 + source.x2) / 2;
                int halfW = cw / 2;
                x1 = midX - halfW;
                x2 = midX + halfW;
                // Overlap into source by DOORWAY_OVERLAP, then extend
                z2 = source.z1 + DOORWAY_OVERLAP;
                z1 = z2 - length;
                break;
            }
            case SOUTH: {
                int midX  = (source.x1 + source.x2) / 2;
                int halfW = cw / 2;
                x1 = midX - halfW;
                x2 = midX + halfW;
                z1 = source.z2 - DOORWAY_OVERLAP;
                z2 = z1 + length;
                break;
            }
            case EAST: {
                int midZ  = (source.z1 + source.z2) / 2;
                int halfW = cw / 2;
                z1 = midZ - halfW;
                z2 = midZ + halfW;
                x1 = source.x2 - DOORWAY_OVERLAP;
                x2 = x1 + length;
                break;
            }
            case WEST: {
                int midZ  = (source.z1 + source.z2) / 2;
                int halfW = cw / 2;
                z1 = midZ - halfW;
                z2 = midZ + halfW;
                x2 = source.x1 + DOORWAY_OVERLAP;
                x1 = x2 - length;
                break;
            }
            default: return null;
        }

        if (!canPlace(x1, z1, x2, z2, source)) return null;
        return addRoom(x1, z1, x2, z2, CORR_WALL, FortressRoomType.CORRIDOR);
    }

    /**
     * Attempts to add a room immediately past the far end of {@code source}
     * in {@code dir}, with a DOORWAY_OVERLAP so they share a wall cutout.
     */
    private Cell tryAddAdjacentRoom(Cell source, Direction dir,
                                    int sizeX, int sizeZ,
                                    int wallThick, FortressRoomType type,
                                    int offsetAlongFace, Random rng) {
        int x1, z1, x2, z2;

        // Random lateral offset to keep rooms from being perfectly centred
        int jitter = (rng == null) ? 0
                   : (rng.nextInt(3) - 1) * 2; // -2, 0, or +2

        switch (dir) {
            case NORTH: {
                int midX = (source.x1 + source.x2) / 2 + jitter;
                x1 = midX - sizeX / 2;
                x2 = x1 + sizeX;
                z2 = source.z1 + DOORWAY_OVERLAP;
                z1 = z2 - sizeZ;
                break;
            }
            case SOUTH: {
                int midX = (source.x1 + source.x2) / 2 + jitter;
                x1 = midX - sizeX / 2;
                x2 = x1 + sizeX;
                z1 = source.z2 - DOORWAY_OVERLAP;
                z2 = z1 + sizeZ;
                break;
            }
            case EAST: {
                int midZ = (source.z1 + source.z2) / 2 + jitter;
                z1 = midZ - sizeZ / 2;
                z2 = z1 + sizeZ;
                x1 = source.x2 - DOORWAY_OVERLAP;
                x2 = x1 + sizeX;
                break;
            }
            case WEST: {
                int midZ = (source.z1 + source.z2) / 2 + jitter;
                z1 = midZ - sizeZ / 2;
                z2 = z1 + sizeZ;
                x2 = source.x1 + DOORWAY_OVERLAP;
                x1 = x2 - sizeX;
                break;
            }
            default: return null;
        }

        if (!canPlace(x1, z1, x2, z2, source)) return null;
        return addRoom(x1, z1, x2, z2, wallThick, type);
    }

    /**
     * Checks that the AABB (x1,z1)–(x2,z2) does not significantly overlap
     * any already-placed cell (excluding {@code allowedNeighbour} which is
     * the source cell the new room is intentionally connecting to).
     *
     * "Significantly" means an overlap larger than DOORWAY_OVERLAP in both
     * dimensions — a small corridor-to-room overlap is acceptable and
     * intentional.
     */
    private boolean canPlace(int x1, int z1, int x2, int z2, Cell allowedNeighbour) {
        for (Cell c : cells) {
            if (c == allowedNeighbour) continue;

            // Compute overlap extents
            int ox = Math.min(x2, c.x2) - Math.max(x1, c.x1);
            int oz = Math.min(z2, c.z2) - Math.max(z1, c.z1);
            if (ox > DOORWAY_OVERLAP && oz > DOORWAY_OVERLAP) {
                return false; // Unacceptable overlap
            }
        }
        return true;
    }

    /** Adds a cell to the layout and returns it. */
    private Cell addRoom(int x1, int z1, int x2, int z2,
                         int wallThick, FortressRoomType type) {
        Cell c = new Cell(x1, z1, x2, z2, wallThick, type);
        cells.add(c);
        return c;
    }

    /** Replaces a CHAMBER cell with a CHEST_ROOM cell in-place. */
    private void promoteCellToChestRoom(Cell chamber) {
        int idx = cells.indexOf(chamber);
        if (idx < 0) return;
        cells.set(idx, new Cell(
                chamber.x1, chamber.z1, chamber.x2, chamber.z2,
                chamber.wallThick, FortressRoomType.CHEST_ROOM));
    }

    // ── Direction ─────────────────────────────────────────────────────────

    enum Direction { NORTH, SOUTH, EAST, WEST }
}