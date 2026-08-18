package io.github.lilkuzcodev.vibranium;

import net.minecraft.ChatFormatting;

/**
 * One kinetic cycle's tuning, in one object.
 *
 * <p>The strike cycle ({@link KineticStrike}) and the ward cycle ({@link KineticWard})
 * are shared machinery: every metal in this mod runs the same code and differs only in
 * these numbers. Vibranium's two profiles live in {@link VibraniumCombat}, godite's in
 * {@link GoditeCombat}. Adding a metal means adding profiles, not copying a mechanic.
 *
 * @param tier			   progression rank, low to high (vibranium 1, godite 2). Used
 *						   only to resolve a MIXED armour set — see {@link KineticWard}.
 * @param hitsToPrime	   hits that build charge; the NEXT hit detonates, so a cycle
 *						   "every 5th hit" is {@code hitsToPrime = 4}
 * @param radius		   burst radius, in blocks
 * @param burstDamage	   bonus damage at the centre, before falloff
 * @param knockback		   horizontal launch at the centre, in blocks/tick
 * @param vertical		   upward launch at the centre, in blocks/tick
 * @param falloff		   fraction of damage and knockback lost at the radius edge
 * @param decayWindowTicks charge resets after this long without a qualifying hit
 * @param particleThreshold charge count where the buildup cue starts
 * @param attacksOnly	   ward cycles only: true counts only damage with an attacker behind
 *						   it (mobs, players, projectiles, blasts) as a "hit received", so
 *						   fall and fire do not charge the cycle. Ignored by strike cycles.
 * @param chargeKey		   translation key for the "n/m" tooltip line
 * @param primedKey		   translation key for the primed tooltip line
 * @param color			   tooltip colour while charging (ignored when {@code rainbow})
 * @param primedColor	   tooltip colour once primed (ignored when {@code rainbow})
 * @param rainbow		   draw the tooltip and the burst in cycling hues instead of
 *						   one flat colour — godite's identity, vibranium's purple is not
 */
public record KineticProfile(
		int tier,
		int hitsToPrime,
		double radius,
		float burstDamage,
		double knockback,
		double vertical,
		float falloff,
		int decayWindowTicks,
		int particleThreshold,
		boolean attacksOnly,
		String chargeKey,
		String primedKey,
		ChatFormatting color,
		ChatFormatting primedColor,
		boolean rainbow) {

	/** The hit number that detonates, for tooltips and self-tests: 4 primed hits -> "every 5th". */
	public int detonatingHit() {
		return hitsToPrime + 1;
	}
}
