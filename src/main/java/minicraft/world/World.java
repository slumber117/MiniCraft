package minicraft.world;

import minicraft.Main;
import minicraft.world.behavior.*;
import minicraft.world.cave.CaveCarver;
import minicraft.world.cave.CaveCell;
import minicraft.world.cave.CaveType;
import minicraft.math.Matrix4f;
import minicraft.renderer.ShaderProgram;
import minicraft.renderer.TextureRegistry;
import minicraft.world.cave.geode.*;
import java.util.*;

/**
 * Procedural world system (400 Height optimized).
 */
public class World {

    private static final int SEA_LEVEL = 102;
    private static final int MOUNTAIN_START = 256;
    private static final int SNOW_START = 512;
    private static final int BEDROCK_Y = 1;
    private static final int DIRT_LAYERS = 5;

    private final WorldGenerator generator;
    private final TextureRegistry textures;
    private final Map<Long, Chunk> chunks = new HashMap<>();

    private Block getGemBlock(GemType type) {
        if (type == null) return Block.STONE;
        switch (type) {
            case DIAMOND:   return Block.DIAMOND_ORE;
            case EMERALD:   return Block.EMERALD_ORE;
            case RUBY:      return Block.RUBY_ORE;
            case TANZANITE: return Block.TANZANITE_ORE;
            case AMETHYST: 
            default:        return Block.AMETHYST_ORE;
        }
    }

    // FIXED: Changed String to Long to match packPos logic
    private final Map<Long, minicraft.entity.Inventory> worldContainers = new HashMap<>();
    private final Map<Long, minicraft.entity.ProcessingFacility> worldFacilities = new HashMap<>();

    private final StructureGenerator structGen = new StructureGenerator();
    private final CaveCarver caveCarver;
    private final int renderDistance;
    private final WeatherManager weatherManager = new WeatherManager();
    private int tickCounter = 0;
    private final Random random = new Random();

    public World(long seed, TextureRegistry textures, int renderDistance) {
        this.textures = textures;
        this.generator = new WorldGenerator(seed);
        this.caveCarver = new CaveCarver(seed);
        this.renderDistance = renderDistance;
    }

    public WorldGenerator getGenerator() {
        return generator;
    }

    public WeatherManager getWeatherManager() {
        return weatherManager;
    }

    public Biome getBiome(int gx, int gz) {
        return generator.generate(gx, gz).biome;
    }

    private final Matrix4f chunkMatrix = new Matrix4f();

