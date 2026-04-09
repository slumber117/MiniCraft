package minicraft.world;

import minicraft.renderer.Mesh;
import minicraft.renderer.TextureRegistry;
import java.util.*;

/**
 * Optimized Chunk: 16x400x16.
 * Uses flattened 1D arrays to minimize object overhead and memory fragmentation.
 */
public class Chunk {

    public static final int WIDTH        = 16;
    public static final int HEIGHT       = 400;
    public static final int DEPTH        = 16;
    public static final int SECTION_SIZE = 80;
    public static final int NUM_SECTIONS = HEIGHT / SECTION_SIZE; // 5

    public final int chunkX;
    public final int chunkZ;

    private final Block[] blocks = new Block[WIDTH * HEIGHT * DEPTH];
    private final float[] skyLightMap = new float[WIDTH * HEIGHT * DEPTH]; 
    private final float[] blockLightMap = new float[WIDTH * HEIGHT * DEPTH]; 
    
    private final Map<String, Mesh> meshes = new HashMap<>();
    private final boolean[] sectionDirty = new boolean[NUM_SECTIONS];

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        for (int i = 0; i < NUM_SECTIONS; i++) sectionDirty[i] = true;
        Arrays.fill(blocks, Block.AIR);
    }

    private int idx(int x, int y, int z) {
        return x + z * WIDTH + y * WIDTH * DEPTH;
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return Block.AIR;
        return blocks[idx(x, y, z)];
    }

    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return;
        int i = idx(x, y, z);
        if (blocks[i] == block) return;
        
        blocks[i] = block;
        sectionDirty[y / SECTION_SIZE] = true;
    }

    public void markDirty() {
        for (int i = 0; i < NUM_SECTIONS; i++) sectionDirty[i] = true;
    }

    public boolean isDirty() {
        for (boolean d : sectionDirty) if (d) return true;
        return false;
    }

    public void buildMesh(TextureRegistry registry, World world) {
        recalculateColumnLighting();
        for (int s = 0; s < NUM_SECTIONS; s++) {
            if (sectionDirty[s]) {
                buildSectionMesh(s, registry, world);
                sectionDirty[s] = false;
            }
        }
    }

    private void buildSectionMesh(int sIdx, TextureRegistry registry, World world) {
        // Cleanup existing meshes for this section
        meshes.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(sIdx + "_")) {
                entry.getValue().cleanup();
                return true;
            }
            return false;
        });

        Map<String, ArrayList<Float>> posByTex = new HashMap<>();
        Map<String, ArrayList<Float>> uvsByTex = new HashMap<>();
        Map<String, ArrayList<Integer>> idxByTex = new HashMap<>();
        Map<String, Integer> cntByTex = new HashMap<>();

        int yStart = sIdx * SECTION_SIZE;
        int yEnd = yStart + SECTION_SIZE;

        for (int y = yStart; y < yEnd; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    Block b = blocks[idx(x, y, z)];
                    if (b == null || b.isAir()) continue;

                    if (b.meshType == Block.MeshType.CROSS) {
                        addCrossMesh(x, y, z, b, posByTex, uvsByTex, idxByTex, cntByTex);
                    } else {
                        checkFace(world, x, y, z, 0, b, Face.SIDE,   posByTex, uvsByTex, idxByTex, cntByTex);
                        checkFace(world, x, y, z, 1, b, Face.SIDE,   posByTex, uvsByTex, idxByTex, cntByTex);
                        checkFace(world, x, y, z, 2, b, Face.TOP,    posByTex, uvsByTex, idxByTex, cntByTex);
                        checkFace(world, x, y, z, 3, b, Face.BOTTOM, posByTex, uvsByTex, idxByTex, cntByTex);
                        checkFace(world, x, y, z, 4, b, Face.SIDE,   posByTex, uvsByTex, idxByTex, cntByTex);
                        checkFace(world, x, y, z, 5, b, Face.SIDE,   posByTex, uvsByTex, idxByTex, cntByTex);
                    }
                }
            }
        }

        // Column lighting already calculated in buildMesh

        for (String texKey : posByTex.keySet()) {
            float[] posArr = toFloatArr(posByTex.get(texKey));
            float[] uvArr = toFloatArr(uvsByTex.get(texKey));
            int[] idxArr = toIntArr(idxByTex.get(texKey));
            
            float[] lightArr = new float[posArr.length / 3 * 2];
            for (int i = 0; i < posArr.length / 3; i++) {
                int vx = (int) Math.round(posArr[i*3]);
                int vy = (int) Math.round(posArr[i*3+1]);
                int vz = (int) Math.round(posArr[i*3+2]);
                
                int clx = Math.max(0, Math.min(WIDTH - 1, vx));
                int cly = Math.max(0, Math.min(HEIGHT - 1, vy));
                int clz = Math.max(0, Math.min(DEPTH - 1, vz));

                int lightIdx = idx(clx, cly, clz);
                lightArr[i * 2] = skyLightMap[lightIdx];
                lightArr[i * 2 + 1] = blockLightMap[lightIdx];
            }
            meshes.put(sIdx + "_" + texKey, new Mesh(posArr, uvArr, lightArr, idxArr, registry.get(texKey)));
        }
    }

    private void recalculateColumnLighting() {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                float skyLight = 1.0f;
                for (int y = HEIGHT - 1; y >= 0; y--) {
                    int i = idx(x, y, z);
                    // Light is blocked by solid blocks
                    if (blocks[i] != null && blocks[i].solid) {
                        skyLight = 0.0f;
                    }
                    skyLightMap[i] = skyLight;
                }
            }
        }
        propagateTorchLight();
    }

    private void propagateTorchLight() {
        Arrays.fill(blockLightMap, 0.0f);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (blocks[idx(x, y, z)] == Block.TORCH) {
                        applyLightSource(x, y, z, 1.0f, 8);
                    }
                }
            }
        }
    }

    private void applyLightSource(int sx, int sy, int sz, float strength, int range) {
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    int x = sx + dx, y = sy + dy, z = sz + dz;
                    if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) continue;
                    float d = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                    if (d > range) continue;
                    
                    float light = 0f;
                    if (d <= 2.0f)      light = 0.95f;
                    else if (d <= 4.0f) light = 0.65f;
                    else if (d <= 8.0f) light = 0.35f;
                    
                    int i = idx(x, y, z);
                    blockLightMap[i] = Math.max(blockLightMap[i], light * strength);
                }
            }
        }
    }

    private void checkFace(World world, int x, int y, int z, int sideIdx, Block b, Face f,
                           Map<String, ArrayList<Float>> posByTex,
                           Map<String, ArrayList<Float>> uvsByTex,
                           Map<String, ArrayList<Integer>> idxByTex,
                           Map<String, Integer> cntByTex) {
        if (b == null || b.isAir()) return;

        int gx = chunkX * WIDTH + x;
        int gz = chunkZ * DEPTH + z;
        int nx = gx + (sideIdx == 0 ? 1 : (sideIdx == 1 ? -1 : 0));
        int ny = y  + (sideIdx == 2 ? 1 : (sideIdx == 3 ? -1 : 0));
        int nz = gz + (sideIdx == 4 ? 1 : (sideIdx == 5 ? -1 : 0));

        Block neighbor = world.getBlock(nx, ny, nz);
        if (neighbor != null && neighbor.isOpaque()) return;

        String tex = b.getTextureForFace(f);
        if (tex == null) return;
        float uvPadding = b.getPaddingForFace(f);

        if (!posByTex.containsKey(tex)) {
            posByTex.put(tex, new ArrayList<>());
            uvsByTex.put(tex, new ArrayList<>());
            idxByTex.put(tex, new ArrayList<>());
            cntByTex.put(tex, 0);
        }

        ArrayList<Float> pos = posByTex.get(tex);
        int base = cntByTex.get(tex);

        float x0 = x, y0 = y, z0 = z, x1 = x + 1, y1 = y + 1, z1 = z + 1;
        
        // 3D Torch Stick Shrink (2x2x10 pixels centered)
        if (b == Block.TORCH) {
            float margin = 7.0f / 16.0f; // Center the 2/16 width stick
            x0 += margin; x1 -= margin;
            z0 += margin; z1 -= margin;
            y1 = y + 10.0f / 16.0f; // 10/16 height
        }

        switch (sideIdx) {
            case 0: v(pos, x1, y0, z1); v(pos, x1, y0, z0); v(pos, x1, y1, z0); v(pos, x1, y1, z1); break;
            case 1: v(pos, x0, y0, z0); v(pos, x0, y0, z1); v(pos, x0, y1, z1); v(pos, x0, y1, z0); break;
            case 2: v(pos, x0, y1, z1); v(pos, x1, y1, z1); v(pos, x1, y1, z0); v(pos, x0, y1, z0); break;
            case 3: v(pos, x0, y0, z0); v(pos, x1, y0, z0); v(pos, x1, y0, z1); v(pos, x0, y0, z1); break;
            case 4: v(pos, x0, y0, z1); v(pos, x1, y0, z1); v(pos, x1, y1, z1); v(pos, x0, y1, z1); break;
            case 5: v(pos, x1, y0, z0); v(pos, x0, y0, z0); v(pos, x0, y1, z0); v(pos, x1, y1, z0); break;
        }

        addUVs(uvsByTex.get(tex), uvPadding);
        addIdx(idxByTex.get(tex), base);
        cntByTex.put(tex, base + 4);
    }

    private void addCrossMesh(int x, int y, int z, Block b,
                              Map<String, ArrayList<Float>> posByTex,
                              Map<String, ArrayList<Float>> uvsByTex,
                              Map<String, ArrayList<Integer>> idxByTex,
                              Map<String, Integer> cntByTex) {
        String tex = b.sideTexture;
        if (!posByTex.containsKey(tex)) {
            posByTex.put(tex, new ArrayList<>());
            uvsByTex.put(tex, new ArrayList<>());
            idxByTex.put(tex, new ArrayList<>());
            cntByTex.put(tex, 0);
        }
        addCrossMesh(posByTex.get(tex), uvsByTex.get(tex), idxByTex.get(tex), x, y, z, b);
        cntByTex.put(tex, cntByTex.get(tex) + 32); // Cross mesh has many verts
    }

    private void addCrossMesh(List<Float> pos, List<Float> uv, List<Integer> ind, int x, int y, int z, Block b) {
        int startVertex = pos.size() / 3;
        v(pos, x+0.5f, y, z); v(pos, x+0.5f, y, z+1); v(pos, x+0.5f, y+1, z+1); v(pos, x+0.5f, y+1, z);
        v(pos, x, y, z+0.5f); v(pos, x+1, y, z+0.5f); v(pos, x+1, y+1, z+0.5f); v(pos, x, y+1, z+0.5f);
        v(pos, x, y, z); v(pos, x+1, y, z+1); v(pos, x+1, y+1, z+1); v(pos, x, y+1, z);
        v(pos, x, y, z+1); v(pos, x+1, y, z); v(pos, x+1, y+1, z); v(pos, x, y+1, z+1);

        for (int i = 0; i < 4; i++) {
            uv.add(0f); uv.add(0f); uv.add(1f); uv.add(0f); uv.add(1f); uv.add(1f); uv.add(0f); uv.add(1f);
            int o = startVertex + i * 4;
            ind.add(o); ind.add(o+1); ind.add(o+2); ind.add(o+2); ind.add(o+3); ind.add(o);
            ind.add(o+2); ind.add(o+1); ind.add(o); ind.add(o); ind.add(o+3); ind.add(o+2);
        }
    }

    private void v(List<Float> p, float x, float y, float z) { p.add(x); p.add(y); p.add(z); }

    private void addUVs(ArrayList<Float> uvs, float p) {
        float o = p / 1024.0f;
        uvs.add(0f+o); uvs.add(0f+o); uvs.add(1f-o); uvs.add(0f+o); uvs.add(1f-o); uvs.add(1f-o); uvs.add(0f+o); uvs.add(1f-o);
    }

    private void addIdx(ArrayList<Integer> idx, int base) {
        idx.add(base); idx.add(base+1); idx.add(base+2); idx.add(base); idx.add(base+2); idx.add(base+3);
    }

    public void render() { for (Mesh m : meshes.values()) m.render(); }

    public void cleanup() { meshes.values().forEach(Mesh::cleanup); meshes.clear(); }

    public float getLight(int lx, int y, int lz) {
        if (y < 0 || y >= HEIGHT) return 1.0f;
        int i = idx(lx, y, lz);
        return Math.max(skyLightMap[i], blockLightMap[i]);
    }

    private float[] toFloatArr(ArrayList<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }
    private int[] toIntArr(ArrayList<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }
}
