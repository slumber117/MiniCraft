package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.ItemEntity;
import minicraft.item.Item;
import minicraft.world.World;

public class Leviathan extends Entity {
    private float aggroRange = 40.0f;
    private float moveSpeed = 2.0f;
    private float damageValue = 60.0f;

    public Leviathan() {
        super(EntityType.LEVIATHAN, EntityType.LEVIATHAN.baseHealth);
        this.width = 4.0f;
        this.height = 3.0f;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        if (isDead()) return;

        minicraft.entity.Player player = manager.getNearestPlayer(position.x, position.z);
        if (player != null && position.distance(player.position) < aggroRange) {
            float dx = player.position.x - position.x;
            float dz = player.position.z - position.z;
            float len = (float) Math.sqrt(dx*dx + dz*dz);
            if (len > 2.0f) {
                velocity.x += (dx / len) * moveSpeed * dt;
                velocity.z += (dz / len) * moveSpeed * dt;
            }
            if (len < 4.0f) {
                player.damage(damageValue, this);
            }
        }
        applyVelocity(world, dt);
    }

    @Override
    public void onDeath(EntityManager manager, World world) {
        super.onDeath(manager, world);
        // Drop Painite
        Item drop = new Item("Painite", null, "PAINITE_ORE", 64);
        ItemEntity e = new ItemEntity(drop);
        e.position.set(position);
        manager.spawn(e);
    }
}
