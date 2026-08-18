package io.github.lilkuzcodev.vibranium;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Godite's four blocks: the two ores and the two storage blocks. Everything mirrors the
 * vibranium equivalents — same hardness, same XP band, same diamond-tier harvest gate (in
 * {@code data/minecraft/tags/block/needs_diamond_tool.json}) — because the rarity of the
 * ore is what makes godite hard to get, not the hardness of the block.
 *
 * <p>No pits, veinstone or crystal clusters: godite is a plain ore, five times rarer.
 */
public final class GoditeBlocks {
	// A rainbow has no map colour, so the ore blocks stay stone/deepslate like vibranium's
	// and the metal blocks take magenta — the most vivid thing on the map palette, and
	// unmistakably not vibranium's purple when the two sit side by side in a base.
	public static final Block GODITE_ORE = Registration.block(
			"godite_ore",
			properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(3.0F, 3.0F));

	public static final Block DEEPSLATE_GODITE_ORE = Registration.block(
			"deepslate_godite_ore",
			properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.DEEPSLATE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(4.5F, 3.0F)
					.sound(SoundType.DEEPSLATE));

	public static final Block BLOCK_OF_GODITE = Registration.block(
			"block_of_godite",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_MAGENTA)
					.requiresCorrectToolForDrops()
					.strength(5.0F, 6.0F)
					.sound(SoundType.METAL));

	public static final Block RAW_GODITE_BLOCK = Registration.block(
			"raw_godite_block",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_MAGENTA)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(5.0F, 6.0F));

	public static void init() {
		// Registration happens in the static initializers above.
	}

	private GoditeBlocks() {
	}
}
