package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.item.ItemStack;

public class ChestUI {
    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        if (main.activeChest == null) return;

        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.78f));

        final float SLOT = 56f, GAP = 8f;
        float gridW = 9 * SLOT + 8 * GAP;
        float panelW = gridW + 60f, panelH = 580f;
        float sx = (width - panelW) / 2f, sy = (height - panelH) / 2f;

        ui.drawTacticalFrame(shader, sx, sy, panelW, panelH);
        ui.drawText(shader, "STORAGE CHEST", sx + 18, sy + 12, 0.85f, UIPalette.RUSTIC_PARCHMENT);

        float[] mouse = ui.getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];
        float gx = sx + (panelW - gridW) / 2f;

        ItemStack[] chestInv = main.activeChest.getMainInventory();
        float chestY = sy + 64f;
        ui.drawText(shader, "CONTAINER", gx, chestY - 14, 0.42f, UIPalette.RUSTIC_PARCHMENT);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP), ty = chestY + row * (SLOT + GAP);
            ui.drawSlot(shader, tx, ty, SLOT, chestInv[i], ui.isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT));
        }

        float sepY = chestY + 3 * (SLOT + GAP) + 12f;
        ui.drawRectInternal(shader, gx, sepY, gridW, 1f, UIPalette.RUSTIC_BORDER);

        ItemStack[] playerInv = player.inventory.getMainInventory();
        float playerY = sepY + 20f;
        ui.drawText(shader, "YOUR BAG", gx, playerY - 14, 0.42f, UIPalette.RUSTIC_PARCHMENT);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP), ty = playerY + row * (SLOT + GAP);
            ui.drawSlot(shader, tx, ty, SLOT, playerInv[i], ui.isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT));
        }
    }
}
