package minicraft.world;

import minicraft.world.cave.CaveCarver;
import minicraft.world.cave.CaveCell;
import minicraft.world.cave.CaveType;
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
    private final Map<String, minicraft.entity.ProcessingFacility> worldFacilities = new HashMap<>();
    private final StructureGenerator structGen = new StructureGenerator();
    private final CaveCarver caveCarver;
    private final int renderDistance; 
    private final WeatherManager weatherManager = new WeatherManager();
    private final Random random = new Random();

    public World(long seed, TextureRegistry textures, int renderDistance) {
        this.textures = textures;
        this.generator = new WorldGenerator(seed);
        this.caveCarver = new CaveCarver(seed);
        this.renderDistance = renderDistance;
    }

    public WorldGenerator getGenerator() { return generator; }
    public WeatherManager getWeatherManager() { return weatherManager; }


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
        int cx = Math.floorDiv(gx, Chunk.WIDTH);
        int cz = Math.floorDiv(gz, Chunk.DEPTH);
        int lx = Math.floorMod(gx, Chunk.WIDTH);
        int lz = Math.floorMod(gz, Chunk.DEPTH);

        Chunk chunk = getOrGenerate(cx, cz);
        chunk.setBlock(lx, gy, lz, b);

        // Mark neighbor chunks as dirty if on boundary
        if (lx == 0) markChunkDirty(cx - 1, cz);
        if (lx == Chunk.WIDTH - 1) markChunkDirty(cx + 1, cz);
        if (lz == 0) markChunkDirty(cx, cz - 1);
        if (lz == Chunk.DEPTH - 1) markChunkDirty(cx, cz + 1);
    }

    private void markChunkDirty(int cx, int cz) {
        Chunk neighbor = chunks.get(key(cx, cz));
        if (neighbor != null) neighbor.markDirty();
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
        
        // Prepare unified cave system for this chunk
        int centerSurfaceY = (int)(region[8][8].elevation * Chunk.HEIGHT);
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

                // CAVE SYSTEM
                for (int y = BEDROCK_Y + 1; y < surfaceY; y++) {
                    CaveCell caveCell = caveCarver.query(gx, y, gz, cell, surfaceY);
                    if (caveCell.isCarved) {
                        Block b = (caveCell.type == CaveType.UNDERWATER) ? Block.WATER : Block.AIR;
                        chunk.setBlock(x, y, z, b);
                    }
                }
            }
        }

        // STRUCTURES & RE-POPULATION
        
        // 1. Guaranteed Shipyards on Highest Mountain Peaks (Centered Check)
        WorldCell centerCell = generator.generate(cx * 16 + 8, cz * 16 + 8);
        
        // Calculate peak locally since the chunk isn't in the global map yet
        int centerPeakY = 0;
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            if (chunk.getBlock(8, y, 8).solid) { centerPeakY = y + 1; break; }
        }
        
        // Isolate Shipyard megastructures to a 64x64 chunk geographical grid (1 per massive continent mapping)
        // FORCE a shipyard at (0,0) to ensure the player always has a way to the ship deck
        boolean isInitialSpawn = (cx == 0 && cz == 0);
        boolean isPeak = (centerCell.biome == Biome.MOUNTAINS || centerCell.biome == Biome.SNOWY_PEAKS || centerCell.biome == Biome.HIGHLANDS) && centerPeakY > 160;

        if (isInitialSpawn || (cx % 64 == 0 && cz % 64 == 0 && isPeak)) {
            // Build the shipyard safely above all possible mountain layers (Max peak = ~220)
            int targetY = Math.max(240, centerPeakY + 20);
            structGen.generateFloatingFactory(chunk, targetY, centerPeakY);
        }

        // 2. Random Fortresses, Castles, and Villages (8% chance)
        // Use random offset for organic placement
        int sx = random.nextInt(8) + 4;
        int sz = random.nextInt(8) + 4;
        WorldCell scell = generator.generate(sx + cx * 16, sz + cz * 16);
        int structY = getSafeSpawnY(sx + cx * 16, sz + cz * 16);

        if (Math.random() < 0.08) {
             if (structY > seaLevelY + 5 && structY < Chunk.HEIGHT - 40) {
                 if (scell.biome == Biome.SAVANNA || scell.biome == Biome.GRASSLAND) {
                     structGen.generateVillage(chunk, sx, structY, sz, scell.biome);
                 } else if (scell.biome == Biome.MOUNTAINS || scell.biome == Biome.SNOWY_PEAKS) {
                     structGen.generateFortress(chunk, sx, structY, sz, scell.biome);
                 } else {
                     structGen.generateCastle(chunk, sx, structY, sz, scell.biome);
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
                    
                    // CRITICAL: Prevent trees from spawning on water or floating in air
                    if (ground == Block.WATER || ground == Block.ICE || ground == Block.AIR) continue;

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
        int maxSteps = 50000; // Massive search to guarantee finding a mountain continent
        for (int i = 0; i < maxSteps; i++) {
            // Expand search radius to 20,000 blocks
            int rx = startX + (int)(Math.random() * 40000 - 20000);
            int rz = startZ + (int)(Math.random() * 40000 - 20000);
            
            // Fast math: snap mathematically to the deterministic 64x64 chunk grid constraint (1024x1024 blocks)
            int cx = (int)Math.floor(rx / 16.0);
            int cz = (int)Math.floor(rz / 16.0);
            cx = (cx / 64) * 64; 
            cz = (cz / 64) * 64;
            rx = cx * 16;
            rz = cz * 16;
            
            // Fast mathematical check using the noise engine
            WorldCell cell = generator.generate(rx, rz);
            if (cell.biome == Biome.MOUNTAINS || cell.biome == Biome.SNOWY_PEAKS || cell.biome == Biome.HIGHLANDS) {
                // Calculate height natively
                int predictedSurfaceY = (int) (cell.elevation * Chunk.HEIGHT);
                
                // If it's a high peak OR the initial spawn chunk (0,0), a shipyard is guaranteed to build here
                boolean isInitialSpawnChunk = (cx == 0 && cz == 0);
                if (predictedSurfaceY > 160 || isInitialSpawnChunk) {
                    // Center the check just like chunk generation does!
                    WorldCell syncCell = generator.generate(cx * 16 + 8, cz * 16 + 8);
                    boolean isShipyardBiome = (syncCell.biome == Biome.MOUNTAINS || syncCell.biome == Biome.SNOWY_PEAKS || syncCell.biome == Biome.HIGHLANDS);
                    
                    if (isInitialSpawnChunk || (isShipyardBiome && (int)(syncCell.elevation * Chunk.HEIGHT) > 160)) {
                        
                        // Force the chunk to exist before the player spawns into it
                        getOrGenerate(cx, cz);
                        System.out.println("Spawn System: Shipyard Target Locked! Coords: " + (cx*16) + ", " + (cz*16));
                        
                        // Calculate exact ground elevation at portal location
                        int peakY = getSafeSpawnY(cx * 16 + 12, cz * 16 + 12);
                        
                        // Spawn player slightly adjacent to the ground portal (15.5)
                        return new minicraft.math.Vector3f(cx * 16 + 15.5f, peakY + 1.0f, cz * 16 + 15.5f);
                    }
                }
            }
        }
        // Fallback to original grass logic
        for (int i = 0; i < 100; i++) {
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

    public int getSafeSpawnY(int x, int z) {
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

    public minicraft.entity.ProcessingFacility getFacility(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        return worldFacilities.computeIfAbsent(key, k -> new minicraft.entity.ProcessingFacility());
    }

    public void tick(float dt, minicraft.item.ProcessingManager pm) {
        weatherManager.update(dt);
        
        // Update Facilities (Furnaces/Cookers)
        for (Map.Entry<String, minicraft.entity.ProcessingFacility> entry : worldFacilities.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int fx = Integer.parseInt(parts[0]);
            int fy = Integer.parseInt(parts[1]);
            int fz = Integer.parseInt(parts[2]);
            
            Block b = getBlock(fx, fy, fz);
            if (b != Block.FURNACE && b != Block.COOKER) continue;
            
            minicraft.entity.ProcessingFacility fac = entry.getValue();
            boolean isCooker = (b == Block.COOKER);
            
            // 1. Handle Fuel Consumption
            if (fac.remainingFuelTime <= 0) {
                minicraft.item.ItemStack fuelStack = fac.getSlot(1);
                if (fuelStack != null) {
                    float fuelVal = pm.getFuelTime(fuelStack.getItem().getName(), isCooker);
                    if (fuelVal > 0) {
                        fac.remainingFuelTime = fuelVal;
                        fac.maxFuelTime = fuelVal;
                        fuelStack.remove(1);
                        if (fuelStack.getCount() <= 0) fac.setSlot(1, null);
                    }
                }
            }

            // 2. Handle Processing
            minicraft.item.ItemStack input = fac.getSlot(0);
            if (input != null && fac.remainingFuelTime > 0) {
                minicraft.item.Recipe res = isCooker ? pm.getCookerResult(input.getItem().getName()) : pm.getFurnaceResult(input.getItem().getName());
                
                if (res != null) {
                    // Check if output slot is compatible
                    minicraft.item.ItemStack output = fac.getSlot(2);
                    if (output == null || (output.getItem().equals(res.getResult()) && output.getCount() < 64)) {
                        fac.isActive = true;
                        fac.processProgress += dt / pm.getProcessTime(input.getItem().getName());
                        fac.remainingFuelTime -= dt;

                        if (fac.processProgress >= 1.0f) {
                            input.remove(1);
                            if (input.getCount() <= 0) fac.setSlot(0, null);
                            
                            if (output == null) {
                                fac.setSlot(2, new minicraft.item.ItemStack(res.getResult(), res.getResultCount()));
                            } else {
                                output.add(res.getResultCount());
                            }
                            fac.processProgress = 0;
                        }
                    } else {
                        fac.isActive = false;
                        fac.processProgress = 0;
                    }
                } else {
                    fac.isActive = false;
                    fac.processProgress = 0;
                }
            } else {
                fac.isActive = false;
                fac.processProgress = 0;
                if (fac.remainingFuelTime > 0) fac.remainingFuelTime -= dt * 0.5f; // Passive fuel drain
            }
        }
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

    /**
     * Voxel raycast to find the first solid block in a direction.
     * @return The distance to the hit, or maxDist if no hit.
     */
    public float raycast(minicraft.math.Vector3f start, minicraft.math.Vector3f dir, float maxDist) {
        float step = 0.2f;
        for (float d = 0; d < maxDist; d += step) {
            int x = (int) Math.floor(start.x + dir.x * d);
            int y = (int) Math.floor(start.y + dir.y * d);
            int z = (int) Math.floor(start.z + dir.z * d);
            if (getBlock(x, y, z).solid) return d;
        }
        return maxDist;
    }

    public void cleanup() { chunks.values().forEach(Chunk::cleanup); chunks.clear(); worldContainers.clear(); }
}
