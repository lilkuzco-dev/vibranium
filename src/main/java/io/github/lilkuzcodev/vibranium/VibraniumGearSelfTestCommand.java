package io.github.lilkuzcodev.vibranium;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;

/**
 * {@code /vibranium_gear_selftest} — headless proof of the armor and of the
 * "enchantable exactly like diamond" property, for dev-server batteries. An
 * enchanting table cannot be driven over RCON, and enchantability is decided by
 * two things that both fail SILENTLY when wrong: the {@code minecraft:enchantable}
 * component value, and membership in the vanilla {@code #minecraft:enchantable/*}
 * item tags (a custom sword absent from {@code #minecraft:swords} simply offers no
 * enchantments — no error, no log line). So both are asserted here, and asserted by
 * COMPARISON against the diamond counterpart rather than against a hardcoded list.
 *
 * <p>Asserts:
 * <ol>
 * <li>every vibranium gear item's enchantability value equals its diamond counterpart's</li>
 * <li>every vibranium gear item sits in exactly the same {@code enchantable/*} tags
 *     as its diamond counterpart</li>
 * <li>the full armor set beats netherite's on armor points, toughness and knockback
 *     resistance, computed from the items' real attribute components</li>
 * <li>the four armor pieces run the kinetic ward, equip to the right slots, carry the
 *     vibranium equipment asset, and repair with the vibranium ingot</li>
 * </ol>
 */
public final class VibraniumGearSelfTestCommand {
	/** Every {@code #minecraft:enchantable/*} tag, so the comparison covers all of them. */
	private static final List<TagKey<Item>> ENCHANTABLE_TAGS = List.of(
			ItemTags.ARMOR_ENCHANTABLE, ItemTags.FOOT_ARMOR_ENCHANTABLE, ItemTags.LEG_ARMOR_ENCHANTABLE,
			ItemTags.CHEST_ARMOR_ENCHANTABLE, ItemTags.HEAD_ARMOR_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE,
			ItemTags.WEAPON_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.MELEE_WEAPON_ENCHANTABLE,
			ItemTags.SWEEPING_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.LUNGE_ENCHANTABLE,
			ItemTags.MINING_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE,
			ItemTags.DURABILITY_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE,
			ItemTags.BOW_ENCHANTABLE, ItemTags.CROSSBOW_ENCHANTABLE, ItemTags.TRIDENT_ENCHANTABLE,
			ItemTags.FISHING_ENCHANTABLE, ItemTags.MACE_ENCHANTABLE);

	/** Every vibranium gear item paired with the diamond item it must match. */
	private static final List<Item[]> GEAR_PAIRS = List.of(
			new Item[]{VibraniumItems.VIBRANIUM_SWORD, Items.DIAMOND_SWORD},
			new Item[]{VibraniumItems.VIBRANIUM_AXE, Items.DIAMOND_AXE},
			new Item[]{VibraniumItems.VIBRANIUM_PICKAXE, Items.DIAMOND_PICKAXE},
			new Item[]{VibraniumItems.VIBRANIUM_SHOVEL, Items.DIAMOND_SHOVEL},
			new Item[]{VibraniumItems.VIBRANIUM_HOE, Items.DIAMOND_HOE},
			new Item[]{VibraniumItems.VIBRANIUM_SPEAR, Items.DIAMOND_SPEAR},
			new Item[]{VibraniumItems.VIBRANIUM_HELMET, Items.DIAMOND_HELMET},
			new Item[]{VibraniumItems.VIBRANIUM_CHESTPLATE, Items.DIAMOND_CHESTPLATE},
			new Item[]{VibraniumItems.VIBRANIUM_LEGGINGS, Items.DIAMOND_LEGGINGS},
			new Item[]{VibraniumItems.VIBRANIUM_BOOTS, Items.DIAMOND_BOOTS});

