package io.github.lilkuzcodev.vibranium;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The vibranium fabricator: a crafting-table-style workstation (stateless, no
 * block entity — grid contents return to the player on close) whose menu crafts
 * from the mod's own 5x5 "fabricating" recipe type. Future tiers (SUPER
 * FABRICATOR) subclass or re-instantiate this block and open a menu registered
 * with their own grid size; FabricatorMenu#stillValid accepts any
 * FabricatorBlock, so higher tiers keep working against this menu code.
 */
public class FabricatorBlock extends Block {
	public static final MapCodec<FabricatorBlock> CODEC = simpleCodec(FabricatorBlock::new);
	private static final Component CONTAINER_TITLE = Component.translatable("container.vibranium.fabricator");

	public FabricatorBlock(final BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends FabricatorBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
		return new SimpleMenuProvider(
				(containerId, inventory, player) -> new FabricatorMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
				CONTAINER_TITLE);
	}
}
