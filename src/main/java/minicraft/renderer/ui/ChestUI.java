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

        // Wooden Bookshelf Background
        ui.drawTexture(shader, "chest_ui_background", 0, 0, width, height, new Vector4f(1, 1, 1, 1));
        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.4f)); // Dim slightly

        final float SLOT = 56f, GAP = 8f;
        float gridW = 9 * SLOT + 8 * GAP;
        float panelW = gridW + 60f, panelH = 580f;
        float sx = (width - panelW) / 2f, sy = (height - panelH) / 2f;

        ui.drawTacticalFrame(shader, sx, sy, panelW, panelH);
        ui.drawText(shader, "ANCIENT ARCHIVE", sx + 18, sy + 12, 0.85f, UIPalette.RUSTIC_PARCHMENT);

        float[] mouse = ui.getScaledMouse(main, width, height);
        float mouseX = mouse[0], mouseY = mouse[1];
        float gx = sx + (panelW - gridW) / 2f;

        // --- Container Inventory ---
        ItemStack[] chestInv = main.activeChest.getMainInventory();
        float chestY = sy + 64f;
        ui.drawText(shader, "CHEST STORAGE", gx, chestY - 14, 0.42f, UIPalette.RUSTIC_PARCHMENT);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP), ty = chestY + row * (SLOT + GAP);
            boolean hovered = ui.isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT);
            ui.drawSlot(shader, tx, ty, SLOT, chestInv[i], hovered, player.level);
            
            // Scroll to Transfer (Chest -> Player)
            if (hovered && main.scrollDelta != 0 && chestInv[i] != null) {
                player.inventory.add(chestInv[i].getItem(), 1);
                chestInv[i].setCount(chestInv[i].getCount() - 1);
                if (chestInv[i].getCount() <= 0) chestInv[i] = null;
            }
        }

        float sepY = chestY + 3 * (SLOT + GAP) + 12f;
        ui.drawRectInternal(shader, gx, sepY, gridW, 1f, UIPalette.RUSTIC_BORDER);

        // --- Player Inventory ---
        ItemStack[] playerInv = player.inventory.getMainInventory();
        float playerY = sepY + 20f;
        ui.drawText(shader, "PERSONAL SATCHEL", gx, playerY - 14, 0.42f, UIPalette.RUSTIC_PARCHMENT);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float tx = gx + col * (SLOT + GAP), ty = playerY + row * (SLOT + GAP);
            boolean hovered = ui.isHovered(mouseX, mouseY, tx, ty, SLOT, SLOT);
            ui.drawSlot(shader, tx, ty, SLOT, playerInv[i], hovered, player.level);

            // Scroll to Transfer (Player -> Chest)
            if (hovered && main.scrollDelta != 0 && playerInv[i] != null) {
                main.activeChest.add(playerInv[i].getItem(), 1);
                playerInv[i].setCount(playerInv[i].getCount() - 1);
                if (playerInv[i].getCount() <= 0) playerInv[i] = null;
            }
        }
    }
}
