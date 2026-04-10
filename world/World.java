package minicraft.world;

import java.util.Random;

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

    private final WorldGenerator generator;
    private final TextureRegistry textures;
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Map<String, minicraft.entity.Inventory> worldContainers = new HashMap<>();
    private final StructureGenerator structGen = new StructureGenerator();
    private final int renderDistance; 
    private final WeatherManager weatherManager = new WeatherManager();
    private final Random random = new Random();

    public World(long seed, TextureRegistry textures, int renderDistance) {
        this.textures = textures;
        this.generator = new WorldGenerator(seed);
        this.renderDistance = renderDistance;
    }

    public WorldGenerator getGenerator() { return generator; }


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

    public Block getSurfaceBlock(Biome biome) {
        switch (biome) {
            case OCEAN:        return Block.SAND;
            case FROZEN_OCEAN: return Block.ICE;
            case ARCTIC:
            case TUNDRA:
            case SNOWY_FOREST:
            case SNOWY_PEAKS:  return Block.SNOW;
            case REDWOOD:      return Block.PODZOL;
            case DESERT:       return Block.SAND;
            case MOUNTAINS:    return Block.STONE;
            case JUNGLE:
            case SAVANNA:
            case GRASSLAND:
            case FOREST:
            case HIGHLANDS:    return Block.GRASS;
            default:           return Block.GRASS;
        }
    }

    public Block getFillerBlock(Biome biome) {
        switch (biome) {
            case OCEAN:
            case DESERT:       return Block.SAND;
            case FROZEN_OCEAN:
            case ARCTIC:       return Block.ICE;
            case MOUNTAINS:
            case SNOWY_PEAKS:  return Block.STONE;
            default:           return Block.DIRT;
        }
    }

    public minicraft.math.Vector3f getFogColor(Biome biome) {
        switch (biome) {
            case FROZEN_OCEAN:
            case ARCTIC:
            case SNOWY_PEAKS:  return new minicraft.math.Vector3f(0.8f, 0.8f, 0.9f);
            case DESERT:       return new minicraft.math.Vector3f(0.8f, 0.7f, 0.5f);
            case JUNGLE:       return new minicraft.math.Vector3f(0.4f, 0.6f, 0.4f);
            case REDWOOD:      return new minicraft.math.Vector3f(0.4f, 0.5f, 0.6f);
            case SAVANNA:      return new minicraft.math.Vector3f(0.7f, 0.7f, 0.5f);
            default:           return new minicraft.math.Vector3f(0.5f, 0.6f, 0.7f);
        }
    }


    private Chunk generate(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        WorldCell[][] region = generator.generateRegion(cx * 16, cz * 16, 16, 16);
        int seaLevelY = (int)(WorldCell.SEA_LEVEL * Chunk.HEIGHT);

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                WorldCell cell = region[x][z];
                int surfaceY = (int) (cell.elevation * Chunk.HEIGHT);
                surfaceY = Math.min(Chunk.HEIGHT - 20, Math.max(BEDROCK_Y + 2, surfaceY));

                for (int y = BEDROCK_Y; y <= surfaceY; y++) {
                    Block b;
                    if (y == BEDROCK_Y) b = Block.BEDROCK;
                    else if (y == surfaceY) b = getSurfaceBlock(cell.biome);
                    else if (y >= surfaceY - DIRT_LAYERS) b = getFillerBlock(cell.biome);
                    else b = Block.STONE;
                    chunk.setBlock(x, y, z, b);
                }

                if (cell.isWater) {
                    for (int y = surfaceY + 1; y <= seaLevelY; y++) chunk.setBlock(x, y, z, Block.WATER);
                    // Undersea life
                    if (cell.biome == Biome.OCEAN && surfaceY < seaLevelY - 3 && Math.random() < 0.05) {
                        chunk.setBlock(x, surfaceY + 1, z, Math.random() < 0.7 ? Block.SEA_WEED : Block.CORAL);
                    }
                }

                // CAVES (using continentalness as a seed factor)
                for (int y = BEDROCK_Y + 1; y < surfaceY - 10; y++) {
                    double n1 = generator.getElevationOnly((x + cx * 16) * 1.5, (z + cz * 16) * 1.5 + y * 2.5);
                    if (Math.abs(n1) < 0.02) {
                        chunk.setBlock(x, y, z, Block.AIR);
                    }
                }
            }
        }

        // STRUCTURES & RE-POPULATION
        if (Math.random() < 0.08) {
             int sx = random.nextInt(8) + 4;
             int sz = random.nextInt(8) + 4;
             WorldCell scell = generator.generate(sx + cx * 16, sz + cz * 16);
             int sY = getSafeSpawnY(sx + cx * 16, sz + cz * 16);

             if (sY > seaLevelY + 5 && sY < Chunk.HEIGHT - 40) {
                 if (scell.biome == Biome.SAVANNA || scell.biome == Biome.GRASSLAND) {
                     structGen.generateVillage(chunk, sx, sY, sz, scell.biome);
                 } else if (scell.biome == Biome.MOUNTAINS || scell.biome == Biome.SNOWY_PEAKS) {
                     structGen.generateFortress(chunk, sx, sY, sz, scell.biome);
                 } else {
                     structGen.generateCastle(chunk, sx, sY, sz, scell.biome);
                 }
             }
        }

        // Vegetation Pass
        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                int sY = getSafeSpawnY(x + cx * 16, z + cz * 16);
                WorldCell cell = region[x][z];
                
                if (sY > seaLevelY && sY < Chunk.HEIGHT - 60) {
                    Block ground = chunk.getBlock(x, sY - 1, z);
                    float treeChance = 0.005f;
                    Block log = Block.OAK_WOOD, leaf = Block.OAK_LEAVES;

                    if (cell.biome == Biome.JUNGLE) { treeChance = 0.08f; log = Block.JUNGLE_WOOD; leaf = Block.JUNGLE_LEAVES; }
                    else if (cell.biome == Biome.REDWOOD) { treeChance = 0.03f; log = Block.REDWOOD_WOOD; leaf = Block.REDWOOD_LEAVES; }
                    else if (cell.biome == Biome.FOREST || cell.biome == Biome.SNOWY_FOREST) { treeChance = 0.02f; }
                    else if (cell.biome == Biome.TUNDRA) { treeChance = 0.005f; log = Block.REDWOOD_WOOD; leaf = Block.REDWOOD_LEAVES; }
                    else if (cell.biome == Biome.DESERT && Math.random() < 0.01) { chunk.setBlock(x, sY, z, Block.CACTUS); continue; }

                    if (Math.random() < treeChance) {
                        spawnTreeType(chunk, x, sY, z, log, leaf);
                    }
                }
            }
        }

        spawnOres(chunk, cx, cz);
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

    private void spawnTreeType(Chunk c, int x, int y, int z, Block log, Block leaf) {
        int h = 5 + (int)(Math.random() * 3);
        int r = 2;

        if (log == Block.REDWOOD_WOOD) {
            h = 15 + (int)(Math.random() * 10);
            r = 3;
        } else if (log == Block.JUNGLE_WOOD) {
            h = 10 + (int)(Math.random() * 8);
            r = 4;
        }

        if (y + h + 2 >= Chunk.HEIGHT) return;
        
        // Spawn Trunk
        for (int i = 0; i < h; i++) c.setBlock(x, y + i, z, log);
        
        // Spawn Foliage
        for (int lx = -r; lx <= r; lx++)
            for (int lz = -r; lz <= r; lz++)
                for (int ly = -2; ly <= 2; ly++) {
                    if (Math.abs(lx) + Math.abs(lz) + Math.abs(ly/2) <= r + 1) {
                        if (c.getBlock(x+lx, y+h+ly, z+lz).isAir()) {
                             c.setBlock(x+lx, y+h+ly, z+lz, leaf);
                        }
                    }
                }
    }

    private void spawnTree(Chunk c, int x, int y, int z) {
        spawnTreeType(c, x, y, z, Block.OAK_WOOD, Block.OAK_LEAVES);
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
            Block ground = getBlock(rx, y - 1, rz);
            if ((ground == Block.GRASS || ground == Block.PODZOL || ground == Block.SNOW) && getBlock(rx, y, rz).isAir()) {
                 return new minicraft.math.Vector3f(rx + 0.5f, y, rz + 0.5f);
            }
        }
        return new minicraft.math.Vector3f(startX, getSafeSpawnY(startX, startZ), startZ);
    }

    private int getSafeSpawnY(int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            Block b = getBlock(x, y, z);
            if (b.solid || b == Block.WATER || b == Block.ICE) return y + 1;
        }
        return (int)(WorldCell.SEA_LEVEL * Chunk.HEIGHT) + 2;
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

    public minicraft.entity.Inventory getContainer(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        if (!worldContainers.containsKey(key)) {
            minicraft.entity.Inventory inv = new minicraft.entity.Inventory();
            populateLoot(inv, x, y, z);
            worldContainers.put(key, inv);
        }
        return worldContainers.get(key);
    }

    private void populateLoot(minicraft.entity.Inventory inv, int x, int y, int z) {
        Random r = new Random((long)x * 1234567L + (long)z * 7654321L);
        WorldCell cell = generator.generate(x, z);
        float elev = cell.elevation;
        
        // Tier 1: Iron Age (Coal, Iron, Wood)
        inv.add(Block.COAL_ORE, 5 + r.nextInt(10));
        inv.add(Block.IRON_ORE, 2 + r.nextInt(5));
        
        // Tier 2: Elite Age (Gold, Silver, Obsidiam if high elev)
        if (elev > 0.45) {
            inv.add(Block.GOLD_ORE, 1 + r.nextInt(3));
            inv.add(Block.SILVER_ORE, 1 + r.nextInt(4));
        }
        
        // Tier 3: Legendary (Diamond if extreme elev)
        if (elev > 0.75) {
            inv.add(Block.DIAMOND_ORE, 1 + r.nextInt(2));
        }
    }

    public WeatherManager getWeather() { return weatherManager; }
    public void cleanup() { chunks.values().forEach(Chunk::cleanup); chunks.clear(); worldContainers.clear(); }
}
