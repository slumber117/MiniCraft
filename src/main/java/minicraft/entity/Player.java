package minicraft.entity;

import minicraft.renderer.Camera;
import minicraft.item.ToolItem;
import minicraft.item.FoodItem;
import minicraft.math.Vector3f;
import minicraft.world.World;
import minicraft.world.Block;
import minicraft.world.WorldCell;

public class Player extends Entity {
    public float maxHunger = 100f;
    public float hunger = 100f;
    public float maxThirst= 100f, thirst = 100f;
    
    // Piloting
    private minicraft.entity.ship.ShipEntity ridingShip = null;
    
    // Combat Stats
    public float damageFlashTimer = 0f;
    private float invincibilityTimer = 0f;
    private static final float INVINCIBILITY_TIME = 0.5f;

    // Temperature in Celsius
    public float temperature = 36.6f;
    public String tempState = "Normal";
    
    // Environmental Cooldowns
    public float transmatCooldown = 0f;
    public float jumpTimer = 0f;
    public float coyoteTime = 0f;
    
    // Vanguard Ability Cooldowns
    public float vanguardCooldown = 0f; // Antimatter
    public float darkmatterCooldown = 0f;
    public float gammaRayCooldown = 0f;
    public float nebulaCooldown = 0f;

    public final Inventory inventory = new Inventory();
    public float miningProgress = 0f;
    public float eatingProgress = 0f;

    // Leveling
    public int level = 1;
    public float xp = 0;
    public float xpToNextLevel = 15f;
    
    public boolean isGrounded = false;
    public boolean isInWater  = false;
    private Camera camera;

    // Quest hooks
    public java.util.function.Consumer<EntityType> onKillCallback = null;
    public float totalDistanceTravelled = 0f;

    public Player(Camera camera) {
        super(EntityType.PLAYER, 100f);
        this.camera = camera;
        this.width = 0.6f;
        this.height = 1.8f;
        
        // Starting Tools - Removed Stone Pickaxe for progression
        inventory.add(new ToolItem("Wood Pick", ToolItem.ToolType.PICKAXE, 0, 2.0f, "item_pick_wood"), 1);
        
        // Give player a Torch and a Sword to test the new combat
        minicraft.item.Item torch = new minicraft.item.Item("TORCH", Block.TORCH);
        inventory.add(torch, 10);
        inventory.setOffhandItem(torch); 
        
        ToolItem sword = new ToolItem("Bronze Sword", ToolItem.ToolType.SWORD, 2, 5.0f, "item_sword_bronze"); // Fast swing
        inventory.add(sword, 1);
        
        // Give food to test eating
        inventory.add(new FoodItem("COOKED_MEAT", "item_meat_cooked", 25f, 40f), 10);
    }

    private float plutoniumPulseTimer = 0f;

    public float radiationLevel = 0f;
    private float radiationDamageTimer = 0f;
    private float geigerClickTimer = 0f;

    private void respawn() {
        this.dead = false;
        this.health = this.maxHealth;
        this.hunger = this.maxHunger;
        this.thirst = this.maxThirst;
        
        // Find safe surface Y at current location
        // We'll be moved to the surface of where we died
        // Access to world is needed, we'll handle this in tick or pass it
        this.needsRespawn = true;
    }

    private boolean needsRespawn = false;
    public float attackTimer = 0f;

