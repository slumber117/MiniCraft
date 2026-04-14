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
    private int framebufferW = WIN_W;
    private int framebufferH = WIN_H;
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
    public boolean inventoryOpen = false;
    public boolean chestOpen = false;
    public boolean shipConsoleOpen = false;
    public minicraft.entity.Inventory activeChest = null;

    // ── Shipyard ──────────────────────────────────────────────────────────
    public int drydockX = 0, drydockY = 0, drydockZ = 0;

    // ── Crafting ──────────────────────────────────────────────────────────
    public Recipe.Category activeCategory = Recipe.Category.TOOLS;
    public int recipeIndex = 0;
    public int inventoryIndex = 0;
    public int chestIndex = 0;
    public final CraftingManager craftingManager = new CraftingManager();

    private boolean prevC = false, prevE = false;
    private boolean prevEnter = false, prevUp = false, prevDown = false;

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
            System.out.println("MiniCraft | LWJGL " + Version.getVersion());

            // ── Initialise registries BEFORE anything else ────────────────
            ShipRegistry.initialize();

            // ── Fixed-timestep game loop (20 Hz) ─────────────────────────
            gameLoop = new GameLoop(20);

            init();
            setup();
            loop();
            cleanup();

        } catch (Exception e) {
            System.err.println("CRITICAL ENGINE ERROR: See crash_report.txt");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("crash_report.txt"))) {
                e.printStackTrace(pw);
                pw.println("\n--- Engine State ---");
                pw.println("World ready:    " + (world != null));
                pw.println("Player health:  " + (player != null ? player.getHealth() : "N/A"));
            } catch (java.io.IOException io) {
                io.printStackTrace();
            }
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public long getWindow() {
        return window;
    }

    public World getWorld() {
        return world;
    }

    // ─────────────────────────────────────────────────────────────────────
    // init() — GLFW window + callbacks
    // ─────────────────────────────────────────────────────────────────────

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Cannot init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
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
                    shipConsoleOpen = false;
                    glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    firstMouse = true;
                }
            }
        });

        // ── Mouse button callback ─────────────────────────────────────────
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                if (inventoryOpen)
                    handleInventoryClick();
                else if (chestOpen)
                    handleChestClick();
                else if (shipConsoleOpen)
                    handleShipConsoleClick();
            }
        });

        // ── Cursor callback ───────────────────────────────────────────────
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (!inventoryOpen && !craftingOpen && !shipConsoleOpen && !chestOpen) {
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

        glfwSetScrollCallback(window, (win, xoff, yoff) -> player.inventory.changeSelection((int) -yoff));
    }

    // ─────────────────────────────────────────────────────────────────────
    // setup() — OpenGL + world + player
    // ─────────────────────────────────────────────────────────────────────

    private void setup() throws Exception {
        GL.createCapabilities();

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
        System.out.println("Generating initial chunks...");
        world.update(0, 0, 0f);
        System.out.println("World ready.");

        camera = new Camera();

        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                world.getOrGenerate(x, z);

        minicraft.math.Vector3f spawnPos = world.findSafeGrassSpawn(8, 8);
        System.out.println("Spawn: " + spawnPos);

        player = new Player(camera);
        player.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);

        Item torch = new Item("TORCH", Block.TORCH);
        player.inventory.add(torch, 10);
        player.inventory.setOffhandItem(torch);

        ToolItem sword = new ToolItem("Bronze Sword", ToolItem.ToolType.SWORD, 2, 5.0f, "item_sword_bronze");
        ToolItem pick = new ToolItem("Wooden Pickaxe", ToolItem.ToolType.PICKAXE, 0, 2.0f, "item_pick_wood");
        player.inventory.add(sword, 1);
        player.inventory.add(pick, 1);

        camera.setPosition(spawnPos.x, spawnPos.y + 1.6f, spawnPos.z);

        entityManager = new EntityManager();
        entityManager.spawn(player);

        // ── Register fixed-tick systems ───────────────────────────────────
        gameLoop.addTickable(dt -> {
            entityManager.update(dt, world);
            world.update(dt, player, particleManager);
            particleManager.update(dt);
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

            // ── Cursor mode ───────────────────────────────────────────────
            if (inventoryOpen || craftingOpen || shipConsoleOpen || chestOpen) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            int currentW, currentH;
            try (MemoryStack stack = stackPush()) {
                IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
                glfwGetWindowSize(window, pw, ph);
                currentW = pw.get(0);
                currentH = ph.get(0);
            }

            // ── 1. Input ──────────────────────────────────────────────────
            updateInput(dt);

            // ── 2. Fixed-timestep simulation tick ─────────────────────────
            gameLoop.update(dt);

            // ── 3. Drain pending one-shot actions (ship spawns, etc.) ─────
            // These are posted by callbacks to avoid doing heavy work
            // inside a GLFW callback. Runs on the main thread where it
            // is safe to call world.setBlock().
            Runnable action;
            while ((action = pendingActions.poll()) != null) {
                action.run();
            }

            // ── 4. Camera sync ────────────────────────────────────────────
            // NOTE: player.isRiding() and getRidingShip() are stubbed until
            // Phase 3 builds ShipEntity. The else-branch handles all cases now.
            if (player.isRiding()) {
                // Phase 3: ShipEntity camera sync goes here
                // minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
                // ...
                camera.setPosition(player.position.x, player.position.y + 1.6f, player.position.z);
            } else {
                camera.setPosition(player.position.x, player.position.y + 1.6f, player.position.z);
            }

            // ── 5. Chunk streaming ────────────────────────────────────────
            int cx = (int) Math.floor(player.position.x / 16.0);
            int cz = (int) Math.floor(player.position.z / 16.0);
            world.update(cx, cz, dt);

            // ── 6. Render ─────────────────────────────────────────────────
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

            updateAtmosphere(dt);

            shaderProgram.setUniform("torchPos", player.position);
            shaderProgram.setUniform("torchStrength",
                    player.inventory.hasTorchEquipped() ? 1.0f : 0.0f);

            boolean isUnderwater = world.getBlock(
                    (int) Math.floor(player.position.x),
                    (int) Math.floor(player.position.y + 1.6f),
                    (int) Math.floor(player.position.z)) == Block.WATER;

            shaderProgram.setUniform("colorTint",
                    isUnderwater ? new Vector4f(0.4f, 0.6f, 1.0f, 1.0f) : whiteTint);

            world.render(shaderProgram, player.position);

            entityRenderer.render(entityManager, shaderProgram, textures, viewMatrix);
            particleManager.render(shaderProgram, viewMatrix, projectionMatrix);

            glEnable(GL_BLEND);
            weatherRenderer.render(player.position, world.getWeather(), shaderProgram, dt);

            glDisable(GL_DEPTH_TEST);
            renderHand(shaderProgram, dt);
            glEnable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);

            // Item pickup
            if (!craftingOpen) {
                minicraft.entity.Entity item = entityManager.getNearestOfType(
                        camera.getPosition().x, camera.getPosition().y, camera.getPosition().z,
                        2.0f, minicraft.entity.EntityType.ITEM);
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

        // Place schematic blocks
        spawnShipFromSchematic(def, wx, wy, wz);

        // Teleport player to the bridge
        org.joml.Vector3i bridge = def.schematic.bridgeLocalPos;
        player.setPosition(
                wx + bridge.x + 0.5f,
                wy + bridge.y + 1.1f,
                wz + bridge.z + 0.5f);

        System.out.println("LOGISTICS NETWORK: " + def.displayName + " DEPLOYED.");

        // Phase 3 will add:
        // entityManager.spawn(new ShipEntity(def, wx, wy, wz));
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

        float speed = 8.0f;
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
            // Phase 3: ship input routing goes here once ShipEntity is built
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
        float x = (float) mx[0];
        float y = (float) my[0];

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

    private void handleInventoryClick() {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);

        int[] winW = new int[1], winH = new int[1];
        glfwGetWindowSize(window, winW, winH);
        float x = (float) mx[0] * ((float) framebufferW / Math.max(1, winW[0]));
        float y = (float) my[0] * ((float) framebufferH / Math.max(1, winH[0]));

        boolean isShift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;

        float panelW = 800;
        float panelH = 500;
        float sx = (framebufferW - panelW) / 2f;
        float sy = (framebufferH - panelH) / 2f;
        float slotSize = 64;
        float gap = 15;

        float invX = sx + 240;
        for (int i = 0; i < 27; i++) {
            float slotX = invX + 40 + (i % 9) * (slotSize + gap);
            float slotY = sy + 100 + (i / 9) * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= slotY && y <= slotY + slotSize) {
                if (isShift)
                    player.inventory.quickMove(i, false);
                else
                    player.inventory.clickSlot(i, false);
                return;
            }
        }

        float hbX = (framebufferW - ((slotSize + gap) * 9)) / 2f;
        float hbY = sy + panelH - 80;
        for (int i = 0; i < 9; i++) {
            float slotX = hbX + i * (slotSize + gap);
            if (x >= slotX && x <= slotX + slotSize && y >= hbY && y <= hbY + slotSize) {
                if (isShift)
                    player.inventory.quickMove(i, true);
                else
                    player.inventory.clickSlot(i, true);
                return;
            }
        }
    }

    private void handleChestClick() {
        if (activeChest == null)
            return;
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);

        int[] winW = new int[1], winH = new int[1];
        glfwGetWindowSize(window, winW, winH);
        float x = (float) mx[0] * ((float) framebufferW / Math.max(1, winW[0]));
        float y = (float) my[0] * ((float) framebufferH / Math.max(1, winH[0]));

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
        boolean mouseLeftDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : craftingManager.getRecipes())
            if (r.getCategory() == activeCategory)
                filtered.add(r);

        if (mouseLeftDown && !prevMouseLeftDown) {
            double[] mx = new double[1], my = new double[1];
            glfwGetCursorPos(window, mx, my);
            float x = (float) mx[0];
            float y = (float) my[0];

            float menuW = 600, menuH = 500;
            float startX = (WIN_W - menuW) / 2f;
            float startY = (WIN_H - menuH) / 2f;

            Recipe.Category[] cats = Recipe.Category.values();
            float tabW = menuW / cats.length;
            if (y >= startY && y <= startY + 40)
                for (int i = 0; i < cats.length; i++)
                    if (x >= startX + i * tabW && x <= startX + (i + 1) * tabW) {
                        activeCategory = cats[i];
                        recipeIndex = 0;
                        break;
                    }

            for (int i = 0; i < filtered.size(); i++) {
                float ry = startY + 60 + i * 40;
                if (x >= startX + 20 && x <= startX + 380 && y >= ry && y <= ry + 35) {
                    recipeIndex = i;
                    break;
                }
            }

            if (recipeIndex >= 0 && recipeIndex < filtered.size()) {
                float btnX = startX + menuW - 180;
                float btnY = startY + menuH - 70;
                if (x >= btnX && x <= btnX + 160 && y >= btnY && y <= btnY + 50)
                    craftingManager.craft(filtered.get(recipeIndex), player.inventory);
            }
        }

        boolean isUp = glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS;
        boolean isDown = glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS;
        boolean isEnter = glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS;
        if (isUp && !prevUp)
            recipeIndex = (recipeIndex - 1 + filtered.size()) % Math.max(1, filtered.size());
        if (isDown && !prevDown)
            recipeIndex = (recipeIndex + 1) % Math.max(1, filtered.size());
        if (isEnter && !prevEnter && recipeIndex < filtered.size())
            craftingManager.craft(filtered.get(recipeIndex), player.inventory);

        prevUp = isUp;
        prevDown = isDown;
        prevEnter = isEnter;
        prevMouseLeftDown = mouseLeftDown;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Combat + placement
    // ─────────────────────────────────────────────────────────────────────

    private void handlePlayerAttack(float dt) {
        if (!prevMouseLeftDown)
            player.meleeAttack(entityManager);

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
            float efficiency = 0.5f;
            int toolLevel = 0;
            if (held instanceof ToolItem) {
                ToolItem t = (ToolItem) held;
                if (t.getToolType() == ToolItem.ToolType.PICKAXE) {
                    efficiency = t.getEfficiency();
                    toolLevel = t.getHarvestLevel();
                }
            }

            if (toolLevel < b.requiredHarvestLevel)
                miningProgress = 0f;
            else
                miningProgress += (dt * efficiency) / b.hardness;

            player.miningProgress = miningProgress;

            if (miningProgress >= 1.0f) {
                world.setBlock(gx, gy, gz, Block.AIR);
                miningProgress = player.miningProgress = 0f;
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

            if (targeted == Block.CHEST || targeted == Block.CRAFTING_TABLE
                    || targeted == Block.FURNACE || targeted == Block.SHIP_CONSOLE) {
                if (targeted == Block.CHEST) {
                    activeChest = world.getContainer(gx, gy, gz);
                    chestOpen = true;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                } else if (targeted == Block.CRAFTING_TABLE) {
                    craftingOpen = true;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                } else if (targeted == Block.SHIP_CONSOLE) {
                    shipConsoleOpen = true;
                    drydockX = gx;
                    drydockY = gy - 1;
                    drydockZ = gz;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
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
        if (heldItem.getName().contains("Pickaxe"))
            model = ModelRegistry.getModel("pickaxe_wooden");
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
        h_model.rotate((float) Math.toRadians(160), new Vector3f(0, 1, 0));
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

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}