package minicraft.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import minicraft.entity.ship.ShipEntity;

/**
 * Manages all active entities in the world.
 * Handles ticking, spawning, removal, and spatial queries.
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final Random rng = new Random();

    /** Time accumulator for fixed-rate ticking (20 ticks/sec). */
    private float tickAccum = 0f;
    private static final float TICK_RATE = 1f / 20f;

    // ─────────────────────────────────────────────────────────────────────
    //  Main update
    // ─────────────────────────────────────────────────────────────────────

    public void update(float dt, minicraft.world.World world, minicraft.entity.ParticleManager particleManager) {
        tickAccum += dt;
        while (tickAccum >= TICK_RATE) {
            tickAccum -= TICK_RATE;
            tick(TICK_RATE, world, particleManager);
            spawnHostilesInDarkness(world);
            handleBlockAuras(world, particleManager);
        }
    }

    private void tick(float dt, minicraft.world.World world, minicraft.entity.ParticleManager particleManager) {
        // Use a copy to avoid ConcurrentModificationException if entities spawn during tick
        List<Entity> toTick = new ArrayList<>(entities);
        for (Entity e : toTick) {
            if (!e.isDead()) {
                e.tick(this, world, particleManager, dt);
                if (e.justHit) {
                    particleManager.spawnDamage(e.position.x, e.position.y, e.position.z, new minicraft.math.Vector4f(1, 0.1f, 0.1f, 1));
                    e.justHit = false;
                }
            }
        }
        // Remove dead entities
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e.isDead()) {
                e.onDeath(this, world);
                it.remove();
            }
        }
    }
    private void spawnHostilesInDarkness(minicraft.world.World world) {
        // Only spawn occasionally
        if (entities.size() > 100 || rng.nextFloat() < 0.98f) return;

        // Pick a random player to spawn near
        Player p = null;
        for (Entity e : entities) if (e instanceof Player) { p = (Player) e; break; }
        if (p == null) return;

        // Try a few spots around player
        for (int i = 0; i < 5; i++) {
            float sx = p.position.x + (rng.nextFloat() * 40 - 20);
            float sz = p.position.z + (rng.nextFloat() * 40 - 20);
            float sy = p.position.y + (rng.nextFloat() * 10 - 5);
            
            int gx = (int) Math.floor(sx);
            int gy = (int) Math.floor(sy);
            int gz = (int) Math.floor(sz);

            // Light & Depth check
            if (world.getLight(gx, gy, gz) < 0.2f) {
                if (gy < 60) {
                    // Underground spawns
                    if (world.getBlock(gx, gy, gz) == minicraft.world.Block.AIR) {
                        // Check for Procedural Boss Arenas
                        if (checkBossSpawn(gx, gy, gz, world)) return;
                        
                        if (gy < 15 && rng.nextFloat() < 0.01f) {
                            spawnAt(EntityType.FIRE_DEMON, gx + 0.5f, gy, gz + 0.5f);
                        } else {
                            EntityType type = rng.nextFloat() < 0.7f ? EntityType.ZOMBIE : EntityType.SPIDER;
                            spawnAt(type, gx + 0.5f, gy, gz + 0.5f);
                        }
                        return;
                    }
                } else if (gy >= 60 && gy < 150) {
                    // Surface Night Spawns (Including Trolls)
                    if (world.getBlock(gx, gy, gz) == minicraft.world.Block.AIR && world.getBlock(gx, gy-1, gz).solid) {
                        if (rng.nextFloat() < 0.15f) {
                            spawnAt(EntityType.TROLL, gx + 0.5f, gy, gz + 0.5f);
                        } else {
                            // Tundra check for Ice Golem
                            minicraft.world.Biome b = world.getBiome(gx, gz);
                            if (b == minicraft.world.Biome.TUNDRA || b == minicraft.world.Biome.SNOWY_FOREST || b == minicraft.world.Biome.SNOWY_PEAKS) {
                                if (rng.nextFloat() < 0.10f) spawnAt(EntityType.ICE_BEAST, gx + 0.5f, gy, gz + 0.5f);
                                else if (rng.nextFloat() < 0.25f) spawnAt(EntityType.ICE_GOLEM, gx + 0.5f, gy, gz + 0.5f);
                                else spawnAt(EntityType.ZOMBIE, gx + 0.5f, gy, gz + 0.5f);
                            } else if (b == minicraft.world.Biome.FOREST || b == minicraft.world.Biome.REDWOOD || b == minicraft.world.Biome.JUNGLE) {
                                // Forest bosses — Rare Arborius and Crawler
                                if (rng.nextFloat() < 0.005f) spawnAt(EntityType.ARBORIUS, gx + 0.5f, gy, gz + 0.5f);
                                else if (rng.nextFloat() < 0.03f) spawnAt(EntityType.FOREST_CRAWLER, gx + 0.5f, gy, gz + 0.5f);
                                else spawnAt(EntityType.ZOMBIE, gx + 0.5f, gy, gz + 0.5f);
                            } else {
                                spawnAt(EntityType.ZOMBIE, gx + 0.5f, gy, gz + 0.5f);
                            }
                        }
                        return;
                    }
                }
            } else {
                // Check for Sky Dragon Arenas
                if (gy > 700) {
                    if (checkDragonArenaSpawn(gx, gy, gz, world)) return;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Spawn helpers
    // ─────────────────────────────────────────────────────────────────────

    public void spawn(Entity entity) {
        entities.add(entity);
    }

    /** Convenience: spawn an entity of a given type at a world position. */
    public Entity spawnAt(EntityType type, float x, float y, float z) {
        Entity e = createEntity(type);
        e.setPosition(x, y, z);
        spawn(e);
        return e;
    }

    private Entity createEntity(EntityType type) {
        switch (type) {
            // Passive
            case COW:    return new minicraft.entity.animals.Cow();
            case SHEEP:  return new minicraft.entity.animals.Sheep();
            case RAM:    return new minicraft.entity.animals.Ram();
            case DOG:    return new minicraft.entity.animals.Dog();
            case CAT:    return new minicraft.entity.animals.Cat();
            case WHALE:  return new minicraft.entity.animals.Whale();
            // Predators
            case BEAR:   return new minicraft.entity.animals.Bear();
            case WOLF:   return new minicraft.entity.animals.Wolf();
            case TIGER:  return new minicraft.entity.animals.Tiger();
            case LION:   return new minicraft.entity.animals.Lion();
            case EAGLE:  return new minicraft.entity.animals.Eagle();
            // NPCs
            case FARMER: return new minicraft.entity.npcs.Farmer();
            // Monsters
            case ZOMBIE: return new minicraft.entity.monsters.Zombie();
            case SPIDER: return new minicraft.entity.monsters.Spider();
            case TROLL:  return new minicraft.entity.monsters.Troll();
            case FIRE_DRAGON: return new minicraft.entity.monsters.FireDragon();
            case FIRE_DEMON:  return new minicraft.entity.monsters.FireDemon();
            case GOLD_DRAGON: return new minicraft.entity.monsters.GoldDragon();
            case ONYX_DRAGON: return new minicraft.entity.monsters.OnyxDragon();
            case ORC: return new minicraft.entity.monsters.Orc();
            case GRIFFINS: return new minicraft.entity.monsters.Griffin();
            case LEVIATHAN: return new minicraft.entity.monsters.Leviathan();
            case ICE_GOLEM: return new minicraft.entity.monsters.IceGolem();
            case ICE_BEAST: return new minicraft.entity.monsters.IceBeast();
            case FOREST_CRAWLER: return new minicraft.entity.monsters.ForestCrawler();
            case ARBORIUS: return new minicraft.entity.monsters.Arborius();
            case FIREBALL: return new minicraft.entity.monsters.Fireball(null, 0, 0, 0);
            case ONYX_PROJECTILE: return new minicraft.entity.monsters.OnyxProjectile(null, 0, 0, 0);
            case GOLD_FIREBALL: return new minicraft.entity.monsters.GoldFireball(null, 0, 0, 0);
            // Megastructures
            case STALWART_SHIP: 
                // Ships cannot be spawned via generic type-only factory as they require definitions.
                // Use executeShipSpawn in Main instead.
                throw new UnsupportedOperationException("Ship entities must be spawned with a ShipDefinition.");
            // Resources
            case ITEM:   return new ItemEntity(minicraft.world.Block.DIRT); // Default, usually overridden
            default:     throw new IllegalArgumentException("Unknown entity type: " + type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Spatial queries
    // ─────────────────────────────────────────────────────────────────────

    /** All entities within radius of (x,y,z). */
    public List<Entity> getNearby(float x, float y, float z, float radius) {
        List<Entity> out = new ArrayList<>();
        float r2 = radius * radius;
        for (Entity e : entities) {
            if (!e.isDead() && e.distanceSq(x, y, z) <= r2) out.add(e);
        }
        return out;
    }

    /** Nearest entity of a given type, or null if none in range. If type is null, any type matches. */
    public Entity getNearestOfType(float x, float y, float z, float radius, EntityType type) {
        Entity best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity e : getNearby(x, y, z, radius)) {
            if (type == null || e.type == type) {
                float d = e.distanceSq(x, y, z);
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }

    /** All passive animals within radius. */
    public List<Entity> getNearbyPassive(float x, float y, float z, float radius) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : getNearby(x, y, z, radius)) {
            if (e.type.isPassive()) out.add(e);
        }
        return out;
    }

    public List<Entity> getEntitiesInBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : entities) {
            if (!e.isDead() && e.position.x >= x1 && e.position.x <= x2 &&
                e.position.y >= y1 && e.position.y <= y2 &&
                e.position.z >= z1 && e.position.z <= z2) {
                out.add(e);
            }
        }
        return out;
    }

    public List<Entity> getAll() { return entities; }
    public int count()            { return entities.size(); }

    public Player getNearestPlayer(float x, float z) {
        Player best = null;
        float bestDistSq = Float.MAX_VALUE;
        for (Entity e : entities) {
            if (e instanceof Player) {
                float dx = e.position.x - x;
                float dz = e.position.z - z;
                float d2 = dx*dx + dz*dz;
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    best = (Player) e;
                }
            }
        }
        return best;
    }
    
    private float distSq(float x1, float z1, float x2, float z2) {
        float dx = x1 - x2, dz = z1 - z2;
        return dx*dx + dz*dz;
    }

    private boolean checkDragonArenaSpawn(int gx, int gy, int gz, minicraft.world.World world) {
        int gridSize = 128;
        int cx = Math.floorDiv(gx, 16);
        int cz = Math.floorDiv(gz, 16);
        
        int gridX = Math.floorDiv(cx, gridSize) * gridSize + 64;
        int gridZ = Math.floorDiv(cz, gridSize) * gridSize + 64;
        
        long seed = (long)gridX * 3123456789L + (long)gridZ * 123456789L + 777;
        Random r = new Random(seed);
        
        if (r.nextFloat() < 0.8f) {
            int bossType = r.nextInt(5);
            minicraft.world.WorldCell centerCell = world.getGenerator().generate(gridX * 16 + 8, gridZ * 16 + 8);
            
            // Check if we are inside the arena volume
            float dx = (gx + 0.5f) - (gridX * 16 + 8);
            float dz = (gz + 0.5f) - (gridZ * 16 + 8);
            float distSq = dx*dx + dz*dz;
            
            if (distSq < 40*40 && rng.nextFloat() < 0.05f) {
                switch(bossType) {
                    case 0: if (centerCell.biome == minicraft.world.Biome.MOUNTAINS) spawnAt(EntityType.GOLD_DRAGON, gx+0.5f, gy, gz+0.5f); break;
                    case 1: if (centerCell.biome == minicraft.world.Biome.DESERT) spawnAt(EntityType.ONYX_DRAGON, gx+0.5f, gy, gz+0.5f); break;
                    case 2: if (centerCell.temperature > 0.8f) spawnAt(EntityType.FIRE_DRAGON, gx+0.5f, gy, gz+0.5f); break;
                    case 3: if (centerCell.temperature < 0.2f) spawnAt(EntityType.ICE_DRAGON, gx+0.5f, gy, gz+0.5f); break;
                    case 4: if (centerCell.biome == minicraft.world.Biome.FOREST || centerCell.biome == minicraft.world.Biome.JUNGLE) 
                                spawnAt(EntityType.EARTH_DRAGON, gx+0.5f, gy, gz+0.5f); break;
                }
                return true;
            }
        }
        return false;
    }

    private boolean checkBossSpawn(int gx, int gy, int gz, minicraft.world.World world) {
        int gridSize = 64;
        int cx = Math.floorDiv(gx, 16);
        int cz = Math.floorDiv(gz, 16);
        
        int gridX = Math.floorDiv(cx, gridSize) * gridSize + 32;
        int gridZ = Math.floorDiv(cz, gridSize) * gridSize + 32;
        
        long seed = (long)gridX * 3123456789L + (long)gridZ * 123456789L + 777;
        Random r = new Random(seed);
        
        if (r.nextFloat() < 0.6f) {
            int bossType = r.nextInt(3);
            minicraft.world.WorldCell centerCell = world.getGenerator().generate(gridX * 16 + 8, gridZ * 16 + 8);
            
            float dx = (gx + 0.5f) - (gridX * 16 + 8);
            float dz = (gz + 0.5f) - (gridZ * 16 + 8);
            float distSq = dx*dx + dz*dz;
            
            // Dragons removed from underground dome arenas per request - swapped with Fire Demons
            if (bossType == 0 && centerCell.biome == minicraft.world.Biome.MOUNTAINS && distSq < 40*40) {
                if (rng.nextFloat() < 0.2f) { spawnAt(EntityType.FIRE_DEMON, gx+0.5f, gy, gz+0.5f); return true; }
            } else if (bossType == 1 && centerCell.biome == minicraft.world.Biome.DESERT && distSq < 50*50) {
                if (rng.nextFloat() < 0.2f) { spawnAt(EntityType.FIRE_DEMON, gx+0.5f, gy, gz+0.5f); return true; }
            } else if (bossType == 2 && centerCell.biome == minicraft.world.Biome.SAVANNA && distSq < 30*30) {
                if (rng.nextFloat() < 0.2f) { spawnAt(EntityType.FIRE_DEMON, gx+0.5f, gy, gz+0.5f); return true; }
            }
        }
        return false;
    }

    private void handleBlockAuras(minicraft.world.World world, minicraft.entity.ParticleManager pm) {
        for (Entity e : entities) {
            if (e instanceof Player) {
                Player p = (Player) e;
                // Sample 20 random blocks in a 31x31x11 area around each player
                for (int i = 0; i < 20; i++) {
                    int rx = (int)p.position.x + rng.nextInt(31) - 15;
                    int ry = (int)p.position.y + rng.nextInt(11) - 5;
                    int rz = (int)p.position.z + rng.nextInt(31) - 15;
                    if (world.getBlock(rx, ry, rz) == minicraft.world.Block.GOLDEN_CHEST) {
                        pm.spawnDivineAura(rx + 0.5f, ry, rz + 0.5f);
                    }
                }
            }
        }
    }
}
