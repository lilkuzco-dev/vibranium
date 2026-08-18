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
const GODITE_DIVISOR = 5;  // godite is this many times rarer AGAIN than vibranium

// The two metals. Everything below is generated once per entry, so the ONLY difference
// between vibranium's ore and godite's is the divisor on this table: godite comes out at
// 5 x 5 = 25 times rarer than diamond, which is exactly "five times harder to find than
// vibranium" measured the same way vibranium's own 5:1 was (/vibranium_census).
const MATERIALS = [
	{ name: "vibranium", divisor: DENSITY_DIVISOR, ore: "vibranium:vibranium_ore", deepslate: "vibranium:deepslate_vibranium_ore" },
	{ name: "godite", divisor: DENSITY_DIVISOR * GODITE_DIVISOR, ore: "vibranium:godite_ore", deepslate: "vibranium:deepslate_godite_ore" },
];

// Per-batch definitions (counts/sizes/discards = vanilla diamond's own values).
// rarity is "1 in N chunks"; a rarity of 1 means every chunk.
const batchesFor = (divisor) => ({
	small:  { count: 7, rarity: divisor,     size: 4,  discard: 0.5, height: "trapezoid" },
	medium: { count: 2, rarity: divisor,     size: 8,  discard: 0.5, height: "uniform" },
	large:  { count: 1, rarity: 9 * divisor, size: 12, discard: 0.7, height: "trapezoid" },
	buried: { count: 4, rarity: divisor,     size: 8,  discard: 1.0, height: "trapezoid" },
});
// ============================================================================

// ============================================================================
//  VIBRANIUM PIT TUNING — geode-style hollow deposit (second config spot)
// ----------------------------------------------------------------------------
//  Built on vanilla's minecraft:geode feature (amethyst geodes' triple-shell
//  architecture): smooth basalt casing -> veinstone shell -> dense vibranium
//  lining with raw-block "jackpot" studs -> hollow, crystal-lit center.
// ============================================================================
const PIT_RARITY_CHUNKS = 240;      // 1 per N chunks (amethyst geodes: 24 -> 10x rarer)
const PIT_Y_MIN = -55;              // deepslate depths, never breaching the surface
const PIT_Y_MAX = -20;
const PIT_WALL_DISTANCE = { min: 4, max: 7 }; // shell radius -> ~12-18 blocks across
// Inner lining mix — the yield knob. Ore fraction of the inner shell controls the
// 40-80 target (measured; veinstone dilutes, raw-block studs add the jackpot).
const PIT_INNER_MIX = [
	["vibranium:vibranium_veinstone", 10],
	["vibranium:vibranium_ore", 1],
	["vibranium:deepslate_vibranium_ore", 1],
];
const PIT_RAW_JACKPOT_CHANCE = 0.06; // chance per inner-shell block to be raw_vibranium_block
const PIT_CRACK_CHANCE = 0.95;      // like amethyst geodes: usually has a visible opening
// ============================================================================

const fs = require("node:fs");
const path = require("node:path");

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

function configured(batch, material) {
	return {
		type: "minecraft:ore",
		config: {
			discard_chance_on_air_exposure: batch.discard,
			size: batch.size,
			targets: [
				{ state: { Name: material.ore }, target: { predicate_type: "minecraft:tag_match", tag: "minecraft:stone_ore_replaceables" } },
				{ state: { Name: material.deepslate }, target: { predicate_type: "minecraft:tag_match", tag: "minecraft:deepslate_ore_replaceables" } },
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
for (const material of MATERIALS) {
	for (const [name, batch] of Object.entries(batchesFor(material.divisor))) {
		files[`configured_feature/ore_${material.name}_${name}.json`] = configured(batch, material);
		const placedName = name === "small" ? `ore_${material.name}` : `ore_${material.name}_${name}`;
		files[`placed_feature/${placedName}.json`] = placed(`vibranium:ore_${material.name}_${name}`, batch);
	}
}
// ---------- vibranium pit (geode feature) — vibranium only; godite is plain ore ----------
files["configured_feature/vibranium_pit.json"] = {
	type: "minecraft:geode",
	config: {
		blocks: {
			filling_provider: { type: "minecraft:simple_state_provider", state: { Name: "minecraft:air" } },
			inner_layer_provider: {
				type: "minecraft:weighted_state_provider",
				entries: PIT_INNER_MIX.map(([name, weight]) => ({ weight, data: { Name: name } })),
			},
			alternate_inner_layer_provider: { type: "minecraft:simple_state_provider", state: { Name: "vibranium:raw_vibranium_block" } },
			middle_layer_provider: { type: "minecraft:simple_state_provider", state: { Name: "vibranium:vibranium_veinstone" } },
			outer_layer_provider: { type: "minecraft:simple_state_provider", state: { Name: "minecraft:smooth_basalt" } },
			inner_placements: [
				{ Name: "vibranium:vibranium_crystal_cluster", Properties: { facing: "up", waterlogged: "false" } },
			],
			cannot_replace: "#minecraft:features_cannot_replace",
			invalid_blocks: "#minecraft:geode_invalid_blocks",
		},
		crack: { generate_crack_chance: PIT_CRACK_CHANCE },
		invalid_blocks_threshold: 1,
		layers: {},
		outer_wall_distance: { type: "minecraft:uniform", min_inclusive: PIT_WALL_DISTANCE.min, max_inclusive: PIT_WALL_DISTANCE.max },
		use_alternate_layer0_chance: PIT_RAW_JACKPOT_CHANCE,
	},
};
files["placed_feature/vibranium_pit.json"] = {
	feature: "vibranium:vibranium_pit",
	placement: [
		{ type: "minecraft:rarity_filter", chance: PIT_RARITY_CHUNKS },
		{ type: "minecraft:in_square" },
		{ type: "minecraft:height_range", height: { type: "minecraft:uniform", min_inclusive: { absolute: PIT_Y_MIN }, max_inclusive: { absolute: PIT_Y_MAX } } },
		{ type: "minecraft:biome" },
	],
};

for (const [rel, json] of Object.entries(files)) {
	const file = path.join(WORLDGEN, rel);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(json, null, 2) + "\n");
	console.log(`wrote ${rel}`);
}
