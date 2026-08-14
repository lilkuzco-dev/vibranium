#!/usr/bin/env node
// Generates the vibranium worldgen JSON (configured + placed features) into
// src/main/resources/data/vibranium/worldgen/. Run after changing any knob:
//
//     node tools/gen-worldgen.js
//
// then rebuild. The JSON shape mirrors vanilla 26.2 diamond exactly (extracted
// from the game jar), with an added chunk-rarity gate to scale density down.

// ============================================================================
//  SPAWN-RATE TUNING — the single config spot
// ----------------------------------------------------------------------------
//  Vanilla 26.2 diamond generates FOUR batches per chunk (measured ~26 ore
//  blocks/chunk on this project's test seed — regional variance is large,
//  11-26/chunk, so tune by RATIO, not absolute count):
//    small:  7 blobs/chunk, size 4,  50% discarded when generating against air
//    medium: 2 blobs/chunk, size 8,  50% air-discard, uniform Y -64..-4
//    large:  1 blob per 9 chunks, size 12, 70% air-discard
//    buried: 4 blobs/chunk, size 8, 100% air-discard (only fully buried)
//  All but medium use a trapezoid Y-distribution (above_bottom -80..+80),
//  peaking at the bottom of the world.
//
//  Vibranium = every diamond batch, gated to 1 in DENSITY_DIVISOR chunks.
//  Each batch's expected output is therefore exactly diamond/DENSITY_DIVISOR.
//  Measured with divisor 5 over 450 fresh chunks (2026-08-14): vibranium
//  4.71/chunk vs diamond 25.06/chunk = 5.3:1 (per-region 5.0:1 and 5.6:1).
//  The trade-off vs fractional per-chunk counts: distribution is clumpier — a
//  qualifying chunk gets diamond-like density, the rest only see the rare
//  large blob.
// ============================================================================
const DENSITY_DIVISOR = 5; // vibranium is this many times rarer than diamond

// Per-batch definitions (counts/sizes/discards = vanilla diamond's own values).
// rarity is "1 in N chunks"; a rarity of 1 means every chunk.
const BATCHES = {
	small:  { count: 7, rarity: DENSITY_DIVISOR,     size: 4,  discard: 0.5, height: "trapezoid" },
	medium: { count: 2, rarity: DENSITY_DIVISOR,     size: 8,  discard: 0.5, height: "uniform" },
	large:  { count: 1, rarity: 9 * DENSITY_DIVISOR, size: 12, discard: 0.7, height: "trapezoid" },
	buried: { count: 4, rarity: DENSITY_DIVISOR,     size: 8,  discard: 1.0, height: "trapezoid" },
};
// ============================================================================

const fs = require("node:fs");
const path = require("node:path");

const ORE = "vibranium:vibranium_ore";
const DEEPSLATE_ORE = "vibranium:deepslate_vibranium_ore";
const HEIGHTS = {
	// same trapezoid as diamond: peak density at the world bottom
	trapezoid: {
		type: "minecraft:height_range",
		height: { type: "minecraft:trapezoid", min_inclusive: { above_bottom: -80 }, max_inclusive: { above_bottom: 80 } },
	},
	// diamond-medium's band: always inside the world
	uniform: {
		type: "minecraft:height_range",
		height: { type: "minecraft:uniform", min_inclusive: { absolute: -64 }, max_inclusive: { absolute: -4 } },
	},
};

function configured(batch) {
	return {
		type: "minecraft:ore",
		config: {
			discard_chance_on_air_exposure: batch.discard,
			size: batch.size,
			targets: [
				{ state: { Name: ORE }, target: { predicate_type: "minecraft:tag_match", tag: "minecraft:stone_ore_replaceables" } },
				{ state: { Name: DEEPSLATE_ORE }, target: { predicate_type: "minecraft:tag_match", tag: "minecraft:deepslate_ore_replaceables" } },
			],
		},
	};
}
function placed(configuredId, batch) {
	const placement = [];
	if (batch.rarity > 1) placement.push({ type: "minecraft:rarity_filter", chance: batch.rarity });
	if (batch.count !== 1) placement.push({ type: "minecraft:count", count: batch.count });
	placement.push({ type: "minecraft:in_square" }, HEIGHTS[batch.height], { type: "minecraft:biome" });
	return { feature: configuredId, placement };
}

// File names follow vanilla's pattern: the "small" batch is the unsuffixed one.
const WORLDGEN = path.join(__dirname, "..", "src/main/resources/data/vibranium/worldgen");
const files = {};
for (const [name, batch] of Object.entries(BATCHES)) {
	files[`configured_feature/ore_vibranium_${name}.json`] = configured(batch);
	const placedName = name === "small" ? "ore_vibranium" : `ore_vibranium_${name}`;
	files[`placed_feature/${placedName}.json`] = placed(`vibranium:ore_vibranium_${name}`, batch);
}
for (const [rel, json] of Object.entries(files)) {
	const file = path.join(WORLDGEN, rel);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(json, null, 2) + "\n");
	console.log(`wrote ${rel}`);
}
