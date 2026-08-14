package io.github.lilkuzcodev.vibranium;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The vibranium extractor: a fueled machine that mines a 3x3 column straight
 * down beneath itself. The LIT state swaps the bottom face to the animated
 * spinning-drill texture. No facing — the drill points down by definition.
 */
public class ExtractorBlock extends BaseEntityBlock {
	public static final MapCodec<ExtractorBlock> CODEC = simpleCodec(ExtractorBlock::new);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public ExtractorBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends ExtractorBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new ExtractorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, VibraniumMachines.EXTRACTOR_BLOCK_ENTITY,
						(innerLevel, pos, innerState, entity) -> ExtractorBlockEntity.serverTick(serverLevel, pos, innerState, entity))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ExtractorBlockEntity extractor) {
			player.openMenu(extractor);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
		// contents drop via BlockEntity#preRemoveSideEffects (Container default)
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}
}
