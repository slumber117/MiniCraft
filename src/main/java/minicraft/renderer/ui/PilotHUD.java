package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.entity.ship.ShipEntity;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;

public class PilotHUD {
    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height) {
        ShipEntity ship = player.getRidingShip();
        if (ship == null) return;

        ui.drawRectInternal(shader, 0, 0, width, 58, UIPalette.TACT_DIM);
        ui.drawRectInternal(shader, 0, 58, width, 2, UIPalette.RUSTIC_BORDER);
        ui.drawText(shader, "AEGIS-7 TACTICAL HUB", 38, 14, 1.1f, UIPalette.TACT_ORANGE);
        ui.drawText(shader, "VESSEL: " + ship.getDefinition().displayName.toUpperCase() + "  //  LINK STATUS: NOMINAL", 38, 40, 0.46f, UIPalette.RUSTIC_PARCHMENT);
        ui.drawText(shader, String.valueOf(System.currentTimeMillis() % 1000000), width - 200, 20, 0.75f, UIPalette.TEXT_COLOR);

        float lx = 38, ly = 110;
        ui.drawText(shader, "VITAL TELEMETRY", lx, ly, 0.55f, UIPalette.TACT_ORANGE);
        ly += 28;
        ui.drawTacticalBar(shader, lx, ly, 215, 9, ship.getHealth() / ship.getMaxHealth(), UIPalette.TACT_GREEN, "HULL INTEGRITY");
        ly += 42;
        ui.drawTacticalBar(shader, lx, ly, 215, 9, ship.getShieldPct(), UIPalette.TACT_BLUE, "SHIELD CAPACITY");
        ly += 42;
        ui.drawTacticalBar(shader, lx, ly, 215, 9, ship.getEnergyPct(), new Vector4f(0.9f, 0.9f, 0, 1), "POWER CORE");
        ly += 42;
        ui.drawTacticalBar(shader, lx, ly, 215, 9, ship.getFuelPct(), UIPalette.TACT_ORANGE, "FUEL RESERVES");

        float rSize = 320f, rx = (width - rSize) / 2f, ry = (height - rSize) / 2f;
        ui.drawRectInternal(shader, rx, ry, rSize, rSize, new Vector4f(1, 1, 1, 0.04f));
        ui.drawRectInternal(shader, rx + rSize / 2f - 1, ry, 2, rSize, UIPalette.RUSTIC_BORDER);
        ui.drawRectInternal(shader, rx, ry + rSize / 2f - 1, rSize, 2, UIPalette.RUSTIC_BORDER);
        ui.drawCrosshair(shader, width / 2f, height / 2f);
        float anim = (float) Math.sin(System.currentTimeMillis() / 200.0) * 8f;
        ui.drawRectInternal(shader, width / 2f - 22 - anim / 2f, height / 2f - 22 - anim / 2f, 44 + anim, 44 + anim, new Vector4f(1, 0.5f, 0, 0.18f));
        ui.drawText(shader, "VECTOR: " + (int) ship.yaw + "\u00b0", rx, ry + rSize + 18, 0.65f, UIPalette.TACT_ORANGE);
        ui.drawText(shader, "MAG: " + String.format("%.1f", ship.getVelocityKms()) + " KM/S", rx + 145, ry + rSize + 18, 0.65f, UIPalette.TEXT_COLOR);

        float wx = width - 258, wy = 110;
        ui.drawText(shader, "WEAPONS MATRIX", wx, wy, 0.55f, UIPalette.TACT_ORANGE);
        wy += 28;
        String[] weapons = { "MAC CANNON", "ARCHER PODS", "PULSE LASER" };
        ShipEntity.WeaponSystem active = ship.getActiveWeapon();
        for (int i = 0; i < weapons.length; i++) {
            boolean sel = (active.ordinal() == i);
            ui.drawRectInternal(shader, wx, wy + i * 48, 218, 38, sel ? new Vector4f(1, 0.5f, 0, 0.2f) : UIPalette.TACT_DIM);
            ui.drawRectInternal(shader, wx, wy + i * 48, 2, 38, sel ? UIPalette.TACT_ORANGE : UIPalette.RUSTIC_PARCHMENT);
            ui.drawText(shader, weapons[i], wx + 14, wy + 11 + i * 48, 0.65f, sel ? UIPalette.TEXT_COLOR : UIPalette.RUSTIC_PARCHMENT);
        }

        float bfY = height - 115, bfGap = 155, bfX = (width - bfGap * 3) / 2f;
        ui.drawTacticalField(shader, bfX, bfY, "X-COORD", String.valueOf((int) ship.position.x));
        ui.drawTacticalField(shader, bfX + bfGap, bfY, "Y-COORD", String.valueOf((int) ship.position.y));
        ui.drawTacticalField(shader, bfX + bfGap * 2, bfY, "Z-COORD", String.valueOf((int) ship.position.z));

        ui.drawRectInternal(shader, 0, height - 38, width, 38, UIPalette.TACT_DIM);
        long tTime = System.currentTimeMillis() / 50;
        float tX = width - (tTime % (width + 1000));
        ui.drawText(shader, "[24:12:08] SYSTEM NOMINAL // NEURAL LINK STABLE // SHIELDS AT 100% // READY FOR SLIPSPACE JUMP", tX, height - 25, 0.55f, new Vector4f(0.48f, 0.48f, 0.48f, 1));
    }
}
