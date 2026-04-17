package minicraft.entity;

/**
 * Base class for hostile / predatory entities.
 * Implements a: idle → wander → chase → attack state machine.
 */
public abstract class Predator extends Entity {

    protected final float attackDamage;
    protected final float attackRange;
    protected final float chaseRange;
    protected final float moveSpeed;

    protected float attackCooldown = 0f;
    protected float wanderTimer    = 0f;
    protected float wanderDirX     = 0f;
    protected float wanderDirZ     = 0f;

    protected Entity target = null;

    public Predator(EntityType type, float attackDamage, float attackRange,
                    float chaseRange, float moveSpeed) {
        super(type, type.baseHealth);
        this.attackDamage = attackDamage;
        this.attackRange  = attackRange;
        this.chaseRange   = chaseRange;
        this.moveSpeed    = moveSpeed;
        this.width  = type.bodyWidth;
        this.height = type.bodyHeight;
    }

    @Override
    public void tick(EntityManager manager, minicraft.world.World world, minicraft.entity.ParticleManager particleManager, float dt) {
        if (dead) { state = EntityState.DEAD; return; }

        attackCooldown = Math.max(0, attackCooldown - dt);

        // Find or validate target
        findTarget(manager);

        if (target != null && !target.isDead()) {
            float dist = (float) Math.sqrt(distanceSq(target));
            if (dist <= attackRange) {
                doAttack(dt);
            } else {
                chaseTarget(dt);
            }
        } else {
            target = null;
            wander(dt);
        }

        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f) {
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));
        }
    }

    protected void findTarget(EntityManager manager) {
        if (target != null && !target.isDead()
                && distanceSq(target) <= chaseRange * chaseRange) return;
        // Hunt nearest passive animal
        target = null;
        float bestDist = chaseRange * chaseRange;
        for (Entity e : manager.getNearby(position.x, position.y, position.z, chaseRange)) {
            if (e.type.isPassive()) {
                float d = distanceSq(e);
                if (d < bestDist) { bestDist = d; target = e; }
            }
        }
    }

    protected void chaseTarget(float dt) {
        state = EntityState.CHASING;
        float dx = target.position.x - position.x;
        float dz = target.position.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dz*dz);
        if (len > 0) {
            velocity.x += (dx / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }

    protected void doAttack(float dt) {
        state = EntityState.ATTACKING;
        if (attackCooldown <= 0f) {
            target.damage(attackDamage, this);
            attackCooldown = 1.5f; // 1.5 sec between strikes
        }
    }

    protected void wander(float dt) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            state = PassiveAnimal.RNG.nextFloat() < 0.4f
                    ? EntityState.IDLE : EntityState.WANDERING;
            float angle = (float)(PassiveAnimal.RNG.nextDouble() * Math.PI * 2);
            wanderDirX  = (float) Math.sin(angle);
            wanderDirZ  = (float) Math.cos(angle);
            wanderTimer = 2f + PassiveAnimal.RNG.nextFloat() * 5f;
        }
        if (state == EntityState.WANDERING) {
            velocity.x += wanderDirX * (moveSpeed * 0.5f) * dt;
            velocity.z += wanderDirZ * (moveSpeed * 0.5f) * dt;
        }
    }
}
