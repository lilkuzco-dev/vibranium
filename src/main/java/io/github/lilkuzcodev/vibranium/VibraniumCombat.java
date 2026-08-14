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
	public static final float MINING_SPEED = 10.0F;       // netherite 9.0 (one step up)
	public static final float DAMAGE_BONUS = 5.0F;        // netherite 4.0
	public static final int ENCHANTABILITY = 10;          // same as diamond (netherite is 15)
	public static final float SWORD_BASE_DAMAGE = 3.0F;   // vanilla sword baseline
	public static final float SWORD_ATTACK_SPEED = -2.4F; // vanilla sword baseline
	public static final float AXE_BASE_DAMAGE = 5.0F;     // vanilla axe baseline
	public static final float AXE_ATTACK_SPEED = -3.0F;   // diamond/netherite axe baseline

	// ==================== KINETIC DISCHARGE PERK ====================
	public static final float CHARGE_CAP = 20.0F;           // max absorbed damage = 10 hearts
	public static final float ABSORB_RATIO = 1.0F;          // charge per point of (post-armor) damage taken
	public static final float BURST_DAMAGE = 8.0F;          // AoE damage at FULL charge = 4 hearts (scales with charge)
	public static final double BURST_RADIUS = 4.0;          // blocks
	public static final double KNOCKBACK_STRENGTH = 2.5;    // horizontal launch velocity at full charge, point blank
	public static final double KNOCKBACK_VERTICAL = 1.1;    // upward launch velocity at full charge
	public static final float EDGE_FALLOFF = 0.7F;          // fraction of damage/knockback lost at the radius edge
	public static final int DISCHARGE_COOLDOWN_TICKS = 100; // 5 seconds

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