    private long key(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getOrGenerate(int cx, int cz) {
        return chunks.computeIfAbsent(key(cx, cz), k -> generate(cx, cz));
    }

    public Block getBlock(int globalX, int globalY, int globalZ) {
        if (globalY < 0 || globalY >= Chunk.HEIGHT)
            return Block.AIR;
        Chunk chunk = chunks.get(key(Math.floorDiv(globalX, Chunk.WIDTH), Math.floorDiv(globalZ, Chunk.DEPTH)));
        if (chunk == null)
            return Block.AIR;
        return chunk.getBlock(Math.floorMod(globalX, Chunk.WIDTH), globalY, Math.floorMod(globalZ, Chunk.DEPTH));
    }

    public void setBlock(int gx, int gy, int gz, Block b) {
        if (gy < 0 || gy >= Chunk.HEIGHT)
            return;
        int cx = Math.floorDiv(gx, Chunk.WIDTH);
        int cz = Math.floorDiv(gz, Chunk.DEPTH);
        int lx = Math.floorMod(gx, Chunk.WIDTH);
        int lz = Math.floorMod(gz, Chunk.DEPTH);

        Chunk chunk = getOrGenerate(cx, cz);
        chunk.setBlock(lx, gy, lz, b);

        if (lx == 0)
            markChunkDirty(cx - 1, cz);
        if (lx == Chunk.WIDTH - 1)
            markChunkDirty(cx + 1, cz);
        if (lz == 0)
            markChunkDirty(cx, cz - 1);
        if (lz == Chunk.DEPTH - 1)
            markChunkDirty(cx, cz + 1);
    }

    public void markChunkDirty(int cx, int cz) {
        Chunk neighbor = chunks.get(key(cx, cz));
        if (neighbor != null)
            neighbor.markDirty();
    }

    public float getLight(int gx, int gy, int gz) {
        if (gy < 0 || gy >= Chunk.HEIGHT)
            return 1.0f;
        Chunk chunk = chunks.get(key(Math.floorDiv(gx, Chunk.WIDTH), Math.floorDiv(gz, Chunk.DEPTH)));
        if (chunk == null)
            return 1.0f;
        return chunk.getLight(Math.floorMod(gx, Chunk.WIDTH), gy, Math.floorMod(gz, Chunk.DEPTH));
    }

    public Block getSurfaceBlock(Biome biome) {
        switch (biome) {
            case OCEAN:
                return Block.SAND;
            case FROZEN_OCEAN:
                return Block.ICE;
            case ARCTIC:
            case TUNDRA:
            case SNOWY_FOREST:
            case SNOWY_PEAKS:
                return Block.SNOW;
            case REDWOOD:
                return Block.PODZOL;
            case DESERT:
                return Block.SAND;
            case MOUNTAINS:
                return Block.STONE;
            case JUNGLE:
            case SAVANNA:
            case GRASSLAND:
            case FOREST:
            case HIGHLANDS:
                return Block.GRASS;
            default:
                return Block.GRASS;
        }
    }

    public Block getFillerBlock(Biome biome) {
        switch (biome) {
            case OCEAN:
            case DESERT:
                return Block.SAND;
            case FROZEN_OCEAN:
            case ARCTIC:
                return Block.ICE;
            case MOUNTAINS:
            case SNOWY_PEAKS:
                return Block.STONE;
            default:
                return Block.DIRT;
        }
    }

    public minicraft.math.Vector3f getFogColor(Biome biome) {
        switch (biome) {
            case FROZEN_OCEAN:
            case ARCTIC:
            case SNOWY_PEAKS:
                return new minicraft.math.Vector3f(0.8f, 0.8f, 0.9f);
            case DESERT:
                return new minicraft.math.Vector3f(0.8f, 0.7f, 0.5f);
            case JUNGLE:
                return new minicraft.math.Vector3f(0.4f, 0.6f, 0.4f);
            case REDWOOD:
                return new minicraft.math.Vector3f(0.4f, 0.5f, 0.6f);
            case SAVANNA:
                return new minicraft.math.Vector3f(0.7f, 0.7f, 0.5f);
            default:
                return new minicraft.math.Vector3f(0.5f, 0.6f, 0.7f);
        }
    }

    private Chunk generate(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        WorldCell[][] region = generator.generateRegion(cx * 16, cz * 16, 16, 16);
        int seaLevelY = (int) (WorldCell.SEA_LEVEL * Chunk.HEIGHT);
        int centerSurfaceY = (int) (region[8][8].elevation * Chunk.HEIGHT);
        caveCarver.prepareChunk(cx, cz, centerSurfaceY, Chunk.HEIGHT);

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                WorldCell cell = region[x][z];
                int surfaceY = (int) (cell.elevation * Chunk.HEIGHT);
                surfaceY = Math.min(Chunk.HEIGHT - 20, Math.max(BEDROCK_Y + 2, surfaceY));
                int gx = x + cx * 16;
                int gz = z + cz * 16;

                for (int y = BEDROCK_Y; y <= surfaceY; y++) {
                    Block b;
                    if (y == BEDROCK_Y)
                        b = Block.BEDROCK;
                    else if (y == surfaceY)
                        b = getSurfaceBlock(cell.biome);
                    else if (y >= surfaceY - DIRT_LAYERS)
                        b = getFillerBlock(cell.biome);
                    else {
                        boolean isDeep = y < 80;
                        boolean isMountainCore = (cell.biome == Biome.MOUNTAINS || cell.biome == Biome.SNOWY_PEAKS)
                                && y > (surfaceY - 45);
                        b = (isDeep || isMountainCore) ? Block.STONE_DARK : Block.STONE;
                    }
                    chunk.setBlock(x, y, z, b);
                }

                if (cell.isWater) {
                    for (int y = surfaceY + 1; y <= seaLevelY; y++)
                        chunk.setBlock(x, y, z, Block.WATER);
                }

                for (int y = BEDROCK_Y + 1; y < surfaceY; y++) {
                    CaveCell caveCell = caveCarver.query(gx, y, gz, cell, surfaceY);
                    
                    // ── Geode Materialization ──────────────────────────────────────
                    if (caveCell.type == CaveType.GEODE_SHELL) {
                        chunk.setBlock(x, y, z, Block.STONE_DARK);
                        continue;
                    }
                    if (caveCell.type == CaveType.GEODE_CRYSTAL) {
                        chunk.setBlock(x, y, z, getGemBlock(caveCell.gemType));
                        continue;
                    }

                    if (caveCell.isCarved) {
                        Block b = Block.AIR;
                        if (caveCell.type == CaveType.UNDERWATER || caveCell.type == CaveType.GEODE_HOLLOW)
                            b = Block.WATER; // Use water for underwater, air for geode hollows (overridden later)
                        
                        if (caveCell.type == CaveType.GEODE_HOLLOW) b = Block.AIR;

                        if (caveCell.type == CaveType.UNDERWATER) b = Block.WATER;
                        else if (y < 80 && Math.random() < 0.05 && caveCell.type != CaveType.GEODE_HOLLOW)
                            b = Block.LAVA;
                        
                        chunk.setBlock(x, y, z, b);
                    } else if (y < 80 && Math.random() < 0.03) {
                        chunk.setBlock(x, y, z, Block.MAGMA);
                    }
                }

                if (!cell.isWater && cell.biome == Biome.DESERT && Math.random() < 0.0001) {
                    for (int ly = surfaceY - 2; ly <= surfaceY; ly++)
                        chunk.setBlock(x, ly, z, Block.LAVA);
                }
            }
        }

