package minicraft.renderer;

import minicraft.ship.ShipSchematic;
import minicraft.world.Block;
import minicraft.world.Face;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.*;

/**
 * VoxelBaker — optimized geometry assembler for procedural ships.
 * 
 * Bakes a ShipSchematic into a collection of Mesh objects, organized by block texture.
 * Implements internal face culling to ensure high performance for 10k+ block vessels.
 */
public class VoxelBaker {

    public static class BakedShip {
        public final Map<String, Mesh> meshes;
        public final org.joml.Vector3f centreOffset;

        public BakedShip(Map<String, Mesh> meshes, org.joml.Vector3f centreOffset) {
            this.meshes = meshes;
            this.centreOffset = centreOffset;
        }

        public void cleanup() {
            for (Mesh m : meshes.values()) m.cleanup();
        }
    }

    /**
     * Bakes the provided schematic into a renderable collection of meshes.
     */
    public static BakedShip bake(ShipSchematic schematic, TextureRegistry registry) {
        Map<String, List<Float>> positions = new HashMap<>();
        Map<String, List<Float>> uvs = new HashMap<>();
        Map<String, List<Integer>> indices = new HashMap<>();
        Map<String, List<Float>> lightInfo = new HashMap<>();
        Map<String, Integer> vertexCounts = new HashMap<>();

        // 1. Map blocks for fast adjacency lookups during culling
        Map<Vector3i, Block> blockMap = new HashMap<>();
        for (int i = 0; i < schematic.blockCount; i++) {
            blockMap.put(schematic.getLocalPosition(i), schematic.getBlock(i));
        }

        // 2. Iterate and generate faces
        for (int i = 0; i < schematic.blockCount; i++) {
            Vector3i pos = schematic.getLocalPosition(i);
            Block b = schematic.getBlock(i);
            if (b == null || b.isAir()) continue;

            // Check all 6 faces for neighbor occlusion
            checkAndAddFace(b, pos, 0, 1, 0, 0, Face.SIDE,   blockMap, positions, uvs, indices, lightInfo, vertexCounts); // +X
            checkAndAddFace(b, pos, 1, -1, 0, 0, Face.SIDE,  blockMap, positions, uvs, indices, lightInfo, vertexCounts); // -X
            checkAndAddFace(b, pos, 2, 0, 1, 0, Face.TOP,    blockMap, positions, uvs, indices, lightInfo, vertexCounts); // +Y
            checkAndAddFace(b, pos, 3, 0, -1, 0, Face.BOTTOM,blockMap, positions, uvs, indices, lightInfo, vertexCounts); // -Y
            checkAndAddFace(b, pos, 4, 0, 0, 1, Face.SIDE,   blockMap, positions, uvs, indices, lightInfo, vertexCounts); // +Z
            checkAndAddFace(b, pos, 5, 0, 0, -1, Face.SIDE,  blockMap, positions, uvs, indices, lightInfo, vertexCounts); // -Z
        }

        // 3. Create final Mesh objects
        Map<String, Mesh> meshMap = new HashMap<>();
        for (String texKey : positions.keySet()) {
            float[] posArr = toFloatArr(positions.get(texKey));
            float[] uvArr = toFloatArr(uvs.get(texKey));
            float[] litArr = toFloatArr(lightInfo.get(texKey));
            int[] idxArr = toIntArr(indices.get(texKey));

            meshMap.put(texKey, new Mesh(posArr, uvArr, litArr, idxArr, registry.get(texKey)));
        }

        org.joml.Vector3f offset = new org.joml.Vector3f(schematic.geometricCentre);
        return new BakedShip(meshMap, offset);
    }