    @Override
    public void tick(EntityManager manager, World world, ParticleManager particleManager, float dt) {
        if (needsRespawn) {
            int respawnY = world.getSafeSpawnY((int)position.x, (int)position.y, (int)position.z);
            position.y = (float)respawnY + 0.1f;
            velocity.set(0,0,0);
            needsRespawn = false;
            System.out.println("[PLAYER] Respawned at Y=" + respawnY);
        }

        super.tick(manager, world, particleManager, dt);
        if (dead) return;

        // --- Timers ---
        if (invincibilityTimer > 0) invincibilityTimer -= dt;
        if (transmatCooldown > 0) transmatCooldown -= dt;
        if (jumpTimer > 0) jumpTimer -= dt;
        if (coyoteTime > 0) coyoteTime -= dt;
        if (isGrounded) coyoteTime = 0.15f; // Reset coyote time when on ground
        if (vanguardCooldown > 0) vanguardCooldown -= dt;
        if (darkmatterCooldown > 0) darkmatterCooldown -= dt;
        if (gammaRayCooldown > 0) gammaRayCooldown -= dt;
        if (nebulaCooldown > 0) nebulaCooldown -= dt;

        // 1. Survival ticks
        hunger = Math.max(0, hunger - 0.05f * dt);
        thirst = Math.max(0, thirst - 0.1f * dt);
        
        // 2. Physics & Collision
        applyGravity(dt, world);
        resolveMovement(dt, world);

        // --- Radiation Logic (Environmental) ---
        updateRadiation(dt, world);

        // --- Plutonium Radiation Pulse ---
        if (inventory.hasFullSet("Plutonium")) {
            plutoniumPulseTimer += dt;
            if (plutoniumPulseTimer >= 2.0f) {
                plutoniumPulseTimer = 0f;
                // Emit pulse: damage nearby hostiles
                for (Entity e : manager.getNearby(position.x, position.y, position.z, 6.0f)) {
                    if (e != this && e.type.isHostile()) {
                        e.damage(e.getMaxHealth() * 0.25f, this); // 25% pulse
                        particleManager.spawnSmoke(e.position.x, e.position.y + 1f, e.position.z);
                    }
                }
            }
        }
        
        // --- Xenotime Omega Radiation Pulse ---
        if (inventory.hasFullSet("Xenotime")) {
            plutoniumPulseTimer += dt;
            if (plutoniumPulseTimer >= 1.5f) { // Faster pulse
                plutoniumPulseTimer = 0f;
                // Emit huge pulse (Double area: 6.0f -> 12.0f)
                for (Entity e : manager.getNearby(position.x, position.y, position.z, 12.0f)) {
                    if (e != this && e.type.isHostile()) {
                        e.damage(e.getMaxHealth() * 0.35f, this); // 35% pulse
                        e.applyRadiationSickness(5.0f); // Sickness from pulse
                        for(int i=0; i<3; i++) particleManager.spawnSmoke(e.position.x, e.position.y + 1f, e.position.z);
                    }
                }
                // Visual aura at player
                for(int i=0; i<15; i++) {
                   particleManager.spawnSmoke(position.x + (rng.nextFloat()-0.5f)*2f, position.y, position.z + (rng.nextFloat()-0.5f)*2f);
                }
            }
            // Continuous particles
            if (rng.nextFloat() < 0.3f) {
                particleManager.spawnSmoke(position.x, position.y, position.z);
            }
        }
        
        // --- Bastnaesite Obsidian Smoke Particles ---
        if (inventory.hasFullSet("Bastnaesite")) {
            if (rng.nextFloat() < 0.4f) {
                // Black smoke
                particleManager.spawnSmoke(position.x, position.y, position.z);
            }
        }

        // 3. Environment (Temp & Hazards)
        updateEnvironment(dt, world);
        checkBlockHazards(dt, world);
        
        // 4. Effects
        updateHealthEffects(dt);
        
        // 5. Environmental Interactions (Transmat Portal)
        Block floor = world.getBlock(
                (int)Math.floor(position.x), 
                (int)Math.floor(position.y - 0.2f), 
                (int)Math.floor(position.z)
        );
        if (floor == Block.TRANSMAT_PAD && transmatCooldown <= 0f) {
            // Safety: Only trigger if we are within the base camp range or station range
            // This prevents "shadow pads" from caves triggering accidental ascension
            if (position.y > 60 || position.y > 240) {
            if (position.y < 230) {
                // Ground portal triggers ascension to Shipyard Sky Deck
                System.out.println("TRANSMAT: Routing to Orbital Coordinates...");
                
                // Warp effect at old position
                for(int i=0; i<10; i++) particleManager.spawnSmoke(position.x, position.y, position.z);
                
                position.y = 241f; // Target altitude
                velocity.set(0,0,0);
                transmatCooldown = 3.0f; // Prevent immediate regression loop
                
                // Warp effect at new position
                for(int i=0; i<10; i++) particleManager.spawnSmoke(position.x, position.y, position.z);
            } else if (position.y >= 240) {
                // Sky portal triggers regression to Mountain peak
                System.out.println("TRANSMAT: Descending to Base Camp...");
                
                // Warp effect
                for(int i=0; i<10; i++) particleManager.spawnSmoke(position.x, position.y, position.z);

                int cx = (int)Math.floor(position.x / 16.0);
                int cz = (int)Math.floor(position.z / 16.0);
                
                position.y = world.getSafeSpawnY(cx*16+12, cz*16+12) + 2; 
                velocity.set(0,0,0);
                transmatCooldown = 3.0f; // Prevent immediate ascension loop

                // Warp effect at new location
                for(int i=0; i<10; i++) particleManager.spawnSmoke(position.x, position.y, position.z);
            }
        }
    }
}

    // Keep the update method for mouse look / independent updates if needed, 
    // but physics should stay in tick or synced.
    public void update(float dt, World world) {
        // Now mostly handled in tick for fixed-rate physics consistency.
        // We can place non-fixed stuff here.
    }

