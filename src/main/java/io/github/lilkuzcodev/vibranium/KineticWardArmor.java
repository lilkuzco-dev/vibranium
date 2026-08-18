package io.github.lilkuzcodev.vibranium;

/**
 * Marker for armour that runs the kinetic ward cycle (see {@link KineticWard}), plus the
 * tuning that cycle uses. A mixed set wards on the WEAKEST worn piece's profile.
 */
public interface KineticWardArmor {
	/** The ward profile this piece contributes, e.g. {@code VibraniumCombat.VIBRANIUM_WARD}. */
	KineticProfile kineticProfile();
}
