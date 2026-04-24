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
import minicraft.renderer.TextureRegion;
import minicraft.renderer.GTBuffer;
import minicraft.math.Vector3f;
import minicraft.math.Vector4f;
import minicraft.math.Matrix4f;
import minicraft.world.World;
import minicraft.world.Block;
import minicraft.world.WorldGenerator;
import minicraft.world.WorldCell;
import minicraft.world.Biome;
import minicraft.core.GameLoop;
import minicraft.ship.ShipRegistry;
import minicraft.ship.ShipDefinition;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import minicraft.renderer.UIRenderer;
import minicraft.utils.Utils;

public class Main {

    // ── Window ────────────────────────────────────────────────────────────
    private long window;
    private static final int WIN_W = 1280;
    private static final int WIN_H = 720;
    private static final String TITLE = "MiniCraft Engine";

    // ── Rendering ─────────────────────────────────────────────────────────
    private ShaderProgram shaderProgram;
    private TextureRegistry textures;
    private UIRenderer uiRenderer;

    // ── RTGI Subsystem ───────────────────────────────────────────────────
    private GTBuffer gtBuffer;
    private ShaderProgram rtgiShader;
    private ShaderProgram compositeShader;
    private int giTexture;
    private Mesh screenQuad;
    private boolean rtgiEnabled = true;

    // ── World ─────────────────────────────────────────────────────────────
    private World world;
    private EntityManager entityManager;
    private EntityRenderer entityRenderer;
    private minicraft.renderer.WeatherRenderer weatherRenderer;
    private static final long SEED = 12345L;
    private static final int RENDER_DISTANCE = 4;

    // ── Core engine ───────────────────────────────────────────────────────
    private GameLoop gameLoop;

    /**
     * Thread-safe queue of one-shot actions to execute on the main thread
     * at the start of each frame, after the fixed tick but before rendering.
     *
     * GLFW callbacks post work here instead of doing it inline — this is
     * what prevents handleShipConsoleClick() from freezing the render thread.
     */
    private final ConcurrentLinkedQueue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();

    // ── Performance: reusable matrix/vector objects (avoids GC pressure) ──
    private int framebufferW, framebufferH;
    private int resizeCounter = 0;
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Vector4f whiteTint = new Vector4f(1f, 1f, 1f, 1f);

    // ── Player / input ────────────────────────────────────────────────────
    private Player player;
    private Camera camera;
    private static final float FOV = (float) Math.toRadians(70.0);
    private static final float Z_NEAR = 0.05f;
    private static final float Z_FAR = 2000.0f;
    private static final float MOUSE_SENSITIVITY = 0.15f;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private boolean prevMouseLeftDown = false;
    private boolean prevMouseRightDown = false;

    // ── Atmosphere ────────────────────────────────────────────────────────
    private float worldTime = 8000f;
    private final float DAY_LENGTH = 24000f;

    // ── Mining ────────────────────────────────────────────────────────────
    private float miningProgress = 0f;
    private int miningX = -1, miningY = -1, miningZ = -1;

    // ── UI state ──────────────────────────────────────────────────────────
    public boolean craftingOpen = false;
    public boolean inventoryOpen = false, chestOpen = false;
    public boolean shipConsoleOpen = false, furnaceOpen = false, cookerOpen = false;
    public boolean questLogOpen = false;

    // --- Loading State ---
    public volatile boolean isLoading = true;
    public volatile float loadingProgress = 0f;
    public volatile String loadingStatus = "Waking up AI Terrain Diffusion...";
    public long gameStartedTime = 0;
    public minicraft.entity.Inventory activeChest = null;
    public minicraft.entity.ProcessingFacility activeFacility = null;
    public String activeStatusMessage = "";
    public float statusMessageTimer = 0f;

    // ── Shipyard ──────────────────────────────────────────────────────────
    public int drydockX = 0, drydockY = 0, drydockZ = 0;

    // ── Crafting ──────────────────────────────────────────────────────────
    public Recipe.Category activeCategory = Recipe.Category.TOOLS;
    public int recipeIndex = 0;
    public int recipeScrollOffset = 0;
    public int inventoryScroll = 0;
    public int inventoryIndex = 0;
    public int chestIndex = 0;
    public final CraftingManager craftingManager = new CraftingManager();
    public Block focusedBlock = Block.AIR;
    public final minicraft.item.ProcessingManager processingManager = new minicraft.item.ProcessingManager();
    public final minicraft.quest.QuestManager questManager = new minicraft.quest.QuestManager();

    private boolean prevC = false, prevE = false;
    private boolean prevF = false, prevG = false, prevQ = false;
    private boolean prevEnter = false, prevUp = false, prevDown = false;
    private boolean prevLeft = false, prevRight = false;

    // ── Particles ─────────────────────────────────────────────────────────
    public minicraft.entity.ParticleManager particleManager;

    // ── Hand animation ────────────────────────────────────────────────────
    private float handAnimTime = 0.0f;
    private float handSwingTime = 0.0f;
    private boolean isHandSwinging = false;

