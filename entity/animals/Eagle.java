package minicraft.entity.animals;

import minicraft.entity.*;

/**
 * Eagle – flies overhead, diving at prey below.
 * Maintains a cruising altitude above terrain and dives to attack.
 */
public class Eagle extends Predator {
    private static final float CRUISE_HEIGHT = 20f; // blocks above ground
    private float cruiseY;
    private boolean diving = false;

    public Eagle() {
        super(EntityType.EAGLE, 3f, 1.5f, 30f, 8f);
        cruiseY = 80f; // default altitude until we have terrain height lookup
    }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, float dt) {
        super.tick(manager, world, dt);
        state = EntityState.FLYING;

        if (target != null && !target.isDead()) {
            float distY = Math.abs(position.y - target.position.y);
            if (distY > 3f && !diving) {
                // Glide down toward target
                velocity.y -= 2f * dt;
                diving = true;
            }
        } else {
            diving = false;
            // Float back up to cruise altitude
            if (position.y < cruiseY) {
                velocity.y += 3f * dt;
            }
        }
    }
}
