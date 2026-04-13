package minicraft.ship;

import minicraft.world.Block;
import static minicraft.ship.ThrusterMount.ThrustAxis.*;

/**
 * StalwartSchematic — voxel blueprint for the Stalwart-class Light Frigate.
 */
public final class StalwartSchematic {

    private StalwartSchematic() {}

    public static ShipSchematic build() {
        ShipSchematic.Builder b = new ShipSchematic.Builder();

        int hullHalfW = 11;   
        int hullLen   = 60;   
        int hullH     = 8;    
        int deckY     = hullH; 

        for (int z = 5; z < hullLen; z++) {
            b.block(0, 0, z, Block.OBSIDIAN);
            b.block(1, 0, z, Block.OBSIDIAN);
        }

        for (int z = 0; z < hullLen; z++) {
            int halfW = taperWidth(z, hullLen, hullHalfW);

            for (int x = -halfW; x <= halfW; x++) {
                for (int y = 0; y <= hullH; y++) {
                    boolean isEdge = (x == -halfW || x == halfW || y == 0 || y == hullH);

                    boolean isBevelCorner = (Math.abs(x) == halfW && (y == 0 || y == hullH));
                    if (isBevelCorner && halfW > 3) continue;

                    if (isEdge) {
                        b.block(x, y, z, Block.ALLOY_PLATE);
                    } else if (isInnerFrame(x, y, halfW, hullH)) {
                        b.block(x, y, z, Block.STONE_BRICKS);
                    }
                }
            }
        }

        for (int z = 8; z < hullLen - 8; z++) {
            int halfW = taperWidth(z, hullLen, hullHalfW) - 2;
            for (int x = -halfW; x <= halfW; x++) {
                b.block(x, hullH + 1, z, Block.ALLOY_PLATE);
            }
        }

        for (int z = 5; z < hullLen - 10; z += 3) {
            int halfW = taperWidth(z, hullLen, hullHalfW);
            b.block(halfW,     3, z, Block.GLASS);
            b.block(halfW,     4, z, Block.GLASS);
            b.block(halfW,     5, z, Block.GLASS);
            b.block(-halfW,    3, z, Block.GLASS);
            b.block(-halfW,    4, z, Block.GLASS);
            b.block(-halfW,    5, z, Block.GLASS);
        }

        int bridgeW = 6;
        int bridgeH = 6;
        b.shell(-bridgeW, hullH, 0, bridgeW, hullH + bridgeH, 14, Block.ALLOY_PLATE);

        for (int x = -bridgeW + 1; x <= bridgeW - 1; x++) {
            b.block(x, hullH + 2, 0, Block.GLASS);
            b.block(x, hullH + 3, 0, Block.GLASS);
            b.block(x, hullH + 4, 0, Block.GLASS);
        }

        b.fill(-bridgeW + 1, hullH + 1, 1, bridgeW - 1, hullH + 1, 13, Block.STONE_BRICKS);
        b.block(0, hullH + 1, 6, Block.TRANSMAT_PAD);  
        b.block(0, hullH + 1, 3, Block.SHIP_CONSOLE);  

        for (int x = -6; x <= 6; x++) {
            b.block(x, 1, 25, Block.ALLOY_PLATE); 
            b.block(x, 1, 45, Block.ALLOY_PLATE); 
        }
        for (int z = 25; z <= 45; z++) {
            b.block(-6, 1, z, Block.ALLOY_PLATE); 
            b.block( 6, 1, z, Block.ALLOY_PLATE); 
        }

        buildNacelle(b, -hullHalfW - 2, 1, hullLen - 18, 5, 6);  
        buildNacelle(b,  hullHalfW - 3, 1, hullLen - 18, 5, 6);  

        float mainForce = 8_000_000f; 
        b.thruster( hullHalfW - 3, 3, hullLen, 0, 0,  1, mainForce, FORWARD);
        b.thruster(-hullHalfW - 2, 3, hullLen, 0, 0,  1, mainForce, FORWARD);

        float reverseForce = 2_000_000f;
        b.thruster( 4, 3, 0, 0, 0, -1, reverseForce, BACKWARD);
        b.thruster(-4, 3, 0, 0, 0, -1, reverseForce, BACKWARD);

        float lateralForce = 1_500_000f;
        b.thruster( hullHalfW, 4, 30,  1, 0, 0, lateralForce, RIGHT);
        b.thruster(-hullHalfW, 4, 30, -1, 0, 0, lateralForce, LEFT);

        float verticalForce = 3_000_000f;
        b.thruster(0, 0,     30, 0, -1, 0, verticalForce, DOWN);
        b.thruster(0, hullH, 30, 0,  1, 0, verticalForce, UP);

        b.bridge(0, hullH + 2, 6); 

        return b.build();
    }

    private static int taperWidth(int z, int hullLen, int maxHalfW) {
        if (z < 10) {
            return Math.max(2, (int)(maxHalfW * (z / 10.0)));
        } else if (z > hullLen - 10) {
            float t = (hullLen - z) / 10.0f;
            return Math.max(4, (int)(maxHalfW * (0.7f + 0.3f * t)));
        }
        return maxHalfW;
    }

    private static boolean isInnerFrame(int x, int y, int halfW, int hullH) {
        boolean xRib = (Math.abs(x) == halfW - 2);
        boolean yRib = (y == 2 || y == hullH - 2);
        return xRib || yRib;
    }

    private static void buildNacelle(ShipSchematic.Builder b,
                                     int ox, int oy, int oz,
                                     int w, int h) {
        int len = 16;
        for (int z = oz; z < oz + len; z++) {
            for (int x = ox; x < ox + w; x++) {
                for (int y = oy; y < oy + h; y++) {
                    boolean edge = (x == ox || x == ox + w - 1 ||
                                    y == oy || y == oy + h - 1 ||
                                    z == oz || z == oz + len - 1);
                    if (edge) b.block(x, y, z, Block.ALLOY_PLATE);
                }
            }
        }
        for (int x = ox + 1; x < ox + w - 1; x++) {
            for (int y = oy + 1; y < oy + h - 1; y++) {
                b.block(x, y, oz + len - 1, Block.GLASS);
            }
        }
    }
}
