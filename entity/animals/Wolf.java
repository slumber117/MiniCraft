package minicraft.entity.animals;

import minicraft.entity.*;

/** Wolf – hunts in packs; targets the same prey as nearby wolves. */
public class Wolf extends Predator {
    private static final float PACK_RANGE = 20f;

    public Wolf() { super(EntityType.WOLF, 4f, 1.2f, 16f, 5f); }

    @Override
    protected void findTarget(EntityManager manager) {
        // First check if a nearby wolf already has a target; share it (pack hunting)
        for (Entity nearby : manager.getNearby(position.x, position.y, position.z, PACK_RANGE)) {
            if (nearby instanceof Wolf && nearby != this) {
                Wolf packMate = (Wolf) nearby;
                if (packMate.target != null && !packMate.target.isDead()) {
                    this.target = packMate.target;
                    return;
                }
            }
        }
        super.findTarget(manager);
    }
}
