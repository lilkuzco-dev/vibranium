package io.github.lilkuzcodev.vibranium;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class VibraniumEntities {
	// builder settings mirror vanilla's snowball/ender pearl
	public static final EntityType<KineticEnergyBallEntity> KINETIC_ENERGY_BALL = register("kinetic_energy_ball",
			EntityType.Builder.<KineticEnergyBallEntity>of(KineticEnergyBallEntity::new, MobCategory.MISC)
					.noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Vibranium.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}

	private VibraniumEntities() {
	}
}
