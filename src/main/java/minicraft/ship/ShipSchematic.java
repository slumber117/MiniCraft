package minicraft.ship;

import minicraft.world.Block;
import org.joml.Vector3i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ShipSchematic — the voxel blueprint for a ship.
 */
public final class ShipSchematic {

    private final Vector3i[] localPositions;
    private final Block[]    blocks;

    public final int blockCount;
    public final Vector3i boundsMin;
    public final Vector3i boundsMax;
    public final Vector3f geometricCentre;
    public final Vector3i bridgeLocalPos;

    private final List<ThrusterMount> thrusterMounts;

    private ShipSchematic(Vector3i[] localPositions, Block[] blocks,
                          Vector3i bridgeLocalPos, List<ThrusterMount> thrusterMounts) {
        this.localPositions = localPositions;
        this.blocks         = blocks;
        this.blockCount     = blocks.length;
        this.bridgeLocalPos = new Vector3i(bridgeLocalPos);
        this.thrusterMounts = Collections.unmodifiableList(new ArrayList<>(thrusterMounts));

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        float sumX = 0, sumY = 0, sumZ = 0;

        for (Vector3i p : localPositions) {
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); maxZ = Math.max(maxZ, p.z);
            sumX += p.x; sumY += p.y; sumZ += p.z;
        }

        this.boundsMin       = new Vector3i(minX, minY, minZ);
        this.boundsMax       = new Vector3i(maxX, maxY, maxZ);
        this.geometricCentre = new Vector3f(
            sumX / blockCount, sumY / blockCount, sumZ / blockCount
        );
    }

    public Vector3i getLocalPosition(int i) { return localPositions[i]; }
    public Block    getBlock(int i)          { return blocks[i]; }
    public List<ThrusterMount> getThrusterMounts() { return thrusterMounts; }

    public int getWidth()  { return boundsMax.x - boundsMin.x + 1; }
    public int getHeight() { return boundsMax.y - boundsMin.y + 1; }
    public int getDepth()  { return boundsMax.z - boundsMin.z + 1; }

    public static final class Builder {

        private final List<Vector3i> positions = new ArrayList<>();
        private final List<Block>    blockList = new ArrayList<>();
        private final List<ThrusterMount> thrusters = new ArrayList<>();
        private Vector3i bridgePos = new Vector3i(0, 1, 0);

        public Builder block(int x, int y, int z, Block block) {
            if (block == null || block.isAir()) return this;
            positions.add(new Vector3i(x, y, z));
            blockList.add(block);
            return this;
        }

        public Builder fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++)
                    for (int z = z0; z <= z1; z++)
                        block(x, y, z, block);
            return this;
        }

        public Builder shell(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++)
                    for (int z = z0; z <= z1; z++) {
                        boolean edge = (x == x0 || x == x1 ||
                                        y == y0 || y == y1 ||
                                        z == z0 || z == z1);
                        if (edge) block(x, y, z, block);
                    }
            return this;
        }

        public Builder thruster(int x, int y, int z,
                                float tx, float ty, float tz,
                                float maxForce,
                                ThrusterMount.ThrustAxis axis) {
            thrusters.add(new ThrusterMount(
                new Vector3f(x, y, z),
                new Vector3f(tx, ty, tz),
                maxForce, axis
            ));
            return this;
        }

        public Builder bridge(int x, int y, int z) {
            this.bridgePos = new Vector3i(x, y, z);
            return this;
        }

        public ShipSchematic build() {
            if (positions.isEmpty()) throw new IllegalStateException("Schematic has no blocks.");
            return new ShipSchematic(
                positions.toArray(new Vector3i[0]),
                blockList.toArray(new Block[0]),
                bridgePos,
                thrusters
            );
        }
    }
}
