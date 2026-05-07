package minicraft.entity.monsters;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityState;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.math.Vector3f;
import minicraft.world.World;

/**
 * Forest Crawler — A colossal ancient tree-beast boss.
 *
 * This towering woodland horror prowls the deepest forests, its bark-armored
 * body dragging through the canopy. It cycles through three attack phases:
 *   Phase 1 (>60% HP): Vine Whip — fast lashing melee strikes.
 *   Phase 2 (30–60% HP): Ground Pound — AoE slam that knocks back all nearby players.
 *   Phase 3 (<30% HP): Enraged — summons Spider minions and attacks with ferocity.
 *
 * On death it drops a chest with rare forest-tier loot.
 */
public class ForestCrawler extends Entity {

    // ── Combat Tuning ─────────────────────────────────────────────────────
    private float aggroRange       = 40.0f;
    private float attackRange      = 5.0f;   // Long vine reach
    private float moveSpeed        = 2.0f;
    private float attackCooldown   = 1.5f;
    private float attackTimer      = 0f;

    // Phase-specific damage values
    private static final float VINE_WHIP_DAMAGE   = 60.0f;
    private static final float GROUND_POUND_DAMAGE = 120.0f;
    private static final float ENRAGED_DAMAGE      = 90.0f;
    private static final float GROUND_POUND_RADIUS  = 8.0f;

    // ── Summon Mechanics ──────────────────────────────────────────────────
    private float summonCooldown   = 12.0f;
    private float summonTimer      = 0f;
    private static final int MAX_SUMMONS = 4;
    private int activeSummons      = 0;

    // ── Phase Tracking ────────────────────────────────────────────────────
    private enum Phase { VINE_WHIP, GROUND_POUND, ENRAGED }
    private Phase currentPhase = Phase.VINE_WHIP;

    // ── Roaming (idle patrol) ─────────────────────────────────────────────
    private float roamTimer = 0f;
    private float roamDirX  = 0f;
    private float roamDirZ  = 0f;

    // ── Ground Pound animation lock ───────────────────────────────────────
    private float slamWindup  = 0f;
    private boolean isSlamming = false;

