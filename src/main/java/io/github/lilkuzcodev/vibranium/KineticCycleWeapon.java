package io.github.lilkuzcodev.vibranium;

/**
 * Marker for weapons that run the kinetic strike cycle (see {@link KineticStrike}), plus
 * the tuning that cycle uses — which is what makes the mechanic shareable across metals
 * instead of copied per metal.
 */
public interface KineticCycleWeapon {
	/** The strike profile this weapon detonates on, e.g. {@code VibraniumCombat.VIBRANIUM_STRIKE}. */
	KineticProfile kineticProfile();
}
