package minicraft.renderer;

import minicraft.entity.Player;
import minicraft.math.Matrix4f;
import minicraft.math.Vector4f;
import minicraft.world.Block;
import minicraft.item.Recipe;
import minicraft.item.Item;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.awt.*;
import minicraft.ship.ShipRegistry;
import minicraft.ship.ShipDefinition;

import static org.lwjgl.opengl.GL11.*;

public class UIRenderer {

    // ── Layout constants — single source of truth, nothing overlaps ──────────
    //
    // HOTBAR: bottom 14px margin, 42px slots → occupies y = [H-56 .. H-14]
    // XP BAR: 10px tall, 4px gap above hotbar → occupies y = [H-70 .. H-60]
    // XP LABEL: 12px above XP bar → y ≈ H-82
    // STAT PANEL (HP/Hunger): sits above XP label with 6px gap → base at H-124
    // COORD PANEL: top-left, always 14px from edges
    // TEMP HUD: top-right, always 14px from edges
    // OFFHAND: sits inside stat panel, left of bars — never floats separately
    //
    private static final float HOTBAR_MARGIN_BOT = 14f;
    private static final float HOTBAR_SLOT_SIZE = 42f;
    private static final float HOTBAR_GAP = 6f;
    private static final int HOTBAR_SLOTS = 9;
    private static final float HOTBAR_H = HOTBAR_SLOT_SIZE;
    // bottom edge of hotbar = H - HOTBAR_MARGIN_BOT
    // top edge of hotbar = H - HOTBAR_MARGIN_BOT - HOTBAR_H

    private static final float XP_BAR_H = 10f;
    private static final float XP_TO_HOTBAR_GAP = 4f;
    // top of XP bar = hotbar_top - XP_BAR_H - XP_TO_HOTBAR_GAP

    private static final float STAT_BAR_W = 210f;
    private static final float STAT_BAR_H = 12f;
    private static final float STAT_BETWEEN_GAP = 10f;
    // two bars + gap = STAT_BAR_H*2 + STAT_BETWEEN_GAP = 34px
    private static final float STAT_PANEL_PAD = 8f;
    private static final float STAT_PANEL_H = STAT_BAR_H * 2 + STAT_BETWEEN_GAP + STAT_PANEL_PAD * 2 + 6f;
    // stat panel sits directly above XP label (12px) with 6px gap
    private static final float STAT_TO_XP_GAP = 6f;

    // ── Meshes & fonts ───────────────────────────────────────────────────────
    private Mesh quadMesh;
    private Mesh textQuadMesh;
    private FontTexture fontTexture;
    private Texture whiteTexture;
    private final TextureRegistry textures;

    // ── Color palette ────────────────────────────────────────────────────────
    private final Vector4f healthColor = new Vector4f(0.88f, 0.12f, 0.12f, 1.0f);
    private final Vector4f healthColor2 = new Vector4f(0.55f, 0.04f, 0.04f, 1.0f);
    private final Vector4f hungerColor = new Vector4f(0.95f, 0.52f, 0.00f, 1.0f);
    private final Vector4f hungerColor2 = new Vector4f(0.65f, 0.30f, 0.00f, 1.0f);
    private final Vector4f glassBgColor = new Vector4f(0.00f, 0.00f, 0.00f, 0.55f);
    private final Vector4f glassBorderColor = new Vector4f(0.17f, 0.72f, 0.79f, 0.45f);
    private final Vector4f highlightColor = new Vector4f(0.17f, 0.72f, 0.79f, 0.22f);
    private final Vector4f textColor = new Vector4f(1.00f, 1.00f, 1.00f, 1.0f);
    private final Vector4f crosshairColor = new Vector4f(1.00f, 1.00f, 1.00f, 0.85f);
    private final Vector4f tactOrange = new Vector4f(0.95f, 0.48f, 0.00f, 1.0f);
    private final Vector4f tactBlue = new Vector4f(0.12f, 0.63f, 1.00f, 1.0f);
    private final Vector4f tactGreen = new Vector4f(0.00f, 0.88f, 0.36f, 1.0f);
    private final Vector4f tactDim = new Vector4f(0.04f, 0.04f, 0.08f, 0.55f);
    private final Vector4f barShine = new Vector4f(1.00f, 1.00f, 1.00f, 0.18f);
    private final Vector4f slotHoverColor = new Vector4f(1.00f, 1.00f, 1.00f, 0.18f);

