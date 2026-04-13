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
            // High-resolution premium font
            Font font = new Font(Font.MONOSPACED, Font.BOLD, 22);
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

        // --- INVENTORY V3 ---
        if (main.inventoryOpen) {
            renderInventoryScreenV3(player, shader, width, height, main);
            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            return;
        }

        if (main.chestOpen) {
            renderChestScreen(player, shader, width, height, main);
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

        if (main.shipConsoleOpen) {
            renderShipConsoleScreen(player, shader, width, height, main);
            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            return;
        }

        if (player.isRiding()) {
            renderPilotHUD(player, shader, width, height);
        } else {
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
        }

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawDamageVignette(ShaderProgram shader, int width, int height, float alpha) {
        float intensity = alpha * 0.4f;
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(1.0f, 0, 0, intensity));
    }

    private void renderChestScreen(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        if (main.activeChest == null) return;
        
        // Darken world
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.75f));
        
        float panelW = 800;
        float panelH = 550;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;
        
        drawRectInternal(shader, sx, sy, panelW, panelH, glassBgColor);
        drawRectInternal(shader, sx, sy, panelW, 2, glassBorderColor);
        drawText(shader, "LOOT CONTAINER", sx + 30, sy + 30, 1.2f, new Vector4f(0.4f, 1.0f, 0.4f, 1.0f));

        // 1. Render Chest Contents (Top 3x9)
        minicraft.item.ItemStack[] chestInv = main.activeChest.getMainInventory();
        float slotSize = 64;
        float gap = 15;
        float gridStartX = sx + (panelW - (slotSize + gap) * 9) / 2f;
        float chestStartY = sy + 80;

        drawText(shader, "TREASURE", gridStartX, chestStartY - 25, 0.6f, highlightColor);
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float x = gridStartX + col * (slotSize + gap);
            float y = chestStartY + row * (slotSize + gap);
            drawSlot(shader, x, y, slotSize, chestInv[i]);
        }

        // 2. Render Player Inventory (Bottom 3x9)
        minicraft.item.ItemStack[] playerInv = player.inventory.getMainInventory();
        float playerStartY = sy + 300;
        drawText(shader, "YOUR BAG", gridStartX, playerStartY - 25, 0.6f, highlightColor);
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float x = gridStartX + col * (slotSize + gap);
            float y = playerStartY + row * (slotSize + gap);
            drawSlot(shader, x, y, slotSize, playerInv[i]);
        }

        // 3. Shared Cursor ItemStack
        minicraft.item.ItemStack cursor = player.inventory.getCursorStack();
        if (cursor != null && !cursor.isEmpty()) {
            double[] mx = new double[1], my = new double[1];
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
            
            int[] winW = new int[1], winH = new int[1];
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(main.getWindow(), winW, winH);
            float scaleX = (float) width / Math.max(1, winW[0]);
            float scaleY = (float) height / Math.max(1, winH[0]);
            
            float curX = (float) mx[0] * scaleX - slotSize/2;
            float curY = (float) my[0] * scaleY - slotSize/2;
            
            drawItemIcon(shader, cursor.getItem(), curX + 5, curY + 5, slotSize - 10);
            if (cursor.getCount() > 1) {
                drawText(shader, String.valueOf(cursor.getCount()), curX + 10, curY + slotSize - 15, 0.8f);
            }
        }
    }

    private void renderInventoryScreenV3(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        // Darken world
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.75f));
        
        float panelW = 800;
        float panelH = 550;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;
        
        // --- 1. Main Background ---
        drawRectInternal(shader, sx, sy, panelW, panelH, glassBgColor);
        drawRectInternal(shader, sx, sy, panelW, 2, glassBorderColor);
        drawText(shader, "PLAYER INVENTORY", sx + 30, sy + 30, 1.2f, new Vector4f(0.9f, 0.9f, 0.4f, 1.0f));

        // --- 2. Main 3x9 Grid ---
        minicraft.item.ItemStack[] mainInv = player.inventory.getMainInventory();
        float slotSize = 64;
        float gap = 15;
        float invStartX = sx + (panelW - (slotSize + gap) * 9) / 2f;
        float invStartY = sy + 100;

        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            float x = invStartX + col * (slotSize + gap);
            float y = invStartY + row * (slotSize + gap);
            
            drawSlot(shader, x, y, slotSize, mainInv[i]);
        }

        // --- 3. Hotbar 1x9 Grid (Utilized in Inventory) ---
        minicraft.item.ItemStack[] hotbar = player.inventory.getHotbar();
        float hbY = sy + panelH - 95;
        drawText(shader, "HOTBAR", invStartX, hbY - 25, 0.7f, highlightColor);
        
        for (int i = 0; i < 9; i++) {
            float x = invStartX + i * (slotSize + gap);
            drawSlot(shader, x, hbY, slotSize, hotbar[i]);
            if (i == player.inventory.getSelectedIndex()) {
                drawRectInternal(shader, x - 2, hbY - 2, slotSize + 4, 3, new Vector4f(1, 1, 0, 1));
            }
        }

        // --- 4. Cursor ItemStack (Attached to Mouse) ---
        minicraft.item.ItemStack cursor = player.inventory.getCursorStack();
        if (cursor != null && !cursor.isEmpty()) {
            double[] mx = new double[1], my = new double[1];
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
            float curX = (float) mx[0] - slotSize/2;
            float curY = (float) my[0] - slotSize/2;
            drawItemIcon(shader, cursor.getItem(), curX + 5, curY + 5, slotSize - 10);
            if (cursor.getCount() > 1) {
                drawText(shader, String.valueOf(cursor.getCount()), curX + 10, curY + slotSize - 15, 0.8f);
            }
        }
    }

    private void renderShipConsoleScreen(Player player, ShaderProgram shader, int width, int height, minicraft.Main main) {
        drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0.05f, 0.1f, 0.85f)); // Deep cyber-blue tint
        
        float panelW = 900;
        float panelH = 600;
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;
        
        drawRectInternal(shader, sx, sy, panelW, panelH, glassBgColor);
        drawRectInternal(shader, sx, sy, panelW, 2, new Vector4f(0, 1, 1, 0.5f)); // Cyan border
        
        drawText(shader, "UNSC FLEET LOGISTICS NETWORK", sx + 30, sy + 30, 1.4f, new Vector4f(0, 1, 1, 1));
        drawText(shader, "DRYDOCK STATUS: STANDBY", sx + 30, sy + 65, 0.8f, highlightColor);
        
        // Dynamic Ship Registry
        List<ShipDefinition> ships = ShipRegistry.getInstance().getAll();
        
        float btnWidth = 260;
        float btnHeight = 400;
        float gap = 30;
        float startX = sx + (panelW - (Math.min(3, ships.size())*btnWidth + (Math.min(3, ships.size())-1)*gap)) / 2f;
        float startY = sy + 120;
        
        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        float mouseX = (float)mx[0];
        float mouseY = (float)my[0];
        
        for (int i = 0; i < ships.size(); i++) {
            ShipDefinition def = ships.get(i);
            float bx = startX + i * (btnWidth + gap);
            boolean hover = mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight;
            
            Vector4f bg = hover ? new Vector4f(0, 0.4f, 0.4f, 0.8f) : new Vector4f(0, 0.1f, 0.2f, 0.6f);
            drawRectInternal(shader, bx, startY, btnWidth, btnHeight, bg);
            drawRectInternal(shader, bx, startY, btnWidth, 2, new Vector4f(0, 1, 1, hover ? 1f : 0.3f));
            
            drawText(shader, def.displayName.toUpperCase(), bx + 15, startY + 20, 0.8f, new Vector4f(1,1,1,1));
            
            // Thumbnail / Icon Area
            drawRectInternal(shader, bx + 20, startY + 60, btnWidth - 40, btnWidth - 40, new Vector4f(0,0,0,0.5f));
            drawText(shader, "SCHEMATIC PREVIEW", bx + 35, startY + 160, 0.6f, new Vector4f(0.4f, 0.4f, 0.5f, 1));

            // Stats
            drawText(shader, "CLASS: " + def.shipClass.displayName, bx + 15, startY + btnWidth + 30, 0.65f, new Vector4f(0.8f, 0.8f, 1f, 1f));
            drawText(shader, "BLOCKS: " + def.getBlockCount(), bx + 15, startY + btnWidth + 55, 0.65f, new Vector4f(0.8f, 0.8f, 1f, 1f));
            drawText(shader, def.getDimensionsString(), bx + 15, startY + btnWidth + 80, 0.65f, new Vector4f(0.8f, 0.8f, 1f, 1f));
            
            // Description (Truncated)
            String desc = def.description;
            if (desc.length() > 60) desc = desc.substring(0, 57) + "...";
            drawText(shader, desc, bx + 15, startY + btnWidth + 110, 0.55f, new Vector4f(0.6f, 0.6f, 0.6f, 1f));
        }
        
        // Mouse Cursor
        drawRectInternal(shader, mouseX, mouseY, 12, 12, new Vector4f(0, 1, 1, 1));
    }

    private void drawSlot(ShaderProgram shader, float x, float y, float size, minicraft.item.ItemStack stack) {
        // Slot Shadow/Background
        drawRectInternal(shader, x, y, size, size, new Vector4f(0, 0, 0, 0.6f));
        
        if (stack != null && !stack.isEmpty()) {
            drawItemIcon(shader, stack.getItem(), x + 8, y + 8, size - 16);
            if (stack.getCount() > 1) {
                drawText(shader, String.valueOf(stack.getCount()), x + 5, y + size - 16, 0.7f);
            }
        }
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
        if (item == null) return;
        
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

    private void renderPilotHUD(Player player, ShaderProgram shader, int width, int height) {
        minicraft.entity.ship.ShipEntity ship = player.getRidingShip();
        if (ship == null) return;

        float hudW = 450;
        float hudH = 140;
        float sx = (width - hudW) / 2f;
        float sy = 40;

        // 1. Background Hull Glass
        drawRectInternal(shader, sx, sy, hudW, hudH, glassBgColor);
        drawRectInternal(shader, sx, sy, hudW, 1, glassBorderColor);
        drawRectInternal(shader, sx, sy + hudH - 1, hudW, 1, glassBorderColor);

        // 2. Telemetry Labels
        drawText(shader, "VESSEL: STALWART CLASS FRIGATE", sx + 20, sy + 15, 0.9f, new Vector4f(0, 1, 1, 1));
        drawText(shader, "STATUS: NEURAL LINK ACTIVE", sx + 20, sy + 40, 0.6f, new Vector4f(0.4f, 1, 0.4f, 1));
        
        // Thrust Meter
        float thrustLvl = ship.getThrustLevel(); // 0 to 1
        drawPremiumBar(shader, sx + 20, sy + 65, 120, 10, thrustLvl, thirstColor, thirstColor2, "A");
        drawText(shader, "THRUST", sx + 150, sy + 65, 0.6f, textColor);

        // Weapon Selection
        float wx = sx + hudW - 180;
        float wy = sy + 15;
        drawText(shader, "WEAPON SYSTEM", wx, wy, 0.7f, highlightColor);
        
        String[] wNames = {"MAC", "MISSILES", "PDW"};
        minicraft.entity.ship.ShipEntity.WeaponSystem active = ship.getActiveWeapon();
        
        for (int i = 0; i < 3; i++) {
            boolean isSel = (active.ordinal() == i);
            Vector4f color = isSel ? new Vector4f(1, 1, 0, 1) : highlightColor;
            drawText(shader, (isSel ? "> " : "  ") + wNames[i], wx, wy + 25 + i * 20, 0.7f, color);
        }

        // Heading & Altitude
        String head = String.format("HDG: %d°  ALT: %dM", (int)(ship.yaw % 360), (int)ship.position.y);
        drawText(shader, head, sx + 20, sy + hudH - 30, 0.8f, textColor);
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

        minicraft.item.ItemStack[] hotbar = player.inventory.getHotbar();
        int selectedIdx = player.inventory.getSelectedIndex();

        for (int i = 0; i < totalSlots; i++) {
            float slotX = startX + i * (slotSize + padding);
            
            // Highlight
            if (i == selectedIdx) {
                drawRectInternal(shader, slotX, y, slotSize, slotSize, new Vector4f(1, 1, 1, 0.3f));
                drawRectInternal(shader, slotX, y, slotSize, 2, new Vector4f(1,1,0,1)); // Selection top bar
            } else {
                drawRectInternal(shader, slotX, y, slotSize, slotSize, new Vector4f(0, 0, 0, 0.4f));
            }

            minicraft.item.ItemStack stack = hotbar[i];
            if (stack != null && !stack.isEmpty()) {
                // Item Icon
                drawItemIcon(shader, stack.getItem(), slotX + 5, y + 5, slotSize - 10);
                
                // Count
                if (stack.getCount() > 1) {
                    drawText(shader, String.valueOf(stack.getCount()), slotX + 2, y + slotSize - 12, 0.6f);
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
