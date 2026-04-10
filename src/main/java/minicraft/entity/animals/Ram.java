package minicraft.entity.animals;

import minicraft.entity.*;

/** Ram – stronger than a sheep; headbutts back if hit. */
public class Ram extends PassiveAnimal {
    private float retaliateTimer = 0f;

    public Ram() { super(EntityType.RAM); }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, float dt) {
        retaliateTimer = Math.max(0, retaliateTimer - dt);
        super.tick(manager, world, dt);
    }

    @Override
    public void damage(float amount) {
        super.damage(amount);
        retaliateTimer = 5f; // 5-second aggro window after being hit
        state = EntityState.FLEEING; // rams briefly charge then flee
    }
}
