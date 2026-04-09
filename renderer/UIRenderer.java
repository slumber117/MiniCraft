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

import static org.lwjgl.opengl.GL11.*;

public class UIRenderer {

    private Mesh quadMesh;
    private Mesh textQuadMesh;
    private FontTexture fontTexture;
    private Texture whiteTexture;
    private final TextureRegistry textures;
    
    // Premium Color Palette
    private final Vector4f healthColor      = new Vector4f(0.95f, 0.15f, 0.15f, 1.0f); // Crimson
    private final Vector4f healthColor2     = new Vector4f(0.60f, 0.05f, 0.05f, 1.0f); // Deep Ruby
    private final Vector4f hungerColor      = new Vector4f(0.95f, 0.65f, 0.05f, 1.0f); // Golden
    private final Vector4f hungerColor2     = new Vector4f(0.65f, 0.45f, 0.00f, 1.0f); // Warm Clay
    private final Vector4f thirstColor      = new Vector4f(0.15f, 0.75f, 0.95f, 1.0f); // Sky
    private final Vector4f thirstColor2     = new Vector4f(0.05f, 0.35f, 0.65f, 1.0f); // Deep Ocean
    private final Vector4f glassBgColor     = new Vector4f(0.00f, 0.00f, 0.00f, 0.5f); 
    private final Vector4f glassBorderColor = new Vector4f(1.00f, 1.00f, 1.00f, 0.25f);
    private final Vector4f highlightColor   = new Vector4f(1.00f, 1.00f, 1.00f, 0.15f);
    private final Vector4f textColor        = new Vector4f(1.00f, 1.00f, 1.00f, 1.0f);
    private final Vector4f crosshairColor   = new Vector4f(1.00f, 1.00f, 1.00f, 0.8f);

    public UIRenderer(TextureRegistry textures) {
        this.textures = textures;
        float[] positions = new float[] {
            0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0
        };
        float[] uvs = new float[] {
            0, 1, 1, 1, 1, 0, 0, 0
        };
        int[] indices = new int[] {
            0, 1, 2, 2, 3, 0
        };
        
        whiteTexture = textures.get("snow"); 
        quadMesh = new Mesh(positions, uvs, indices, whiteTexture);
        textQuadMesh = new Mesh(positions, uvs, indices, null);

        try {
            // High-resolution font for clarity
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
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
        shader.setUniform("useLighting", 0.0f); // UI is unlit

        if (main.inventoryOpen) {
            renderInventoryScreen(player, shader, width, height, main);
            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            return;
        }

        if (main.craftingOpen) {
            renderCraftingMenu(player, shader, width, height, main);
            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            return;
        }

        // 1. Draw Crosshair
        drawCrosshair(shader, width / 2f, height / 2f);

        // 2. Draw HUD Elements (Bottom Left)
        float margin = 30;
        float barWidth = 220;
        float barHeight = 14;
        float spacing = 32; // Increased spacing for icons
        float currentY = height - margin - barHeight;

        // Thirst
        drawPremiumBar(shader, margin, currentY, barWidth, barHeight, player.thirst / player.maxThirst, thirstColor, thirstColor2, "W");
        currentY -= spacing;
        
        // Hunger
        drawPremiumBar(shader, margin, currentY, barWidth, barHeight, player.hunger / player.maxHunger, hungerColor, hungerColor2, "F");
        currentY -= spacing;
        
        // Health
        drawPremiumBar(shader, margin, currentY, barWidth, barHeight, player.getHealth() / player.getMaxHealth(), healthColor, healthColor2, "V");

        // --- Offhand Slot Indicator ---
        if (player.inventory.getOffhandItem() != null) {
            drawRectInternal(shader, margin, currentY - 50, 40, 40, glassBgColor);
            drawText(shader, "L", margin + 14, currentY - 40, 0.8f); // Left hand icon
        }

        // 3. Temperature HUD (Top Right)
        drawTemperatureHUD(player, shader, width, margin);

        // 4. Hotbar
        renderHotbar(player, shader, width, height);
        
        if (player.damageFlashTimer > 0) {
            drawDamageVignette(shader, width, height, player.damageFlashTimer);
        }

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawDamageVignette(ShaderProgram shader, int width, int height, float alpha) {
        float intensity = alpha * 0.4f;
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(1.0f, 0, 0, intensity));
    }

    private void renderInventoryScreen(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        // Darken background
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.7f));
        
