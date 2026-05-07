package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

/**
 * Arborius — The Ancient Grove Guardian.
 * A massive horned bear boss that guards the deep forests.
 * 
 * Arborius uses heavy swipes and a "Solar Charge" dash attack.
 * On death, it drops Apex-tier Bastnaesite materials.
 */
public class Arborius extends Entity {

    private float aggroRange = 50.0f;
    private float attackRange = 6.0f;
    private float moveSpeed = 3.5f;
    private float attackCooldown = 2.0f;
    private float attackTimer = 0f;

    private float chargeTimer = 0f;
    private boolean isCharging = false;
    private Vector3f chargeDir = new Vector3f(0, 0, 0);

    public Arborius() {
        super(EntityType.ARBORIUS, EntityType.ARBORIUS.baseHealth);
        this.width = EntityType.ARBORIUS.bodyWidth;
        this.height = EntityType.ARBORIUS.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) return;

        if (attackTimer > 0) attackTimer -= dt;

        Player p = findNearbyPlayer(manager);
        if (p != null) {
            float dSq = distanceSq(p.position.x, p.position.y, p.position.z);
            if (dSq < aggroRange * aggroRange) {
                if (isCharging) {
                    chargeTimer -= dt;
                    velocity.x = chargeDir.x * 12.0f;
                    velocity.z = chargeDir.z * 12.0f;
                    if (chargeTimer <= 0) isCharging = false;
                    
                    // Collision damage during charge
                    for (Entity e : manager.getNearby(position.x, position.y, position.z, 4.0f)) {
                        if (e instanceof Player) {
                            e.damage(200f * dt, this);
                            ((Player)e).velocity.y += 2.0f;
                        }
                    }
                } else {
                    moveTo(p.position, dt);
                    if (dSq < attackRange * attackRange && attackTimer <= 0) {
                        if (rng.nextFloat() < 0.3f) {
                            startCharge(p);
                        } else {
                            p.damage(150f, this);
                            attackTimer = attackCooldown;
                        }
                    }
                }
            }
        }

        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
            
        // Ambient Solar Particles (Green)
        if (rng.nextFloat() < 0.3f) {
            particleManager.spawnSmoke(position.x + (rng.nextFloat()-0.5f)*4f, position.y + 2f, position.z + (rng.nextFloat()-0.5f)*4f);
        }
    }

    private void startCharge(Player p) {
        isCharging = true;
        chargeTimer = 1.2f;
        float dx = p.position.x - position.x;
        float dz = p.position.z - position.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 0) {
            chargeDir.set(dx / len, 0, dz / len);
        }
        attackTimer = 3.0f;
    }

    private void moveTo(Vector3f target, float dt) {
        float dx = target.x - position.x;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 0.1f) {
            velocity.x += (dx / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }

    private Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    @Override
    public void onDeath(EntityManager manager, World world) {
        super.onDeath(manager, world);
        // Boss loot: Bastnaesite Ore
        int ix = (int) Math.floor(position.x);
        int iy = (int) Math.floor(position.y);
        int iz = (int) Math.floor(position.z);
        world.setBlock(ix, iy, iz, minicraft.world.Block.CHEST);
        minicraft.entity.Inventory inv = world.getOrCreateContainer(ix, iy, iz);
        if (inv != null) {
            inv.add(new minicraft.item.Item("BASTNAESITE", minicraft.world.Block.BASTNAESITE_ORE), 12);
        }
    }
}
