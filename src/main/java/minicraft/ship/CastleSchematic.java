package minicraft.ship;

import minicraft.world.Block;
import static minicraft.ship.ThrusterMount.ThrustAxis.*;

/**
 * CastleSchematic — voxel blueprint for the Castle-class Super Heavy Frigate.
 *
 * The Castle-class is a UNSC super heavy frigate in the tradition of the
 * Pillar of Autumn and Forward Unto Dawn — massive, imposing warships built
 * for prolonged deep-space engagement rather than speed or agility.
 *
 * ── Visual design language ────────────────────────────────────────────────
 *
 *   UNSC capital ships share a distinctive industrial brutalist aesthetic:
 *
 *   1. SLAB HULL — the primary hull is a thick, almost rectangular cross-
 *      section. No graceful curves. This is a warship, not a yacht. The
 *      sides are near-vertical walls of ALLOY_PLATE.
 *
 *   2. LAYERED DORSAL SUPERSTRUCTURE — multiple deck levels stacked on the
 *      upper hull like a city built upward. The bridge is not at the bow
 *      but recessed mid-ship on the highest deck, protected by armour
 *      overhangs fore and aft. This is the most distinctive UNSC capital
 *      ship feature.
 *
 *   3. PRONOUNCED VENTRAL KEEL — the underside drops lower than the hull
 *      walls, forming a deep keel ridge running the full length. OBSIDIAN
 *      armour plate on the centreline. Sensor arrays and weapon hardpoints
 *      hang from it.
 *
 *   4. WIDE FLAT BOW — the prow is blunt and wide, not a sharp needle.
 *      It tapers slightly but never to a point — the MAC gun barrel
 *      (represented by OBSIDIAN blocks) protrudes from the centreline bow.
 *
 *   5. QUAD ENGINE CLUSTER — four massive engine nacelles at the stern,
 *      arranged in a 2×2 square pattern, each one larger than the entire
 *      Stalwart-class hull cross-section. GLASS exhaust faces glow.
 *
 *   6. ARMOUR BLISTERS — raised rectangular ALLOY_PLATE pods on the hull
 *      sides, spaced evenly. These are missile batteries and point-defence
 *      hardpoints — visual weight without piercing the hull.
 *
 *   7. RECESSED HANGAR BAYS — two large hangar openings on each flank,
 *      mid-ship, large enough for Pelican dropships. Framed by STONE_BRICKS
 *      with ALLOY_PLATE blast doors partially open.
 *
 *   8. COMM TOWER — a tall, thin vertical mast rising from the dorsal
 *      superstructure, STONE_BRICKS with GLASS sensor nodes.
 *
 * ── Dimensions ────────────────────────────────────────────────────────────
 *
 *   Length:  120 blocks  (Z = 0 bow → Z = 119 stern)
 *   Width:    40 blocks  (X = -20 to +20, port to starboard)
 *   Height:   28 blocks  (Y = 0 keel to Y = 27 comm mast)
 *   Hull H:   16 blocks  (primary hull Y = 0 to Y = 15)
 *
 *   Approximate block count: ~8,500 (hollow, with internal frames)
 *
 * ── Coordinate system ─────────────────────────────────────────────────────
 *
 *   Origin (0,0,0) = centreline, keel level, at the bow tip.
 *   +Z = aft (toward engines)
 *   +X = starboard
 *   +Y = up (dorsal)
 *
 *   Bridge is at Y=22, Z=35 (recessed mid-forward on the superstructure).
 */
public final class CastleSchematic {

    private CastleSchematic() {}

    // ── Dimensions ────────────────────────────────────────────────────────

    private static final int HULL_LEN    = 120;   // bow to stern
    private static final int HULL_HALF_W = 20;    // half-width
    private static final int HULL_H      = 16;    // primary hull height
    private static final int KEEL_DROP   = 3;     // how far keel hangs below Y=0

