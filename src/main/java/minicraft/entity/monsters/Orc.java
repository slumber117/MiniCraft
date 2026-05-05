package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.ItemEntity;
import minicraft.item.Item;
import minicraft.item.ToolItem;
import minicraft.world.World;
import java.util.Random;

public class Orc extends Entity {
    private float aggroRange = 14.0f;
    private float moveSpeed = 1.8f;
    private float damageValue = 15.0f;
    private float attackCooldown = 1.0f;
    private float attackTimer = 0f;
    private Random rand = new Random();

    public Orc() {
        super(EntityType.ORC, EntityType.ORC.baseHealth);
        this.width = 0.8f;
        this.height = 1.8f;
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
            if (len > 0.5f) {
                velocity.x += (dx / len) * moveSpeed * dt;
                velocity.z += (dz / len) * moveSpeed * dt;
            }
            if (len < 1.5f && attackTimer <= 0) {
                player.damage(damageValue, this);
                attackTimer = attackCooldown;
            }
        }
        if (attackTimer > 0) attackTimer -= dt;
        applyVelocity(world, dt);
    }

    @Override
    public void onDeath(EntityManager manager, World world) {
        super.onDeath(manager, world);
        // Drop stone equipment
        ToolItem.ToolType[] types = ToolItem.ToolType.values();
        ToolItem.ToolType type = types[rand.nextInt(types.length)];
        String tex = "item_pick_stone";
        if (type == ToolItem.ToolType.SWORD) tex = "item_sword_stone";
        
        Item drop = new ToolItem("Stone " + type.name().toLowerCase(), type, 1, 4.0f, tex);
        ItemEntity e = new ItemEntity(drop);
        e.position.set(position);
        manager.addEntity(e);
    }
}
