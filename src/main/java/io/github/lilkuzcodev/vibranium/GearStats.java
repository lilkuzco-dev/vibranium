package io.github.lilkuzcodev.vibranium;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Reads a gear item's real, registered numbers back off its components — the same values the
 * player sees in the tooltip, not the constants they were built from. Both self-test commands
 * assert through this, so a stat that is right in {@code VibraniumCombat} but wired up wrong at
 * registration still fails the test.
 */
final class GearStats {
	/** The item's total ADD_VALUE for one attribute in one slot, straight off its component. */
	static double attribute(Item item, EquipmentSlot slot, Holder<Attribute> attribute) {
		ItemAttributeModifiers modifiers = new ItemStack(item).get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (modifiers == null) {
			return 0.0;
		}
		double[] total = {0.0};
		modifiers.forEach(slot, (held, modifier) -> {
			if (held.equals(attribute)) {
				total[0] += modifier.amount();
			}
		});
		return total[0];
	}

	/** Attack damage as the tooltip shows it: the player's own 1.0 plus the weapon's modifiers. */
	static double displayedAttackDamage(Item item) {
		return 1.0 + attribute(item, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
	}

	/** Attack speed as the tooltip shows it: the base 4.0 plus the weapon's (negative) modifier. */
	static double displayedAttackSpeed(Item item) {
		return 4.0 + attribute(item, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED);
	}

	static String key(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private GearStats() {
	}
}
