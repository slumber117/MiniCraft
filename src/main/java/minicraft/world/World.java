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
import minicraft.world.fortress.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Procedural world system (400 Height optimized).
 */
public class World implements IWeatherWorld {

    private static final int SEA_LEVEL = 512;
    private static final int MOUNTAIN_START = 640;
    private static final int SNOW_START = 800;
    private static final int BEDROCK_Y = 1;
    private static final int DIRT_LAYERS = 5;

    private final WorldGenerator generator;
    private final TextureRegistry textures;
    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Set<Long> pendingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Queue<long[]> generationQueue = new ConcurrentLinkedQueue<>();

    private Block getGemBlock(GemType type) {
        if (type == null) return Block.STONE;
        switch (type) {
            case DIAMOND:       return Block.DIAMOND_ORE;
            case EMERALD:       return Block.EMERALD_ORE;
            case RUBY:          return Block.RUBY_ORE;
            case TANZANITE:     return Block.TANZANITE_ORE;
            case AMETHYST:      return Block.AMETHYST_ORE;
            case AGATE:         return Block.AGATE_ORE;
            case GARNET:        return Block.GARNET_ORE;
            case TOURMALINE:    return Block.TOURMALINE_ORE;
            case OPAL:          return Block.OPAL_ORE;
            case ALEXANDRITE:   return Block.ALEXANDRITE_ORE;
            case ONYX:          return Block.ONYX_ORE;
            case PAINITE:       return Block.PAINITE_ORE;
            case MUSGRAVITE:    return Block.MUSGRAVITE_ORE;
            case TAAFFEITE:     return Block.TAAFFEITE_ORE;
            case GRANDIDIERITE: return Block.GRANDIDIERITE_ORE;
            case SERENDIBITE:   return Block.SERENDIBITE_ORE;
            default:            return Block.STONE;
        }
    }

    // FIXED: Changed String to Long to match packPos logic
    private final Map<Long, minicraft.entity.Inventory> worldContainers = new ConcurrentHashMap<>();
    private final Map<Long, minicraft.entity.ProcessingFacility> worldFacilities = new ConcurrentHashMap<>();

    private final StructureGenerator structGen = new StructureGenerator();
    private final CaveCarver caveCarver;
    private final FortressCarver fortressCarver;
    private final int renderDistance;
    private final WeatherManager weatherManager;
    private int tickCounter = 0;
    private final Random random = new Random();
    private minicraft.entity.EntityManager entityManager;

    public World(long seed, TextureRegistry textures, int renderDistance) {
        this.textures = textures;
        this.generator = new WorldGenerator(seed);
        this.caveCarver = new CaveCarver(seed);
        this.fortressCarver = new FortressCarver(seed);
        this.renderDistance = renderDistance;
        this.weatherManager = new WeatherManager(seed);
    }

    public void setEntityManager(minicraft.entity.EntityManager em) {
        this.entityManager = em;
    }

    public minicraft.entity.Inventory getOrCreateContainer(int x, int y, int z) {
        long key = packPos(x, y, z);
        return worldContainers.computeIfAbsent(key, k -> new minicraft.entity.Inventory());
    }

    private long packPos(int x, int y, int z) {
        return ((long) x & 0xFFFFFFL) | (((long) y & 0xFFFFL) << 24) | (((long) z & 0xFFFFFFL) << 40);
    }

    @Override
    public long getSeed() { return generator.getSeed(); }

    @Override
    public List<minicraft.entity.Entity> getEntitiesInBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        if (entityManager == null) return new ArrayList<>();
        return entityManager.getEntitiesInBox(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public void damageEntity(minicraft.entity.Entity e, float damage) {
        if (e != null) e.damage(damage, null);
    }

    @Override
    public int getSurfaceY(int x, int z) {
        Chunk chunk = chunks.get(key(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH)));
        if (chunk != null) {
            for (int y = minicraft.world.Chunk.HEIGHT - 1; y >= 0; y--) {
                if (chunk.getBlock(Math.floorMod(x, Chunk.WIDTH), y, Math.floorMod(z, Chunk.DEPTH)) != Block.AIR) return y;
            }
        }
        // Fallback to generator's analytical elevation if chunk is not yet in the map
        return (int) (generator.generate(x, z).elevation * Chunk.HEIGHT);
    }

