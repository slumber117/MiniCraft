package minicraft;

import minicraft.entity.Entity;
import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.Player;
import minicraft.entity.ItemEntity;
import minicraft.entity.EntityState;
import minicraft.item.Item;
import minicraft.item.ToolItem;
import minicraft.item.CraftingManager;
import minicraft.item.Recipe;
import minicraft.renderer.Camera;
import minicraft.renderer.EntityRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.renderer.Mesh;
import minicraft.renderer.ModelRegistry;
import minicraft.renderer.TextureRegistry;
import minicraft.math.Vector3f;
import minicraft.math.Vector4f;
import minicraft.math.Matrix4f;
import minicraft.world.World;
import minicraft.world.Block;
import minicraft.world.WorldGenerator;
import minicraft.world.WorldCell;
import minicraft.world.Biome;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import minicraft.renderer.TextureRegistry;
import minicraft.renderer.UIRenderer;
import minicraft.world.World;
import minicraft.world.Block;
import minicraft.utils.Utils;
import minicraft.math.Matrix4f;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    // ---- Window ----
    private long window;
    private static final int   WIN_W  = 1280;
    private static final int   WIN_H  = 720;
    private static final String TITLE = "MiniCraft Engine";

    // ---- Rendering ----
    private ShaderProgram  shaderProgram;
    private TextureRegistry textures;
    private UIRenderer uiRenderer;

    // ---- World ----
    private World  world;
    private EntityManager entityManager;
    private EntityRenderer entityRenderer;
    private minicraft.renderer.WeatherRenderer weatherRenderer;
    private static final long  SEED            = 12345L;
    private static final int   RENDER_DISTANCE = 4;   // chunks in each direction

    // Performance Optimization: Reusable Objects to avoid GC pressure in loop
    private int framebufferW = WIN_W;
    private int framebufferH = WIN_H;
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix       = new Matrix4f();
    private final Matrix4f modelMatrix      = new Matrix4f();
    private final Vector4f whiteTint        = new Vector4f(1f, 1f, 1f, 1f);

    // ---- Player / input ----
    private Player player;
    private Camera camera;
    private static final float FOV              = (float) Math.toRadians(70.0);
    private static final float Z_NEAR           = 0.05f;
    private static final float Z_FAR            = 2000.0f;
    private static final float CAMERA_SPEED     = 0.15f;
    private static final float SPRINT_MULT      = 3.0f;
    private static final float MOUSE_SENSITIVITY= 0.15f;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private boolean prevMouseLeftDown = false;
    private boolean prevMouseRightDown = false;

    // Atmospheric State
    private float worldTime = 8000f; // Start in morning
    private final float DAY_LENGTH = 24000f; 
    // Mining State
    private float miningProgress = 0f;
    private int miningX = -1, miningY = -1, miningZ = -1;

    // Weather State is now handled by world.getWeather()

    // Crafting / Inventory State
    public boolean craftingOpen = false;
    public boolean inventoryOpen = false;
    public boolean chestOpen = false;
    public minicraft.entity.Inventory activeChest = null;
    
    public Recipe.Category activeCategory = Recipe.Category.TOOLS;
    public int recipeIndex = 0;
    public int inventoryIndex = 0;
    public int chestIndex = 0;
    public final CraftingManager craftingManager = new CraftingManager();
    
    private boolean prevC = false, prevE = false;
    private boolean prevEnter = false, prevUp = false, prevDown = false;

    // -----------------------------------------------------------------------

    public minicraft.entity.ParticleManager particleManager;
    
    public Main() {
    }

    public void run() {
        try {
            System.out.println("MiniCraft | LWJGL " + Version.getVersion());
            init();
            setup();
            loop();
            cleanup();
        } catch (Exception e) {
            System.err.println("CRITICAL ENGINE ERROR: See crash_report.txt for details.");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("crash_report.txt"))) {
                e.printStackTrace(pw);
                pw.println("\n--- Engine State ---");
                pw.println("World ready: " + (world != null));
                pw.println("Player health: " + (player != null ? player.getHealth() : "N/A"));
            } catch (java.io.IOException io) {
                e.printStackTrace();
            }
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public long getWindow() { return window; }
    public World getWorld() { return world; }

    // -----------------------------------------------------------------------
    //  Init
    // -----------------------------------------------------------------------

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Cannot init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,                GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE,              GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,  3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,  3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,         GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_SAMPLES,                4); // MSAA

        window = glfwCreateWindow(WIN_W, WIN_H, TITLE, NULL, NULL);
        if (window == NULL) throw new RuntimeException("Cannot create GLFW window");

        // Key callback: ESC to exit
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_C || key == GLFW_KEY_V) {
                    craftingOpen = !craftingOpen;
                    if (craftingOpen) {
                        inventoryOpen = false;
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    } else {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                        firstMouse = true;
                    }
                }
                if (key == GLFW_KEY_E || key == GLFW_KEY_I) {
                    inventoryOpen = !inventoryOpen;
                    if (inventoryOpen) {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                        craftingOpen = false;
                    } else {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                        firstMouse = true;
                    }
                }
                
                // Close everything if ESC is pressed
                if (key == GLFW_KEY_ESCAPE) {
                    craftingOpen = false;
                    inventoryOpen = false;
                    chestOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    firstMouse = true;
                }
            }
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                if (inventoryOpen) {
                    handleInventoryClick();
                } else if (chestOpen) {
                    handleChestClick();
                }
            }
        });

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (!inventoryOpen && !craftingOpen) {
                if (firstMouse) {
                    lastMouseX = xpos;
                    lastMouseY = ypos;
                    firstMouse = false;
                }
                double dx = xpos - lastMouseX;
                double dy = lastMouseY - ypos;
                lastMouseX = xpos;
                lastMouseY = ypos;
                player.handleMouseInput((float) dx * MOUSE_SENSITIVITY, (float) dy * MOUSE_SENSITIVITY);
            }
        });

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            glViewport(0, 0, w, h);
            this.framebufferW = w;
            this.framebufferH = h;
        });

        // Centre window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
            glfwGetWindowSize(window, pw, ph);
            GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
            assert vm != null;
            glfwSetWindowPos(window, (vm.width()  - pw.get(0)) / 2,
                                     (vm.height() - ph.get(0)) / 2);
            this.framebufferW = pw.get(0);
            this.framebufferH = ph.get(0);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        // Capture cursor for mouse-look
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Scroll callback for Inventory
        glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            player.inventory.changeSelection((int) -yoff);
        });
    }

    // -----------------------------------------------------------------------
    //  Setup – called after GL context is current
    // -----------------------------------------------------------------------

    private void setup() throws Exception {
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Sky colour
        glClearColor(0.45f, 0.68f, 0.95f, 1.0f);

        // Textures
        textures = new TextureRegistry("/textures/");
        particleManager = new minicraft.entity.ParticleManager();

        // World
        world = new World(SEED, textures, RENDER_DISTANCE);
        System.out.println("Generating initial chunks...");
        world.update(0, 0, 0f);
        System.out.println("World ready.");

        // Camera
        camera = new Camera();
        
        // 5. Spawn Player Safely on Grass (ensure surrounding chunks exist)
        for (int x = -1; x <= 1; x++) 
            for (int z = -1; z <= 1; z++) 
                world.getOrGenerate(x, z); 

        minicraft.math.Vector3f spawnPos = world.findSafeGrassSpawn(8, 8);
        System.out.println("Spawn System: Transmat to location: " + spawnPos);
        
        // Player (feet)
        player = new Player(camera);
        player.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Give player a Torch and a Sword to test the new combat
        Item torch = new Item("TORCH", Block.TORCH);
        player.inventory.add(torch, 10);
        player.inventory.setOffhandItem(torch); 
        
        ToolItem sword = new ToolItem("Bronze Sword", ToolItem.ToolType.SWORD, 2, 5.0f, "item_sword_bronze"); // Fast swing
        player.inventory.add(sword, 1);

        ToolItem pickaxe = new ToolItem("Wooden Pickaxe", ToolItem.ToolType.PICKAXE, 0, 2.0f, "item_pick_wood");
        player.inventory.add(pickaxe, 1);
        
        // Sync Camera to Eye-Level
        camera.setPosition(spawnPos.x, spawnPos.y + 1.6f, spawnPos.z);

        // Register Player in Entity Manager so monsters can find them
        entityManager = new EntityManager();
        entityManager.spawn(player);

        // Shaders
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(Utils.loadResource("/shaders/vertex.glsl"));
        shaderProgram.createFragmentShader(Utils.loadResource("/shaders/fragment.glsl"));
        shaderProgram.link();
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("viewMatrix");
        shaderProgram.createUniform("modelMatrix");
        shaderProgram.createUniform("texture_sampler");
        shaderProgram.createUniform("colorTint");
        shaderProgram.createUniform("torchPos");
        shaderProgram.createUniform("torchStrength");
        shaderProgram.createUniform("useLighting");
        shaderProgram.createUniform("sunBrightness");
        shaderProgram.createUniform("weatherIntensity");
        shaderProgram.createUniform("weatherType");

        // UI
        uiRenderer = new UIRenderer(textures);

        entityRenderer = new EntityRenderer(textures);
        weatherRenderer = new minicraft.renderer.WeatherRenderer();
        world = new World(SEED, textures, RENDER_DISTANCE);

        // 3D Model System
        ModelRegistry.init(textures);
    }

    private float handAnimTime = 0.0f;
    private float handSwingTime = 0.0f;
    private boolean isHandSwinging = false;

    private void spawnInitialEntities() {
        // Passive animals – scatter around spawn area
        for (int i = 0; i < 6; i++)  entityManager.spawnAt(EntityType.COW,    8 + (float)(Math.random()*30-15), 70, 8 + (float)(Math.random()*30-15));
        for (int i = 0; i < 6; i++)  entityManager.spawnAt(EntityType.SHEEP,  8 + (float)(Math.random()*30-15), 70, 8 + (float)(Math.random()*30-15));
        for (int i = 0; i < 3; i++)  entityManager.spawnAt(EntityType.RAM,    8 + (float)(Math.random()*50-25), 80, 8 + (float)(Math.random()*50-25));
        for (int i = 0; i < 4; i++)  entityManager.spawnAt(EntityType.DOG,    8 + (float)(Math.random()*20-10), 70, 8 + (float)(Math.random()*20-10));
        for (int i = 0; i < 3; i++)  entityManager.spawnAt(EntityType.CAT,    8 + (float)(Math.random()*20-10), 70, 8 + (float)(Math.random()*20-10));
        for (int i = 0; i < 2; i++)  entityManager.spawnAt(EntityType.WHALE,  8 + (float)(Math.random()*60-30), 62, 8 + (float)(Math.random()*60-30));
        // Predators – spawn further out
        for (int i = 0; i < 3; i++)  entityManager.spawnAt(EntityType.BEAR,   8 + (float)(Math.random()*80-40), 70, 8 + (float)(Math.random()*80-40));
        for (int i = 0; i < 4; i++)  entityManager.spawnAt(EntityType.WOLF,   8 + (float)(Math.random()*80-40), 70, 8 + (float)(Math.random()*80-40));
        for (int i = 0; i < 2; i++)  entityManager.spawnAt(EntityType.TIGER,  8 + (float)(Math.random()*100-50), 70, 8 + (float)(Math.random()*100-50));
        for (int i = 0; i < 2; i++)  entityManager.spawnAt(EntityType.LION,   8 + (float)(Math.random()*100-50), 70, 8 + (float)(Math.random()*100-50));
        for (int i = 0; i < 3; i++)  entityManager.spawnAt(EntityType.EAGLE,  8 + (float)(Math.random()*60-30), 90, 8 + (float)(Math.random()*60-30));
        // NPCs
        for (int i = 0; i < 3; i++)  entityManager.spawnAt(EntityType.FARMER, 8 + (float)(Math.random()*16-8), 70, 8 + (float)(Math.random()*16-8));
        System.out.println("Spawned " + entityManager.count() + " entities.");
    }

    // -----------------------------------------------------------------------
    //  Game Loop
    // -----------------------------------------------------------------------

    private void loop() {
        long lastTime = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            // --- UI Interaction Logic ---
            if (inventoryOpen || craftingOpen) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Get window size
            int currentW, currentH;
            try (MemoryStack stack = stackPush()) {
                IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
                glfwGetWindowSize(window, pw, ph);
                currentW = pw.get(0);
                currentH = ph.get(0);
            }

            // 5. Update Input, Physics & Weather
            updateInput((float) dt);
            entityManager.update((float) dt, world);
            world.update((float) dt, player, particleManager);
            particleManager.update((float) dt);

            // Sync Camera after physics
            if (player.isRiding()) {
                minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
                float ryaw = (float) Math.toRadians(ship.yaw);
                float dist = 40.0f; // Chase distance for 80-block ship
                
                float cx = ship.position.x + (float)Math.sin(ryaw) * dist;
                float cy = ship.position.y + 12.0f;
                float cz = ship.position.z + (float)Math.cos(ryaw) * dist;
                
                camera.setPosition(cx, cy, cz);
                camera.setRotation(15.0f, ship.yaw, 0); // Cinematic elevated view
            } else {
                camera.setPosition(player.position.x, player.position.y + 1.6f, player.position.z);
            }

            // Update visible chunks based on camera position
            int cx = (int) Math.floor(player.position.x / 16.0);
            int cz = (int) Math.floor(player.position.z / 16.0);
            world.update(cx, cz, (float) dt);

            shaderProgram.bind();

            // ── Render 3D World ──
            float aspectRatio = (float) framebufferW / Math.max(1, framebufferH);
            projectionMatrix.perspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
            
            // Avoid creating new Matrix objects – use .get(camera.getViewMatrix()) equivalent or pass camera
            viewMatrix.set(camera.getViewMatrix());
            modelMatrix.identity();

            shaderProgram.setUniform("projectionMatrix", projectionMatrix);
            shaderProgram.setUniform("viewMatrix",       viewMatrix);
            shaderProgram.setUniform("modelMatrix",      modelMatrix);
            shaderProgram.setUniform("texture_sampler",  0);
            
            // Render World (normal color tint)
            shaderProgram.setUniform("colorTint", whiteTint);
            // 4. Update Environmental Atmosphere (Day/Night)
            updateAtmosphere((float) dt);
            
            // Pass Light Info
            shaderProgram.setUniform("torchPos", player.position); // Logic: Player carries the light source
            float torchPower = (player.inventory.hasTorchEquipped()) ? 1.0f : 0.0f;
            shaderProgram.setUniform("torchStrength", torchPower);

            // 3. Render World
            boolean isUnderwater = world.getBlock((int)Math.floor(player.position.x), (int)Math.floor(player.position.y + 1.6f), (int)Math.floor(player.position.z)) == Block.WATER;
            if (isUnderwater) {
                shaderProgram.setUniform("colorTint", new Vector4f(0.4f, 0.6f, 1.0f, 1.0f));
            } else {
                shaderProgram.setUniform("colorTint", whiteTint);
            }
            
            world.render(shaderProgram, player.position);
            
            // Render Entities
            entityRenderer.render(entityManager, shaderProgram, textures, viewMatrix);
            particleManager.render(shaderProgram, viewMatrix, projectionMatrix);
            
            glEnable(GL_BLEND);
            
            // Render Weather Particles
            weatherRenderer.render(player.position, world.getWeather(), shaderProgram, (float) dt);
            // --- RENDER 3D HAND / PICKAXE ---
            glDisable(GL_DEPTH_TEST); // Render over world
            renderHand(shaderProgram, (float) dt);
            glEnable(GL_DEPTH_TEST);
            
            glEnable(GL_BLEND);
            if (!craftingOpen) {
                minicraft.entity.Entity item = entityManager.getNearestOfType(
                    camera.getPosition().x, camera.getPosition().y, camera.getPosition().z, 
                    2.0f, minicraft.entity.EntityType.ITEM
                );
                if (item instanceof minicraft.entity.ItemEntity) {
                    player.inventory.add(((minicraft.entity.ItemEntity) item).block, 1);
                    item.damage(100f);
                }
            }
            uiRenderer.render(player, shaderProgram, currentW, currentH, this);

            shaderProgram.unbind();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    // -----------------------------------------------------------------------
    //  Input
    // -----------------------------------------------------------------------

    private void updateInput(float dt) {
        handleMovementInput(dt);
        handleInteractionInput(dt);
    }

    private void handleMovementInput(float dt) {
        if (craftingOpen) {
            player.velocity.x = 0;
            player.velocity.z = 0;
            return; // No walking while crafting
        }
        
        float speed = 8.0f; // Walk speed

        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = 0, dz = 0;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            dx += (float)Math.sin(yaw) * speed;
            dz -= (float)Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            dx -= (float)Math.sin(yaw) * speed;
            dz += (float)Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            dx -= (float)Math.cos(yaw) * speed;
            dz -= (float)Math.sin(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            dx += (float)Math.cos(yaw) * speed;
            dz += (float)Math.sin(yaw) * speed;
        }

        // Apply to player velocity (horizontal)
        if (player.isRiding()) {
            boolean w = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
            boolean s = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
            boolean a = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
            boolean d = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
            boolean space = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
            boolean shift = (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS);
            
            player.getRidingShip().handleInput(w, s, a, d, space, shift, dt);
            player.velocity.set(0, 0, 0); // No independent walking
        } else {
            player.velocity.x = dx;
            player.velocity.z = dz;
        }

        // Jump
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && player.isGrounded) {
            player.velocity.y = 8.2f; // Calibrated for ~1.2m jump height
            player.isGrounded = false;
        }

        // Mouse look
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        if (firstMouse) { lastMouseX = mx[0]; lastMouseY = my[0]; firstMouse = false; }
        double mdx = mx[0] - lastMouseX;
        double mdy = my[0] - lastMouseY;
        lastMouseX = mx[0]; lastMouseY = my[0];
        camera.moveRotation((float)(mdy * MOUSE_SENSITIVITY), (float)(mdx * MOUSE_SENSITIVITY), 0);

        // Clamp pitch
        if (camera.getRotation().x >  89f) camera.setRotation( 89f, camera.getRotation().y, 0);
        if (camera.getRotation().x < -89f) camera.setRotation(-89f, camera.getRotation().y, 0);
    }

    private void handleInventoryInput() {
        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        // Simplified in V3 - handoff to handleInventoryClick via mouse callback
        prevMouseLeftDown = mouseLeftDown;
    }

    private void handleInteractionInput(float dt) {
        // Toggle Inventory (Key E)
        boolean currentE = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        if (currentE && !prevE) {
            inventoryOpen = !inventoryOpen;
            if (inventoryOpen) {
                craftingOpen = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }
        prevE = currentE;

        if (inventoryOpen) {
            handleInventoryInput();
            return;
        }

        // Toggle Crafting (Key C)
        boolean isC = glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS;
        if (isC && !prevC) {
            craftingOpen = !craftingOpen;
            if (craftingOpen) {
                inventoryOpen = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }
        prevC = isC;

        if (craftingOpen) {
            handleCraftingInput();
            return;
        }

        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean mouseRightDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        if (mouseLeftDown) {
            handlePlayerAttack((float) dt);
        } else {
            miningProgress = 0f;
            miningX = -1;
        }

        if (mouseRightDown && !prevMouseRightDown) {
            handlePlayerPlace();
        }

        prevMouseLeftDown = mouseLeftDown;
        prevMouseRightDown = mouseRightDown;
    }

    private void updateAtmosphere(float dt) {
        worldTime += dt * 100f; // 100 ticks per second
        if (worldTime >= DAY_LENGTH) worldTime = 0;

        float timeP = worldTime / DAY_LENGTH;
        float rain = world.getWeather().getRainIntensity();
        
        // Linear interpolate sky color based on time and rain
        float r, g, b;
        if (timeP < 0.25f) { // Sunrise
            r = 0.45f + timeP * 4 * 0.4f;
            g = 0.68f; b = 0.95f;
        } else if (timeP < 0.5f) { // Day
            r = 0.45f; g = 0.68f; b = 0.95f;
        } else if (timeP < 0.75f) { // Sunset
            float p = (timeP - 0.5f) * 4;
            r = 0.45f + p * 0.3f;
            g = 0.68f - p * 0.4f;
            b = 0.95f - p * 0.6f;
        } else { // Night
            r = 0.1f; g = 0.1f; b = 0.2f;
        }
        
        // Incorporate Biome Fog Color
        WorldGenerator gen = world.getGenerator();
        WorldCell cell = gen.generate(player.position.x, player.position.z);
        minicraft.math.Vector3f fog = world.getFogColor(cell.biome);
        
        r = (r + fog.x) * 0.5f;
        g = (g + fog.y) * 0.5f;
        b = (b + fog.z) * 0.5f;

        glClearColor(r, g, b, 1.0f);
    }

    private void handlePlayerAttack(float dt) {
        // 1. Melee attack check (entities)
        if (!prevMouseLeftDown) {
            player.meleeAttack(entityManager);
        }

        // 2. Block interaction trace
        Vector3f pos = camera.getPosition();
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw   = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) -Math.sin(pitch);
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
        
        float reach = 6.0f;

        for (float dist = 0.5f; dist <= reach; dist += 0.1f) {
            float px = pos.x + dx * dist;
            float py = pos.y + dy * dist;
            float pz = pos.z + dz * dist;
            
            int gx = (int) Math.floor(px);
            int gy = (int) Math.floor(py);
            int gz = (int) Math.floor(pz);

            Block b = world.getBlock(gx, gy, gz);
            if (b.solid) {
                if (b.hardness < 0) return; // Unbreakable (Bedrock)

                // Track if we are still mining the same block
                if (gx != miningX || gy != miningY || gz != miningZ) {
                    miningX = gx; miningY = gy; miningZ = gz;
                    miningProgress = 0f;
                }

                Item heldItem = player.inventory.getSelectedItem();
                float efficiency = 1.0f;
                int toolLevel = 0;

                if (heldItem instanceof ToolItem) {
                    ToolItem tool = (ToolItem) heldItem;
                    if (tool.getToolType() == ToolItem.ToolType.PICKAXE) {
                        efficiency = tool.getEfficiency();
                        toolLevel = tool.getHarvestLevel();
                    }
                } else {
                    efficiency = 0.5f; // Hand is slow
                }

                miningProgress += (dt * efficiency) / b.hardness;
                
                // Tier Check: cannot mine if wood/stone/etc is too low tier
                if (toolLevel < b.requiredHarvestLevel) {
                    miningProgress = 0f;
                }
                
                player.miningProgress = miningProgress; // For UI

                if (miningProgress >= 1.0f) {
                    world.setBlock(gx, gy, gz, Block.AIR);
                    miningProgress = 0f;
                    player.miningProgress = 0f;

                    // Drop item only if harvest level is met
                    if (toolLevel >= b.requiredHarvestLevel) {
                        ItemEntity item = new ItemEntity(b);
                        item.setPosition(gx + 0.5f, gy + 0.5f, gz + 0.5f);
                        item.velocity.set(0, 2f, 0);
                        entityManager.spawn(item);
                    } else {
                        System.out.println("Tool too weak for " + b.name());
                    }
                }
                return;
            }
        }
        miningProgress = 0f;
        player.miningProgress = 0f;
    }

    private void handlePlayerPlace() {
        Vector3f pos = camera.getPosition();
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw   = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) -Math.sin(pitch);
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
        
        float reach = 6.0f;
        
        Block selected = player.inventory.getSelectedBlock();
        if (selected == null) return;

        // Trace to find the FIRST solid block
        for (float dist = 0.5f; dist <= reach; dist += 0.1f) {
            float px = pos.x + dx * dist;
            float py = pos.y + dy * dist;
            float pz = pos.z + dz * dist;
            
            int gx = (int) Math.floor(px);
            int gy = (int) Math.floor(py);
            int gz = (int) Math.floor(pz);

            // 1. INTERACTION CHECK
            Block targeted = world.getBlock(gx, gy, gz);
            if (targeted == Block.CHEST || targeted == Block.CRAFTING_TABLE || targeted == Block.FURNACE || targeted == Block.SHIP_CONSOLE) {
                if (targeted == Block.CHEST) {
                    activeChest = world.getContainer(gx, gy, gz);
                    chestOpen = true;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                } else if (targeted == Block.CRAFTING_TABLE) {
                    craftingOpen = true;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                } else if (targeted == Block.SHIP_CONSOLE) {
                    // LAUNCH SEQUENCE
                    world.setBlock(gx, gy, gz, Block.AIR);
                    // Spawn ship entity at Y=190, offset from the factory to avoid collision
                    minicraft.entity.ship.ShipEntity ship = (minicraft.entity.ship.ShipEntity) entityManager.spawnAt(EntityType.STALWART_SHIP, gx + 20, 190, gz);
                    player.setRiding(ship);
                    ship.setPassenger(player);
                    System.out.println("LAUNCH COMPLETE: UNSC ENCOURAGEMENT DEPLOYED");
                }
                return; // Interaction handled
            }

            // 2. PLACEMENT LOGIC
            if (targeted.solid) {
                // We hit a block! Place ADJACENT to it (using the previous step)
                float prevPx = pos.x + dx * (dist - 0.1f);
                float prevPy = pos.y + dy * (dist - 0.1f);
                float prevPz = pos.z + dz * (dist - 0.1f);
                
                int pgx = (int) Math.floor(prevPx);
                int pgy = (int) Math.floor(prevPy);
                int pgz = (int) Math.floor(prevPz);
                
                // Collision check (don't place inside player)
                if (pgx == (int)Math.floor(pos.x) && pgz == (int)Math.floor(pos.z) && 
                   (pgy == (int)Math.floor(pos.y) || pgy == (int)Math.floor(pos.y+1))) {
                    return; 
                }

                if (selected != null && !world.getBlock(pgx, pgy, pgz).solid) {
                    world.setBlock(pgx, pgy, pgz, selected);
                    player.inventory.remove(selected, 1);
                    return;
                }
            }
        }
    }

    private void handleChestClick() {
        if (activeChest == null) return;
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        
        int[] winW = new int[1], winH = new int[1];
        glfwGetWindowSize(window, winW, winH);
        float scaleX = (float) framebufferW / Math.max(1, winW[0]);
        float scaleY = (float) framebufferH / Math.max(winH[0], 1);
        
        float x = (float) mx[0] * scaleX;
        float y = (float) my[0] * scaleY;

        float panelW = 800;
        float sx = (framebufferW - panelW) / 2f;
        float sy = (framebufferH - 550) / 2f;
        float slotSize = 64;
        float gap = 15;

        // Chest Interior Detection (Top 3x9)
        float chestStartX = sx + (panelW - (slotSize + gap) * 9) / 2f;
        float chestStartY = sy + 80;
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float slotX = chestStartX + col * (slotSize + gap);
            float slotY = chestStartY + row * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                activeChest.clickSlot(i, false);
                return;
            }
        }

        // Player Feedback Detection (Bottom 3x9)
        float playerStartY = sy + 300;
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float slotX = chestStartX + col * (slotSize + gap);
            float slotY = playerStartY + row * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                player.inventory.clickSlot(i, false);
                return;
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Cleanup
    // -----------------------------------------------------------------------

    private void cleanup() {
        if (shaderProgram != null) shaderProgram.cleanup();
        if (world         != null) world.cleanup();
        if (textures      != null) textures.cleanup();
        if (entityRenderer!= null) entityRenderer.cleanup();
        if (uiRenderer    != null) uiRenderer.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // Increase the LWJGL internal MemoryStack size (2MB) as a safety measure for complex voxel meshes
        System.setProperty("org.lwjgl.system.stackSize", "2048");
        new Main().run();
    }

    private void handleCraftingInput() {
        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : craftingManager.getRecipes()) {
            if (r.getCategory() == activeCategory) filtered.add(r);
        }

        if (mouseLeftDown && !prevMouseLeftDown) {
            double[] mx = new double[1], my = new double[1];
            glfwGetCursorPos(window, mx, my);
            float x = (float)mx[0];
            float y = (float)my[0];

            float menuW = 600;
            float menuH = 500;
            float startX = (WIN_W - menuW) / 2f;
            float startY = (WIN_H - menuH) / 2f;

            // 1. Check Tabs
            Recipe.Category[] cats = Recipe.Category.values();
            float tabW = menuW / cats.length;
            if (y >= startY && y <= startY + 40) {
                for (int i = 0; i < cats.length; i++) {
                    if (x >= startX + i * tabW && x <= startX + (i+1) * tabW) {
                        activeCategory = cats[i];
                        recipeIndex = 0;
                        break;
                    }
                }
            }

            // 2. Check Recipe List
            for (int i = 0; i < filtered.size(); i++) {
                float ry = startY + 60 + i * 40;
                if (x >= startX + 20 && x <= startX + 380 && y >= ry && y <= ry + 35) {
                    recipeIndex = i;
                    break;
                }
            }

            // 3. Check Craft Button
            if (recipeIndex >= 0 && recipeIndex < filtered.size()) {
                float btnX = startX + menuW - 180;
                float btnY = startY + menuH - 70;
                if (x >= btnX && x <= btnX + 160 && y >= btnY && y <= btnY + 50) {
                    craftingManager.craft(filtered.get(recipeIndex), player.inventory);
                }
            }
        }

        // Keyboard navigation (Fallback)
        boolean isUp = glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS;
        boolean isDown = glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS;
        boolean isEnter = glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS;
        if (isUp && !prevUp) recipeIndex = (recipeIndex - 1 + filtered.size()) % Math.max(1, filtered.size());
        if (isDown && !prevDown) recipeIndex = (recipeIndex + 1) % Math.max(1, filtered.size());
        if (isEnter && !prevEnter && recipeIndex < filtered.size()) {
            craftingManager.craft(filtered.get(recipeIndex), player.inventory);
        }

        prevUp = isUp; prevDown = isDown; prevEnter = isEnter;
        prevMouseLeftDown = mouseLeftDown;
    }

    private void renderHealthBar(Entity e, ShaderProgram shader, Matrix4f viewMatrix) {
    }

    private void renderHand(ShaderProgram shader, float dt) {
        Item heldItem = player.inventory.getSelectedItem();
        if (heldItem == null) return;

        Mesh model = null;
        if (heldItem.getName().contains("Pickaxe")) {
            model = ModelRegistry.getModel("pickaxe_wooden");
        }

        if (model == null) return;

        // --- HAND ANIMATION LOGIC ---
        // 1. Walking Bob
        float walkSpeed = (float) Math.sqrt(player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z);
        if (player.isGrounded && walkSpeed > 0.1f) {
            handAnimTime += dt * walkSpeed * 0.5f;
        } else {
            handAnimTime = 0.0f;
        }
        
        float bobX = (float) Math.sin(handAnimTime * 2.0f) * 0.02f;
        float bobY = (float) Math.abs(Math.cos(handAnimTime * 2.0f)) * 0.02f;

        // 2. Mining Swing
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && !craftingOpen) {
            isHandSwinging = true;
        }

        if (isHandSwinging) {
            handSwingTime += dt * 8.0f; // Fast swing
            if (handSwingTime > Math.PI) {
                handSwingTime = 0.0f;
                isHandSwinging = false;
            }
        }
        float swingRot = (float) Math.sin(handSwingTime) * 45.0f;

        // --- RENDERING ---
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK); // Fix "see-through" by ensuring correct face culling
        glClear(GL_DEPTH_BUFFER_BIT); // Draw on top of world

        Matrix4f h_proj = new Matrix4f().perspective((float)Math.toRadians(70), (float)framebufferW/Math.max(1, framebufferH), 0.01f, 100.0f);
        Matrix4f h_view = new Matrix4f().identity(); 

        Matrix4f h_model = new Matrix4f().identity();
        h_model.translate(0.55f + bobX, -0.45f + bobY, -0.65f); // Position on screen
        
        // --- NEW ORIENTATION ---
        // 1. Point the head AWAY from torso (+180 on Y if it was facing us)
        h_model.rotate((float)Math.toRadians(160), new Vector3f(0, 1, 0)); 
        // 2. Natural grip tilt
        h_model.rotate((float)Math.toRadians(-25 + (isHandSwinging ? swingRot : 0)), new Vector3f(1, 0, 0)); 
        // 3. Side angle for better visibility
        h_model.rotate((float)Math.toRadians(20), new Vector3f(0, 0, 1));
        
        h_model.scale(0.4f, 0.4f, 0.4f); 

        shader.setUniform("projectionMatrix", h_proj);
        shader.setUniform("viewMatrix", h_view);
        shader.setUniform("modelMatrix", h_model);
        
        // --- SHADING UPGRADE ---
        shader.setUniform("useLighting", 1); 
        shader.setUniform("sunBrightness", 0.9f); // Bright "Studio" look for hand
        shader.setUniform("torchStrength", 0.5f); // Subtle glow on hand

        model.render();
        
        glEnable(GL_CULL_FACE);
    }

    private void handleInventoryClick() {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        
        // --- High-DPI Calibration ---
        int[] winW = new int[1], winH = new int[1];
        glfwGetWindowSize(window, winW, winH);
        float scaleX = (float) framebufferW / Math.max(1, winW[0]);
        float scaleY = (float) framebufferH / Math.max(1, winH[0]);
        
        float x = (float) mx[0] * scaleX;
        float y = (float) my[0] * scaleY;

        boolean isShift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;

        float panelW = 800;
        float panelH = 500;
        float sx = (framebufferW - panelW) / 2f;
        float sy = (framebufferH - panelH) / 2f;

        // 1. Main Inventory Detection (3x9 grid)
        float invX = sx + 240;
        float slotSize = 64;
        float gap = 15;
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float slotX = invX + 40 + col * (slotSize + gap);
            float slotY = sy + 100 + row * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                if (isShift) {
                    player.inventory.quickMove(i, false);
                } else {
                    player.inventory.clickSlot(i, false);
                }
                return;
            }
        }

        // 2. Hotbar Detection (1x9 grid at bottom of panel)
        float hbX = (framebufferW - ((slotSize + gap) * 9)) / 2f;
        float hbY = sy + panelH - 80;
        for (int i = 0; i < 9; i++) {
            float slotX = hbX + i * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= hbY && y <= hbY + slotSize) {
                if (isShift) {
                    player.inventory.quickMove(i, true);
                } else {
                    player.inventory.clickSlot(i, true);
                }
                return;
            }
        }
    }
}
