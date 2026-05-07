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
import minicraft.renderer.ui.*;
import minicraft.ship.ShipRegistry;
import minicraft.ship.ShipDefinition;
import minicraft.Main;
import minicraft.world.WeatherManager;

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
    public static final float HOTBAR_MARGIN_BOT = 14f;
    public static final float HOTBAR_SLOT_SIZE = 42f;
    public static final float HOTBAR_GAP = 6f;
    public static final int   HOTBAR_SLOTS = 9;
    public static final float HOTBAR_H = HOTBAR_SLOT_SIZE;

    public static final float XP_BAR_H = 10f;
    public static final float XP_TO_HOTBAR_GAP = 34f; 

    public static final float STAT_BAR_W = 210f;
    public static final float STAT_BAR_H = 12f;
    public static final float STAT_BETWEEN_GAP = 10f;
    public static final float STAT_PANEL_PAD = 8f;
    public static final float STAT_PANEL_H = STAT_BAR_H * 2 + STAT_BETWEEN_GAP + STAT_PANEL_PAD * 2 + 6f;

    // ── Meshes & fonts ───────────────────────────────────────────────────────
    private Mesh quadMesh;
    private Mesh textQuadMesh;
    private FontTexture fontTexture;
    private Texture whiteTexture;
    private final TextureRegistry textures;

    // ── UI Components (Modular) ─────────────────────────────────────────────
    private final InventoryUI inventoryUI = new InventoryUI();
    private final CraftingUI craftingUI = new CraftingUI();
    private final ChestUI chestUI = new ChestUI();
    private final FacilityUI facilityUI = new FacilityUI();
    private final ShipConsoleUI shipConsoleUI = new ShipConsoleUI();
    private final HUD hud = new HUD();
    private final PilotHUD pilotHUD = new PilotHUD();
    private final QuestLogUI questLogUI = new QuestLogUI();

    public UIRenderer(TextureRegistry textures) {
        this.textures = textures;

        float[] positions = { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0 };
        float[] uvs = { 0, 1, 1, 1, 1, 0, 0, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        TextureRegion whiteRegion = textures.get("snow");
        whiteTexture = whiteRegion != null ? whiteRegion.getTexture() : null;
        quadMesh = new Mesh(positions, uvs, indices, whiteTexture);
        textQuadMesh = new Mesh(positions, uvs, indices, null);

        try {
            java.awt.Font font = new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 28);
            fontTexture = new FontTexture(font, "ISO-8859-1");
            textQuadMesh.setTexture(fontTexture.getTexture());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        shader.setUniform("useTexture", 1.0f);

        // Exactly one branch executes — screens are mutually exclusive
        if (main.inventoryOpen) {
            inventoryUI.render(this, player, shader, width, height, main);
        } else if (main.chestOpen) {
            chestUI.render(this, player, shader, width, height, main);
        } else if (main.craftingOpen || main.blacksmithOpen) {
            craftingUI.render(this, player, shader, width, height, main);
        } else if (main.shipConsoleOpen) {
            shipConsoleUI.render(this, player, shader, width, height, main);
        } else if (main.furnaceOpen || main.cookerOpen) {
            facilityUI.render(this, player, shader, width, height, main);
        } else if (player.isRiding()) {
            pilotHUD.render(this, player, shader, width, height);
        } else if (main.questLogOpen) {
            questLogUI.render(this, player, shader, width, height, main);
        } else {
            // Pure gameplay HUD
            drawCrosshair(shader, width / 2f, height / 2f);
            if (player.radiationLevel > 0) drawRadiationVignette(shader, width, height, player.radiationLevel);
            hud.renderPlayHUD(this, player, shader, width, height, main);
        }

        // Global cursor overlay (menus only)
        if (main.inventoryOpen || main.chestOpen || main.craftingOpen || main.blacksmithOpen
                || main.shipConsoleOpen || main.furnaceOpen || main.cookerOpen) {

            float[] mouse = getScaledMouse(main, width, height);
            float mouseX = mouse[0], mouseY = mouse[1];

            minicraft.item.ItemStack cursor = player.inventory.getCursorStack();
            if (cursor != null && !cursor.isEmpty()) {
                float iconSize = 48f;
                drawItemIcon(shader, cursor.getItem(), mouseX - iconSize / 2f, mouseY - iconSize / 2f, iconSize);
                if (cursor.getCount() > 1)
                    drawText(shader, String.valueOf(cursor.getCount()),
                            mouseX + iconSize / 2f - 14, mouseY + iconSize / 2f - 8, 0.65f);
            }

            drawMenuCursor(shader, mouseX, mouseY);
        }

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shared helpers
    // ══════════════════════════════════════════════════════════════════════════

    public float[] getScaledMouse(Main main, int renderW, int renderH) {
        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        int[] winW = new int[1], winH = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(main.getWindow(), winW, winH);
        float scaleX = winW[0] > 0 ? (float) renderW / winW[0] : 1f;
        float scaleY = winH[0] > 0 ? (float) renderH / winH[0] : 1f;
        return new float[] { (float) mx[0] * scaleX, (float) my[0] * scaleY };
    }

    public boolean isHovered(float mx, float my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    public void drawMenuCursor(ShaderProgram shader, float mx, float my) {
        float size = 16f;
        float thickness = 2f;
        Vector4f col = new Vector4f(0.17f, 0.72f, 0.79f, 0.95f); // Theme cyan

        drawRectInternal(shader, mx - size / 2f, my - thickness / 2f, size, thickness, col);
        drawRectInternal(shader, mx - thickness / 2f, my - size / 2f, thickness, size, col);
        drawRectInternal(shader, mx - thickness, my - thickness, thickness * 2, thickness * 2, new Vector4f(1, 1, 1, 0.8f));
    }

    public void drawTacticalFrame(ShaderProgram shader, float x, float y, float w, float h) {
        drawRectInternal(shader, x, y, w, h, UIPalette.RUSTIC_BG);
        float frameT = 4f;
        drawRectInternal(shader, x, y, w, frameT, UIPalette.RUSTIC_BORDER);
        drawRectInternal(shader, x, y + h - frameT, w, frameT, UIPalette.RUSTIC_BORDER);
        drawRectInternal(shader, x, y, frameT, h, UIPalette.RUSTIC_BORDER);
        drawRectInternal(shader, x + w - frameT, y, frameT, h, UIPalette.RUSTIC_BORDER);
        drawRectInternal(shader, x + frameT, y + frameT, w - frameT*2, 1, UIPalette.RUSTIC_LIGHT_WOOD);
        drawRectInternal(shader, x + frameT, y + h - frameT - 1, w - frameT*2, 1, UIPalette.RUSTIC_LIGHT_WOOD);
        float sS = 8f;
        drawRectInternal(shader, x, y, sS, sS, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x + w - sS, y, sS, sS, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x, y + h - sS, sS, sS, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x + w - sS, y + h - sS, sS, sS, UIPalette.RUSTIC_STONE);
    }

    public void drawTacticalBar(ShaderProgram shader, float x, float y, float w, float h,
            float fill, Vector4f color, String label) {
        drawText(shader, label, x, y - 14, 0.40f, UIPalette.RUSTIC_PARCHMENT);
        drawRectInternal(shader, x - 2, y - 2, w + 4, h + 4, new Vector4f(0.08f, 0.08f, 0.08f, 0.85f));
        drawRectInternal(shader, x - 1, y - 1, w + 2, h + 2, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x, y, w * Math.max(0, Math.min(1, fill)), h, color);
    }

    public void drawTacticalField(ShaderProgram shader, float x, float y, String label, String value) {
        drawRectInternal(shader, x, y, 4, 38, UIPalette.RUSTIC_BORDER);
        drawText(shader, label, x + 12, y, 0.40f, UIPalette.RUSTIC_PARCHMENT);
        drawText(shader, value, x + 12, y + 17, 0.82f, UIPalette.TEXT_COLOR);
    }

    public void drawCrosshair(ShaderProgram shader, float cx, float cy) {
        float size = 13f, thick = 2f;
        drawRectInternal(shader, cx - thick / 2f, cy - size / 2f, thick, size, UIPalette.CROSSHAIR_COLOR);
        drawRectInternal(shader, cx - size / 2f, cy - thick / 2f, size, thick, UIPalette.CROSSHAIR_COLOR);
    }

    public void drawRadiationVignette(ShaderProgram shader, int width, int height, float level) {
        float alpha = Math.min(0.6f, level * 0.05f);
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0.3f, 1.0f, 0.2f, alpha));
    }

    public void drawDamageVignette(ShaderProgram shader, int width, int height, float timer) {
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(1.0f, 0, 0, timer * 0.38f));
    }

    public void drawItemIcon(ShaderProgram shader, Item item, float x, float y, float size) {
        if (item == null) return;
        if (item instanceof minicraft.item.ToolItem) {
            String tex = ((minicraft.item.ToolItem) item).getTextureName();
            if (tex != null) {
                drawRectInternal(shader, x, y, size, size, UIPalette.TEXT_COLOR, tex);
                return;
            }
        }
        if (item.getTextureName() != null) {
            drawRectInternal(shader, x, y, size, size, UIPalette.TEXT_COLOR, item.getTextureName());
            return;
        }
        if (item.isBlock()) {
            drawRectInternal(shader, x, y, size, size, UIPalette.TEXT_COLOR, item.getBlock().sideTexture);
        } else {
            drawRectInternal(shader, x, y, size, size, new Vector4f(0.55f, 0.55f, 0.55f, 1f));
        }
    }

    public void drawArmorSlot(ShaderProgram shader, float x, float y, String label, Item item, int playerLevel) {
        drawRectInternal(shader, x, y, 64, 64, new Vector4f(0, 0, 0, 0.6f));
        if (item != null) {
            drawItemIcon(shader, item, x + 8, y + 8, 48);
            if (item.getLevelRequirement() > playerLevel) {
                drawRectInternal(shader, x + 44, y + 4, 18, 14, new Vector4f(0.8f, 0, 0, 0.6f));
                drawText(shader, "L" + item.getLevelRequirement(), x + 46, y + 6, 0.35f, new Vector4f(1, 0.8f, 0.8f, 1f));
            }
        } else {
            drawText(shader, label.substring(0, 1), x + 24, y + 20, 0.95f, UIPalette.RUSTIC_PARCHMENT);
        }
        drawText(shader, label, x + 72, y + 24, 0.48f);
    }

    public void drawSlot(ShaderProgram shader, float x, float y, float size,
            minicraft.item.ItemStack stack, boolean hovered, int playerLevel) {
        Vector4f bg = hovered ? new Vector4f(0.24f, 0.22f, 0.20f, 1.0f) : new Vector4f(0.12f, 0.11f, 0.10f, 1.0f);
        drawRectInternal(shader, x, y, size, size, bg);
        drawRectInternal(shader, x, y, size, 2f, new Vector4f(0.08f, 0.08f, 0.08f, 1.0f));
        drawRectInternal(shader, x, y, 2f, size, new Vector4f(0.08f, 0.08f, 0.08f, 1.0f));
        drawRectInternal(shader, x, y + size - 2, size, 2f, new Vector4f(0.35f, 0.35f, 0.35f, 1.0f));
        drawRectInternal(shader, x + size - 2, y, 2f, size, new Vector4f(0.35f, 0.35f, 0.35f, 1.0f));

        if (hovered) {
            drawRectInternal(shader, x - 2, y - 2, size + 4, 2, UIPalette.RUSTIC_LIGHT_WOOD);
            drawRectInternal(shader, x - 2, y + size, size + 4, 2, UIPalette.RUSTIC_LIGHT_WOOD);
            drawRectInternal(shader, x - 2, y - 2, 2, size + 4, UIPalette.RUSTIC_LIGHT_WOOD);
            drawRectInternal(shader, x + size, y - 2, 2, size + 4, UIPalette.RUSTIC_LIGHT_WOOD);
        }

        if (stack != null && !stack.isEmpty()) {
            float pad = size * 0.13f;
            drawItemIcon(shader, stack.getItem(), x + pad, y + pad, size - pad * 2);
            if (stack.getCount() > 1)
                drawText(shader, String.valueOf(stack.getCount()), x + 3, y + size - 12, 0.58f);
                
            // Level Requirement Display
            if (stack.getItem().getLevelRequirement() > playerLevel) {
                drawRectInternal(shader, x + size - 20, y + 2, 18, 14, new Vector4f(0.8f, 0, 0, 0.6f));
                drawText(shader, "L" + stack.getItem().getLevelRequirement(), x + size - 18, y + 4, 0.35f, new Vector4f(1, 0.8f, 0.8f, 1f));
            }
        }
    }

    public void drawSlot(ShaderProgram shader, float x, float y, float size, minicraft.item.ItemStack stack) {
        drawSlot(shader, x, y, size, stack, false, 1);
    }

    public void drawPremiumBar(ShaderProgram shader, float x, float y, float w, float h, float fill,
            Vector4f c1, Vector4f c2, String icon) {
        float f = Math.max(0f, Math.min(1f, fill));
        float pulse = 1.0f;
        if (f < 0.20f) pulse = 0.72f + (float) Math.abs(Math.sin(System.currentTimeMillis() / 220.0)) * 0.38f;
        drawText(shader, icon, x - 22, y - 4, 0.70f, new Vector4f(c1.x, c1.y, c1.z, pulse));
        drawRectInternal(shader, x - 2, y - 2, w + 4, h + 4, new Vector4f(0.08f, 0.08f, 0.08f, 0.85f));
        drawRectInternal(shader, x - 1, y - 1, w + 2, h + 2, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x, y, w * f, h, c1);
    }

    public void renderWeather(ShaderProgram shader, int width, int height, WeatherManager weather) {
        if (weather == null || weather.getCurrentType() == WeatherManager.WeatherType.CLEAR) return;
        float intensity = weather.getIntensity();
        boolean isSnow = weather.getCurrentType() == WeatherManager.WeatherType.SNOW || weather.getCurrentType() == WeatherManager.WeatherType.BLIZZARD;
        Vector4f color = isSnow ? new Vector4f(1, 1, 1, 0.78f) : new Vector4f(0.5f, 0.6f, 1.0f, 0.38f);
        int count = 100;
        long time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            float seed = (float) Math.sin(i * 123.456f);
            float x = Math.abs(seed * width * 10) % width;
            float speed = (300f + Math.abs(seed * 400f)) * (1.0f + intensity);
            float y = (time * speed / 1000f) % height;
            if (!isSnow) drawRectInternal(shader, x, y, 2, 10 + 10 * intensity, color);
            else {
                float sz = 2 + 3 * intensity;
                drawRectInternal(shader, x, y, sz, sz, color);
            }
        }
    }

    public void drawTexture(ShaderProgram shader, String textureName, float x, float y, float w, float h, Vector4f color) {
        drawRectInternal(shader, x, y, w, h, color, textureName);
    }

    public void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h, Vector4f color, String textureName) {
        Matrix4f model = new Matrix4f().identity().translate(x, y, 0).scale(w, h, 1);
        shader.setUniform("modelMatrix", model);
        shader.setUniform("colorTint", color);
        TextureRegion region = (textureName != null) ? textures.get(textureName) : null;
        if (region != null) {
            quadMesh.setUVs(new float[] { region.getU1(), region.getV2(), region.getU2(), region.getV2(), region.getU2(), region.getV1(), region.getU1(), region.getV1() });
            quadMesh.render(region.getTexture());
        } else {
            quadMesh.setUVs(new float[] { 0, 1, 1, 1, 1, 0, 0, 0 });
            quadMesh.render(whiteTexture);
        }
    }

    public void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h, Vector4f color) {
        drawRectInternal(shader, x, y, w, h, color, null);
    }

    public void drawText(ShaderProgram shader, String text, float x, float y, float scale) {
        drawText(shader, text, x, y, scale, UIPalette.TEXT_COLOR);
    }

    public void drawText(ShaderProgram shader, String text, float x, float y, float scale, Vector4f color) {
        if (fontTexture == null) return;
        float offset = 1.0f;
        drawTextRaw(shader, text, x + offset, y + offset, scale, new Vector4f(0, 0, 0, color.w * 0.85f));
        drawTextRaw(shader, text, x, y, scale, color);
    }

    private void drawTextRaw(ShaderProgram shader, String text, float x, float y, float scale, Vector4f color) {
        if (fontTexture == null) return;
        float currentX = x;
        shader.setUniform("colorTint", color);
        for (char c : text.toCharArray()) {
            FontTexture.CharInfo info = fontTexture.getCharInfo(c);
            if (info == null) continue;
            float w = info.width * scale;
            float h = fontTexture.getHeight() * scale;
            float u1 = (float) info.startX / fontTexture.getWidth();
            float u2 = (float) (info.startX + info.width) / fontTexture.getWidth();
            textQuadMesh.setUVs(new float[] { u1, 1, u2, 1, u2, 0, u1, 0 });
            shader.setUniform("modelMatrix", new Matrix4f().identity().translate(currentX, y, 0).scale(w, h, 1));
            textQuadMesh.render();
            currentX += w;
        }
    }

    public void drawArrow(ShaderProgram shader, float cx, float cy, float radius, float rotation, Vector4f color) {
        Matrix4f base = new Matrix4f()
            .identity()
            .translate(cx, cy, 0)
            .rotateZ(rotation);
            
        shader.setUniform("colorTint", color);
        
        // Stem
        Matrix4f stem = new Matrix4f(base).scale(4, radius * 0.8f, 1).translate(-0.5f, -0.5f, 0);
        shader.setUniform("modelMatrix", stem);
        quadMesh.render(whiteTexture);
        
        // Tip (Triangle approximation with 2 angled bars)
        float tipY = radius * 0.3f;
        Matrix4f headL = new Matrix4f(base).translate(0, tipY, 0).rotateZ((float)Math.toRadians(45)).scale(4, radius*0.5f, 1).translate(-0.5f, -1.0f, 0);
        shader.setUniform("modelMatrix", headL);
        quadMesh.render(whiteTexture);

        Matrix4f headR = new Matrix4f(base).translate(0, tipY, 0).rotateZ((float)Math.toRadians(-45)).scale(4, radius*0.5f, 1).translate(-0.5f, -1.0f, 0);
        shader.setUniform("modelMatrix", headR);
        quadMesh.render(whiteTexture);
    }

    public void renderLoadingScreen(ShaderProgram shader, int width, int height, Main main) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        shader.bind();
        shader.setUniform("projectionMatrix", new Matrix4f().identity().ortho(0, width, height, 0, -1, 1));
        shader.setUniform("useLighting", 0);
        shader.setUniform("useTexture", 1);
        whiteTexture.bind();
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0.02f, 0.02f, 0.05f, 1.0f));
        float barW = width * 0.4f, barH = 16f;
        float x = (width - barW) / 2f, y = height / 2f;
        drawText(shader, "MINICRAFT ALPHA", x, y - 60, 0.8f, UIPalette.RUSTIC_PARCHMENT);
        drawText(shader, main.loadingStatus.toUpperCase(), x, y - 25, 0.4f, UIPalette.TEXT_COLOR);
        drawTacticalFrame(shader, x - 12, y - 8, barW + 24, barH + 24);
        float fill = barW * main.loadingProgress;
        drawRectInternal(shader, x - 2, y - 2, barW + 4, barH + 4, new Vector4f(0, 0, 0, 0.85f));
        drawRectInternal(shader, x - 1, y - 1, barW + 2, barH + 2, UIPalette.RUSTIC_STONE);
        drawRectInternal(shader, x, y, fill, barH, UIPalette.RUSTIC_LIGHT_WOOD);
        String pct = String.format("%d%%", (int)(main.loadingProgress * 100));
        drawText(shader, pct, x + barW - 40, y - 25, 0.4f, UIPalette.TACT_BLUE);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (quadMesh != null) quadMesh.cleanup();
        if (textQuadMesh != null) textQuadMesh.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
    }

    public QuestLogUI getQuestLogUI() { return questLogUI; }
    public TextureRegistry getTextures() { return textures; }
}