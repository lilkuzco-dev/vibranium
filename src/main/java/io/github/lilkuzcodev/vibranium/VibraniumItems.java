package io.github.lilkuzcodev.vibranium;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class VibraniumItems {
	// Metal identity, mirroring iron: the ore drops raw_vibranium, which smelts
	// (or blasts) into vibranium_ingot. Round 2 tools/weapons will craft from ingots.
	public static final Item RAW_VIBRANIUM = register("raw_vibranium", new Item.Properties());
	public static final Item VIBRANIUM_INGOT = register("vibranium_ingot", new Item.Properties());

	private static Item register(String name, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Vibranium.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
	}

	public static void init() {
		// Slot our content into the vanilla creative tabs next to its iron counterparts
		// (ores stay next to the diamond ores, matching their depth and rarity).
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
			output.insertAfter(Items.DEEPSLATE_DIAMOND_ORE, VibraniumBlocks.VIBRANIUM_ORE, VibraniumBlocks.DEEPSLATE_VIBRANIUM_ORE);
			output.insertAfter(Items.RAW_IRON_BLOCK, VibraniumBlocks.RAW_VIBRANIUM_BLOCK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				output.insertAfter(Items.IRON_BLOCK, VibraniumBlocks.BLOCK_OF_VIBRANIUM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(Items.RAW_IRON, RAW_VIBRANIUM);
			output.insertAfter(Items.IRON_INGOT, VIBRANIUM_INGOT);
		});
	}

	private VibraniumItems() {
	}
}
