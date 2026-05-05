package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;
import java.util.Random;

public abstract class BaseDragon extends Entity {
    protected enum DragonState { FLYING, LANDING, GROUNDED, TAKING_OFF }
    protected DragonState dragonState = DragonState.FLYING;
    
    protected float stateTimer = 0f;
    protected final float FLY_DURATION = 90f; // 1.5 mins
    protected final float GROUND_DURATION = 120f; // 2 mins
    
    protected float attackTimer = 0f;
    protected float moveSpeed = 8.0f;
    protected Random rng = new Random();
    
    protected abstract String getRequiredMaterial();
    protected abstract void fireProjectile(Player p, EntityManager manager);
    protected abstract float getBiteDamage();

    public BaseDragon(EntityType type, float maxHealth) {
        super(type, maxHealth);
        this.state = EntityState.FLYING;
        this.stateTimer = FLY_DURATION;
    }

    @Override
    public void tick(EntityManager manager, World world, minicraft.entity.ParticleManager pm, float dt) {
        super.tick(manager, world, pm, dt);
        if (dead) return;

        Player p = findNearbyPlayer(manager);
        if (p != null) {
            updateState(p, world, dt);
            executeBehavior(p, manager, world, dt);
            
            // Gear Check: Punishment for lack of correct armor
            if (distanceSq(p) < 15*15) {
                checkArmorRequirement(p, dt);
            }
        }

        if (attackTimer > 0) attackTimer -= dt;
    }

    private void updateState(Player p, World world, float dt) {
        stateTimer -= dt;
        switch (dragonState) {
            case FLYING:
                if (stateTimer <= 0) {
                    dragonState = DragonState.LANDING;
                    state = EntityState.WANDERING; // Start descending
                }
                break;
            case LANDING:
                if (position.y <= world.getSafeSpawnY((int)position.x, (int)position.y, (int)position.z) + 0.1f) {
                    dragonState = DragonState.GROUNDED;
                    stateTimer = GROUND_DURATION;
                    velocity.y = 0;
                }
                break;
            case GROUNDED:
                if (stateTimer <= 0) {
                    dragonState = DragonState.TAKING_OFF;
                }
                break;
            case TAKING_OFF:
                if (position.y >= p.position.y + 25.0f) {
                    dragonState = DragonState.FLYING;
                    stateTimer = FLY_DURATION;
                    state = EntityState.FLYING;
                }
                break;
        }
    }

    private void executeBehavior(Player p, EntityManager manager, World world, float dt) {
        float dSq = distanceSq(p);
        
        switch (dragonState) {
            case FLYING:
                // Hover 25 units above player and circle
                Vector3f target = new Vector3f(p.position.x, p.position.y + 25.0f, p.position.z);
                moveTo(target, dt);
                if (attackTimer <= 0 && dSq < 50*50) {
                    fireProjectile(p, manager);
                    attackTimer = 3.0f + rng.nextFloat();
                }
                break;
                
            case LANDING:
                // Move towards player X/Z but drop Y
                int groundY = world.getSafeSpawnY((int)position.x, (int)position.y, (int)position.z);
                moveTo(new Vector3f(p.position.x, groundY, p.position.z), dt);
                velocity.y = -5.0f; // Constant descent
                break;
                
            case GROUNDED:
                // Chase player on ground and bite
                moveTo(p.position, dt);
                velocity.y = 0;
                if (dSq < 6*6 && attackTimer <= 0) {
                    p.damage(getBiteDamage(), this);
                    attackTimer = 1.5f;
                }
                break;
                
            case TAKING_OFF:
                velocity.y = 5.0f;
                moveTo(new Vector3f(p.position.x, p.position.y + 30f, p.position.z), dt);
                break;
        }

        // Apply movement (custom for flying/grounded)
        position.x += velocity.x * dt;
        position.y += velocity.y * dt;
        position.z += velocity.z * dt;
        
        velocity.x *= 0.98f;
        velocity.z *= 0.98f;
        if (dragonState == DragonState.FLYING || dragonState == DragonState.TAKING_OFF) velocity.y *= 0.98f;

        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
    }

    private void checkArmorRequirement(Player p, float dt) {
        if (!p.inventory.hasFullSet(getRequiredMaterial())) {
            // Suffocate or deal damage for not having the required armor
            p.damage(10.0f * dt, this); // Continuous damage
        }
    }

    @Override
    public void damage(float amount, Entity attacker) {
        if (attacker instanceof Player) {
            Player p = (Player) attacker;
            minicraft.item.Item held = p.inventory.getSelectedItem();
            String req = getRequiredMaterial() + " Sword";
            if (held == null || !held.getDisplayName().equalsIgnoreCase(req)) {
                // Immune to non-matching swords
                amount = 0;
            }
        }
        super.damage(amount, attacker);
    }

    protected Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    protected void moveTo(Vector3f target, float dt) {
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 1.0f) {
            velocity.x += (dx / len) * moveSpeed * dt;
            if (dragonState != DragonState.LANDING && dragonState != DragonState.TAKING_OFF)
                velocity.y += (dy / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }
}
