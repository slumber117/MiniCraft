package minicraft.world;

import minicraft.math.Matrix4f;
import minicraft.renderer.ShaderProgram;
import minicraft.renderer.TextureRegistry;
import java.util.*;

/**
 * Procedural world system (400 Height optimized).
 */
public class World {

    private static final int SEA_LEVEL       = 64;
    private static final int MOUNTAIN_START  = 160;
    private static final int SNOW_START      = 320;
    private static final int BEDROCK_Y       = 1;
    private static final int DIRT_LAYERS     = 5;

    private final PerlinNoise noise;
    private final TextureRegistry textures;
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final int renderDistance; 
    private final WeatherManager weatherManager = new WeatherManager();

    public World(long seed, TextureRegistry textures, int renderDistance) {
        this.textures = textures;
        this.noise = new PerlinNoise(seed);
        this.renderDistance = renderDistance;
    }

    private final Matrix4f chunkMatrix = new Matrix4f();

    private long key(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getOrGenerate(int cx, int cz) {
        return chunks.computeIfAbsent(key(cx, cz), k -> generate(cx, cz));
    }

    public Block getBlock(int globalX, int globalY, int globalZ) {
        if (globalY < 0 || globalY >= Chunk.HEIGHT) return Block.AIR;
        Chunk chunk = chunks.get(key(Math.floorDiv(globalX, Chunk.WIDTH), Math.floorDiv(globalZ, Chunk.DEPTH)));
        if (chunk == null) return Block.AIR;
        return chunk.getBlock(Math.floorMod(globalX, Chunk.WIDTH), globalY, Math.floorMod(globalZ, Chunk.DEPTH));
    }

    public void setBlock(int gx, int gy, int gz, Block b) {
        if (gy < 0 || gy >= Chunk.HEIGHT) return;
        Chunk chunk = getOrGenerate(Math.floorDiv(gx, Chunk.WIDTH), Math.floorDiv(gz, Chunk.DEPTH));
        chunk.setBlock(Math.floorMod(gx, Chunk.WIDTH), gy, Math.floorMod(gz, Chunk.DEPTH), b);
    }

    public float getLight(int gx, int gy, int gz) {
        if (gy < 0 || gy >= Chunk.HEIGHT) return 1.0f;
        Chunk chunk = chunks.get(key(Math.floorDiv(gx, Chunk.WIDTH), Math.floorDiv(gz, Chunk.DEPTH)));
        if (chunk == null) return 1.0f;
        return chunk.getLight(Math.floorMod(gx, Chunk.WIDTH), gy, Math.floorMod(gz, Chunk.DEPTH));
    }

    private Chunk generate(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        int wx0 = cx * Chunk.WIDTH;
        int wz0 = cz * Chunk.DEPTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                double wx = (wx0 + x) / 100.0;
                double wz = (wz0 + z) / 100.0;

                // Surface Logic: Normalized to 400 height
                double heightNoise = noise.octaveNoise(wx, wz, 4, 0.5);
                int surfaceY = (int) ((heightNoise * 0.5 + 0.5) * 180) + 70;
                surfaceY = Math.min(Chunk.HEIGHT - 20, surfaceY);

                for (int y = BEDROCK_Y; y <= surfaceY; y++) {
                    Block b;
                    if (y == BEDROCK_Y) b = Block.BEDROCK;
                    else if (y == surfaceY) {
                        if (surfaceY >= SNOW_START) b = Block.SNOW;
                        else if (surfaceY <= SEA_LEVEL + 1) b = Block.SAND;
                        else b = Block.GRASS;
                    } else if (y >= surfaceY - DIRT_LAYERS) b = Block.DIRT;
                    else b = Block.STONE;
                    chunk.setBlock(x, y, z, b);
                }

                if (surfaceY < SEA_LEVEL) {
                    for (int y = surfaceY + 1; y <= SEA_LEVEL; y++) chunk.setBlock(x, y, z, Block.WATER);
                }

                // Caves logic (Vertical scale adjusted for 400 range)
                for (int y = BEDROCK_Y + 1; y < surfaceY - 8; y++) {
                    double wy = y / 20.0;
                    double cave1 = noise.noise(wx * 1.5, wy * 0.5, wz * 1.5);
                    double cave2 = noise.noise(wx * 0.5, wy * 1.5, wz * 0.5);
                    if (Math.abs(cave1) < 0.08 || Math.abs(cave2) < 0.06) chunk.setBlock(x, y, z, Block.AIR);
                }

                if (Math.random() < 0.012 && surfaceY > SEA_LEVEL + 1 && surfaceY < SNOW_START - 20) {
                    spawnTree(chunk, x, surfaceY + 1, z);
                }
            }
        }

