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

    @Override
    public void tick(EntityManager manager, World world, ParticleManager particleManager, float dt) {
        super.tick(manager, world, particleManager, dt);
        if (dead) return;

        // --- Timers ---
        if (invincibilityTimer > 0) invincibilityTimer -= dt;
        if (transmatCooldown > 0) transmatCooldown -= dt;

        // 1. Survival ticks
        hunger = Math.max(0, hunger - 0.05f * dt);
        thirst = Math.max(0, thirst - 0.1f * dt);
        
        // 2. Physics & Collision
        applyGravity(dt, world);
        resolveMovement(dt, world);

        // --- Radiation Logic ---
        updateRadiation(dt);

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
            }
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

    @Override
    public void damage(float amount, Entity attacker) {
        if (invincibilityTimer > 0) return; 
        
        float defense = inventory.getTotalDefense();
        float reduction = 1.0f - defense;
        float actualDamage = amount * reduction;
        super.damage(actualDamage, attacker);
        
        damageFlashTimer = 1.0f;
        invincibilityTimer = INVINCIBILITY_TIME;

        // --- Armor Abilities (Thorns & Reflection) ---
        if (attacker != null) {
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
                health = Math.min(maxHealth, health + 0.15f * dt);
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

    private void checkBlockHazards(float dt, World world) {
        // Check blocks at feet and head
        Block b1 = world.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z));
        Block b2 = world.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y + 1.2f), (int)Math.floor(position.z));
        
        if (b1 == Block.LAVA || b2 == Block.LAVA) {
            damage(80f * dt, null); // Instant high-damage vaporization
            // Kick player back to safety
            velocity.y = 5.0f; 
        } else if (b1 == Block.MAGMA || b2 == Block.MAGMA) {
            damage(15f * dt, null); // Heat burn
        }
    }

    public void handleMouseInput(float dx, float dy) {
        camera.handleMouseInput(dx, dy);
    }

    public void addXp(float amount, ParticleManager pm) {
        if (level >= 100) return;
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

    private void updateRadiation(float dt) {
        float ambientRad = 0f;
        
        // Check inventory for radioactive materials
        for (minicraft.item.ItemStack stack : inventory.getHotbar()) {
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getItem().getName();
            if (name.contains("URANIUM")) ambientRad += 0.05f * stack.getCount();
            if (name.contains("PLUTONIUM")) ambientRad += 0.15f * stack.getCount();
        }
        for (minicraft.item.ItemStack stack : inventory.getMainInventory()) {
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getItem().getName();
            if (name.contains("URANIUM")) ambientRad += 0.05f * stack.getCount();
            if (name.contains("PLUTONIUM")) ambientRad += 0.15f * stack.getCount();
        }

        // Shielding logic
        boolean isProtected = inventory.hasFullSet("Plutonium") || inventory.hasFullSet("Mithril");
        if (isProtected) ambientRad *= 0.05f; // 95% reduction

        // Smoothly adjust player radiation level
        if (ambientRad > radiationLevel) {
            radiationLevel += dt * 0.5f;
        } else {
            radiationLevel = Math.max(0, radiationLevel - dt * 0.1f);
        }

        // Apply damage
        if (radiationLevel > 1.0f) {
            radiationDamageTimer += dt;
            if (radiationDamageTimer >= 1.0f) {
                radiationDamageTimer = 0f;
                damage(radiationLevel * 2f, null);
                System.out.println("RADIATION CRITICAL: " + (int)radiationLevel + "%");
            }

            // --- Geiger Counter Clicks ---
            geigerClickTimer += dt;
            float clickInterval = Math.max(0.05f, 1.5f - (radiationLevel / 50f)); // Clicks faster as level rises
            if (geigerClickTimer >= clickInterval) {
                geigerClickTimer = 0f;
                // Since we have no audio engine yet, we print to console as a tactile feedback
                System.out.println("[GEIGER] CLICK");
            }
        }
    }
}
