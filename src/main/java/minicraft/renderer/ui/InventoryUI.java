package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.item.ItemStack;
import minicraft.item.ArmorItem;
import minicraft.item.Item;
import minicraft.renderer.ModelRegistry;
import minicraft.renderer.Mesh;
import minicraft.math.Matrix4f;
import minicraft.math.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class InventoryUI {
    private final Map<String, Vector3f> armorColors = new HashMap<>();

    public InventoryUI() {
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
        armorColors.put("NEODYMIUM", new Vector3f(0.50f, 0.00f, 0.50f));
        armorColors.put("MUSGRAVITE", new Vector3f(0.70f, 0.50f, 0.90f));
        armorColors.put("CLOTH", new Vector3f(0.70f, 0.60f, 0.50f));
        armorColors.put("PAINITE", new Vector3f(0.80f, 0.00f, 0.00f));
        armorColors.put("PRASEODYMIUM", new Vector3f(0.20f, 0.80f, 0.20f));
        armorColors.put("DYSPROSIUM", new Vector3f(1.00f, 0.80f, 0.00f));
        armorColors.put("ERBIUM", new Vector3f(1.00f, 0.20f, 0.60f));
        armorColors.put("LUTETIUM", new Vector3f(1.00f, 1.00f, 1.00f));
        armorColors.put("OBSIDIAN", new Vector3f(0.15f, 0.05f, 0.25f));
    }

    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        // Darken world
        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        final float SLOT = 58f;
        final float GAP = 8f;
        final float COLS = 9f;
        
        float gridW = COLS * SLOT + (COLS - 1) * GAP;
        float dollAreaW = 240f; 
        float panelW = gridW + dollAreaW + 80f; 
        float panelH = 4 * SLOT + 3 * GAP + 140f; 
        
        float sx = (width - panelW) / 2f;
        float sy = (height - panelH) / 2f;

        ui.drawTacticalFrame(shader, sx, sy, panelW, panelH);
        ui.drawText(shader, "RESOURCES & SATCHEL", sx + 22, sy + 22, 0.90f, UIPalette.RUSTIC_PARCHMENT);

        float[] mouse = ui.getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];

        // ── 1. Paper Doll Section (Left) ─────────────────────────────────────
        float dollX = sx + 28f;
        float dollY = sy + 64f;
        float dollW = 160f;
        float dollH = 220f;
        
        ui.drawRectInternal(shader, dollX, dollY, dollW, dollH, new Vector4f(0, 0, 0, 0.45f));
        ui.drawRectInternal(shader, dollX, dollY, dollW, 1, UIPalette.RUSTIC_BORDER);
        ui.drawRectInternal(shader, dollX, dollY+dollH, dollW, 1, UIPalette.RUSTIC_BORDER);
        
        renderPaperDoll(ui, player, shader, dollX + dollW/2, dollY + dollH - 30f, 75f);

        // ── 2. Armor Slots ──────────────────────────────────────────────────
        float armorX = dollX + dollW + 12f;
        float armorY = dollY;
        
        ui.drawArmorSlot(shader, armorX, armorY, "HELMET", player.inventory.getHelmet(), player.level);
        ui.drawArmorSlot(shader, armorX, armorY + SLOT + GAP, "CHEST", player.inventory.getChestplate(), player.level);
        ui.drawArmorSlot(shader, armorX, armorY + 2*(SLOT + GAP), "LEGS", player.inventory.getLeggings(), player.level);
        ui.drawArmorSlot(shader, armorX, armorY + 3*(SLOT + GAP), "BOOTS", player.inventory.getBoots(), player.level);

        // ── 3. Vitals Readout ──────────────────────────────────────────────
        float statsY = dollY + dollH + 18f;
        ui.drawText(shader, "VITAL SIGNS & TELEMETRY", dollX, statsY, 0.40f, UIPalette.TACT_ORANGE);
        
        float def = player.inventory.getTotalDefense() * 100f;
        float spd = (player.inventory.getTotalSpeedMod() - 1.0f) * 100f;
        float ins = player.inventory.getTotalInsulation() * 100f;
        
        ui.drawText(shader, String.format("ARMOR PROTECTION: +%.0f%%", def), dollX + 4, statsY + 18, 0.38f, 
                 def > 0 ? UIPalette.RUSTIC_PARCHMENT : new Vector4f(0.5f,0.5f,0.5f,1f));
        ui.drawText(shader, String.format("MOBILITY RATING: %s%.0f%%", spd >= 0 ? "+" : "", spd), dollX + 4, statsY + 34, 0.38f,
                 spd != 0 ? UIPalette.RUSTIC_PARCHMENT : new Vector4f(0.5f,0.5f,0.5f,1f));
        ui.drawText(shader, String.format("THERMAL SHIELD: +%.0f%%", ins), dollX + 4, statsY + 50, 0.38f,
                 ins > 0 ? UIPalette.RUSTIC_PARCHMENT : new Vector4f(0.5f,0.5f,0.5f,1f));

        // ── 4. Main Grid (Right) ─────────────────────────────────────────────
        float gridStartX = sx + dollAreaW + 50f;
        float mainGridY = sy + 64f;
        ItemStack[] mainInv = player.inventory.getMainInventory();

        ui.drawText(shader, "STORAGE", gridStartX, mainGridY - 16, 0.45f, UIPalette.RUSTIC_PARCHMENT);

        // Scrolling Logic
        float gridViewH = 3 * (SLOT + GAP);
        float scrollTrackX = gridStartX + gridW + 12f;
        float scrollTrackH = gridViewH;
        
        ui.drawRectInternal(shader, scrollTrackX, mainGridY, 10f, scrollTrackH, new Vector4f(0, 0, 0, 0.45f));
        ui.drawRectInternal(shader, scrollTrackX, mainGridY, 10f, 1f, UIPalette.RUSTIC_BORDER);
        ui.drawRectInternal(shader, scrollTrackX, mainGridY + scrollTrackH, 10f, 1f, UIPalette.RUSTIC_BORDER);

        float totalRows = 9f;
        float visibleRows = 3f;
        float handleH = (visibleRows / totalRows) * scrollTrackH;
        float maxScroll = (totalRows - visibleRows) * (SLOT + GAP);
        float handleOffset = (main.inventoryScroll / maxScroll) * (scrollTrackH - handleH);
        
        ui.drawRectInternal(shader, scrollTrackX + 2, mainGridY + handleOffset + 2, 6f, handleH - 4, UIPalette.TACT_ORANGE);

        glEnable(GL_SCISSOR_TEST);
        int sx_sc = (int)gridStartX;
        int sy_sc = (int)(height - (mainGridY + gridViewH));
        int sw_sc = (int)(gridW + 4);
        int sh_sc = (int)gridViewH;
        glScissor(sx_sc, sy_sc, sw_sc, sh_sc);

        for (int i = 0; i < 81; i++) {
            int col = i % 9, row = i / 9;
            float sx2 = gridStartX + col * (SLOT + GAP);
            float sy2 = mainGridY + row * (SLOT + GAP) - main.inventoryScroll;
            
            if (sy2 + SLOT > mainGridY - 100 && sy2 < mainGridY + gridViewH + 100) {
                boolean hover = ui.isHovered(mouseX, mouseY, sx2, sy2, SLOT, SLOT);
                ui.drawSlot(shader, sx2, sy2, SLOT, mainInv[i], hover, player.level);
            }
        }
        glDisable(GL_SCISSOR_TEST);

        // ── 5. Hotbar row ────────────────────────────────────────────────────
        float sepY = mainGridY + 3 * (SLOT + GAP) + 12f;
        ui.drawRectInternal(shader, gridStartX, sepY, gridW, 1f, UIPalette.RUSTIC_BORDER);

        float hotbarRowY = sepY + 20f;
        ItemStack[] hotbar = player.inventory.getHotbar();
        int selIdx = player.inventory.getSelectedIndex();

        ui.drawText(shader, "HOTBAR", gridStartX, hotbarRowY - 14, 0.45f, UIPalette.RUSTIC_PARCHMENT);

        for (int i = 0; i < 9; i++) {
            float sx2 = gridStartX + i * (SLOT + GAP);
            boolean sel = (i == selIdx);
            boolean hover = ui.isHovered(mouseX, mouseY, sx2, hotbarRowY, SLOT, SLOT);
            ui.drawSlot(shader, sx2, hotbarRowY, SLOT, hotbar[i], hover, player.level);
            if (sel) {
                ui.drawRectInternal(shader, sx2, hotbarRowY, SLOT, 3f, new Vector4f(1.0f, 0.95f, 0.0f, 1.0f));
            }
        }

        // ── 6. Tooltip Pass (Draws on top of everything) ─────────────────────
        ItemStack hoveredStack = null;
        // Check main inventory
        for (int i = 0; i < 81; i++) {
            int col = i % 9, row = i / 9;
            float sx2 = gridStartX + col * (SLOT + GAP);
            float sy2 = mainGridY + row * (SLOT + GAP) - main.inventoryScroll;
            if (sy2 + SLOT > mainGridY && sy2 < mainGridY + gridViewH) {
                if (ui.isHovered(mouseX, mouseY, sx2, sy2, SLOT, SLOT)) {
                    hoveredStack = mainInv[i];
                    break;
                }
            }
        }
        // Check hotbar
        if (hoveredStack == null) {
            for (int i = 0; i < 9; i++) {
                float sx2 = gridStartX + i * (SLOT + GAP);
                if (ui.isHovered(mouseX, mouseY, sx2, hotbarRowY, SLOT, SLOT)) {
                    hoveredStack = hotbar[i];
                    break;
                }
            }
        }
        // Check Armor
        if (hoveredStack == null) {
            ArmorItem[] pieces = { player.inventory.getHelmet(), player.inventory.getChestplate(), player.inventory.getLeggings(), player.inventory.getBoots() };
            for (int i = 0; i < 4; i++) {
                if (ui.isHovered(mouseX, mouseY, armorX, armorY + i * (SLOT + GAP), SLOT, SLOT)) {
                    if (pieces[i] != null) hoveredStack = new ItemStack(pieces[i], 1);
                    break;
                }
            }
        }

        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            Item item = hoveredStack.getItem();
            String name = item.getDisplayName();
            String tier = item.getTierInfo();
            
            float tw = Math.max(name.length() * 12f, (tier != null ? tier.length() * 10f : 0));
            float th = tier != null ? 50f : 32f;
            float tx = mouseX + 16, ty = mouseY + 16;
            
            // Bounds check
            if (tx + tw > width) tx = mouseX - tw - 16;
            if (ty + th > height) ty = mouseY - th - 16;
            
            ui.drawRectInternal(shader, tx, ty, tw + 20, th, new Vector4f(0,0,0,0.9f));
            ui.drawRectInternal(shader, tx, ty, tw + 20, 1, UIPalette.RUSTIC_BORDER);
            ui.drawRectInternal(shader, tx, ty + th, tw + 20, 1, UIPalette.RUSTIC_BORDER);
            
            ui.drawText(shader, name, tx + 10, ty + 8, 0.55f, UIPalette.TEXT_COLOR);
            if (tier != null) {
                ui.drawText(shader, tier, tx + 10, ty + 26, 0.40f, UIPalette.TACT_ORANGE);
            }
        }
    }

    private void renderPaperDoll(UIRenderer ui, Player player, ShaderProgram shader, float cx, float cy, float scale) {
        float rotation = 160f + (float)Math.sin(System.currentTimeMillis() / 1000.0) * 20f;
        float pulse = (float)Math.sin(System.currentTimeMillis() / 400.0) * 0.02f;
        
        Matrix4f baseModelMatrix = new Matrix4f().identity()
                .translate(cx, cy, 100f) 
                .scale(scale * (1f + pulse), -scale * (1f + pulse), scale)
                .rotateY((float) Math.toRadians(rotation));
        
        shader.setUniform("modelMatrix", baseModelMatrix);
        shader.setUniform("useLighting", 1.0f);
        shader.setUniform("sunBrightness", 1.0f);
        shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
        
        Mesh human = ModelRegistry.getModel("character_full"); 
        if (human != null) {
            human.render(ui.getTextures().get("char_farmer").getTexture()); 

            ArmorItem[] pieces = {
                player.inventory.getHelmet(), 
                player.inventory.getChestplate(), 
                player.inventory.getLeggings(), 
                player.inventory.getBoots()
            };

            for (ArmorItem piece : pieces) {
                if (piece != null) {
                    Vector3f color = armorColors.get(piece.getTierName().toUpperCase());
                    if (color == null) color = new Vector3f(1, 1, 1);
                    shader.setUniform("colorTint", new Vector4f(color.x, color.y, color.z, 0.75f));
                    Matrix4f shellMatrix = new Matrix4f(baseModelMatrix).scale(1.04f, 1.04f, 1.04f);
                    shader.setUniform("modelMatrix", shellMatrix);
                    human.render(ui.getTextures().get("alloy_plate").getTexture());
                }
            }

            minicraft.item.Item held = player.inventory.getSelectedItem();
            if (held instanceof minicraft.item.ToolItem) {
                minicraft.item.ToolItem tool = (minicraft.item.ToolItem) held;
                String toolName = tool.getName();
                String modelId = null;
                if (toolName.contains("Wood")) modelId = "pickaxe_wooden";
                else if (toolName.contains("Stone")) modelId = "pickaxe_stone";
                else if (toolName.contains("Iron")) modelId = "pickaxe_iron";
                else if (toolName.contains("Gold")) modelId = "pickaxe_gold";
                else if (toolName.contains("Diamond")) modelId = "pickaxe_diamond";
                else if (toolName.contains("Neodymium")) modelId = "pickaxe_neodymium";
                else if (toolName.contains("Musgravite")) modelId = "pickaxe_musgravite";
                else if (toolName.contains("Painite")) modelId = "pickaxe_painite";
                else if (toolName.contains("Uranium")) modelId = "pickaxe_uranium";
                else if (toolName.contains("Praseodymium")) modelId = "pickaxe_praseodymium";
                else if (toolName.contains("Dysprosium")) modelId = "pickaxe_dysprosium";
                else if (toolName.contains("Erbium")) modelId = "pickaxe_erbium";
                else if (toolName.contains("Lutetium")) modelId = "pickaxe_lutetium";

                if (modelId != null) {
                    Mesh toolMesh = ModelRegistry.getModel(modelId);
                    if (toolMesh != null) {
                        Matrix4f toolMatrix = new Matrix4f(baseModelMatrix)
                                .translate(0.35f, -0.6f, 0.5f)
                                .rotateZ((float) Math.toRadians(135f))
                                .scale(0.85f, 0.85f, 0.85f);
                        shader.setUniform("modelMatrix", toolMatrix);
                        if (toolName.contains("Diamond")) {
                            shader.setUniform("colorTint", new Vector4f(0.5f, 0.95f, 1.0f, 1.0f));
                        } else {
                            shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
                        }
                        toolMesh.render(ui.getTextures().get("alloy_plate").getTexture());
                    }
                }
            }
        }
        shader.setUniform("colorTint", new Vector4f(1f, 1f, 1f, 1f));
        shader.setUniform("useLighting", 0.0f);
    }
}
