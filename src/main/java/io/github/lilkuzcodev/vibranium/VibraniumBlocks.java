package io.github.lilkuzcodev.vibranium;

import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class VibraniumBlocks {
	// Hardness/resistance, XP (3-7) and sounds mirror vanilla diamond ore exactly.
	// The required tool tier is data-driven, NOT set here: see
	// src/main/resources/data/minecraft/tags/block/needs_diamond_tool.json
	public static final Block VIBRANIUM_ORE = register(
			"vibranium_ore",
			properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(3.0F, 3.0F));

	public static final Block DEEPSLATE_VIBRANIUM_ORE = register(
			"deepslate_vibranium_ore",
			properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.DEEPSLATE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(4.5F, 3.0F)
					.sound(SoundType.DEEPSLATE));

	public static final Block BLOCK_OF_VIBRANIUM = register(
			"block_of_vibranium",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.requiresCorrectToolForDrops()
					.strength(5.0F, 6.0F)
					.sound(SoundType.METAL));

	// Mirrors vanilla raw_iron_block: same strength, default stone sound.
	public static final Block RAW_VIBRANIUM_BLOCK = register(
			"raw_vibranium_block",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(5.0F, 6.0F));

	// Pit middle shell: deep purple stone, any pickaxe, drops itself. Future crafting material.
	public static final Block VIBRANIUM_VEINSTONE = register(
			"vibranium_veinstone",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(3.0F, 6.0F)
					.sound(SoundType.DEEPSLATE));

	// Pit dressing: amethyst-cluster-style crystal, light level 7, purple.
	public static final Block VIBRANIUM_CRYSTAL_CLUSTER = register(
			"vibranium_crystal_cluster",
			properties -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0F, 10.0F, properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.forceSolidOn()
					.noOcclusion()
					.sound(SoundType.AMETHYST_CLUSTER)
					.strength(1.5F)
					.lightLevel(state -> 7)
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

	// ---------- machines (v1.6.0) ----------
	// Fabricator: crafting-table-like workstation (no fuel/power); casing art from
	// TechReborn (hue-shifted, see CREDITS.md). Strength ~ crafting table but stone-y.
	public static final Block VIBRANIUM_FABRICATOR = register(
			"vibranium_fabricator",
			FabricatorBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(3.5F, 6.0F)
					.sound(SoundType.METAL));

	// Extractor: the fueled auto-miner. LIT swaps the animated drill face in.
	public static final Block VIBRANIUM_EXTRACTOR = register(
			"vibranium_extractor",
			ExtractorBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(3.5F, 6.0F)
					.sound(SoundType.METAL)
					.lightLevel(state -> state.getValue(ExtractorBlock.LIT) ? 7 : 0));

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return Registration.block(name, factory, properties);
	}

	public static void init() {
		// Registration happens in the static initializers above.
	}

	private VibraniumBlocks() {
	}
}
