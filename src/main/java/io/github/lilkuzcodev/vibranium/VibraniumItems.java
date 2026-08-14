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

	// Weapons: crafted directly (no smithing), fire-resistant as dropped items like netherite.
	// Stats live in VibraniumCombat.
	public static final Item VIBRANIUM_SWORD = register("vibranium_sword", VibraniumSwordItem::new,
			new Item.Properties()
					.sword(VibraniumCombat.VIBRANIUM_MATERIAL, VibraniumCombat.SWORD_BASE_DAMAGE, VibraniumCombat.SWORD_ATTACK_SPEED)
					.fireResistant());
	public static final Item VIBRANIUM_AXE = register("vibranium_axe",
			properties -> new VibraniumAxeItem(VibraniumCombat.VIBRANIUM_MATERIAL, VibraniumCombat.AXE_BASE_DAMAGE, VibraniumCombat.AXE_ATTACK_SPEED, properties),
			new Item.Properties().fireResistant());
	public static final Item VIBRANIUM_ENERGY_BALL = register("vibranium_energy_ball", KineticEnergyBallItem::new,
			new Item.Properties().stacksTo(VibraniumCombat.ENERGY_BALL_STACK_SIZE));

	private static Item register(String name, Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Vibranium.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
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
		// Weapons next to their netherite counterparts (swords: Combat; axes: Tools),
		// energy ball in Combat as specified.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(Items.NETHERITE_SWORD, VIBRANIUM_SWORD);
			output.accept(VIBRANIUM_ENERGY_BALL);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertAfter(Items.NETHERITE_AXE, VIBRANIUM_AXE));
	}

	private VibraniumItems() {
	}
}
