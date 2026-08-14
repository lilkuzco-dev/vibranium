package io.github.lilkuzcodev.vibranium;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Extractor GUI: fuel slot (8,53), 9x2 output grid (8,76)/(8,94), player
 * inventory at (8,124) — coordinates match textures/gui/container/extractor.png
 * composed by tools/gen-textures.js. Status/progress reach the client through
 * the synced {@link ContainerData} (see ExtractorBlockEntity's data layout).
 *
 * <p>Slot indices: 0 = fuel, 1..18 = output, 19..45 = inventory, 46..54 = hotbar.
 */
public class ExtractorMenu extends AbstractContainerMenu {
	private static final int OUTPUT_END = ExtractorBlockEntity.OUTPUT_START + ExtractorBlockEntity.OUTPUT_COUNT; // exclusive
	private static final int INV_END = OUTPUT_END + 36; // exclusive

	private final Container container;
	private final ContainerData data;
	private final net.minecraft.world.level.Level level;

	public ExtractorMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(ExtractorBlockEntity.OUTPUT_START + ExtractorBlockEntity.OUTPUT_COUNT),
				new SimpleContainerData(ExtractorBlockEntity.DATA_COUNT));
	}

	public ExtractorMenu(final int containerId, final Inventory inventory, final Container container, final ContainerData data) {
		super(VibraniumMachines.EXTRACTOR_MENU, containerId);
		checkContainerSize(container, ExtractorBlockEntity.OUTPUT_START + ExtractorBlockEntity.OUTPUT_COUNT);
		checkContainerDataCount(data, ExtractorBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		this.level = inventory.player.level();
		this.addSlot(new FuelSlot(container, ExtractorBlockEntity.SLOT_FUEL, 8, 53));
		for (int i = 0; i < ExtractorBlockEntity.OUTPUT_COUNT; i++) {
			this.addSlot(new OutputSlot(container, ExtractorBlockEntity.OUTPUT_START + i, 8 + 18 * (i % 9), 76 + 18 * (i / 9)));
		}
		this.addStandardInventorySlots(inventory, 8, 124);
		this.addDataSlots(data);
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex < OUTPUT_END) {
				// machine -> player
				if (!this.moveItemStackTo(stack, OUTPUT_END, INV_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, clicked);
			} else if (this.slots.get(ExtractorBlockEntity.SLOT_FUEL).mayPlace(stack)) {
				if (!this.moveItemStackTo(stack, ExtractorBlockEntity.SLOT_FUEL, ExtractorBlockEntity.SLOT_FUEL + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				int hotbarStart = INV_END - 9;
				if (slotIndex < hotbarStart) {
					if (!this.moveItemStackTo(stack, hotbarStart, INV_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.moveItemStackTo(stack, OUTPUT_END, hotbarStart, false)) {
					return ItemStack.EMPTY;
				}
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
		}
		return clicked;
	}

	// ---------- data accessors for the screen ----------
	public boolean isLit() {
		return this.data.get(0) > 0;
	}

	public float litProgress() {
		int total = this.data.get(1);
		return Mth.clamp((float) this.data.get(0) / (total == 0 ? 200 : total), 0.0F, 1.0F);
	}

	public float mineProgress() {
		int total = this.data.get(3);
		int current = this.data.get(2);
		return total != 0 && current != 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
	}

	public int status() {
		return this.data.get(4);
	}

	public int mineY() {
		return this.data.get(5);
	}

	public boolean boosted() {
		return this.data.get(6) != 0;
	}

	private class FuelSlot extends Slot {
		FuelSlot(final Container container, final int index, final int x, final int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			// same rule as ExtractorBlockEntity#canPlaceItem, evaluated against
			// whichever level this menu lives on (server BE or client mirror)
			return stack.is(VibraniumItems.VIBRANIUM_INGOT)
					|| ExtractorMenu.this.level.fuelValues().isFuel(stack);
		}
	}

	private static class OutputSlot extends Slot {
		OutputSlot(final Container container, final int index, final int x, final int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			return false;
		}
	}
}
