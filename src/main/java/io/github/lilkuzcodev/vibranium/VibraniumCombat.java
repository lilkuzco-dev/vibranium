package io.github.lilkuzcodev.vibranium;

import java.util.Map;
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
	public static final float SPEAR_REACH = 5.5F;          // survival thrust reach (vanilla spears: 4.5)
	public static final float SPEAR_CREATIVE_REACH = 7.5F; // creative-mode reach (vanilla spears: 6.5)

	// ==================== KINETIC STRIKE CYCLE ====================
	public static final int HITS_TO_PRIME = 6;            // hits that build charge; the NEXT (7th) hit detonates
	public static final double STRIKE_RADIUS = 3.0;       // burst radius around the STRUCK TARGET (not the player)
	public static final float STRIKE_BONUS_DAMAGE = 8.0F; // bonus AoE damage at center = 4 hearts, falls off
	public static final double STRIKE_KNOCKBACK = 1.8;    // horizontal launch at center
	public static final double STRIKE_VERTICAL = 0.9;     // upward launch ("up/away")
	public static final float STRIKE_FALLOFF = 0.6F;      // fraction of damage/knockback lost at the radius edge
	public static final int DECAY_WINDOW_TICKS = 600;     // charges reset after 30 s without landing a hit
	public static final int PARTICLE_THRESHOLD = 4;       // charge count where the buildup particle cue starts

	// ======================= VIBRANIUM ARMOR =======================
	// A tier above netherite, the same way the weapons are: slightly higher on every
	// axis rather than a new mechanic. Vanilla for reference —
	//   netherite: 3/6/8/3 = 20 armor, toughness 3.0, kb 0.1, durability x37, ench 15
	//   diamond:   3/6/8/3 = 20 armor, toughness 2.0, kb 0.0, durability x33, ench 10
	// NOTE on armor points: CombatRules clamps EFFECTIVE armor to 20, so points past
	// 20 only buy headroom against big hits — toughness is the knob that actually
	// moves damage reduction. Both are raised here, toughness deliberately more.
	public static final int ARMOR_DURABILITY_MULTIPLIER = 45; // netherite 37 -> helmet 495, chest 720, legs 675, boots 585
	public static final int ARMOR_DEFENSE_HELMET = 4;         // netherite 3
	public static final int ARMOR_DEFENSE_CHESTPLATE = 9;     // netherite 8
	public static final int ARMOR_DEFENSE_LEGGINGS = 7;       // netherite 6
	public static final int ARMOR_DEFENSE_BOOTS = 3;          // netherite 3  (full set: 23 vs netherite's 20)
	public static final int ARMOR_DEFENSE_BODY = 19;          // netherite 19; unused — we ship no animal armor
	public static final float ARMOR_TOUGHNESS = 4.0F;         // netherite 3.0
	public static final float ARMOR_KNOCKBACK_RESISTANCE = 0.12F; // per piece; netherite 0.1 (full set 0.48 vs 0.4)
	public static final int ARMOR_ENCHANTABILITY = ENCHANTABILITY; // 10 — diamond, same as the weapons

	// ==================== KINETIC WARD (ARMOR) ====================
	// The strike cycle's mirror image: the weapons count hits you LAND, the armor
	// counts hits you TAKE, and the 7th releases the burst around the wearer.
	// Any number of vibranium pieces participate and they share one count (the max
	// across worn pieces), so a full set is not four separate cycles.
	public static final int WARD_HITS_TO_PRIME = 6;       // hits absorbed; the NEXT (7th) detonates
	public static final double WARD_RADIUS = 4.0;         // burst radius around the WEARER (weapon strike is 3.0)
	public static final float WARD_BURST_DAMAGE = 8.0F;   // damage at center = 4 hearts, falls off
	public static final double WARD_KNOCKBACK = 1.6;      // horizontal launch at center
	public static final double WARD_VERTICAL = 0.7;       // upward launch
	public static final float WARD_FALLOFF = 0.6F;        // fraction of damage/knockback lost at the radius edge
	public static final int WARD_DECAY_WINDOW_TICKS = 600; // charges reset after 30 s without taking a hit
	public static final int WARD_PARTICLE_THRESHOLD = 4;  // charge count where the buildup cue starts
	// true  = only damage with an attacker behind it counts (mobs, players, projectiles,
	//         creeper blasts) — a "hit received", not fall damage or drowning.
	// false = every source of damage counts.
	public static final boolean WARD_ATTACKS_ONLY = true;

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

	/**
	 * The worn-armor look: {@code assets/vibranium/equipment/vibranium.json}, which
	 * points the humanoid / humanoid_baby / humanoid_leggings layers at our purple
	 * recolors of the vanilla diamond sheets.
	 */
	public static final ResourceKey<EquipmentAsset> VIBRANIUM_ARMOR_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, Vibranium.id("vibranium"));

	/** Repairs with the ingot (same tag as the tools) and equips with the netherite sound. */
	public static final ArmorMaterial VIBRANIUM_ARMOR_MATERIAL = new ArmorMaterial(
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
			VIBRANIUM_TOOL_MATERIALS,
			VIBRANIUM_ARMOR_ASSET);

	private VibraniumCombat() {
	}
}
