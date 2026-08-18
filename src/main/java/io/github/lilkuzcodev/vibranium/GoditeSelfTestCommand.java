package io.github.lilkuzcodev.vibranium;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantable;

/**
 * {@code /godite_selftest} — headless proof of the two rules godite is defined by, asserted
 * against the REGISTERED items rather than against the constants they were built from.
 *
 * <p>Both rules fail silently if they are wrong. A weapon whose damage is 6% higher instead of
 * 10% looks completely normal in game; so does a cycle that primes on the 6th hit; so does a
 * launch that carries a mob 40 blocks instead of 120. Nothing throws, nothing logs. So:
 *
 * <ol>
 * <li>every offensive and defensive number is exactly {@link GoditeCombat#STRENGTH_MULTIPLIER}
 *     times its vibranium counterpart, read off the real attribute components — with the two
 *     integer-rounded exceptions (armour points, durability) called out by name</li>
 * <li>attack speed and reach are UNCHANGED, which is half of what "only 10% stronger" means</li>
 * <li>both godite cycles detonate on the 5th hit and vibranium's still detonate on the 7th</li>
 * <li>the launch, run through vanilla's own drag model, carries past 100 blocks</li>
 * <li>godite gear is enchantable exactly where its vibranium counterpart is (a custom item
 *     missing from {@code #minecraft:swords} offers no enchantments, with no error at all)</li>
 * <li>the cycle markers, equip slots, equipment asset and repair material are wired</li>
 * </ol>
 */
public final class GoditeSelfTestCommand {
	/** Tolerance on a float-derived stat: tight enough to catch a wrong constant. */
	private static final double EPSILON = 1.0E-4;

	/** Every {@code #minecraft:enchantable/*} tag, so the comparison covers all of them. */
	private static final List<TagKey<Item>> ENCHANTABLE_TAGS = List.of(
			ItemTags.ARMOR_ENCHANTABLE, ItemTags.FOOT_ARMOR_ENCHANTABLE, ItemTags.LEG_ARMOR_ENCHANTABLE,
			ItemTags.CHEST_ARMOR_ENCHANTABLE, ItemTags.HEAD_ARMOR_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE,
			ItemTags.WEAPON_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.MELEE_WEAPON_ENCHANTABLE,
			ItemTags.SWEEPING_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.LUNGE_ENCHANTABLE,
			ItemTags.MINING_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE,
			ItemTags.DURABILITY_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE,
			ItemTags.BOW_ENCHANTABLE, ItemTags.CROSSBOW_ENCHANTABLE, ItemTags.TRIDENT_ENCHANTABLE,
			ItemTags.FISHING_ENCHANTABLE, ItemTags.MACE_ENCHANTABLE);

	/** Every godite item paired with the vibranium item it must be exactly 10% above. */
	private static final List<Item[]> GEAR_PAIRS = List.of(
			new Item[]{GoditeItems.GODITE_SWORD, VibraniumItems.VIBRANIUM_SWORD},
			new Item[]{GoditeItems.GODITE_AXE, VibraniumItems.VIBRANIUM_AXE},
			new Item[]{GoditeItems.GODITE_PICKAXE, VibraniumItems.VIBRANIUM_PICKAXE},
			new Item[]{GoditeItems.GODITE_SHOVEL, VibraniumItems.VIBRANIUM_SHOVEL},
			new Item[]{GoditeItems.GODITE_HOE, VibraniumItems.VIBRANIUM_HOE},
			new Item[]{GoditeItems.GODITE_SPEAR, VibraniumItems.VIBRANIUM_SPEAR},
			new Item[]{GoditeItems.GODITE_HELMET, VibraniumItems.VIBRANIUM_HELMET},
			new Item[]{GoditeItems.GODITE_CHESTPLATE, VibraniumItems.VIBRANIUM_CHESTPLATE},
			new Item[]{GoditeItems.GODITE_LEGGINGS, VibraniumItems.VIBRANIUM_LEGGINGS},
			new Item[]{GoditeItems.GODITE_BOOTS, VibraniumItems.VIBRANIUM_BOOTS});

