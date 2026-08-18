package io.github.lilkuzcodev.vibranium;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Applies a kinetic burst's impulse at the END of the server tick rather than in the middle
 * of the hit that caused it.
 *
 * <p><b>Why this exists.</b> {@code Player.attack} applies its OWN knockback to the entity it
 * struck, and it does so after {@code Item.hurtEnemy} — which is where the strike cycle runs.
 * {@code LivingEntity.knockback} halves whatever velocity it finds before adding its own, so
 * vanilla's little shove silently ate half of ours: godite's 11.0 launch reached the zombie as
 * a measured 6.78, and 122 blocks of flight arrived as 79.8. Nothing errored. The burst still
 * looked and sounded exactly right — it was simply weaker than every constant said it was, and
 * the same bug had been quietly halving vibranium's 1.8 since the strike cycle shipped.
 *
 * <p>Deferring to {@code END_SERVER_TICK} puts our impulse last: vanilla's knockback has already
 * been applied and read, and the entity integrates our velocity on the following tick. The
 * one-tick delay is invisible — the burst's particles and sound play on the hit tick either way.
 */
public final class KineticLaunch {
	private record Pending(LivingEntity victim, double horizontal, double vertical, Vec3 away, DamageSource source) {
	}

	private static final List<Pending> QUEUE = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> flush());
	}

	/**
	 * Queues one victim's launch. {@code away} is the horizontal unit vector pointing from the
	 * burst's centre toward the victim; both magnitudes should already have falloff applied.
	 */
	static void queue(LivingEntity victim, double horizontal, double vertical, Vec3 away, DamageSource source) {
		QUEUE.add(new Pending(victim, horizontal, vertical, away, source));
	}

	private static void flush() {
		if (QUEUE.isEmpty()) {
			return;
		}
		for (Pending pending : QUEUE) {
			LivingEntity victim = pending.victim();
			if (!victim.isAlive() || victim.isRemoved()) {
				continue;
			}
			// knockback() pushes OPPOSITE the vector it is given, so the away-vector is negated
			victim.knockback(pending.horizontal(), -pending.away().x, -pending.away().z, pending.source(), 0.0F);
			double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			victim.push(0.0, pending.vertical() * Math.max(0.0, 1.0 - resistance), 0.0);
			victim.hurtMarked = true;
		}
		QUEUE.clear();
	}

	private KineticLaunch() {
	}
}
