package io.github.lilkuzcodev.vibranium;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The machine layer: the vibranium fabricator (5x5 crafting station with its own
 * data-driven "fabricating" recipe type) and the vibranium extractor (the first
 * automated machine — mines a 3x3 column straight down).
 */
public final class VibraniumMachines {
	// ========================================================================
	//  EXTRACTOR TUNING — the single config spot for the mining machine
	// ------------------------------------------------------------------------
	/** Ticks per mined block at 1x speed (40 = one block every 2 seconds). */
	public static final int MINE_INTERVAL_TICKS = 40;
	/** Column footprint radius around the machine column (1 = 3x3). */
	public static final int COLUMN_RADIUS = 1;
	/** Fuel burns at standard furnace rates via {@code level.fuelValues()} (coal = 1600 ticks). */
	// (no knob needed — the vanilla fuel map is used directly)
	/** A vibranium ingot burns this long as extractor fuel... */
	public static final int VIBRANIUM_FUEL_BURN_TICKS = 1600; // same duration as coal...
	/** ...but mines this many times faster for that whole burn. */
	public static final int VIBRANIUM_SPEED_MULT = 4;
	/** How many empty layers the head may skip per tick (bounds cave descent cost). */
	public static final int MAX_LAYER_SKIPS_PER_TICK = 8;
	/** Ticks between attempts to push output into an adjacent chest/barrel. */
	public static final int CHEST_PUSH_INTERVAL_TICKS = 20;
	// ========================================================================

	// Custom recipe type: fabricating recipes live in data/<ns>/recipe/*.json with
	// "type": "vibranium:fabricating" and support patterns up to 5x5. The vanilla
	// crafting table never queries this type, so fabricating recipes are
	// craftable ONLY at a fabricator.
	public static final RecipeType<FabricatingRecipe> FABRICATING = Registry.register(
			BuiltInRegistries.RECIPE_TYPE, Vibranium.id("fabricating"), new RecipeType<FabricatingRecipe>() {
				@Override
				public String toString() {
					return "vibranium:fabricating";
				}
			});
	public static final RecipeSerializer<FabricatingRecipe> FABRICATING_SERIALIZER = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER, Vibranium.id("fabricating"),
			new RecipeSerializer<>(FabricatingRecipe.MAP_CODEC, FabricatingRecipe.STREAM_CODEC));

	// Menu (screen handler) types. A future SUPER FABRICATOR tier registers its
	// own MenuType here with different grid dimensions (see FabricatorMenu's
	// protected constructor) — nothing below assumes there is only one tier.
	public static final MenuType<FabricatorMenu> FABRICATOR_MENU = Registry.register(
			BuiltInRegistries.MENU, Vibranium.id("fabricator").toString(),
			new MenuType<>(FabricatorMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<ExtractorMenu> EXTRACTOR_MENU = Registry.register(
			BuiltInRegistries.MENU, Vibranium.id("extractor").toString(),
			new MenuType<>(ExtractorMenu::new, FeatureFlags.VANILLA_SET));

	public static final BlockEntityType<ExtractorBlockEntity> EXTRACTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Vibranium.id("vibranium_extractor"),
			new BlockEntityType<>(ExtractorBlockEntity::new, java.util.Set.of(VibraniumBlocks.VIBRANIUM_EXTRACTOR)));

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.insertAfter(Items.CRAFTING_TABLE, VibraniumBlocks.VIBRANIUM_FABRICATOR);
			output.insertAfter(Items.BLAST_FURNACE, VibraniumBlocks.VIBRANIUM_EXTRACTOR);
		});
	}

	private VibraniumMachines() {
	}
}
