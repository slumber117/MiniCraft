package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.Main;
import minicraft.ship.ShipRegistry;
import minicraft.ship.ShipDefinition;

import java.util.List;

public class ShipConsoleUI {
    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0, 0.04f, 0.09f, 0.88f));

        float panelW = 900f, panelH = 600f;
        float sx = (width - panelW) / 2f, sy = (height - panelH) / 2f;

        ui.drawTacticalFrame(shader, sx, sy, panelW, panelH);
        ui.drawText(shader, "VESSEL COMMAND", sx + 28, sy + 28, 1.2f, UIPalette.RUSTIC_PARCHMENT);
        ui.drawText(shader, "DRYDOCK STATUS: READY", sx + 28, sy + 58, 0.72f, UIPalette.RUSTIC_PARCHMENT);

        List<ShipDefinition> ships = ShipRegistry.getInstance().getAll();
        float btnW = 250f, btnH = 380f, gap = 28f;
        int count = Math.min(3, ships.size());
        float startX = sx + (panelW - (count * btnW + (count - 1) * gap)) / 2f, startY = sy + 100f;

        double[] mx = new double[1], my = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(main.getWindow(), mx, my);
        float mouseX = (float) mx[0], mouseY = (float) my[0];

        for (int i = 0; i < ships.size(); i++) {
            ShipDefinition def = ships.get(i);
            float bx = startX + i * (btnW + gap);
            boolean hover = mouseX >= bx && mouseX <= bx + btnW && mouseY >= startY && mouseY <= startY + btnH;

            ui.drawRectInternal(shader, bx, startY, btnW, btnH,
                    hover ? new Vector4f(0, 0.38f, 0.40f, 0.82f) : new Vector4f(0, 0.09f, 0.18f, 0.62f));
            ui.drawRectInternal(shader, bx, startY, btnW, 2, new Vector4f(0, 1, 1, hover ? 1f : 0.28f));

            ui.drawText(shader, def.displayName.toUpperCase(), bx + 14, startY + 18, 0.75f, UIPalette.TEXT_COLOR);
            ui.drawRectInternal(shader, bx + 18, startY + 52, btnW - 36, btnW - 36, new Vector4f(0, 0, 0, 0.5f));
            ui.drawText(shader, "SCHEMATIC PREVIEW", bx + 32, startY + 148, 0.52f, new Vector4f(0.4f, 0.4f, 0.5f, 1));

            ui.drawText(shader, "CLASS: " + def.shipClass.displayName, bx + 14, startY + btnW + 22, 0.60f, new Vector4f(0.8f, 0.8f, 1f, 1f));
            ui.drawText(shader, "BLOCKS: " + def.getBlockCount(), bx + 14, startY + btnW + 44, 0.60f, new Vector4f(0.8f, 0.8f, 1f, 1f));
            ui.drawText(shader, def.getDimensionsString(), bx + 14, startY + btnW + 66, 0.60f, new Vector4f(0.8f, 0.8f, 1f, 1f));

            String desc = def.description.length() > 55 ? def.description.substring(0, 52) + "..." : def.description;
            ui.drawText(shader, desc, bx + 14, startY + btnW + 92, 0.50f, new Vector4f(0.58f, 0.58f, 0.58f, 1f));
        }
        ui.drawRectInternal(shader, mouseX - 6, mouseY - 6, 12, 12, new Vector4f(0, 1, 1, 1));
    }
}
