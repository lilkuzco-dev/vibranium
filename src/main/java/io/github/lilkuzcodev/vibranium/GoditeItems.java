package io.github.lilkuzcodev.vibranium;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Godite's items: the metal, the full weapon and tool set, and the armour. Every one of
 * them is the vibranium item with {@link GoditeCombat}'s numbers substituted in, running
 * the same shared classes — {@link KineticSwordItem}, {@link KineticArmorItem} and friends.
 *
 * <p>No godite energy ball: the kinetic energy ball is a thrown explosive rather than a
 * weapon, tool or piece of armour, so it stays vibranium-only.
 */
public final class GoditeItems {
	// Metal identity, mirroring vibranium exactly: the ore drops raw_godite, which smelts
	// (or blasts) into godite_ingot, and everything crafts from ingots.
	public static final Item RAW_GODITE = Registration.item("raw_godite", Item::new, new Item.Properties());
	public static final Item GODITE_INGOT = Registration.item("godite_ingot", Item::new, new Item.Properties());

	// Weapons: crafted directly (no smithing), fire-resistant as dropped items like netherite.
	public static final Item GODITE_SWORD = Registration.item("godite_sword",
			properties -> new KineticSwordItem(properties, GoditeCombat.GODITE_STRIKE),
			new Item.Properties()
					.sword(GoditeCombat.GODITE_MATERIAL, GoditeCombat.SWORD_BASE_DAMAGE, GoditeCombat.SWORD_ATTACK_SPEED)
					.fireResistant());
	public static final Item GODITE_AXE = Registration.item("godite_axe",
			properties -> new KineticAxeItem(GoditeCombat.GODITE_MATERIAL, GoditeCombat.AXE_BASE_DAMAGE,
					GoditeCombat.AXE_ATTACK_SPEED, properties, GoditeCombat.GODITE_STRIKE),
			new Item.Properties().fireResistant());

	// Tools on the same material (no kinetic cycle on tools, as with vibranium).
	public static final Item GODITE_PICKAXE = Registration.item("godite_pickaxe", Item::new,
			new Item.Properties()
					.pickaxe(GoditeCombat.GODITE_MATERIAL, GoditeCombat.PICKAXE_BASE_DAMAGE, GoditeCombat.PICKAXE_ATTACK_SPEED)
					.fireResistant());
	public static final Item GODITE_SHOVEL = Registration.item("godite_shovel",
			properties -> new ShovelItem(GoditeCombat.GODITE_MATERIAL, GoditeCombat.SHOVEL_BASE_DAMAGE,
					GoditeCombat.SHOVEL_ATTACK_SPEED, properties),
			new Item.Properties().fireResistant());
	public static final Item GODITE_HOE = Registration.item("godite_hoe",
			properties -> new HoeItem(GoditeCombat.GODITE_MATERIAL, GoditeCombat.HOE_BASE_DAMAGE, 0.0F, properties),
			new Item.Properties().fireResistant());

	// Armour: runs the godite ward — same cycle as vibranium's, 5th hit instead of 7th,
	// and the launch instead of the nudge.
	public static final Item GODITE_HELMET = armor("godite_helmet", ArmorType.HELMET);
	public static final Item GODITE_CHESTPLATE = armor("godite_chestplate", ArmorType.CHESTPLATE);
	public static final Item GODITE_LEGGINGS = armor("godite_leggings", ArmorType.LEGGINGS);
	public static final Item GODITE_BOOTS = armor("godite_boots", ArmorType.BOOTS);

	// Extended-reach spear: vanilla 26.2 spear properties with godite's damage swapped in.
	// The timing tuple and the reach are vibranium's — only strength scales (GoditeCombat).
	public static final Item GODITE_SPEAR = Registration.item("godite_spear",
			properties -> new KineticSpearItem(properties, GoditeCombat.GODITE_STRIKE),
			new Item.Properties()
					.spear(GoditeCombat.GODITE_MATERIAL, GoditeCombat.SPEAR_ATTACK_DURATION,
							1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F)
					.component(DataComponents.ATTACK_RANGE,
							new AttackRange(2.0F, GoditeCombat.SPEAR_REACH, 2.0F, GoditeCombat.SPEAR_CREATIVE_REACH, 0.125F, 0.5F))
					.attributes(ItemAttributeModifiers.builder()
							.add(Attributes.ATTACK_DAMAGE,
									new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
											GoditeCombat.SPEAR_BASE_DAMAGE + GoditeCombat.DAMAGE_BONUS,
											AttributeModifier.Operation.ADD_VALUE),
									EquipmentSlotGroup.MAINHAND)
							.add(Attributes.ATTACK_SPEED,
									new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
											1.0F / GoditeCombat.SPEAR_ATTACK_DURATION - 4.0,
											AttributeModifier.Operation.ADD_VALUE),
									EquipmentSlotGroup.MAINHAND)
							.build())
					.fireResistant());

	private static Item armor(String name, ArmorType type) {
		return Registration.item(name,
				properties -> new KineticArmorItem(properties, GoditeCombat.GODITE_WARD),
				new Item.Properties().humanoidArmor(GoditeCombat.GODITE_ARMOR_MATERIAL, type).fireResistant());
	}

	public static void init() {
		// Every godite entry sits directly after its vibranium counterpart, so the creative
		// menu reads as the progression it is: netherite -> vibranium -> godite.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
			output.insertAfter(VibraniumBlocks.DEEPSLATE_VIBRANIUM_ORE, GoditeBlocks.GODITE_ORE, GoditeBlocks.DEEPSLATE_GODITE_ORE);
			output.insertAfter(VibraniumBlocks.RAW_VIBRANIUM_BLOCK, GoditeBlocks.RAW_GODITE_BLOCK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				output.insertAfter(VibraniumBlocks.BLOCK_OF_VIBRANIUM, GoditeBlocks.BLOCK_OF_GODITE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(VibraniumItems.RAW_VIBRANIUM, RAW_GODITE);
			output.insertAfter(VibraniumItems.VIBRANIUM_INGOT, GODITE_INGOT);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(VibraniumItems.VIBRANIUM_SWORD, GODITE_SWORD);
			output.insertAfter(VibraniumItems.VIBRANIUM_SPEAR, GODITE_SPEAR);
			output.insertAfter(VibraniumItems.VIBRANIUM_HELMET, GODITE_HELMET);
			output.insertAfter(VibraniumItems.VIBRANIUM_CHESTPLATE, GODITE_CHESTPLATE);
			output.insertAfter(VibraniumItems.VIBRANIUM_LEGGINGS, GODITE_LEGGINGS);
			output.insertAfter(VibraniumItems.VIBRANIUM_BOOTS, GODITE_BOOTS);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.insertAfter(VibraniumItems.VIBRANIUM_AXE, GODITE_AXE);
			output.insertAfter(VibraniumItems.VIBRANIUM_PICKAXE, GODITE_PICKAXE);
			output.insertAfter(VibraniumItems.VIBRANIUM_SHOVEL, GODITE_SHOVEL);
			output.insertAfter(VibraniumItems.VIBRANIUM_HOE, GODITE_HOE);
		});
	}

	private GoditeItems() {
	}
}
