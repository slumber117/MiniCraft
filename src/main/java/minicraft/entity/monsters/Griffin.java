package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.ItemEntity;
import minicraft.item.Item;
import minicraft.world.World;
import java.util.Random;

public class Griffin extends Entity {
    private float aggroRange = 25.0f;
    private float moveSpeed = 3.5f;
    private float damageValue = 25.0f;
    private Random rand = new Random();

    public Griffin() {
        super(EntityType.GRIFFINS, EntityType.GRIFFINS.baseHealth);
        this.width = 1.5f;
        this.height = 1.5f;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        if (isDead()) return;

        minicraft.entity.Player player = manager.getNearestPlayer(position.x, position.z);
        if (player != null && position.distance(player.position) < aggroRange) {
            float dx = player.position.x - position.x;
            float dy = (player.position.y + 1.0f) - position.y;
            float dz = player.position.z - position.z;
            float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len > 1.0f) {
                velocity.x += (dx / len) * moveSpeed * dt;
                velocity.y += (dy / len) * moveSpeed * dt;
                velocity.z += (dz / len) * moveSpeed * dt;
            } else {
                player.damage(damageValue, this);
            }
        }
        applyVelocity(world, dt);
    }

    @Override
    public void onDeath(EntityManager manager, World world) {
        super.onDeath(manager, world);
        // Drop Emerald
        Item drop = new Item("Emerald", null, "EMERALD_ORE", 64);
        ItemEntity e = new ItemEntity(drop);
        e.position.set(position);
        manager.addEntity(e);
    }
}
