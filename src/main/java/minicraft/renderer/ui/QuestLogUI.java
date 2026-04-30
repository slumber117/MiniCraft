package minicraft.renderer.ui;

import minicraft.entity.Player;
import minicraft.renderer.UIRenderer;
import minicraft.renderer.ShaderProgram;
import minicraft.math.Vector4f;
import minicraft.math.Matrix4f;
import minicraft.Main;
import minicraft.item.Item;
import minicraft.quest.Quest;
import minicraft.quest.QuestObjective;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class QuestLogUI {
    private Quest.Phase questPhaseTab = Quest.Phase.NOVICE;
    private int questSelectedIndex = 0;
    private float questListScroll = 0f;
    
    private float acceptBtnX = 0f;
    private float acceptBtnY = 0f;
    private float acceptBtnW = 160f;
    private float acceptBtnH = 36f;
    private boolean acceptBtnVisible = false;

    public void render(UIRenderer ui, Player player, ShaderProgram shader, int width, int height, Main main) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -1, 1);
        shader.setUniform("projectionMatrix", ortho);
        shader.setUniform("viewMatrix", new Matrix4f().identity());
        shader.setUniform("useLighting", 0.0f);

        ui.drawRectInternal(shader, 0, 0, width, height, new Vector4f(0f, 0f, 0f, 0.84f));

        final float PW = Math.min(980f, width - 60), PH = Math.min(660f, height - 60);
        final float PX = (width - PW) / 2f, PY = (height - PH) / 2f;
        final float TAB_H = 32f, LPAD = 16f, LIST_W = 230f;
        final float DX = PX + LIST_W + LPAD * 2, DW = PW - LIST_W - LPAD * 3;
        final float BODY_Y = PY + TAB_H + 40f, BODY_H = PH - TAB_H - 50f;
        final float SLOT_H = 42f, SLOT_G = 4f;

        ui.drawTacticalFrame(shader, PX, PY, PW, PH);
        ui.drawText(shader, "QUEST JOURNAL", PX + LPAD, PY + 10, 0.90f, UIPalette.TACT_ORANGE);
        ui.drawText(shader, "[Q] CLOSE", PX + PW - 85, PY + 14, 0.42f, new Vector4f(0.55f, 0.55f, 0.55f, 1f));

        Quest.Phase[] phases = Quest.Phase.values();
        float tabW = (PW - LPAD * 2) / phases.length;
        for (int i = 0; i < phases.length; i++) {
            Quest.Phase p = phases[i];
            float tx = PX + LPAD + i * tabW, ty = PY + 48f;
            boolean active = (p == questPhaseTab);
            ui.drawRectInternal(shader, tx, ty, tabW - 3f, TAB_H, active ? new Vector4f(0.12f, 0.45f, 0.85f, 0.95f) : new Vector4f(0.06f, 0.06f, 0.12f, 0.80f));
            if (active) ui.drawRectInternal(shader, tx, ty + TAB_H - 2, tabW - 3f, 2f, UIPalette.TACT_ORANGE);
            String label = "Phase " + p.numeral + "  " + p.displayName;
            ui.drawText(shader, label, tx + 8, ty + 9, 0.55f, active ? UIPalette.TEXT_COLOR : new Vector4f(0.55f, 0.58f, 0.62f, 1f));
        }

        List<Quest> phaseQuests = main.questManager.getByPhase(questPhaseTab);
        if (questSelectedIndex >= phaseQuests.size()) questSelectedIndex = 0;
        float listX = PX + LPAD, listY = BODY_Y;
        ui.drawRectInternal(shader, listX + LIST_W + LPAD, listY, 1f, BODY_H, UIPalette.RUSTIC_BORDER);

        glEnable(GL_SCISSOR_TEST);
        glScissor((int) listX, (int) (height - (listY + BODY_H)), (int) LIST_W, (int) BODY_H);
        for (int i = 0; i < phaseQuests.size(); i++) {
            Quest q = phaseQuests.get(i);
            float ey = listY + i * (SLOT_H + SLOT_G) - questListScroll;
            if (ey + SLOT_H < listY - 10 || ey > listY + BODY_H + 10) continue;
            boolean sel = (i == questSelectedIndex);
            ui.drawRectInternal(shader, listX, ey, LIST_W, SLOT_H, sel ? new Vector4f(0.10f, 0.30f, 0.58f, 0.95f) : new Vector4f(0.04f, 0.04f, 0.08f, 0.85f));
            ui.drawRectInternal(shader, listX, ey, 3f, SLOT_H, questStateColour(q.state));
            boolean locked = q.state == Quest.State.LOCKED;
            ui.drawText(shader, questStateIcon(q.state) + " " + q.title, listX + 8, ey + 8, 0.52f, locked ? new Vector4f(0.40f, 0.40f, 0.40f, 1f) : UIPalette.TEXT_COLOR);
            if (q.state == Quest.State.IN_PROGRESS) {
                float pf = q.totalProgress();
                ui.drawRectInternal(shader, listX + 8, ey + SLOT_H - 8, LIST_W - 16, 4f, new Vector4f(0f, 0f, 0f, 0.5f));
                ui.drawRectInternal(shader, listX + 8, ey + SLOT_H - 8, (LIST_W - 16) * pf, 4f, UIPalette.TACT_ORANGE);
            }
        }
        glDisable(GL_SCISSOR_TEST);

        if (!phaseQuests.isEmpty()) {
            Quest sel = phaseQuests.get(questSelectedIndex);
            float dy = BODY_Y;
            ui.drawText(shader, sel.title.toUpperCase(), DX + 4, dy, 1.05f, UIPalette.TACT_BLUE);
            dy += 34; ui.drawRectInternal(shader, DX, dy, DW, 1f, UIPalette.RUSTIC_BORDER);
            dy += 14; ui.drawText(shader, sel.description, DX + 4, dy, 0.48f, new Vector4f(0.70f, 0.70f, 0.75f, 1f));
            dy += 40; ui.drawText(shader, "PHASE " + sel.phase.numeral + " — " + sel.phase.displayName.toUpperCase(), DX + 4, dy, 0.40f, UIPalette.TACT_ORANGE);
            dy += 30; ui.drawRectInternal(shader, DX, dy, DW, 1f, UIPalette.RUSTIC_BORDER);
            dy += 18; ui.drawText(shader, "OBJECTIVES", DX + 4, dy, 0.48f, UIPalette.TEXT_COLOR);
            dy += 26;
            for (QuestObjective o : sel.objectives) {
                boolean done = o.isDone();
                ui.drawText(shader, (done ? "[X]" : "[ ]") + "  " + o.description, DX + 4, dy, 0.50f, done ? new Vector4f(0f, 0.88f, 0.36f, 1f) : UIPalette.TEXT_COLOR);
                dy += 15;
                if (!done) {
                    float pf = o.getProgressFraction();
                    ui.drawRectInternal(shader, DX + 4, dy, DW - 8, 5f, new Vector4f(0f, 0f, 0f, 0.5f));
                    ui.drawRectInternal(shader, DX + 4, dy, (DW - 8) * pf, 5f, UIPalette.TACT_BLUE);
                    ui.drawText(shader, o.getProgressText(), DX + DW - 55, dy - 13, 0.38f, new Vector4f(0.55f, 0.55f, 0.55f, 1f));
                }
                dy += 16;
            }
            dy += 12; ui.drawRectInternal(shader, DX, dy, DW, 1f, UIPalette.RUSTIC_BORDER);
            dy += 18; ui.drawText(shader, "REWARD", DX + 4, dy, 0.50f, UIPalette.TACT_ORANGE);
            dy += 26;
            for (Item ri : sel.reward.items) {
                Vector4f qCol = qualityColour(ri.getQuality());
                if (ri.hasQuality()) {
                    float bw = DW - 8, bh = 30f;
                    ui.drawRectInternal(shader, DX, dy, bw, bh, new Vector4f(0.05f, 0.05f, 0.10f, 0.85f));
                    ui.drawRectInternal(shader, DX, dy, bw, 2f, qCol);
                    ui.drawRectInternal(shader, DX, dy + bh - 2, bw, 2f, qCol);
                    ui.drawText(shader, ri.getDisplayName(), DX + 8, dy + 7, 0.55f, qCol);
                    if (ri.getQuality() == Item.QualityTier.HEIRLOOM) {
                        float sh = 0.55f + 0.35f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 600.0));
                        ui.drawRectInternal(shader, DX, dy, bw * sh, 2f, new Vector4f(1f, 0.85f, 0.30f, 0.70f));
                    }
                    dy += 34;
                } else {
                    ui.drawText(shader, ri.getDisplayName(), DX + 4, dy + 4, 0.50f, UIPalette.TEXT_COLOR);
                    dy += 22;
                }
            }
            if (sel.reward.xp > 0) {
                ui.drawText(shader, "+ " + sel.reward.xp + " XP", DX + 4, dy + 4, 0.50f, new Vector4f(0.95f, 0.85f, 0.15f, 1f));
                dy += 26;
            }
            
            acceptBtnVisible = false;
            if (sel.state == Quest.State.AVAILABLE) {
                dy += 10f;
                acceptBtnX = DX + (DW - acceptBtnW) / 2f;
                acceptBtnY = dy;
                acceptBtnVisible = true;
                
                ui.drawRectInternal(shader, acceptBtnX, acceptBtnY, acceptBtnW, acceptBtnH, new Vector4f(0.12f, 0.63f, 1f, 0.8f));
                ui.drawRectInternal(shader, acceptBtnX, acceptBtnY, acceptBtnW, 2f, UIPalette.TACT_BLUE);
                ui.drawRectInternal(shader, acceptBtnX, acceptBtnY + acceptBtnH - 2f, acceptBtnW, 2f, UIPalette.TACT_BLUE);
                ui.drawText(shader, "ACCEPT QUEST", acceptBtnX + 22, acceptBtnY + 10, 0.55f, new Vector4f(1f, 1f, 1f, 1f));
            }
        }
        glDisable(GL_BLEND); glEnable(GL_DEPTH_TEST);
    }

    private static Vector4f qualityColour(Item.QualityTier q) {
        if (q == null) return new Vector4f(1f, 1f, 1f, 1f);
        int c = q.colour;
        return new Vector4f(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
    }

    private static Vector4f questStateColour(Quest.State state) {
        switch (state) {
            case COMPLETED: return new Vector4f(0f, 0.88f, 0.36f, 1f);
            case IN_PROGRESS: return new Vector4f(0.95f, 0.48f, 0f, 1f);
            case AVAILABLE: return new Vector4f(0.12f, 0.63f, 1f, 1f);
            default: return new Vector4f(0.30f, 0.30f, 0.30f, 1f);
        }
    }

    private static String questStateIcon(Quest.State state) {
        switch (state) {
            case COMPLETED: return "[V]";
            case IN_PROGRESS: return "[>]";
            case AVAILABLE: return "[ ]";
            default: return "[L]";
        }
    }
    
    public void setTab(Quest.Phase p) { this.questPhaseTab = p; }
    public void scroll(float d) { this.questListScroll += d; }
    public void select(int delta, int size) { 
        if (size == 0) return;
        questSelectedIndex = (questSelectedIndex + delta + size) % size;
    }

    public boolean handleInput(float x, float y, boolean clicked, int width, int height, Main main) {
        if (!clicked) return false;
        
        final float PW = Math.min(980f, width - 60), PH = Math.min(660f, height - 60);
        final float PX = (width - PW) / 2f, PY = (height - PH) / 2f;
        final float TAB_H = 32f, LPAD = 16f, LIST_W = 230f;
        final float BODY_Y = PY + TAB_H + 40f, BODY_H = PH - TAB_H - 50f;
        final float SLOT_H = 42f, SLOT_G = 4f;

        // Check tabs
        Quest.Phase[] phases = Quest.Phase.values();
        float tabW = (PW - LPAD * 2) / phases.length;
        for (int i = 0; i < phases.length; i++) {
            float tx = PX + LPAD + i * tabW, ty = PY + 48f;
            if (x >= tx && x <= tx + tabW - 3f && y >= ty && y <= ty + TAB_H) {
                questPhaseTab = phases[i];
                questSelectedIndex = 0;
                questListScroll = 0f;
                return true;
            }
        }

        // Check list items
        List<Quest> phaseQuests = main.questManager.getByPhase(questPhaseTab);
        float listX = PX + LPAD, listY = BODY_Y;
        for (int i = 0; i < phaseQuests.size(); i++) {
            float ey = listY + i * (SLOT_H + SLOT_G) - questListScroll;
            if (ey + SLOT_H < listY - 10 || ey > listY + BODY_H + 10) continue;
            if (x >= listX && x <= listX + LIST_W && y >= ey && y <= ey + SLOT_H) {
                questSelectedIndex = i;
                return true;
            }
        }

        // Check Accept button
        if (acceptBtnVisible && !phaseQuests.isEmpty() && questSelectedIndex >= 0 && questSelectedIndex < phaseQuests.size()) {
            if (x >= acceptBtnX && x <= acceptBtnX + acceptBtnW && y >= acceptBtnY && y <= acceptBtnY + acceptBtnH) {
                phaseQuests.get(questSelectedIndex).accept();
                return true;
            }
        }

        return false;
    }
}
