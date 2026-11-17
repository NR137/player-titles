package de.nr137.playertitles.loot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Title name tags ("loot" in config.json).
 *
 * @param markChances       per loot table, the chance that a name tag from it becomes a title name tag (the others
 *                          stay vanilla name tags): chests by their loot table, fishing as
 *                          "minecraft:gameplay/fishing"
 * @param otherMarkChance   for loot tables not listed (other mods, data packs)
 * @param blankChance       the chance that a title name tag is blank: used up, no title
 * @param lastBlankChance   the chance of a blank while only the title for having them all ("loot_complete") is
 *                          missing
 */
public record LootConfig(Map<String, Double> markChances, double otherMarkChance, double blankChance, double lastBlankChance) {
	public static final String FISHING = "minecraft:gameplay/fishing";

	public static LootConfig defaults() {
		Map<String, Double> chances = new LinkedHashMap<>();
		chances.put("minecraft:chests/abandoned_mineshaft", 0.85);
		chances.put("minecraft:chests/simple_dungeon", 0.85);
		chances.put(FISHING, 0.5);
		return new LootConfig(chances, 0.5, 0.05, 0.8);
	}

	public double markChance(String lootTable) {
		return markChances == null ? otherMarkChance : markChances.getOrDefault(lootTable, otherMarkChance);
	}
}
