package io.github.lilkuzcodev.vibranium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class VibraniumComponents {
	/**
	 * Kinetic strike cycle state on a weapon: hits landed this cycle + when the last
	 * one landed (for the out-of-combat decay window). Persists on the stack.
	 */
	public record KineticHits(int hits, long lastHitTime) {
		public static final Codec<KineticHits> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("hits").forGetter(KineticHits::hits),
				Codec.LONG.fieldOf("last_hit_time").forGetter(KineticHits::lastHitTime)
		).apply(i, KineticHits::new));

		public static final StreamCodec<ByteBuf, KineticHits> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, KineticHits::hits,
				ByteBufCodecs.VAR_LONG, KineticHits::lastHitTime,
				KineticHits::new);
	}

	public static final DataComponentType<KineticHits> KINETIC_HITS = DataComponentType.<KineticHits>builder()
			.persistent(KineticHits.CODEC)
			.networkSynchronized(KineticHits.STREAM_CODEC)
			.build();

	/**
	 * @deprecated The pre-1.3.0 absorb/discharge mechanic's component. The type stays
	 * registered so weapons saved by older versions still deserialize; nothing writes it.
	 */
	@Deprecated
	public static final DataComponentType<Float> KINETIC_CHARGE = DataComponentType.<Float>builder()
			.persistent(Codec.FLOAT)
			.networkSynchronized(ByteBufCodecs.FLOAT)
			.build();

	public static void init() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Vibranium.id("kinetic_hits"), KINETIC_HITS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Vibranium.id("kinetic_charge"), KINETIC_CHARGE);
	}

	private VibraniumComponents() {
	}
}
