package minicraft.entity;

import minicraft.world.Block;
import minicraft.world.World;

/**
 * A physical item dropped in the world that can be collected.
 */
public class ItemEntity extends Entity {

    public final Block block;
    private float age = 0;
    private float bobbingOffset = 0;

    public ItemEntity(Block block) {
        super(EntityType.ITEM, 1f);
        this.block = block;
        this.width = 0.3f;
        this.height = 0.3f;
    }

    @Override
    public void tick(EntityManager manager, World world, ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        age += dt;
        
        // Basic physics
        applyVelocity(world, dt);
        
        // Bobbing effect
        bobbingOffset = (float) Math.sin(age * 3.0f) * 0.1f;
        
        // Rotation (just logic, used by renderer)
        yaw += 90f * dt;
    }

    public float getBobbingOffset() {
        return bobbingOffset;
    }

    public Block getBlock() {
        return block;
    }
}
