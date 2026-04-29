package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.world.Block;
import minicraft.item.ItemStack;
import minicraft.item.Item;

public class HUD {
    public void renderPlayHUD(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        float hotbarTop = height - 14f - 42f;
        float xpBarTop = hotbarTop - 10f - 34f;
        float xpLabelTop = xpBarTop - 13f;
        float statTop = xpLabelTop - 36f - (12f * 2 + 10f + 8f * 2 + 6f);
        float marginLeft = 22f;

        renderHotbar(ui, player, shader, width, height, hotbarTop);
        renderXPBar(ui, player, shader, width, xpBarTop, xpLabelTop);

        float panelX = marginLeft - 8f, panelW = 210f + 40f + 16f, panelH = 12f * 2 + 10f + 16f + 6f;
        ui.drawTacticalFrame(shader, panelX, statTop, panelW, panelH);

        float barX = marginLeft + 32f, hungBarY = statTop + 8f + 4f;
        
        // --- Radiation Bar ---
        if (player.radiationLevel > 0) {
            float radY = statTop - 32f;
            ui.drawTacticalFrame(shader, panelX, radY, panelW, 30f);
            float pulse = 0.8f + 0.2f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0));
            Vector4f radCol = new Vector4f(0.2f, 1.0f, 0.1f, pulse);
            ui.drawPremiumBar(shader, barX, radY + 8f, 210f, 10f, Math.min(1.0f, player.radiationLevel / 100f), radCol, new Vector4f(0.05f, 0.3f, 0.05f, 1.0f), "R");
            ui.drawText(shader, "RADS", barX - 28, radY + 8, 0.45f, radCol);
        }

        ui.drawPremiumBar(shader, barX, hungBarY, 210f, 12f, player.hunger / player.maxHunger, UIPalette.HUNGER_COLOR, UIPalette.HUNGER_COLOR_DARK, "F");
        ui.drawPremiumBar(shader, barX, hungBarY + 12f + 10f, 210f, 12f, player.getHealth() / player.getMaxHealth(), UIPalette.HEALTH_COLOR, UIPalette.HEALTH_COLOR_DARK, "V");

        if (player.inventory.getOffhandItem() != null) {
            float ohX = panelX + panelW + 6f, ohY = statTop + (panelH - 36f) / 2f;
            ui.drawRectInternal(shader, ohX, ohY, 36, 36, UIPalette.RUSTIC_BG);
            ui.drawItemIcon(shader, player.inventory.getOffhandItem(), ohX + 4, ohY + 4, 28);
        }

        ui.drawTacticalFrame(shader, 14, 14, 188, 68);
        ui.drawText(shader, "LOCAL NAVIGATION", 26, 26, 0.38f, UIPalette.RUSTIC_PARCHMENT);
        Vector4f lightGray = new Vector4f(0.72f, 0.70f, 0.68f, 1f);
        ui.drawText(shader, String.format("X:%-6d  Y:%-6d", (int) player.position.x, (int) player.position.y), 26, 44, 0.45f, lightGray);
        ui.drawText(shader, String.format("Z:%-6d", (int) player.position.z), 26, 59, 0.45f, lightGray);
        ui.drawText(shader, main.getWorld().getBiome((int) player.position.x, (int) player.position.z).displayName.toUpperCase(), 26, 74, 0.35f, new Vector4f(0.55f, 0.52f, 0.48f, 1f));

        drawTemperatureHUD(ui, player, shader, width, 20f);

        if (main.focusedBlock != null && main.focusedBlock != Block.AIR) {
            drawFocusedBlockHUD(ui, shader, width, 72f, main.focusedBlock);
        }

        if (main.activeStatusMessage != null && !main.activeStatusMessage.isEmpty()) {
            drawStatusMessage(ui, shader, width, height, main.activeStatusMessage, main.statusMessageTimer);
        }

        float hpPct = player.getHealth() / player.getMaxHealth();
        if (hpPct < 1.0f && player.damageFlashTimer > 0 && hpPct >= 0.10f) {
            ui.drawDamageVignette(shader, width, height, player.damageFlashTimer * 0.5f);
        } else if (hpPct < 0.10f) {
            float pulse = 0.20f + 0.12f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 400.0));
            ui.drawDamageVignette(shader, width, height, pulse);
        }
    }

    public void renderHotbar(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, float topY) {
        float slotS = 42f, gap = 6f, stripW = 9 * (slotS + gap) - gap;
        float startX = (width - stripW) / 2f;
        ui.drawRectInternal(shader, startX - 4, topY - 4, stripW + 8, slotS + 8, UIPalette.RUSTIC_BG);
        ui.drawRectInternal(shader, startX - 4, topY - 4, stripW + 8, 1f, UIPalette.RUSTIC_BORDER);

        ItemStack[] hotbar = player.inventory.getHotbar();
        int sel = player.inventory.getSelectedIndex();
        for (int i = 0; i < 9; i++) {
            float sx = startX + i * (slotS + gap);
            if (i == sel) {
                ui.drawRectInternal(shader, sx, topY, slotS, slotS, new Vector4f(1, 1, 1, 0.28f));
                ui.drawRectInternal(shader, sx, topY, slotS, 3f, new Vector4f(1.0f, 0.95f, 0.0f, 1.0f));
            } else {
                ui.drawRectInternal(shader, sx, topY, slotS, slotS, new Vector4f(0, 0, 0, 0.45f));
            }
            if (hotbar[i] != null && !hotbar[i].isEmpty()) {
                ui.drawItemIcon(shader, hotbar[i].getItem(), sx + 5, topY + 5, slotS - 10);
                if (hotbar[i].getCount() > 1) ui.drawText(shader, String.valueOf(hotbar[i].getCount()), sx + 3, topY + slotS - 11, 0.52f);
            }
        }
        Item selected = player.inventory.getSelectedItem();
        if (selected != null) {
            String name = selected.getName();
            ui.drawText(shader, name, (width - name.length() * 7.5f) / 2f, topY - 16f, 0.6f, UIPalette.TEXT_COLOR);
        }
        if (player.miningProgress > 0) {
            float pbW = 90f, pbH = 5f, px = (width-pbW)/2f, py = height/2f+22f;
            ui.drawRectInternal(shader, px, py, pbW, pbH, new Vector4f(0, 0, 0, 0.55f));
            ui.drawRectInternal(shader, px, py, pbW * player.miningProgress, pbH, new Vector4f(0.38f, 0.95f, 0.38f, 0.95f));
        }
        if (player.eatingProgress > 0) {
            float pbW = 90f, pbH = 5f, px = (width-pbW)/2f, py = height/2f+22f;
            ui.drawRectInternal(shader, px, py, pbW, pbH, new Vector4f(0, 0, 0, 0.55f));
            ui.drawRectInternal(shader, px, py, pbW * player.eatingProgress, pbH, new Vector4f(1.0f, 0.75f, 0.0f, 1.0f)); // Gold/Food color
        }
    }

    private void renderXPBar(UIRenderer ui, Player player, ShaderProgram shader, int width, float barTop, float labelTop) {
        float barW = 200f, x = (width - barW) / 2f, prog = Math.max(0, Math.min(1, player.xp / player.xpToNextLevel));
        ui.drawText(shader, "RANK " + player.level, x, labelTop, 0.40f, UIPalette.RUSTIC_PARCHMENT);
        ui.drawRectInternal(shader, x, barTop, barW, 10f, new Vector4f(0, 0, 0, 0.65f));
        float fill = barW * prog;
        ui.drawRectInternal(shader, x, barTop, fill, 10f * 0.55f, UIPalette.TACT_BLUE);
        ui.drawRectInternal(shader, x, barTop + 10f * 0.55f, fill, 10f * 0.45f, new Vector4f(0.06f, 0.45f, 0.85f, 1.0f));
        ui.drawRectInternal(shader, x, barTop, fill, 2f, UIPalette.BAR_SHINE);
        ui.drawRectInternal(shader, x, barTop, barW, 1f, UIPalette.RUSTIC_BORDER);
    }

    private void drawTemperatureHUD(UIRenderer ui, Player player, ShaderProgram shader, int screenWidth, float margin) {
        float pW = 150f, pH = 48f, x = screenWidth - margin - pW, y = margin;
        ui.drawTacticalFrame(shader, x, y, pW, pH);
        ui.drawText(shader, String.format("%.1f\u00b0C", player.temperature), x + 10, y + 14, 0.85f, new Vector4f(0.65f, 0.65f, 0.65f, 1f));
        Vector4f col; String state = player.tempState;
        if (state.contains("Hypothermia")) col = new Vector4f(0.75f, 0.0f, 1.0f, 1.0f);
        else if (state.equals("Cold")) col = UIPalette.TACT_BLUE;
        else if (state.equals("Too Warm")) col = UIPalette.HEALTH_COLOR;
        else if (state.equals("Warm")) col = UIPalette.TACT_ORANGE;
        else col = UIPalette.TACT_GREEN;
        ui.drawText(shader, state.toUpperCase(), x + 10, y + 33, 0.38f, col);
    }

    private void drawStatusMessage(UIRenderer ui, ShaderProgram shader, int width, int height, String msg, float timer) {
        float scale = 0.55f, textW = msg.length() * 14f * scale, x = (width - textW) / 2f, y = 28f;
        ui.drawText(shader, msg.toUpperCase(), x, y, scale, new Vector4f(1f, 1f, 1f, Math.min(1.0f, timer * 2.0f)));
    }

    private void drawFocusedBlockHUD(UIRenderer ui, ShaderProgram shader, int screenWidth, float y, Block block) {
        float pW = 188f, pH = 46f, x = screenWidth - 20f - pW;
        ui.drawTacticalFrame(shader, x, y, pW, pH);
        ui.drawText(shader, "TARGET IDENTIFIED", x + 10, y + 10, 0.35f, UIPalette.TACT_ORANGE);
        ui.drawText(shader, block.getFriendlyName().toUpperCase(), x + 10, y + 25, 0.45f, new Vector4f(0.65f, 0.65f, 0.65f, 1f));
    }
}