    @Override
    public List<minicraft.world.IWeatherEntity> getEntitiesInRadius(float x, float y, float z, float radius) {
        if (entityManager == null) return new ArrayList<>();
        List<minicraft.world.IWeatherEntity> out = new ArrayList<>();
        for (minicraft.entity.Entity e : entityManager.getNearby(x, y, z, radius)) {
            out.add(e);
        }
        return out;
    }

    @Override
    public boolean isFlammable(int x, int y, int z) {
        Block b = getBlock(x, y, z);
        return b != null && (b == Block.WOOD || b == Block.OAK_WOOD || b == Block.LEAVES || b == Block.GRASS);
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return getBlock(x, y, z) == Block.AIR;
    }

    @Override
    public void setFire(int x, int y, int z) {
        setBlock(x, y, z, Block.FIRE);
    }

    @Override
    public boolean isWater(int x, int y, int z) {
        return getBlock(x, y, z) == Block.WATER;
    }

    @Override
    public boolean setIce(int x, int y, int z) {
        if (getBlock(x, y, z) == Block.WATER) {
            setBlock(x, y, z, Block.ICE);
            return true;
        }
        return false;
    }

    @Override
    public void meltIce(int x, int y, int z) {
        if (getBlock(x, y, z) == Block.ICE) {
            setBlock(x, y, z, Block.WATER);
        }
    }

    @Override
    public int getMaxY() {
        return minicraft.world.Chunk.HEIGHT;
    }

    @Override
    public boolean isWindDestructible(int x, int y, int z) {
        Block b = getBlock(x, y, z);
        if (b == null) return false;
        // Small things easily blown away
        return b == Block.TALL_GRASS || b == Block.FLOWER_RED || b == Block.FLOWER_BLUE || b == Block.MUSHROOM || b == Block.LEAVES;
    }

    @Override
    public void destroyBlock(int x, int y, int z, boolean dropItems) {
        // Logic for destroying a block via weather (usually replaces with air)
        setBlock(x, y, z, Block.AIR);
    }