        float panelW = 800;
        float panelH = 500;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;
        
        // 1. Armor Pane (Left)
        float armorX = sx + 20;
        float armorY = sy + 60;
        drawRectInternal(shader, armorX, sy + 20, 200, panelH - 40, new Vector4f(0.1f, 0.1f, 0.1f, 0.8f));
        drawText(shader, "EQUIPMENT", armorX + 30, sy + 40, 1.0f);
        
        drawArmorSlot(shader, armorX + 50, armorY + 40,  "HELMET",     player.inventory.getHelmet());
        drawArmorSlot(shader, armorX + 50, armorY + 120, "CHESTPLATE", player.inventory.getChestplate());
        drawArmorSlot(shader, armorX + 50, armorY + 200, "LEGGINGS",   player.inventory.getLeggings());
        drawArmorSlot(shader, armorX + 50, armorY + 280, "BOOTS",      player.inventory.getBoots());
        
        drawText(shader, "DEFENSE: " + (int)(player.inventory.getTotalDefense() * 100) + "%", armorX + 30, sy + panelH - 60, 0.8f);

        // 2. Inventory Pane (Right)
        float invX = sx + 240;
        drawRectInternal(shader, invX, sy + 20, 540, panelH - 40, new Vector4f(0.15f, 0.15f, 0.15f, 0.8f));
        drawText(shader, "INVENTORY", invX + 30, sy + 40, 1.0f);
        
        Map<Item, Integer> items = player.inventory.getItems();
        List<Item> keys = new ArrayList<>(items.keySet());
        
        int cols = 6;
        float slotSize = 64;
        float gap = 15;
        
        for (int i = 0; i < keys.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            float x = invX + 40 + col * (slotSize + gap);
            float y = sy + 100 + row * (slotSize + gap);
            
            drawRectInternal(shader, x, y, slotSize, slotSize, new Vector4f(0,0,0,0.5f));
            drawItemIcon(shader, keys.get(i), x + 5, y + 5, slotSize - 10);
            drawText(shader, String.valueOf(items.get(keys.get(i))), x + 5, y + slotSize - 15, 0.6f);
        }

