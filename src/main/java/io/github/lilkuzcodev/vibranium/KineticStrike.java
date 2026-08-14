package io.github.lilkuzcodev.vibranium;

import io.github.lilkuzcodev.vibranium.VibraniumComponents.KineticHits;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The kinetic STRIKE CYCLE shared by all vibranium weapons (replaces the old
 * absorb-damage/discharge mechanic). Every real melee hit on a living entity adds one
 * charge (cap {@link VibraniumCombat#HITS_TO_PRIME}); the next hit after priming lands
 * as a kinetic strike — an AoE burst centered on the STRUCK TARGET — then the cycle
 * restarts. Charges decay to zero out of combat. All numbers live in VibraniumCombat.
 *
 * <p>Charge counting rides on {@code Item.hurtEnemy}, which vanilla calls exactly once
 * per swing for the primary target only — sweep-attack secondary hits never add charges.
 */
public final class KineticStrike {
	/** Called from the weapons' {@code hurtEnemy}: counts the hit or detonates. */
	public static void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker.level() instanceof ServerLevel level) || target instanceof ArmorStand) {
			return;
		}
		long now = level.getGameTime();
		KineticHits state = stack.getOrDefault(VibraniumComponents.KINETIC_HITS, new KineticHits(0, 0));
		int hits = now - state.lastHitTime() > VibraniumCombat.DECAY_WINDOW_TICKS ? 0 : state.hits();
		if (hits >= VibraniumCombat.HITS_TO_PRIME) {
			strike(level, target, attacker);
			stack.remove(VibraniumComponents.KINETIC_HITS);
		} else {
			stack.set(VibraniumComponents.KINETIC_HITS, new KineticHits(hits + 1, now));
		}
	}

	/** AoE burst centered on the struck target: bonus damage + launch, no blocks, no wielder. */
	private static void strike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		double radius = VibraniumCombat.STRIKE_RADIUS;
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				target.getBoundingBox().inflate(radius, Math.max(1.5, radius * 0.75), radius),
				v -> v != attacker && v.isAlive() && !v.isSpectator() && !(v instanceof ArmorStand))) {
			double distance = victim == target ? 0.0 : victim.distanceTo(target);
			if (distance > radius) {
				continue;
			}
			float falloff = 1.0F - (float) (distance / radius) * VibraniumCombat.STRIKE_FALLOFF;

			// the struck target is i-framed from the triggering weapon hit — the bonus must land
			victim.invulnerableTime = 0;
			var source = attacker instanceof Player player
					? level.damageSources().playerAttack(player)
					: level.damageSources().mobAttack(attacker);
			victim.hurt(source, VibraniumCombat.STRIKE_BONUS_DAMAGE * falloff);

			Vec3 away = victim == target
					? target.position().subtract(attacker.position())
					: victim.position().subtract(target.position());
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
			victim.knockback(VibraniumCombat.STRIKE_KNOCKBACK * falloff, -dir.x, -dir.z, source, 0.0F);
			double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			victim.push(0.0, VibraniumCombat.STRIKE_VERTICAL * falloff * Math.max(0.0, 1.0 - resistance), 0.0);
			victim.hurtMarked = true;
		}

		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.5;
		double z = target.getZ();
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.WITCH, x, y, z, 40, 1.2, 0.8, 1.2, 0.2);
		level.sendParticles(ParticleTypes.CRIT, x, y, z, 25, 1.5, 0.6, 1.5, 0.3);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.75F);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 0.5F);
	}

	/** Tooltip: "Kinetic Charge: 4/6", plus an unmistakable primed line. */
	public static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		int hits = state == null ? 0 : state.hits();
		tooltip.accept(Component.translatable("item.vibranium.kinetic_charge", hits, VibraniumCombat.HITS_TO_PRIME)
				.withStyle(hits >= VibraniumCombat.HITS_TO_PRIME ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
		if (hits >= VibraniumCombat.HITS_TO_PRIME) {
			tooltip.accept(Component.translatable("item.vibranium.kinetic_primed").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		}
	}

	/** Held tick: out-of-combat decay + the escalating buildup cue. */
	public static void tickHeld(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND) {
			return;
		}
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		if (state == null || state.hits() <= 0) {
			return;
		}
		long now = level.getGameTime();
		if (now - state.lastHitTime() > VibraniumCombat.DECAY_WINDOW_TICKS) {
			stack.remove(VibraniumComponents.KINETIC_HITS); // cycle broken: reset to 0
			return;
		}
		boolean primed = state.hits() >= VibraniumCombat.HITS_TO_PRIME;
		if (state.hits() < VibraniumCombat.PARTICLE_THRESHOLD) {
			return;
		}
		long interval = primed ? 5L : 12L;
		if (now % interval != 0L) {
			return;
		}
		int count = primed ? 6 : state.hits() - VibraniumCombat.PARTICLE_THRESHOLD + 1;
		level.sendParticles(ParticleTypes.WITCH,
				owner.getX(), owner.getY() + 1.1, owner.getZ(), count, 0.25, 0.4, 0.25, 0.02);
		if (primed) {
			level.sendParticles(ParticleTypes.END_ROD,
					owner.getX(), owner.getY() + 1.2, owner.getZ(), 2, 0.15, 0.3, 0.15, 0.01);
		}
	}

	private KineticStrike() {
	}
}
