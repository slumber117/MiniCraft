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

    private static final int SEA_LEVEL = 102;
    private static final int MOUNTAIN_START = 256;
    private static final int SNOW_START = 512;
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

    private void spawnOres(Chunk chunk, int cx, int cz) {
        // Tier 1: Common Surface Ores (Y: 80 - 250)
        spawnOreGrip(chunk, Block.COAL_ORE, ClusterSize.LARGE, 25, 80, 250);
        spawnOreGrip(chunk, Block.IRON_ORE, ClusterSize.MEDIUM, 18, 60, 200);
        
        // --- High Altitude Resource Injection (Per Request) ---
        spawnOreGrip(chunk, Block.IRON_ORE, ClusterSize.LARGE, 20, 300, 500); // Dense Iron Belt
        spawnOreGrip(chunk, Block.GOLD_ORE, ClusterSize.MEDIUM, 12, 200, 300); // Precious Metal Belt
        spawnOreGrip(chunk, Block.TITANIUM_ORE, ClusterSize.MEDIUM, 10, 200, 300); // Industrial Metal Belt
        spawnOreGrip(chunk, Block.SAPPHIRE_ORE, ClusterSize.MEDIUM, 8, 180, 220); // High-Altitude Gems
        spawnOreGrip(chunk, Block.EMERALD_ORE, ClusterSize.MEDIUM, 8, 180, 220); // High-Altitude Gems
 
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
        spawnOreGrip(chunk, Block.EMERALD_ORE, ClusterSize.TINY, 4, 5, 45); 
        spawnOreGrip(chunk, Block.RUBY_ORE, ClusterSize.TINY, 2, 5, 50);
        spawnOreGrip(chunk, Block.SAPPHIRE_ORE, ClusterSize.TINY, 4, 5, 45);
        spawnOreGrip(chunk, Block.TITANIUM_ORE, ClusterSize.SMALL, 12, 5, 60);

        // Tier 4: Rare & Radioactive (Y: 0 - 25)
        spawnOreGrip(chunk, Block.URANIUM_ORE, ClusterSize.SMALL, 3, 2, 25);
        spawnOreGrip(chunk, Block.PLUTONIUM_ORE, ClusterSize.SMALL, 2, 2, 20);
        spawnOreGrip(chunk, Block.ADAMANTINE_ORE, ClusterSize.TINY, 1, 2, 15);
        spawnOreGrip(chunk, Block.MITHRIL_ORE, ClusterSize.TINY, 2, 2, 30);

        // Tier 5: Legendary Gems (Deepest Layers Y: 0 - 15)
        spawnOreGrip(chunk, Block.ONYX_ORE, ClusterSize.TINY, 1, 2, 12);
        spawnOreGrip(chunk, Block.ALEXANDRITE_ORE, ClusterSize.TINY, 1, 5, 20);
        spawnOreGrip(chunk, Block.OPAL_ORE, ClusterSize.TINY, 2, 5, 40);

        // Tier 6: Absolute Rarity (Deepest Core Y: 0 - 8)
        spawnOreGrip(chunk, Block.SERENDIBITE_ORE, ClusterSize.TINY, 1, 1, 5);
        spawnOreGrip(chunk, Block.GRANDIDIERITE_ORE, ClusterSize.TINY, 1, 1, 6);
        spawnOreGrip(chunk, Block.TAAFFEITE_ORE, ClusterSize.TINY, 1, 1, 7);
        spawnOreGrip(chunk, Block.MUSGRAVITE_ORE, ClusterSize.TINY, 1, 2, 8);
        spawnOreGrip(chunk, Block.PAINITE_ORE, ClusterSize.TINY, 1, 2, 10);

        // Tier 7: Rare Earth & Exotic Metals (Y: 0 - 150)
        spawnOreGrip(chunk, Block.XANTHIOSITE_ORE, ClusterSize.MEDIUM, 12, 40, 150);
        
        // Deep Rare Earths (T12-T16)
        spawnOreGrip(chunk, Block.MONAZITE_ORE, ClusterSize.SMALL, 4, 5, 40);
        spawnOreGrip(chunk, Block.BASTNAESITE_ORE, ClusterSize.SMALL, 3, 5, 35);
        spawnOreGrip(chunk, Block.XENOTIME_ORE, ClusterSize.SMALL, 3, 5, 30);
        spawnOreGrip(chunk, Block.LOPARITE_ORE, ClusterSize.SMALL, 2, 5, 25);
        spawnOreGrip(chunk, Block.TANTALITE_ORE, ClusterSize.SMALL, 2, 5, 25);

        // Deepest Rare Earths (T17-T21)
        spawnOreGrip(chunk, Block.VANADINITE_ORE, ClusterSize.SMALL, 2, 2, 20);
        spawnOreGrip(chunk, Block.GADOLINIUM_ORE, ClusterSize.SMALL, 2, 2, 18);
        spawnOreGrip(chunk, Block.TERBIUM_ORE, ClusterSize.SMALL, 1, 2, 16);
        spawnOreGrip(chunk, Block.DYSPROSIUM_ORE, ClusterSize.SMALL, 1, 2, 14);
        spawnOreGrip(chunk, Block.HOLMIUM_ORE, ClusterSize.SMALL, 1, 2, 12);

        // Bedrock Layer Rare Earths (T22-T26)
        spawnOreGrip(chunk, Block.ERBIUM_ORE, ClusterSize.TINY, 1, 1, 10);
        spawnOreGrip(chunk, Block.YTTRIUM_ORE, ClusterSize.TINY, 1, 1, 10);
        spawnOreGrip(chunk, Block.LUTETIUM_ORE, ClusterSize.TINY, 1, 1, 8);
        spawnOreGrip(chunk, Block.SAMARIUM_ORE, ClusterSize.TINY, 1, 1, 8);
        spawnOreGrip(chunk, Block.NEODYMIUM_ORE, ClusterSize.TINY, 1, 1, 6);

        // Legendary Core Metals (T27-T30)
        spawnOreGrip(chunk, Block.PRASEODYMIUM_ORE, ClusterSize.TINY, 1, 0, 5);
        spawnOreGrip(chunk, Block.CERIUM_ORE, ClusterSize.TINY, 1, 0, 4);
        spawnOreGrip(chunk, Block.LANTHANUM_ORE, ClusterSize.TINY, 1, 0, 3);
        spawnOreGrip(chunk, Block.PROMETHIUM_ORE, ClusterSize.TINY, 1, 0, 3);
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
