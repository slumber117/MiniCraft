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
        }
    }

    private void tick(float dt, minicraft.world.World world, minicraft.entity.ParticleManager particleManager) {
        for (Entity e : entities) {
            if (!e.isDead()) e.tick(this, world, particleManager, dt);
        }
        // Remove dead entities
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e.isDead()) {
                e.onDeath(this);
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
            if (world.getLight(gx, gy, gz) < 0.2f && gy < 60) {
                // Check if air
                if (world.getBlock(gx, gy, gz) == minicraft.world.Block.AIR) {
                    EntityType type = rng.nextFloat() < 0.7f ? EntityType.ZOMBIE : EntityType.SPIDER;
                    spawnAt(type, gx + 0.5f, gy, gz + 0.5f);
                    return;
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
}