    // ─────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("org.lwjgl.system.stackSize", "2048");
        new Main().run();
    }

    public void run() {
        try {
            initLWJGL();
            setup();
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public long getWindow() {
        return window;
    }

    public World getWorld() {
        return world;
    }

    public int getFramebufferW() { return framebufferW; }
    public int getFramebufferH() { return framebufferH; }

    // ─────────────────────────────────────────────────────────────────────
    // init() — GLFW window + callbacks
    // ─────────────────────────────────────────────────────────────────────

    private void initLWJGL() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Cannot init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(WIN_W, WIN_H, TITLE, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Cannot create GLFW window");

        // ── Key callback ──────────────────────────────────────────────────
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_C || key == GLFW_KEY_V) {
                    craftingOpen = !craftingOpen;
                    if (craftingOpen) {
                        inventoryOpen = false;
                        glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    } else {
                        glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                        firstMouse = true;
                    }
                }
                if (key == GLFW_KEY_E || key == GLFW_KEY_I) {
                    inventoryOpen = !inventoryOpen;
                    if (inventoryOpen) {
                        craftingOpen = false;
                        glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    } else {
                        glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                        firstMouse = true;
                    }
                }
                if (key == GLFW_KEY_ESCAPE) {
                    craftingOpen = false;
                    inventoryOpen = false;
                    chestOpen = false;
                    furnaceOpen = false;
                    cookerOpen = false;
                    shipConsoleOpen = false;
                    glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    firstMouse = true;
                }
                if (key == GLFW_KEY_G) {
                    rtgiEnabled = !rtgiEnabled;
                    System.out.println("RTGI: " + (rtgiEnabled ? "Enabled" : "Disabled"));
                }
            }
        });

        // ── Mouse button callback ─────────────────────────────────────────
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                // Prioritize facility interactions over general inventory clicks
                if (furnaceOpen || cookerOpen)
                    handleFacilityClick();
                else if (chestOpen)
                    handleChestClick();
                else if (inventoryOpen)
                    handleInventoryClick();
                else if (shipConsoleOpen)
                    handleShipConsoleClick();
            }
        });

        // ── Cursor callback ───────────────────────────────────────────────
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (!inventoryOpen && !craftingOpen && !shipConsoleOpen && !chestOpen && !furnaceOpen && !cookerOpen) {
                if (firstMouse) {
                    lastMouseX = xpos;
                    lastMouseY = ypos;
                    firstMouse = false;
                }
                double dx = xpos - lastMouseX;
                double dy = lastMouseY - ypos;
                lastMouseX = xpos;
                lastMouseY = ypos;
                player.handleMouseInput((float) dx * MOUSE_SENSITIVITY,
                        (float) dy * MOUSE_SENSITIVITY);
            }
        });

        // ── Unified Scroll Callback ───────────────────────────────────────
        glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            if (inventoryOpen) {
                // Scroll the inventory grid (30px per notch)
                inventoryScroll -= (int) (yoff * 30);
                // Clamp: 9 rows total, 3 rows visible. Each row is ~66px.
                // Max scroll is (9-3) * 66 = 396
                inventoryScroll = Math.max(0, Math.min(inventoryScroll, 396));
            } else if (craftingOpen) {
                final int VISIBLE = 11;
                List<Recipe> filtered = new ArrayList<>();
                for (Recipe r : craftingManager.getRecipes())
                    if (r.getCategory() == activeCategory)
                        filtered.add(r);
                if (filtered.isEmpty())
                    return;

                if (yoff > 0)
                    recipeScrollOffset--;
                else if (yoff < 0)
                    recipeScrollOffset++;

                int maxOff = Math.max(0, filtered.size() - VISIBLE);
                recipeScrollOffset = (int) Math.max(0, Math.min(recipeScrollOffset, maxOff));
            } else {
                // Default: Change hotbar selection
                player.inventory.changeSelection((int) -yoff);
            }
        });

        // ── Framebuffer resize ────────────────────────────────────────────
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            glViewport(0, 0, w, h);
            framebufferW = w;
            framebufferH = h;
        });

        // ── Centre window ─────────────────────────────────────────────────
        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
            glfwGetWindowSize(window, pw, ph);
            GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
            assert vm != null;
            glfwSetWindowPos(window,
                    (vm.width() - pw.get(0)) / 2,
                    (vm.height() - ph.get(0)) / 2);
            framebufferW = pw.get(0);
            framebufferH = ph.get(0);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

    }

    // ─────────────────────────────────────────────────────────────────────
    // setup() — OpenGL + world + player
    // ─────────────────────────────────────────────────────────────────────

    private void setup() throws Exception {
        GL.createCapabilities();
        gameLoop = new GameLoop(20);
        entityManager = new EntityManager();
        ShipRegistry.initialize();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0.45f, 0.68f, 0.95f, 1.0f);

        textures = new TextureRegistry("/textures/");
        particleManager = new minicraft.entity.ParticleManager();

        // ── World — created ONCE ──────────────────────────────────────────
        world = new World(SEED, textures, RENDER_DISTANCE);
        camera = new Camera();
        player = new Player(camera);

        // Start background generation
        new Thread(() -> {
            try {
                loadingStatus = "Analyzing neural landscape...";
                loadingProgress = 0.1f;
                
                // Pre-warm the ML assets
                com.github.xandergos.terraindiffusionmc.pipeline.ModelAssetManager.ensureAssetsReady();
                loadingProgress = 0.3f;
                loadingStatus = "Synthesizing natural landforms...";

                // Generate initial spawn region
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        world.getOrGenerate(x, z);
                        loadingProgress = 0.3f + 0.2f * ((x + 1) * 3 + (z + 1)) / 9f;
                    }
                }

                loadingStatus = "Finding optimal landing site...";
                loadingProgress = 0.7f;
                minicraft.math.Vector3f spawnPos = world.findSafeGrassSpawn(8, 8);
                
                int scx = Math.floorDiv((int)spawnPos.x, minicraft.world.Chunk.WIDTH);
                int scz = Math.floorDiv((int)spawnPos.z, minicraft.world.Chunk.DEPTH);
                
                loadingStatus = "Synthesizing landing zone...";
                world.getOrGenerate(scx, scz); // Ensure it's requested
                
                // Safety Gate: Wait for actual chunk arrival
                while(world.getChunk(scx, scz) == null) {
                    Thread.sleep(100);
                }

                player.setPosition(spawnPos.x, spawnPos.y + 1.2f, spawnPos.z);
                camera.setPosition(spawnPos.x, spawnPos.y + 1.2f + 1.6f, spawnPos.z);
                
                // Starting Tools - Removed duplicates and Stone Pickaxe
                player.inventory.add(new Item("TORCH", Block.TORCH), 10);
                player.inventory.setOffhandItem(new Item("TORCH", Block.TORCH));
                player.inventory.add(new ToolItem("Bronze Sword", ToolItem.ToolType.SWORD, 2, 5.0f, "item_sword_bronze"), 1);
                
                // Facility testing kit
                player.inventory.add(Block.COOKER, 1);
                player.inventory.add(Block.FURNACE, 1);
                player.inventory.add(Block.COAL_ORE, 15);
                player.inventory.add(Block.IRON_ORE, 5);
                player.inventory.add(new Item("RAW_MEAT"), 5);

                entityManager.spawn(player);

                // Register quest kill callback
                player.onKillCallback = (type) -> questManager.onEntityKilled(type);

                loadingStatus = "Finalizing terrain integration...";
                loadingProgress = 1.0f;
                Thread.sleep(800); // Premium pause
                
                isLoading = false;
                gameStartedTime = System.currentTimeMillis();
            } catch (Exception e) {
                e.printStackTrace();
                loadingStatus = "Initialization failed: " + e.getMessage();
            }
        }, "WorldGenThread").start();

        // ── Chunk Worker Thread — processes the generation queue in background ──
        new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                long[] pos = world.getGenerationQueue().poll();
                if (pos != null) {
                    world.processGeneration((int) pos[0], (int) pos[1]);
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "ChunkWorkerThread").start();

        // ── Register fixed-tick systems ───────────────────────────────────
        gameLoop.addTickable(dt -> {
            if (statusMessageTimer > 0)
                statusMessageTimer -= dt;
            else
                activeStatusMessage = "";

            updateFocusedBlock();

            entityManager.update(dt, world, particleManager);
            world.tick(dt, processingManager);
            particleManager.update(dt);

            // Quest tick — checks depth/level objectives, auto-completes
            java.util.List<minicraft.quest.Quest> completed = questManager.tick(player);
            for (minicraft.quest.Quest q : completed) {
                setStatusMessage("QUEST COMPLETE: " + q.title);
            }
        });

        // ── Shaders ───────────────────────────────────────────────────────
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(Utils.loadResource("/shaders/vertex.glsl"));
        shaderProgram.createFragmentShader(Utils.loadResource("/shaders/fragment.glsl"));
        shaderProgram.link();
        for (String u : new String[] {
                "projectionMatrix", "viewMatrix", "modelMatrix", "texture_sampler",
                "colorTint", "torchPos", "torchStrength", "useLighting",
                "sunBrightness", "weatherIntensity", "weatherType" }) {
            shaderProgram.createUniform(u);
        }

        uiRenderer = new UIRenderer(textures);
        entityRenderer = new EntityRenderer(textures);
        weatherRenderer = new minicraft.renderer.WeatherRenderer();

        // ── GI Initialisation ─────────────────────────────────────────────
        gtBuffer = new GTBuffer(framebufferW, framebufferH);

        rtgiShader = new ShaderProgram();
        rtgiShader.createComputeShader(Utils.loadResource("/shaders/rtgi.glsl"));
        rtgiShader.link();

        // Final validation for GI shader
        glValidateProgram(rtgiShader.getProgramId());
        if (glGetProgrami(rtgiShader.getProgramId(), GL_VALIDATE_STATUS) == GL_FALSE)
            throw new RuntimeException("RTGI shader invalid: " + glGetProgramInfoLog(rtgiShader.getProgramId()));

        for (String u : new String[] { "texAlbedo", "texNormal", "texDepth", "projectionMatrix", "viewMatrix",
                "invProjection", "invView", "cameraPos", "uTime" })
            rtgiShader.createUniform(u);

        compositeShader = new ShaderProgram();
        compositeShader.createVertexShader(Utils.loadResource("/shaders/post_vertex.glsl"));
        compositeShader.createFragmentShader(Utils.loadResource("/shaders/composite.glsl"));
        compositeShader.link();
        compositeShader.createUniform("texAlbedo");
        compositeShader.createUniform("texGI");
        compositeShader.createUniform("texNormal");
        compositeShader.createUniform("texDepth");
        compositeShader.createUniform("torchPos");
        compositeShader.createUniform("torchStrength");
        compositeShader.createUniform("invProjection");
        compositeShader.createUniform("invView");
        compositeShader.createUniform("rtgiEnabled");

        glValidateProgram(compositeShader.getProgramId());
        if (glGetProgrami(compositeShader.getProgramId(), GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("COMPOSITE SHADER INVALID: " + glGetProgramInfoLog(compositeShader.getProgramId()));
        }

        // Create GI Storage Texture
        giTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, giTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, framebufferW, framebufferH, 0, GL_RGBA, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
        float[] clearColor = { 0f, 0f, 0f, 1f };
        // Ensure GL44 is imported if glClearTexImage is to be used,
        // using simple clear fallback if not explicitly available.
        // For now, we manually bind and clear to be safe across environments.
        org.lwjgl.opengl.GL44.glClearTexImage(giTexture, 0, GL_RGBA, GL_FLOAT, clearColor);

        // Screen Quad for Composite
        float[] quadPos = { -1, 1, 0, -1, -1, 0, 1, -1, 0, 1, 1, 0 };
        float[] quadUV = { 0, 1, 0, 0, 1, 0, 1, 1 };
        int[] quadIdx = { 0, 1, 2, 0, 2, 3 };
        screenQuad = new Mesh(quadPos, quadUV, quadIdx, null);

        ModelRegistry.init(textures);
    }

    // ─────────────────────────────────────────────────────────────────────
    // loop() — main render + tick loop
    // ─────────────────────────────────────────────────────────────────────

    private void loop() {
        long lastTime = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            if (isLoading) {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                uiRenderer.renderLoadingScreen(shaderProgram, framebufferW, framebufferH, this);
                glfwSwapBuffers(window);
                glfwPollEvents();
                continue;
            }

            // ── Cursor mode ───────────────────────────────────────────────
            if (inventoryOpen || craftingOpen || shipConsoleOpen || chestOpen || furnaceOpen || cookerOpen) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }

            int currentW, currentH;
            try (MemoryStack stack = stackPush()) {
                IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
                glfwGetFramebufferSize(window, pw, ph);
                currentW = pw.get(0);
                currentH = ph.get(0);
            }

            // ── RESOLUTION RESILIENCE ─────────────────────────────────────
            if (currentW != framebufferW || currentH != framebufferH) {
                resizeCounter++;
                if (resizeCounter > 20) { // Debounce: Wait for 20 frames of stability
                    framebufferW = currentW;
                    framebufferH = currentH;
                    System.out.println("Resizing rendering pipeline: " + framebufferW + "x" + framebufferH);
                    try {
                        gtBuffer.cleanup();
                        gtBuffer = new minicraft.renderer.GTBuffer(framebufferW, framebufferH);

                        // Re-create GI Storage Texture
                        glDeleteTextures(giTexture);
                        giTexture = glGenTextures();
                        glBindTexture(GL_TEXTURE_2D, giTexture);
                        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, framebufferW, framebufferH, 0, GL_RGBA, GL_FLOAT, 0);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

                        float[] clr = { 0, 0, 0, 1 };
                        org.lwjgl.opengl.GL44.glClearTexImage(giTexture, 0, GL_RGBA, GL_FLOAT, clr);
                        resizeCounter = 0;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                resizeCounter = 0;
            }

            // ── 1. Input ──────────────────────────────────────────────────
            updateInput(dt);

            // ── 2. Fixed-timestep simulation tick ─────────────────────────
            gameLoop.update(dt);

            // ── 3. Drain pending one-shot actions (ship spawns, etc.) ─────
            Runnable action;
            while ((action = pendingActions.poll()) != null) {
                action.run();
            }

            // ── 4. Camera sync ────────────────────────────────────────────
            if (player.isRiding()) {
                minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
                float distance = 120.0f;
                float yawRad = (float) Math.toRadians(ship.yaw);
                float cos = (float) Math.cos(yawRad);
                float sin = (float) Math.sin(yawRad);

                // Position camera behind and above the ship
                camera.setPosition(
                        ship.position.x + sin * distance,
                        ship.position.y + 35.0f,
                        ship.position.z + cos * distance);
                camera.setRotation(camera.getRotation().x, ship.yaw, 0); // Preserve user pitch, follow ship yaw
            } else {
                camera.setPosition(player.position.x, player.position.y + 1.6f, player.position.z);
            }

            // ── 5. Chunk streaming ────────────────────────────────────────
            int cx = (int) Math.floor(player.position.x / 16.0);
            int cz = (int) Math.floor(player.position.z / 16.0);
            world.update(cx, cz, dt);

            // ── 6. Render Pass — Geometry (G-Buffer) ──────────────────────
            gtBuffer.bind();
            updateAtmosphere(dt); // Set the dynamic sky clear color
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);

            shaderProgram.bind();
            float aspect = (float) framebufferW / Math.max(1, framebufferH);

            projectionMatrix.perspective(FOV, aspect, Z_NEAR, Z_FAR);
            viewMatrix.set(camera.getViewMatrix());
            modelMatrix.identity();

            shaderProgram.setUniform("projectionMatrix", projectionMatrix);
            shaderProgram.setUniform("viewMatrix", viewMatrix);
            shaderProgram.setUniform("modelMatrix", modelMatrix);
            shaderProgram.setUniform("texture_sampler", 0);
            shaderProgram.setUniform("colorTint", whiteTint);
            shaderProgram.setUniform("sunBrightness", world.getWeatherManager().getSunBrightness());
            shaderProgram.setUniform("torchPos", player.position);
            shaderProgram.setUniform("torchStrength", (player.inventory.hasTorchEquipped() ? 1.0f : 0.0f));

            minicraft.math.Vector3f totalGlow = player.inventory.getTotalGlow();
            shaderProgram.setUniform("glowColor", totalGlow);
            shaderProgram.setUniform("glowStrength", (totalGlow.x + totalGlow.y + totalGlow.z > 0.01f ? 1.0f : 0.0f));
            boolean isUnderwater = world.getBlock(
                    (int) Math.floor(player.position.x),
                    (int) Math.floor(player.position.y + 1.6f),
                    (int) Math.floor(player.position.z)) == Block.WATER;

            shaderProgram.setUniform("colorTint",
                    isUnderwater ? new Vector4f(0.4f, 0.6f, 1.0f, 1.0f) : whiteTint);

            world.render(shaderProgram, player.position, 1.0f);
            shaderProgram.setUniform("useLighting", 1.0f); // IMPORTANT: Enable Lighting Engine

            float sun = world.getWeatherManager().getSunBrightness();
            entityRenderer.render(entityManager, shaderProgram, textures, viewMatrix, sun);
            particleManager.render(shaderProgram, textures, viewMatrix, projectionMatrix);

            glEnable(GL_BLEND);
            weatherRenderer.render(player.position, world.getWeather(), shaderProgram, dt);

            glDisable(GL_DEPTH_TEST);
            renderHand(shaderProgram, dt);
            glEnable(GL_DEPTH_TEST);

            shaderProgram.unbind();
            gtBuffer.unbind();

            // ── 7. Render Pass — RTGI (Compute) ──────────────────────────
            rtgiShader.bind();

            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getAlbedoTexture());
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getNormalTexture());
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getDepthTexture());

            glBindImageTexture(0, giTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);

            rtgiShader.setUniform("projectionMatrix", projectionMatrix);
            rtgiShader.setUniform("viewMatrix", viewMatrix);

            Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
            Matrix4f invView = new Matrix4f(viewMatrix).invert();
            rtgiShader.setUniform("invProjection", invProj);
            rtgiShader.setUniform("invView", invView);

            rtgiShader.setUniform("cameraPos", camera.getPosition());
            rtgiShader.setUniform("uTime", (float) (System.currentTimeMillis() % 100000) / 1000f);

            if (rtgiEnabled) {
                rtgiShader.dispatchCompute((framebufferW + 15) / 16, (framebufferH + 15) / 16, 1);
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);
                // CLEANUP: Unbind image unit 0
                glBindImageTexture(0, 0, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
            }

            rtgiShader.unbind();

            // ── 8. Render Pass — Final Composite (Full-Screen) ────────────
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, framebufferW, framebufferH);
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Restore black clear
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);

            compositeShader.bind();

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getAlbedoTexture());
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, giTexture);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getNormalTexture());
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, gtBuffer.getDepthTexture());

            compositeShader.setUniform("texAlbedo", 0);
            compositeShader.setUniform("texGI", 1);
            compositeShader.setUniform("texNormal", 2);
            compositeShader.setUniform("texDepth", 3);
            compositeShader.setUniform("torchPos", player.position);
            compositeShader.setUniform("torchStrength", (player.inventory.hasTorchEquipped() ? 1.0f : 0.0f));
            compositeShader.setUniform("invProjection", invProj);
            compositeShader.setUniform("invView", invView);

            compositeShader.setUniform("rtgiEnabled", rtgiEnabled ? 1 : 0);

            // FIXED: Don't use screenQuad.render() because it unbinds unit 0
            glBindVertexArray(screenQuad.getVaoId());
            glDrawElements(GL_TRIANGLES, screenQuad.getVertexCount(), GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);

            compositeShader.unbind();


            // ── 9. Overlays (Inventory / UI) ─────────────────────────────
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            
            shaderProgram.bind();
            uiRenderer.render(player, shaderProgram, currentW, currentH, this);
            if (questLogOpen) {
                uiRenderer.renderQuestLog(player, shaderProgram, currentW, currentH, this);
            }
            shaderProgram.unbind();

            glEnable(GL_DEPTH_TEST);

            // Item pickup logic
            if (!craftingOpen) {
                minicraft.entity.Entity item = entityManager.getNearestOfType(
                        camera.getPosition().x, camera.getPosition().y, camera.getPosition().z,
                        2.0f, minicraft.entity.EntityType.ITEM);
                if (item instanceof minicraft.entity.ItemEntity) {
                    player.inventory.add(((minicraft.entity.ItemEntity) item).block, 1);
                    item.damage(100f, null);
                }
            }

            shaderProgram.unbind();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ship spawning
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Heavy spawn work — clears airspace then places the schematic.
     * Called via pendingActions queue, never directly from a GLFW callback.
     *
     * Phase 3 will split this across multiple ticks so large ships spawn
     * progressively without any visible stutter. The queue pattern here is
     * the exact foundation that makes that change trivial.
     */
    private void executeShipSpawn(ShipDefinition def, int wx, int wy, int wz) {
        // Clear airspace around the drydock
        for (int dx = -50; dx <= 50; dx++)
            for (int dy = 0; dy <= 40; dy++)
                for (int dz = -20; dz <= 80; dz++)
                    world.setBlock(wx + dx, wy + dy, wz + dz, Block.AIR);

        // Place schematic blocks (REMOVED: flyable ships use VoxelBaker entities only)
        // spawnShipFromSchematic(def, wx, wy, wz);

        // Teleport player to the bridge
        org.joml.Vector3i bridge = def.schematic.bridgeLocalPos;
        player.setPosition(
                wx + bridge.x + 0.5f,
                wy + bridge.y + 1.1f,
                wz + bridge.z + 0.5f);

        // 3. Create the ShipEntity for physics and piloting
        minicraft.entity.ship.ShipEntity ship = new minicraft.entity.ship.ShipEntity(minicraft.entity.EntityType.SHIP,
                def);
        ship.position.set(wx, wy, wz);
        entityManager.spawn(ship);

        // 4. Force player into Neural Link (Riding State)
        player.setRiding(ship);
        ship.setPassenger(player);

        System.out.println("NEURAL LINK ESTABLISHED: Pilot synchronized with " + def.displayName);
    }

    private void spawnShipFromSchematic(ShipDefinition def, int wx, int wy, int wz) {
        minicraft.ship.ShipSchematic s = def.schematic;
        for (int i = 0; i < s.blockCount; i++) {
            org.joml.Vector3i local = s.getLocalPosition(i);
            world.setBlock(wx + local.x, wy + local.y, wz + local.z, s.getBlock(i));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────

    private void updateInput(float dt) {
        handleMovementInput(dt);
        handleInteractionInput(dt);
    }

    private void handleMovementInput(float dt) {
        if (craftingOpen) {
            player.velocity.x = 0;
            player.velocity.z = 0;
            return;
        }

        float speed = 8.0f * player.inventory.getTotalSpeedMod();
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = 0, dz = 0;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            dx += (float) Math.sin(yaw) * speed;
            dz -= (float) Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            dx -= (float) Math.sin(yaw) * speed;
            dz += (float) Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            dx -= (float) Math.cos(yaw) * speed;
            dz -= (float) Math.sin(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            dx += (float) Math.cos(yaw) * speed;
            dz += (float) Math.sin(yaw) * speed;
        }

        if (player.isRiding()) {
            minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
            boolean w = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
            boolean s = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
            boolean a = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
            boolean d = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
            boolean space = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
            boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;

            ship.handleInput(w, s, a, d, space, shift, dt);

            // Weapons
            if (glfwGetKey(window, GLFW_KEY_F) == GLFW_PRESS && !prevF)
                ship.nextWeapon();
            if (glfwGetKey(window, GLFW_KEY_G) == GLFW_PRESS && !prevG)
                ship.prevWeapon();
            prevF = glfwGetKey(window, GLFW_KEY_F) == GLFW_PRESS;
            prevG = glfwGetKey(window, GLFW_KEY_G) == GLFW_PRESS;

            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                ship.fireActiveWeapon(entityManager, world, particleManager, getLookDirection());
            }

            // Exit ship
            if (glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS) {
                ship.setPassenger(null);
                player.setRiding(null);
                System.out.println("NEURAL LINK SEVERED: Local control restored.");
            }
            player.velocity.set(0, 0, 0);
        } else {
            player.velocity.x = dx;
            player.velocity.z = dz;
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && player.isGrounded) {
            player.velocity.y = 8.2f;
            player.isGrounded = false;
        }

        // Mouse look (polled here as well as via callback for reliability)
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        if (firstMouse) {
            lastMouseX = mx[0];
            lastMouseY = my[0];
            firstMouse = false;
        }
        double mdx = mx[0] - lastMouseX;
        double mdy = my[0] - lastMouseY;
        lastMouseX = mx[0];
        lastMouseY = my[0];
        camera.moveRotation((float) (mdy * MOUSE_SENSITIVITY), (float) (mdx * MOUSE_SENSITIVITY), 0);

        if (camera.getRotation().x > 89f)
            camera.setRotation(89f, camera.getRotation().y, 0);
        if (camera.getRotation().x < -89f)
            camera.setRotation(-89f, camera.getRotation().y, 0);
    }

    private void handleInventoryInput() {
        prevMouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    private void handleInteractionInput(float dt) {
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

        // Quest log — Q key
        boolean isQ = glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS;
        if (isQ && !prevQ) {
            questLogOpen = !questLogOpen;
            if (questLogOpen) {
                inventoryOpen = false; craftingOpen = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }
        prevQ = isQ;

        if (questLogOpen) return; // consume input while journal is open

        if (inventoryOpen) {
            handleInventoryInput();
            return;
        }

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
        if (shipConsoleOpen || chestOpen)
            return;

        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean mouseRightDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        if (mouseLeftDown) {
            handlePlayerAttack(dt);
        } else {
            miningProgress = 0f;
            miningX = -1;
        }

        if (mouseRightDown && !prevMouseRightDown)
            handlePlayerPlace();

        prevMouseLeftDown = mouseLeftDown;
        prevMouseRightDown = mouseRightDown;
    }

    // ─────────────────────────────────────────────────────────────────────
    public void setStatusMessage(String msg) {
        if (msg == null || msg.isEmpty())
            return;
        this.activeStatusMessage = msg;
        this.statusMessageTimer = 3.2f; // ~3 seconds visibility
    }

    // UI click handlers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called from the GLFW mouse callback when the ship console is open.
     *
     * Does NOT do any heavy work itself — posts executeShipSpawn() to
     * pendingActions so the work runs on the main thread next frame.
     * This is the fix for Bug 2.
     */
    private void handleShipConsoleClick() {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        float[] mouse = getScaledMouse();
        float x = mouse[0];
        float y = mouse[1];

        float panelW = 900;
        float panelH = 600;
        float sx = (WIN_W - panelW) / 2f;
        float sy = (WIN_H - panelH) / 2f;
        float btnWidth = 260;
        float btnHeight = 400;
        float gap = 30;
        float startX = sx + (panelW - (btnWidth * 3 + gap * 2)) / 2f;
        float startY = sy + 120;

        List<ShipDefinition> ships = ShipRegistry.getInstance().getAll();
        for (int i = 0; i < ships.size(); i++) {
            float bx = startX + i * (btnWidth + gap);
            if (x >= bx && x <= bx + btnWidth && y >= startY && y <= startY + btnHeight) {

                ShipDefinition def = ships.get(i);

                // Capture drydock coords — the lambda runs next frame
                // so we must not close over the mutable fields directly.
                final int spawnX = drydockX;
                final int spawnY = drydockY;
                final int spawnZ = drydockZ;

                // Post the heavy work — callback returns instantly, no freeze
                pendingActions.add(() -> executeShipSpawn(def, spawnX, spawnY, spawnZ));

                shipConsoleOpen = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                firstMouse = true;
                return;
            }
        }
    }

    private void handleFacilityClick() {
        if (activeFacility == null) return;
        float[] mouse = getScaledMouse();
        minicraft.world.behavior.FurnaceBlock.handleClick(activeFacility, player, this, mouse[0], mouse[1], framebufferW, framebufferH);
    }

    public float[] getScaledMouse() {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        int[] winW = new int[1], winH = new int[1];
        glfwGetWindowSize(window, winW, winH);
        float scaleX = winW[0] > 0 ? (float) framebufferW / winW[0] : 1f;
        float scaleY = winH[0] > 0 ? (float) framebufferH / winH[0] : 1f;
        return new float[] { (float) mx[0] * scaleX, (float) my[0] * scaleY };
    }

    private void handleInventoryClick() {
        float[] mouse = getScaledMouse();
        float x = mouse[0], y = mouse[1];

        boolean isShift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;

        // ── Must exactly match UIRenderer.renderInventoryScreen constants ──
        final float SLOT = 58f;
        final float GAP = 8f;
        final float COLS = 9f;
        final float ROWS = 4f;

        float gridW = COLS * SLOT + (COLS - 1) * GAP;
        float dollAreaW = 240f;
        float panelW = gridW + dollAreaW + 80f;
        float panelH = 4 * SLOT + 3 * GAP + 140f;
        float sx = (framebufferW - panelW) / 2f;
        float sy = (framebufferH - panelH) / 2f;

        float dollX = sx + 28f;
        float armorX = dollX + 160f + 12f;
        float armorYBase = sy + 64f;

        float gridStartX = sx + dollAreaW + 50f;
        float mainGridY = sy + 64f;

        // ── Main 81-slot grid (Scrollable) ──────────────────────────────────
        float gridVisibleYMin = mainGridY;
        float gridVisibleYMax = mainGridY + 3 * (SLOT + GAP); // Only top 3 rows visible area

        for (int i = 0; i < 81; i++) {
            int col = i % 9, row = i / 9;
            float slotX = gridStartX + col * (SLOT + GAP);
            float slotY = mainGridY + row * (SLOT + GAP) - inventoryScroll;

            // Only allow interaction if the slot is within the visible scissored window
            if (slotY + SLOT > gridVisibleYMin && slotY < gridVisibleYMax) {
                if (x >= slotX && x <= slotX + SLOT && y >= slotY && y <= slotY + SLOT) {
                    if (isShift)
                        player.inventory.quickMove(i, false);
                    else
                        player.inventory.clickSlot(i, false);
                    return;
                }
            }
        }

        // ── Armor Slots ───────────────────────────────────────────────────
        minicraft.item.ArmorItem.ArmorSlot[] types = minicraft.item.ArmorItem.ArmorSlot.values();
        for (int i = 0; i < 4; i++) {
            float slotY = armorYBase + i * (SLOT + GAP);
            if (x >= armorX && x <= armorX + SLOT && y >= slotY && y <= slotY + SLOT) {
                player.inventory.clickArmorSlot(types[i]);
                return;
            }
        }

        // ── Hotbar row ─────────────────────────────────────────────────────
        // Separator line sits at mainGridY + 3*(SLOT+GAP) + 6, hotbar row 14px below
        float sepY = mainGridY + 3 * (SLOT + GAP) + 6f;
        float hotbarRowY = sepY + 14f;
        for (int i = 0; i < 9; i++) {
            float slotX = gridStartX + i * (SLOT + GAP);
            if (x >= slotX && x <= slotX + SLOT && y >= hotbarRowY && y <= hotbarRowY + SLOT) {
                if (isShift)
                    player.inventory.quickMove(i, true);
                else
                    player.inventory.clickSlot(i, true);
                return;
            }
        }
    }

    private void handleChestClick() {
        if (activeChest == null) return;
        float[] mouse = getScaledMouse();
        float x = mouse[0], y = mouse[1];

        float panelW = 800;
        float sx = (framebufferW - panelW) / 2f;
        float sy = (framebufferH - 550) / 2f;
        float slotSize = 64;
        float gap = 15;
        float startX = sx + (panelW - (slotSize + gap) * 9) / 2f;

        for (int i = 0; i < 27; i++) {
            float slotX = startX + (i % 9) * (slotSize + gap);
            float slotY = sy + 80 + (i / 9) * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                activeChest.clickSlot(i, false);
                return;
            }
        }

        float playerStartY = sy + 300;
        for (int i = 0; i < 27; i++) {
            float slotX = startX + (i % 9) * (slotSize + gap);
            float slotY = playerStartY + (i / 9) * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                player.inventory.clickSlot(i, false);
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Crafting input
    // ─────────────────────────────────────────────────────────────────────

    private void handleCraftingInput() {
        // ── Constants MUST match UIRenderer.renderCraftingMenu exactly ────────
        final int COLS = 7;
        final float MENU_W = 700f, MENU_H = 540f;
        final float ICON_SIZE = 56f, ICON_GAP = 8f;
        final float GRID_OFF_X = 14f, GRID_OFF_Y = 60f;
        final float DETAIL_W = 182f;
        final float SX = (framebufferW - MENU_W) / 2f;
        final float SY = (framebufferH - MENU_H) / 2f;
        final float GX = SX + GRID_OFF_X;
        final float GY = SY + GRID_OFF_Y;
        final float GRID_AREA_W = COLS * ICON_SIZE + (COLS - 1) * ICON_GAP;

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : craftingManager.getRecipes())
            if (r.getCategory() == activeCategory)
                filtered.add(r);

        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        if (mouseLeftDown && !prevMouseLeftDown) {
            float[] mouse = getScaledMouse();
            float x = mouse[0], y = mouse[1];

            // Tab clicks
            Recipe.Category[] cats = Recipe.Category.values();
            float tabW = GRID_AREA_W / cats.length;
            float tabY = SY + 34f;
            if (y >= tabY && y <= tabY + 24f) {
                for (int i = 0; i < cats.length; i++) {
                    float tx = SX + GRID_OFF_X + i * (tabW + 2f);
                    if (x >= tx && x <= tx + tabW - 2f) {
                        activeCategory = cats[i];
                        recipeIndex = 0;
                        recipeScrollOffset = 0;
                        break;
                    }
                }
            }

            // Icon grid clicks
            for (int i = 0; i < filtered.size(); i++) {
                int col = i % COLS, row = i / COLS;
                float ix = GX + col * (ICON_SIZE + ICON_GAP);
                float iy = GY + row * (ICON_SIZE + ICON_GAP) - recipeScrollOffset;
                
                // Clip to viewport
                if (iy + ICON_SIZE < GY || iy > SY + MENU_H - 30f)
                    continue;

                if (x >= ix && x <= ix + ICON_SIZE && y >= iy && y <= iy + ICON_SIZE) {
                    recipeIndex = i;
                    break;
                }
            }

            // Forge button click
            if (recipeIndex >= 0 && recipeIndex < filtered.size()) {
                float bbx = SX + GRID_OFF_X + GRID_AREA_W + 18f + 8f;
                float bby = SY + MENU_H - 52f;
                float bbw = DETAIL_W - 16f;
                if (x >= bbx && x <= bbx + bbw && y >= bby && y <= bby + 38f)
                    craftingManager.craft(filtered.get(recipeIndex), player.inventory);
            }
        }

        // Arrow keys navigate the grid (left/right/up/down)
        boolean isUp = glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS;
        boolean isDown = glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS;
        boolean isLeft = glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS;
        boolean isRight = glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS;
        boolean isEnter = glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS;

        if (!filtered.isEmpty()) {
            if (isUp && !prevUp)
                recipeIndex = Math.max(0, recipeIndex - COLS);
            if (isDown && !prevDown)
                recipeIndex = Math.min(filtered.size() - 1, recipeIndex + COLS);
            if (isLeft && !prevLeft)
                recipeIndex = Math.max(0, recipeIndex - 1);
            if (isRight && !prevRight)
                recipeIndex = Math.min(filtered.size() - 1, recipeIndex + 1);

            // Handle Mouse Wheel
            if (glfwGetWindowAttrib(window, GLFW_HOVERED) != 0) {
                // Approximate wheel delta from a static or pending scroll field if it existed, 
                // but since we don't have a direct scroll callback field for crafting yet, 
                // we use a simple jump for now until I add a scroll callback or use a known one.
                // For now, let's just ensure Smart Scroll works.
            }

            // Smart Scroll: Ensure recipeIndex is in view
            int selRow = recipeIndex / COLS;
            float selYMin = GY + selRow * (ICON_SIZE + ICON_GAP) - recipeScrollOffset;
            float selYMax = selYMin + ICON_SIZE;
            float viewMax = SY + MENU_H - 30f;

            if (selYMin < GY) {
                recipeScrollOffset = (int) (selRow * (ICON_SIZE + ICON_GAP));
            } else if (selYMax > viewMax) {
                recipeScrollOffset = (int) ((selRow + 1) * (ICON_SIZE + ICON_GAP) - (viewMax - GY));
            }
        }
        if (isEnter && !prevEnter && recipeIndex < filtered.size())
            craftingManager.craft(filtered.get(recipeIndex), player.inventory);

        prevUp = isUp;
        prevDown = isDown;
        prevLeft = isLeft;
        prevRight = isRight;
        prevEnter = isEnter;
        prevMouseLeftDown = mouseLeftDown;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Combat + placement
    // ─────────────────────────────────────────────────────────────────────

    private org.joml.Vector3f getLookDirection() {
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        return new org.joml.Vector3f(
                (float) (Math.sin(yaw) * Math.cos(pitch)),
                (float) -Math.sin(pitch),
                (float) (-Math.cos(yaw) * Math.cos(pitch)));
    }

    private void updateFocusedBlock() {
        if (craftingOpen || inventoryOpen || chestOpen || furnaceOpen || cookerOpen) {
            focusedBlock = Block.AIR;
            return;
        }

        Vector3f pos = camera.getPosition();
        org.joml.Vector3f dir = getLookDirection();
        float reach = 10.0f;

        for (float dist = 0.5f; dist <= reach; dist += 0.2f) {
            int gx = (int) Math.floor(pos.x + dir.x * dist);
            int gy = (int) Math.floor(pos.y + dir.y * dist);
            int gz = (int) Math.floor(pos.z + dir.z * dist);
            Block b = world.getBlock(gx, gy, gz);
            if (b.solid) {
                focusedBlock = b;
                return;
            }
        }
        focusedBlock = Block.AIR;
    }

    private void handlePlayerAttack(float dt) {
        if (!prevMouseLeftDown)
            player.meleeAttack(entityManager, particleManager);

        Vector3f pos = camera.getPosition();
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) (-Math.sin(pitch));
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
        float reach = 10.0f;

        for (float dist = 0.5f; dist <= reach; dist += 0.1f) {
            int gx = (int) Math.floor(pos.x + dx * dist);
            int gy = (int) Math.floor(pos.y + dy * dist);
            int gz = (int) Math.floor(pos.z + dz * dist);
            Block b = world.getBlock(gx, gy, gz);
            if (!b.solid)
                continue;
            if (b.hardness < 0)
                return;

            if (gx != miningX || gy != miningY || gz != miningZ) {
                miningX = gx;
                miningY = gy;
                miningZ = gz;
                miningProgress = 0f;
            }

            Item held = player.inventory.getSelectedItem();
            float baseEfficiency = 0.5f; // Hand efficiency
            int toolLevel = -1; // Hand tool level
            ToolItem.ToolType toolType = null;

            if (held instanceof ToolItem) {
                ToolItem t = (ToolItem) held;
                baseEfficiency = t.getEfficiency();
                toolLevel = t.getHarvestLevel();
                toolType = t.getToolType();
            }

            // --- Tool Type Efficiency Check ---
            float efficiencyMultiplier = 1.0f;
            boolean isSoftBlock = (b == Block.DIRT || b == Block.GRASS || b == Block.SAND || b == Block.RED_SAND || b == Block.SNOW || b == Block.PODZOL);
            boolean isHardBlock = (b == Block.STONE || b == Block.STONE_DARK || b == Block.STONE_BRICKS || b.name().contains("ORE") || b.name().contains("BLOCK"));
            boolean isWoodBlock = (b.name().contains("WOOD") || b == Block.WOOD);

            if (isSoftBlock) {
                if (toolType != ToolItem.ToolType.SHOVEL) {
                    efficiencyMultiplier = 0.2f; // 80% slower
                }
            } else if (isHardBlock) {
                if (toolType != ToolItem.ToolType.PICKAXE) {
                    efficiencyMultiplier = 0.2f; // 80% slower
                }
            } else if (isWoodBlock) {
                if (toolType != ToolItem.ToolType.AXE) {
                    efficiencyMultiplier = 0.2f; // 80% slower
                }
            }

            float finalEfficiency = baseEfficiency * efficiencyMultiplier;

            if (toolLevel < b.requiredHarvestLevel) {
                miningProgress = 0f;
            } else {
                float armorMiningMod = player.inventory.hasFullSet("Plutonium") ? 1.5f : 1.0f;
                miningProgress += (dt * finalEfficiency * armorMiningMod) / b.hardness;
            }

            player.miningProgress = miningProgress;

            if (miningProgress >= 1.0f) {
                world.setBlock(gx, gy, gz, Block.AIR);
                miningProgress = player.miningProgress = 0f;
                player.addXp(b.xpValue, particleManager);
                questManager.onBlockMined(b); // Quest hook
                if (toolLevel >= b.requiredHarvestLevel) {
                    ItemEntity drop = new ItemEntity(b);
                    drop.setPosition(gx + 0.5f, gy + 0.5f, gz + 0.5f);
                    drop.velocity.set(0, 2f, 0);
                    entityManager.spawn(drop);
                }
            }
            return;
        }
        miningProgress = player.miningProgress = 0f;
    }

    private void handlePlayerPlace() {
        Vector3f pos = camera.getPosition();
        float pitch = (float) Math.toRadians(camera.getRotation().x);
        float yaw = (float) Math.toRadians(camera.getRotation().y);
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) (-Math.sin(pitch));
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
        float reach = 10.0f;

        for (float dist = 0.5f; dist <= reach; dist += 0.1f) {
            int gx = (int) Math.floor(pos.x + dx * dist);
            int gy = (int) Math.floor(pos.y + dy * dist);
            int gz = (int) Math.floor(pos.z + dz * dist);
            Block targeted = world.getBlock(gx, gy, gz);

            // ── 1. Block Interactions ──
            if (targeted.interaction != null) {
                targeted.onInteract(this, world, gx, gy, gz);
                return;
            }

            // Fallback for non-interaction blocks (legacy safety)
            if (targeted == Block.SHIP_CONSOLE) { // SHIP_CONSOLE is handled by its behavior, so this block could be removed too, but let's be safe.
                shipConsoleOpen = true;
                drydockX = gx;
                drydockY = gy - 1;
                drydockZ = gz;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                return;
            }

            if (targeted.solid) {
                Block selected = player.inventory.getSelectedBlock();
                if (selected == null)
                    return;

                int pgx = (int) Math.floor(pos.x + dx * (dist - 0.1f));
                int pgy = (int) Math.floor(pos.y + dy * (dist - 0.1f));
                int pgz = (int) Math.floor(pos.z + dz * (dist - 0.1f));

                if (pgx == (int) Math.floor(pos.x) && pgz == (int) Math.floor(pos.z)
                        && (pgy == (int) Math.floor(pos.y) || pgy == (int) Math.floor(pos.y + 1)))
                    return;

                if (!world.getBlock(pgx, pgy, pgz).solid) {
                    world.setBlock(pgx, pgy, pgz, selected);
                    player.inventory.remove(selected, 1);
                }
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Atmosphere
    // ─────────────────────────────────────────────────────────────────────

    private void updateAtmosphere(float dt) {
        worldTime = (worldTime + dt * 100f) % DAY_LENGTH;
        float t = worldTime / DAY_LENGTH;

        float r, g, b;
        if (t < 0.25f) {
            float p = t * 4;
            r = 0.45f + p * 0.4f;
            g = 0.68f;
            b = 0.95f;
        } else if (t < 0.50f) {
            r = 0.45f;
            g = 0.68f;
            b = 0.95f;
        } else if (t < 0.75f) {
            float p = (t - 0.5f) * 4;
            r = 0.45f + p * 0.3f;
            g = 0.68f - p * 0.4f;
            b = 0.95f - p * 0.6f;
        } else {
            r = 0.1f;
            g = 0.1f;
            b = 0.2f;
        }

        minicraft.math.Vector3f fog = world.getFogColor(
                world.getGenerator().generate(player.position.x, player.position.z).biome);
        glClearColor((r + fog.x) * 0.5f, (g + fog.y) * 0.5f, (b + fog.z) * 0.5f, 1.0f);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Hand rendering
    // ─────────────────────────────────────────────────────────────────────

    private void renderHand(ShaderProgram shader, float dt) {
        Item heldItem = player.inventory.getSelectedItem();
        if (heldItem == null)
            return;

        Mesh model = null;
        if (heldItem.getName().equalsIgnoreCase("Iron Pickaxe"))
            model = ModelRegistry.getModel("pickaxe_iron");
        else if (heldItem.getName().equalsIgnoreCase("Stone Pickaxe"))
            model = ModelRegistry.getModel("pickaxe_stone");
        else if (heldItem.getName().equalsIgnoreCase("Diamond Pickaxe"))
            model = ModelRegistry.getModel("pickaxe_diamond");
        else if (heldItem.getName().contains("Pickaxe"))
            model = ModelRegistry.getModel("pickaxe_wooden");
        else if (heldItem instanceof minicraft.item.ArmorItem) {
            String tier = ((minicraft.item.ArmorItem) heldItem).getTierName().toLowerCase();
            TextureRegion reg = textures.get(tier + "_ore");
            model = ModelRegistry.getModel("primitive_cube");
            if (model != null && reg != null) {
                model.updateUVs(reg);
            }
        }

        if (model == null)
            return;

        float walkSpeed = (float) Math.sqrt(
                player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z);
        handAnimTime = (player.isGrounded && walkSpeed > 0.1f) ? handAnimTime + dt * walkSpeed * 0.5f : 0f;

        float bobX = (float) Math.sin(handAnimTime * 2.0f) * 0.02f;
        float bobY = (float) Math.abs(Math.cos(handAnimTime * 2.0f)) * 0.02f;

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && !craftingOpen)
            isHandSwinging = true;
        if (isHandSwinging) {
            handSwingTime += dt * 8.0f;
            if (handSwingTime > Math.PI) {
                handSwingTime = 0f;
                isHandSwinging = false;
            }
        }
        float swingRot = (float) Math.sin(handSwingTime) * 45.0f;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClear(GL_DEPTH_BUFFER_BIT);

        Matrix4f h_proj = new Matrix4f().perspective(
                (float) Math.toRadians(70), (float) framebufferW / Math.max(1, framebufferH), 0.01f, 100f);
        Matrix4f h_view = new Matrix4f().identity();
        Matrix4f h_model = new Matrix4f().identity();
        h_model.translate(0.55f + bobX, -0.45f + bobY, -0.65f);
        h_model.rotate((float) Math.toRadians(70), new Vector3f(0, 1, 0)); // Fixed orientation: pointing forward-right
        h_model.rotate((float) Math.toRadians(-25 + (isHandSwinging ? swingRot : 0)), new Vector3f(1, 0, 0));
        h_model.rotate((float) Math.toRadians(20), new Vector3f(0, 0, 1));
        h_model.scale(0.4f, 0.4f, 0.4f);

        shader.setUniform("projectionMatrix", h_proj);
        shader.setUniform("viewMatrix", h_view);
        shader.setUniform("modelMatrix", h_model);
        shader.setUniform("useLighting", 1);
        shader.setUniform("sunBrightness", 0.9f);
        shader.setUniform("torchStrength", 0.5f);

        model.render();
        glEnable(GL_CULL_FACE);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────

    private void cleanup() {
        if (shaderProgram != null)
            shaderProgram.cleanup();
        if (world != null)
            world.cleanup();
        if (textures != null)
            textures.cleanup();
        if (entityRenderer != null)
            entityRenderer.cleanup();
        if (uiRenderer != null)
            uiRenderer.cleanup();

        // GI Cleanup
        if (gtBuffer != null)
            gtBuffer.cleanup();
        if (rtgiShader != null)
            rtgiShader.cleanup();
        if (compositeShader != null)
            compositeShader.cleanup();
        if (giTexture != 0)
            glDeleteTextures(giTexture);
        if (screenQuad != null)
            screenQuad.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}