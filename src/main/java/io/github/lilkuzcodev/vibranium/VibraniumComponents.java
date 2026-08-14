package io.github.lilkuzcodev.vibranium;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class VibraniumComponents {
	/** Absorbed kinetic damage stored on a weapon (persists on the stack, survives relog). */
	public static final DataComponentType<Float> KINETIC_CHARGE = DataComponentType.<Float>builder()
			.persistent(Codec.FLOAT)
			.networkSynchronized(ByteBufCodecs.FLOAT)
			.build();

	public static void init() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Vibranium.id("kinetic_charge"), KINETIC_CHARGE);
	}

	private VibraniumComponents() {
	}
}
