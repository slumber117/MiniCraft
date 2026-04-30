package minicraft.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * A single quest entry with objectives, reward, and lifecycle state.
 */
public class Quest {

    public enum Phase {
        NOVICE   ("Novice",   "I"),
        EXPLORER ("Explorer", "II"),
        VETERAN  ("Veteran",  "III"),
        MASTER   ("Master",   "IV");

        public final String displayName;
        public final String numeral;
        Phase(String displayName, String numeral) {
            this.displayName = displayName;
            this.numeral     = numeral;
        }
    }

    public enum State {
        LOCKED,       // prerequisites not met
        AVAILABLE,    // ready to start
        IN_PROGRESS,  // at least one objective touched
        COMPLETED     // all objectives done, reward claimed
    }

    // ── Identity ──────────────────────────────────────────────────────────
    public final String id;
    public final String title;
    public final String description;
    public final Phase  phase;

    // ── Requirements ─────────────────────────────────────────────────────
    public final List<String> prerequisites;   // ids of quests that must be COMPLETED first

    // ── Content ──────────────────────────────────────────────────────────
    public final List<QuestObjective> objectives;
    public final QuestReward reward;

    // ── Runtime state ────────────────────────────────────────────────────
    public State state = State.LOCKED;

    public Quest(String id, String title, String description, Phase phase,
                 List<String> prerequisites,
                 List<QuestObjective> objectives,
                 QuestReward reward) {
        this.id            = id;
        this.title         = title;
        this.description   = description;
        this.phase         = phase;
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        this.objectives    = objectives;
        this.reward        = reward;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    public boolean allObjectivesDone() {
        for (QuestObjective o : objectives) if (!o.isDone()) return false;
        return true;
    }

    /** Returns overall progress fraction (0..1) averaged across objectives. */
    public float totalProgress() {
        if (objectives.isEmpty()) return 1f;
        float sum = 0;
        for (QuestObjective o : objectives) sum += o.getProgressFraction();
        return sum / objectives.size();
    }

    /** Explicitly accept the quest if it's available. */
    public void accept() {
        if (state == State.AVAILABLE) {
            state = State.IN_PROGRESS;
        }
    }
}
