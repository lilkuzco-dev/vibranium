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
	public static final Item VIBRANIUM_GEM = register("vibranium_gem", new Item.Properties());

	private static Item register(String name, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Vibranium.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
	}

	public static void init() {
		// Slot our content into the vanilla creative tabs next to its diamond counterparts.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output ->
				output.insertAfter(Items.DEEPSLATE_DIAMOND_ORE, VibraniumBlocks.VIBRANIUM_ORE, VibraniumBlocks.DEEPSLATE_VIBRANIUM_ORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				output.insertAfter(Items.DIAMOND_BLOCK, VibraniumBlocks.BLOCK_OF_VIBRANIUM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.DIAMOND, VIBRANIUM_GEM));
	}

	private VibraniumItems() {
	}
}
