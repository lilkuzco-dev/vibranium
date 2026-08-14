package io.github.lilkuzcodev.vibranium;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Thrown kinetic energy ball: ender-pearl flight (no teleport), massive purple
 * explosion on impact. Power and terrain behavior are tuned in {@link VibraniumCombat}.
 */
public class KineticEnergyBallEntity extends ThrowableItemProjectile {
	public KineticEnergyBallEntity(EntityType<? extends KineticEnergyBallEntity> type, Level level) {
		super(type, level);
	}

	public KineticEnergyBallEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
		super(VibraniumEntities.KINETIC_ENERGY_BALL, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return VibraniumItems.VIBRANIUM_ENERGY_BALL;
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level() instanceof ServerLevel level) {
			double x = this.getX();
			double y = this.getY();
			double z = this.getZ();
			// The thrower is deliberately NOT immune: vanilla explosions damage their source.
			level.explode(this, x, y, z, VibraniumCombat.ENERGY_BALL_EXPLOSION_POWER, false,
					VibraniumCombat.ENERGY_BALL_BREAKS_TERRAIN ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
			// purple energy signature on top of the vanilla explosion FX
			level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), x, y, z, 300, 3.5, 3.5, 3.5, 0.12);
			level.sendParticles(ParticleTypes.WITCH, x, y, z, 120, 2.5, 2.5, 2.5, 0.3);
			// extra deep layer under the explosion's own boom (vanilla sound, low pitch)
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
			this.discard();
		}
	}
}
