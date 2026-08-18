package io.github.lilkuzcodev.vibranium;

import io.github.lilkuzcodev.vibranium.VibraniumComponents.KineticHits;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The kinetic STRIKE CYCLE shared by every weapon in this mod. Each real melee hit on a
 * living entity adds one charge (cap {@link KineticProfile#hitsToPrime()}); the next hit
 * after priming lands as a kinetic strike — an AoE burst centred on the STRUCK TARGET —
 * then the cycle restarts. Charges decay to zero out of combat.
 *
 * <p>The numbers are NOT here: each weapon names a {@link KineticProfile}, so vibranium
 * detonates on its 7th hit with a 1.8-strength launch and godite on its 5th with an
 * 11.0-strength one, running this exact code. See {@code VibraniumCombat.VIBRANIUM_STRIKE}
 * and {@code GoditeCombat.GODITE_STRIKE}.
 *
 * <p>Charge counting rides on {@code Item.hurtEnemy}, which vanilla calls exactly once
 * per swing for the primary target only — sweep-attack secondary hits never add charges.
 */
public final class KineticStrike {
	/** Called from the weapons' {@code hurtEnemy}: counts the hit or detonates. */
	public static void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		KineticProfile profile = profileOf(stack);
		if (profile == null || !(attacker.level() instanceof ServerLevel level) || target instanceof ArmorStand) {
			return;
		}
		long now = level.getGameTime();
		KineticHits state = stack.getOrDefault(VibraniumComponents.KINETIC_HITS, new KineticHits(0, 0));
		int hits = now - state.lastHitTime() > profile.decayWindowTicks() ? 0 : state.hits();
		if (hits >= profile.hitsToPrime()) {
			strike(level, target, attacker, profile);
			stack.remove(VibraniumComponents.KINETIC_HITS);
		} else {
			stack.set(VibraniumComponents.KINETIC_HITS, new KineticHits(hits + 1, now));
		}
	}

	/** AoE burst centred on the struck target: bonus damage + launch, no blocks, no wielder. */
	private static void strike(ServerLevel level, LivingEntity target, LivingEntity attacker, KineticProfile profile) {
		double radius = profile.radius();
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				target.getBoundingBox().inflate(radius, Math.max(1.5, radius * 0.75), radius),
				v -> v != attacker && v.isAlive() && !v.isSpectator() && !(v instanceof ArmorStand))) {
			double distance = victim == target ? 0.0 : victim.distanceTo(target);
			if (distance > radius) {
				continue;
			}
			float falloff = 1.0F - (float) (distance / radius) * profile.falloff();

			// the struck target is i-framed from the triggering weapon hit — the bonus must land
			victim.invulnerableTime = 0;
			var source = attacker instanceof Player player
					? level.damageSources().playerAttack(player)
					: level.damageSources().mobAttack(attacker);
			victim.hurtServer(level, source, profile.burstDamage() * falloff);

			Vec3 away = victim == target
					? target.position().subtract(attacker.position())
					: victim.position().subtract(target.position());
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
			// deferred to the end of the tick — vanilla's own post-hit knockback would
			// otherwise halve it; see KineticLaunch
			KineticLaunch.queue(victim, profile.knockback() * falloff, profile.vertical() * falloff, dir, source);
		}

		burstEffects(level, profile, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
	}

	/** The bang. The ward's is deliberately its own, bigger and ringed — see {@link KineticWard}. */
	private static void burstEffects(ServerLevel level, KineticProfile profile, double x, double y, double z) {
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		if (profile.rainbow()) {
			// a full turn of the wheel around the burst, one colour per spoke
			for (int i = 0; i < 24; i++) {
				double angle = i * Math.PI * 2.0 / 24.0;
				DustParticleOptions dust = new DustParticleOptions(Rainbow.rgb((double) i / 24.0), 1.8F);
				level.sendParticles(dust, x + Math.cos(angle) * 1.2, y, z + Math.sin(angle) * 1.2, 8, 0.35, 0.5, 0.35, 0.06);
			}
			level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.8, 0.5, 0.8, 0.4);
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.55F);
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 1.6F);
		} else {
			level.sendParticles(ParticleTypes.WITCH, x, y, z, 40, 1.2, 0.8, 1.2, 0.2);
			level.sendParticles(ParticleTypes.CRIT, x, y, z, 25, 1.5, 0.6, 1.5, 0.3);
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.75F);
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 0.5F);
		}
	}

	/** Tooltip: "Kinetic Charge: 4/6", plus an unmistakable primed line. */
	public static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
		KineticProfile profile = profileOf(stack);
		if (profile == null) {
			return;
		}
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		int hits = state == null ? 0 : state.hits();
		KineticFx.appendTooltip(tooltip, profile, hits);
	}

	/** Held tick: out-of-combat decay + the escalating buildup cue. */
	public static void tickHeld(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		KineticProfile profile = profileOf(stack);
		if (profile == null || slot != EquipmentSlot.MAINHAND) {
			return;
		}
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		if (state == null || state.hits() <= 0) {
			return;
		}
		long now = level.getGameTime();
		if (now - state.lastHitTime() > profile.decayWindowTicks()) {
			stack.remove(VibraniumComponents.KINETIC_HITS); // cycle broken: reset to 0
			return;
		}
		boolean primed = state.hits() >= profile.hitsToPrime();
		if (state.hits() < profile.particleThreshold()) {
			return;
		}
		long interval = primed ? 5L : 12L;
		if (now % interval != 0L) {
			return;
		}
		int count = primed ? 6 : state.hits() - profile.particleThreshold() + 1;
		KineticFx.cueBurst(level, profile, owner.getX(), owner.getY() + 1.1, owner.getZ(), count, 0.25, 0.4, 0.25, 0.02);
		if (primed) {
			level.sendParticles(ParticleTypes.END_ROD,
					owner.getX(), owner.getY() + 1.2, owner.getZ(), 2, 0.15, 0.3, 0.15, 0.01);
		}
	}

	/** The profile the stack's item runs, or null if it runs no strike cycle at all. */
	private static KineticProfile profileOf(ItemStack stack) {
		return stack.getItem() instanceof KineticCycleWeapon weapon ? weapon.kineticProfile() : null;
	}

	private KineticStrike() {
	}
}
