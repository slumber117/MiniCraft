package minicraft.renderer;

import minicraft.entity.Player;
import minicraft.math.Matrix4f;
import minicraft.math.Vector3f;
import minicraft.math.Vector4f;
import minicraft.world.Block;
import minicraft.item.Recipe;
import minicraft.item.Item;
import java.util.Map;
import java.util.HashMap;
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
    private static final float XP_TO_HOTBAR_GAP = 34f; // Increased from 4f to lift XP and Stats higher
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
    private final Map<String, Vector3f> armorColors = new HashMap<>();

    public UIRenderer(TextureRegistry textures) {
        this.textures = textures;
        
        // --- Armor Material Mapping (Expanded for all gameplay tiers) ---
        armorColors.put("IRON", new Vector3f(0.85f, 0.85f, 0.90f));
        armorColors.put("GOLD", new Vector3f(1.00f, 0.85f, 0.10f));
        armorColors.put("DIAMOND", new Vector3f(0.50f, 0.95f, 1.00f));
        armorColors.put("EMERALD", new Vector3f(0.15f, 0.90f, 0.25f));
        armorColors.put("RUBY", new Vector3f(0.95f, 0.10f, 0.15f));
        armorColors.put("SAPPHIRE", new Vector3f(0.15f, 0.35f, 0.95f));
        armorColors.put("AMETHYST", new Vector3f(0.70f, 0.25f, 0.95f));
        armorColors.put("QUARTZ", new Vector3f(1.00f, 1.00f, 1.00f));
        armorColors.put("TOPAZ", new Vector3f(1.00f, 0.75f, 0.15f));
        armorColors.put("AQUAMARINE", new Vector3f(0.65f, 0.85f, 0.90f));
        armorColors.put("TITANIUM", new Vector3f(0.55f, 0.55f, 0.65f));
        armorColors.put("TANTALUM", new Vector3f(0.40f, 0.40f, 0.50f));
        armorColors.put("URANIUM", new Vector3f(0.20f, 1.00f, 0.35f)); 
        armorColors.put("PLUTONIUM", new Vector3f(0.10f, 0.85f, 1.00f)); 
        armorColors.put("ADAMANTINE", new Vector3f(1.00f, 0.25f, 0.10f));
        armorColors.put("MITHRIL", new Vector3f(0.70f, 1.00f, 0.95f));
        armorColors.put("PLATINUM", new Vector3f(0.90f, 0.90f, 1.00f));

        float[] positions = { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0 };
        float[] uvs = { 0, 1, 1, 1, 1, 0, 0, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        TextureRegion whiteRegion = textures.get("snow");
        whiteTexture = whiteRegion != null ? whiteRegion.getTexture() : null;
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
        float statBottom = xpLabelTop - 36f; // Increased from 6f to move HUD higher
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
        Vector4f gray = new Vector4f(0.65f, 0.65f, 0.65f, 1f);
        drawText(shader,
                String.format("X:%-6d  Y:%-6d", (int) player.position.x, (int) player.position.y),
                26, 41, 0.45f, gray);
        drawText(shader, String.format("Z:%-6d", (int) player.position.z), 26, 56, 0.45f, gray);
        drawText(shader,
                main.getWorld().getBiome((int) player.position.x, (int) player.position.z).displayName.toUpperCase(),
                26, 70, 0.35f,
                new Vector4f(0.6f, 0.6f, 0.6f, 1f));

        // ── 5. Temperature (top-right, fixed) — stays clear of coord panel ───
        drawTemperatureHUD(player, shader, width, 20f);

        // ── 6. Focused Block (Look-at HUD, mid-right) ───────────────────────
        if (main.focusedBlock != null && main.focusedBlock != Block.AIR) {
            drawFocusedBlockHUD(shader, width, 72f, main.focusedBlock);
        }

        // ── 7. Status Message (top-center overlay) ───────────────────────────
        if (main.activeStatusMessage != null && !main.activeStatusMessage.isEmpty()) {
            drawStatusMessage(shader, width, height, main.activeStatusMessage, main.statusMessageTimer);
        }

        // ── 7. Damage vignette — only below 10% health, never when full ──────
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

        final float SLOT = 58f;
        final float GAP = 8f;
        final float COLS = 9f;
        
        float gridW = COLS * SLOT + (COLS - 1) * GAP;
        // Expansion for Paper Doll (approx 240px wide for doll + armor slots)
        float dollAreaW = 240f; 
        float panelW = gridW + dollAreaW + 80f; 
        float panelH = 4 * SLOT + 3 * GAP + 140f; 
        
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        drawTacticalFrame(shader, sx, sy, panelW, panelH);
        drawText(shader, "EQUIPMENT & LOGISTICS", sx + 22, sy + 22, 0.90f, tactOrange);

        float[] mouse = getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];

        // ── 1. Paper Doll Section (Left) ─────────────────────────────────────
        float dollX = sx + 28f;
        float dollY = sy + 64f;
        float dollW = 160f;
        float dollH = 220f;
        
        // Background for doll
        drawRectInternal(shader, dollX, dollY, dollW, dollH, new Vector4f(0, 0, 0, 0.45f));
        drawRectInternal(shader, dollX, dollY, dollW, 1, glassBorderColor);
        drawRectInternal(shader, dollX, dollY+dollH, dollW, 1, glassBorderColor);
        
        // Render the 3D Doll
        renderPaperDoll(player, shader, dollX + dollW/2, dollY + dollH - 30f, 90f);

        // ── 2. Armor Slots (Next to Doll) ────────────────────────────────────
        float armorX = dollX + dollW + 12f;
        float armorY = dollY;
        
        drawArmorSlot(shader, armorX, armorY, "HELMET", player.inventory.getHelmet());
        drawArmorSlot(shader, armorX, armorY + SLOT + GAP, "CHEST", player.inventory.getChestplate());
        drawArmorSlot(shader, armorX, armorY + 2*(SLOT + GAP), "LEGS", player.inventory.getLeggings());
        drawArmorSlot(shader, armorX, armorY + 3*(SLOT + GAP), "BOOTS", player.inventory.getBoots());

        // ── 2.5 Vitals Readout ──────────────────────────────────────────────
        float statsY = dollY + dollH + 18f;
        drawText(shader, "VITAL SIGNS & TELEMETRY", dollX, statsY, 0.40f, tactOrange);
        
        float def = player.inventory.getTotalDefense() * 100f;
        float spd = (player.inventory.getTotalSpeedMod() - 1.0f) * 100f;
        float ins = player.inventory.getTotalInsulation() * 100f;
        
        drawText(shader, String.format("ARMOR PROTECTION: +%.0f%%", def), dollX + 4, statsY + 18, 0.38f, 
                 def > 0 ? highlightColor : new Vector4f(0.5f,0.5f,0.5f,1f));
        drawText(shader, String.format("MOBILITY RATING: %s%.0f%%", spd >= 0 ? "+" : "", spd), dollX + 4, statsY + 34, 0.38f,
                 spd != 0 ? highlightColor : new Vector4f(0.5f,0.5f,0.5f,1f));
        drawText(shader, String.format("THERMAL SHIELD: +%.0f%%", ins), dollX + 4, statsY + 50, 0.38f,
                 ins > 0 ? highlightColor : new Vector4f(0.5f,0.5f,0.5f,1f));

        // ── 3. Main Grid (Right) ─────────────────────────────────────────────
        float gridStartX = sx + dollAreaW + 50f;
        float mainGridY = sy + 64f;
        minicraft.item.ItemStack[] mainInv = player.inventory.getMainInventory();

        drawText(shader, "STORAGE", gridStartX, mainGridY - 16, 0.45f, highlightColor);

        // --- Scrolling Logic ---
        float gridViewH = 3 * (SLOT + GAP); // visible height of 3 rows
        float scrollTrackX = gridStartX + gridW + 12f;
        float scrollTrackH = gridViewH;
        
        // Draw Scroll Track (Tactical Dark)
        drawRectInternal(shader, scrollTrackX, mainGridY, 10f, scrollTrackH, new Vector4f(0, 0, 0, 0.45f));
        drawRectInternal(shader, scrollTrackX, mainGridY, 10f, 1f, glassBorderColor);
        drawRectInternal(shader, scrollTrackX, mainGridY + scrollTrackH, 10f, 1f, glassBorderColor);

        // Draw Scroll Handle
        float totalRows = 9f; // 81 slots / 9 per row
        float visibleRows = 3f;
        float handleH = (visibleRows / totalRows) * scrollTrackH;
        float maxScroll = (totalRows - visibleRows) * (SLOT + GAP); // ~396px
        float handleOffset = (main.inventoryScroll / maxScroll) * (scrollTrackH - handleH);
        
        drawRectInternal(shader, scrollTrackX + 2, mainGridY + handleOffset + 2, 6f, handleH - 4, tactOrange);

        // --- Clipped Grid Rendering ---
        // Save current scissor state or just use global
        glEnable(GL_SCISSOR_TEST);
        // Note: OpenGL scissor coordinates are from BOTTOM-LEFT
        // We must convert our TOP-LEFT screen coords
        int sx_sc = (int)gridStartX;
        int sy_sc = (int)(height - (mainGridY + gridViewH));
        int sw_sc = (int)(gridW + 4);
        int sh_sc = (int)gridViewH;
        glScissor(sx_sc, sy_sc, sw_sc, sh_sc);

        for (int i = 0; i < 81; i++) {
            int col = i % 9, row = i / 9;
            float sx2 = gridStartX + col * (SLOT + GAP);
            float sy2 = mainGridY + row * (SLOT + GAP) - main.inventoryScroll;
            
            // Optimization: Only render if potentially visible (within or near scissored area)
            if (sy2 + SLOT > mainGridY - 100 && sy2 < mainGridY + gridViewH + 100) {
                boolean hover = isHovered(mouseX, mouseY, sx2, sy2, SLOT, SLOT);
                drawSlot(shader, sx2, sy2, SLOT, mainInv[i], hover);
            }
        }
        glDisable(GL_SCISSOR_TEST);

        // ── 4. Hotbar row ────────────────────────────────────────────────────
        float sepY = mainGridY + 3 * (SLOT + GAP) + 12f;
        drawRectInternal(shader, gridStartX, sepY, gridW, 1f, glassBorderColor);

        float hotbarRowY = sepY + 20f;
        minicraft.item.ItemStack[] hotbar = player.inventory.getHotbar();
        int selIdx = player.inventory.getSelectedIndex();

        drawText(shader, "HOTBAR", gridStartX, hotbarRowY - 14, 0.45f, highlightColor);

        for (int i = 0; i < 9; i++) {
            float sx2 = gridStartX + i * (SLOT + GAP);
            boolean sel = (i == selIdx);
            boolean hover = isHovered(mouseX, mouseY, sx2, hotbarRowY, SLOT, SLOT);
            drawSlot(shader, sx2, hotbarRowY, SLOT, hotbar[i], hover);
            if (sel) {
                drawRectInternal(shader, sx2, hotbarRowY, SLOT, 3f, new Vector4f(1.0f, 0.95f, 0.0f, 1.0f));
            }
        }
    }

    private void renderPaperDoll(Player player, ShaderProgram shader, float cx, float cy, float scale) {
        // Setup 3D transform for the UI doll
        float rotation = (System.currentTimeMillis() % 10000) / 10000f * 360f;
        float pulse = (float)Math.sin(System.currentTimeMillis() / 400.0) * 0.02f;
        
        Matrix4f baseModelMatrix = new Matrix4f().identity()
                .translate(cx, cy, 50f) 
                .scale(scale * (1f + pulse), -scale * (1f + pulse), scale)
                .rotateY((float) Math.toRadians(rotation));
        
        shader.setUniform("modelMatrix", baseModelMatrix);
        shader.setUniform("useLighting", 1.0f);
        shader.setUniform("sunBrightness", 1.0f);
        shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
        
        Mesh human = ModelRegistry.getModel("zombie"); 
        if (human != null) {
            // 1. Render Base Skin
            human.render(textures.get("zombie_hd").getTexture()); 

            // 2. Render Armor Layers (Sequential Overlays)
            // We use the same mesh but scale up slightly for each piece to act as a 3D shell.
            // In a production engine we'd use separate armor meshes, but here we use a tinted 'Aura Shell'.
            
            minicraft.item.ArmorItem[] pieces = {
                player.inventory.getHelmet(), 
                player.inventory.getChestplate(), 
                player.inventory.getLeggings(), 
                player.inventory.getBoots()
            };

            for (minicraft.item.ArmorItem piece : pieces) {
                if (piece != null) {
                    Vector3f color = armorColors.get(piece.getTierName().toUpperCase());
                    if (color == null) color = new Vector3f(1, 1, 1); // Fallback

                    // Set tint for this armor piece
                    shader.setUniform("colorTint", new Vector4f(color.x, color.y, color.z, 0.75f));
                    
                    // Render slightly larger shell (1.04x)
                    Matrix4f shellMatrix = new Matrix4f(baseModelMatrix).scale(1.04f, 1.04f, 1.04f);
                    shader.setUniform("modelMatrix", shellMatrix);
                    
                    human.render(textures.get("alloy_plate").getTexture()); // Metallic texture
                }
            }

            // 3. Render Held Tool (if any)
            minicraft.item.Item held = player.inventory.getSelectedItem();
            if (held instanceof minicraft.item.ToolItem) {
                minicraft.item.ToolItem tool = (minicraft.item.ToolItem) held;
                String toolName = tool.getName();
                String modelId = null;

                if (toolName.contains("Wood")) modelId = "pickaxe_wooden";
                else if (toolName.contains("Stone")) modelId = "pickaxe_stone";
                else if (toolName.contains("Iron")) modelId = "pickaxe_iron";
                else if (toolName.contains("Diamond")) modelId = "pickaxe_diamond";

                if (modelId != null) {
                    Mesh toolMesh = ModelRegistry.getModel(modelId);
                    if (toolMesh != null) {
                        // Offset to the 'hand' position (right-side, slightly forward)
                        Matrix4f toolMatrix = new Matrix4f(baseModelMatrix)
                                .translate(0.35f, -0.6f, 0.5f) // Position in hand
                                .rotateZ((float) Math.toRadians(135f)) // Angle it correctly
                                .scale(0.85f, 0.85f, 0.85f);
                        
                        shader.setUniform("modelMatrix", toolMatrix);
                        
                        // Apply Diamond tint if applicable
                        if (toolName.contains("Diamond")) {
                            shader.setUniform("colorTint", new Vector4f(0.5f, 0.95f, 1.0f, 1.0f));
                        } else {
                            shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
                        }

                        toolMesh.render(textures.get("alloy_plate").getTexture());
                    }
                }
            }
        }
        
        // Reset uniforms
        shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
        shader.setUniform("useLighting", 0.0f);
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
        // Unified color: matches coordinates (gray 0.65f)
        drawText(shader, tempText, x + 10, y + 14, 0.85f, new Vector4f(0.65f, 0.65f, 0.65f, 1f));

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

    private void drawStatusMessage(ShaderProgram shader, int width, int height, String msg, float timer) {
        float scale = 0.55f;
        float textW = msg.length() * 14f * scale; // Approximated
        float x = (width - textW) / 2f;
        float y = 28f;

        // Fade out in the last 0.5s
        float alpha = Math.min(1.0f, timer * 2.0f);
        Vector4f color = new Vector4f(1f, 1f, 1f, alpha);
        
        drawText(shader, msg.toUpperCase(), x, y, scale, color);
    }

    private void drawFocusedBlockHUD(ShaderProgram shader, int screenWidth, float y, Block block) {
        float panelW = 188f, panelH = 46f;
        float x = screenWidth - 20f - panelW;
        
        drawTacticalFrame(shader, x, y, panelW, panelH);
        drawText(shader, "TARGET IDENTIFIED", x + 10, y + 10, 0.35f, tactOrange);
        drawText(shader, block.getFriendlyName().toUpperCase(), x + 10, y + 25, 0.45f, new Vector4f(0.65f, 0.65f, 0.65f, 1f));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Crafting menu — icon-grid layout
    // ══════════════════════════════════════════════════════════════════════════
    private void renderCraftingMenu(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.88f));

        // ── Layout constants (MUST match handleCraftingInput in Main.java) ────
        final float MENU_W    = 700f, MENU_H = 540f;
        final float ICON_SIZE = 56f,  ICON_GAP = 8f;
        final int   COLS      = 7;
        final float GRID_OFF_X = 14f, GRID_OFF_Y = 60f;
        final float DETAIL_W  = 182f;
        float sx = (width  - MENU_W) / 2f;
        float sy = (height - MENU_H) / 2f;

        // Panel
        drawTacticalFrame(shader, sx, sy, MENU_W, MENU_H);
        drawText(shader, "CRAFTING WORKSHOP", sx + 18, sy + 16, 0.75f, tactOrange);

        // ── Category tabs ─────────────────────────────────────────────────────
        Recipe.Category[] cats = Recipe.Category.values();
        float gridAreaW = COLS * ICON_SIZE + (COLS - 1) * ICON_GAP;
        float tabW = gridAreaW / cats.length;
        for (int i = 0; i < cats.length; i++) {
            float tx = sx + GRID_OFF_X + i * (tabW + 2f);
            float ty = sy + 34f;
            boolean active = (main.activeCategory == cats[i]);
            drawRectInternal(shader, tx, ty, tabW - 2, 22f,
                    active ? new Vector4f(0.12f, 0.63f, 1.00f, 0.90f)
                           : new Vector4f(0.08f, 0.08f, 0.14f, 0.85f));
            if (active)
                drawRectInternal(shader, tx, ty + 22f, tabW - 2, 2f, tactOrange);
            String lbl = cats[i].name();
            drawText(shader, lbl, tx + tabW / 2f - lbl.length() * 3.1f, ty + 6f, 0.58f,
                    active ? textColor : new Vector4f(0.60f, 0.62f, 0.65f, 1f));
        }

        // ── Recipe icon grid ──────────────────────────────────────────────────
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : main.craftingManager.getRecipes())
            if (r.getCategory() == main.activeCategory)
                filtered.add(r);

        float gx = sx + GRID_OFF_X;
        float gy = sy + GRID_OFF_Y;

        for (int i = 0; i < filtered.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            float ix = gx + col * (ICON_SIZE + ICON_GAP);
            float iy = gy + row * (ICON_SIZE + ICON_GAP);
            // Clip to panel
            if (iy + ICON_SIZE > sy + MENU_H - 30f) break;

            boolean selected = (i == main.recipeIndex);

            // Affordability check
            boolean canAfford = true;
            for (Map.Entry<Item, Integer> e : filtered.get(i).getIngredients().entrySet())
                if (player.inventory.getCount(e.getKey()) < e.getValue()) { canAfford = false; break; }

            // Slot bg
            drawRectInternal(shader, ix, iy, ICON_SIZE, ICON_SIZE,
                    selected ? new Vector4f(0.10f, 0.32f, 0.60f, 0.95f)
                             : new Vector4f(0.05f, 0.05f, 0.10f, 0.88f));

            // Icon
            float pad = 7f;
            drawItemIcon(shader, filtered.get(i).getResult(),
                    ix + pad, iy + pad, ICON_SIZE - pad * 2);

            // Red dim overlay if can't afford
            if (!canAfford)
                drawRectInternal(shader, ix, iy, ICON_SIZE, ICON_SIZE,
                        new Vector4f(0.65f, 0.0f, 0.0f, 0.45f));

            // Border
            float bw = selected ? 2f : 1f;
            Vector4f bc = selected ? tactBlue
                        : (canAfford ? new Vector4f(0.10f, 0.65f, 0.80f, 0.45f)
                                     : new Vector4f(0.55f, 0.10f, 0.10f, 0.50f));
            drawRectInternal(shader, ix,               iy,               ICON_SIZE, bw, bc);
            drawRectInternal(shader, ix,               iy+ICON_SIZE-bw,  ICON_SIZE, bw, bc);
            drawRectInternal(shader, ix,               iy,               bw, ICON_SIZE, bc);
            drawRectInternal(shader, ix+ICON_SIZE-bw,  iy,               bw, ICON_SIZE, bc);

            // Tiny affordability dot (top-right corner)
            drawRectInternal(shader, ix + ICON_SIZE - 8f, iy, 8f, 8f,
                    canAfford ? tactGreen : new Vector4f(0.90f, 0.18f, 0.18f, 1f));
        }

        // ── Detail panel (right side) ─────────────────────────────────────────
        float dx = sx + GRID_OFF_X + gridAreaW + 18f;
        float dy = sy + GRID_OFF_Y;
        float dh = MENU_H - GRID_OFF_Y - 14f;
        drawRectInternal(shader, dx, dy, DETAIL_W, dh, new Vector4f(0.03f, 0.03f, 0.08f, 0.92f));
        drawRectInternal(shader, dx,      dy, DETAIL_W, 1f, glassBorderColor);
        drawRectInternal(shader, dx,      dy, 1f, dh, glassBorderColor);
        drawRectInternal(shader, dx+DETAIL_W-1, dy, 1f, dh, glassBorderColor);

        if (main.recipeIndex >= 0 && main.recipeIndex < filtered.size()) {
            Recipe sel = filtered.get(main.recipeIndex);

            // Large preview icon
            float prev = 64f;
            float px = dx + (DETAIL_W - prev) / 2f;
            float py = dy + 10f;
            drawRectInternal(shader, px, py, prev, prev, new Vector4f(0.08f, 0.08f, 0.14f, 1f));
            drawItemIcon(shader, sel.getResult(), px + 7, py + 7, prev - 14);

            // Name
            String name = sel.getName();
            float ns = name.length() > 14 ? 0.52f : 0.60f;
            drawText(shader, name, dx + DETAIL_W / 2f - name.length() * ns * 5.2f, py + prev + 8f, ns, textColor);

            // Divider
            drawRectInternal(shader, dx + 8, py + prev + 26f, DETAIL_W - 16, 1f, glassBorderColor);
            drawText(shader, "REQUIRES:", dx + 10, py + prev + 32f, 0.50f, tactOrange);

            // Ingredients
            boolean canCraft = true;
            int k = 0;
            for (Map.Entry<Item, Integer> e : sel.getIngredients().entrySet()) {
                int owned = player.inventory.getCount(e.getKey());
                boolean ok = owned >= e.getValue();
                if (!ok) canCraft = false;
                Vector4f col = ok ? tactGreen : new Vector4f(1f, 0.35f, 0.35f, 1f);
                float iy2 = py + prev + 46f + k * 36f;
                drawItemIcon(shader, e.getKey(), dx + 10, iy2, 20f);
                drawText(shader, e.getKey().getName(), dx + 34, iy2 + 1, 0.48f, col);
                drawText(shader, owned + " / " + e.getValue(), dx + 34, iy2 + 14, 0.56f, col);
                k++;
            }

            // FORGE button
            float bby = sy + MENU_H - 52f;
            float bbx = dx + 8f, bbw = DETAIL_W - 16f;
            drawRectInternal(shader, bbx, bby, bbw, 38f,
                    canCraft ? new Vector4f(0.10f, 0.58f, 0.98f, 1f)
                             : new Vector4f(0.18f, 0.18f, 0.24f, 0.90f));
            String lbl = canCraft ? "FORGE" : "LOCKED";
            drawText(shader, lbl, bbx + bbw / 2f - lbl.length() * 3.9f, bby + 12f, 0.70f,
                    canCraft ? textColor : new Vector4f(0.45f, 0.45f, 0.50f, 1f));
        } else {
            drawText(shader, "SELECT AN ITEM",  dx + 16, dy + dh / 2f - 10, 0.55f,
                    new Vector4f(0.40f, 0.42f, 0.48f, 1f));
        }

        // Hint bar
        drawText(shader, "CLICK = SELECT    ENTER/FORGE = CRAFT    ARROWS = NAVIGATE",
                sx + 8, sy + MENU_H - 14f, 0.36f, new Vector4f(0.38f, 0.40f, 0.45f, 1f));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Facility screen (furnace / cooker)
    // ══════════════════════════════════════════════════════════════════════════
    private void renderFacilityScreen(Player player, ShaderProgram shader,
            int width, int height, minicraft.Main main) {
        if (main.activeFacility == null)
            return;

        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        float panelW = 680f, panelH = 580f;
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

        // Fuel ratio bar with Heat animation
        float fuelRatio = main.activeFacility.getFuelRatio();
        float heatPulse = main.activeFacility.isActive ? 0.8f + 0.2f * (float)Math.sin(System.currentTimeMillis() / 150.0) : 1.0f;
        Vector4f fuelCol = main.furnaceOpen ? new Vector4f(1.0f, 0.4f * heatPulse, 0, 0.9f) : new Vector4f(0.2f, 0.6f, 1.0f, 0.9f);
        
        drawRectInternal(shader, cx - 36, cy + 132, slotSize * fuelRatio, 6, fuelCol);
        drawRectInternal(shader, cx - 36, cy + 132, slotSize, 1, glassBorderColor); // Track

        // Smelting Flame Icon (Flickering Glow)
        if (main.activeFacility.isActive) {
            float flameAlpha = 0.4f + 0.6f * (float)Math.random(); // Fast jitter
            drawRectInternal(shader, cx - 12, cy + 12, 24, 24, new Vector4f(1.0f, 0.5f, 0, flameAlpha * 0.4f));
            drawRectInternal(shader, cx - 6, cy + 18, 12, 12, new Vector4f(1.0f, 0.8f, 0, flameAlpha));
        }

        // Output
        drawSlot(shader, cx + 98, cy - 36, slotSize, main.activeFacility.getSlot(2));
        drawText(shader, "OUTPUT", cx + 98, cy - 52, 0.52f, highlightColor);

        // Progress arrow / bar
        float prog = main.activeFacility.getProgress();
        drawRectInternal(shader, cx - 66, cy, 130, 8, new Vector4f(1, 1, 1, 0.10f));
        if (prog > 0) {
            Vector4f progCol = new Vector4f(titleCol.x, titleCol.y, titleCol.z, 0.7f + 0.3f * (float)Math.sin(System.currentTimeMillis() / 200.0));
            drawRectInternal(shader, cx - 66, cy, 130 * prog, 8, progCol);
            drawText(shader, (int) (prog * 100) + "%", cx - 14, cy + 18, 0.58f, textColor);
        }

        // Player inventory (3×9 in middle of panel)
        final float ISLOT = 54f, IGAP = 6f;
        float gridW = 9 * ISLOT + 8 * IGAP;
        float invStartX = sx + (panelW - gridW) / 2f;
        float invStartY = sy + 300f;

        drawText(shader, "INVENTORY", invStartX, invStartY - 16, 0.52f, highlightColor);
        minicraft.item.ItemStack[] pInv = player.inventory.getMainInventory();
        for (int i = 0; i < 27; i++) {
            int row = i / 9, col = i % 9;
            drawSlot(shader, invStartX + col * (ISLOT + IGAP),
                    invStartY + row * (ISLOT + IGAP), ISLOT, pInv[i]);
        }

        // Hotbar row (1x9 at bottom of panel)
        float hotStartY = invStartY + 3 * (ISLOT + IGAP) + 18f;
        drawText(shader, "HOTBAR", invStartX, hotStartY - 14, 0.52f, highlightColor);
        minicraft.item.ItemStack[] hInv = player.inventory.getHotbar();
        for (int i = 0; i < 9; i++) {
            drawSlot(shader, invStartX + i * (ISLOT + IGAP), hotStartY, ISLOT, hInv[i]);
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
        if (item.getTextureName() != null) {
            drawRectInternal(shader, x, y, size, size, textColor, item.getTextureName());
            return;
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

        TextureRegion region = (textureName != null) ? textures.get(textureName) : null;
        if (region != null) {
            // Map atlas coordinates to the quad mesh
            quadMesh.setUVs(new float[] {
                    region.getU1(), region.getV2(),
                    region.getU2(), region.getV2(),
                    region.getU2(), region.getV1(),
                    region.getU1(), region.getV1()
            });
            quadMesh.render(region.getTexture());
        } else {
            // Solid color quad fallback
            quadMesh.setUVs(new float[] { 0, 1, 1, 1, 1, 0, 0, 0 });
            quadMesh.render(whiteTexture);
        }
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