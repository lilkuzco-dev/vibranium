package io.github.lilkuzcodev.vibranium;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The presentation shared by both kinetic cycles: the charge tooltip and the buildup
 * particle. Both read {@link KineticProfile#rainbow()} — vibranium draws one purple, godite
 * sweeps the wheel — so a metal reads the same on the tooltip as it does in the air.
 */
final class KineticFx {
	/** "Kinetic Charge: 4/6", plus an unmistakable primed line once the cycle is ready. */
	static void appendTooltip(Consumer<Component> tooltip, KineticProfile profile, int hits) {
		boolean primed = hits >= profile.hitsToPrime();
		ChatFormatting flat = primed ? profile.primedColor() : profile.color();
		Component charge = Component.translatable(profile.chargeKey(), hits, profile.hitsToPrime());
		tooltip.accept(profile.rainbow()
				? Rainbow.gradient(charge, profile.chargeKey(), flat, false)
				: charge.copy().withStyle(flat));
		if (primed) {
			Component line = Component.translatable(profile.primedKey());
			tooltip.accept(profile.rainbow()
					? Rainbow.gradient(line, profile.primedKey(), profile.primedColor(), true)
					: line.copy().withStyle(profile.primedColor(), ChatFormatting.BOLD));
		}
	}

	/**
	 * The escalating buildup mote. One witch particle per charge for vibranium; for godite,
	 * one dust particle per charge with the hue walking round the wheel as the world ticks,
	 * so a primed godite weapon shimmers rather than sitting on one colour.
	 */
	static void cueBurst(ServerLevel level, KineticProfile profile, double x, double y, double z,
			int count, double spreadX, double spreadY, double spreadZ, double speed) {
		if (!profile.rainbow()) {
			level.sendParticles(ParticleTypes.WITCH, x, y, z, count, spreadX, spreadY, spreadZ, speed);
			return;
		}
		long now = level.getGameTime();
		for (int i = 0; i < count; i++) {
			int color = Rainbow.rgb(now / 20.0 + (double) i / Math.max(1, count));
			level.sendParticles(new DustParticleOptions(color, 1.1F), x, y, z, 1, spreadX, spreadY, spreadZ, speed);
		}
	}

	private KineticFx() {
	}
}
