package minicraft.entity;

import minicraft.renderer.Camera;
import minicraft.item.ToolItem;
import minicraft.math.Vector3f;
import minicraft.world.World;
import minicraft.world.Block;

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
    
    public boolean isGrounded = false;
    private Camera camera;

    public Player(Camera camera) {
        super(EntityType.PLAYER, 100f);
        this.camera = camera;
        this.width = 0.6f;
        this.height = 1.8f;
        
        // Starting Tools
        inventory.add(new ToolItem("Wood Pick", ToolItem.ToolType.PICKAXE, 0, 2.0f, "item_pick_wood"), 1);
        inventory.add(new ToolItem("Stone Pick", ToolItem.ToolType.PICKAXE, 1, 4.0f, "item_pick_stone"), 1);
        
        // Give player a Torch and a Sword to test the new combat
        minicraft.item.Item torch = new minicraft.item.Item("TORCH", Block.TORCH);
        inventory.add(torch, 10);
        inventory.setOffhandItem(torch); 
        
        ToolItem sword = new ToolItem("Bronze Sword", ToolItem.ToolType.SWORD, 2, 5.0f, "item_sword_bronze"); // Fast swing
        inventory.add(sword, 1);
    }

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
        
        // 2. Physics & Collision (Moved from update)
        applyGravity(dt);
        resolveMovement(dt, world);

        // 3. Environment (Temp)
        updateEnvironment(dt);
        
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

    private void applyGravity(float dt) {
        if (!isGrounded) {
             velocity.y -= 25.0f * dt; 
        }
    }

    private void resolveMovement(float dt, World world) {
        // Try move X
        if (!isColliding(position.x + velocity.x * dt, position.y, position.z, world)) {
            position.x += velocity.x * dt;
        } else {
            velocity.x = 0;
        }
    
        // Try move Z
        if (!isColliding(position.x, position.y, position.z + velocity.z * dt, world)) {
            position.z += velocity.z * dt;
        } else {
            velocity.z = 0;
        }
    
        // Try move Y
        float nextY = position.y + velocity.y * dt;
        if (!isColliding(position.x, nextY, position.z, world)) {
            position.y = nextY;
            isGrounded = false;
        } else {
            if (velocity.y < 0) {
                position.y = (float) Math.floor(nextY + 0.001f) + 1.0f;
                isGrounded = true;
            }
            velocity.y = 0;
        }
        
        velocity.x *= 0.8f;
        velocity.z *= 0.8f;
    }

    public void meleeAttack(EntityManager manager) {
        float range = 3.0f;
        float cos45 = (float) Math.cos(Math.toRadians(45));

        for (Entity e : manager.getNearby(position.x, position.y, position.z, range)) {
            if (e == this) continue; 
            
            float dx = e.position.x - position.x;
            float dz = e.position.z - position.z;
            float dist = (float) Math.sqrt(dx*dx + dz*dz);
            if (dist == 0) continue;
            
            dx /= dist; dz /= dist;
            float fx = (float) -Math.sin(Math.toRadians(camera.getRotation().y));
            float fz = (float) -Math.cos(Math.toRadians(camera.getRotation().y));
            
            float dot = dx * fx + dz * fz;
            if (dot > cos45) { 
                e.damage(20f); 
                e.applyKnockback(dx * 4f, 1.5f, dz * 4f);
            }
        }
    }

    @Override
    public void damage(float amount) {
        if (invincibilityTimer > 0) return; 
        
        float defense = inventory.getTotalDefense();
        float reduction = 1.0f - defense;
        super.damage(amount * reduction);
        
        damageFlashTimer = 1.0f;
        invincibilityTimer = INVINCIBILITY_TIME;
        
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

    private void updateEnvironment(float dt) {
        float altitude = position.y;
        float baseTemp = 36.6f;
        
        // 1. Altitude Cold Factor (Starts at 120, gets lethal by 200)
        float altitudeColdFactor = Math.max(0, (altitude - 120) * 0.15f);
        
        // 2. Armor Insulation Check
        boolean isInsulated = inventory.getTotalDefense() > 0.05f; // Leather or better
        if (isInsulated) altitudeColdFactor *= 0.1f; // 90% protection

        if (altitude > 120) {
            temperature = baseTemp - altitudeColdFactor;
        } else if (altitude < 60) {
            temperature = baseTemp + (60 - altitude) * 0.1f;
        } else {
            temperature = baseTemp;
        }

        if (temperature < 32.0f) tempState = "Severe Hypothermia";
        else if (temperature < 35.0f) tempState = "Cold";
        else if (temperature > 38.0f) tempState = "Too Warm";
        else tempState = "Normal";
    }

    private void updateHealthEffects(float dt) {
        if (hunger <= 0 || thirst <= 0 || tempState.equals("Cold") || tempState.equals("Too Warm")) {
            health = Math.max(0, health - 0.5f * dt);
        } else if (tempState.equals("Severe Hypothermia")) {
            health = Math.max(0, health - 2.5f * dt); // Lethal damage
        } else {
            if (hunger > 80 && thirst > 80) {
                health = Math.min(maxHealth, health + 0.1f * dt);
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

    public void handleMouseInput(float dx, float dy) {
        camera.handleMouseInput(dx, dy);
    }
}
