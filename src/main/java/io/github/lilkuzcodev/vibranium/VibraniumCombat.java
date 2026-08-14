package io.github.lilkuzcodev.vibranium;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * Every combat-related tuning knob in one place.
 */
public final class VibraniumCombat {
	// ======================= VIBRANIUM WEAPON TIER =======================
	// Vanilla damage math: displayed attack damage = 1 (player) + weapon baseline
	// + material DAMAGE_BONUS. Displayed speed = 4 + weapon speed baseline.
	//   -> sword: 1 + 3.0 + 5.0 = 9 damage, 4 - 2.4 = 1.6 speed  (netherite: 8 / 1.6)
	//   -> axe:   1 + 5.0 + 5.0 = 11 damage, 4 - 3.0 = 1.0 speed (netherite: 10 / 1.0)
	public static final int DURABILITY = 2600;            // netherite 2031, diamond 1561
	public static final float MINING_SPEED = 11.0F;       // netherite 9.0
	public static final float DAMAGE_BONUS = 5.0F;        // netherite 4.0
	public static final int ENCHANTABILITY = 10;          // same as diamond (netherite is 15)
	public static final float SWORD_BASE_DAMAGE = 3.0F;   // vanilla sword baseline
	public static final float SWORD_ATTACK_SPEED = -2.4F; // vanilla sword baseline
	public static final float AXE_BASE_DAMAGE = 5.0F;     // vanilla axe baseline
	public static final float AXE_ATTACK_SPEED = -3.0F;   // diamond/netherite axe baseline
	// tools: vanilla baselines (pickaxe 1.0/-2.8, shovel 1.5/-3.0, hoe cancels to 1 dmg)
	public static final float HOE_BASE_DAMAGE = -5.0F;    // -5 + 5 bonus + 1 = 1 damage, like all hoes

	// ======================= VIBRANIUM SPEAR =======================
	// Mirrors the vanilla 26.2 spear (kinetic charge component, piercing, STAB swing)
	// with a longer ATTACK_RANGE and tier-above damage. Reach is vanilla's own
	// AttackRange component — no custom hit handling.
	public static final float SPEAR_ATTACK_DURATION = 1.0F / 1.2F; // -> exactly 1.2 attack speed
	public static final float SPEAR_BASE_DAMAGE = 2.0F;   // 1 + 2 + 5 bonus = 8 total (sword is 9)
	public static final float SPEAR_REACH = 5.5F;         // standard thrust reach (vanilla spears: 4.5)
	public static final float SPEAR_CHARGED_REACH = 7.5F; // sprint/mounted lunge reach (vanilla: 6.5)

	// ==================== KINETIC STRIKE CYCLE ====================
	public static final int HITS_TO_PRIME = 6;            // hits that build charge; the NEXT (7th) hit detonates
	public static final double STRIKE_RADIUS = 3.0;       // burst radius around the STRUCK TARGET (not the player)
	public static final float STRIKE_BONUS_DAMAGE = 8.0F; // bonus AoE damage at center = 4 hearts, falls off
	public static final double STRIKE_KNOCKBACK = 1.8;    // horizontal launch at center
	public static final double STRIKE_VERTICAL = 0.9;     // upward launch ("up/away")
	public static final float STRIKE_FALLOFF = 0.6F;      // fraction of damage/knockback lost at the radius edge
	public static final int DECAY_WINDOW_TICKS = 600;     // charges reset after 30 s without landing a hit
	public static final int PARTICLE_THRESHOLD = 4;       // charge count where the buildup particle cue starts

	// ==================== KINETIC ENERGY BALL ====================
	public static final float ENERGY_BALL_EXPLOSION_POWER = 20.0F; // TNT is 4.0
	public static final boolean ENERGY_BALL_BREAKS_TERRAIN = true; // false = entity damage only, no crater
	public static final int ENERGY_BALL_STACK_SIZE = 16;           // like ender pearls
	// Recipe yield (8) lives in data/vibranium/recipe/vibranium_energy_ball.json ("count": 8).

	/** Anvil repair uses this tag (data/vibranium/tags/item/vibranium_tool_materials.json = the ingot). */
	public static final TagKey<Item> VIBRANIUM_TOOL_MATERIALS =
			TagKey.create(Registries.ITEM, Vibranium.id("vibranium_tool_materials"));

	// Own tier above netherite: nothing in vanilla is incorrect for netherite tools,
	// so the netherite incorrect-blocks tag is exactly right for a higher tier too.
	public static final ToolMaterial VIBRANIUM_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL, DURABILITY, MINING_SPEED, DAMAGE_BONUS, ENCHANTABILITY,
			VIBRANIUM_TOOL_MATERIALS);

	private VibraniumCombat() {
	}
}