        spawnOres(chunk, cx, cz);
        // buildMesh is intentionally NOT called here to prevent initial deadlock recursion.
        // It will be called in the update loop when visible.
        return chunk;
    }

    private void spawnOres(Chunk chunk, int cx, int cz) {
        Random r = new Random((long) cx * 342211L + (long) cz * 439241L);
        spawn(chunk, r, Block.COAL_ORE,   10, 200, 6, 40);
        spawn(chunk, r, Block.IRON_ORE,   5, 120, 5, 20);
        spawn(chunk, r, Block.GOLD_ORE,   1, 50, 4, 10);
        spawn(chunk, r, Block.DIAMOND_ORE, 1, 20, 3, 5);
    }

    private void spawn(Chunk c, Random r, Block o, int min, int max, int sz, int att) {
        for (int i = 0; i < att; i++) {
            int ox = r.nextInt(16), oy = min + r.nextInt(Math.max(1, max - min)), oz = r.nextInt(16);
            for (int v = 0; v < sz; v++) {
                int bx = ox + r.nextInt(3)-1, by = oy + r.nextInt(3)-1, bz = oz + r.nextInt(3)-1;
                if (c.getBlock(bx, by, bz) == Block.STONE) c.setBlock(bx, by, bz, o);
            }
        }
    }

    private void spawnTree(Chunk c, int x, int y, int z) {
        int h = 5 + (int)(Math.random() * 3);
        if (y + h + 2 >= Chunk.HEIGHT) return;
        for (int i = 0; i < h; i++) c.setBlock(x, y + i, z, Block.WOOD);
        for (int lx = -2; lx <= 2; lx++)
            for (int lz = -2; lz <= 2; lz++)
                for (int ly = -1; ly <= 2; ly++)
                    if (Math.abs(lx) + Math.abs(lz) + Math.abs(ly) <= 4)
                        if (c.getBlock(x+lx, y+h+ly, z+lz).isAir()) c.setBlock(x+lx, y+h+ly, z+lz, Block.LEAVES);
    }

    public void update(float dt, minicraft.entity.Player player, minicraft.entity.ParticleManager pm) {
        // Occasionally spawn smoke from nearby torches
        int px = (int) player.position.x;
        int py = (int) player.position.y;
        int pz = (int) player.position.z;
        int r = 10;
        
        if (new java.util.Random().nextInt(5) == 0) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (getBlock(px + dx, py + dy, pz + dz) == Block.TORCH) {
                            pm.spawnSmoke(px + dx + 0.5f, py + dy, pz + dz + 0.5f);
                        }
                    }
                }
            }
        }
    }

    public void update(int cx, int cz, float dt) {
        weatherManager.update(dt);
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                Chunk chunk = getOrGenerate(cx + dx, cz + dz);
                if (chunk.isDirty()) chunk.buildMesh(textures, this);
            }
        }
    }

    public minicraft.math.Vector3f findSafeGrassSpawn(int startX, int startZ) {
        int maxSteps = 100;
        for (int i = 0; i < maxSteps; i++) {
            int rx = startX + (int)(Math.random() * 64 - 32);
            int rz = startZ + (int)(Math.random() * 64 - 32);
            int y = getSafeSpawnY(rx, rz);
            if (getBlock(rx, y - 1, rz) == Block.GRASS && getBlock(rx, y, rz).isAir()) {
                 return new minicraft.math.Vector3f(rx + 0.5f, y, rz + 0.5f);
            }
        }
        return new minicraft.math.Vector3f(startX, getSafeSpawnY(startX, startZ), startZ);
    }

    private int getSafeSpawnY(int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            if (getBlock(x, y, z).solid) return y + 1;
        }
        return SEA_LEVEL + 2;
    }

    public void render(ShaderProgram shader, minicraft.math.Vector3f playerPos) {
        shader.setUniform("sunBrightness", weatherManager.getSunBrightness());
        shader.setUniform("weatherIntensity", weatherManager.getIntensity());
        shader.setUniform("weatherType", weatherManager.getCurrentType().ordinal());

        int pcx = (int) Math.floor(playerPos.x / 16.0);
        int pcz = (int) Math.floor(playerPos.z / 16.0);

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                Chunk chunk = chunks.get(key(pcx + dx, pcz + dz));
                if (chunk != null) {
                    chunkMatrix.identity().translate(chunk.chunkX * 16, 0, chunk.chunkZ * 16);
                    shader.setUniform("modelMatrix", chunkMatrix);
                    chunk.render();
                }
            }
        }
    }

    public WeatherManager getWeather() { return weatherManager; }
    public void cleanup() { chunks.values().forEach(Chunk::cleanup); chunks.clear(); }
}