	/** Armor pieces paired with the slot they must equip to and the netherite piece to beat. */
	private static final List<Object[]> ARMOR_PIECES = List.of(
			new Object[]{VibraniumItems.VIBRANIUM_HELMET, EquipmentSlot.HEAD, Items.NETHERITE_HELMET},
			new Object[]{VibraniumItems.VIBRANIUM_CHESTPLATE, EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE},
			new Object[]{VibraniumItems.VIBRANIUM_LEGGINGS, EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS},
			new Object[]{VibraniumItems.VIBRANIUM_BOOTS, EquipmentSlot.FEET, Items.NETHERITE_BOOTS});

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
				dispatcher.register(Commands.literal("vibranium_gear_selftest")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> run(ctx.getSource()))));
	}

	private static int run(final CommandSourceStack source) {
		List<String> failures = new ArrayList<>();
		List<String> notes = new ArrayList<>();

		// --- 1 + 2: enchantability parity with diamond, value AND tag membership ---
		for (Item[] pair : GEAR_PAIRS) {
			ItemStack mine = new ItemStack(pair[0]);
			ItemStack diamond = new ItemStack(pair[1]);
			String name = key(pair[0]);

			Enchantable mineValue = mine.get(DataComponents.ENCHANTABLE);
			Enchantable diamondValue = diamond.get(DataComponents.ENCHANTABLE);
			if (mineValue == null) {
				failures.add(name + ": no enchantable component at all");
			} else if (diamondValue == null || mineValue.value() != diamondValue.value()) {
				failures.add(name + ": enchantability " + mineValue.value()
						+ ", diamond is " + (diamondValue == null ? "none" : diamondValue.value()));
			}

			for (TagKey<Item> tag : ENCHANTABLE_TAGS) {
				if (mine.is(tag) != diamond.is(tag)) {
					failures.add(name + ": " + tag.location() + " membership is " + mine.is(tag)
							+ ", diamond is " + diamond.is(tag));
				}
			}
		}
		notes.add("enchantability + enchantable/* tags: " + GEAR_PAIRS.size()
				+ " items compared against their diamond counterparts");

		// --- 3: the full set out-protects netherite, read from the real components ---
		double armor = 0.0;
		double toughness = 0.0;
		double knockback = 0.0;
		double netheriteArmor = 0.0;
		double netheriteToughness = 0.0;
		double netheriteKnockback = 0.0;
		for (Object[] piece : ARMOR_PIECES) {
			EquipmentSlot slot = (EquipmentSlot) piece[1];
			armor += attribute((Item) piece[0], slot, Attributes.ARMOR);
			toughness += attribute((Item) piece[0], slot, Attributes.ARMOR_TOUGHNESS);
			knockback += attribute((Item) piece[0], slot, Attributes.KNOCKBACK_RESISTANCE);
			netheriteArmor += attribute((Item) piece[2], slot, Attributes.ARMOR);
			netheriteToughness += attribute((Item) piece[2], slot, Attributes.ARMOR_TOUGHNESS);
			netheriteKnockback += attribute((Item) piece[2], slot, Attributes.KNOCKBACK_RESISTANCE);
		}
		if (armor <= netheriteArmor) {
			failures.add("armor points " + armor + " do not beat netherite's " + netheriteArmor);
		}
		if (toughness <= netheriteToughness) {
			failures.add("toughness " + toughness + " does not beat netherite's " + netheriteToughness);
		}
		if (knockback <= netheriteKnockback) {
			failures.add("knockback resistance " + knockback + " does not beat netherite's " + netheriteKnockback);
		}
		notes.add(String.format(Locale.ROOT, "full set: %.0f armor / %.1f toughness / %.2f knockback"
						+ "  (netherite: %.0f / %.1f / %.2f)",
				armor, toughness, knockback, netheriteArmor, netheriteToughness, netheriteKnockback));

		// --- 4: ward wiring, slots, equipment asset, repair material ---
		for (Object[] piece : ARMOR_PIECES) {
			Item item = (Item) piece[0];
			EquipmentSlot expected = (EquipmentSlot) piece[1];
			ItemStack stack = new ItemStack(item);
			String name = key(item);

			if (!(item instanceof KineticWardArmor)) {
				failures.add(name + ": does not implement KineticWardArmor — the ward will never see it");
			}
			var equippable = stack.get(DataComponents.EQUIPPABLE);
			if (equippable == null || equippable.slot() != expected) {
				failures.add(name + ": equips to " + (equippable == null ? "nothing" : equippable.slot())
						+ ", expected " + expected);
			} else if (equippable.assetId().isEmpty()
					|| !equippable.assetId().get().equals(VibraniumCombat.VIBRANIUM_ARMOR_ASSET)) {
				failures.add(name + ": equipment asset is " + equippable.assetId()
						+ ", expected " + VibraniumCombat.VIBRANIUM_ARMOR_ASSET);
			}
			var repairable = stack.get(DataComponents.REPAIRABLE);
			if (repairable == null || !repairable.isValidRepairItem(new ItemStack(VibraniumItems.VIBRANIUM_INGOT))) {
				failures.add(name + ": does not anvil-repair with a vibranium ingot");
			}
			if (stack.getMaxDamage() <= new ItemStack((Item) piece[2]).getMaxDamage()) {
				failures.add(name + ": durability " + stack.getMaxDamage()
						+ " does not beat netherite's " + new ItemStack((Item) piece[2]).getMaxDamage());
			}
		}
		notes.add("ward wiring: 4 pieces implement KineticWardArmor, burst on hit "
				+ (VibraniumCombat.WARD_HITS_TO_PRIME + 1)
				+ " within " + VibraniumCombat.WARD_DECAY_WINDOW_TICKS + " ticks of the last");

		for (String note : notes) {
			source.sendSuccess(() -> Component.literal("  " + note), false);
		}
		if (failures.isEmpty()) {
			source.sendSuccess(() -> Component.literal("vibranium gear self-test: PASS"), true);
			return 1;
		}
		for (String failure : failures) {
			source.sendFailure(Component.literal("  FAIL " + failure));
		}
		source.sendFailure(Component.literal("vibranium gear self-test: " + failures.size() + " FAILURE(S)"));
		return 0;
	}

	/** Both self-tests read registered stats the same way — see {@link GearStats}. */
	private static double attribute(Item item, EquipmentSlot slot, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
		return GearStats.attribute(item, slot, attribute);
	}

	private static String key(Item item) {
		return GearStats.key(item);
	}

	private VibraniumGearSelfTestCommand() {
	}
}