    public static ShipSchematic build() {
        ShipSchematic.Builder b = new ShipSchematic.Builder();

        buildKeel(b);
        buildMacGun(b);
        buildPrimaryHull(b);
        buildDorsalSuperstructure(b);
        buildArmourBlisters(b);
        buildHangarBays(b);
        buildEngineCluster(b);
        buildCommTower(b);
        buildBridge(b);
        buildThrusters(b);

        b.bridge(0, HULL_H + 8, 35);
        return b.build();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  KEEL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Deep OBSIDIAN keel running the full hull length.
     * Drops 3 blocks below the hull floor — makes the ship look heavy and
     * grounded even when floating. Sensor blisters hang off it every 15 blocks.
     */
    private static void buildKeel(ShipSchematic.Builder b) {
        for (int z = 5; z < HULL_LEN; z++) {
            // Central keel spine — 3 wide, 3 deep below hull
            for (int kd = 0; kd <= KEEL_DROP; kd++) {
                b.block(-1, -kd, z, Block.OBSIDIAN);
                b.block( 0, -kd, z, Block.OBSIDIAN);
                b.block( 1, -kd, z, Block.OBSIDIAN);
            }
        }

        // Sensor blisters hanging from keel every 15 blocks
        for (int z = 20; z < HULL_LEN - 20; z += 15) {
            for (int x = -2; x <= 2; x++) {
                b.block(x, -KEEL_DROP - 1, z,     Block.STONE_BRICKS);
                b.block(x, -KEEL_DROP - 1, z + 1, Block.STONE_BRICKS);
                b.block(x, -KEEL_DROP - 2, z,     Block.ALLOY_PLATE);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MAC GUN
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Magnetic Accelerator Cannon barrel protruding from the bow centreline.
     * The MAC is the defining weapon of UNSC capital ships — a ship-length
     * electromagnetic rail that fires 600-tonne slugs. Here it's represented
     * as a 14-block OBSIDIAN barrel with a STONE_BRICKS housing at the base.
     *
     * The barrel protrudes 10 blocks ahead of the bow hull (Z < 0) and runs
     * back through the ship at centreline.
     */
    private static void buildMacGun(ShipSchematic.Builder b) {
        // Barrel protruding forward (negative Z = ahead of bow)
        for (int z = -10; z < 8; z++) {
            b.block(-1, HULL_H / 2,     z, Block.OBSIDIAN);
            b.block( 0, HULL_H / 2,     z, Block.OBSIDIAN);
            b.block( 1, HULL_H / 2,     z, Block.OBSIDIAN);
            b.block(-1, HULL_H / 2 + 1, z, Block.OBSIDIAN);
            b.block( 0, HULL_H / 2 + 1, z, Block.OBSIDIAN);
            b.block( 1, HULL_H / 2 + 1, z, Block.OBSIDIAN);
        }

        // MAC housing / reinforcement block where it enters the hull
        for (int x = -3; x <= 3; x++) {
            for (int y = HULL_H / 2 - 1; y <= HULL_H / 2 + 2; y++) {
                b.block(x, y, 0, Block.STONE_BRICKS);
                b.block(x, y, 1, Block.STONE_BRICKS);
                b.block(x, y, 2, Block.STONE_BRICKS);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PRIMARY HULL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * The main body of the ship.
     *
     * Cross-section: near-rectangular with slight bevel at the four edges.
     * The bow (Z < 15) widens from a blunt prow — not a needle, more like
     * the flat face of a hammer. The stern (Z > 100) is a flat wall where
     * the engine cluster mounts flush.
     *
     * Hull skin:    ALLOY_PLATE (2-block-thick outer layer)
     * Inner frame:  STONE_BRICKS structural ribs every 12 blocks along Z,
     *               plus continuous top and bottom ribs
     * Interior:     hollow
     */
    private static void buildPrimaryHull(ShipSchematic.Builder b) {
        for (int z = 0; z < HULL_LEN; z++) {
            int hw = bowTaper(z);   // half-width at this Z slice

            for (int x = -hw; x <= hw; x++) {
                for (int y = 0; y <= HULL_H; y++) {

                    boolean outerSkin = (x == -hw || x == hw || y == 0 || y == HULL_H);

                    // Bevel extreme corners — skip corner voxels where |x|==hw AND y==0||HULL_H
                    if (Math.abs(x) == hw && (y == 0 || y == HULL_H)) continue;

                    // Structural rib rings every 12 blocks
                    boolean rib = (z % 12 == 0) &&
                                  (Math.abs(x) >= hw - 2 || y <= 1 || y >= HULL_H - 1);

                    if (outerSkin || rib) {
                        // Double-layer the outer skin for extra visual weight
                        b.block(x, y, z, Block.ALLOY_PLATE);

                        // Second skin layer inset by 1 on sides (not top/bottom)
                        if ((x == -hw || x == hw) && y > 0 && y < HULL_H) {
                            int insetX = (x == -hw) ? x + 1 : x - 1;
                            b.block(insetX, y, z, Block.STONE_BRICKS);
                        }
                    }
                }
            }

            // Upper hull plating strip — extra dorsal armour layer
            // Runs from just inside the bow to just before the stern
            if (z >= 8 && z < HULL_LEN - 8) {
                int plateHW = hw - 2;
                for (int x = -plateHW; x <= plateHW; x++) {
                    b.block(x, HULL_H + 1, z, Block.ALLOY_PLATE);
                }
            }

            // Port and starboard observation window strips
            // Three-block-tall GLASS panels, spaced every 4 blocks
            if (z >= 10 && z < HULL_LEN - 15 && z % 4 == 0) {
                for (int wy = HULL_H / 2 - 1; wy <= HULL_H / 2 + 1; wy++) {
                    b.block(-hw, wy, z, Block.GLASS);
                    b.block( hw, wy, z, Block.GLASS);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DORSAL SUPERSTRUCTURE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * The layered command and crew decks stacked on top of the primary hull.
     *
     * Layer 1 (Y = HULL_H+2 to HULL_H+6):   Wide base deck. Runs Z=10..90.
     *                                         Width = 24 blocks.
     * Layer 2 (Y = HULL_H+7 to HULL_H+11):  Narrower mid-deck. Z=15..70.
     *                                         Width = 16 blocks.
     * Layer 3 (Y = HULL_H+12 to HULL_H+14): Command deck housing the bridge.
     *                                         Z=20..50. Width = 10 blocks.
     *
     * Each layer is a shell (hollow). Forward face has GLASS windows.
     * ALLOY_PLATE armour overhangs protrude fore and aft of each level.
     */
    private static void buildDorsalSuperstructure(ShipSchematic.Builder b) {

        // ── Layer 1 — wide base ───────────────────────────────────────────
        int l1Y0 = HULL_H + 2, l1Y1 = HULL_H + 6;
        int l1HW = 12;
        b.shell(-l1HW, l1Y0, 10, l1HW, l1Y1, 90, Block.ALLOY_PLATE);
        // Floor
        b.fill(-l1HW + 1, l1Y0 + 1, 11, l1HW - 1, l1Y0 + 1, 89, Block.STONE_BRICKS);
        // Forward window band
        for (int x = -l1HW + 1; x <= l1HW - 1; x++) {
            b.block(x, l1Y0 + 2, 10, Block.GLASS);
            b.block(x, l1Y0 + 3, 10, Block.GLASS);
        }
        // Armour overhang — forward lip
        for (int x = -l1HW - 2; x <= l1HW + 2; x++) {
            b.block(x, l1Y1, 8,  Block.ALLOY_PLATE);
            b.block(x, l1Y1, 9,  Block.ALLOY_PLATE);
        }
        // Armour overhang — aft lip
        for (int x = -l1HW - 2; x <= l1HW + 2; x++) {
            b.block(x, l1Y1, 90, Block.ALLOY_PLATE);
            b.block(x, l1Y1, 91, Block.ALLOY_PLATE);
        }

        // ── Layer 2 — mid deck ────────────────────────────────────────────
        int l2Y0 = HULL_H + 7, l2Y1 = HULL_H + 11;
        int l2HW = 8;
        b.shell(-l2HW, l2Y0, 15, l2HW, l2Y1, 70, Block.ALLOY_PLATE);
        b.fill(-l2HW + 1, l2Y0 + 1, 16, l2HW - 1, l2Y0 + 1, 69, Block.STONE_BRICKS);
        // Forward windows
        for (int x = -l2HW + 1; x <= l2HW - 1; x++) {
            b.block(x, l2Y0 + 2, 15, Block.GLASS);
            b.block(x, l2Y0 + 3, 15, Block.GLASS);
        }
        // Side windows — periodic
        for (int z = 20; z < 70; z += 5) {
            b.block(-l2HW, l2Y0 + 2, z, Block.GLASS);
            b.block( l2HW, l2Y0 + 2, z, Block.GLASS);
        }

        // ── Layer 3 — command deck ────────────────────────────────────────
        int l3Y0 = HULL_H + 12, l3Y1 = HULL_H + 14;
        int l3HW = 5;
        b.shell(-l3HW, l3Y0, 20, l3HW, l3Y1, 50, Block.ALLOY_PLATE);
        b.fill(-l3HW + 1, l3Y0 + 1, 21, l3HW - 1, l3Y0 + 1, 49, Block.STONE_BRICKS);
        // Full forward glass face — the panoramic command viewport
        for (int x = -l3HW + 1; x <= l3HW - 1; x++) {
            b.block(x, l3Y0 + 1, 20, Block.GLASS);
            b.block(x, l3Y0 + 2, 20, Block.GLASS);
        }
        // Side viewports
        for (int z = 22; z < 50; z += 4) {
            b.block(-l3HW, l3Y0 + 1, z, Block.GLASS);
            b.block( l3HW, l3Y0 + 1, z, Block.GLASS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ARMOUR BLISTERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Raised rectangular weapon/sensor pods mounted on the hull sides.
     * 5 per side, evenly spaced along the mid-ship section.
     * Each blister is 8W × 4H × 6D, standing proud of the hull wall by 2.
     *
     * These are purely structural/visual — they add mass to the ship
     * (ALLOY_PLATE blocks contribute to physics) and break up the flat
     * slab-sides that would otherwise look too plain.
     */
    private static void buildArmourBlisters(ShipSchematic.Builder b) {
        int[] blisterZ = { 20, 35, 55, 72, 88 };
        int hw = HULL_HALF_W;

        for (int bz : blisterZ) {
            // Starboard blister
            buildBlister(b, hw, 3, bz, 1);
            // Port blister (mirror)
            buildBlister(b, -hw, 3, bz, -1);
        }
    }

    private static void buildBlister(ShipSchematic.Builder b,
                                     int hullX, int baseY, int bz, int side) {
        int bW = 3;  // half-depth of blister along Z
        int bH = 4;  // height
        int bD = 2;  // how far it protrudes from hull (X direction)

        for (int dz = -bW; dz <= bW; dz++) {
            for (int dy = 0; dy < bH; dy++) {
                for (int dd = 0; dd <= bD; dd++) {
                    int wx = hullX + side * dd;
                    int wy = baseY + dy;
                    int wz = bz + dz;
                    boolean edge = (dz == -bW || dz == bW || dy == 0 || dy == bH - 1 || dd == bD);
                    if (edge) b.block(wx, wy, wz, Block.ALLOY_PLATE);
                }
            }
        }
        // GLASS sensor strip on outward face
        for (int dz = -bW + 1; dz <= bW - 1; dz++) {
            b.block(hullX + side * bD, baseY + 1, bz + dz, Block.GLASS);
            b.block(hullX + side * bD, baseY + 2, bz + dz, Block.GLASS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HANGAR BAYS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Two hangar bay openings per side — one forward, one aft of mid-ship.
     * Each hangar is a recessed rectangular cut in the hull side:
     *   12 blocks wide (along Z)
     *   8 blocks tall
     *   4 blocks deep into the hull
     *
     * The interior is STONE_BRICKS (landing deck). The opening is framed
     * by ALLOY_PLATE blast door columns. A single TRANSMAT_PAD marks the
     * primary landing zone in the forward starboard hangar.
     *
     * Because the hull is already hollow we simply frame the opening and
     * fill the landing deck — no carving needed.
     */
    private static void buildHangarBays(ShipSchematic.Builder b) {
        // Forward hangars: Z = 30..44
        buildHangar(b,  HULL_HALF_W, 1, 30, false);  // starboard forward
        buildHangar(b, -HULL_HALF_W, 1, 30, true);   // port forward

        // Aft hangars: Z = 65..79
        buildHangar(b,  HULL_HALF_W, 1, 65, false);  // starboard aft
        buildHangar(b, -HULL_HALF_W, 1, 65, true);   // port aft
    }

    private static void buildHangar(ShipSchematic.Builder b,
                                    int wallX, int floorY, int startZ, boolean isPort) {
        int depth  = 4;   // blocks into hull
        int height = 8;
        int length = 14;  // along Z
        int side   = isPort ? 1 : -1;  // direction into hull from wall

        // Landing deck
        for (int z = startZ; z < startZ + length; z++) {
            for (int d = 0; d <= depth; d++) {
                b.block(wallX + side * d, floorY, z, Block.STONE_BRICKS);
            }
        }

        // Frame columns at opening
        for (int y = floorY; y <= floorY + height; y++) {
            b.block(wallX, y, startZ,           Block.ALLOY_PLATE);
            b.block(wallX, y, startZ + length,  Block.ALLOY_PLATE);
        }

        // Lintel (top of opening)
        for (int z = startZ; z <= startZ + length; z++) {
            b.block(wallX, floorY + height, z, Block.ALLOY_PLATE);
        }

        // TRANSMAT_PAD in first starboard forward hangar only
        if (!isPort && startZ == 30) {
            b.block(wallX + side * 2, floorY + 1, startZ + 6, Block.TRANSMAT_PAD);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ENGINE CLUSTER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Four massive engine nacelles in a 2×2 arrangement at the stern.
     *
     * Arrangement (viewed from stern, looking forward):
     *
     *   [Port-Upper]    [Starboard-Upper]
     *   [Port-Lower]    [Starboard-Lower]
     *
     * Each nacelle is 12W × 10H × 20D.
     * The exhaust face (aft-most Z face) is GLASS — represents the engine glow.
     * The housing is ALLOY_PLATE with OBSIDIAN reinforcement rings every 5 blocks.
     *
     * The four nacelles together form a 2×2 square exhaust pattern that is
     * immediately recognisable as UNSC capital ship propulsion.
     */
    private static void buildEngineCluster(ShipSchematic.Builder b) {
        int nW  = 6;    // nacelle half-width (X)
        int nH  = 8;    // nacelle height
        int nD  = 20;   // nacelle depth (Z)
        int sternZ = HULL_LEN;

        // Nacelle centre positions (X, Y)
        int[][] centres = {
            { -10, HULL_H / 2 + 2  },   // port-upper
            {  10, HULL_H / 2 + 2  },   // starboard-upper
            { -10, HULL_H / 2 - 6  },   // port-lower
            {  10, HULL_H / 2 - 6  }    // starboard-lower
        };

        for (int[] c : centres) {
            int cx = c[0];
            int cy = c[1];
            buildNacelle(b, cx, cy, sternZ, nW, nH, nD);
        }

        // Central engine block — the fifth "core" thruster between the four
        // nacelles, slightly recessed. Characteristic of Autumn-class designs.
        int coreZ = sternZ + 4;
        for (int x = -4; x <= 4; x++) {
            for (int y = HULL_H / 2 - 3; y <= HULL_H / 2 + 3; y++) {
                boolean edge = (Math.abs(x) == 4 || y == HULL_H / 2 - 3 || y == HULL_H / 2 + 3);
                if (edge) b.block(x, y, coreZ,     Block.ALLOY_PLATE);
                else      b.block(x, y, coreZ,     Block.GLASS);         // core glow
            }
        }
    }

    private static void buildNacelle(ShipSchematic.Builder b,
                                     int cx, int cy, int sternZ,
                                     int halfW, int h, int depth) {
        for (int z = sternZ; z < sternZ + depth; z++) {
            for (int x = cx - halfW; x <= cx + halfW; x++) {
                for (int y = cy; y <= cy + h; y++) {
                    boolean edge = (x == cx - halfW || x == cx + halfW
                                 || y == cy         || y == cy + h);
                    boolean exhaustFace = (z == sternZ + depth - 1);
                    boolean ring = ((z - sternZ) % 5 == 0);

                    // Skip bevel corners
                    if ((x == cx - halfW || x == cx + halfW) && (y == cy || y == cy + h)) continue;

                    if (exhaustFace) {
                        // Exhaust glow — GLASS interior, ALLOY_PLATE rim
                        if (edge) b.block(x, y, z, Block.ALLOY_PLATE);
                        else      b.block(x, y, z, Block.GLASS);
                    } else if (ring) {
                        // OBSIDIAN reinforcement rings at every 5 blocks
                        if (edge) b.block(x, y, z, Block.OBSIDIAN);
                    } else {
                        if (edge) b.block(x, y, z, Block.ALLOY_PLATE);
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  COMM TOWER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vertical sensor and communications mast rising from the top of the
     * dorsal superstructure. STONE_BRICKS shaft with GLASS sensor nodes
     * at regular intervals and a wider ALLOY_PLATE platform at the base.
     *
     * Height: 10 blocks above the command deck.
     * Position: centreline, above the mid-point of the command deck (Z=35).
     */
    private static void buildCommTower(ShipSchematic.Builder b) {
        int baseY = HULL_H + 15;   // sits on top of command deck
        int towerZ = 35;

        // Base platform
        for (int x = -3; x <= 3; x++) {
            for (int z = towerZ - 2; z <= towerZ + 2; z++) {
                b.block(x, baseY, z, Block.ALLOY_PLATE);
            }
        }

        // Shaft — 2×2 STONE_BRICKS column
        for (int y = baseY + 1; y <= baseY + 9; y++) {
            b.block(-1, y, towerZ,     Block.STONE_BRICKS);
            b.block( 0, y, towerZ,     Block.STONE_BRICKS);
            b.block(-1, y, towerZ + 1, Block.STONE_BRICKS);
            b.block( 0, y, towerZ + 1, Block.STONE_BRICKS);
        }

        // GLASS sensor nodes at Y+3, Y+6, Y+9
        for (int offset : new int[]{3, 6, 9}) {
            int ny = baseY + offset;
            // Cross-shaped node
            b.block(-2, ny, towerZ,     Block.GLASS);
            b.block( 1, ny, towerZ,     Block.GLASS);
            b.block(-1, ny, towerZ - 1, Block.GLASS);
            b.block( 0, ny, towerZ + 2, Block.GLASS);
        }

        // Tip — OBSIDIAN cap (transponder/MAC targeting array)
        b.block(-1, baseY + 10, towerZ,     Block.OBSIDIAN);
        b.block( 0, baseY + 10, towerZ,     Block.OBSIDIAN);
        b.block(-1, baseY + 10, towerZ + 1, Block.OBSIDIAN);
        b.block( 0, baseY + 10, towerZ + 1, Block.OBSIDIAN);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BRIDGE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * The bridge interior — recessed inside the command deck (layer 3 of
     * the dorsal superstructure). Contains the SHIP_CONSOLE helm and the
     * TRANSMAT_PAD player entry point.
     *
     * The bridge sits at Z=22..48, Y=HULL_H+13..HULL_H+14 (command deck interior).
     */
    private static void buildBridge(ShipSchematic.Builder b) {
        int bridgeY = HULL_H + 13;
        int bZ0 = 22, bZ1 = 48;

        // Helm console — forward, facing the bow
        b.block(0, bridgeY, bZ0 + 4, Block.SHIP_CONSOLE);

        // TRANSMAT_PAD — player entry point (bridge centre)
        b.block(0, bridgeY, 35, Block.TRANSMAT_PAD);

        // Secondary consoles port and starboard of helm
        b.block(-3, bridgeY, bZ0 + 4, Block.CRAFTING_TABLE);
        b.block( 3, bridgeY, bZ0 + 4, Block.CRAFTING_TABLE);

        // Crew stations along aft bridge wall
        for (int x = -4; x <= 4; x += 2) {
            b.block(x, bridgeY, bZ1 - 4, Block.STONE_BRICKS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  THRUSTERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Castle-class thruster layout.
     *
     * Four main engines (FORWARD):   matched to the four nacelle positions,
     *                                 28 MN each (112 MN total forward thrust).
     * Two reverse ports (BACKWARD):  bow-mounted, 8 MN each.
     * Four lateral thrusters:        2 port, 2 starboard at bow and stern.
     * Two vertical thrusters:        keel (DOWN) and dorsal (UP), mid-ship.
     *
     * Total forward thrust: 112 MN on a 30M kg ship = 3.73 m/s² max accel.
     * At SUPER_HEAVY_FRIGATE drag (0.44), terminal velocity is ~120 blocks/s.
     * Feels like a mountain that eventually moves very fast.
     */
    private static void buildThrusters(ShipSchematic.Builder b) {
        float mainForce    = 28_000_000f;  // 28 MN per main nacelle
        float reverseForce =  8_000_000f;
        float lateralForce =  5_000_000f;
        float vertForce    = 12_000_000f;

        int sternZ = HULL_LEN;
        int midZ   = HULL_LEN / 2;

        // Four main engines — matched to nacelle centres
        b.thruster(-10, HULL_H / 2 + 2, sternZ,  0, 0, 1, mainForce, FORWARD);
        b.thruster( 10, HULL_H / 2 + 2, sternZ,  0, 0, 1, mainForce, FORWARD);
        b.thruster(-10, HULL_H / 2 - 6, sternZ,  0, 0, 1, mainForce, FORWARD);
        b.thruster( 10, HULL_H / 2 - 6, sternZ,  0, 0, 1, mainForce, FORWARD);

        // Central core engine
        b.thruster(0, HULL_H / 2, sternZ + 4, 0, 0, 1, mainForce * 0.5f, FORWARD);

        // Reverse: bow-facing ports
        b.thruster(-6, HULL_H / 2, 0, 0, 0, -1, reverseForce, BACKWARD);
        b.thruster( 6, HULL_H / 2, 0, 0, 0, -1, reverseForce, BACKWARD);

        // Lateral: bow and stern pairs
        b.thruster(-HULL_HALF_W, HULL_H / 2, 20,        -1, 0, 0, lateralForce, LEFT);
        b.thruster( HULL_HALF_W, HULL_H / 2, 20,         1, 0, 0, lateralForce, RIGHT);
        b.thruster(-HULL_HALF_W, HULL_H / 2, sternZ - 20, -1, 0, 0, lateralForce, LEFT);
        b.thruster( HULL_HALF_W, HULL_H / 2, sternZ - 20,  1, 0, 0, lateralForce, RIGHT);

        // Vertical: keel down, dorsal up
        b.thruster(0, -KEEL_DROP, midZ, 0, -1, 0, vertForce, DOWN);
        b.thruster(0, HULL_H + 2, midZ, 0,  1, 0, vertForce, UP);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BOW TAPER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Computes the hull half-width at a given Z position.
     *
     * UNSC capital ships have a blunt, wide bow — not a sharp prow.
     * The hull reaches near-full-width within the first 12 blocks then
     * holds that width for the entire mid-ship section. The stern is
     * perfectly flat (no taper) because the engine cluster mounts flush.
     *
     *   Z =   0 →  8 :  bow  — narrows from 60% to 100% width
     *   Z =   8 → 108 : mid-ship — full width
     *   Z = 108 → 120 : stern — holds full width (flat engine mounting face)
     */
    private static int bowTaper(int z) {
        if (z < 8) {
            // Bow: ramps from half-width to full in 8 blocks
            // Minimum of HULL_HALF_W / 2 at the very tip (blunt, not pointed)
            double t = z / 8.0;
            return (int) Math.round(HULL_HALF_W * (0.5 + 0.5 * t));
        }
        // Mid and stern — always full width
        return HULL_HALF_W;
    }
}
