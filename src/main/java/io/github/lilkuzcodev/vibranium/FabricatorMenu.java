package io.github.lilkuzcodev.vibranium;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * 5x5 crafting menu backed by the "fabricating" recipe type. Modeled on vanilla
 * {@link net.minecraft.world.inventory.CraftingMenu} minus the recipe-book
 * plumbing (fabricating recipes are not in any recipe book).
 *
 * <p>Slot indices: 0 = result, 1..25 = grid, 26..52 = inventory, 53..61 = hotbar.
 * Screen-coordinate layout matches textures/gui/container/fabricator.png, which
 * tools/gen-textures.js composes: grid at (8,16), result at (130,52), player
 * inventory at (8,124).
 *
 * <p>Tier note: the protected constructor takes the grid size; a SUPER
 * FABRICATOR registers its own MenuType calling it with bigger dimensions.
 */
public class FabricatorMenu extends AbstractContainerMenu {
	public static final int GRID_WIDTH = 5;
	public static final int GRID_HEIGHT = 5;
	private static final int RESULT_SLOT = 0;
	private static final int GRID_START = 1;

	private final ContainerLevelAccess access;
	private final Player player;
	final CraftingContainer craftSlots;
	private final ResultContainer resultSlots = new ResultContainer();
	private final int gridEnd; // exclusive
	private final int invEnd; // exclusive (inventory + hotbar)

	public FabricatorMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public FabricatorMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
		this(VibraniumMachines.FABRICATOR_MENU, GRID_WIDTH, GRID_HEIGHT, containerId, inventory, access);
	}

	protected FabricatorMenu(final MenuType<?> menuType, final int gridWidth, final int gridHeight,
			final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
		super(menuType, containerId);
		this.access = access;
		this.player = inventory.player;
		this.craftSlots = new TransientCraftingContainer(this, gridWidth, gridHeight);
		this.gridEnd = GRID_START + gridWidth * gridHeight;
		this.invEnd = this.gridEnd + 36;
		this.addSlot(new FabricatorResultSlot(this.player, this.craftSlots, this.resultSlots, 0, 130, 52));
		for (int y = 0; y < gridHeight; y++) {
			for (int x = 0; x < gridWidth; x++) {
				this.addSlot(new Slot(this.craftSlots, x + y * gridWidth, 8 + x * 18, 16 + y * 18));
			}
		}
		this.addStandardInventorySlots(inventory, 8, 124);
	}

	@Override
	public void slotsChanged(final Container container) {
		this.access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				CraftingInput input = this.craftSlots.asCraftInput();
				ItemStack result = ItemStack.EMPTY;
				Optional<RecipeHolder<FabricatingRecipe>> maybeRecipe =
						serverLevel.getServer().getRecipeManager().getRecipeFor(VibraniumMachines.FABRICATING, input, serverLevel);
				if (maybeRecipe.isPresent()) {
					RecipeHolder<FabricatingRecipe> holder = maybeRecipe.get();
					if (this.resultSlots.setRecipeUsed((ServerPlayer) this.player, holder)) {
						ItemStack assembled = holder.value().assemble(input);
						if (assembled.isItemEnabled(serverLevel.enabledFeatures())) {
							result = assembled;
						}
					}
				}
				this.resultSlots.setItem(0, result);
				this.setRemoteSlot(0, result);
				((ServerPlayer) this.player).connection.send(
						new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, result));
			}
		});
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
	}

	@Override
	public boolean stillValid(final Player player) {
		// any fabricator tier keeps the menu open (cf. vanilla's single-block check)
		return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof FabricatorBlock
				&& player.isWithinBlockInteractionRange(pos, 4.0), true);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex == RESULT_SLOT) {
				stack.getItem().onCraftedBy(stack, player);
				if (!this.moveItemStackTo(stack, this.gridEnd, this.invEnd, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, clicked);
			} else if (slotIndex >= this.gridEnd) {
				// inventory <-> hotbar, or into the grid
				if (!this.moveItemStackTo(stack, GRID_START, this.gridEnd, false)) {
					int hotbarStart = this.invEnd - 9;
					if (slotIndex < hotbarStart) {
						if (!this.moveItemStackTo(stack, hotbarStart, this.invEnd, false)) {
							return ItemStack.EMPTY;
						}
					} else if (!this.moveItemStackTo(stack, this.gridEnd, hotbarStart, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (!this.moveItemStackTo(stack, this.gridEnd, this.invEnd, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (stack.getCount() == clicked.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
			if (slotIndex == RESULT_SLOT) {
				player.drop(stack, false);
			}
		}
		return clicked;
	}

	@Override
	public boolean canTakeItemForPickAll(final ItemStack carried, final Slot target) {
		return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
	}

	/**
	 * Result slot for the fabricating grid. Vanilla's ResultSlot is hardwired to
	 * RecipeType.CRAFTING when computing container-item remainders, so this
	 * variant applies the per-item default remainder (bucket-style) directly.
	 */
	static class FabricatorResultSlot extends Slot {
		private final CraftingContainer craftSlots;
		private final Player player;
		private int removeCount;

		FabricatorResultSlot(final Player player, final CraftingContainer craftSlots, final Container container,
				final int index, final int x, final int y) {
			super(container, index, x, y);
			this.player = player;
			this.craftSlots = craftSlots;
		}

		@Override
		public boolean mayPlace(final ItemStack itemStack) {
			return false;
		}

		@Override
		public ItemStack remove(final int amount) {
			if (this.hasItem()) {
				this.removeCount += Math.min(amount, this.getItem().getCount());
			}
			return super.remove(amount);
		}

		@Override
		protected void onQuickCraft(final ItemStack picked, final int count) {
			this.removeCount += count;
			this.checkTakeAchievements(picked);
		}

		@Override
		protected void onSwapCraft(final int count) {
			this.removeCount += count;
		}

		@Override
		protected void checkTakeAchievements(final ItemStack carried) {
			if (this.removeCount > 0) {
				carried.onCraftedBy(this.player, this.removeCount);
			}
			if (this.container instanceof net.minecraft.world.inventory.RecipeCraftingHolder holder) {
				holder.awardUsedRecipes(this.player, this.craftSlots.getItems());
			}
			this.removeCount = 0;
		}

		@Override
		public void onTake(final Player player, final ItemStack carried) {
			this.checkTakeAchievements(carried);
			CraftingInput.Positioned positioned = this.craftSlots.asPositionedCraftInput();
			CraftingInput input = positioned.input();
			int left = positioned.left();
			int top = positioned.top();
			NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
			for (int y = 0; y < input.height(); y++) {
				for (int x = 0; x < input.width(); x++) {
					int slot = x + left + (y + top) * this.craftSlots.getWidth();
					ItemStack inGrid = this.craftSlots.getItem(slot);
					ItemStack replacement = remaining.get(x + y * input.width());
					if (!inGrid.isEmpty()) {
						this.craftSlots.removeItem(slot, 1);
						inGrid = this.craftSlots.getItem(slot);
					}
					if (!replacement.isEmpty()) {
						if (inGrid.isEmpty()) {
							this.craftSlots.setItem(slot, replacement);
						} else if (ItemStack.isSameItemSameComponents(inGrid, replacement)) {
							replacement.grow(inGrid.getCount());
							this.craftSlots.setItem(slot, replacement);
						} else if (!this.player.getInventory().add(replacement)) {
							this.player.drop(replacement, false);
						}
					}
				}
			}
		}

		@Override
		public boolean isFake() {
			return true;
		}
	}
}
