package io.github.lilkuzcodev.vibranium;

import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Attaches the godite ore features to all overworld biomes.
 *
 * <p>Godite is FIVE TIMES rarer than vibranium, which is itself five times rarer than
 * diamond — one godite for every twenty-five diamond. Like vibranium's, the rate lives in
 * exactly one place: {@code DENSITY_DIVISOR} and the {@code MATERIALS} table at the top of
 * {@code tools/gen-worldgen.js}, which generates the feature JSON under
 * {@code src/main/resources/data/vibranium/worldgen/}. Edit there, re-run, rebuild.
 *
 * <p>Four batches, no pit: vibranium's geode-style pit is its own landmark and godite does
 * not get one.
 */
public final class GoditeWorldgen {
	public static final ResourceKey<PlacedFeature> ORE_GODITE = key("ore_godite");
	public static final ResourceKey<PlacedFeature> ORE_GODITE_MEDIUM = key("ore_godite_medium");
	public static final ResourceKey<PlacedFeature> ORE_GODITE_LARGE = key("ore_godite_large");
	public static final ResourceKey<PlacedFeature> ORE_GODITE_BURIED = key("ore_godite_buried");

	private static ResourceKey<PlacedFeature> key(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Vibranium.id(name));
	}

	public static void init() {
		for (ResourceKey<PlacedFeature> feature : List.of(ORE_GODITE, ORE_GODITE_MEDIUM, ORE_GODITE_LARGE, ORE_GODITE_BURIED)) {
			BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, feature);
		}
	}

	private GoditeWorldgen() {
	}
}
