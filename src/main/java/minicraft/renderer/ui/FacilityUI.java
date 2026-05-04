package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.item.ItemStack;
import minicraft.world.Block;

public class FacilityUI {
    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        if (main.activeFacility == null)
            return;

        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        float panelW = 680f, panelH = 580f;
        float sx = (width - panelW) / 2f, sy = (height - panelH) / 2f - 80f;

        Block b = main.getWorld().getBlock(main.activeFacX, main.activeFacY, main.activeFacZ);
        String title = b.getInteractionLabel();
        if (title.isEmpty())
            title = main.furnaceOpen ? "INDUSTRIAL SMELTER" : "HIGH-EFFICIENCY COOKER";
        Vector4f titleCol = (b == Block.COOKER) ? UIPalette.TACT_BLUE
                : (b == Block.ALLOY_FORGE ? UIPalette.TACT_GREEN : UIPalette.TACT_ORANGE);

        ui.drawTacticalFrame(shader, sx, sy, panelW, panelH);
        ui.drawText(shader, title, sx + 22, sy + 22, 1.0f, titleCol);

        float cx = sx + panelW / 2f, cy = sy + 180f, slotSize = 72f;

        ui.drawSlot(shader, cx - 170, cy - 36, slotSize, main.activeFacility.getSlot(0));
        ui.drawText(shader, "INPUT", cx - 170, cy - 52, 0.52f, UIPalette.RUSTIC_PARCHMENT);

        ui.drawSlot(shader, cx - 36, cy + 45, slotSize, main.activeFacility.getSlot(1));
        ui.drawText(shader, "FUEL", cx - 36, cy + 36, 0.52f, UIPalette.RUSTIC_PARCHMENT);

        float fuelRatio = main.activeFacility.getFuelRatio();
        float heatPulse = main.activeFacility.isActive
                ? 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 150.0)
                : 1.0f;
        Vector4f fuelCol = main.furnaceOpen ? new Vector4f(1.0f, 0.4f * heatPulse, 0, 0.9f)
                : new Vector4f(0.2f, 0.6f, 1.0f, 0.9f);
        ui.drawRectInternal(shader, cx - 36, cy + 132, slotSize * fuelRatio, 6, fuelCol);
        ui.drawRectInternal(shader, cx - 36, cy + 132, slotSize, 1, UIPalette.RUSTIC_BORDER);

        if (main.activeFacility.isActive) {
            float flameAlpha = 0.4f + 0.6f * (float) Math.random();
            ui.drawRectInternal(shader, cx - 12, cy + 12, 24, 24, new Vector4f(1.0f, 0.5f, 0, flameAlpha * 0.4f));
            ui.drawRectInternal(shader, cx - 6, cy + 18, 12, 12, new Vector4f(1.0f, 0.8f, 0, flameAlpha));
        }

        ui.drawSlot(shader, cx + 98, cy - 36, slotSize, main.activeFacility.getSlot(2));
        ui.drawText(shader, "OUTPUT", cx + 98, cy - 52, 0.52f, UIPalette.RUSTIC_PARCHMENT);

        float prog = main.activeFacility.getProgress();
        ui.drawRectInternal(shader, cx - 66, cy, 130, 8, new Vector4f(1, 1, 1, 0.10f));
        if (prog > 0) {
            Vector4f progCol = new Vector4f(titleCol.x, titleCol.y, titleCol.z,
                    0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 200.0));
            ui.drawRectInternal(shader, cx - 66, cy, 130 * prog, 8, progCol);
            ui.drawText(shader, (int) (prog * 100) + "%", cx - 14, cy + 18, 0.58f, UIPalette.TEXT_COLOR);
        }

        final float ISLOT = 54f, IGAP = 6f;
        float invStartX = sx + (panelW - (9 * ISLOT + 8 * IGAP)) / 2f, invStartY = sy + 300f;
        ui.drawText(shader, "INVENTORY", invStartX, invStartY - 16, 0.52f, UIPalette.RUSTIC_PARCHMENT);
        ItemStack[] pInv = player.inventory.getMainInventory();
        for (int i = 0; i < 27; i++) {
            ui.drawSlot(shader, invStartX + (i % 9) * (ISLOT + IGAP), invStartY + (i / 9) * (ISLOT + IGAP), ISLOT,
                    pInv[i]);
        }
        float hotStartY = invStartY + 3 * (ISLOT + IGAP) + 18f;
        ui.drawText(shader, "HOTBAR", invStartX, hotStartY - 14, 0.52f, UIPalette.RUSTIC_PARCHMENT);
        ItemStack[] hInv = player.inventory.getHotbar();
        for (int i = 0; i < 9; i++) {
            ui.drawSlot(shader, invStartX + i * (ISLOT + IGAP), hotStartY, ISLOT, hInv[i]);
        }
    }
}
