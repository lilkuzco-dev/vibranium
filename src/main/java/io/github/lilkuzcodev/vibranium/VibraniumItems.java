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

	// Tools on the same tier-above-netherite material (no kinetic cycle on tools).
	public static final Item VIBRANIUM_PICKAXE = register("vibranium_pickaxe",
			new Item.Properties().pickaxe(VibraniumCombat.VIBRANIUM_MATERIAL, 1.0F, -2.8F).fireResistant());
	public static final Item VIBRANIUM_SHOVEL = register("vibranium_shovel",
			properties -> new net.minecraft.world.item.ShovelItem(VibraniumCombat.VIBRANIUM_MATERIAL, 1.5F, -3.0F, properties),
			new Item.Properties().fireResistant());
	public static final Item VIBRANIUM_HOE = register("vibranium_hoe",
			properties -> new net.minecraft.world.item.HoeItem(VibraniumCombat.VIBRANIUM_MATERIAL, VibraniumCombat.HOE_BASE_DAMAGE, 0.0F, properties),
			new Item.Properties().fireResistant());

	// Extended-reach spear: vanilla 26.2 spear properties (kinetic-charge component,
	// piercing, STAB swing; timing params mirror the netherite spear), with our
	// attack range and damage attributes swapped in from the tuning block.
	public static final Item VIBRANIUM_SPEAR = register("vibranium_spear", VibraniumSpearItem::new,
			new Item.Properties()
					.spear(VibraniumCombat.VIBRANIUM_MATERIAL, VibraniumCombat.SPEAR_ATTACK_DURATION,
							1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F)
					.component(net.minecraft.core.component.DataComponents.ATTACK_RANGE,
							new net.minecraft.world.item.component.AttackRange(2.0F, VibraniumCombat.SPEAR_REACH,
									2.0F, VibraniumCombat.SPEAR_CHARGED_REACH, 0.125F, 0.5F))
					.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
							.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
									new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
											VibraniumCombat.SPEAR_BASE_DAMAGE + VibraniumCombat.DAMAGE_BONUS,
											net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
									net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
							.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
									new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
											1.0F / VibraniumCombat.SPEAR_ATTACK_DURATION - 4.0,
											net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
									net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
							.build())
					.fireResistant());

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
			output.insertAfter(Items.AMETHYST_CLUSTER, VibraniumBlocks.VIBRANIUM_CRYSTAL_CLUSTER);
			output.insertAfter(Items.DEEPSLATE, VibraniumBlocks.VIBRANIUM_VEINSTONE);
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
			output.insertAfter(Items.NETHERITE_SPEAR, VIBRANIUM_SPEAR);
			output.accept(VIBRANIUM_ENERGY_BALL);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.insertAfter(Items.NETHERITE_AXE, VIBRANIUM_AXE);
			output.insertAfter(Items.NETHERITE_PICKAXE, VIBRANIUM_PICKAXE);
			output.insertAfter(Items.NETHERITE_SHOVEL, VIBRANIUM_SHOVEL);
			output.insertAfter(Items.NETHERITE_HOE, VIBRANIUM_HOE);
		});
	}

	private VibraniumItems() {
	}
}
