package fr.mathildeuh.worldmanager.dialogs;

import org.bukkit.GameRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Buckets vanilla game rules into the same rough categories Minecraft's own F3 game
 * rule screen uses, purely for a friendlier browsing experience - any rule not listed
 * here (a newer vanilla rule, or one added by another plugin/mod) falls into "Other".
 */
final class GameRuleCategories {

    static final String OTHER = "other";

    private static final Map<String, Set<String>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("player", Set.of(
                "announceAdvancements", "disableElytraMovementCheck", "doImmediateRespawn",
                "fallDamage", "fireDamage", "freezeDamage", "drowningDamage", "keepInventory",
                "naturalRegeneration", "playersSleepingPercentage", "showDeathMessages",
                "spawnRadius", "spectatorsGenerateChunks"
        ));
        CATEGORIES.put("mobs", Set.of(
                "doMobLoot", "doMobSpawning", "doPatrolSpawning", "doTraderSpawning",
                "doWardenSpawning", "mobGriefing", "forgiveDeadPlayers", "universalAnger",
                "disableRaids", "doInsomnia", "projectilesCanBreakBlocks"
        ));
        CATEGORIES.put("world", Set.of(
                "doDaylightCycle", "doWeatherCycle", "doFireTick", "doVinesSpread",
                "randomTickSpeed", "doTileDrops", "doEntityDrops", "snowAccumulationHeight",
                "waterSourceConversion", "lavaSourceConversion", "tntExplodes",
                "blockExplosionDropDecay", "mobExplosionDropDecay", "tntExplosionDropDecay",
                "doLimitedCrafting"
        ));
        CATEGORIES.put("chat", Set.of(
                "commandBlockOutput", "logAdminCommands", "sendCommandFeedback"
        ));
        CATEGORIES.put("misc", Set.of(
                "reducedDebugInfo", "maxEntityCramming", "maxCommandChainLength",
                "commandModificationBlockLimit", "spawnChunkRadius"
        ));
    }

    private GameRuleCategories() { }

    /** Ordered category keys, "other" always last. */
    static Iterable<String> orderedKeys() {
        List<String> keys = new ArrayList<>(CATEGORIES.keySet());
        keys.add(OTHER);
        return keys;
    }

    static String categoryOf(GameRule<?> rule) {
        for (Map.Entry<String, Set<String>> entry : CATEGORIES.entrySet()) {
            if (entry.getValue().contains(rule.getName())) {
                return entry.getKey();
            }
        }
        return OTHER;
    }

    /** Splits camelCase into readable "Title Case" text, e.g. "doFireTick" -> "Do Fire Tick". */
    static String readableName(String ruleName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ruleName.length(); i++) {
            char c = ruleName.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
