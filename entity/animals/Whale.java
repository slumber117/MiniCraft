package minicraft.entity.animals;

import minicraft.entity.*;

/** Whale – wanders slowly in ocean biomes. Sea-level locked. */
public class Whale extends PassiveAnimal {
    private static final float SEA_Y = 62f;

    public Whale() { super(EntityType.WHALE); }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, float dt) {
        super.tick(manager, world, dt);
        // Keep whale at sea level regardless of terrain
        position.y = SEA_Y;
        velocity.y = 0;
        state = EntityState.SWIMMING;
    }
}