    private static void checkAndAddFace(Block b, Vector3i localPos, int sideIdx, int dx, int dy, int dz, Face face,
                                        Map<Vector3i, Block> blockMap,
                                        Map<String, List<Float>> positions,
                                        Map<String, List<Float>> uvs,
                                        Map<String, List<Integer>> indices,
                                        Map<String, List<Float>> lightInfo,
                                        Map<String, Integer> vertexCounts) {
        
        Vector3i neighborPos = new Vector3i(localPos.x + dx, localPos.y + dy, localPos.z + dz);
        Block neighbor = blockMap.get(neighborPos);

        // Standard hidden face culling
        if (neighbor != null && neighbor.isOpaque()) return;

        String tex = b.getTextureForFace(face);
        if (tex == null) return;

        if (!positions.containsKey(tex)) {
            positions.put(tex, new ArrayList<>());
            uvs.put(tex, new ArrayList<>());
            indices.put(tex, new ArrayList<>());
            lightInfo.put(tex, new ArrayList<>());
            vertexCounts.put(tex, 0);
        }

        List<Float> posList = positions.get(tex);
        List<Float> uvList = uvs.get(tex);
        List<Integer> idxList = indices.get(tex);
        List<Float> litList = lightInfo.get(tex);
        int base = vertexCounts.get(tex);

        // Float coordinates (centred around 0,0,0)
        float x = localPos.x, y = localPos.y, z = localPos.z;
        float x1 = x + 1, y1 = y + 1, z1 = z + 1;

        // Effect brightness: MAC (Obsidian) and Engines (Glass) glow
        float glow = 0.0f; // Default: fully affected by lighting
        if (b == Block.OBSIDIAN || b == Block.GLASS) glow = 1.0f;

        // Directional Shading: Over-brighten to achieve a vibrant "Light Gray" metallic pop
        float faceBrightness = 1.1f; // High base for industrial gray
        switch (sideIdx) {
            case 2: faceBrightness = 1.4f; break; // Top (+Y) - Over-bright highlight
            case 3: faceBrightness = 0.8f; break; // Bottom (-Y)
            case 4: 
            case 5: faceBrightness = 1.2f; break; // Front/Back (Z)
        }

        switch (sideIdx) {
            case 0: // +X
                addQuad(posList, uvList, litList, idxList, base,
                        x1, y, z1,  x1, y, z,  x1, y1, z,  x1, y1, z1, glow, faceBrightness);
                break;
            case 1: // -X
                addQuad(posList, uvList, litList, idxList, base,
                        x, y, z,  x, y, z1,  x, y1, z1,  x, y1, z, glow, faceBrightness);
                break;
            case 2: // +Y (TOP)
                addQuad(posList, uvList, litList, idxList, base,
                        x, y1, z1,  x1, y1, z1,  x1, y1, z,  x, y1, z, glow, faceBrightness);
                break;
            case 3: // -Y (BOTTOM)
                addQuad(posList, uvList, litList, idxList, base,
                        x, y, z,  x1, y, z,  x1, y, z1,  x, y, z1, glow, faceBrightness);
                break;
            case 4: // +Z
                addQuad(posList, uvList, litList, idxList, base,
                        x, y, z1,  x1, y, z1,  x1, y1, z1,  x, y1, z1, glow, faceBrightness);
                break;
            case 5: // -Z
                addQuad(posList, uvList, litList, idxList, base,
                        x1, y, z,  x, y, z,  x, y1, z,  x1, y1, z, glow, faceBrightness);
                break;
        }

        vertexCounts.put(tex, base + 4);
    }

    private static void addQuad(List<Float> pos, List<Float> uv, List<Float> lit, List<Integer> idx, int base,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float glow, float faceBrightness) {
        // Positions
        pos.add(x1); pos.add(y1); pos.add(z1);
        pos.add(x2); pos.add(y2); pos.add(z2);
        pos.add(x3); pos.add(y3); pos.add(z3);
        pos.add(x4); pos.add(y4); pos.add(z4);

        // UVs
        uv.add(1f); uv.add(1f);
        uv.add(0f); uv.add(1f);
        uv.add(0f); uv.add(0f);
        uv.add(1f); uv.add(0f);

        // Light Info (Glow hack: uses second channel as emissivity flag)
        for (int i = 0; i < 4; i++) {
            lit.add(faceBrightness); // Vertex Light Intensity (Directional Shading)
            lit.add(glow);           // Glow intensity (0.0 to 1.0)
        }

        // Indices
        idx.add(base); idx.add(base + 1); idx.add(base + 2);
        idx.add(base + 2); idx.add(base + 3); idx.add(base);
    }

    private static float[] toFloatArr(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] toIntArr(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
