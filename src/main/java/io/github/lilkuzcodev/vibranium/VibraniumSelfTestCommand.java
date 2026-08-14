package io.github.lilkuzcodev.vibranium;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * {@code /vibranium_fabricator_selftest} — headless proof of the fabricating
 * recipe wiring, for dev-server batteries (crafting GUIs can't be driven over
 * RCON). Asserts:
 * <ol>
 * <li>the exact 5x5 extractor layout matches via the FABRICATING recipe type</li>
 * <li>a perturbed layout (one frame ingot swapped for iron) does NOT match</li>
 * <li>the same 5x5 input yields nothing from the vanilla CRAFTING type
 *     (fabricating recipes are fabricator-exclusive)</li>
 * <li>the fabricator's own 3x3 recipe resolves via the vanilla CRAFTING type
 *     (it is craftable on a regular crafting table)</li>
 * </ol>
 */
public final class VibraniumSelfTestCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
				dispatcher.register(Commands.literal("vibranium_fabricator_selftest")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> run(ctx.getSource()))));
	}

	private static CraftingInput extractorLayout(final Item cornerFrame) {
		// IIIII / IVRVI / IRPRI / IVRVI / IIIII  (I=ingot, V=veinstone, R=redstone block, P=iron pickaxe)
		Item ingot = VibraniumItems.VIBRANIUM_INGOT;
		Item vein = VibraniumBlocks.VIBRANIUM_VEINSTONE.asItem();
		Item red = Items.REDSTONE_BLOCK;
		Item pick = Items.IRON_PICKAXE;
		Item[] grid = {
				cornerFrame, ingot, ingot, ingot, ingot,
				ingot, vein, red, vein, ingot,
				ingot, red, pick, red, ingot,
				ingot, vein, red, vein, ingot,
				ingot, ingot, ingot, ingot, ingot,
		};
		List<ItemStack> stacks = new ArrayList<>(grid.length);
		for (Item item : grid) {
			stacks.add(new ItemStack(item));
		}
		return CraftingInput.of(5, 5, stacks);
	}

	private static int run(final CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		int failures = 0;

		CraftingInput exact = extractorLayout(VibraniumItems.VIBRANIUM_INGOT);
		Optional<RecipeHolder<FabricatingRecipe>> hit =
				level.getServer().getRecipeManager().getRecipeFor(VibraniumMachines.FABRICATING, exact, level);
		ItemStack result = hit.map(h -> h.value().assemble(exact)).orElse(ItemStack.EMPTY);
		failures += report(source, "exact 5x5 layout -> vibranium_extractor via fabricating",
				result.is(VibraniumBlocks.VIBRANIUM_EXTRACTOR.asItem()));

		CraftingInput perturbed = extractorLayout(Items.IRON_INGOT);
		failures += report(source, "perturbed layout (iron corner) -> no fabricating match",
				level.getServer().getRecipeManager().getRecipeFor(VibraniumMachines.FABRICATING, perturbed, level).isEmpty());

		failures += report(source, "exact 5x5 layout -> no VANILLA crafting match (fabricator-exclusive)",
				level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, exact, level).isEmpty());

		Item ingot = VibraniumItems.VIBRANIUM_INGOT;
		Item vein = VibraniumBlocks.VIBRANIUM_VEINSTONE.asItem();
		Item[] fabricatorGrid = {
				ingot, Items.OBSIDIAN, ingot,
				vein, Items.CRAFTING_TABLE, vein,
				ingot, Items.OBSIDIAN, ingot,
		};
		List<ItemStack> fabStacks = new ArrayList<>();
		for (Item item : fabricatorGrid) {
			fabStacks.add(new ItemStack(item));
		}
		CraftingInput fabricatorInput = CraftingInput.of(3, 3, fabStacks);
		ItemStack fabricatorResult = level.getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, fabricatorInput, level)
				.map(h -> h.value().assemble(fabricatorInput)).orElse(ItemStack.EMPTY);
		failures += report(source, "3x3 fabricator recipe -> vibranium_fabricator via VANILLA crafting",
				fabricatorResult.is(VibraniumBlocks.VIBRANIUM_FABRICATOR.asItem()));

		final int failed = failures;
		source.sendSuccess(() -> Component.literal(failed == 0 ? "SELFTEST PASS (4/4)" : "SELFTEST FAIL (" + failed + " failed)"), false);
		return failed == 0 ? 1 : 0;
	}

	private static int report(final CommandSourceStack source, final String name, final boolean ok) {
		source.sendSuccess(() -> Component.literal((ok ? "PASS: " : "FAIL: ") + name), false);
		return ok ? 0 : 1;
	}

	private VibraniumSelfTestCommand() {
	}
}