        // Draw Mouse Cursor Overlay (in UI space)
        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        drawRectInternal(shader, (float)mx[0], (float)my[0], 10, 10, new Vector4f(1,1,1,1));
    }

    private void drawArmorSlot(ShaderProgram shader, float x, float y, String label, Item item) {
        drawRectInternal(shader, x, y, 64, 64, new Vector4f(0,0,0,0.6f));
        if (item != null) {
            drawItemIcon(shader, item, x + 8, y + 8, 48);
        } else {
            drawText(shader, label.substring(0, 1), x + 25, y + 20, 1.0f, highlightColor);
        }
        drawText(shader, label, x + 75, y + 25, 0.5f);
    }

    private void drawItemIcon(ShaderProgram shader, Item item, float x, float y, float size) {
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
            // Generic fallback
            drawRectInternal(shader, x, y, size, size, new Vector4f(0.6f, 0.6f, 0.6f, 1.0f));
        }
    }

    private void renderCraftingMenu(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        // Darken background
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.75f));
        
        float menuW = 600;
        float menuH = 500;
        float startX = (width - menuW) / 2f;
        float startY = (height - menuH) / 2f;
        
        // 1. Draw Main Background (Glass Style)
        drawRectInternal(shader, startX, startY, menuW, menuH, new Vector4f(0.12f, 0.12f, 0.12f, 0.95f));
        drawRectInternal(shader, startX, startY, menuW, 2, glassBorderColor); 
        
        // 2. Render Tabs
        Recipe.Category[] cats = Recipe.Category.values();
        float tabW = menuW / cats.length;
        for (int i = 0; i < cats.length; i++) {
            float tx = startX + i * tabW;
            boolean active = (main.activeCategory == cats[i]);
            
            // Tab Background
            Vector4f tabColor = active ? new Vector4f(0.25f, 0.25f, 0.25f, 1.0f) : new Vector4f(0.15f, 0.15f, 0.15f, 0.8f);
            drawRectInternal(shader, tx, startY, tabW, 40, tabColor);
            if (active) drawRectInternal(shader, tx, startY + 38, tabW, 2, new Vector4f(1, 1, 0, 1));
            
            drawText(shader, cats[i].name(), tx + (tabW/2f) - (cats[i].name().length()*4), startY + 12, 0.8f, active ? textColor : highlightColor);
        }

        // 3. Render Recipe List
        List<Recipe> filtered = new java.util.ArrayList<>();
        for (Recipe r : main.craftingManager.getRecipes()) {
            if (r.getCategory() == main.activeCategory) filtered.add(r);
        }

        float listX = startX + 20;
        for (int i = 0; i < filtered.size(); i++) {
            Recipe r = filtered.get(i);
            float ry = startY + 60 + i * 40;
            boolean selected = (i == main.recipeIndex);
            
            if (selected) {
                drawRectInternal(shader, listX, ry, 360, 35, highlightColor);
                drawRectInternal(shader, listX - 2, ry, 4, 35, new Vector4f(1, 1, 0, 1));
            }
            
            drawText(shader, r.getName(), listX + 15, ry + 8, 1.0f, selected ? new Vector4f(1, 1, 0, 1) : textColor);
        }

        // 4. Recipe Details Pane (Right)
        if (main.recipeIndex >= 0 && main.recipeIndex < filtered.size()) {
            Recipe selectedRecipe = filtered.get(main.recipeIndex);
            float detailX = startX + 390;
            float detailY = startY + 60;
            
            drawRectInternal(shader, detailX, detailY, 190, menuH - 120, glassBgColor);
            drawText(shader, "REQUIRED:", detailX + 10, detailY + 10, 0.7f, highlightColor);
            
            int k = 0;
            boolean canCraft = true;
            for (Map.Entry<Item, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
                int owned = player.inventory.getCount(entry.getKey());
                boolean sufficient = owned >= entry.getValue();
                if (!sufficient) canCraft = false;
                
                Vector4f color = sufficient ? new Vector4f(0.4f, 1.0f, 0.4f, 1.0f) : new Vector4f(1.0f, 0.4f, 0.4f, 1.0f);
                drawText(shader, entry.getKey().getName(), detailX + 15, detailY + 40 + k*45, 0.6f, color);
                drawText(shader, owned + " / " + entry.getValue(), detailX + 15, detailY + 58 + k*45, 0.7f, color);
                k++;
            }
            
            float btnX = startX + menuW - 180;
            float btnY = startY + menuH - 70;
            drawRectInternal(shader, btnX, btnY, 160, 50, canCraft ? new Vector4f(0.2f, 0.6f, 0.2f, 0.9f) : new Vector4f(0.3f, 0.3f, 0.3f, 0.8f));
            drawText(shader, "CRAFT ITEM", btnX + 25, btnY + 15, 0.9f);
        }

        // Mouse Cursor
        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        drawRectInternal(shader, (float)mx[0], (float)my[0], 12, 12, new Vector4f(1,1,1,1));
    }

    private void renderWeather(ShaderProgram shader, int width, int height, minicraft.world.WeatherManager weather) {
        if (weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.CLEAR) return;

        float intensity = weather.getIntensity();
        boolean isSnow = (weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.SNOW || 
                         weather.getCurrentType() == minicraft.world.WeatherManager.WeatherType.BLIZZARD);
        
        Vector4f color;
        int count;
        
        switch (weather.getCurrentType()) {
            case THUNDERSTORM:    color = new Vector4f(0.4f, 0.4f, 0.7f, 0.6f); count = 200; break;
            case TORRENTIAL_RAIN: color = new Vector4f(0.5f, 0.5f, 0.8f, 0.8f); count = 400; break;
            case BLIZZARD:        color = new Vector4f(1.0f, 1.0f, 1.0f, 0.9f); count = 500; break;
            case HURRICANE:
            case CYCLONE:         color = new Vector4f(0.3f, 0.3f, 0.5f, 0.7f); count = 600; break;
            default:              color = isSnow ? new Vector4f(1,1,1,0.8f) : new Vector4f(0.5f, 0.6f, 1.0f, 0.4f);
                                  count = 100; break;
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
                float size = 2 + 3 * intensity;
                drawRectInternal(shader, x, y, size, size, color);
            }
        }
    }

    private void drawCrosshair(ShaderProgram shader, float cx, float cy) {
        float size = 15;
        float thickness = 2;
        // Vertical
        drawRectInternal(shader, cx - thickness/2f, cy - size/2f, thickness, size, crosshairColor);
        // Horizontal
        drawRectInternal(shader, cx - size/2f, cy - thickness/2f, size, thickness, crosshairColor);
    }

    private void drawPremiumBar(ShaderProgram shader, float x, float y, float w, float h, float fill, Vector4f c1, Vector4f c2, String icon) {
        float f = Math.max(0, Math.min(1, fill));
        
        // Pulse Effect at low vitals
        float pulse = 1.0f;
        if (f < 0.2f) {
            pulse = 0.8f + (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0)) * 0.4f;
        }

        // 1. Icon Backing Circle (Primitive)
        drawRectInternal(shader, x - 35, y - 5, 24, 24, glassBgColor);
        drawText(shader, icon, x - 28, y, 0.8f, new Vector4f(c1.x, c1.y, c1.z, pulse));

        // 2. Background Glass
        drawRectInternal(shader, x - 2, y - 2, w + 4, h + 4, glassBgColor);
        drawRectInternal(shader, x - 2, y - 2, w + 4, 1, glassBorderColor); 
        
        // 3. Fill (Gradient Style using two halves)
        drawRectInternal(shader, x, y, w * f, h * 0.5f, c1); // Top half
        drawRectInternal(shader, x, y + h*0.5f, w * f, h * 0.5f, c2); // Bottom half
        
        // 4. Glass Highlight
        drawRectInternal(shader, x, y, w * f, 2, highlightColor); // Top luster
    }

    private void drawTemperatureHUD(Player player, ShaderProgram shader, int screenWidth, float margin) {
        String tempText = String.format("%.1f°C", player.temperature);
        float textWidth = 80;
        float x = screenWidth - margin - textWidth;
        float y = margin;

        // Background Glass
        drawRectInternal(shader, x - 10, y - 5, textWidth + 20, 30, glassBgColor);
        drawRectInternal(shader, x - 10, y - 5, textWidth + 20, 1, glassBorderColor);

        Vector4f stateColor = textColor;
        if (player.tempState.equals("Cold")) stateColor = thirstColor;
        else if (player.tempState.equals("Too Warm")) stateColor = healthColor;

        drawText(shader, tempText, x, y + 2, 1.0f);
        drawText(shader, player.tempState, x, y + 22, 0.6f, stateColor);
    }

    private void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h, Vector4f color, String textureName) {
        Matrix4f model = new Matrix4f().identity().translate(x, y, 0).scale(w, h, 1);
        shader.setUniform("modelMatrix", model);
        shader.setUniform("colorTint", color);
        quadMesh.setUVs(new float[] { 0, 1, 1, 1, 1, 0, 0, 0 });
        quadMesh.render(textures.get(textureName));
    }

    private void drawRectInternal(ShaderProgram shader, float x, float y, float w, float h, Vector4f color) {
        drawRectInternal(shader, x, y, w, h, color, null);
    }

    private void drawText(ShaderProgram shader, String text, float x, float y, float scale) {
        drawText(shader, text, x, y, scale, textColor);
    }

    private void drawText(ShaderProgram shader, String text, float x, float y, float scale, Vector4f color) {
        if (fontTexture == null) return;
        
        // 1. Draw Multi-Pass Outline (Dark High-Contrast)
        float offset = 1.0f * scale;
        Vector4f outlineColor = new Vector4f(0, 0, 0, color.w * 0.9f);
        drawTextRaw(shader, text, x - offset, y,          scale, outlineColor);
        drawTextRaw(shader, text, x + offset, y,          scale, outlineColor);
        drawTextRaw(shader, text, x,          y - offset, scale, outlineColor);
        drawTextRaw(shader, text, x,          y + offset, scale, outlineColor);
        
        // 2. Draw Main Text (Light Gray Shell)
        Vector4f lightGray = new Vector4f(0.85f, 0.85f, 0.85f, color.w);
        drawTextRaw(shader, text, x, y, scale, lightGray);
    }

    private void drawTextRaw(ShaderProgram shader, String text, float x, float y, float scale, Vector4f color) {
        if (fontTexture == null) return;
        
        float currentX = x;
        shader.setUniform("colorTint", color);
        
        for (char c : text.toCharArray()) {
            FontTexture.CharInfo charInfo = fontTexture.getCharInfo(c);
            if (charInfo == null) continue;

            float w = charInfo.width * scale;
            float h = fontTexture.getHeight() * scale;
            
            float u1 = (float) charInfo.startX / fontTexture.getWidth();
            float u2 = (float) (charInfo.startX + charInfo.width) / fontTexture.getWidth();
            
            textQuadMesh.setUVs(new float[] { u1, 1, u2, 1, u2, 0, u1, 0 });
            
            Matrix4f model = new Matrix4f().identity().translate(currentX, y, 0).scale(w, h, 1);
            shader.setUniform("modelMatrix", model);
            textQuadMesh.render();
            
            currentX += w;
        }
    }

    public void renderHotbar(Player player, ShaderProgram shader, int width, int height) {
        float slotSize = 45;
        float padding = 10;
        int totalSlots = 9;
        float barWidth = (slotSize + padding) * totalSlots;
        float startX = (width - barWidth) / 2f;
        float marginBottom = 15;
        float y = height - marginBottom - slotSize;

        // Background Glass
        drawRectInternal(shader, startX - 5, y - 5, barWidth + 5, slotSize + 10, glassBgColor);
        drawRectInternal(shader, startX - 5, y - 5, barWidth + 5, 1, glassBorderColor);

        Map<minicraft.item.Item, Integer> items = player.inventory.getItems();
        java.util.List<minicraft.item.Item> keys = new java.util.ArrayList<>(items.keySet());
        int selectedIdx = player.inventory.getSelectedIndex();

        for (int i = 0; i < totalSlots; i++) {
            float slotX = startX + i * (slotSize + padding);
            
            // Highlight
            if (i == selectedIdx) {
                drawRectInternal(shader, slotX, y, slotSize, slotSize, new Vector4f(1, 1, 1, 0.3f));
            } else {
                drawRectInternal(shader, slotX, y, slotSize, slotSize, new Vector4f(0, 0, 0, 0.4f));
            }

            if (i < keys.size()) {
                minicraft.item.Item item = keys.get(i);
                int count = items.get(item);
                
                // Item Icon
                drawItemIcon(shader, item, slotX + 5, y + 5, slotSize - 10);
                
                // Count
                if (item.isBlock() && count > 1) {
                    drawText(shader, String.valueOf(count), slotX + 2, y + slotSize - 12, 0.8f);
                }
            }
        }
        
        // Show current selection name
        minicraft.item.Item selected = player.inventory.getSelectedItem();
        if (selected != null) {
            String name = selected.getName();
            drawText(shader, name, width / 2f - 30, y - 25, 0.7f);
        }
        
        // --- 5. Mining Progress Bar ---
        if (player.miningProgress > 0) {
            float pbW = 100;
            float pbH = 6;
            float pX = (width - pbW) / 2f;
            float pY = height / 2f + 20; // Just below crosshair
            
            drawRectInternal(shader, pX, pY, pbW, pbH, new Vector4f(0, 0, 0, 0.5f));
            drawRectInternal(shader, pX, pY, pbW * player.miningProgress, pbH, new Vector4f(0.4f, 1.0f, 0.4f, 0.9f));
        }
    }

    public void cleanup() {
        if (quadMesh != null) quadMesh.cleanup();
        if (textQuadMesh != null) textQuadMesh.cleanup();
        if (fontTexture != null) fontTexture.cleanup();
    }
}