        // --- Multi-Tier Ore Spawning ---
        spawnOres(chunk, cx, cz);

        // Structure and Vegetation logic
        WorldCell centerCell = generator.generate(cx * 16 + 8, cz * 16 + 8);
        int centerPeakY = 0;
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            if (chunk.getBlock(8, y, 8).solid) {
                centerPeakY = y + 1;
                break;
            }
        }
        boolean isInitialSpawn = (cx == 0 && cz == 0);
        boolean isPeak = (centerCell.biome == Biome.MOUNTAINS || centerCell.biome == Biome.SNOWY_PEAKS
                || centerCell.biome == Biome.HIGHLANDS) && centerPeakY > 250;
        if (isInitialSpawn || (cx % 64 == 0 && cz % 64 == 0 && isPeak)) {
            int targetY = Math.max(480, centerPeakY + 30);
            structGen.generateFloatingFactory(chunk, targetY, centerPeakY);
        }

        int sx = random.nextInt(8) + 4;
        int sz = random.nextInt(8) + 4;
        WorldCell scell = generator.generate(sx + cx * 16, sz + cz * 16);
        int structY = getSafeSpawnY(sx + cx * 16, sz + cz * 16);
        if (Math.random() < 0.08) {
            if (structY > seaLevelY + 5 && structY < Chunk.HEIGHT - 40) {
                if (scell.biome == Biome.SAVANNA || scell.biome == Biome.GRASSLAND)
                    structGen.generateVillage(chunk, sx, structY, sz, scell.biome);
                else if (scell.biome == Biome.MOUNTAINS || scell.biome == Biome.SNOWY_PEAKS)
                    structGen.generateFortress(chunk, sx, structY, sz, scell.biome);
                else
                    structGen.generateCastle(chunk, sx, structY, sz, scell.biome);
            }
        }

        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                int sY = getSafeSpawnY(x + cx * 16, z + cz * 16);
                WorldCell cell = region[x][z];
                if (sY > seaLevelY && sY < Chunk.HEIGHT - 60) {
                    Block ground = chunk.getBlock(x, sY - 1, z);
                    if (ground == Block.WATER || ground == Block.ICE || ground == Block.AIR)
                        continue;
                    float treeChance = 0.005f;
                    Block log = Block.OAK_WOOD, leaf = Block.OAK_LEAVES;
                    if (cell.biome == Biome.JUNGLE) {
                        treeChance = 0.08f;
                        log = Block.JUNGLE_WOOD;
                        leaf = Block.JUNGLE_LEAVES;
                    } else if (cell.biome == Biome.REDWOOD) {
                        treeChance = 0.03f;
                        log = Block.REDWOOD_WOOD;
                        leaf = Block.REDWOOD_LEAVES;
                    } else if (cell.biome == Biome.FOREST || cell.biome == Biome.SNOWY_FOREST) {
                        treeChance = 0.02f;
                    } else if (cell.biome == Biome.TUNDRA) {
                        treeChance = 0.005f;
                        log = Block.REDWOOD_WOOD;
                        leaf = Block.REDWOOD_LEAVES;
                    } else if (cell.biome == Biome.DESERT) {
                        if (Math.random() < 0.02)
                            chunk.setBlock(x, sY, z, Block.CACTUS);
                        continue;
                    }
                    if (Math.random() < treeChance)
                        spawnTreeType(chunk, x, sY, z, log, leaf);
                    else if (Math.random() < 0.15f) {
                        double f = Math.random();
                        if (f < 0.60)
                            chunk.setBlock(x, sY, z, Block.TALL_GRASS);
                        else if (f < 0.75)
                            chunk.setBlock(x, sY, z, Block.FLOWER_RED);
                        else if (f < 0.85)
                            chunk.setBlock(x, sY, z, Block.FLOWER_BLUE);
                        else if (f < 0.95)
                            chunk.setBlock(x, sY, z, Block.MUSHROOM);
                    }
                } else if (sY <= seaLevelY) {
                    // Undersea Decoration
                    if (chunk.getBlock(x, sY - 1, z) == Block.DIRT || chunk.getBlock(x, sY - 1, z) == Block.STONE) {
                        if (Math.random() < 0.05) {
                            chunk.setBlock(x, sY, z, Block.SEA_WEED);
                        } else if (Math.random() < 0.01) {
                            chunk.setBlock(x, sY, z, Block.CORAL);
                        }
                    }
                }
            }
        }
        return chunk;
    }

    private void spawnOres(Chunk chunk, int cx, int cz) {
        // Tier 1: Common Surface Ores (Y: 80 - 250)
        spawnOreGrip(chunk, Block.COAL_ORE, ClusterSize.LARGE, 25, 80, 250);
        spawnOreGrip(chunk, Block.IRON_ORE, ClusterSize.MEDIUM, 18, 60, 200);
        spawnOreGrip(chunk, Block.COPPER_ORE, ClusterSize.MEDIUM, 15, 60, 180);
        spawnOreGrip(chunk, Block.TIN_ORE, ClusterSize.MEDIUM, 12, 60, 160);

        // Tier 2: Mid-level Industrial/Precious (Y: 40 - 120)
        spawnOreGrip(chunk, Block.GOLD_ORE, ClusterSize.SMALL, 8, 30, 120);
        spawnOreGrip(chunk, Block.SILVER_ORE, ClusterSize.SMALL, 10, 30, 110);
        spawnOreGrip(chunk, Block.NICKEL_ORE, ClusterSize.SMALL, 7, 20, 100);
        spawnOreGrip(chunk, Block.TANZANITE_ORE, ClusterSize.SMALL, 4, 20, 90);
        spawnOreGrip(chunk, Block.QUARTZ_ORE, ClusterSize.MEDIUM, 15, 30, 250);

        // Tier 3: Deep Gems and Hard Metals (Y: 0 - 60)
        spawnOreGrip(chunk, Block.DIAMOND_ORE, ClusterSize.TINY, 3, 5, 45);
        spawnOreGrip(chunk, Block.EMERALD_ORE, ClusterSize.TINY, 2, 80, 512); // Mountains only usually, but deep too
        spawnOreGrip(chunk, Block.RUBY_ORE, ClusterSize.TINY, 2, 5, 50);
        spawnOreGrip(chunk, Block.SAPPHIRE_ORE, ClusterSize.TINY, 2, 5, 50);
        spawnOreGrip(chunk, Block.TITANIUM_ORE, ClusterSize.SMALL, 12, 5, 60);

        // Tier 4: Rare & Radioactive (Y: 0 - 25)
        spawnOreGrip(chunk, Block.URANIUM_ORE, ClusterSize.SMALL, 3, 2, 25);
        spawnOreGrip(chunk, Block.PLUTONIUM_ORE, ClusterSize.SMALL, 2, 2, 20);
        spawnOreGrip(chunk, Block.ADAMANTINE_ORE, ClusterSize.TINY, 1, 2, 15);
        spawnOreGrip(chunk, Block.MITHRIL_ORE, ClusterSize.TINY, 2, 2, 30);
    }

    private enum ClusterSize { TINY(2), SMALL(4), MEDIUM(8), LARGE(12); int count; ClusterSize(int c) { count = c; } }

    private void spawnOreGrip(Chunk chunk, Block ore, ClusterSize size, int density, int minY, int maxY) {
        for (int i = 0; i < density; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            
            // Check if near a cave (air neighbor)
            boolean nearCave = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (x + dx >= 0 && x + dx < 16 && y + dy >= 0 && y + dy < Chunk.HEIGHT && z + dz >= 0 && z + dz < 16) {
                            if (chunk.getBlock(x + dx, y + dy, z + dz) == Block.AIR) {
                                nearCave = true;
                                break;
                            }
                        }
                    }
                    if (nearCave) break;
                }
                if (nearCave) break;
            }

            int finalCount = nearCave ? size.count * 2 : size.count;

            if (chunk.getBlock(x, y, z) == Block.STONE || chunk.getBlock(x, y, z) == Block.STONE_DARK) {
                // Simplified clump logic: sprinkle around center
                for (int c = 0; c < finalCount; c++) {
                    int dx = random.nextInt(3) - 1;
                    int dy = random.nextInt(3) - 1;
                    int dz = random.nextInt(3) - 1;
                    if (x + dx >= 0 && x + dx < 16 && y + dy >= 0 && y + dy < Chunk.HEIGHT && z + dz >= 0 && z + dz < 16) {
                        if (chunk.getBlock(x + dx, y + dy, z + dz) == Block.STONE || chunk.getBlock(x + dx, y + dy, z + dz) == Block.STONE_DARK) {
                            chunk.setBlock(x + dx, y + dy, z + dz, ore);
                        }
                    }
                }
            }
        }
    }

    private void spawnTreeType(Chunk chunk, int x, int y, int z, Block log, Block leaves) {
        int h = 5 + (int) (Math.random() * 3);
        int r = 2;
        if (log == Block.REDWOOD_WOOD) {
            h = 15 + (int) (Math.random() * 10);
            r = 3;
        } else if (log == Block.JUNGLE_WOOD) {
            h = 10 + (int) (Math.random() * 8);
            r = 4;
        }
        if (y + h + 2 >= Chunk.HEIGHT)
            return;
        for (int i = 0; i < h; i++)
            chunk.setBlock(x, y + i, z, log);
        for (int lx = -r; lx <= r; lx++)
            for (int lz = -r; lz <= r; lz++)
                for (int ly = -2; ly <= 2; ly++) {
                    if (Math.abs(lx) + Math.abs(lz) + Math.abs(ly / 2) <= r + 1) {
                        if (chunk.getBlock(x + lx, y + h + ly, z + lz).isAir())
                            chunk.setBlock(x + lx, y + h + ly, z + lz, leaves);
                    }
                }
    }

    public void update(float dt, minicraft.entity.Player player, minicraft.entity.ParticleManager pm) {
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
                if (chunk.isDirty())
                    chunk.buildMesh(textures, this);
            }
        }
    }

    public minicraft.math.Vector3f findSafeGrassSpawn(int startX, int startZ) {
        int maxSteps = 50000;
        for (int i = 0; i < maxSteps; i++) {
            int rx = startX + (int) (Math.random() * 40000 - 20000);
            int rz = startZ + (int) (Math.random() * 40000 - 20000);
            int cx = (int) Math.floor(rx / 16.0);
            int cz = (int) Math.floor(rz / 16.0);
            cx = (cx / 64) * 64;
            cz = (cz / 64) * 64;
            rx = cx * 16;
            rz = cz * 16;
            WorldCell cell = generator.generate(rx, rz);
            if (cell.biome == Biome.MOUNTAINS || cell.biome == Biome.SNOWY_PEAKS || cell.biome == Biome.HIGHLANDS) {
                int predictedSurfaceY = (int) (cell.elevation * Chunk.HEIGHT);
                boolean isInitialSpawnChunk = (cx == 0 && cz == 0);
                if (predictedSurfaceY > 160 || isInitialSpawnChunk) {
                    WorldCell syncCell = generator.generate(cx * 16 + 8, cz * 16 + 8);
                    boolean isShipyardBiome = (syncCell.biome == Biome.MOUNTAINS || syncCell.biome == Biome.SNOWY_PEAKS
                            || syncCell.biome == Biome.HIGHLANDS);
                    if (isInitialSpawnChunk || (isShipyardBiome && (int) (syncCell.elevation * Chunk.HEIGHT) > 160)) {
                        getOrGenerate(cx, cz);
                        int peakY = getSafeSpawnY(cx * 16 + 12, cz * 16 + 12);
                        return new minicraft.math.Vector3f(cx * 16 + 15.5f, peakY + 1.0f, cz * 16 + 15.5f);
                    }
                }
            }
        }
        return new minicraft.math.Vector3f(startX, getSafeSpawnY(startX, startZ), startZ);
    }

    public int getSafeSpawnY(int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            Block b = getBlock(x, y, z);
            if (b.solid || b == Block.WATER || b == Block.ICE)
                return y + 1;
        }
        return (int) (WorldCell.SEA_LEVEL * Chunk.HEIGHT) + 2;
    }

    public void render(ShaderProgram shader, minicraft.math.Vector3f playerPos, float timeBrightness) {
        shader.setUniform("sunBrightness", timeBrightness * weatherManager.getSunBrightness());
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
        long key = packPos(x, y, z);
        if (!worldContainers.containsKey(key)) {
            minicraft.entity.Inventory inv = new minicraft.entity.Inventory();
            populateLoot(inv, x, y, z);
            worldContainers.put(key, inv);
        }
        return worldContainers.get(key);
    }

    public minicraft.entity.ProcessingFacility getFacility(int x, int y, int z) {
        return worldFacilities.computeIfAbsent(packPos(x, y, z), k -> new minicraft.entity.ProcessingFacility());
    }

    public void tick(float dt, minicraft.item.ProcessingManager pm) {
        weatherManager.update(dt);
        tickCounter++;
        if (tickCounter % 300 == 0) {
            System.out.println("WORLD: Ticking " + worldFacilities.size() + " registered facilities.");
        }

        // Iterate through all facilities using the optimized long keys
        for (Map.Entry<Long, minicraft.entity.ProcessingFacility> entry : worldFacilities.entrySet()) {
            long pos = entry.getKey();
            int fx = unpackX(pos);
            int fy = unpackY(pos);
            int fz = unpackZ(pos);

            minicraft.entity.ProcessingFacility fac = entry.getValue();
            // Delegate all facility logic to the FurnaceBlock behavioral handler
            minicraft.world.behavior.FurnaceBlock.tick(fac, this, fx, fy, fz, dt, pm);
        }
    }

    private void populateLoot(minicraft.entity.Inventory inv, int x, int y, int z) {
        Random r = new Random((long) x * 1234567L + (long) z * 7654321L);
        WorldCell cell = generator.generate(x, z);
        float elev = cell.elevation;
        // NOTE: Replace direct array access with inv.addItem() for better safety
        inv.getHotbar()[0] = new minicraft.item.ItemStack(new minicraft.item.Item("COAL"), 5 + r.nextInt(10));
        inv.getMainInventory()[0] = new minicraft.item.ItemStack(new minicraft.item.Item("IRON_ORE"), 2 + r.nextInt(5));
        if (elev > 0.45) {
            inv.getMainInventory()[1] = new minicraft.item.ItemStack(new minicraft.item.Item("GOLD_ORE"),
                    1 + r.nextInt(3));
            inv.getMainInventory()[2] = new minicraft.item.ItemStack(new minicraft.item.Item("SILVER_ORE"),
                    1 + r.nextInt(4));
        }
        if (elev > 0.75) {
            inv.getMainInventory()[3] = new minicraft.item.ItemStack(new minicraft.item.Item("DIAMOND_ORE"),
                    1 + r.nextInt(2));
        }
    }

    private long packPos(int x, int y, int z) {
        return (((long) x & 0x1FFFFF)) | (((long) y & 0x7FF) << 21) | (((long) z & 0x1FFFFF) << 32);
    }

    private int unpackX(long pos) {
        return (int) ((pos << 43) >> 43); // Sign-extended 21-bit X
    }

    private int unpackY(long pos) {
        return (int) ((pos >> 21) & 0x7FF);
    }

    private int unpackZ(long pos) {
        return (int) ((pos >> 32) << 11 >> 11); // Sign-extended 21-bit Z
    }

    public WeatherManager getWeather() {
        return weatherManager;
    }

    public float raycast(minicraft.math.Vector3f start, minicraft.math.Vector3f dir, float maxDist) {
        int stepX = (dir.x > 0) ? 1 : -1;
        int stepY = (dir.y > 0) ? 1 : -1;
        int stepZ = (dir.z > 0) ? 1 : -1;
        int x = (int) Math.floor(start.x);
        int y = (int) Math.floor(start.y);
        int z = (int) Math.floor(start.z);
        float deltaX = Math.abs(1.0f / (dir.x + 1e-6f));
        float deltaY = Math.abs(1.0f / (dir.y + 1e-6f));
        float deltaZ = Math.abs(1.0f / (dir.z + 1e-6f));
        float tMaxX = (stepX > 0) ? (x + 1 - start.x) * deltaX : (start.x - x) * deltaX;
        float tMaxY = (stepY > 0) ? (y + 1 - start.y) * deltaY : (start.y - y) * deltaY;
        float tMaxZ = (stepZ > 0) ? (z + 1 - start.z) * deltaZ : (start.z - z) * deltaZ;
        float distance = 0;
        while (distance < maxDist) {
            if (getBlock(x, y, z).solid)
                return distance;
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += deltaX;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += deltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += deltaY;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += deltaZ;
                }
            }
        }
        return maxDist;
    }

    public void cleanup() {
        chunks.values().forEach(Chunk::cleanup);
        chunks.clear();
        worldContainers.clear();
        worldFacilities.clear();
    }
}
