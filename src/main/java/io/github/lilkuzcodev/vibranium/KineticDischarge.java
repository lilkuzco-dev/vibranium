package io.github.lilkuzcodev.vibranium;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The kinetic absorb-and-release perk shared by all vibranium weapons.
 * All tuning numbers live in {@link VibraniumCombat}.
 *
 * <p>Charging: while a player HOLDS a vibranium weapon in the main hand, melee damage
 * dealt to them by mobs/players (post-armor, not blocked) charges the weapon. Environmental
 * damage (fall, fire, cactus...) and self-inflicted damage never charge it.
 *
 * <p>Discharge: sneak + right-click releases a radial burst — damage and knockback scale
 * with stored charge and fall off with distance. No block damage, no self damage.
 */
public final class KineticDischarge {
	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(KineticDischarge::afterDamage);
	}

	public static float getCharge(ItemStack stack) {
		return stack.getOrDefault(VibraniumComponents.KINETIC_CHARGE, 0.0F);
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F) {
			return;
		}
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		// Balance guard: only genuine melee hits from another living attacker charge the
		// meter. Environmental damage has no attacker; projectiles have a non-living
		// direct entity; self-damage is excluded explicitly.
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player) {
			return;
		}
		if (!(source.getDirectEntity() instanceof LivingEntity)) {
			return;
		}
		ItemStack held = player.getMainHandItem(); // only the held weapon charges
		if (!(held.getItem() instanceof KineticWeapon)) {
			return;
		}
		// The event reports PRE-armor damage; the spec charges on POST-armor damage,
		// so apply vanilla's own armor absorption formula.
		float postArmor = CombatRules.getDamageAfterAbsorb(player, damageTaken, source,
				player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		if (postArmor <= 0.0F) {
			return;
		}
		float updated = Math.min(VibraniumCombat.CHARGE_CAP, getCharge(held) + postArmor * VibraniumCombat.ABSORB_RATIO);
		held.set(VibraniumComponents.KINETIC_CHARGE, updated);
	}

	/** Shared {@code Item.use} behavior for vibranium weapons: sneak + right-click discharges. */
	public static InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (getCharge(stack) <= 0.0F || player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			discharge(serverLevel, serverPlayer, stack);
		}
		return InteractionResult.SUCCESS;
	}

	private static void discharge(ServerLevel level, ServerPlayer player, ItemStack stack) {
		float fraction = getCharge(stack) / VibraniumCombat.CHARGE_CAP;
		double radius = VibraniumCombat.BURST_RADIUS;

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius, Math.max(1.5, radius * 0.65), radius),
				t -> t != player && t.isAlive() && !t.isSpectator())) {
			double distance = target.distanceTo(player);
			if (distance > radius) {
				continue;
			}
			float falloff = 1.0F - (float) (distance / radius) * VibraniumCombat.EDGE_FALLOFF;
			float strengthScale = fraction * falloff;

			// The burst must land even if the target is in melee i-frames.
			target.invulnerableTime = 0;
			DamageSource burstSource = level.damageSources().playerAttack(player);
			target.hurt(burstSource, VibraniumCombat.BURST_DAMAGE * strengthScale);

			Vec3 away = target.position().subtract(player.position());
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
			// horizontal via vanilla knockback (applies knockback resistance),
			// vertical launch added separately, then flagged for client velocity sync
			target.knockback(VibraniumCombat.KNOCKBACK_STRENGTH * strengthScale, -dir.x, -dir.z, burstSource, 0.0F);
			double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			target.push(0.0, VibraniumCombat.KNOCKBACK_VERTICAL * strengthScale * Math.max(0.0, 1.0 - resistance), 0.0);
			target.hurtMarked = true;
		}

		// burst FX: core blast + purple energy cloud + two expanding rings
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.WITCH, x, y, z, Math.round(20 + 26 * fraction), 0.65, 0.35, 0.65, 0.15);
		for (double ringRadius = radius * 0.5; ringRadius <= radius; ringRadius += radius * 0.5) {
			int points = (int) Math.ceil(ringRadius * 10);
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
						x + Math.cos(angle) * ringRadius, player.getY() + 0.15, z + Math.sin(angle) * ringRadius,
						1, 0.02, 0.02, 0.02, 0.0);
			}
		}
		// layered boom, all vanilla sounds
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.55F);

		stack.remove(VibraniumComponents.KINETIC_CHARGE);
		player.getCooldowns().addCooldown(stack, VibraniumCombat.DISCHARGE_COOLDOWN_TICKS);
	}

	/** Tooltip line: current / max charge in hearts. */
	public static void appendChargeTooltip(ItemStack stack, Consumer<Component> tooltip) {
		float charge = getCharge(stack);
		tooltip.accept(Component.translatable("item.vibranium.kinetic_charge",
						String.format("%.1f", charge / 2.0F), String.format("%.0f", VibraniumCombat.CHARGE_CAP / 2.0F))
				.withStyle(charge >= VibraniumCombat.CHARGE_CAP ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
	}

	/** Full-charge cue: a soft witch-particle shimmer around the holder while in main hand. */
	public static void fullChargeParticles(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND || getCharge(stack) < VibraniumCombat.CHARGE_CAP) {
			return;
		}
		if (level.getGameTime() % 15L != 0L) {
			return;
		}
		level.sendParticles(ParticleTypes.WITCH, owner.getX(), owner.getY() + 1.2, owner.getZ(), 4, 0.3, 0.45, 0.3, 0.0);
	}

	private KineticDischarge() {
	}
}
