package io.github.lilkuzcodev.vibranium;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Every armour piece that runs a kinetic ward cycle, in both metals. Stats, durability,
 * equip sound, repair material and enchantability all come from
 * {@code Properties.humanoidArmor(...)} at registration; this class only carries the
 * ward's profile, tooltip and decay tick.
 *
 * <p>The burst itself is NOT triggered here — 26.2 gives an item no "was worn when
 * hit" hook, so {@link KineticWard} listens on the damage event instead.
 */
public class KineticArmorItem extends Item implements KineticWardArmor {
	private final KineticProfile profile;

	public KineticArmorItem(Properties properties, KineticProfile profile) {
		super(properties);
		this.profile = profile;
	}

	@Override
	public KineticProfile kineticProfile() {
		return profile;
	}

	@Override
	@SuppressWarnings("deprecation") // 26.2 still exposes no non-deprecated custom-item tooltip hook
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		KineticWard.appendTooltip(stack, tooltip);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(stack, level, owner, slot);
		KineticWard.tickWorn(stack, level, owner, slot);
	}
}