    public ForestCrawler() {
        super(EntityType.FOREST_CRAWLER, EntityType.FOREST_CRAWLER.baseHealth);
        this.width  = EntityType.FOREST_CRAWLER.bodyWidth;
        this.height = EntityType.FOREST_CRAWLER.bodyHeight;
        pickNewRoamDirection();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Main tick
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void tick(EntityManager manager, World world,
                     minicraft.entity.ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) { state = EntityState.DEAD; return; }

        if (attackTimer > 0) attackTimer -= dt;
        if (summonTimer > 0) summonTimer -= dt;

        updatePhase();

        Player p = findNearbyPlayer(manager);

        if (p != null) {
            float dSq = distanceSq(p.position.x, p.position.y, p.position.z);

            if (dSq < aggroRange * aggroRange) {
                // Handle slam windup lock
                if (isSlamming) {
                    slamWindup -= dt;
                    velocity.x *= 0.5f;
                    velocity.z *= 0.5f;
                    if (slamWindup <= 0) {
                        executeGroundPound(manager, particleManager);
                        isSlamming = false;
                    }
                } else {
                    moveTo(p.position, dt);

                    if (dSq < attackRange * attackRange) {
                        switch (currentPhase) {
                            case VINE_WHIP:
                                tryVineWhip(p);
                                break;
                            case GROUND_POUND:
                                tryGroundPound(p);
                                break;
                            case ENRAGED:
                                tryEnragedAttack(p);
                                trySummonSpiders(manager);
                                break;
                        }
                    }
                }
            } else {
                roam(world, dt);
            }
        } else {
            roam(world, dt);
        }

        applyVelocity(world, dt);
        if (Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f)
            yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, -velocity.z));

        // Ambient particle effects — leaves and spore clouds
        spawnAmbientParticles(particleManager);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Phase management
    // ─────────────────────────────────────────────────────────────────────

    private void updatePhase() {
        float ratio = health / maxHealth;
        if (ratio > 0.60f) {
            currentPhase = Phase.VINE_WHIP;
            moveSpeed = 2.0f;
            attackCooldown = 1.5f;
        } else if (ratio > 0.30f) {
            currentPhase = Phase.GROUND_POUND;
            moveSpeed = 1.4f;      // Slower, heavier
            attackCooldown = 2.5f;
        } else {
            currentPhase = Phase.ENRAGED;
            moveSpeed = 3.5f;      // Frenzied speed
            attackCooldown = 0.8f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Attack: Phase 1 — Vine Whip
    // ─────────────────────────────────────────────────────────────────────

    private void tryVineWhip(Player player) {
        if (attackTimer <= 0) {
            player.damage(VINE_WHIP_DAMAGE, this);
            attackTimer = attackCooldown;
            // Lash knockback
            float dx = player.position.x - position.x;
            float dz = player.position.z - position.z;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                player.velocity.x += (dx / len) * 4.0f;
                player.velocity.z += (dz / len) * 4.0f;
            }
            player.velocity.y += 2.0f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Attack: Phase 2 — Ground Pound (AoE)
    // ─────────────────────────────────────────────────────────────────────

    private void tryGroundPound(Player player) {
        if (attackTimer <= 0 && !isSlamming) {
            // Start windup — boss rears up
            isSlamming = true;
            slamWindup = 0.8f; // 0.8s telegraph
            velocity.y = 4.0f; // Jump up before slamming
            attackTimer = attackCooldown;
        }
    }

    private void executeGroundPound(EntityManager manager,
                                     minicraft.entity.ParticleManager particleManager) {
        // AoE damage to all nearby entities
        for (Entity e : manager.getNearby(position.x, position.y, position.z, GROUND_POUND_RADIUS)) {
            if (e instanceof Player) {
                Player p = (Player) e;
                p.damage(GROUND_POUND_DAMAGE, this);
                // Radial knockback
                float dx = p.position.x - position.x;
                float dz = p.position.z - position.z;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1f) {
                    p.velocity.x += (dx / len) * 8.0f;
                    p.velocity.z += (dz / len) * 8.0f;
                }
                p.velocity.y += 6.0f;
            }
        }
        // Slam particles
        for (int i = 0; i < 20; i++) {
            float ox = (rng.nextFloat() * 6 - 3);
            float oz = (rng.nextFloat() * 6 - 3);
            particleManager.spawnSmoke(position.x + ox, position.y + 0.5f, position.z + oz);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Attack: Phase 3 — Enraged Melee + Spider Summons
    // ─────────────────────────────────────────────────────────────────────

    private void tryEnragedAttack(Player player) {
        if (attackTimer <= 0) {
            player.damage(ENRAGED_DAMAGE, this);
            attackTimer = attackCooldown;
            // Frenzied double-hit with lift
            velocity.y = 2.5f;
            float dx = player.position.x - position.x;
            float dz = player.position.z - position.z;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                player.velocity.x += (dx / len) * 6.0f;
                player.velocity.z += (dz / len) * 6.0f;
            }
            player.velocity.y += 4.0f;
        }
    }

    private void trySummonSpiders(EntityManager manager) {
        if (summonTimer <= 0 && activeSummons < MAX_SUMMONS) {
            // Spawn 2 spiders around the crawler
            for (int i = 0; i < 2; i++) {
                float angle = rng.nextFloat() * (float)(Math.PI * 2);
                float dist = 3.0f + rng.nextFloat() * 3.0f;
                float sx = position.x + (float) Math.cos(angle) * dist;
                float sz = position.z + (float) Math.sin(angle) * dist;
                manager.spawnAt(EntityType.SPIDER, sx, position.y, sz);
                activeSummons++;
            }
            summonTimer = summonCooldown;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Movement
    // ─────────────────────────────────────────────────────────────────────

    private void moveTo(Vector3f target, float dt) {
        state = EntityState.WANDERING;
        float dx = target.x - position.x;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 0.1f) {
            velocity.x += (dx / len) * moveSpeed * dt;
            velocity.z += (dz / len) * moveSpeed * dt;
        }
    }

    private void roam(World world, float dt) {
        state = EntityState.WANDERING;
        roamTimer -= dt;
        if (roamTimer <= 0) pickNewRoamDirection();
        float roamSpeed = moveSpeed * 0.2f;
        velocity.x += roamDirX * roamSpeed * dt;
        velocity.z += roamDirZ * roamSpeed * dt;
        velocity.x *= 0.92f;
        velocity.z *= 0.92f;
    }

    private void pickNewRoamDirection() {
        float angle = rng.nextFloat() * (float)(Math.PI * 2);
        roamDirX = (float) Math.cos(angle);
        roamDirZ = (float) Math.sin(angle);
        roamTimer = 4.0f + rng.nextFloat() * 6.0f;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Particles
    // ─────────────────────────────────────────────────────────────────────

    private void spawnAmbientParticles(minicraft.entity.ParticleManager particleManager) {
        // Leaf / spore trail
        if (rng.nextFloat() < 0.25f) {
            float ox = (rng.nextFloat() * 3 - 1.5f);
            float oz = (rng.nextFloat() * 3 - 1.5f);
            particleManager.spawnSmoke(position.x + ox, position.y + height * 0.8f, position.z + oz);
        }
        // Enraged: more intense particle cloud
        if (currentPhase == Phase.ENRAGED && rng.nextFloat() < 0.4f) {
            float ox = (rng.nextFloat() * 5 - 2.5f);
            float oz = (rng.nextFloat() * 5 - 2.5f);
            particleManager.spawnSmoke(position.x + ox, position.y + height * 0.5f, position.z + oz);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────────────────────────────

    private Player findNearbyPlayer(EntityManager manager) {
        for (Entity e : manager.getAll()) if (e instanceof Player) return (Player) e;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Loot
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onDeath(EntityManager manager, minicraft.world.World world) {
        super.onDeath(manager, world);

        int ix = (int) Math.floor(position.x);
        int iy = (int) Math.floor(position.y);
        int iz = (int) Math.floor(position.z);

        world.setBlock(ix, iy, iz, minicraft.world.Block.CHEST);
        minicraft.entity.Inventory chestInv = world.getOrCreateContainer(ix, iy, iz);
        if (chestInv != null) {
            // Forest-tier rewards — Emerald Armor set + enchanted wooden weapons
            chestInv.add(new minicraft.item.ArmorItem(
                "Emerald Helmet", minicraft.item.ArmorItem.ArmorSlot.HELMET,
                0.45f, "armor_emerald_helmet", "Emerald",
                1.0f, 1.05f, 0.35f, null), 1);
            chestInv.add(new minicraft.item.ArmorItem(
                "Emerald Chestplate", minicraft.item.ArmorItem.ArmorSlot.CHESTPLATE,
                0.65f, "armor_emerald_chest", "Emerald",
                1.8f, 0.95f, 0.45f, null), 1);
            chestInv.add(new minicraft.item.ArmorItem(
                "Emerald Leggings", minicraft.item.ArmorItem.ArmorSlot.LEGGINGS,
                0.55f, "armor_emerald_legs", "Emerald",
                1.4f, 1.0f, 0.40f, null), 1);
            chestInv.add(new minicraft.item.ArmorItem(
                "Emerald Boots", minicraft.item.ArmorItem.ArmorSlot.BOOTS,
                0.35f, "armor_emerald_boots", "Emerald",
                0.9f, 1.1f, 0.30f, null), 1);

            chestInv.add(new minicraft.item.ToolItem(
                "Vine Whip Sword", minicraft.item.ToolItem.ToolType.SWORD,
                4, 14.0f, "item_sword_emerald"), 1);
            chestInv.add(new minicraft.item.ToolItem(
                "Ancient Bark Axe", minicraft.item.ToolItem.ToolType.AXE,
                4, 15.0f, "item_axe_emerald"), 1);
        }
    }
}
