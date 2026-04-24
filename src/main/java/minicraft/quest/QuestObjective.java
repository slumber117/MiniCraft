package minicraft.quest;

/**
 * A single trackable objective within a Quest.
 */
public class QuestObjective {

    public enum Type {
        MINE_BLOCK,       // targetId = Block.name()
        KILL_ENTITY,      // targetId = EntityType.name()
        CRAFT_ITEM,       // targetId = item name
        COLLECT_ITEM,     // targetId = item name
        REACH_DEPTH,      // targetId = max Y (as string)
        REACH_LEVEL       // targetId = player level (as string)
    }

    public final String description;
    public final Type   type;
    public final String targetId;
    public final int    required;
    private int current = 0;

    public QuestObjective(String description, Type type, String targetId, int required) {
        this.description = description;
        this.type        = type;
        this.targetId    = targetId;
        this.required    = required;
    }

    public int getCurrent()  { return current; }
    public boolean isDone()  { return current >= required; }

    /** Advances progress by {@code amount}. Does not exceed {@code required}. */
    public void advance(int amount) {
        current = Math.min(current + amount, required);
    }

    /** Force-sets progress (e.g. for REACH_DEPTH). */
    public void set(int value) {
        current = Math.min(value, required);
    }

    public String getProgressText() {
        return current + " / " + required;
    }

    public float getProgressFraction() {
        return (float) current / required;
    }
}