	/** The weapons and tools, where "10% stronger" is a damage number. */
	private static final List<Item[]> WEAPON_PAIRS = GEAR_PAIRS.subList(0, 6);

	/** Armour pieces paired with their slot and the vibranium piece they must be 10% above. */
	private static final List<Object[]> ARMOR_PIECES = List.of(
			new Object[]{GoditeItems.GODITE_HELMET, EquipmentSlot.HEAD, VibraniumItems.VIBRANIUM_HELMET},
			new Object[]{GoditeItems.GODITE_CHESTPLATE, EquipmentSlot.CHEST, VibraniumItems.VIBRANIUM_CHESTPLATE},
			new Object[]{GoditeItems.GODITE_LEGGINGS, EquipmentSlot.LEGS, VibraniumItems.VIBRANIUM_LEGGINGS},
			new Object[]{GoditeItems.GODITE_BOOTS, EquipmentSlot.FEET, VibraniumItems.VIBRANIUM_BOOTS});

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
				dispatcher.register(Commands.literal("godite_selftest")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> run(ctx.getSource()))));
	}

	private static int run(final CommandSourceStack source) {
		List<String> failures = new ArrayList<>();
		List<String> notes = new ArrayList<>();
		float scale = GoditeCombat.STRENGTH_MULTIPLIER;

		// --- 1: displayed damage is exactly 10% up; attack speed is exactly unchanged ---
		for (Item[] pair : WEAPON_PAIRS) {
			String name = GearStats.key(pair[0]);
			double mine = GearStats.displayedAttackDamage(pair[0]);
			double theirs = GearStats.displayedAttackDamage(pair[1]);
			if (Math.abs(mine - theirs * scale) > EPSILON) {
				failures.add(String.format(Locale.ROOT, "%s: %.4f damage, expected %.4f (vibranium %.4f x %.2f)",
						name, mine, theirs * scale, theirs, scale));
			}
			double mineSpeed = GearStats.displayedAttackSpeed(pair[0]);
			double theirSpeed = GearStats.displayedAttackSpeed(pair[1]);
			if (Math.abs(mineSpeed - theirSpeed) > EPSILON) {
				failures.add(String.format(Locale.ROOT, "%s: attack speed %.4f, vibranium's is %.4f — speed must NOT scale",
						name, mineSpeed, theirSpeed));
			}
		}
		notes.add(String.format(Locale.ROOT, "damage x%.2f on %d weapons/tools, attack speed unchanged; sword %.2f -> %.2f, axe %.2f -> %.2f",
				scale, WEAPON_PAIRS.size(),
				GearStats.displayedAttackDamage(VibraniumItems.VIBRANIUM_SWORD), GearStats.displayedAttackDamage(GoditeItems.GODITE_SWORD),
				GearStats.displayedAttackDamage(VibraniumItems.VIBRANIUM_AXE), GearStats.displayedAttackDamage(GoditeItems.GODITE_AXE)));

		// --- 2: durability, mining speed, enchantability ---
		for (Item[] pair : GEAR_PAIRS) {
			String name = GearStats.key(pair[0]);
			Enchantable mineEnchant = new ItemStack(pair[0]).get(DataComponents.ENCHANTABLE);
			Enchantable theirEnchant = new ItemStack(pair[1]).get(DataComponents.ENCHANTABLE);
			if (mineEnchant == null || theirEnchant == null || mineEnchant.value() != theirEnchant.value()) {
				failures.add(name + ": enchantability " + (mineEnchant == null ? "none" : mineEnchant.value())
						+ ", vibranium's is " + (theirEnchant == null ? "none" : theirEnchant.value()));
			}
			for (TagKey<Item> tag : ENCHANTABLE_TAGS) {
				if (new ItemStack(pair[0]).is(tag) != new ItemStack(pair[1]).is(tag)) {
					failures.add(name + ": " + tag.location() + " membership differs from vibranium's");
				}
			}
		}
		// Durability splits in two. A tool's is a direct integer knob, so it takes the exact 10%.
		// A piece of armour's is (material multiplier x the vanilla per-slot base), so it can only
		// move in whole multiplier steps: 45 x 1.10 = 49.5 rounds to 50, which lands every piece at
		// +11.1% rather than +10%. That is the closest the scale allows, and it is asserted as
		// exactly that rather than waved through.
		for (Item[] pair : WEAPON_PAIRS) {
			int mine = new ItemStack(pair[0]).getMaxDamage();
			int theirs = new ItemStack(pair[1]).getMaxDamage();
			if (mine != Math.round(theirs * scale)) {
				failures.add(GearStats.key(pair[0]) + ": durability " + mine + ", expected "
						+ Math.round(theirs * scale) + " (vibranium " + theirs + ")");
			}
		}
		if (GoditeCombat.ARMOR_DURABILITY_MULTIPLIER != Math.round(VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER * scale)) {
			failures.add("armor durability multiplier " + GoditeCombat.ARMOR_DURABILITY_MULTIPLIER + ", expected "
					+ Math.round(VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER * scale));
		}
		for (Object[] piece : ARMOR_PIECES) {
			int mine = new ItemStack((Item) piece[0]).getMaxDamage();
			int theirs = new ItemStack((Item) piece[2]).getMaxDamage();
			int expected = theirs / VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER * GoditeCombat.ARMOR_DURABILITY_MULTIPLIER;
			if (mine != expected) {
				failures.add(GearStats.key((Item) piece[0]) + ": durability " + mine + ", expected " + expected
						+ " (vibranium " + theirs + " at multiplier " + VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER + ")");
			}
		}

		double mineSpeed = new ItemStack(GoditeItems.GODITE_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		double vibSpeed = new ItemStack(VibraniumItems.VIBRANIUM_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		if (Math.abs(mineSpeed - vibSpeed * scale) > EPSILON) {
			failures.add(String.format(Locale.ROOT, "mining speed %.4f, expected %.4f", mineSpeed, vibSpeed * scale));
		}
		notes.add(String.format(Locale.ROOT,
				"durability: tools x%.2f (sword %d -> %d), armour multiplier %d -> %d = +%.1f%% (integer step); "
						+ "mining speed %.1f -> %.1f; enchantability unchanged at %d",
				scale, new ItemStack(VibraniumItems.VIBRANIUM_SWORD).getMaxDamage(),
				new ItemStack(GoditeItems.GODITE_SWORD).getMaxDamage(),
				VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER, GoditeCombat.ARMOR_DURABILITY_MULTIPLIER,
				100.0 * (GoditeCombat.ARMOR_DURABILITY_MULTIPLIER / (double) VibraniumCombat.ARMOR_DURABILITY_MULTIPLIER - 1.0),
				vibSpeed, mineSpeed, GoditeCombat.ENCHANTABILITY));

		// --- 3: the armour set, read live off the components ---
		double armor = 0.0;
		double toughness = 0.0;
		double knockback = 0.0;
		double vibArmor = 0.0;
		double vibToughness = 0.0;
		double vibKnockback = 0.0;
		for (Object[] piece : ARMOR_PIECES) {
			EquipmentSlot slot = (EquipmentSlot) piece[1];
			armor += GearStats.attribute((Item) piece[0], slot, Attributes.ARMOR);
			toughness += GearStats.attribute((Item) piece[0], slot, Attributes.ARMOR_TOUGHNESS);
			knockback += GearStats.attribute((Item) piece[0], slot, Attributes.KNOCKBACK_RESISTANCE);
			vibArmor += GearStats.attribute((Item) piece[2], slot, Attributes.ARMOR);
			vibToughness += GearStats.attribute((Item) piece[2], slot, Attributes.ARMOR_TOUGHNESS);
			vibKnockback += GearStats.attribute((Item) piece[2], slot, Attributes.KNOCKBACK_RESISTANCE);
		}
		// toughness and knockback resistance take the exact 10%; armour POINTS are integers per
		// piece, so 23 x 1.10 = 25.3 lands on 25 — the closest the scale allows, asserted as such.
		if (Math.abs(toughness - vibToughness * scale) > EPSILON) {
			failures.add(String.format(Locale.ROOT, "toughness %.4f, expected %.4f", toughness, vibToughness * scale));
		}
		if (Math.abs(knockback - vibKnockback * scale) > EPSILON) {
			failures.add(String.format(Locale.ROOT, "knockback resistance %.4f, expected %.4f", knockback, vibKnockback * scale));
		}
		double expectedArmor = 0.0;
		for (Object[] piece : ARMOR_PIECES) {
			expectedArmor += Math.round(GearStats.attribute((Item) piece[2], (EquipmentSlot) piece[1], Attributes.ARMOR) * scale);
		}
		if (Math.abs(armor - expectedArmor) > EPSILON) {
			failures.add(String.format(Locale.ROOT, "armor points %.0f, expected %.0f (vibranium %.0f, rounded per piece)",
					armor, expectedArmor, vibArmor));
		}
		notes.add(String.format(Locale.ROOT, "full set: %.0f armor / %.1f toughness / %.3f knockback  (vibranium: %.0f / %.1f / %.3f)",
				armor, toughness, knockback, vibArmor, vibToughness, vibKnockback));

		// --- 4: the cycles fire on the 5th hit, and vibranium's still fire on the 7th ---
		for (KineticProfile profile : List.of(GoditeCombat.GODITE_STRIKE, GoditeCombat.GODITE_WARD)) {
			if (profile.detonatingHit() != 5) {
				failures.add("godite cycle detonates on hit " + profile.detonatingHit() + ", expected the 5th");
			}
			if (profile.knockback() != GoditeCombat.LAUNCH_KNOCKBACK || profile.vertical() != GoditeCombat.LAUNCH_VERTICAL) {
				failures.add("godite cycle launch is " + profile.knockback() + "/" + profile.vertical()
						+ ", expected " + GoditeCombat.LAUNCH_KNOCKBACK + "/" + GoditeCombat.LAUNCH_VERTICAL);
			}
			if (!profile.rainbow()) {
				failures.add("godite cycle is not drawn rainbow");
			}
			if (profile.tier() <= VibraniumCombat.VIBRANIUM_WARD.tier()) {
				failures.add("godite tier " + profile.tier() + " does not outrank vibranium's "
						+ VibraniumCombat.VIBRANIUM_WARD.tier() + " — a mixed set would resolve wrong");
			}
		}
		for (KineticProfile profile : List.of(VibraniumCombat.VIBRANIUM_STRIKE, VibraniumCombat.VIBRANIUM_WARD)) {
			if (profile.detonatingHit() != 7 || profile.rainbow()) {
				failures.add("vibranium's cycle changed: detonates on hit " + profile.detonatingHit()
						+ ", rainbow=" + profile.rainbow() + " — godite must not have altered it");
			}
		}

		// --- 5: the launch actually reaches three digits, under vanilla's own drag model ---
		double projected = projectedLaunchBlocks(GoditeCombat.LAUNCH_KNOCKBACK, GoditeCombat.LAUNCH_VERTICAL);
		double vibraniumProjected = projectedLaunchBlocks(VibraniumCombat.STRIKE_KNOCKBACK, VibraniumCombat.STRIKE_VERTICAL);
		if (projected < 100.0) {
			failures.add(String.format(Locale.ROOT, "launch projects to %.1f blocks, under the 100-block target", projected));
		}
		notes.add(String.format(Locale.ROOT,
				"launch %.1f/%.1f projects to %.0f blocks of open ground (vibranium's %.1f/%.1f: %.0f) — model, not a measurement",
				GoditeCombat.LAUNCH_KNOCKBACK, GoditeCombat.LAUNCH_VERTICAL, projected,
				VibraniumCombat.STRIKE_KNOCKBACK, VibraniumCombat.STRIKE_VERTICAL, vibraniumProjected));

		// --- 6: cycle markers, equip slots, equipment asset, repair material ---
		for (Item item : List.of(GoditeItems.GODITE_SWORD, GoditeItems.GODITE_AXE, GoditeItems.GODITE_SPEAR)) {
			if (!(item instanceof KineticCycleWeapon weapon) || weapon.kineticProfile() != GoditeCombat.GODITE_STRIKE) {
				failures.add(GearStats.key(item) + ": does not run the godite strike cycle");
			}
		}
		for (Object[] piece : ARMOR_PIECES) {
			Item item = (Item) piece[0];
			EquipmentSlot expected = (EquipmentSlot) piece[1];
			ItemStack stack = new ItemStack(item);
			String name = GearStats.key(item);
			if (!(item instanceof KineticWardArmor armor2) || armor2.kineticProfile() != GoditeCombat.GODITE_WARD) {
				failures.add(name + ": does not run the godite ward cycle");
			}
			var equippable = stack.get(DataComponents.EQUIPPABLE);
			if (equippable == null || equippable.slot() != expected) {
				failures.add(name + ": equips to " + (equippable == null ? "nothing" : equippable.slot()) + ", expected " + expected);
			} else if (equippable.assetId().isEmpty() || !equippable.assetId().get().equals(GoditeCombat.GODITE_ARMOR_ASSET)) {
				failures.add(name + ": equipment asset is " + equippable.assetId() + ", expected " + GoditeCombat.GODITE_ARMOR_ASSET);
			}
		}
		for (Item[] pair : GEAR_PAIRS) {
			var repairable = new ItemStack(pair[0]).get(DataComponents.REPAIRABLE);
			if (repairable == null || !repairable.isValidRepairItem(new ItemStack(GoditeItems.GODITE_INGOT))) {
				failures.add(GearStats.key(pair[0]) + ": does not anvil-repair with a godite ingot");
			}
			if (repairable != null && repairable.isValidRepairItem(new ItemStack(VibraniumItems.VIBRANIUM_INGOT))) {
				failures.add(GearStats.key(pair[0]) + ": repairs with a VIBRANIUM ingot — wrong tier's material");
			}
		}
		notes.add("wiring: 3 weapons on the godite strike cycle, 4 armour pieces on the godite ward, "
				+ GEAR_PAIRS.size() + " items repairing with the godite ingot only");

		for (String note : notes) {
			source.sendSuccess(() -> Component.literal("  " + note), false);
		}
		if (failures.isEmpty()) {
			source.sendSuccess(() -> Component.literal("godite self-test: PASS"), true);
			return 1;
		}
		for (String failure : failures) {
			source.sendFailure(Component.literal("  FAIL " + failure));
		}
		source.sendFailure(Component.literal("godite self-test: " + failures.size() + " FAILURE(S)"));
		return 0;
	}

	/**
	 * How far a launched mob travels over open ground, under vanilla's own per-tick model:
	 * position advances by the velocity, then {@code vy = (vy - 0.08) * 0.98} and the
	 * horizontal component decays by the 0.91 air-drag factor. A grounded victim's vertical
	 * comes from {@code LivingEntity.knockback}, which clamps its own lift to 0.4, plus the
	 * {@code push()} the cycle adds on top.
	 *
	 * <p>This is a projection, not a measurement — the real flight is in VERIFY.md. It is
	 * asserted anyway because it is the cheap check that catches a retuned constant.
	 */
	private static double projectedLaunchBlocks(double horizontal, double vertical) {
		double x = 0.0;
		double y = 0.0;
		double vx = horizontal;
		double vy = Math.min(0.4, horizontal) + vertical;
		for (int tick = 0; tick < 400; tick++) {
			x += vx;
			y += vy;
			if (y <= 0.0 && tick > 1) {
				break;
			}
			vy = (vy - 0.08) * 0.98;
			vx *= 0.91;
		}
		return x;
	}

	private GoditeSelfTestCommand() {
	}
}
