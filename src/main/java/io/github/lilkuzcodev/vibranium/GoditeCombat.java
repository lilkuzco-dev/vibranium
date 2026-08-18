package io.github.lilkuzcodev.vibranium;

import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Every godite tuning knob in one place — the tier above vibranium.
 *
 * <p>Godite is deliberately NOT a free-hand rebalance. Two rules define it and the whole
 * class is those two rules applied:
 *
 * <ol>
 * <li><b>Exactly 10% stronger than vibranium, and nothing else.</b> Every offensive and
 *     defensive number is {@link #STRENGTH_MULTIPLIER} times vibranium's, DERIVED from
 *     {@link VibraniumCombat} rather than retyped, so retuning vibranium retunes godite and
 *     the gap cannot silently drift. Attack speed and reach are untouched — those are not
 *     strength, and scaling them would be a much larger buff than 10%.</li>
 * <li><b>Its own mechanic is the launch.</b> The kinetic cycle fires on every 5th hit
 *     instead of every 7th, and it launches for {@link #LAUNCH_KNOCKBACK} instead of
 *     vibranium's 1.8. That is the thing you buy godite for; the 10% is the trim.</li>
 * </ol>
 */
public final class GoditeCombat {
	/** Progression rank, one above {@link VibraniumCombat#TIER}. See {@link KineticWard} for its only use. */
	public static final int TIER = VibraniumCombat.TIER + 1;

	/** The whole first rule. Every derived number below is vibranium's times this. */
	public static final float STRENGTH_MULTIPLIER = 1.10F;

	// ======================= GODITE WEAPON TIER =======================
	// Vanilla damage math: displayed attack damage = 1 (player) + weapon baseline
	// + material DAMAGE_BONUS. The per-weapon baselines below are solved BACKWARDS from
	// that formula (see scaledBase) so each weapon's DISPLAYED number — the one the player
	// actually reads — lands exactly 10% above its vibranium counterpart:
	//   -> sword: 9.9 damage vs vibranium 9.0, 1.6 speed (unchanged)
	//   -> axe:  12.1 damage vs vibranium 11.0, 1.0 speed (unchanged)
	public static final int DURABILITY = Math.round(VibraniumCombat.DURABILITY * STRENGTH_MULTIPLIER);   // 2860
	public static final float MINING_SPEED = VibraniumCombat.MINING_SPEED * STRENGTH_MULTIPLIER;         // 12.1
	public static final float DAMAGE_BONUS = VibraniumCombat.DAMAGE_BONUS * STRENGTH_MULTIPLIER;         // 5.5
	/** Unchanged at 10 (diamond's). Enchantability is not strength — see the class doc. */
	public static final int ENCHANTABILITY = VibraniumCombat.ENCHANTABILITY;
	public static final float SWORD_BASE_DAMAGE = scaledBase(VibraniumCombat.SWORD_BASE_DAMAGE);         // 3.4
	public static final float SWORD_ATTACK_SPEED = VibraniumCombat.SWORD_ATTACK_SPEED;                   // -2.4
	public static final float AXE_BASE_DAMAGE = scaledBase(VibraniumCombat.AXE_BASE_DAMAGE);             // 5.6
	public static final float AXE_ATTACK_SPEED = VibraniumCombat.AXE_ATTACK_SPEED;                       // -3.0
	public static final float PICKAXE_BASE_DAMAGE = scaledBase(1.0F);                                    // 1.2
	public static final float PICKAXE_ATTACK_SPEED = -2.8F;                                              // vanilla baseline
	public static final float SHOVEL_BASE_DAMAGE = scaledBase(1.5F);                                     // 1.75
	public static final float SHOVEL_ATTACK_SPEED = -3.0F;                                               // vanilla baseline
	/** A hoe is 1 damage everywhere in vanilla; 10% of that is 1.1, and the rule has no exceptions. */
	public static final float HOE_BASE_DAMAGE = scaledBase(VibraniumCombat.HOE_BASE_DAMAGE);             // -5.4

	// ======================= GODITE SPEAR =======================
	// Reach is untouched (vibranium's 5.5 / 7.5): it is not strength, and doubling down on
	// the one weapon that already outranges everything is not what "10% stronger" means.
	public static final float SPEAR_ATTACK_DURATION = VibraniumCombat.SPEAR_ATTACK_DURATION;
	public static final float SPEAR_BASE_DAMAGE = scaledBase(VibraniumCombat.SPEAR_BASE_DAMAGE);         // 2.3 -> 8.8 shown
	public static final float SPEAR_REACH = VibraniumCombat.SPEAR_REACH;
	public static final float SPEAR_CREATIVE_REACH = VibraniumCombat.SPEAR_CREATIVE_REACH;

	// ==================== THE LAUNCH — GODITE'S OWN MECHANIC ====================
	// Both cycles fire on the 5th hit and both launch for the same absurd amount: the sword
	// on the 5th hit you LAND, the armour on the 5th you TAKE.
	//
	// Minecraft ballistics for a launched mob, per tick: horizontal *= 0.91 (air drag),
	// vy = (vy - 0.08) * 0.98. Total horizontal travel converges to v0 / 0.09 = v0 * 11.11,
	// so distance is bounded by the INITIAL speed, not by hang time — reaching three digits
	// takes v0 >= 9. LAUNCH_VERTICAL exists to buy the ~45 ticks of air needed to spend it:
	// 11.0 with 1.6 of lift measures ~120 blocks over open ground (see VERIFY.md).
	public static final int HITS_TO_PRIME = 4;            // hits that build charge; the NEXT (5th) detonates
	public static final double LAUNCH_KNOCKBACK = 11.0;   // horizontal launch at centre (vibranium: 1.8)
	public static final double LAUNCH_VERTICAL = 1.6;     // upward launch at centre (vibranium: 0.9)
	public static final float STRIKE_BONUS_DAMAGE = VibraniumCombat.STRIKE_BONUS_DAMAGE * STRENGTH_MULTIPLIER; // 8.8
	public static final float WARD_BURST_DAMAGE = VibraniumCombat.WARD_BURST_DAMAGE * STRENGTH_MULTIPLIER;     // 8.8
	public static final double STRIKE_RADIUS = VibraniumCombat.STRIKE_RADIUS * STRENGTH_MULTIPLIER;      // 3.3
	public static final double WARD_RADIUS = VibraniumCombat.WARD_RADIUS * STRENGTH_MULTIPLIER;          // 4.4
	public static final float FALLOFF = VibraniumCombat.STRIKE_FALLOFF;   // 0.6 — the edge still flies ~50 blocks
	public static final int DECAY_WINDOW_TICKS = VibraniumCombat.DECAY_WINDOW_TICKS;   // 30 s, as vibranium
	public static final int PARTICLE_THRESHOLD = 2;       // half of 4, as vibranium's 4 is of its 6

	// ======================= GODITE ARMOR =======================
	// Vibranium for reference: 4/9/7/3 = 23 armour, toughness 4.0, kb 0.12/piece, x45 durability.
	// Armour points are integers, so the 10% lands as 4/10/8/3 = 25 (+8.7%, the closest the
	// scale allows); toughness and knockback resistance take the exact 10%, and toughness is
	// the knob that actually moves damage reduction once effective armour clamps at 20.
	// 45 x 1.10 = 49.5, which rounds to 50 — so the pieces land at +11.1% rather than +10%.
	// Durability here is (this multiplier x the vanilla per-slot base), so it can only move in
	// whole steps; 50 is the closest the scale allows. /godite_selftest asserts exactly this.
	public static final int ARMOR_DURABILITY_MULTIPLIER = Math.round(VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER * STRENGTH_MULTIPLIER); // 50
	public static final int ARMOR_DEFENSE_HELMET = Math.round(VibraniumCombat.ARMOR_DEFENSE_HELMET * STRENGTH_MULTIPLIER);         // 4
	public static final int ARMOR_DEFENSE_CHESTPLATE = Math.round(VibraniumCombat.ARMOR_DEFENSE_CHESTPLATE * STRENGTH_MULTIPLIER); // 10
	public static final int ARMOR_DEFENSE_LEGGINGS = Math.round(VibraniumCombat.ARMOR_DEFENSE_LEGGINGS * STRENGTH_MULTIPLIER);     // 8
	public static final int ARMOR_DEFENSE_BOOTS = Math.round(VibraniumCombat.ARMOR_DEFENSE_BOOTS * STRENGTH_MULTIPLIER);           // 3
	public static final int ARMOR_DEFENSE_BODY = Math.round(VibraniumCombat.ARMOR_DEFENSE_BODY * STRENGTH_MULTIPLIER);             // 21; unused
	public static final float ARMOR_TOUGHNESS = VibraniumCombat.ARMOR_TOUGHNESS * STRENGTH_MULTIPLIER;             // 4.4
	public static final float ARMOR_KNOCKBACK_RESISTANCE = VibraniumCombat.ARMOR_KNOCKBACK_RESISTANCE * STRENGTH_MULTIPLIER; // 0.132
	public static final int ARMOR_ENCHANTABILITY = ENCHANTABILITY;

	// ==================== THE TWO CYCLE PROFILES ====================
	// Same shared machinery vibranium runs (KineticStrike / KineticWard); only these numbers
	// differ. rainbow=true is what makes the tooltip and the burst sweep the wheel.
	public static final KineticProfile GODITE_STRIKE = new KineticProfile(
			TIER, HITS_TO_PRIME, STRIKE_RADIUS, STRIKE_BONUS_DAMAGE, LAUNCH_KNOCKBACK, LAUNCH_VERTICAL,
			FALLOFF, DECAY_WINDOW_TICKS, PARTICLE_THRESHOLD, true,
			"item.vibranium.godite_charge", "item.vibranium.godite_primed",
			ChatFormatting.GOLD, ChatFormatting.YELLOW, true);

	public static final KineticProfile GODITE_WARD = new KineticProfile(
			TIER, HITS_TO_PRIME, WARD_RADIUS, WARD_BURST_DAMAGE, LAUNCH_KNOCKBACK, LAUNCH_VERTICAL,
			FALLOFF, DECAY_WINDOW_TICKS, PARTICLE_THRESHOLD, VibraniumCombat.WARD_ATTACKS_ONLY,
			"item.vibranium.godite_ward", "item.vibranium.godite_ward_primed",
			ChatFormatting.GOLD, ChatFormatting.YELLOW, true);

	/**
	 * The per-weapon baseline that puts the DISPLAYED damage exactly {@link #STRENGTH_MULTIPLIER}
	 * above vibranium's, given that both materials add a different flat bonus on top.
	 * Displayed damage is {@code 1 + baseline + materialBonus}, so this inverts that.
	 */
	private static float scaledBase(float vibraniumBaseline) {
		float vibraniumDisplayed = 1.0F + vibraniumBaseline + VibraniumCombat.DAMAGE_BONUS;
		return vibraniumDisplayed * STRENGTH_MULTIPLIER - 1.0F - DAMAGE_BONUS;
	}

	/** Anvil repair uses this tag (data/vibranium/tags/item/godite_tool_materials.json = the ingot). */
	public static final TagKey<Item> GODITE_TOOL_MATERIALS =
			TagKey.create(Registries.ITEM, Vibranium.id("godite_tool_materials"));

	// Nothing in vanilla is incorrect for a netherite tool, so that tag is right for every
	// tier above netherite too — godite mines everything vibranium does.
	public static final ToolMaterial GODITE_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL, DURABILITY, MINING_SPEED, DAMAGE_BONUS, ENCHANTABILITY,
			GODITE_TOOL_MATERIALS);

	/** The worn-armour look: {@code assets/vibranium/equipment/godite.json}. */
	public static final ResourceKey<EquipmentAsset> GODITE_ARMOR_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, Vibranium.id("godite"));

	/** Repairs with the godite ingot (same tag as the tools) and equips with the netherite sound. */
	public static final ArmorMaterial GODITE_ARMOR_MATERIAL = new ArmorMaterial(
			ARMOR_DURABILITY_MULTIPLIER,
			Map.of(ArmorType.HELMET, ARMOR_DEFENSE_HELMET,
					ArmorType.CHESTPLATE, ARMOR_DEFENSE_CHESTPLATE,
					ArmorType.LEGGINGS, ARMOR_DEFENSE_LEGGINGS,
					ArmorType.BOOTS, ARMOR_DEFENSE_BOOTS,
					ArmorType.BODY, ARMOR_DEFENSE_BODY),
			ARMOR_ENCHANTABILITY,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			ARMOR_TOUGHNESS,
			ARMOR_KNOCKBACK_RESISTANCE,
			GODITE_TOOL_MATERIALS,
			GODITE_ARMOR_ASSET);

	private GoditeCombat() {
	}
}
