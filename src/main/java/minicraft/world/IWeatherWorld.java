package minicraft.world;

import minicraft.entity.Entity;
import java.util.List;

/**
 * Interface for world-weather interactions to decouple WeatherManager from World.
 */
public interface IWeatherWorld {
    void setBlock(int gx, int gy, int gz, Block b);
    Block getBlock(int gx, int gy, int gz);
    List<Entity> getEntitiesInBox(float x1, float y1, float z1, float x2, float y2, float z2);
    void damageEntity(Entity e, float damage);
    long getSeed();

    // --- Lightning / Fire ---
    int getSurfaceY(int x, int z);
    List<IWeatherEntity> getEntitiesInRadius(float x, float y, float z, float radius);
    boolean isFlammable(int x, int y, int z);
    boolean isAir(int x, int y, int z);
    void setFire(int x, int y, int z);

    // --- Blizzard / Ice ---
    boolean isWater(int x, int y, int z);
    boolean setIce(int x, int y, int z);
    void meltIce(int x, int y, int z);

    // --- Cyclone / Hurricane ---
    int getMaxY();
    boolean isWindDestructible(int x, int y, int z);
    void destroyBlock(int x, int y, int z, boolean dropItems);

    // --- Entity Queries ---
    List<IWeatherEntity> getAllEntities();
}