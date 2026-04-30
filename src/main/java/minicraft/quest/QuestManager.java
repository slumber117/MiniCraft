package minicraft.quest;

import minicraft.entity.Player;
import minicraft.entity.EntityType;
import minicraft.item.Item;
import minicraft.item.ToolItem;
import minicraft.world.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central quest registry and runtime event processor.
 *
 * Usage:
 *   questManager.onBlockMined(block)      — call when a block is destroyed
 *   questManager.onEntityKilled(type)     — call when an entity dies to the player
 *   questManager.onItemCrafted(item)      — call after crafting
 *   questManager.tick(player)             — call every game tick for depth/level checks
 *   questManager.checkCompletions(player) — call every tick; grants rewards automatically
 */
public class QuestManager {

    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestManager() {
        registerAllQuests();
        unlockAvailable();
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUEST LIBRARY
    // ══════════════════════════════════════════════════════════════════════

    private void registerAllQuests() {

        // ── PHASE 1 — NOVICE ─────────────────────────────────────────────

        register(quest("novice_first_strike",
                "First Strike",
                "Mine your first blocks to get started.",
                Quest.Phase.NOVICE,
                none(),
                objectives(
                    obj("Mine 5 Dirt or Grass", QuestObjective.Type.MINE_BLOCK, "DIRT", 5)
                ),
                QuestReward.tool(
                    "Apprentice Wooden Shovel",
                    ToolItem.ToolType.SHOVEL, 0, 2.5f,
                    "item_shovel_wood", Item.QualityTier.APPRENTICE, 20
                )
        ));

        register(quest("novice_lumberjack",
                "Lumberjack",
                "Chop down trees to gather wood.",
                Quest.Phase.NOVICE,
                none(),
                objectives(
                    obj("Chop 10 Wood", QuestObjective.Type.MINE_BLOCK, "OAK_WOOD", 10)
                ),
                QuestReward.tool(
                    "Journeyman Wooden Axe",
                    ToolItem.ToolType.AXE, 0, 2.5f,
                    "item_axe_wood", Item.QualityTier.JOURNEYMAN, 25
                )
        ));

        register(quest("novice_stone_age",
                "Stone Age",
                "Gather stone for your first real tools.",
                Quest.Phase.NOVICE,
                list("novice_first_strike"),
                objectives(
                    obj("Mine 15 Stone", QuestObjective.Type.MINE_BLOCK, "STONE", 15)
                ),
                QuestReward.tool(
                    "Ramshackle Stone Pickaxe",
                    ToolItem.ToolType.PICKAXE, 1, 3.5f,
                    "item_pick_stone", Item.QualityTier.RAMSHACKLE, 30
                )
        ));

        register(quest("novice_survivor",
                "Survivor",
                "Level up to prove your survival instincts.",
                Quest.Phase.NOVICE,
                list("novice_stone_age"),
                objectives(
                    obj("Reach Level 3", QuestObjective.Type.REACH_LEVEL, "3", 1)
                ),
                QuestReward.xpOnly(50)
        ));

        // ── PHASE 2 — EXPLORER ────────────────────────────────────────────

        register(quest("explorer_spelunker",
                "Spelunker",
                "Venture deep and mine coal for fuel.",
                Quest.Phase.EXPLORER,
                list("novice_survivor"),
                objectives(
                    obj("Mine 30 Coal Ore", QuestObjective.Type.MINE_BLOCK, "COAL_ORE", 30)
                ),
                QuestReward.tool(
                    "Journeyman Stone Pickaxe",
                    ToolItem.ToolType.PICKAXE, 1, 4.0f,
                    "item_pick_stone", Item.QualityTier.JOURNEYMAN, 40
                )
        ));

        register(quest("explorer_first_blood",
                "First Blood",
                "Prove yourself in combat — slay five zombies.",
                Quest.Phase.EXPLORER,
                list("novice_survivor"),
                objectives(
                    obj("Kill 5 Zombies", QuestObjective.Type.KILL_ENTITY, "ZOMBIE", 5)
                ),
                QuestReward.tool(
                    "MasterCraft Iron Sword",
                    ToolItem.ToolType.SWORD, 2, 8.0f,
                    "item_sword_iron", Item.QualityTier.MASTERCRAFT, 60
                )
        ));

        register(quest("explorer_prospector",
                "Prospector",
                "Find and extract iron ore from the underground.",
                Quest.Phase.EXPLORER,
                list("explorer_spelunker"),
                objectives(
                    obj("Mine 10 Iron Ore", QuestObjective.Type.MINE_BLOCK, "IRON_ORE", 10)
                ),
                QuestReward.tool(
                    "Apprentice Iron Pickaxe",
                    ToolItem.ToolType.PICKAXE, 2, 7.0f,
                    "item_pick_iron", Item.QualityTier.APPRENTICE, 50
                )
        ));

        register(quest("explorer_deep_roots",
                "Deep Roots",
                "Descend below Y=60 into the deep stone layer.",
                Quest.Phase.EXPLORER,
                list("explorer_prospector"),
                objectives(
                    obj("Reach Y < 60", QuestObjective.Type.REACH_DEPTH, "60", 1)
                ),
                new QuestReward(55)
                    .addTool("Journeyman Iron Axe",
                             ToolItem.ToolType.AXE, 2, 7.0f,
                             "item_axe_iron", Item.QualityTier.JOURNEYMAN)
                    .addItem(new Item("TORCH", Block.TORCH, null, 64))
        ));

        // ── PHASE 3 — VETERAN ─────────────────────────────────────────────

        register(quest("veteran_geode_hunter",
                "Geode Hunter",
                "Seek out rare crystal geodes in the deep.",
                Quest.Phase.VETERAN,
                list("explorer_deep_roots"),
                objectives(
                    obj("Mine 3 Tanzanite Ore", QuestObjective.Type.MINE_BLOCK, "TANZANITE_ORE", 3)
                ),
                QuestReward.tool(
                    "MasterCraft Diamond Pickaxe",
                    ToolItem.ToolType.PICKAXE, 4, 14.0f,
                    "item_pick_diamond", Item.QualityTier.MASTERCRAFT, 100
                )
        ));

        register(quest("veteran_slayer",
                "Slayer",
                "Become a seasoned monster hunter.",
                Quest.Phase.VETERAN,
                list("explorer_first_blood"),
                objectives(
                    obj("Kill 25 Enemies", QuestObjective.Type.KILL_ENTITY, "*", 25)
                ),
                QuestReward.tool(
                    "Legendary Ruby Sword",
                    ToolItem.ToolType.SWORD, 5, 18.0f,
                    "item_sword_ruby", Item.QualityTier.LEGENDARY, 150
                )
        ));

        register(quest("veteran_the_deep",
                "The Deep",
                "Reach the bedrock core of the world.",
                Quest.Phase.VETERAN,
                list("veteran_geode_hunter"),
                objectives(
                    obj("Reach Y < 20", QuestObjective.Type.REACH_DEPTH, "20", 1)
                ),
                QuestReward.tool(
                    "Legendary Diamond Pickaxe",
                    ToolItem.ToolType.PICKAXE, 4, 16.0f,
                    "item_pick_diamond", Item.QualityTier.LEGENDARY, 200
                )
        ));

        // ── PHASE 4 — MASTER ──────────────────────────────────────────────

        register(quest("master_legendary_miner",
                "Legendary Miner",
                "Mine the rarest mineral on Earth — Serendibite.",
                Quest.Phase.MASTER,
                list("veteran_the_deep"),
                objectives(
                    obj("Mine 1 Serendibite Ore", QuestObjective.Type.MINE_BLOCK, "SERENDIBITE_ORE", 1)
                ),
                QuestReward.tool(
                    "Heirloom Serendibite Pickaxe",
                    ToolItem.ToolType.PICKAXE, 9, 60.0f,
                    "item_pick_serendibite", Item.QualityTier.HEIRLOOM, 500
                )
        ));

        register(quest("master_titan",
                "Titan",
                "Conquer the darkness — a hundred foes slain.",
                Quest.Phase.MASTER,
                list("veteran_slayer"),
                objectives(
                    obj("Kill 100 Enemies", QuestObjective.Type.KILL_ENTITY, "*", 100)
                ),
                QuestReward.tool(
                    "Legendary Adamantine Sword",
                    ToolItem.ToolType.SWORD, 5, 30.0f,
                    "item_sword_adamantine", Item.QualityTier.LEGENDARY, 400
                )
        ));

        register(quest("master_worlds_end",
                "World's End",
                "Touch the very bottom of the world.",
                Quest.Phase.MASTER,
                list("master_legendary_miner"),
                objectives(
                    obj("Reach Y = 1", QuestObjective.Type.REACH_DEPTH, "1", 1)
                ),
                QuestReward.tool(
                    "Heirloom Grandidierite Sword",
                    ToolItem.ToolType.SWORD, 8, 45.0f,
                    "item_sword_grandidierite", Item.QualityTier.HEIRLOOM, 600
                )
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // EVENT HOOKS  — called by Main.java
    // ══════════════════════════════════════════════════════════════════════

    public void onBlockMined(Block block) {
        String id = block.name();
        for (Quest q : quests.values()) {
            if (q.state != Quest.State.IN_PROGRESS) continue;
            for (QuestObjective o : q.objectives) {
                if (o.type == QuestObjective.Type.MINE_BLOCK && o.targetId.equals(id) && !o.isDone()) {
                    o.advance(1);
                }
            }
        }
    }

    public void onEntityKilled(EntityType type) {
        String id = type.name();
        for (Quest q : quests.values()) {
            if (q.state != Quest.State.IN_PROGRESS) continue;
            for (QuestObjective o : q.objectives) {
                if (o.type == QuestObjective.Type.KILL_ENTITY
                        && (o.targetId.equals("*") || o.targetId.equalsIgnoreCase(id))
                        && !o.isDone()) {
                    o.advance(1);
                }
            }
        }
    }

    public void onItemCrafted(Item item) {
        String name = item.getName();
        for (Quest q : quests.values()) {
            if (q.state != Quest.State.IN_PROGRESS) continue;
            for (QuestObjective o : q.objectives) {
                if (o.type == QuestObjective.Type.CRAFT_ITEM
                        && o.targetId.equalsIgnoreCase(name) && !o.isDone()) {
                    o.advance(1);
                }
            }
        }
    }

    /**
     * Called every tick to check depth/level objectives and auto-complete quests.
     * @return list of quests newly completed this tick (for notification display)
     */
    public List<Quest> tick(Player player) {
        int playerY    = (int) player.position.y;
        int playerLvl  = player.level;
        List<Quest> newlyCompleted = new ArrayList<>();

        for (Quest q : quests.values()) {
            if (q.state != Quest.State.IN_PROGRESS) continue;

            // REACH_DEPTH and REACH_LEVEL checks
            for (QuestObjective o : q.objectives) {
                if (o.isDone()) continue;

                if (o.type == QuestObjective.Type.REACH_DEPTH) {
                    int threshold = Integer.parseInt(o.targetId);
                    if (playerY <= threshold) {
                        o.set(1);
                    }
                } else if (o.type == QuestObjective.Type.REACH_LEVEL) {
                    int lvlTarget = Integer.parseInt(o.targetId);
                    o.set(playerLvl >= lvlTarget ? 1 : 0);
                }
            }

            // Auto-complete when all objectives satisfied
            if (q.allObjectivesDone() && q.state != Quest.State.COMPLETED) {
                q.state = Quest.State.COMPLETED;
                // Grant reward items to player
                for (Item item : q.reward.items) {
                    player.inventory.add(item, 1);
                }
                player.addXp(q.reward.xp, null);
                newlyCompleted.add(q);
                unlockAvailable(); // re-evaluate locked quests
            }
        }
        return newlyCompleted;
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUERY HELPERS
    // ══════════════════════════════════════════════════════════════════════

    public List<Quest> getByPhase(Quest.Phase phase) {
        List<Quest> result = new ArrayList<>();
        for (Quest q : quests.values()) if (q.phase == phase) result.add(q);
        return result;
    }

    public List<Quest> getAll() {
        return new ArrayList<>(quests.values());
    }

    // ══════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void register(Quest q) {
        quests.put(q.id, q);
    }

    /** After each completion, unlock any quests whose prerequisites are all done. */
    private void unlockAvailable() {
        for (Quest q : quests.values()) {
            if (q.state != Quest.State.LOCKED) continue;
            boolean prereqsMet = true;
            for (String pre : q.prerequisites) {
                Quest dep = quests.get(pre);
                if (dep == null || dep.state != Quest.State.COMPLETED) {
                    prereqsMet = false;
                    break;
                }
            }
            if (prereqsMet) q.state = Quest.State.AVAILABLE;
        }
    }

    // ── DSL helpers for the quest library ────────────────────────────────

    private static Quest quest(String id, String title, String description, Quest.Phase phase,
                               List<String> prereqs, List<QuestObjective> objs, QuestReward reward) {
        return new Quest(id, title, description, phase, prereqs, objs, reward);
    }

    private static List<String> none() { return Collections.emptyList(); }
    private static List<String> list(String... ids) { return Arrays.asList(ids); }

    private static List<QuestObjective> objectives(QuestObjective... objs) {
        return Arrays.asList(objs);
    }

    private static QuestObjective obj(String desc, QuestObjective.Type type, String target, int req) {
        return new QuestObjective(desc, type, target, req);
    }
}
