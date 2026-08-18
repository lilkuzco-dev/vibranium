package io.github.lilkuzcodev.vibranium;

import io.github.lilkuzcodev.vibranium.VibraniumComponents.KineticHits;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The kinetic WARD cycle — the armour's mirror of {@link KineticStrike}. The weapons
 * count the hits you land; the armour counts the hits you take. Every hit received
 * while wearing vibranium adds one charge (cap {@link VibraniumCombat#WARD_HITS_TO_PRIME});
 * the next hit after priming releases the burst — an AoE detonation centred on the
 * WEARER that damages and launches everything around them — then the cycle restarts.
 * Charges decay out of combat. All numbers live in VibraniumCombat.
 *
 * <p>Every worn vibranium piece carries the same {@code kinetic_hits} component the
 * weapons use, and they share one count (the highest across the worn pieces). Wearing
 * four pieces is therefore one cycle, not four — and the count survives swapping a
 * piece mid-fight, because the survivors carry it.
 *
 * <p>Counting rides on Fabric's {@code AFTER_DAMAGE}, which fires server-side once per
 * damage event that actually landed. There is no armour equivalent of
 * {@code Item.hurtEnemy} in 26.2, so this is the hook.
 */
public final class KineticWard {
	/** Armour slots, in the order the tooltip/particle cue prefers when several are worn. */
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET,
	};

	/**
	 * Server-thread latch: a ward burst never triggers another ward burst. Without it,
	 * two primed wearers in each other's radius detonate back and forth in one damage
	 * event. The hit still counts for anyone caught in a burst — it just cannot
	 * detonate until the next hit that is not itself part of a burst.
	 */
	private static boolean bursting;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(
				(entity, source, baseDamage, damageTaken, blocked) -> onDamage(entity, source, damageTaken));
	}

	/** Called for every landed damage event: counts the hit or detonates. */
	private static void onDamage(LivingEntity wearer, DamageSource source, float damageTaken) {
		if (!(wearer.level() instanceof ServerLevel level) || wearer instanceof ArmorStand) {
			return;
		}
		if (damageTaken <= 0.0F) {
			return; // fully absorbed/blocked — nothing got through, so nothing was stored
		}
		if (VibraniumCombat.WARD_ATTACKS_ONLY && source.getEntity() == null) {
			return; // environmental damage (fall, fire, drowning) is not a hit received
		}
		List<ItemStack> worn = wornPieces(wearer);
		if (worn.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		int hits = chargeOf(worn, now);
		if (hits >= VibraniumCombat.WARD_HITS_TO_PRIME && !bursting) {
			for (ItemStack piece : worn) {
				piece.remove(VibraniumComponents.KINETIC_HITS);
			}
			burst(level, wearer);
		} else {
			KineticHits next = new KineticHits(Math.min(hits + 1, VibraniumCombat.WARD_HITS_TO_PRIME), now);
			for (ItemStack piece : worn) {
				piece.set(VibraniumComponents.KINETIC_HITS, next);
			}
		}
	}

	/** AoE burst centred on the wearer: bonus damage + launch, no blocks, never the wearer. */
	private static void burst(ServerLevel level, LivingEntity wearer) {
		bursting = true;
		try {
			double radius = VibraniumCombat.WARD_RADIUS;
			DamageSource source = wearer instanceof Player player
					? level.damageSources().playerAttack(player)
					: level.damageSources().mobAttack(wearer);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
					wearer.getBoundingBox().inflate(radius, Math.max(1.5, radius * 0.75), radius),
					v -> v != wearer && v.isAlive() && !v.isSpectator() && !(v instanceof ArmorStand))) {
				double distance = victim.distanceTo(wearer);
				if (distance > radius) {
					continue;
				}
				float falloff = 1.0F - (float) (distance / radius) * VibraniumCombat.WARD_FALLOFF;

				// whoever just hit us is i-framed from their own attack — the burst must land
				victim.invulnerableTime = 0;
				victim.hurtServer(level, source, VibraniumCombat.WARD_BURST_DAMAGE * falloff);

				Vec3 away = victim.position().subtract(wearer.position());
				Vec3 flat = new Vec3(away.x, 0.0, away.z);
				Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
				victim.knockback(VibraniumCombat.WARD_KNOCKBACK * falloff, -dir.x, -dir.z, source, 0.0F);
				double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
				victim.push(0.0, VibraniumCombat.WARD_VERTICAL * falloff * Math.max(0.0, 1.0 - resistance), 0.0);
				victim.hurtMarked = true;
			}

			double x = wearer.getX();
			double y = wearer.getY() + wearer.getBbHeight() * 0.5;
			double z = wearer.getZ();
			level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.WITCH, x, y, z, 60, 1.4, 0.9, 1.4, 0.25);
			level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 0.6, 0.4, 0.6, 0.35);
			// a shockwave ring at the edge, so the radius is something you can see
			double radiusSteps = VibraniumCombat.WARD_RADIUS;
			for (int i = 0; i < 32; i++) {
				double angle = i * Math.PI * 2.0 / 32.0;
				level.sendParticles(ParticleTypes.WITCH,
						x + Math.cos(angle) * radiusSteps, wearer.getY() + 0.15, z + Math.sin(angle) * radiusSteps,
						2, 0.05, 0.05, 0.05, 0.01);
			}
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.6F);
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.6F);
		} finally {
			bursting = false;
		}
	}

	/** The worn vibranium pieces, in cue order. Empty when none are worn. */
	private static List<ItemStack> wornPieces(LivingEntity wearer) {
		List<ItemStack> worn = new ArrayList<>(ARMOR_SLOTS.length);
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = wearer.getItemBySlot(slot);
			if (stack.getItem() instanceof KineticWardArmor) {
				worn.add(stack);
			}
		}
		return worn;
	}

	/** The set's shared charge: the highest count across worn pieces, zero once decayed. */
	private static int chargeOf(List<ItemStack> worn, long now) {
		int hits = 0;
		for (ItemStack piece : worn) {
			KineticHits state = piece.get(VibraniumComponents.KINETIC_HITS);
			if (state != null && now - state.lastHitTime() <= VibraniumCombat.WARD_DECAY_WINDOW_TICKS) {
				hits = Math.max(hits, state.hits());
			}
		}
		return hits;
	}

	/** Tooltip: "Kinetic Ward: 4/6", plus an unmistakable primed line. */
	public static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		int hits = state == null ? 0 : state.hits();
		tooltip.accept(Component.translatable("item.vibranium.kinetic_ward", hits, VibraniumCombat.WARD_HITS_TO_PRIME)
				.withStyle(hits >= VibraniumCombat.WARD_HITS_TO_PRIME ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
		if (hits >= VibraniumCombat.WARD_HITS_TO_PRIME) {
			tooltip.accept(Component.translatable("item.vibranium.kinetic_ward_primed").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		}
	}

	/**
	 * Worn tick: out-of-combat decay + the escalating buildup cue. Decay is a comparison
	 * against the stored game time rather than a countdown, so a piece that missed ticks
	 * (stored in a chest, carried through a dimension change) still reads correctly.
	 */
	public static void tickWorn(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (slot == null || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
			return;
		}
		KineticHits state = stack.get(VibraniumComponents.KINETIC_HITS);
		if (state == null || state.hits() <= 0) {
			return;
		}
		long now = level.getGameTime();
		if (now - state.lastHitTime() > VibraniumCombat.WARD_DECAY_WINDOW_TICKS) {
			stack.remove(VibraniumComponents.KINETIC_HITS); // cycle broken: reset to 0
			return;
		}
		if (state.hits() < VibraniumCombat.WARD_PARTICLE_THRESHOLD || !isCueSlot(owner, slot)) {
			return; // one cue for the whole set, not one per piece
		}
		boolean primed = state.hits() >= VibraniumCombat.WARD_HITS_TO_PRIME;
		long interval = primed ? 5L : 12L;
		if (now % interval != 0L) {
			return;
		}
		int count = primed ? 6 : state.hits() - VibraniumCombat.WARD_PARTICLE_THRESHOLD + 1;
		level.sendParticles(ParticleTypes.WITCH,
				owner.getX(), owner.getY() + 1.0, owner.getZ(), count, 0.45, 0.5, 0.45, 0.01);
		if (primed) {
			level.sendParticles(ParticleTypes.END_ROD,
					owner.getX(), owner.getY() + 1.0, owner.getZ(), 3, 0.5, 0.5, 0.5, 0.01);
		}
	}

	/** True for the highest-priority vibranium piece worn, so the cue fires once per wearer. */
	private static boolean isCueSlot(Entity owner, EquipmentSlot slot) {
		if (!(owner instanceof LivingEntity wearer)) {
			return false;
		}
		for (EquipmentSlot candidate : ARMOR_SLOTS) {
			if (wearer.getItemBySlot(candidate).getItem() instanceof KineticWardArmor) {
				return candidate == slot;
			}
		}
		return false;
	}

	private KineticWard() {
	}
}
