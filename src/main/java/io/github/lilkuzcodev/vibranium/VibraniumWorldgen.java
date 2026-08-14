package io.github.lilkuzcodev.vibranium;

import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Attaches the vibranium ore features to all overworld biomes.
 *
 * <p>The actual spawn-rate tuning (batch counts, rarity, vein sizes, air-discard) lives in
 * ONE place: the constants block at the top of {@code tools/gen-worldgen.js}, which generates
 * the feature JSON under {@code src/main/resources/data/vibranium/worldgen/}. Edit the knobs
 * there, re-run the script, rebuild.
 */
public final class VibraniumWorldgen {
	public static final ResourceKey<PlacedFeature> ORE_VIBRANIUM = key("ore_vibranium");
	public static final ResourceKey<PlacedFeature> ORE_VIBRANIUM_MEDIUM = key("ore_vibranium_medium");
	public static final ResourceKey<PlacedFeature> ORE_VIBRANIUM_LARGE = key("ore_vibranium_large");
	public static final ResourceKey<PlacedFeature> ORE_VIBRANIUM_BURIED = key("ore_vibranium_buried");
	public static final ResourceKey<PlacedFeature> VIBRANIUM_PIT = key("vibranium_pit");

	private static ResourceKey<PlacedFeature> key(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Vibranium.id(name));
	}

	public static void init() {
		for (ResourceKey<PlacedFeature> feature : List.of(ORE_VIBRANIUM, ORE_VIBRANIUM_MEDIUM, ORE_VIBRANIUM_LARGE, ORE_VIBRANIUM_BURIED)) {
			BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, feature);
		}
		// pits generate in the same step vanilla geodes use
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.LOCAL_MODIFICATIONS, VIBRANIUM_PIT);
	}

	private VibraniumWorldgen() {
	}
}