    private void applyGravity(float dt, World world) {
        // Detect water at torso level
        Block torsoBlock = world.getBlock(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y + 0.9f),
                (int) Math.floor(position.z));
        isInWater = (torsoBlock == Block.WATER);

        if (isInWater) {
            // Buoyancy: gentle sink, not free-fall
            velocity.y -= 4.0f * dt;
            if (velocity.y < -4.0f) velocity.y = -4.0f;
            // Water drag on all axes
            float drag = (float) Math.pow(0.85f, dt * 20f);
            velocity.y *= drag;
            velocity.x *= (float) Math.pow(0.75f, dt * 20f);
            velocity.z *= (float) Math.pow(0.75f, dt * 20f);
        } else if (!isGrounded) {
            velocity.y -= 16.0f * dt;
            if (velocity.y < -20.0f) velocity.y = -20.0f;
        }
    }

    private void resolveMovement(float dt, World world) {
        // ── Horizontal ───────────────────────────────────────────────────────
        if (!isColliding(position.x + velocity.x * dt, position.y, position.z, world)) {
            float dx = velocity.x * dt;
            position.x += dx;
            totalDistanceTravelled += Math.abs(dx);
        } else {
            velocity.x = 0;
        }
        if (!isColliding(position.x, position.y, position.z + velocity.z * dt, world)) {
            float dz = velocity.z * dt;
            position.z += dz;
            totalDistanceTravelled += Math.abs(dz);
        } else {
            velocity.z = 0;
        }

        // ── Vertical with sub-stepping (prevents tunnelling) ─────────────────
        final float MAX_STEP = 0.4f;
        float remainingY = velocity.y * dt;
        isGrounded = false;
        while (Math.abs(remainingY) > 0.001f) {
            float step = Math.max(-MAX_STEP, Math.min(MAX_STEP, remainingY));
            float nextY = position.y + step;
            if (!isColliding(position.x, nextY, position.z, world)) {
                position.y = nextY;
            } else {
                if (velocity.y < 0) {
                    position.y = (float) Math.floor(position.y);
                    isGrounded = true;
                }
                velocity.y = 0;
                break;
            }
            remainingY -= step;
        }

        // ── Friction (skip in water — drag handled in applyGravity) ──────────
        if (!isInWater) {
            float speedMod = inventory.getTotalSpeedMod();
            if (inventory.hasFullSet("Xenotime")) speedMod *= 1.40f; // Xenotime 40% Speed Boost
            if (inventory.hasFullSet("Bastnaesite")) speedMod *= 1.30f; // Bastnaesite 30% Speed Boost
            
            // --- VANGUARD SPEED BOOSTS ---
            if (inventory.hasFullSet("Antimatter")) speedMod *= 1.75f; // 50% faster than Topaz (1.15 * 1.5 = 1.725)
            if (inventory.hasFullSet("Darkmatter")) speedMod *= 1.65f;
            if (inventory.hasFullSet("Gamma Ray")) speedMod *= 1.50f;
            if (inventory.hasFullSet("Nebula")) speedMod *= 2.00f;
            
            velocity.x *= 0.8f * speedMod;
            velocity.z *= 0.8f * speedMod;
        }
    }

    public void meleeAttack(EntityManager manager, ParticleManager pm) {
        float range = 3.5f; // Slightly increased range for epic swords
        float cos45 = (float) Math.cos(Math.toRadians(45));

        minicraft.item.Item held = inventory.getSelectedItem();
        float baseDmg = 20f;
        float percentDmg = 0.0f;
        boolean instaKill = false;
        float bossPercent = 0.0f;

        if (held instanceof ToolItem) {
            ToolItem tool = (ToolItem) held;
            if (tool.getToolType() == ToolItem.ToolType.SWORD) {
                baseDmg = tool.getEfficiency() * 4f; // Scaled sword damage
                percentDmg = tool.getPercentDamage();
                instaKill = tool.isInstaKill();
                bossPercent = tool.getBossPercentDamage();
                
                // Set cooldown based on attack speed
                attackTimer = 0.4f / tool.attackSpeedMultiplier;
            } else {
                attackTimer = 0.5f; // Default swing speed for tools
            }
        } else {
            attackTimer = 0.6f; // Hand speed
        }

        for (Entity e : manager.getNearby(position.x, position.y, position.z, range)) {
            if (e == this) continue; 
            if (e.dead) continue;
            
            float dx = e.position.x - position.x;
            float dz = e.position.z - position.z;
            float dist = (float) Math.sqrt(dx*dx + dz*dz);
            if (dist == 0) continue;
            
            dx /= dist; dz /= dist;
            float fx = (float) -Math.sin(Math.toRadians(camera.getRotation().y));
            float fz = (float) -Math.cos(Math.toRadians(camera.getRotation().y));
            
            float dot = dx * fx + dz * fz;
            if (dot > cos45) {
                float finalDamage = baseDmg;
                
                // --- Bastnaesite Sword Logic ---
                if (held != null && held.getDisplayName().equals("Bastnaesite Sword")) {
                    finalDamage = 800f; 
                    if (e.getType() == EntityType.ONYX_DRAGON) {
                        finalDamage *= 2.5f; // Dragon Slayer
                    }
                    
                    // Summon Solar Beam (First hit in loop summons it)
                    if (e == manager.getNearby(position.x, position.y, position.z, range).get(0)) {
                        summonSolarBeam(manager, pm);
                    }
                }
                
                // --- Xenotime Sword Logic ---
                if (held != null && held.getDisplayName().equals("Xenotime Sword")) {
                    finalDamage = 600f; // The Zenith Strike
                    e.applyRadiationSickness(10.0f); // Severe radiation
                }
                
                // --- VANGUARD TIER SWORDS ---
                
                // Citadel (Antimatter)
                if (held != null && held.getDisplayName().equals("Citadel")) {
                    finalDamage = 20000.0f;
                    if (vanguardCooldown <= 0) {
                        summonAntimatterExplosion(manager, pm);
                        vanguardCooldown = 12.0f;
                    }
                }
                
                // Darkmatter Sword
                if (held != null && (held.getDisplayName().equals("Darkmatter Sword") || held.getDisplayName().equals("Darkmatter"))) {
                    finalDamage = 15000.0f;
                    if (darkmatterCooldown <= 0) {
                        summonDarkmatterBeam(manager, pm);
                        darkmatterCooldown = 12.0f;
                    }
                }
                
                // Gamma Ray Sword
                if (held != null && (held.getDisplayName().equals("Gamma Ray Sword") || held.getDisplayName().equals("Gamma Ray"))) {
                    finalDamage = 175000.0f;
                    if (gammaRayCooldown <= 0) {
                        summonBlackHole(manager, pm);
                        gammaRayCooldown = 12.0f;
                    }
                }
                
                // Nebula Sword
                if (held != null && (held.getDisplayName().equals("Nebula Sword") || held.getDisplayName().equals("Nebula"))) {
                    finalDamage = 200000.0f;
                    if (nebulaCooldown <= 0) {
                        summonNebulaBeams(manager, pm);
                        nebulaCooldown = 12.0f;
                    }
                }
                
                // --- Garnet Sword Special Logic ---
                if (held != null && held.getDisplayName().equals("Garnet Sword")) {
                    finalDamage = 100f; // 4 hits to kill 400HP Orc
                }
                
                // --- Topaz Sword Special Logic ---
                if (held != null && held.getDisplayName().equals("Topaz Sword")) {
                    if (e.getType() == EntityType.ZOMBIE || e.getType() == EntityType.SPIDER) {
                        finalDamage *= 2.0f; // Smites common arthropods and undead
                    }
                }
                
                // --- Topaz Sword Special Logic ---
                
                // --- Radiation Blade Special Logic ---
                if (held != null && held.getDisplayName().equals("Radiation Blade")) {
                    if (e.getType() == EntityType.TROLL) {
                        e.applyRadiation(15f); // DoT damage
                        finalDamage = e.getMaxHealth() * 0.40f; 
                    } else if (e.getMaxHealth() <= 50f) { // Spiders, Zombies, etc.
                        finalDamage = e.getHealth() + 1000f; // Vaporize
                    }
                }
                
                // --- Specialized Logic ---
                if (percentDmg > 0) {
                    finalDamage = e.getHealth() * percentDmg;
                }
                
                if (instaKill) {
                    // Larger mobs = 80+ health (Whales, Bosses)
                    if (e.getMaxHealth() >= 80f) {
                        finalDamage = e.getMaxHealth() * bossPercent;
                    } else {
                        finalDamage = e.getHealth() + 100f; // Insta-kill
                    }
                }

                boolean wasDead = e.isDead();
                e.damage(finalDamage, this);
                if (!wasDead && e.isDead()) {
                    addXp(e.getType().isHostile() ? 10f : 2f, pm);
                    if (onKillCallback != null) onKillCallback.accept(e.getType());
                }
                e.applyKnockback(dx * 4f, 1.5f, dz * 4f);
            }
        }
    }

    private void summonSolarBeam(EntityManager manager, ParticleManager pm) {
        // Simple beam: damage in a line 15 blocks out
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) (-Math.sin(pitch));
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));

        for (float d = 1.0f; d < 15.0f; d += 0.5f) {
            float bx = position.x + dx * d;
            float by = (position.y + 1.5f) + dy * d;
            float bz = position.z + dz * d;
            
            // Visuals
            pm.spawnSmoke(bx, by, bz); // Placeholder for beam particles
            
            for (Entity target : manager.getNearby(bx, by, bz, 1.5f)) {
                if (target != this && target.type.isHostile()) {
                    target.damage(400f, this);
                    target.applyBurn(3.0f);
                }
            }
        }
        System.out.println("SOLAR BEAM FIRED!");
    }

    @Override
    public void damage(float amount, Entity attacker) {
        if (invincibilityTimer > 0) return; 
        
        float defense = inventory.getTotalDefense();
        float reduction = 1.0f - defense;
        float actualDamage = amount * reduction;
        
        // Neodymium 30% Damage Reduction
        if (inventory.hasFullSet("Neodymium")) actualDamage *= 0.70f;
        
        // Onyx 35% Damage Reduction (Hardness)
        if (inventory.hasFullSet("Onyx")) actualDamage *= 0.65f;

        // Obsidian 45% Damage Reduction (Hardness is now 45% as requested, but prot=0.45 already handles base)
        // User asked for "armour hardness of reducing 45% incoming damage"
        // If prot=0.45, then actualDamage = amount * (1.0 - 0.45) = amount * 0.55.
        // This already reduces 45%. 
        
        // Obsidian Fireball Reduction (50%)
        if (inventory.hasFullSet("Obsidian") && attacker != null) {
            if (attacker.type == EntityType.FIREBALL || attacker.type == EntityType.GOLD_FIREBALL || attacker.type == EntityType.FIRE_DEMON) {
                actualDamage *= 0.50f; // 50% Reduction
            }
        }
        
        // Xenotime Projectile/Fireball Resistance
        if (inventory.hasFullSet("Xenotime") && attacker != null) {
            if (attacker.type == EntityType.FIREBALL || attacker.type == EntityType.GOLD_FIREBALL || attacker.type == EntityType.FIRE_DEMON) {
                actualDamage *= 0.35f; // 65% Reduction
            }
        }
        
        // Bastnaesite Projectile/Fireball Resistance
        if (inventory.hasFullSet("Bastnaesite") && attacker != null) {
            if (attacker.type == EntityType.FIREBALL || attacker.type == EntityType.GOLD_FIREBALL || attacker.type == EntityType.FIRE_DEMON) {
                actualDamage *= 0.30f; // 70% Reduction
            }
        }
        
        // --- VANGUARD ARMOR LOGIC ---
        
        // Antimatter: 90% Damage Reduction
        if (inventory.hasFullSet("Antimatter")) {
            actualDamage *= 0.10f; 
        }
        
        // Gamma Ray: 80% Damage Reduction
        if (inventory.hasFullSet("Gamma Ray")) {
            actualDamage *= 0.20f;
        }
        
        // Darkmatter: 85% Damage Reduction
        if (inventory.hasFullSet("Darkmatter")) {
            actualDamage *= 0.15f;
        }

        super.damage(actualDamage, attacker);
        
        damageFlashTimer = 1.0f;
        invincibilityTimer = INVINCIBILITY_TIME;

        // --- Armor Abilities (Thorns & Reflection) ---
        if (attacker != null) {
            // Antimatter Fireball Reflection (70% back to Dragons)
            if (inventory.hasFullSet("Antimatter")) {
                if (attacker.type == EntityType.FIREBALL || attacker.type == EntityType.GOLD_FIREBALL) {
                    attacker.damage(amount * 0.70f, this);
                }
            }
            
            // Gamma Ray Fireball Reflection (55% back to Dragons)
            if (inventory.hasFullSet("Gamma Ray")) {
                if (attacker.type == EntityType.FIREBALL || attacker.type == EntityType.GOLD_FIREBALL) {
                    attacker.damage(amount * 0.55f, this);
                }
            }
            // Bastnaesite Nova Blast (Melee Counter)
            if (inventory.hasFullSet("Bastnaesite")) {
                // Trigger Nova Blast (10 block radius)
                if (this.manager != null) {
                    System.out.println("NOVA BLAST!");
                    for (Entity e : this.manager.getNearby(position.x, position.y, position.z, 10.0f)) {
                        if (e != this && e.type.isHostile()) {
                            e.damage(20f, this);
                            e.applyBurn(5.0f); // 5s burn
                        }
                    }
                }
            }
            
            // Topaz (Reactive Reflection - 20% damage reflected back)
            if (inventory.hasFullSet("Topaz")) {
                attacker.damage(actualDamage * 0.20f, this);
            }
            
            // Obsidian (Reactive Reflection - 15% melee damage reflected back)
            if (inventory.hasFullSet("Obsidian")) {
                boolean isProjectile = attacker.type == EntityType.FIREBALL || 
                                       attacker.type == EntityType.GOLD_FIREBALL || 
                                       attacker.type == EntityType.ONYX_PROJECTILE ||
                                       attacker.type == EntityType.SHIP_MISSILE;
                
                if (!isProjectile) {
                    // It's a direct contact attack (melee/bite)
                    attacker.damage(actualDamage * 0.15f, this);
                    if (this.manager != null) {
                        // Visual effect for reflection
                        // pm.spawnSmoke(attacker.position.x, attacker.position.y + 1f, attacker.position.z);
                    }
                }
            }
            
            // Agate (Reactive Reflection)
            if (inventory.hasFullSet("Agate")) {
                boolean isProjectile = attacker.type == EntityType.FIREBALL || 
                                       attacker.type == EntityType.GOLD_FIREBALL || 
                                       attacker.type == EntityType.ONYX_PROJECTILE ||
                                       attacker.type == EntityType.SHIP_MISSILE;
                
                if (!isProjectile) {
                    // Melee Reflection (20%)
                    attacker.damage(actualDamage * 0.20f, this);
                } else {
                    // Fireball/Projectile Reflection (10%)
                    attacker.damage(actualDamage * 0.10f, this);
                }
            }
            // Tanzanite (Sharp/Thorns)
            if (inventory.hasPiece("Tanzanite")) {
                attacker.damage(5.0f, this); // Flat thorn damage
            }
            // Diamond (Reflect)
            if (inventory.hasPiece("Diamond")) {
                attacker.damage(actualDamage * 0.20f, this); // Reflect 20%
            }
        }
        
        System.out.println("Player took damage! Health: " + health);

        // Custom Respawn Logic: Don't stay dead, just respawn
        if (dead) {
            respawn();
        }
    }

    private boolean isColliding(float x, float y, float z, World world) {
        float halfW = width / 2f;
        float h = height;
        float[][] points = {
            {x - halfW, y, z - halfW}, {x + halfW, y, z - halfW},
            {x - halfW, y, z + halfW}, {x + halfW, y, z + halfW},
            {x - halfW, y + h, z - halfW}, {x + halfW, y + h, z - halfW},
            {x - halfW, y + h, z + halfW}, {x + halfW, y + h, z + halfW},
            {x - halfW, y + h/2f, z - halfW}, {x + halfW, y + h/2f, z - halfW},
            {x - halfW, y + h/2f, z + halfW}, {x + halfW, y + h/2f, z + halfW},
            {x, y + h/2f, z}
        };

        for (float[] p : points) {
            if (world.getBlock((int)Math.floor(p[0]), (int)Math.floor(p[1]), (int)Math.floor(p[2])).solid) {
                return true;
            }
        }
        return false;
    }

    public Vector3f getPosition() { return position; }
    public void setPosition(float x, float y, float z) { position.set(x, y, z); }

    private void updateEnvironment(float dt, World world) {
        int gx = (int) Math.floor(position.x);
        int gz = (int) Math.floor(position.z);
        WorldCell cell = world.getGenerator().generate(gx, gz); // Use generator for stability outside chunks
        
        float worldTemp = (cell != null) ? cell.temperature : 0.5f;
        // Map 0..1 back to -30C..40C
        float baseTemp = worldTemp * 70f - 30f;
        
        // 0. Thermal Lock (Legendary Sets)
        if (inventory.hasFullSet("Painite") || inventory.hasFullSet("Neodymium") || inventory.hasFullSet("Garnet")) {
            temperature = 36.6f;
            tempState = "Normal";
            return;
        }
        
        float altitude = position.y;
        
        // 1. Geothermal Boost (Thermal Gradient for deep layers)
        if (altitude < 80) {
            float depthFactor = Math.max(0, (80 - altitude) / 80f);
            baseTemp += depthFactor * 17.0f; 
        }

        // 2. Altitude Cold Factor (Starts at 102 sea level)
        float altitudeColdFactor = Math.max(0, (altitude - 102) * 0.15f);
        
        // 3. Armor Insulation
        float insulation = inventory.getTotalInsulation();
        float protection = Math.min(0.95f, insulation);
        
        // Cold protection
        float effectiveCold = altitudeColdFactor * (1.0f - protection);
        temperature = baseTemp - effectiveCold;

        // 4. Clothing/Armor Stress: Leather heats you up further if already warm
        if (temperature >= 15.0f && inventory.hasFullSet("Leather")) {
            temperature += 10.0f; 
        }

        if (temperature < -5.0f) tempState = "Severe Hypothermia";
        else if (temperature < 10.0f) tempState = "Cold";
        else if (temperature < 20.0f) tempState = "Okay";
        else if (temperature > 30.0f) tempState = "Too Warm";
        else tempState = "Warm";
    }

    private void updateHealthEffects(float dt) {
        // Calculate dynamic max health based on armor set
        float armorHealthBonus = inventory.getTotalHealthBonus();
        float currentMaxHealth = 100f * (1.0f + armorHealthBonus);
        this.maxHealth = currentMaxHealth; 

        if (tempState.equals("Severe Hypothermia")) {
            health = Math.max(0, health - 2.5f * dt); // Lethal
        } else if (tempState.equals("Cold") || tempState.equals("Too Warm")) {
            health = Math.max(0, health - 0.5f * dt); // Stress damage
        }
        
        // Normal survival (Hunger/Thirst)
        if (hunger <= 0 || thirst <= 0) {
            health = Math.max(0, health - 1.0f * dt);
        } else if (tempState.equals("Okay") || tempState.equals("Warm")) {
                // Regeneration
            if (hunger > 80 && thirst > 80) {
                float regenRate = 0.15f;
                // Neodymium 10% Health Regen Boost
                if (inventory.hasFullSet("Neodymium")) regenRate *= 1.10f;
                // Emerald "Verdant Mending" Bonus
                if (inventory.hasFullSet("Emerald")) regenRate += 0.50f; 
                
                health = Math.min(maxHealth, health + regenRate * dt);
            } else if (inventory.hasFullSet("Emerald")) {
                // Emerald heals even if not full hunger, just slower
                health = Math.min(maxHealth, health + 0.20f * dt);
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public void setRiding(minicraft.entity.ship.ShipEntity ship) {
        this.ridingShip = ship;
    }

    public minicraft.entity.ship.ShipEntity getRidingShip() {
        return ridingShip;
    }

    public boolean isRiding() {
        return ridingShip != null;
    }

    private void summonAntimatterExplosion(EntityManager manager, ParticleManager pm) {
        System.out.println("ANTIMATTER ATOM DETONATED!");
        // Visual: Sphere of smoke/sparks
        for(int i=0; i<50; i++) {
            float rx = (rng.nextFloat()-0.5f)*10f;
            float ry = (rng.nextFloat()-0.5f)*10f;
            float rz = (rng.nextFloat()-0.5f)*10f;
            pm.spawnSmoke(position.x + rx, position.y + ry, position.z + rz);
        }
        for (Entity e : manager.getNearby(position.x, position.y, position.z, 15.0f)) {
            if (e != this && e.type.isHostile()) {
                e.damage(150000.0f, this);
            }
        }
    }

    private void summonDarkmatterBeam(EntityManager manager, ParticleManager pm) {
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) Math.sin(yaw);
        float dz = (float) -Math.cos(yaw);
        
        for (float d = 1.0f; d < 20.0f; d += 0.5f) {
            float bx = position.x + dx * d;
            float by = position.y + 1.0f;
            float bz = position.z + dz * d;
            pm.spawnSmoke(bx, by, bz); // Black/Purple smoke
            
            for (Entity target : manager.getNearby(bx, by, bz, 2.0f)) {
                if (target != this && target.type.isHostile()) {
                    // Darkmatter Poison: 15% health/s for 4s
                    target.damage(target.getMaxHealth() * 0.15f, this);
                    target.applyRadiationSickness(4.0f); // Use radiation as poison base
                }
            }
        }
    }

    private void summonBlackHole(EntityManager manager, ParticleManager pm) {
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float bx = position.x + (float)Math.sin(yaw) * 5f;
        float bz = position.z + (float)-Math.cos(yaw) * 5f;
        float by = position.y + 1f;

        // Effect is persistent damage in area
        System.out.println("BLACK HOLE STABILIZED!");
        for (Entity e : manager.getNearby(bx, by, bz, 6.0f)) {
            if (e != this && e.type.isHostile()) {
                e.damage(e.getMaxHealth() * 0.12f, this); // 12% per hit
                e.applyKnockback((bx - e.position.x)*0.5f, 0, (bz - e.position.z)*0.5f); // Pull in
            }
        }
        for(int i=0; i<20; i++) pm.spawnSmoke(bx + (rng.nextFloat()-0.5f)*4f, by + (rng.nextFloat()-0.5f)*4f, bz + (rng.nextFloat()-0.5f)*4f);
    }

    private void summonNebulaBeams(EntityManager manager, ParticleManager pm) {
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float fx = (float) Math.sin(yaw);
        float fz = (float) -Math.cos(yaw);
        
        for (int i = 0; i < 5; i++) {
            float offset = (i - 2) * 1.5f;
            float startX = position.x + fx * 10f + (float)Math.cos(yaw) * offset;
            float startZ = position.z + fz * 10f + (float)Math.sin(yaw) * offset;
            
            pm.spawnSmoke(startX, position.y + 1f, startZ);
            for (Entity e : manager.getNearby(startX, position.y + 1f, startZ, 3.0f)) {
                if (e != this && e.type.isHostile()) {
                    e.damage(e.getMaxHealth() * 0.05f, this);
                    e.applyBurn(2.0f);
                }
            }
        }
    }

    private void checkBlockHazards(float dt, World world) {
        // Check blocks at feet and head
        Block b1 = world.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z));
        Block b2 = world.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y + 1.2f), (int)Math.floor(position.z));
        
        if (b1 == Block.LAVA || b2 == Block.LAVA) {
            if (!inventory.hasFullSet("Onyx")) {
                damage(80f * dt, null); // Instant high-damage vaporization
                velocity.y = 5.0f; // Kick player back to safety
            }
        } else if (b1 == Block.MAGMA || b2 == Block.MAGMA) {
            if (!inventory.hasFullSet("Onyx")) {
                damage(15f * dt, null); // Heat burn
            }
        }
    }

    public void handleMouseInput(float dx, float dy) {
        camera.handleMouseInput(dx, dy);
    }

    public void addXp(float amount, ParticleManager pm) {
        if (level >= 100) return;
        
        // Global 20% XP Increase
        amount *= 1.20f;
        
        // Painite XP Boost (Stacking)
        if (inventory.hasFullSet("Painite")) amount *= 1.20f;
        
        // Neodymium XP Boost (30%)
        if (inventory.hasFullSet("Neodymium")) amount *= 1.30f;
        
        // Onyx XP Boost (35%)
        if (inventory.hasFullSet("Onyx")) amount *= 1.35f;
        
        this.xp += amount;
        while (this.xp >= xpToNextLevel && level < 100) {
            levelUp(pm);
        }
    }

    private void levelUp(ParticleManager pm) {
        this.xp -= xpToNextLevel;
        this.level++;
        this.health = this.maxHealth; // Refill health
        this.xpToNextLevel *= 1.15f;
        if (pm != null) {
            pm.spawnLevelUp(position.x, position.y, position.z);
        }
    }

    private void updateRadiation(float dt, World world) {
        float ambientRad = 0f;
        
        // Scan 5-block radius for radioactive ores
        int px = (int)Math.floor(position.x);
        int py = (int)Math.floor(position.y);
        int pz = (int)Math.floor(position.z);
        int radius = 5;

        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = py - radius; y <= py + radius; y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    Block b = world.getBlock(x, y, z);
                    if (b == Block.URANIUM_ORE || b == Block.PLUTONIUM_ORE) {
                        float dist = (float) Math.sqrt((x-px)*(x-px) + (y-py)*(y-py) + (z-pz)*(z-pz));
                        if (dist <= radius) {
                            // Fluctuating strength based on proximity and time
                            float flicker = 0.8f + 0.4f * (float)Math.random();
                            float strength = b == Block.PLUTONIUM_ORE ? 2.5f : 1.0f;
                            ambientRad += (strength * flicker) / (dist + 1f);
                        }
                    }
                }
            }
        }

        // Shielding logic (Armor protects from environmental radiation)
        boolean isProtected = inventory.hasFullSet("Plutonium") || inventory.hasFullSet("Mithril");
        if (isProtected) ambientRad = 0f; // Complete protection for industrial tier

        // Smoothly adjust player radiation level based on ambient intensity
        if (ambientRad > radiationLevel) {
            float rate = Math.max(2.0f, (ambientRad - radiationLevel) * 0.5f);
            radiationLevel = Math.min(ambientRad, radiationLevel + dt * rate);
        } else {
            radiationLevel = Math.max(0, radiationLevel - dt * 2.5f); // Natural decay
        }

        // Apply damage
        if (radiationLevel >= 100f) {
            radiationDamageTimer += dt;
            if (radiationDamageTimer >= 0.5f) {
                damage(10f, null);
                radiationDamageTimer = 0f;
            }
        }
        
        // --- Geiger Counter Clicks ---
        geigerClickTimer += dt;
        float clickInterval = Math.max(0.05f, 1.5f - (radiationLevel / 50f));
        if (geigerClickTimer >= clickInterval && radiationLevel > 0) {
            geigerClickTimer = 0f;
            System.out.println("[GEIGER] CLICK");
        }
    }

    @Override public boolean isPlayer() { return true; }

    public minicraft.math.Vector3f getAuraColor() {
        // 1. Tool/Item Aura
        minicraft.item.Item held = inventory.getSelectedItem();
        if (held instanceof ToolItem && ((ToolItem) held).auraColor != null) {
            return ((ToolItem) held).auraColor;
        }
        
        // 2. Armor Set Glow
        return inventory.getDominantGlow();
    }
}
