package io.github.lilkuzcodev.vibranium;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * {@code /vibranium_census <chunkRadius>} — counts vibranium and diamond ore blocks in the
 * square of chunks around the command source, generating chunks on demand. Diamond is counted
 * too as a control, so the measured rarity ratio can be compared against vanilla.
 */
public final class VibraniumCensusCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
				dispatcher.register(Commands.literal("vibranium_census")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("chunkRadius", IntegerArgumentType.integer(1, 12))
								.executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "chunkRadius"))))));
	}

	private static int run(CommandSourceStack source, int radius) {
		ServerLevel level = source.getLevel();
		ChunkPos center = ChunkPos.containing(BlockPos.containing(source.getPosition()));
		long vibranium = 0;
		long diamond = 0;
		int chunks = 0;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int cx = center.x() - radius; cx <= center.x() + radius; cx++) {
			for (int cz = center.z() - radius; cz <= center.z() + radius; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz); // generates the chunk if missing
				chunks++;
				for (int y = level.getMinY(); y < 64; y++) {
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							BlockState state = chunk.getBlockState(cursor.set((cx << 4) + x, y, (cz << 4) + z));
							if (state.is(VibraniumBlocks.VIBRANIUM_ORE) || state.is(VibraniumBlocks.DEEPSLATE_VIBRANIUM_ORE)) {
								vibranium++;
							} else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
								diamond++;
							}
						}
					}
				}
			}
		}
		final int chunkCount = chunks;
		final long vib = vibranium;
		final long dia = diamond;
		source.sendSuccess(() -> Component.literal(String.format(
				"Census over %d chunks: vibranium=%d (%.3f/chunk), diamond=%d (%.3f/chunk), diamond:vibranium = %.1f:1",
				chunkCount, vib, (double) vib / chunkCount, dia, (double) dia / chunkCount,
				vib == 0 ? 0.0 : (double) dia / vib)), false);
		return (int) vib;
	}

	private VibraniumCensusCommand() {
	}
}
