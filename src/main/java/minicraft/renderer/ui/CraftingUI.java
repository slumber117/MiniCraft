package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.item.Recipe;
import minicraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class CraftingUI {

    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0, 0, 0.88f));

        final float MENU_W    = 700f, MENU_H = 540f;
        final float ICON_SIZE = 56f,  ICON_GAP = 8f;
        final int   COLS      = 7;
        final float GRID_OFF_X = 14f, GRID_OFF_Y = 90f;
        final float DETAIL_W  = 182f;
        float sx = (width  - MENU_W) / 2f;
        float sy = (height - MENU_H) / 2f;

        ui.drawTacticalFrame(shader, sx, sy, MENU_W, MENU_H);
        String title = main.blacksmithOpen ? "BLACKSMITH STATION" : "CRAFTING FORGE";
        ui.drawText(shader, title, sx + 18, sy + 12, 0.75f, UIPalette.RUSTIC_PARCHMENT);

        // Category tabs (Hide if Blacksmith)
        Recipe.Category[] cats = Recipe.Category.GENERAL;
        float gridAreaW = COLS * ICON_SIZE + (COLS - 1) * ICON_GAP;
        if (!main.blacksmithOpen) {
            float tabW = gridAreaW / cats.length;
            for (int i = 0; i < cats.length; i++) {
                float tx = sx + GRID_OFF_X + i * (tabW + 2f);
                float ty = sy + 48f;
                boolean active = (main.activeCategory == cats[i]);
                
                // Tab Background
                ui.drawRectInternal(shader, tx, ty, tabW - 2, 28f,
                        active ? UIPalette.RUSTIC_LIGHT_WOOD
                               : new Vector4f(0.22f, 0.20f, 0.18f, 1.0f));
                
                // Tab Accent
                if (active) {
                    ui.drawRectInternal(shader, tx, ty + 28f, tabW - 2, 2f, UIPalette.RUSTIC_PARCHMENT);
                } else {
                    ui.drawRectInternal(shader, tx, ty + 28f, tabW - 2, 1f, new Vector4f(0.35f, 0.32f, 0.30f, 1.0f));
                }

                String lbl = cats[i].name();
                ui.drawText(shader, lbl, tx + tabW / 2f - lbl.length() * 3.5f, ty + 8f, 0.65f,
                        active ? UIPalette.RUSTIC_PARCHMENT : new Vector4f(0.65f, 0.62f, 0.58f, 1f));
            }
        } else {
            ui.drawText(shader, "SPECIALIZED BLUEPRINTS", sx + GRID_OFF_X, sy + 48f, 0.55f, UIPalette.TACT_ORANGE);
        }

        List<Recipe> filtered = new ArrayList<>();
        Recipe.Category filter = main.blacksmithOpen ? Recipe.Category.BLACKSMITH : main.activeCategory;
        for (Recipe r : main.craftingManager.getRecipes())
            if (r.getCategory() == filter)
                filtered.add(r);

        float gx = sx + GRID_OFF_X;
        float gy = sy + GRID_OFF_Y;
        float gridAreaH = MENU_H - GRID_OFF_Y - 20f;

        glEnable(GL_SCISSOR_TEST);
        int scX = (int) (gx * ((float) main.getFramebufferW() / width));
        int scY = (int) ((height - (gy + gridAreaH)) * ((float) main.getFramebufferH() / height));
        int scW = (int) (gridAreaW * ((float) main.getFramebufferW() / width));
        int scH = (int) (gridAreaH * ((float) main.getFramebufferH() / height));
        glScissor(scX, scY, scW, scH);

        for (int i = 0; i < filtered.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            float ix = gx + col * (ICON_SIZE + ICON_GAP);
            float iy = gy + row * (ICON_SIZE + ICON_GAP) - main.recipeScrollOffset;

            boolean selected = (i == main.recipeIndex);
            boolean canAfford = true;
            for (Map.Entry<Item, Integer> e : filtered.get(i).getIngredients().entrySet())
                if (player.inventory.getCount(e.getKey()) < e.getValue()) { canAfford = false; break; }

            ui.drawRectInternal(shader, ix, iy, ICON_SIZE, ICON_SIZE,
                    selected ? new Vector4f(0.10f, 0.32f, 0.60f, 0.95f)
                             : new Vector4f(0.05f, 0.05f, 0.10f, 0.88f));

            float pad = 7f;
            ui.drawItemIcon(shader, filtered.get(i).getResult(), ix + pad, iy + pad, ICON_SIZE - pad * 2);

            if (!canAfford)
                ui.drawRectInternal(shader, ix, iy, ICON_SIZE, ICON_SIZE, new Vector4f(0.65f, 0.0f, 0.0f, 0.45f));

            float bw = selected ? 2f : 1f;
            Vector4f bc = selected ? UIPalette.TACT_BLUE
                        : (canAfford ? new Vector4f(0.10f, 0.65f, 0.80f, 0.45f)
                                     : new Vector4f(0.55f, 0.10f, 0.10f, 0.50f));
            ui.drawRectInternal(shader, ix, iy, ICON_SIZE, bw, bc);
            ui.drawRectInternal(shader, ix, iy+ICON_SIZE-bw, ICON_SIZE, bw, bc);
            ui.drawRectInternal(shader, ix, iy, bw, ICON_SIZE, bc);
            ui.drawRectInternal(shader, ix+ICON_SIZE-bw, iy, bw, ICON_SIZE, bc);

            ui.drawRectInternal(shader, ix + ICON_SIZE - 8f, iy, 8f, 8f,
                    canAfford ? UIPalette.TACT_GREEN : new Vector4f(0.90f, 0.18f, 0.18f, 1f));
        }
        glDisable(GL_SCISSOR_TEST);

        // Scrollbar
        int totalRows = (int) Math.ceil(filtered.size() / (float) COLS);
        float totalH = totalRows * (ICON_SIZE + ICON_GAP);
        if (totalH > gridAreaH) {
            float sbX = gx + gridAreaW + 4f, sbY = gy, sbW = 4f, sbH = gridAreaH;
            ui.drawRectInternal(shader, sbX, sbY, sbW, sbH, new Vector4f(0.05f, 0.05f, 0.10f, 0.5f));
            float thumbH = Math.max(20f, (gridAreaH / totalH) * gridAreaH);
            float thumbY = sbY + (main.recipeScrollOffset / (totalH - gridAreaH)) * (gridAreaH - thumbH);
            ui.drawRectInternal(shader, sbX, thumbY, sbW, thumbH, UIPalette.TACT_ORANGE);
        }

        // Detail panel
        float dx = sx + GRID_OFF_X + gridAreaW + 18f;
        float dy = sy + GRID_OFF_Y;
        float dh = MENU_H - GRID_OFF_Y - 14f;
        ui.drawRectInternal(shader, dx, dy, DETAIL_W, dh, new Vector4f(0.03f, 0.03f, 0.08f, 0.92f));
        ui.drawRectInternal(shader, dx, dy, DETAIL_W, 1f, UIPalette.RUSTIC_BORDER);
        ui.drawRectInternal(shader, dx, dy, 1f, dh, UIPalette.RUSTIC_BORDER);
        ui.drawRectInternal(shader, dx+DETAIL_W-1, dy, 1f, dh, UIPalette.RUSTIC_BORDER);

        if (main.recipeIndex >= 0 && main.recipeIndex < filtered.size()) {
            Recipe sel = filtered.get(main.recipeIndex);
            float prev = 64f, px = dx + (DETAIL_W - prev) / 2f, py = dy + 10f;
            ui.drawRectInternal(shader, px, py, prev, prev, new Vector4f(0.08f, 0.08f, 0.14f, 1f));
            ui.drawItemIcon(shader, sel.getResult(), px + 7, py + 7, prev - 14);

            String name = sel.getName();
            float ns = name.length() > 14 ? 0.52f : 0.60f;
            ui.drawText(shader, name, dx + DETAIL_W / 2f - name.length() * ns * 5.2f, py + prev + 8f, ns, UIPalette.TEXT_COLOR);

            String tierInfo = sel.getResult().getTierInfo();
            float tierOff = 0;
            if (tierInfo != null) {
                ui.drawText(shader, tierInfo, dx + DETAIL_W / 2f - tierInfo.length() * 0.45f * 5.2f, py + prev + 24f, 0.45f, UIPalette.TACT_BLUE);
                tierOff = 18f;
            }

            ui.drawRectInternal(shader, dx + 8, py + prev + 26f + tierOff, DETAIL_W - 16, 1f, UIPalette.RUSTIC_BORDER);
            ui.drawText(shader, "REQUIRES:", dx + 10, py + prev + 32f + tierOff, 0.50f, UIPalette.TACT_ORANGE);

            boolean canCraft = true;
            int k = 0;
            for (Map.Entry<Item, Integer> e : sel.getIngredients().entrySet()) {
                int owned = player.inventory.getCount(e.getKey());
                boolean ok = owned >= e.getValue();
                if (!ok) canCraft = false;
                Vector4f col = ok ? UIPalette.TACT_GREEN : new Vector4f(1f, 0.35f, 0.35f, 1f);
                float iy2 = py + prev + 46f + tierOff + k * 36f;
                ui.drawItemIcon(shader, e.getKey(), dx + 10, iy2, 20f);
                ui.drawText(shader, e.getKey().getName(), dx + 34, iy2 + 1, 0.48f, col);
                ui.drawText(shader, owned + " / " + e.getValue(), dx + 34, iy2 + 14, 0.56f, col);
                k++;
            }

            float bby = sy + MENU_H - 52f, bbx = dx + 8f, bbw = DETAIL_W - 16f;
            ui.drawRectInternal(shader, bbx, bby, bbw, 38f,
                    canCraft ? new Vector4f(0.10f, 0.58f, 0.98f, 1f) : new Vector4f(0.18f, 0.18f, 0.24f, 0.90f));
            String lbl = canCraft ? "FORGE" : "LOCKED";
            ui.drawText(shader, lbl, bbx + bbw / 2f - lbl.length() * 3.9f, bby + 12f, 0.70f,
                    canCraft ? UIPalette.TEXT_COLOR : new Vector4f(0.45f, 0.45f, 0.50f, 1f));
        } else {
            ui.drawText(shader, "SELECT AN ITEM", dx + 16, dy + dh / 2f - 10, 0.55f, new Vector4f(0.40f, 0.42f, 0.48f, 1f));
        }

        ui.drawText(shader, "CLICK = SELECT    ENTER/FORGE = CRAFT    ARROWS = NAVIGATE",
                sx + 8, sy + MENU_H - 14f, 0.36f, new Vector4f(0.38f, 0.40f, 0.45f, 1f));
    }
}
