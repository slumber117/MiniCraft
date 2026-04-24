package minicraft.quest;

import minicraft.item.Item;
import minicraft.item.ToolItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the reward granted when a quest is completed.
 */
public class QuestReward {

    public final List<Item> items = new ArrayList<>();
    public final int xp;

    public QuestReward(int xp) {
        this.xp = xp;
    }

    // ── Fluent builder ────────────────────────────────────────────────────

    public QuestReward addTool(String name, ToolItem.ToolType toolType,
                               int harvestLevel, float baseEfficiency,
                               String textureName, Item.QualityTier quality) {
        items.add(new ToolItem(name, toolType, harvestLevel, baseEfficiency, textureName, quality));
        return this;
    }

    public QuestReward addItem(Item item) {
        items.add(item);
        return this;
    }

    // ── Static factories ─────────────────────────────────────────────────

    /** Convenience: single tool reward + XP. */
    public static QuestReward tool(String name, ToolItem.ToolType toolType,
                                   int harvestLevel, float baseEfficiency,
                                   String textureName, Item.QualityTier quality,
                                   int xp) {
        return new QuestReward(xp)
                .addTool(name, toolType, harvestLevel, baseEfficiency, textureName, quality);
    }

    /** Convenience: XP only. */
    public static QuestReward xpOnly(int xp) {
        return new QuestReward(xp);
    }
}