    @Override
    public List<minicraft.world.IWeatherEntity> getAllEntities() {
        if (entityManager == null) return new ArrayList<>();
        List<minicraft.world.IWeatherEntity> out = new ArrayList<>();
        for (minicraft.entity.Entity e : entityManager.getAll()) {
            out.add(e);
        }
        return out;
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

    public Chunk requestChunk(int cx, int cz) {
        Chunk chunk = chunks.get(key(cx, cz));
        if (chunk != null) return chunk;

        long k = key(cx, cz);
        if (!pendingChunks.contains(k)) {
            pendingChunks.add(k);
            generationQueue.add(new long[]{cx, cz});
        }
        return null;
    }

    public void processGeneration(int cx, int cz) {
        long k = key(cx, cz);
        if (chunks.containsKey(k)) {
            pendingChunks.remove(k);
            return;
        }

        Chunk chunk = generate(cx, cz);
        chunks.put(k, chunk);
        pendingChunks.remove(k);
    }

    public Queue<long[]> getGenerationQueue() {
        return generationQueue;
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(key(cx, cz));
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
                    else if (y == surfaceY) {
                        if (y < seaLevelY) {
                            // Use underwater materials if below sea level: SAND for oceans, STONE/SAND for others
                            b = (cell.biome == Biome.OCEAN || cell.biome == Biome.FROZEN_OCEAN) ? Block.SAND : (Math.random() < 0.5 ? Block.STONE : Block.SAND);
                        } else {
                            b = getSurfaceBlock(cell.biome);
                        }
                    }
                    else if (y >= surfaceY - DIRT_LAYERS)
                        b = getFillerBlock(cell.biome);
                    else {
                        boolean isDeep = y < (SEA_LEVEL - 100);
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
                    // ── Fortress Carving ───────────────────────────────────────────
                    FortressCell fortressCell = fortressCarver.query(gx, y, gz, this);
                    if (fortressCell.layer != FortressCell.Layer.OUTSIDE) {
                        switch (fortressCell.layer) {
                            case INTERIOR:
                                chunk.setBlock(x, y, z, Block.AIR);
                                break;
                            case WALL:
                                chunk.setBlock(x, y, z, Block.FORTRESS_WALL);
                                break;
                            case FLOOR:
                                chunk.setBlock(x, y, z, Block.FORTRESS_FLOOR);
                                break;
                            case CEILING:
                                chunk.setBlock(x, y, z, Block.FORTRESS_CEILING);
                                break;
                            default:
                                chunk.setBlock(x, y, z, Block.OBSIDIAN);
                                break;
                        }
                        continue; // Fortress overrides caves
                    }

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

                        if (caveCell.type == minicraft.world.cave.CaveType.UNDERWATER) b = Block.WATER;
                        else if (y < 50 && Math.random() < 0.05 && caveCell.type != minicraft.world.cave.CaveType.GEODE_HOLLOW)
                            b = Block.LAVA;
                        
                        chunk.setBlock(x, y, z, b);
                    } else if (y < 50 && Math.random() < 0.03) {
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
        
        // --- Boss Arena Generation ---
        applyDragonArenas(chunk);

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
                    else if (Math.random() < 0.35f) {
                        double f = Math.random();
                        if (f < 0.43) // ~15% total chance (0.35 * 0.43 = 0.15)
                            chunk.setBlock(x, sY, z, Block.FIBRE_BUSH);
                        else if (f < 0.70)
                            chunk.setBlock(x, sY, z, Block.TALL_GRASS);
                        else if (f < 0.80)
                            chunk.setBlock(x, sY, z, Block.FLOWER_RED);
                        else if (f < 0.90)
                            chunk.setBlock(x, sY, z, Block.FLOWER_BLUE);
                        else
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

    private void applyDragonArenas(Chunk chunk) {
        int cx = chunk.chunkX;
        int cz = chunk.chunkZ;
        
        // Procedural Boss Arena Grid (Every 128x128 chunks ≈ 2048x2048 blocks)
        // Spaced out more for the massive 1024 world
        int gridSize = 128;
        int gx = Math.floorDiv(cx, gridSize) * gridSize + (gridSize / 2);
        int gz = Math.floorDiv(cz, gridSize) * gridSize + (gridSize / 2);
        
        // Deterministic seed for this grid cell
        long seed = (long)gx * 3123456789L + (long)gz * 123456789L + 777; 
        java.util.Random r = new java.util.Random(seed);
        
        if (r.nextFloat() < 0.8f) { // 80% chance a grid cell has a boss
            int bossType = r.nextInt(5); // 0-4 for different dragons
            
            // Check biome at the potential center
            WorldCell centerCell = generator.generate(gx * 16 + 8, gz * 16 + 8);
            int arenaY = 750 + r.nextInt(150); // Floating in the sky (Y: 750 - 900)
            
            switch(bossType) {
                case 0: // Gold Dragon (Mountains)
                    if (centerCell.biome == Biome.MOUNTAINS)
                        applyDomeArena(chunk, gx * 16 + 8, arenaY, gz * 16 + 8, 50, 40, Block.GOLD_BLOCK, false);
                    break;
                case 1: // Onyx Dragon (Desert)
                    if (centerCell.biome == Biome.DESERT)
                        applyDomeArena(chunk, gx * 16 + 8, arenaY, gz * 16 + 8, 60, 45, Block.ONYX_BLOCK, false);
                    break;
                case 2: // Fire Dragon (Magma/Volcanic)
                    if (centerCell.temperature > 0.8f)
                        applyDomeArena(chunk, gx * 16 + 8, arenaY, gz * 16 + 8, 55, 35, Block.MAGMA, true);
                    break;
                case 3: // Ice Dragon (Tundra/Cold)
                    if (centerCell.temperature < 0.2f)
                        applyDomeArena(chunk, gx * 16 + 8, arenaY, gz * 16 + 8, 50, 40, Block.ICE, false);
                    break;
                case 4: // Earth Dragon (Forests)
                    if (centerCell.biome == Biome.FOREST || centerCell.biome == Biome.JUNGLE)
                        applyDomeArena(chunk, gx * 16 + 8, arenaY, gz * 16 + 8, 55, 35, Block.OAK_WOOD, true); // Wood and Dirt (handled in applyDome)
                    break;
            }
        }
    }

    private void applyDomeArena(Chunk chunk, int centerX, int arenaY, int centerZ, int radius, int height, Block wallMaterial, boolean hasGate) {
        int cx = chunk.chunkX * 16;
        int cz = chunk.chunkZ * 16;
        
        // Quick AABB check
        if (cx > centerX + radius + 10 || cx + 16 < centerX - radius - 10) return;
        if (cz > centerZ + radius + 10 || cz + 16 < centerZ - radius - 10) return;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cx + x;
                int wz = cz + z;
                int dx = wx - centerX;
                int dz = wz - centerZ;
                
                float distSq = dx * dx + dz * dz;
                if (distSq <= (radius + 5) * (radius + 5)) {
                    for (int y = arenaY; y <= arenaY + height + 5; y++) {
                        if (y >= Chunk.HEIGHT) continue;
                        float dy = y - arenaY;
                        float val = distSq / (radius * radius) + (dy * dy) / (height * height);
                        
                        if (val <= 1.0f) {
                            chunk.setBlock(x, y, z, Block.AIR);
                        } else if (val <= 1.2f) {
                            Block mat = wallMaterial;
                            // Earth Arena Special: Alternating Dirt and Wood
                            if (wallMaterial == Block.OAK_WOOD && (y % 2 == 0)) mat = Block.DIRT;
                            chunk.setBlock(x, y, z, mat);
                        }
                    }
                    
                    if (hasGate && dx >= radius - 2 && dx <= radius + 5 && Math.abs(dz) <= 3) {
                         for (int y = arenaY + 1; y <= arenaY + 6; y++) {
                             if (y < Chunk.HEIGHT) chunk.setBlock(x, y, z, Block.BOSS_GATE);
                         }
                    }
                }
            }
        }
    }

    private void spawnOres(Chunk chunk, int cx, int cz) {
        // --- Tier 1: Surface & Highlands (Y: 750 - 1024) ---
        spawnOreGrip(chunk, Block.IRON_ORE, ClusterSize.LARGE, 15, 750, 1024);
        spawnOreGrip(chunk, Block.COAL_ORE, ClusterSize.LARGE, 25, 700, 1024);
        spawnOreGrip(chunk, Block.COPPER_ORE, ClusterSize.LARGE, 15, 750, 950);
        spawnOreGrip(chunk, Block.TIN_ORE, ClusterSize.MEDIUM, 12, 750, 950);

        // --- Tier 2: Shallow Crust (Y: 600 - 800) ---
        spawnOreGrip(chunk, Block.SILVER_ORE, ClusterSize.MEDIUM, 10, 600, 800);
        spawnOreGrip(chunk, Block.QUARTZ_ORE, ClusterSize.MEDIUM, 15, 600, 800);
        spawnOreGrip(chunk, Block.NICKEL_ORE, ClusterSize.SMALL, 10, 600, 800);

        // --- Tier 3: Sub-Surface Transition (Y: 450 - 650) ---
        // Gold now spans across the Layer 500 boundary
        spawnOreGrip(chunk, Block.GOLD_ORE, ClusterSize.MEDIUM, 15, 450, 650);
        spawnOreGrip(chunk, Block.TITANIUM_ORE, ClusterSize.MEDIUM, 10, 450, 650);
        spawnOreGrip(chunk, Block.PLATINUM_ORE, ClusterSize.SMALL, 6, 450, 650);

        // --- Tier 4: Upper-Crust Gems (Y: 350 - 550) ---
        spawnOreGrip(chunk, Block.DIAMOND_ORE, ClusterSize.SMALL, 12, 350, 550);
        spawnOreGrip(chunk, Block.SAPPHIRE_ORE, ClusterSize.SMALL, 12, 350, 550);
        spawnOreGrip(chunk, Block.JADE_ORE, ClusterSize.SMALL, 10, 350, 550);
        spawnOreGrip(chunk, Block.AMETHYST_ORE, ClusterSize.SMALL, 10, 350, 550);
        spawnOreGrip(chunk, Block.AQUAMARINE_ORE, ClusterSize.SMALL, 10, 350, 550);

        // --- Tier 5: Mid-Crust Exotic Gems (Y: 250 - 450) ---
        spawnOreGrip(chunk, Block.EMERALD_ORE, ClusterSize.SMALL, 12, 250, 450);
        spawnOreGrip(chunk, Block.TOPAZ_ORE, ClusterSize.SMALL, 12, 250, 450);
        spawnOreGrip(chunk, Block.PERIDOT_ORE, ClusterSize.SMALL, 10, 250, 450);
        spawnOreGrip(chunk, Block.RUBY_ORE, ClusterSize.SMALL, 8, 250, 450);
        spawnOreGrip(chunk, Block.TANZANITE_ORE, ClusterSize.SMALL, 6, 250, 450);
        spawnOreGrip(chunk, Block.OPAL_ORE, ClusterSize.TINY, 4, 250, 450);

        // --- Tier 6: Radioactive Stratum (Y: 150 - 350) ---
        spawnOreGrip(chunk, Block.URANIUM_ORE, ClusterSize.SMALL, 8, 150, 350);
        spawnOreGrip(chunk, Block.PLUTONIUM_ORE, ClusterSize.TINY, 6, 150, 350);
        spawnOreGrip(chunk, Block.ORICHALCUM_ORE, ClusterSize.MEDIUM, 4, 150, 350);
        spawnOreGrip(chunk, Block.NEPTUNIUM_ORE, ClusterSize.MEDIUM, 3, 150, 350);

        // --- Tier 7: Deep Abyssal Crust (Y: 80 - 200) ---
        spawnOreGrip(chunk, Block.ADAMANTINE_ORE, ClusterSize.TINY, 6, 80, 200);
        spawnOreGrip(chunk, Block.TAAFFEITE_ORE, ClusterSize.TINY, 6, 80, 200);
        spawnOreGrip(chunk, Block.ALEXANDRITE_ORE, ClusterSize.TINY, 10, 80, 200);

        // --- Tier 8: Mantle Transition (Y: 40 - 100) ---
        spawnOreGrip(chunk, Block.AGATE_ORE, ClusterSize.TINY, 5, 40, 100);
        spawnOreGrip(chunk, Block.GARNET_ORE, ClusterSize.TINY, 5, 40, 100);
        spawnOreGrip(chunk, Block.SERENDIBITE_ORE, ClusterSize.TINY, 4, 40, 100);
        spawnOreGrip(chunk, Block.PYRITE_ORE, ClusterSize.TINY, 4, 40, 100);
        spawnOreGrip(chunk, Block.GRANDIDIERITE_ORE, ClusterSize.TINY, 3, 40, 100);
        spawnOreGrip(chunk, Block.MUSGRAVITE_ORE, ClusterSize.TINY, 3, 40, 100);

        // --- Tier 9: Pre-Bedrock Depth (Y: 15 - 50) ---
        spawnOreGrip(chunk, Block.PAINITE_ORE, ClusterSize.TINY, 6, 15, 50);

        // --- Tier 10: Bedrock Floor (Y: 5 - 25) ---
        spawnOreGrip(chunk, Block.ONYX_ORE, ClusterSize.TINY, 4, 5, 25);
        spawnOreGrip(chunk, Block.MITHRIL_ORE, ClusterSize.TINY, 4, 5, 25);
        spawnOreGrip(chunk, Block.XENOTIME_ORE, ClusterSize.SMALL, 6, 5, 25);
        spawnOreGrip(chunk, Block.BASTNAESITE_ORE, ClusterSize.SMALL, 6, 5, 25);

        // --- Tier 11+: The Deepest Zenith (Y: 0 - 10) ---
        spawnOreGrip(chunk, Block.PROMETHIUM_ORE, ClusterSize.TINY, 3, 0, 10);
        spawnOreGrip(chunk, Block.LANTHANUM_ORE, ClusterSize.TINY, 3, 0, 10);
        spawnOreGrip(chunk, Block.CERIUM_ORE, ClusterSize.TINY, 3, 0, 10);
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

    public void update(float dt, minicraft.entity.Player player, minicraft.entity.ParticleManager pm, minicraft.entity.EntityManager em) {
        int px = (int) player.position.x;
        int py = (int) player.position.y;
        int pz = (int) player.position.z;
        int r = 12;
        if (new java.util.Random().nextInt(4) == 0) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Block b = getBlock(px + dx, py + dy, pz + dz);
                        if (b != null && (b == Block.TORCH || b.name().endsWith("_TORCH"))) {
                            pm.spawnSmoke(px + dx + 0.5f, py + dy + 0.6f, pz + dz + 0.5f);
                            
                            // Radiation logic
                            if (b == Block.URANIUM_TORCH || b == Block.PLUTONIUM_TORCH) {
                                float damage = (b == Block.PLUTONIUM_TORCH) ? 1.5f : 0.5f;
                                float radRadius = (b == Block.PLUTONIUM_TORCH) ? 6.0f : 4.0f;
                                
                                List<minicraft.entity.Entity> nearby = em.getNearby(px + dx + 0.5f, py + dy + 0.5f, pz + dz + 0.5f, radRadius);
                                for (minicraft.entity.Entity e : nearby) {
                                    if (e instanceof minicraft.entity.Player) continue;
                                    e.damage(damage, null);
                                }
                                
                                // Spawn green/orange sparks for radiation
                                if (Math.random() < 0.3) {
                                    pm.spawnRadiationSpark(px + dx + 0.5f, py + dy + 0.8f, pz + dz + 0.5f, 
                                        b == Block.URANIUM_TORCH ? new minicraft.math.Vector3f(0.2f, 1.0f, 0.2f) : new minicraft.math.Vector3f(1.0f, 0.5f, 0.1f));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void update(int cx, int cz, float dt, minicraft.entity.Player player) {
        weatherManager.update(dt, this, player.position.x, player.position.z, 64f);
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                Chunk chunk = requestChunk(cx + dx, cz + dz);
                if (chunk != null && chunk.isDirty())
                    chunk.buildMesh(textures, this);
            }
        }
    }

    public minicraft.math.Vector3f findSafeGrassSpawn(int startX, int startZ) {
        // Optimized for AI: Scan only local area first to save on CPU inference
        int maxSteps = 10; 
        for (int i = 0; i < maxSteps; i++) {
            int rx = startX + (int) (Math.random() * 200 - 100);
            int rz = startZ + (int) (Math.random() * 200 - 100);
            WorldCell cell = generator.generate(rx, rz);
            
            // Look for non-ocean
            if (cell.elevation > 0.45f) {
                return new minicraft.math.Vector3f(rx, (float)(cell.elevation * Chunk.HEIGHT) + 2.0f, rz);
            }
        }
        // Fallback to center if no high ground found quickly
        return new minicraft.math.Vector3f(startX, 250.0f, startZ);
    }

    public int getSafeSpawnY(int x, int z) {
        // Standard sky-down scan for surface spawn
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            Block b = getBlock(x, y, z);
            if (b.solid || b == Block.WATER || b == Block.ICE)
                return y + 1;
        }
        return (int) (WorldCell.SEA_LEVEL * Chunk.HEIGHT) + 2;
    }

    /**
     * Finds a safe Y coordinate near the preferred height.
     * Prevents surface teleportation during deep-mine respawns.
     */
    public int getSafeSpawnY(int x, int preferredY, int z) {
        // Clamp preferredY
        int startY = Math.max(1, Math.min(Chunk.HEIGHT - 3, preferredY));
        
        // 1. Search locally (up/down 15 blocks)
        for (int dy = 0; dy <= 15; dy++) {
            // Check current, then above, then below
            int[] targets = {startY + dy, startY - dy};
            for (int y : targets) {
                if (y <= 0 || y >= Chunk.HEIGHT - 1) continue;
                
                Block b = getBlock(x, y, z);
                if (b.solid || b == Block.WATER || b == Block.ICE) {
                    // Potential floor found, check space for player (2 blocks high)
                    if (!getBlock(x, y + 1, z).solid && !getBlock(x, y + 2, z).solid) {
                        return y + 1;
                    }
                }
            }
        }

        // 2. If local search fails, fallback to standard surface scan
        return getSafeSpawnY(x, z);
    }

    /**
     * Finds the nearest safe spawn position on LAND (not water/ice).
     */
    public minicraft.math.Vector3f findSafeSpawn() {
        Random rand = new Random();
        
        // Pick a single random tile to prevent hammering the ML inference pipeline
        int tileX = (rand.nextInt(20) - 10) * 256;
        int tileZ = (rand.nextInt(20) - 10) * 256;
        
        // Extended search within the cached tile to ensure a land-locked start
        for (int i = 0; i < 2000; i++) {
            int rx = tileX + rand.nextInt(256);
            int rz = tileZ + rand.nextInt(256);
            
            // Skip the origin zone entirely to avoid factory platform interference
            if (Math.abs(rx) < 150 && Math.abs(rz) < 150) continue;
            
            WorldCell cell = generator.generate(rx, rz);
            
            // 1. Extreme Land Check: 
            // - Elevation 0.35 is double the sea level (0.18), roughly 100 blocks above water.
            // - Continentalness 0.4 ensures we are deep inland, away from coastal instability.
            if (cell.isWater || cell.elevation < 0.35f || cell.continentalness < 0.4f) continue;
            
            // 2. Height Calculation
            float surfaceY = cell.elevation * Chunk.HEIGHT;
            
            // 3. Stability Guard: Avoid very high mountains or platforms
            if (surfaceY > 380.0f) continue;
            
            System.out.println("WORLD: Verified safe interior spawn at (" + rx + ", " + rz + ") Elevation: " + cell.elevation);
            // Spawn at surface level to prevent being too high to see terrain
            return new minicraft.math.Vector3f(rx + 0.5f, surfaceY + 1.0f, rz + 0.5f);
        }
        
        // Emergency Deep Inland Fallback
        System.out.println("WORLD: Extreme search failed, using deep interior fallback.");
        WorldCell fallbackCell = generator.generate(2048, 2048);
        float fallbackSurfaceY = fallbackCell.elevation * Chunk.HEIGHT;
        return new minicraft.math.Vector3f(2048.5f, fallbackSurfaceY + 1.0f, 2048.5f);
    }

    public void render(ShaderProgram shader, minicraft.math.Vector3f playerPos, float timeBrightness) {
        shader.setUniform("sunBrightness", timeBrightness * weatherManager.getSunBrightness());
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

    public void tick(float dt, minicraft.entity.Player player, minicraft.item.ProcessingManager pm) {
        weatherManager.update(dt, this, player.position.x, player.position.z, 64f);
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


    private int unpackX(long pos) {
        return (int) ((pos << 40) >> 40); // Sign-extend 24-bit X
    }

    private int unpackY(long pos) {
        return (int) ((pos >> 24) & 0xFFFF);
    }

    private int unpackZ(long pos) {
        return (int) (pos >> 40); // Sign-extend 24-bit Z
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
