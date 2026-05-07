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
    // Boss Compass Caching (Async)
    private float cachedTargetX = 0, cachedTargetZ = 0;
    private boolean cachedTargetFound = false;
    private long lastScanTime = 0;
    private boolean isScanning = false;

    public void renderPlayHUD(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        float hotbarTop = height - 14f - 42f;
        float xpBarTop = hotbarTop - 10f - 38f;
        float xpLabelTop = xpBarTop - 13f;

        float panelH = 12f * 2 + 20f + 20f + 6f; // Expanded to 70f to accommodate 20f gap
        
        // --- Calculate Hotbar Layout for Alignment ---
        float slotS = 42f, gap = 6f, stripW = 9 * (slotS + gap) - gap;
        float startX = (width - stripW) / 2f;
        
        float panelW = 210f + 40f + 16f;
        float panelGap = 20f; // Gap between health and hotbar
        
        float panelX = startX - panelW - panelGap;
        float statTop = hotbarTop - (panelH - slotS) / 2f; // Vertically center with hotbar
        float marginLeft = panelX + 8f;

        renderHotbar(ui, player, shader, width, height, hotbarTop);
        renderXPBar(ui, player, shader, width, xpBarTop, xpLabelTop);

        // --- Radiation Bar (Above Health/Hunger) ---
        if (player.radiationLevel > 0) {
            float radY = statTop - 40f;
            ui.drawTacticalFrame(shader, panelX, radY, panelW, 36f);
            float pulse = 0.7f + 0.3f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 150.0));
            Vector4f radCol = new Vector4f(0.5f, 1.0f, 0.0f, pulse);
            ui.drawPremiumBar(shader, marginLeft + 32f, radY + 6f, 210f, 12f,
                    Math.min(1.0f, player.radiationLevel / 100f),
                    radCol, new Vector4f(0.1f, 0.25f, 0.0f, 1.0f), "R");
            ui.drawText(shader, "RAD", marginLeft + 6, radY + 6, 0.45f, radCol);
            Vector4f warningCol = player.radiationLevel > 80f
                    ? new Vector4f(1.0f, 0.2f, 0.2f, pulse)
                    : new Vector4f(0.6f, 1.0f, 0.2f, 1.0f);
            ui.drawText(shader, String.format("TOXICITY LEVEL: %d%%", (int) player.radiationLevel),
                    marginLeft + 32f, radY + 22f, 0.38f, warningCol);
        }

        // ── Stats panel — hunger + medieval health ─────────────────────────
        ui.drawTacticalFrame(shader, panelX, statTop, panelW, panelH);

        float barX = marginLeft + 32f, hungBarY = statTop + 8f + 4f;

        // Hunger bar stays as-is (a flat bar suits food well enough)
        ui.drawPremiumBar(shader, barX, hungBarY, 210f, 12f,
                player.hunger / player.maxHunger,
                UIPalette.HUNGER_COLOR, UIPalette.HUNGER_COLOR_DARK, "F");

        // ── Medieval heart bar replaces the old flat health bar ────────────
        float heartRowY = hungBarY + 12f + 20f; // Increased gap to 20f
        drawMedievalHealthBar(ui, player, shader, barX, heartRowY);

        // Offhand item slot
        if (player.inventory.getOffhandItem() != null) {
            float ohX = panelX + panelW + 6f, ohY = statTop + (panelH - 36f) / 2f;
            ui.drawRectInternal(shader, ohX, ohY, 36, 36, UIPalette.RUSTIC_BG);
            ui.drawItemIcon(shader, player.inventory.getOffhandItem(), ohX + 4, ohY + 4, 28);
        }

        // Navigation panel
        ui.drawTacticalFrame(shader, 14, 14, 188, 68);
        ui.drawText(shader, "LOCAL NAVIGATION", 26, 26, 0.38f, UIPalette.RUSTIC_PARCHMENT);
        Vector4f lightGray = new Vector4f(0.72f, 0.70f, 0.68f, 1f);
        ui.drawText(shader, String.format("X:%-6d  Y:%-6d", (int) player.position.x, (int) player.position.y), 26, 44,
                0.45f, lightGray);
        ui.drawText(shader, String.format("Z:%-6d", (int) player.position.z), 26, 59, 0.45f, lightGray);
        ui.drawText(shader,
                main.getWorld().getBiome((int) player.position.x, (int) player.position.z).displayName.toUpperCase(),
                26, 74, 0.35f, new Vector4f(0.55f, 0.52f, 0.48f, 1f));

        drawTemperatureHUD(ui, player, shader, width, 20f);

        if (main.focusedBlock != null && main.focusedBlock != Block.AIR) {
            drawFocusedBlockHUD(ui, shader, width, 72f, main.focusedBlock);
        }
        if (main.activeStatusMessage != null && !main.activeStatusMessage.isEmpty()) {
            drawStatusMessage(ui, shader, width, height, main.activeStatusMessage, main.statusMessageTimer);
        }

        // Damage vignette
        float hpPct = player.getHealth() / player.getMaxHealth();
        if (hpPct < 1.0f && player.damageFlashTimer > 0 && hpPct >= 0.10f) {
            ui.drawDamageVignette(shader, width, height, player.damageFlashTimer * 0.5f);
        } else if (hpPct < 0.10f) {
            float pulse = 0.20f + 0.12f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 400.0));
            ui.drawDamageVignette(shader, width, height, pulse);
        }

        renderBossCompass(ui, player, shader, width, height, main);
    }

    private void renderBossCompass(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        ItemStack held = player.inventory.getHeldItem();
        if (held == null || !held.getDisplayName().equalsIgnoreCase("Boss Compass")) return;

        long now = System.currentTimeMillis();
        if (!isScanning && now - lastScanTime > 2000) {
            isScanning = true;
            final float px = player.position.x;
            final float pz = player.position.z;
            
            new Thread(() -> {
                try {
                    int gridSize = 64;
                    int cx = (int)Math.floor(px / 16.0);
                    int cz = (int)Math.floor(pz / 16.0);
                    
                    float minDistSq = Float.MAX_VALUE;
                    float tx = 0, tz = 0;
                    boolean found = false;

                    // Expanded 5x5 grid scan (~5000x5000m area)
                    int gridX = Math.floorDiv(cx, gridSize) * gridSize + 32;
                    int gridZ = Math.floorDiv(cz, gridSize) * gridSize + 32;

                    for (int ox = -2; ox <= 2; ox++) {
                        for (int oz = -2; oz <= 2; oz++) {
                            int gx = gridX + ox * gridSize;
                            int gz = gridZ + oz * gridSize;
                            
                            long seed = (long)gx * 3123456789L + (long)gz * 123456789L + 777;
                            java.util.Random r = new java.util.Random(seed);
                            
                            if (r.nextFloat() < 0.6f) {
                                int bossType = r.nextInt(3);
                                // HEAVY ML CALL - Now on background thread!
                                minicraft.world.WorldCell centerCell = main.getWorld().getGenerator().generate(gx * 16 + 8, gz * 16 + 8);
                                
                                boolean valid = (bossType == 0 && centerCell.biome == minicraft.world.Biome.MOUNTAINS) ||
                                                (bossType == 1 && centerCell.biome == minicraft.world.Biome.DESERT) ||
                                                (bossType == 2 && centerCell.biome == minicraft.world.Biome.SAVANNA);
                                
                                if (valid) {
                                    float bx = gx * 16 + 8;
                                    float bz = gz * 16 + 8;
                                    float dSq = (bx - px)*(bx - px) + (bz - pz)*(bz - pz);
                                    if (dSq < minDistSq) {
                                        minDistSq = dSq;
                                        tx = bx;
                                        tz = bz;
                                        found = true;
                                    }
                                }
                            }
                        }
                    }
                    
                    cachedTargetX = tx;
                    cachedTargetZ = tz;
                    cachedTargetFound = found;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lastScanTime = System.currentTimeMillis();
                    isScanning = false;
                }
            }, "CompassScannerThread").start();
        }

        float guiX = width - 110f, guiY = height - 120f;
        float targetX = cachedTargetX;
        float targetZ = cachedTargetZ;
        boolean found = cachedTargetFound;
        
        // Check for entities first (priority tracking)
        for (minicraft.entity.Entity e : main.getEntityManager().getAll()) {
            if (e instanceof minicraft.entity.monsters.ForestCrawler || 
                e.getType() == minicraft.entity.EntityType.GOLD_DRAGON ||
                e.getType() == minicraft.entity.EntityType.ICE_BEAST) {
                targetX = e.position.x;
                targetZ = e.position.z;
                found = true;
                break;
            }
        }

        if (found) {
            float dx = targetX - player.position.x;
            float dz = targetZ - player.position.z;
            float angle = (float) Math.atan2(dz, dx);
            float dist = (float) Math.sqrt(dx*dx + dz*dz);
            
            // Relative to player viewing direction
            float camYaw = (float) Math.toRadians(main.getCamera().getRotation().y);
            float relAngle = angle + camYaw + (float)Math.PI / 2f;

            ui.drawRectInternal(shader, guiX - 35, guiY - 35, 70, 70, new Vector4f(0.12f, 0.11f, 0.10f, 0.85f));
            ui.drawTacticalFrame(shader, guiX - 35, guiY - 35, 70, 70);
            ui.drawArrow(shader, guiX, guiY, 25f, -relAngle, new Vector4f(0.2f, 1.0f, 0.2f, 1.0f));
            ui.drawText(shader, "BOSS SIGNAL", guiX - 45, guiY + 42, 0.35f, new Vector4f(0.2f, 1.0f, 0.2f, 1.0f));
            
            // Distance Meter
            String distStr = (int)dist + "m";
            ui.drawText(shader, distStr, guiX - (distStr.length() * 4), guiY + 54, 0.35f, new Vector4f(1f, 0.8f, 0.2f, 1.0f));
        } else {
            ui.drawText(shader, "NO SIGNAL", guiX - 40, guiY, 0.4f, new Vector4f(0.6f, 0.6f, 0.6f, 1.0f));
        }
    }

    // ── Medieval health bar ────────────────────────────────────────────────

    /**
     * Draws a row of pixel-art hearts inside a stone-carved tray frame.
     *
     * Layout (each heart is 7×6 pixel-units at scale S=3, i.e. 21×18 px):
     *
     * .XX.XX. ← top bumps (two 2-wide bumps)
     * XXXXXXX ← upper body
     * XXXXXXX ← lower body
     * .XXXXX. ← taper
     * ..XXX.. ← taper
     * ...X... ← point
     *
     * Full hearts are crimson with a highlight pixel in the upper-left bump.
     * Half hearts are dark red (right half empty).
     * Empty hearts are a dim stone hollow.
     *
     * A battlemented tray frame (crenellation notches on top) wraps the row.
     * "VITALITY" appears above the hearts in parchment-gold ink.
     *
     * @param barX Left edge of the bar area (aligns with hunger bar)
     * @param barY Top of the heart row (same band height as old flat bar)
     */
    private void drawMedievalHealthBar(UIRenderer ui, Player player,
            ShaderProgram shader,
            float barX, float barY) {

        // ── Geometry constants ─────────────────────────────────────────────
        final int TOTAL_HEARTS = 10; // 10 hearts max (each = maxHp/10)
        final float S = 3f; // pixel scale factor
        final float HW = 7 * S; // heart pixel width = 21
        final float HH = 6 * S; // heart pixel height = 18
        final float GAP = 4f; // gap between hearts
        final float PAD_X = 8f; // frame horizontal padding
        final float PAD_TOP = 14f; // frame top padding (for label + battlements)
        final float PAD_BOT = 6f; // frame bottom padding
        final float CRENEL_W = 5f; // battlement tooth width
        final float CRENEL_H = 4f; // battlement tooth height

        float rowW = TOTAL_HEARTS * (HW + GAP) - GAP;
        float frameW = rowW + PAD_X * 2f;
        float frameH = PAD_TOP + HH + PAD_BOT;
        float frameX = barX - PAD_X;
        float frameY = barY - PAD_TOP;

        // ── Tray frame background ──────────────────────────────────────────
        ui.drawRectInternal(shader, frameX, frameY, frameW, frameH,
                UIPalette.HEART_FRAME_BG);

        // Outer border (1px on each side)
        ui.drawRectInternal(shader, frameX, frameY, frameW, 1f,
                UIPalette.HEART_FRAME_BORDER); // top
        ui.drawRectInternal(shader, frameX, frameY + frameH - 1f, frameW, 1f,
                UIPalette.HEART_FRAME_BORDER); // bottom
        ui.drawRectInternal(shader, frameX, frameY, 1f, frameH,
                UIPalette.HEART_FRAME_BORDER); // left
        ui.drawRectInternal(shader, frameX + frameW - 1f, frameY, 1f, frameH,
                UIPalette.HEART_FRAME_BORDER); // right

        // ── Battlements (crenellations) across the top of the frame ───────
        // Each tooth: CRENEL_W wide, CRENEL_H tall, alternating with gaps.
        // We plant them centred on the top border.
        float toothStep = CRENEL_W * 2f + 2f; // tooth + matching gap
        int numTeeth = (int) (frameW / toothStep);
        float toothStart = frameX + (frameW - numTeeth * toothStep) / 2f;

        for (int t = 0; t < numTeeth; t++) {
            float tx = toothStart + t * toothStep;
            // Tooth body — sits above the frame top edge
            ui.drawRectInternal(shader, tx, frameY - CRENEL_H,
                    CRENEL_W, CRENEL_H, UIPalette.HEART_FRAME_STONE);
            // Tooth border highlight (top + sides, 1px)
            ui.drawRectInternal(shader, tx, frameY - CRENEL_H,
                    CRENEL_W, 1f, UIPalette.HEART_FRAME_BORDER);
            ui.drawRectInternal(shader, tx, frameY - CRENEL_H,
                    1f, CRENEL_H, UIPalette.HEART_FRAME_BORDER);
            ui.drawRectInternal(shader, tx + CRENEL_W - 1f, frameY - CRENEL_H,
                    1f, CRENEL_H, UIPalette.HEART_FRAME_BORDER);
        }

        // ── "VITALITY" label ───────────────────────────────────────────────
        boolean isCritical = player.getHealth() / player.getMaxHealth() < 0.25f;
        Vector4f labelCol = isCritical ? UIPalette.HEART_LABEL_CRITICAL : UIPalette.HEART_LABEL;

        // Pulse label slightly when critical (flicker on a ~600ms sine)
        if (isCritical) {
            float pulse = 0.65f + 0.35f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 300.0));
            labelCol = new Vector4f(labelCol.x, labelCol.y, labelCol.z, pulse);
        }
        ui.drawText(shader, "VITALITY", frameX + PAD_X, frameY + 3f, 0.36f, labelCol);

        // ── Compute heart fill amounts ─────────────────────────────────────
        // Each heart = 1/TOTAL_HEARTS of max health.
        // heartsFloat in [0, TOTAL_HEARTS], e.g. 7.5 = 7 full + 1 half.
        float hpFraction = Math.max(0f, Math.min(1f, player.getHealth() / player.getMaxHealth()));
        float heartsFloat = hpFraction * TOTAL_HEARTS;
        int fullHearts = (int) heartsFloat;
        boolean hasHalf = (heartsFloat - fullHearts) >= 0.5f;

        // ── Draw hearts ────────────────────────────────────────────────────
        for (int i = 0; i < TOTAL_HEARTS; i++) {
            float hx = barX + i * (HW + GAP);
            float hy = barY;

            boolean full = i < fullHearts;
            boolean half = !full && hasHalf && (i == fullHearts);

            drawPixelHeart(ui, shader, hx, hy, S, full, half);
        }
    }

    /**
     * Draws a single 7×6 pixel-art heart at (hx, hy) using drawRectInternal.
     *
     * Pixel layout (. = empty, X = filled):
     *
     * row 0: . X X . X X .
     * row 1: X X X X X X X
     * row 2: X X X X X X X
     * row 3: . X X X X X .
     * row 4: . . X X X . .
     * row 5: . . . X . . .
     *
     * The outline shadow is drawn first (1px expanded rect per row),
     * then the coloured fill, then a small shine pixel in the left bump.
     *
     * @param full Draw as a fully filled heart
     * @param half Draw with only the left lobe filled (right side empty)
     */
    private void drawPixelHeart(UIRenderer ui, ShaderProgram shader,
            float hx, float hy, float s,
            boolean full, boolean half) {

        Vector4f fillCol = full ? UIPalette.HEART_FULL
                : half ? UIPalette.HEART_HALF
                        : UIPalette.HEART_EMPTY;
        Vector4f shadowCol = UIPalette.HEART_SHADOW;

        // ── Shadow outline (drawn 1px larger on each side per row) ────────
        // row 0: two bumps
        drawShadowRect(ui, shader, hx + 1 * s, hy, 2 * s, 1 * s, shadowCol); // left bump shadow
        drawShadowRect(ui, shader, hx + 4 * s, hy, 2 * s, 1 * s, shadowCol); // right bump shadow
        // rows 1–2
        drawShadowRect(ui, shader, hx, hy + 1 * s, 7 * s, 2 * s, shadowCol);
        // row 3
        drawShadowRect(ui, shader, hx + 1 * s, hy + 3 * s, 5 * s, 1 * s, shadowCol);
        // row 4
        drawShadowRect(ui, shader, hx + 2 * s, hy + 4 * s, 3 * s, 1 * s, shadowCol);
        // row 5 (tip)
        drawShadowRect(ui, shader, hx + 3 * s, hy + 5 * s, 1 * s, 1 * s, shadowCol);

        // ── Fill ──────────────────────────────────────────────────────────
        // row 0: two bumps
        ui.drawRectInternal(shader, hx + 1 * s, hy, 2 * s, 1 * s, fillCol);
        ui.drawRectInternal(shader, hx + 4 * s, hy, 2 * s, 1 * s, fillCol);
        // rows 1–2
        ui.drawRectInternal(shader, hx, hy + 1 * s, 7 * s, 2 * s, fillCol);
        // row 3
        ui.drawRectInternal(shader, hx + 1 * s, hy + 3 * s, 5 * s, 1 * s, fillCol);
        // row 4
        ui.drawRectInternal(shader, hx + 2 * s, hy + 4 * s, 3 * s, 1 * s, fillCol);
        // row 5 (tip)
        ui.drawRectInternal(shader, hx + 3 * s, hy + 5 * s, 1 * s, 1 * s, fillCol);

        // ── Half-heart: overdraw right lobe with empty colour ─────────────
        if (half) {
            Vector4f emptyCol = UIPalette.HEART_EMPTY;
            // right bump (row 0)
            ui.drawRectInternal(shader, hx + 4 * s, hy, 2 * s, 1 * s, emptyCol);
            // right half of rows 1–2 (pixels 4–6)
            ui.drawRectInternal(shader, hx + 4 * s, hy + 1 * s, 3 * s, 2 * s, emptyCol);
            // right of row 3 (pixels 4–5)
            ui.drawRectInternal(shader, hx + 4 * s, hy + 3 * s, 2 * s, 1 * s, emptyCol);
        }

        // ── Shine pixel — upper-left of left bump (full hearts only) ──────
        if (full || half) {
            ui.drawRectInternal(shader, hx + 1 * s, hy,
                    1 * s, 1 * s, UIPalette.HEART_SHINE);
        }
    }

    /**
     * Draws a shadow underlay rect expanded 1px on all sides.
     * Used to give hearts a thin dark outline without requiring a sprite.
     */
    private void drawShadowRect(UIRenderer ui, ShaderProgram shader,
            float x, float y, float w, float h,
            Vector4f col) {
        ui.drawRectInternal(shader, x - 1, y - 1, w + 2, h + 2, col);
    }

    // ── Hotbar ─────────────────────────────────────────────────────────────

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
                if (hotbar[i].getCount() > 1)
                    ui.drawText(shader, String.valueOf(hotbar[i].getCount()), sx + 3, topY + slotS - 11, 0.52f);
            }
        }
        Item selected = player.inventory.getSelectedItem();
        if (selected != null) {
            String name = selected.getDisplayName();
            ui.drawText(shader, name, (width - name.length() * 7.5f) / 2f, topY - 16f, 0.6f, UIPalette.TEXT_COLOR);
            String tier = selected.getTierInfo();
            if (tier != null)
                ui.drawText(shader, tier, (width - tier.length() * 5.0f) / 2f, topY - 26f, 0.4f, UIPalette.TACT_ORANGE);
        }
        if (player.miningProgress > 0) {
            float pbW = 90f, pbH = 5f, px = (width - pbW) / 2f, py = height / 2f + 22f;
            ui.drawRectInternal(shader, px, py, pbW, pbH, new Vector4f(0, 0, 0, 0.55f));
            ui.drawRectInternal(shader, px, py, pbW * player.miningProgress, pbH,
                    new Vector4f(0.38f, 0.95f, 0.38f, 0.95f));
        }
        if (player.eatingProgress > 0) {
            float pbW = 90f, pbH = 5f, px = (width - pbW) / 2f, py = height / 2f + 22f;
            ui.drawRectInternal(shader, px, py, pbW, pbH, new Vector4f(0, 0, 0, 0.55f));
            ui.drawRectInternal(shader, px, py, pbW * player.eatingProgress, pbH,
                    new Vector4f(1.0f, 0.75f, 0.0f, 1.0f));
        }
    }

    // ── XP Bar ────────────────────────────────────────────────────────────

    private void renderXPBar(UIRenderer ui, Player player, ShaderProgram shader, int width, float barTop,
            float labelTop) {
        float barW = 200f, x = (width - barW) / 2f, prog = Math.max(0, Math.min(1, player.xp / player.xpToNextLevel));
        ui.drawText(shader, "RANK " + player.level, x, labelTop, 0.40f, UIPalette.RUSTIC_PARCHMENT);
        ui.drawRectInternal(shader, x, barTop, barW, 10f, new Vector4f(0, 0, 0, 0.65f));
        float fill = barW * prog;
        ui.drawRectInternal(shader, x, barTop, fill, 10f * 0.55f, UIPalette.TACT_BLUE);
        ui.drawRectInternal(shader, x, barTop + 10f * 0.55f, fill, 10f * 0.45f,
                new Vector4f(0.06f, 0.45f, 0.85f, 1.0f));
        ui.drawRectInternal(shader, x, barTop, fill, 2f, UIPalette.BAR_SHINE);
        ui.drawRectInternal(shader, x, barTop, barW, 1f, UIPalette.RUSTIC_BORDER);
    }

    // ── Misc HUD elements ──────────────────────────────────────────────────

    private void drawTemperatureHUD(UIRenderer ui, Player player, ShaderProgram shader, int screenWidth, float margin) {
        float pW = 150f, pH = 48f, x = screenWidth - margin - pW, y = margin;
        ui.drawTacticalFrame(shader, x, y, pW, pH);
        ui.drawText(shader, String.format("%.1f\u00b0C", player.temperature), x + 10, y + 14, 0.85f,
                new Vector4f(0.65f, 0.65f, 0.65f, 1f));
        Vector4f col;
        String state = player.tempState;
        if (state.contains("Hypothermia"))
            col = new Vector4f(0.75f, 0.0f, 1.0f, 1.0f);
        else if (state.equals("Cold"))
            col = UIPalette.TACT_BLUE;
        else if (state.equals("Too Warm"))
            col = UIPalette.HEALTH_COLOR;
        else if (state.equals("Warm"))
            col = UIPalette.TACT_ORANGE;
        else
            col = UIPalette.TACT_GREEN;
        ui.drawText(shader, state.toUpperCase(), x + 10, y + 33, 0.38f, col);
    }

    private void drawStatusMessage(UIRenderer ui, ShaderProgram shader, int width, int height, String msg,
            float timer) {
        float scale = 0.55f, textW = msg.length() * 14f * scale, x = (width - textW) / 2f;
        ui.drawText(shader, msg.toUpperCase(), x, 28f, scale, new Vector4f(1f, 1f, 1f, Math.min(1.0f, timer * 2.0f)));
    }

    private void drawFocusedBlockHUD(UIRenderer ui, ShaderProgram shader, int screenWidth, float y, Block block) {
        float pW = 188f, pH = 46f, x = screenWidth - 20f - pW;
        ui.drawTacticalFrame(shader, x, y, pW, pH);
        ui.drawText(shader, "TARGET IDENTIFIED", x + 10, y + 10, 0.35f, UIPalette.TACT_ORANGE);
        ui.drawText(shader, block.getFriendlyName().toUpperCase(), x + 10, y + 25, 0.45f,
                new Vector4f(0.65f, 0.65f, 0.65f, 1f));
    }
}