    // ── Constructor ──────────────────────────────────────────────────────────
    public UIRenderer(TextureRegistry textures) {
        this.textures = textures;
        float[] positions = { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0 };
        float[] uvs = { 0, 1, 1, 1, 1, 0, 0, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        whiteTexture = textures.get("snow");
        quadMesh = new Mesh(positions, uvs, indices, whiteTexture);
        textQuadMesh = new Mesh(positions, uvs, indices, null);

        try {
            Font font = new Font(Font.MONOSPACED, Font.BOLD, 28);
            fontTexture = new FontTexture(font, "ISO-8859-1");
            textQuadMesh.setTexture(fontTexture.getTexture());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Main render entry — menus are mutually exclusive, HUD only when playing
    // ══════════════════════════════════════════════════════════════════════════
    public void render(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        renderWeather(shader, width, height, main.getWorld().getWeather());

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -1, 1);
        shader.setUniform("projectionMatrix", ortho);
        shader.setUniform("viewMatrix", new Matrix4f().identity());
        shader.setUniform("useLighting", 0.0f);

        // Exactly one branch executes — screens are mutually exclusive
        if (main.inventoryOpen) {
            renderInventoryScreen(player, shader, width, height, main);
        } else if (main.chestOpen) {
            renderChestScreen(player, shader, width, height, main);
        } else if (main.craftingOpen) {
            renderCraftingMenu(player, shader, width, height, main);
        } else if (main.shipConsoleOpen) {
            renderShipConsoleScreen(player, shader, width, height, main);
        } else if (main.furnaceOpen || main.cookerOpen) {
            renderFacilityScreen(player, shader, width, height, main);
        } else if (player.isRiding()) {
            renderPilotHUD(player, shader, width, height);
        } else {
            // Pure gameplay HUD — no menus
            drawCrosshair(shader, width / 2f, height / 2f);
            renderPlayHUD(player, shader, width, height, main);
        }

        // ── Global cursor overlay (menus only) ──────────────────────────────
        // The cursor item is drawn ONCE here. Individual screen renderers must
        // NOT draw it themselves to avoid double-rendering.
        if (main.inventoryOpen || main.chestOpen || main.craftingOpen
                || main.shipConsoleOpen || main.furnaceOpen || main.cookerOpen) {

            float[] mouse = getScaledMouse(main, width, height);
            float mouseX = mouse[0], mouseY = mouse[1];

            minicraft.item.ItemStack cursor = player.inventory.getCursorStack();
            if (cursor != null && !cursor.isEmpty()) {
                float iconSize = 48f;
                // Centre icon on cursor tip
                drawItemIcon(shader, cursor.getItem(), mouseX - iconSize / 2f, mouseY - iconSize / 2f, iconSize);
                if (cursor.getCount() > 1)
                    drawText(shader, String.valueOf(cursor.getCount()),
                            mouseX + iconSize / 2f - 14, mouseY + iconSize / 2f - 8, 0.65f);
            }

            // Pointer crosshair
            drawRectInternal(shader, mouseX - 1, mouseY - 8, 2, 16, new Vector4f(1, 1, 1, 0.9f));
            drawRectInternal(shader, mouseX - 8, mouseY - 1, 16, 2, new Vector4f(1, 1, 1, 0.9f));
        }

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Play HUD — all elements laid out from the layout constants above
    // ══════════════════════════════════════════════════════════════════════════
    private void renderPlayHUD(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        // Pre-compute Y positions from bottom up so nothing overlaps
        float hotbarTop = height - HOTBAR_MARGIN_BOT - HOTBAR_H;
        float xpBarTop = hotbarTop - XP_BAR_H - XP_TO_HOTBAR_GAP;
        float xpLabelTop = xpBarTop - 13f;
        float statBottom = xpLabelTop - STAT_TO_XP_GAP;
        float statTop = statBottom - STAT_PANEL_H;

        float marginLeft = 22f;

        // ── 1. Hotbar ────────────────────────────────────────────────────────
        renderHotbar(player, shader, width, height, hotbarTop);

        // ── 2. XP bar (centred, above hotbar) ────────────────────────────────
        renderXPBar(player, shader, width, xpBarTop, xpLabelTop);

        // ── 3. Stat panel (HP + Hunger, bottom-left, above XP area) ──────────
        float panelX = marginLeft - STAT_PANEL_PAD;
        float panelW = STAT_BAR_W + 40f + STAT_PANEL_PAD * 2; // icon col + bar + padding
        drawTacticalFrame(shader, panelX, statTop, panelW, STAT_PANEL_H);

        // Hunger bar (top row inside panel)
        float barX = marginLeft + 32f; // leave room for icon
        float hungBarY = statTop + STAT_PANEL_PAD + 4f;
        drawPremiumBar(shader, barX, hungBarY, STAT_BAR_W, STAT_BAR_H,
                player.hunger / player.maxHunger, hungerColor, hungerColor2, "F");

        // Health bar (below hunger)
        float hpBarY = hungBarY + STAT_BAR_H + STAT_BETWEEN_GAP;
        drawPremiumBar(shader, barX, hpBarY, STAT_BAR_W, STAT_BAR_H,
                player.getHealth() / player.getMaxHealth(), healthColor, healthColor2, "V");

        // Offhand slot — anchored inside the stat panel, right edge
        if (player.inventory.getOffhandItem() != null) {
            float ohX = panelX + panelW + 6f;
            float ohY = statTop + (STAT_PANEL_H - 36f) / 2f;
            drawRectInternal(shader, ohX, ohY, 36, 36, glassBgColor);
            drawItemIcon(shader, player.inventory.getOffhandItem(), ohX + 4, ohY + 4, 28);
        }

        // ── 4. Coordinates (top-left, fixed) ─────────────────────────────────
        float coordW = 188f, coordH = 68f;
        drawTacticalFrame(shader, 14, 14, coordW, coordH);
        drawText(shader, "POSITION DATA", 26, 26, 0.38f, tactBlue);
        drawText(shader,
                String.format("X:%-6d  Y:%-6d", (int) player.position.x, (int) player.position.y),
                26, 41, 0.45f);
        drawText(shader, String.format("Z:%-6d", (int) player.position.z), 26, 56, 0.45f);
        drawText(shader,
                main.getWorld().getBiome((int) player.position.x, (int) player.position.z).displayName.toUpperCase(),
                26, 70, 0.35f,
                new Vector4f(0.6f, 0.6f, 0.6f, 1f));

        // ── 5. Temperature (top-right, fixed) — stays clear of coord panel ───
        drawTemperatureHUD(player, shader, width, 20f);

        // ── 6. Damage vignette — only below 10% health, never when full ──────
        float healthPct = player.getHealth() / player.getMaxHealth();
        if (healthPct < 1.0f && player.damageFlashTimer > 0 && healthPct >= 0.10f) {
            // Brief hit-flash: visible only if not at full health, fades quickly
            drawDamageVignette(shader, width, height, player.damageFlashTimer * 0.5f);
        } else if (healthPct < 0.10f) {
            // Critical — persistent pulsing red border
            float pulse = 0.20f + 0.12f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 400.0));
            drawDamageVignette(shader, width, height, pulse);
        }
        // healthPct >= 1.0f → no vignette at all
    }

    // ── XP bar (helper) ──────────────────────────────────────────────────────
    private void renderXPBar(Player player, ShaderProgram shader, int width,
            float barTop, float labelTop) {
        float barW = 200f;
        float x = (width - barW) / 2f;
        float prog = Math.max(0, Math.min(1, player.xp / player.xpToNextLevel));

        drawText(shader, "LV " + player.level, x, labelTop, 0.40f, tactBlue);

        drawRectInternal(shader, x, barTop, barW, XP_BAR_H, new Vector4f(0, 0, 0, 0.65f));
        float fill = barW * prog;
        drawRectInternal(shader, x, barTop, fill, XP_BAR_H * 0.55f, tactBlue);
        drawRectInternal(shader, x, barTop + XP_BAR_H * 0.55f, fill, XP_BAR_H * 0.45f,
                new Vector4f(0.06f, 0.45f, 0.85f, 1.0f));
        drawRectInternal(shader, x, barTop, fill, 2f, barShine);
        drawRectInternal(shader, x, barTop, barW, 1f, glassBorderColor);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inventory screen — clean slot layout, hover feedback, NO duplicate cursor
    // ══════════════════════════════════════════════════════════════════════════
    private void renderInventoryScreen(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        // Darken world
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        // Panel dimensions — sized to fit 9×4 grid exactly
        final float SLOT = 58f; // slot size
        final float GAP = 8f; // gap between slots
        final float COLS = 9f;
        final float ROWS = 4f; // 3 main rows + 1 hotbar row

        float gridW = COLS * SLOT + (COLS - 1) * GAP;
        float panelW = gridW + 60f;
        float panelH = ROWS * SLOT + (ROWS - 1) * GAP + 110f; // header + row labels + footer
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        drawTacticalFrame(shader, sx, sy, panelW, panelH);
        drawText(shader, "INVENTORY", sx + 18, sy + 18, 0.85f, tactOrange);

        // Scaled mouse position
        float[] mouse = getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];

        // ── Main grid (3 rows × 9 cols) ──────────────────────────────────────
        float gridStartX = sx + (panelW - gridW) / 2f;
        float mainGridY = sy + 52f;
        minicraft.item.ItemStack[] mainInv = player.inventory.getMainInventory();

        drawText(shader, "MAIN", gridStartX, mainGridY - 14, 0.42f, highlightColor);

        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float sx2 = gridStartX + col * (SLOT + GAP);
            float sy2 = mainGridY + row * (SLOT + GAP);
            boolean hover = isHovered(mouseX, mouseY, sx2, sy2, SLOT, SLOT);
            drawSlot(shader, sx2, sy2, SLOT, mainInv[i], hover);
        }

        // ── Separator line ────────────────────────────────────────────────────
        float sepY = mainGridY + 3 * (SLOT + GAP) + 6f;
        drawRectInternal(shader, gridStartX, sepY, gridW, 1f, glassBorderColor);

        // ── Hotbar row (1 row × 9 cols, with selection indicator) ────────────
        float hotbarRowY = sepY + 14f;
        minicraft.item.ItemStack[] hotbar = player.inventory.getHotbar();
        int selIdx = player.inventory.getSelectedIndex();

        drawText(shader, "HOTBAR", gridStartX, hotbarRowY - 10, 0.42f, highlightColor);

        for (int i = 0; i < 9; i++) {
            float sx2 = gridStartX + i * (SLOT + GAP);
            boolean sel = (i == selIdx);
            boolean hover = isHovered(mouseX, mouseY, sx2, hotbarRowY, SLOT, SLOT);
            drawSlot(shader, sx2, hotbarRowY, SLOT, hotbar[i], hover);
            if (sel) {
                // Yellow top bar — matches the in-game hotbar indicator
                drawRectInternal(shader, sx2, hotbarRowY, SLOT, 3f,
                        new Vector4f(1.0f, 0.95f, 0.0f, 1.0f));
            }
        }

        // NOTE: cursor item is drawn by the global cursor block in render(),
        // so we deliberately do NOT draw it here.
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Chest screen — two grids, no cursor duplication
    // ══════════════════════════════════════════════════════════════════════════
    private void renderChestScreen(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        if (main.activeChest == null)
            return;

        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        final float SLOT = 56f;
        final float GAP = 8f;
        float gridW = 9 * SLOT + 8 * GAP;
        float panelW = gridW + 60f;
        float panelH = 580f;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        drawTacticalFrame(shader, sx, sy, panelW, panelH);
        drawText(shader, "LOOT CONTAINER", sx + 18, sy + 18, 0.85f, tactGreen);

        float[] mouse = getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];
        float gx = sx + (panelW - gridW) / 2f;

        // Chest contents (3×9)
        minicraft.item.ItemStack[] chestInv = main.activeChest.getMainInventory();
        float chestY = sy + 52f;
        drawText(shader, "CONTAINER", gx, chestY - 14, 0.42f, highlightColor);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP);
            float ty = chestY + row * (SLOT + GAP);
            drawSlot(shader, tx, ty, SLOT, chestInv[i], isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT));
        }

        // Separator
        float sepY = chestY + 3 * (SLOT + GAP) + 8f;
        drawRectInternal(shader, gx, sepY, gridW, 1f, glassBorderColor);

        // Player inventory (3×9)
        minicraft.item.ItemStack[] playerInv = player.inventory.getMainInventory();
        float playerY = sepY + 16f;
        drawText(shader, "YOUR BAG", gx, playerY - 14, 0.42f, highlightColor);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP);
            float ty = playerY + row * (SLOT + GAP);
            drawSlot(shader, tx, ty, SLOT, playerInv[i], isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT));
        }

        // Cursor drawn by global block in render()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Hotbar — in-game strip, uses layout constants
    // ══════════════════════════════════════════════════════════════════════════
    public void renderHotbar(Player player, ShaderProgram shader, int width, int height, float topY) {
        float stripW = HOTBAR_SLOTS * (HOTBAR_SLOT_SIZE + HOTBAR_GAP) - HOTBAR_GAP;
        float startX = (width - stripW) / 2f;

        // Background strip
        drawRectInternal(shader, startX - 4, topY - 4, stripW + 8, HOTBAR_SLOT_SIZE + 8, glassBgColor);
        drawRectInternal(shader, startX - 4, topY - 4, stripW + 8, 1f, glassBorderColor);

        minicraft.item.ItemStack[] hotbar = player.inventory.getHotbar();
        int sel = player.inventory.getSelectedIndex();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            float sx = startX + i * (HOTBAR_SLOT_SIZE + HOTBAR_GAP);

            if (i == sel) {
                drawRectInternal(shader, sx, topY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE,
                        new Vector4f(1, 1, 1, 0.28f));
                drawRectInternal(shader, sx, topY, HOTBAR_SLOT_SIZE, 3f,
                        new Vector4f(1.0f, 0.95f, 0.0f, 1.0f));
            } else {
                drawRectInternal(shader, sx, topY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE,
                        new Vector4f(0, 0, 0, 0.45f));
            }

            minicraft.item.ItemStack stack = hotbar[i];
            if (stack != null && !stack.isEmpty()) {
                drawItemIcon(shader, stack.getItem(), sx + 5, topY + 5, HOTBAR_SLOT_SIZE - 10);
                if (stack.getCount() > 1)
                    drawText(shader, String.valueOf(stack.getCount()),
                            sx + 3, topY + HOTBAR_SLOT_SIZE - 11, 0.52f);
            }
        }

        // Selected item name — centred above hotbar with 6px gap
        minicraft.item.Item selected = player.inventory.getSelectedItem();
        if (selected != null) {
            String name = selected.getName();
            float nameW = name.length() * 7.5f; // rough char width at scale 0.6
            drawText(shader, name, (width - nameW) / 2f, topY - 16f, 0.6f, textColor);
        }

        // Mining progress bar — below crosshair, never near hotbar
        if (player.miningProgress > 0) {
            float pbW = 90f, pbH = 5f;
            float px = (width - pbW) / 2f;
            float py = height / 2f + 22f;
            drawRectInternal(shader, px, py, pbW, pbH, new Vector4f(0, 0, 0, 0.55f));
            drawRectInternal(shader, px, py, pbW * player.miningProgress, pbH,
                    new Vector4f(0.38f, 0.95f, 0.38f, 0.95f));
        }
    }

    // Backwards-compatible overload used by other callers
    public void renderHotbar(Player player, ShaderProgram shader, int width, int height) {
        float hotbarTop = height - HOTBAR_MARGIN_BOT - HOTBAR_H;
        renderHotbar(player, shader, width, height, hotbarTop);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Slot rendering — hover highlight gives clear feedback during swapping
    // ══════════════════════════════════════════════════════════════════════════
    private void drawSlot(ShaderProgram shader, float x, float y, float size,
            minicraft.item.ItemStack stack, boolean hovered) {
        // Background
        Vector4f bg = hovered
                ? new Vector4f(0.18f, 0.18f, 0.22f, 0.90f)
                : new Vector4f(0.04f, 0.04f, 0.06f, 0.78f);
        drawRectInternal(shader, x, y, size, size, bg);

        // Thin border — brighter on hover
        Vector4f border = hovered
                ? new Vector4f(0.17f, 0.72f, 0.79f, 0.90f)
                : new Vector4f(0.17f, 0.72f, 0.79f, 0.22f);
        drawRectInternal(shader, x, y, size, 1f, border); // top
        drawRectInternal(shader, x, y + size - 1, size, 1f, border); // bottom
        drawRectInternal(shader, x, y, 1f, size, border); // left
        drawRectInternal(shader, x + size - 1, y, 1f, size, border); // right

        // Item icon + count
        if (stack != null && !stack.isEmpty()) {
            float pad = size * 0.13f;
            drawItemIcon(shader, stack.getItem(), x + pad, y + pad, size - pad * 2);
            if (stack.getCount() > 1)
                drawText(shader, String.valueOf(stack.getCount()),
                        x + 3, y + size - 12, 0.58f);
        }
    }

    // Overload for screens that don't track hover (facility, etc.)
    private void drawSlot(ShaderProgram shader, float x, float y, float size,
            minicraft.item.ItemStack stack) {
        drawSlot(shader, x, y, size, stack, false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Stat bar (HP / Hunger) — two-tone fill, shine, pulse at < 20%
    // ══════════════════════════════════════════════════════════════════════════
    private void drawPremiumBar(ShaderProgram shader,
            float x, float y, float w, float h, float fill,
            Vector4f c1, Vector4f c2, String icon) {
        float f = Math.max(0f, Math.min(1f, fill));

        // Pulse alpha at critical level
        float pulse = 1.0f;
        if (f < 0.20f)
            pulse = 0.72f + (float) Math.abs(Math.sin(System.currentTimeMillis() / 220.0)) * 0.38f;

        // Icon
        drawText(shader, icon, x - 22, y + 1, 0.70f, new Vector4f(c1.x, c1.y, c1.z, pulse));

        // Track
        drawRectInternal(shader, x - 1, y - 1, w + 2, h + 2, new Vector4f(0, 0, 0, 0.70f));

        // Two-tone fill
        float fillW = w * f;
        drawRectInternal(shader, x, y, fillW, h * 0.55f, c1);
        drawRectInternal(shader, x, y + h * 0.55f, fillW, h * 0.45f, c2);

        // Shine stripe
        drawRectInternal(shader, x, y, fillW, 2f, barShine);

        // Top border
        drawRectInternal(shader, x - 1, y - 1, w + 2, 1f, glassBorderColor);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Temperature HUD — top-right, width fixed so it never overlaps coords
    // ══════════════════════════════════════════════════════════════════════════
    private void drawTemperatureHUD(Player player, ShaderProgram shader, int screenWidth, float margin) {
        float panelW = 150f, panelH = 48f;
        float x = screenWidth - margin - panelW;
        float y = margin;

        drawTacticalFrame(shader, x, y, panelW, panelH);

        String tempText = String.format("%.1f\u00b0C", player.temperature);
        drawText(shader, tempText, x + 10, y + 14, 0.85f, textColor);

        Vector4f stateColor;
        String state = player.tempState;
        if (state.contains("Hypothermia"))
            stateColor = new Vector4f(0.75f, 0.0f, 1.0f, 1.0f);
        else if (state.equals("Cold"))
            stateColor = tactBlue;
        else if (state.equals("Too Warm"))
            stateColor = healthColor;
        else if (state.equals("Warm"))
            stateColor = tactOrange;
        else
            stateColor = tactGreen;

        drawText(shader, state.toUpperCase(), x + 10, y + 33, 0.38f, stateColor);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Crafting menu
    // ══════════════════════════════════════════════════════════════════════════
    private void renderCraftingMenu(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.85f));

        float menuW = 620f, menuH = 520f;
        float startX = (width - menuW) / 2f;
        float startY = (height - menuH) / 2f;

        drawRectInternal(shader, startX, startY, menuW, menuH, textColor, "crafting_menu_bg");
        drawRectInternal(shader, startX, startY, menuW, 2f, glassBorderColor);

        // Tabs
        Recipe.Category[] cats = Recipe.Category.values();
        float tabW = (menuW - 40f) / cats.length;
        for (int i = 0; i < cats.length; i++) {
            float tx = startX + 20 + i * tabW;
            boolean active = (main.activeCategory == cats[i]);
            drawRectInternal(shader, tx, startY + 10, tabW - 4, 34,
                    active ? new Vector4f(1.2f, 1.2f, 1.2f, 1f) : new Vector4f(0.78f, 0.78f, 0.78f, 1f),
                    "button_wood");
            if (active)
                drawRectInternal(shader, tx, startY + 42, tabW - 4, 2, tactOrange);
            drawText(shader, cats[i].name(),
                    tx + (tabW / 2f) - (cats[i].name().length() * 3.5f),
                    startY + 21, 0.65f,
                    active ? textColor : new Vector4f(0, 0, 0, 0.8f));
        }

        // Recipe list
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : main.craftingManager.getRecipes())
            if (r.getCategory() == main.activeCategory)
                filtered.add(r);

        float listX = startX + 35;
        int maxItems = 9;

        for (int i = 0; i < Math.min(filtered.size() - main.recipeScrollOffset, maxItems); i++) {
            int actualIndex = i + main.recipeScrollOffset;
            Recipe r = filtered.get(actualIndex);
            float ry = startY + 70 + i * 40f;
            boolean selected = (actualIndex == main.recipeIndex);
            if (selected) {
                drawRectInternal(shader, listX - 10, ry - 2, 350, 38, highlightColor);
                drawRectInternal(shader, listX - 12, ry - 2, 4, 38, tactOrange);
            }
            drawText(shader, r.getName(), listX + 15, ry + 8, 0.92f,
                    selected ? tactOrange : textColor);
        }

        // Scrollbar
        if (filtered.size() > maxItems) {
            float sbX = startX + 382, sbY = startY + 70;
            float sbH = maxItems * 40f - 5;
            drawRectInternal(shader, sbX, sbY, 4, sbH, tactDim);
            float thumbH = (sbH / filtered.size()) * maxItems;
            float thumbY = sbY + (sbH / filtered.size()) * main.recipeScrollOffset;
            drawRectInternal(shader, sbX, thumbY, 4, thumbH, tactBlue);
        }

        // Detail pane
        if (main.recipeIndex >= 0 && main.recipeIndex < filtered.size()) {
            Recipe sel = filtered.get(main.recipeIndex);
            float detailX = startX + 400, detailY = startY + 70;

            drawRectInternal(shader, detailX, detailY, 180, menuH - 145,
                    new Vector4f(0, 0, 0, 0.40f));
            drawText(shader, "REQUIRED:", detailX + 10, detailY + 10, 0.65f, tactOrange);

            boolean canCraft = true;
            int k = 0;
            for (Map.Entry<Item, Integer> entry : sel.getIngredients().entrySet()) {
                int owned = player.inventory.getCount(entry.getKey());
                boolean sufficient = owned >= entry.getValue();
                if (!sufficient)
                    canCraft = false;
                Vector4f col = sufficient ? tactGreen : new Vector4f(1f, 0.4f, 0.4f, 1f);
                drawText(shader, entry.getKey().getName(), detailX + 12, detailY + 40 + k * 44, 0.56f, col);
                drawText(shader, owned + " / " + entry.getValue(), detailX + 12, detailY + 57 + k * 44, 0.65f, col);
                k++;
            }

            float btnX = startX + menuW - 192, btnY = startY + menuH - 68;
            drawRectInternal(shader, btnX, btnY, 172, 46,
                    canCraft ? textColor : new Vector4f(0.5f, 0.5f, 0.5f, 0.8f), "button_wood");
            drawText(shader, "FORGE ITEM", btnX + 36, btnY + 15, 0.80f,
                    canCraft ? new Vector4f(0, 0, 0, 1) : new Vector4f(0.25f, 0.25f, 0.25f, 0.8f));
        }

        // Cursor drawn by global block in render()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Facility screen (furnace / cooker)
    // ══════════════════════════════════════════════════════════════════════════
    private void renderFacilityScreen(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        if (main.activeFacility == null)
            return;

        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        float panelW = 680f, panelH = 480f;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        String title = main.furnaceOpen ? "INDUSTRIAL SMELTER" : "HIGH-EFFICIENCY COOKER";
        Vector4f titleCol = main.furnaceOpen ? tactOrange : tactBlue;

        drawTacticalFrame(shader, sx, sy, panelW, panelH);
        drawText(shader, title, sx + 22, sy + 22, 1.0f, titleCol);

        float cx = sx + panelW / 2f;
        float cy = sy + 180f;
        float slotSize = 72f;

        // Input
        drawSlot(shader, cx - 170, cy - 36, slotSize, main.activeFacility.getSlot(0));
        drawText(shader, "INPUT", cx - 170, cy - 52, 0.52f, highlightColor);

        // Fuel
        drawSlot(shader, cx - 36, cy + 52, slotSize, main.activeFacility.getSlot(1));
        drawText(shader, "FUEL", cx - 36, cy + 36, 0.52f, highlightColor);

        // Fuel ratio bar
        float fuelRatio = main.activeFacility.getFuelRatio();
        drawRectInternal(shader, cx - 36, cy + 132, slotSize * fuelRatio, 5,
                new Vector4f(1, 0.5f, 0, 0.9f));

        // Output
        drawSlot(shader, cx + 98, cy - 36, slotSize, main.activeFacility.getSlot(2));
        drawText(shader, "OUTPUT", cx + 98, cy - 52, 0.52f, highlightColor);

        // Progress arrow
        float prog = main.activeFacility.getProgress();
        drawRectInternal(shader, cx - 66, cy, 130, 8, new Vector4f(1, 1, 1, 0.10f));
        drawRectInternal(shader, cx - 66, cy, 130 * prog, 8, titleCol);
        if (prog > 0)
            drawText(shader, (int) (prog * 100) + "%", cx - 14, cy + 18, 0.58f, textColor);

        // Player inventory (3×9 at bottom of panel)
        final float ISLOT = 54f, IGAP = 6f;
        float gridW = 9 * ISLOT + 8 * IGAP;
        float invStartX = sx + (panelW - gridW) / 2f;
        float invStartY = sy + 310f;

        drawText(shader, "INVENTORY", invStartX, invStartY - 16, 0.52f, highlightColor);
        minicraft.item.ItemStack[] pInv = player.inventory.getMainInventory();
        for (int i = 0; i < 27; i++) {
            int row = i / 9, col = i % 9;
            drawSlot(shader, invStartX + col * (ISLOT + IGAP),
                    invStartY + row * (ISLOT + IGAP), ISLOT, pInv[i]);
        }

        // Cursor drawn by global block in render()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ship console
    // ══════════════════════════════════════════════════════════════════════════
    private void renderShipConsoleScreen(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0.04f, 0.09f, 0.88f));

        float panelW = 900f, panelH = 600f;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        drawRectInternal(shader, sx, sy, panelW, panelH, glassBgColor);
        drawRectInternal(shader, sx, sy, panelW, 2, new Vector4f(0, 1, 1, 0.5f));
        drawText(shader, "UNSC FLEET LOGISTICS NETWORK", sx + 28, sy + 28, 1.2f,
                new Vector4f(0, 1, 1, 1));
        drawText(shader, "DRYDOCK STATUS: STANDBY", sx + 28, sy + 58, 0.72f, highlightColor);

        List<ShipDefinition> ships = ShipRegistry.getInstance().getAll();

        float btnW = 250f, btnH = 380f, gap = 28f;
        int count = Math.min(3, ships.size());
        float startX = sx + (panelW - (count * btnW + (count - 1) * gap)) / 2f;
        float startY = sy + 100f;

        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        float mouseX = (float) mx[0], mouseY = (float) my[0];

        for (int i = 0; i < ships.size(); i++) {
            ShipDefinition def = ships.get(i);
            float bx = startX + i * (btnW + gap);
            boolean hover = mouseX >= bx && mouseX <= bx + btnW
                    && mouseY >= startY && mouseY <= startY + btnH;

            drawRectInternal(shader, bx, startY, btnW, btnH,
                    hover ? new Vector4f(0, 0.38f, 0.40f, 0.82f)
                            : new Vector4f(0, 0.09f, 0.18f, 0.62f));
            drawRectInternal(shader, bx, startY, btnW, 2,
                    new Vector4f(0, 1, 1, hover ? 1f : 0.28f));

            drawText(shader, def.displayName.toUpperCase(), bx + 14, startY + 18, 0.75f, textColor);

            drawRectInternal(shader, bx + 18, startY + 52, btnW - 36, btnW - 36,
                    new Vector4f(0, 0, 0, 0.5f));
            drawText(shader, "SCHEMATIC PREVIEW", bx + 32, startY + 148, 0.52f,
                    new Vector4f(0.4f, 0.4f, 0.5f, 1));

            drawText(shader, "CLASS: " + def.shipClass.displayName, bx + 14, startY + btnW + 22, 0.60f,
                    new Vector4f(0.8f, 0.8f, 1f, 1f));
            drawText(shader, "BLOCKS: " + def.getBlockCount(), bx + 14, startY + btnW + 44, 0.60f,
                    new Vector4f(0.8f, 0.8f, 1f, 1f));
            drawText(shader, def.getDimensionsString(), bx + 14, startY + btnW + 66, 0.60f,
                    new Vector4f(0.8f, 0.8f, 1f, 1f));

            String desc = def.description.length() > 55
                    ? def.description.substring(0, 52) + "..."
                    : def.description;
            drawText(shader, desc, bx + 14, startY + btnW + 92, 0.50f,
                    new Vector4f(0.58f, 0.58f, 0.58f, 1f));
        }

        drawRectInternal(shader, mouseX - 6, mouseY - 6, 12, 12, new Vector4f(0, 1, 1, 1));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Pilot HUD
    // ══════════════════════════════════════════════════════════════════════════
    private void renderPilotHUD(Player player, ShaderProgram shader, int width, int height) {
        minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
        if (ship == null)
            return;

        // Top header bar
        drawRectInternal(shader, 0, 0, width, 58, tactDim);
        drawRectInternal(shader, 0, 58, width, 2, glassBorderColor);
        drawText(shader, "AEGIS-7 TACTICAL HUB", 38, 14, 1.1f, tactOrange);
        drawText(shader, "VESSEL: " + ship.getDefinition().displayName.toUpperCase()
                + "  //  LINK STATUS: NOMINAL", 38, 40, 0.46f, highlightColor);
        drawText(shader, String.valueOf(System.currentTimeMillis() % 1000000),
                width - 200, 20, 0.75f, textColor);

        // Left telemetry
        float lx = 38, ly = 110;
        drawText(shader, "VITAL TELEMETRY", lx, ly, 0.55f, tactOrange);
        ly += 28;
        drawTacticalBar(shader, lx, ly, 215, 9, ship.getHealth() / ship.getMaxHealth(), tactGreen, "HULL INTEGRITY");
        ly += 42;
        drawTacticalBar(shader, lx, ly, 215, 9, ship.getShieldPct(), tactBlue, "SHIELD CAPACITY");
        ly += 42;
        drawTacticalBar(shader, lx, ly, 215, 9, ship.getEnergyPct(),
                new Vector4f(0.9f, 0.9f, 0, 1), "POWER CORE");
        ly += 42;
        drawTacticalBar(shader, lx, ly, 215, 9, ship.getFuelPct(), tactOrange, "FUEL RESERVES");

        // Centre radar
        float radarSize = 320f;
        float rx = (width - radarSize) / 2f, ry = (height - radarSize) / 2f;
        drawRectInternal(shader, rx, ry, radarSize, radarSize, new Vector4f(1, 1, 1, 0.04f));
        drawRectInternal(shader, rx + radarSize / 2f - 1, ry, 2, radarSize, glassBorderColor);
        drawRectInternal(shader, rx, ry + radarSize / 2f - 1, radarSize, 2, glassBorderColor);
        float anim = (float) Math.sin(System.currentTimeMillis() / 200.0) * 8f;
        drawCrosshair(shader, width / 2f, height / 2f);
        drawRectInternal(shader, width / 2f - 22 - anim / 2f, height / 2f - 22 - anim / 2f,
                44 + anim, 44 + anim, new Vector4f(1, 0.5f, 0, 0.18f));
        drawText(shader, "VECTOR: " + (int) ship.yaw + "\u00b0", rx, ry + radarSize + 18, 0.65f, tactOrange);
        drawText(shader, "MAG: " + String.format("%.1f", ship.getVelocityKms()) + " KM/S",
                rx + 145, ry + radarSize + 18, 0.65f, textColor);

        // Right weapons matrix
        float wx = width - 258, wy = 110;
        drawText(shader, "WEAPONS MATRIX", wx, wy, 0.55f, tactOrange);
        wy += 28;
        String[] weapons = { "MAC CANNON", "ARCHER PODS", "PULSE LASER" };
        minicraft.entity.ship.ShipEntity.WeaponSystem active = ship.getActiveWeapon();
        for (int i = 0; i < weapons.length; i++) {
            boolean sel = (active.ordinal() == i);
            drawRectInternal(shader, wx, wy + i * 48, 218, 38,
                    sel ? new Vector4f(1, 0.5f, 0, 0.2f) : tactDim);
            drawRectInternal(shader, wx, wy + i * 48, 2, 38,
                    sel ? tactOrange : highlightColor);
            drawText(shader, weapons[i], wx + 14, wy + 11 + i * 48, 0.65f,
                    sel ? textColor : highlightColor);
        }

        // Bottom data fields
        float bfY = height - 115, bfGap = 155;
        float bfX = (width - bfGap * 3) / 2f;
        drawTacticalField(shader, bfX, bfY, "X-COORD", String.valueOf((int) ship.position.x));
        drawTacticalField(shader, bfX + bfGap, bfY, "Y-COORD", String.valueOf((int) ship.position.y));
        drawTacticalField(shader, bfX + bfGap * 2, bfY, "Z-COORD", String.valueOf((int) ship.position.z));

        // Ticker
        drawRectInternal(shader, 0, height - 38, width, 38, tactDim);
        long tickerTime = System.currentTimeMillis() / 50;
        float tickerX = width - (tickerTime % (width + 1000));
        drawText(shader,
                "[24:12:08] SYSTEM NOMINAL // NEURAL LINK STABLE // SHIELDS AT 100% // READY FOR SLIPSPACE JUMP",
                tickerX, height - 25, 0.55f, new Vector4f(0.48f, 0.48f, 0.48f, 1));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shared helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns mouse position scaled to the render resolution. */
    private float[] getScaledMouse(minicraft.Main main, int renderW, int renderH) {
        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        int[] winW = new int[1], winH = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(main.getWindow(), winW, winH);
        float scaleX = winW[0] > 0 ? (float) renderW / winW[0] : 1f;
        float scaleY = winH[0] > 0 ? (float) renderH / winH[0] : 1f;
        return new float[] { (float) mx[0] * scaleX, (float) my[0] * scaleY };
    }

    /** Returns true if (mx, my) is inside the rectangle. */
    private boolean isHovered(float mx, float my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    private void drawTacticalFrame(ShaderProgram shader, float x, float y, float w, float h) {
        drawRectInternal(shader, x, y, w, h, glassBgColor);
        // Thin outline
        drawRectInternal(shader, x, y, w, 1, glassBorderColor);
        drawRectInternal(shader, x, y + h - 1, w, 1, glassBorderColor);
        drawRectInternal(shader, x, y, 1, h, glassBorderColor);
        drawRectInternal(shader, x + w - 1, y, 1, h, glassBorderColor);
        // Corner accents (subtler: 8px length, 2px thick)
        float cL = 8f, cT = 2f;
        drawRectInternal(shader, x, y, cL, cT, tactBlue);
        drawRectInternal(shader, x, y, cT, cL, tactBlue);
        drawRectInternal(shader, x + w - cL, y, cL, cT, tactBlue);
        drawRectInternal(shader, x + w - cT, y, cT, cL, tactBlue);
        drawRectInternal(shader, x, y + h - cT, cL, cT, tactBlue);
        drawRectInternal(shader, x, y + h - cL, cT, cL, tactBlue);
        drawRectInternal(shader, x + w - cL, y + h - cT, cL, cT, tactBlue);
        drawRectInternal(shader, x + w - cT, y + h - cL, cT, cL, tactBlue);
    }

    private void drawTacticalBar(ShaderProgram shader, float x, float y, float w, float h,
            float fill, Vector4f color, String label) {
        drawText(shader, label, x, y - 11, 0.40f, highlightColor);
        drawRectInternal(shader, x, y, w, h, new Vector4f(0.04f, 0.04f, 0.04f, 0.82f));
        drawRectInternal(shader, x, y, w * Math.max(0, Math.min(1, fill)), h, color);
        drawRectInternal(shader, x, y, w, 1, glassBorderColor);
    }

    private void drawTacticalField(ShaderProgram shader, float x, float y, String label, String value) {
        drawRectInternal(shader, x, y, 2, 38, tactOrange);
        drawText(shader, label, x + 9, y, 0.40f, highlightColor);
        drawText(shader, value, x + 9, y + 17, 0.82f, textColor);
    }

    private void drawCrosshair(ShaderProgram shader, float cx, float cy) {
        float size = 13f, thick = 2f;
        drawRectInternal(shader, cx - thick / 2f, cy - size / 2f, thick, size, crosshairColor);
        drawRectInternal(shader, cx - size / 2f, cy - thick / 2f, size, thick, crosshairColor);
    }

    private void drawDamageVignette(ShaderProgram shader, int width, int height, float timer) {
        drawRectInternal(shader, 0, 0, width, height,
                new Vector4f(1.0f, 0, 0, timer * 0.38f));
    }

    private void drawItemIcon(ShaderProgram shader, Item item, float x, float y, float size) {
        if (item == null)
            return;
        if (item instanceof minicraft.item.ToolItem) {
            String tex = ((minicraft.item.ToolItem) item).getTextureName();
            if (tex != null) {
                drawRectInternal(shader, x, y, size, size, textColor, tex);
                return;
            }
        }
        if (item.isBlock()) {
            drawRectInternal(shader, x, y, size, size, textColor, item.getBlock().sideTexture);
        } else {
            drawRectInternal(shader, x, y, size, size, new Vector4f(0.55f, 0.55f, 0.55f, 1f));
        }
    }

    private void drawArmorSlot(ShaderProgram shader, float x, float y, String label, Item item) {
        drawRectInternal(shader, x, y, 64, 64, new Vector4f(0, 0, 0, 0.6f));
        if (item != null) {
            drawItemIcon(shader, item, x + 8, y + 8, 48);
        } else {
            drawText(shader, label.substring(0, 1), x + 24, y + 20, 0.95f, highlightColor);
        }
        drawText(shader, label, x + 72, y + 24, 0.48f);
    }

    private void renderWeather(ShaderProgram shader, int width, int height,
            minicraft.world.WeatherManager weather) {
        if (weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.CLEAR)
            return;

        float intensity = weather.getIntensity();
        boolean isSnow = weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.SNOW
                || weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.BLIZZARD;

        Vector4f color;
        int count;
        switch (weather.getCurrentType()) {
            case THUNDERSTORM:
                color = new Vector4f(0.4f, 0.4f, 0.7f, 0.65f);
                count = 200;
                break;
            case TORRENTIAL_RAIN:
                color = new Vector4f(0.5f, 0.5f, 0.8f, 0.82f);
                count = 380;
                break;
            case BLIZZARD:
                color = new Vector4f(1.0f, 1.0f, 1.0f, 0.88f);
                count = 500;
                break;
            case HURRICANE:
            case CYCLONE:
                color = new Vector4f(0.3f, 0.3f, 0.5f, 0.72f);
                count = 580;
                break;
            default:
                color = isSnow ? new Vector4f(1, 1, 1, 0.78f)
                        : new Vector4f(0.5f, 0.6f, 1.0f, 0.38f);
                count = 100;
                break;
        }

        long time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            float seed = (float) Math.sin(i * 123.456f);
            float x = Math.abs(seed * width * 10) % width;
            float speed = (300f + Math.abs(seed * 400f)) * (1.0f + intensity);
            float y = (time * speed / 1000f) % height;
            if (!isSnow) {
                drawRectInternal(shader, x, y, 2, 10 + 10 * intensity, color);
            } else {
                float sz = 2 + 3 * intensity;
                drawRectInternal(shader, x, y, sz, sz, color);
            }
        }
    }

    // ── Low-level draw calls ─────────────────────────────────────────────────

    private void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h,
            Vector4f color, String textureName) {
        Matrix4f model = new Matrix4f().identity().translate(x, y, 0).scale(w, h, 1);
        shader.setUniform("modelMatrix", model);
        shader.setUniform("colorTint", color);
        quadMesh.setUVs(new float[] { 0, 1, 1, 1, 1, 0, 0, 0 });
        quadMesh.render(textureName != null ? textures.get(textureName) : whiteTexture);
    }

    private void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h,
            Vector4f color) {
        drawRectInternal(shader, x, y, w, h, color, null);
    }

    private void drawText(ShaderProgram shader, String text, float x, float y, float scale) {
        drawText(shader, text, x, y, scale, textColor);
    }

    private void drawText(ShaderProgram shader, String text, float x, float y,
            float scale, Vector4f color) {
        if (fontTexture == null)
            return;
        // Classic Minecraft drop-shadow: one offset pass (bottom-right only)
        float offset = 1.0f;
        drawTextRaw(shader, text, x + offset, y + offset, scale,
                new Vector4f(0, 0, 0, color.w * 0.85f));
        drawTextRaw(shader, text, x, y, scale, color);
    }

    private void drawTextRaw(ShaderProgram shader, String text, float x, float y,
            float scale, Vector4f color) {
        if (fontTexture == null)
            return;
        float currentX = x;
        shader.setUniform("colorTint", color);
        for (char c : text.toCharArray()) {
            FontTexture.CharInfo info = fontTexture.getCharInfo(c);
            if (info == null)
                continue;
            float w = info.width * scale;
            float h = fontTexture.getHeight() * scale;
            float u1 = (float) info.startX / fontTexture.getWidth();
            float u2 = (float) (info.startX + info.width) / fontTexture.getWidth();
            textQuadMesh.setUVs(new float[] { u1, 1, u2, 1, u2, 0, u1, 0 });
            shader.setUniform("modelMatrix",
                    new Matrix4f().identity().translate(currentX, y, 0).scale(w, h, 1));
            textQuadMesh.render();
            currentX += w;
        }
    }

    public void cleanup() {
        if (quadMesh != null)
            quadMesh.cleanup();
        if (textQuadMesh != null)
            textQuadMesh.cleanup();
        if (fontTexture != null)
            fontTexture.cleanup();
    }
}