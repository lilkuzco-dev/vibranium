package io.github.lilkuzcodev.vibranium;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The vibranium extractor's brain: burns standard furnace fuels (or vibranium
 * ingots for a speed boost) to mine a 3x3 column straight down, one block per
 * interval, collecting normal drops (diamond-tier tool, no silk/fortune) into
 * an 18-slot buffer. Halts with a GUI-shown reason on fluid sources and
 * unbreakable blocks; finishes at the bottom of the world. Auto-pushes output
 * into an adjacent chest or barrel. All rates live in {@link VibraniumMachines}.
 *
 * <p>Slots: 0 = fuel (insert from top/sides), 1..18 = output (extract from
 * bottom/sides). Vanilla hoppers can only pull from a container above them, so
 * in unmodded play side-extraction means the chest auto-push; a hopper directly
 * beneath works once the column below it is finished.
 */
public class ExtractorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	static final int SLOT_FUEL = 0;
	static final int OUTPUT_START = 1;
	static final int OUTPUT_COUNT = 18;
	private static final int CONTAINER_SIZE = OUTPUT_START + OUTPUT_COUNT;
	private static final int[] SLOTS_FOR_UP = {SLOT_FUEL};
	private static final int[] SLOTS_FOR_DOWN;
	private static final int[] SLOTS_FOR_SIDES;
	static {
		SLOTS_FOR_DOWN = new int[OUTPUT_COUNT];
		for (int i = 0; i < OUTPUT_COUNT; i++) SLOTS_FOR_DOWN[i] = OUTPUT_START + i;
		SLOTS_FOR_SIDES = new int[CONTAINER_SIZE];
		for (int i = 0; i < CONTAINER_SIZE; i++) SLOTS_FOR_SIDES[i] = i;
	}

	// status codes, mirrored by ExtractorScreen's lang keys
	static final int STATUS_NO_FUEL = 0;
	static final int STATUS_RUNNING = 1;
	static final int STATUS_HALT_LAVA = 2;
	static final int STATUS_HALT_WATER = 3;
	static final int STATUS_HALT_UNBREAKABLE = 4;
	static final int STATUS_DONE = 5;
	static final int STATUS_FULL = 6;
	static final int DATA_COUNT = 7; // lit, litTotal, progress, progressTotal, status, mineY, boosted

	/** Drops are computed as if mined by this tool: diamond tier, unenchanted. */
	private static final ItemStack MINING_TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private int litTimeRemaining;
	private int litTotalTime;
	private boolean litBoosted; // current burn came from a vibranium ingot
	private int mineProgress;
	private int mineY = Integer.MIN_VALUE; // MIN_VALUE = not started yet
	private int status = STATUS_NO_FUEL;
	private int pushCooldown;

	final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(final int dataId) {
			return switch (dataId) {
				case 0 -> ExtractorBlockEntity.this.litTimeRemaining;
				case 1 -> ExtractorBlockEntity.this.litTotalTime;
				case 2 -> ExtractorBlockEntity.this.mineProgress;
				case 3 -> ExtractorBlockEntity.this.currentMineInterval();
				case 4 -> ExtractorBlockEntity.this.status;
				case 5 -> ExtractorBlockEntity.this.mineY == Integer.MIN_VALUE ? ExtractorBlockEntity.this.worldPosition.getY() - 1 : ExtractorBlockEntity.this.mineY;
				case 6 -> ExtractorBlockEntity.this.litBoosted ? 1 : 0;
				default -> 0;
			};
		}

		@Override
		public void set(final int dataId, final int value) {
			switch (dataId) {
				case 0 -> ExtractorBlockEntity.this.litTimeRemaining = value;
				case 1 -> ExtractorBlockEntity.this.litTotalTime = value;
				case 2 -> ExtractorBlockEntity.this.mineProgress = value;
				case 4 -> ExtractorBlockEntity.this.status = value;
				case 5 -> ExtractorBlockEntity.this.mineY = value;
				case 6 -> ExtractorBlockEntity.this.litBoosted = value != 0;
				default -> { }
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public ExtractorBlockEntity(final BlockPos pos, final BlockState state) {
		super(VibraniumMachines.EXTRACTOR_BLOCK_ENTITY, pos, state);
	}

	int currentMineInterval() {
		return VibraniumMachines.MINE_INTERVAL_TICKS / (this.litBoosted ? VibraniumMachines.VIBRANIUM_SPEED_MULT : 1);
	}

	// ---------- persistence ----------
	@Override
	protected void loadAdditional(final ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.litTimeRemaining = input.getIntOr("lit_time_remaining", 0);
		this.litTotalTime = input.getIntOr("lit_total_time", 0);
		this.litBoosted = input.getBooleanOr("lit_boosted", false);
		this.mineProgress = input.getIntOr("mine_progress", 0);
		this.mineY = input.getIntOr("mine_y", Integer.MIN_VALUE);
		this.status = input.getIntOr("status", STATUS_NO_FUEL);
	}

	@Override
	protected void saveAdditional(final ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putInt("lit_time_remaining", this.litTimeRemaining);
		output.putInt("lit_total_time", this.litTotalTime);
		output.putBoolean("lit_boosted", this.litBoosted);
		output.putInt("mine_progress", this.mineProgress);
		output.putInt("mine_y", this.mineY);
		output.putInt("status", this.status);
	}

	// ---------- the mining tick ----------
	public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState state, final ExtractorBlockEntity entity) {
		boolean wasLit = entity.litTimeRemaining > 0;
		if (wasLit) {
			entity.litTimeRemaining--;
		}

		boolean changed = entity.tickMining(level, pos);

		if (entity.pushCooldown++ >= VibraniumMachines.CHEST_PUSH_INTERVAL_TICKS) {
			entity.pushCooldown = 0;
			changed |= entity.pushToAdjacentContainer(level, pos);
		}

		boolean litNow = entity.litTimeRemaining > 0 && entity.status == STATUS_RUNNING;
		if (state.getValue(ExtractorBlock.LIT) != litNow) {
			level.setBlock(pos, state.setValue(ExtractorBlock.LIT, litNow), 3);
			changed = true;
		}
		if (changed) {
			setChanged(level, pos, state);
		}
	}

	/** Advances the mining head; returns true if anything persistent changed. */
	private boolean tickMining(final ServerLevel level, final BlockPos pos) {
		if (this.status == STATUS_HALT_UNBREAKABLE || this.status == STATUS_DONE) {
			return false; // terminal
		}
		// nothing burning and nothing burnable: the head holds position (no free
		// descent through air while unfueled)
		if (this.litTimeRemaining <= 0 && this.burnDuration(level, this.items.get(SLOT_FUEL)) <= 0) {
			boolean statusChanged = this.status != STATUS_NO_FUEL;
			this.status = STATUS_NO_FUEL;
			return statusChanged;
		}
		if (this.mineY == Integer.MIN_VALUE) {
			this.mineY = pos.getY() - 1;
		}

		// find the next target, skipping mined-out/air layers (bounded per tick)
		BlockPos target = null;
		for (int skips = 0; skips < VibraniumMachines.MAX_LAYER_SKIPS_PER_TICK; skips++) {
			if (this.mineY < level.getMinY()) {
				this.status = STATUS_DONE;
				return true;
			}
			LayerScan scan = this.scanLayer(level, pos);
			if (scan.haltStatus != -1) {
				boolean statusChanged = this.status != scan.haltStatus;
				this.status = scan.haltStatus;
				return statusChanged;
			}
			if (scan.target != null) {
				target = scan.target;
				break;
			}
			this.mineY--;
			this.mineProgress = 0;
		}
		if (target == null) {
			return true; // spent this tick descending through air; resume next tick
		}

		// fuel: consume a new item only when there is work to do and nothing burning
		if (this.litTimeRemaining <= 0) {
			ItemStack fuel = this.items.get(SLOT_FUEL);
			int duration = this.burnDuration(level, fuel);
			if (duration > 0) {
				this.litBoosted = fuel.is(VibraniumItems.VIBRANIUM_INGOT);
				this.litTimeRemaining = duration;
				this.litTotalTime = duration;
				Item fuelItem = fuel.getItem();
				fuel.shrink(1);
				if (fuel.isEmpty()) {
					ItemStackTemplate remainder = fuelItem.getCraftingRemainder();
					this.items.set(SLOT_FUEL, remainder != null ? remainder.create() : ItemStack.EMPTY);
				}
			} else {
				boolean statusChanged = this.status != STATUS_NO_FUEL;
				this.status = STATUS_NO_FUEL; // progress freezes until refueled
				return statusChanged;
			}
		}

		this.status = STATUS_RUNNING;
		if (this.mineProgress < this.currentMineInterval()) {
			this.mineProgress++;
		}
		// modest working cues: a grinding note + smoke over the drill site
		if (level.getGameTime() % 60L == 0L) {
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25F, 0.5F);
		}
		if (level.getGameTime() % 20L == 0L) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.0);
		}

		if (this.mineProgress >= this.currentMineInterval()) {
			BlockState targetState = level.getBlockState(target);
			List<ItemStack> drops = Block.getDrops(targetState, level, target, level.getBlockEntity(target), null, MINING_TOOL);
			if (!this.canFitAll(drops)) {
				this.status = STATUS_FULL; // wait (auto-resumes when space frees up)
				return true;
			}
			for (ItemStack drop : drops) {
				this.insertIntoOutput(drop);
			}
			level.destroyBlock(target, false); // plays the break sound + particles
			this.mineProgress = 0;
			return true;
		}
		return true; // progress advanced
	}

	private record LayerScan(@Nullable BlockPos target, int haltStatus) {
	}

	/** Scans the 3x3 layer at mineY: hazards first, else the next minable block. */
	private LayerScan scanLayer(final ServerLevel level, final BlockPos machinePos) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		BlockPos target = null;
		int r = VibraniumMachines.COLUMN_RADIUS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				cursor.set(machinePos.getX() + dx, this.mineY, machinePos.getZ() + dz);
				BlockState state = level.getBlockState(cursor);
				FluidState fluid = state.getFluidState();
				if (!fluid.isEmpty() && fluid.isSource()) {
					return new LayerScan(null, fluid.is(FluidTags.LAVA) ? STATUS_HALT_LAVA : STATUS_HALT_WATER);
				}
				if (state.isAir() || !fluid.isEmpty()) {
					continue; // nothing to mine (flowing fluid is skipped, not mined)
				}
				if (state.getDestroySpeed(level, cursor) < 0.0F) {
					return new LayerScan(null, STATUS_HALT_UNBREAKABLE); // bedrock etc.
				}
				if (target == null) {
					target = cursor.immutable();
				}
			}
		}
		return new LayerScan(target, -1);
	}

	private int burnDuration(final ServerLevel level, final ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(VibraniumItems.VIBRANIUM_INGOT)) {
			return VibraniumMachines.VIBRANIUM_FUEL_BURN_TICKS;
		}
		return level.fuelValues().burnDuration(stack);
	}

	private boolean canFitAll(final List<ItemStack> drops) {
		// simulate insertion on copies so multi-stack drops (e.g. a mined chest)
		// never overfill and get voided
		NonNullList<ItemStack> copy = NonNullList.withSize(OUTPUT_COUNT, ItemStack.EMPTY);
		for (int i = 0; i < OUTPUT_COUNT; i++) {
			copy.set(i, this.items.get(OUTPUT_START + i).copy());
		}
		for (ItemStack drop : drops) {
			ItemStack rest = insertStack(copy, drop.copy());
			if (!rest.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private void insertIntoOutput(final ItemStack stack) {
		NonNullList<ItemStack> view = NonNullList.withSize(OUTPUT_COUNT, ItemStack.EMPTY);
		for (int i = 0; i < OUTPUT_COUNT; i++) {
			view.set(i, this.items.get(OUTPUT_START + i));
		}
		ItemStack rest = insertStack(view, stack);
		for (int i = 0; i < OUTPUT_COUNT; i++) {
			this.items.set(OUTPUT_START + i, view.get(i));
		}
		if (!rest.isEmpty()) { // canFitAll should prevent this; never void items
			Block.popResource(this.level, this.worldPosition.above(), rest);
		}
	}

	/** Merge-then-fill insertion into a slot list; returns what did not fit. */
	private static ItemStack insertStack(final NonNullList<ItemStack> slots, final ItemStack stack) {
		for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
			ItemStack existing = slots.get(i);
			if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
				int room = existing.getMaxStackSize() - existing.getCount();
				int moved = Math.min(room, stack.getCount());
				existing.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
			if (slots.get(i).isEmpty()) {
				slots.set(i, stack.copy());
				stack.setCount(0);
			}
		}
		return stack;
	}

	/** Moves output into the first adjacent chest/barrel (sides or above). */
	private boolean pushToAdjacentContainer(final ServerLevel level, final BlockPos pos) {
		Container targetContainer = null;
		for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
			BlockEntity be = level.getBlockEntity(pos.relative(dir));
			if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
				targetContainer = (Container) be;
				break;
			}
		}
		if (targetContainer == null) {
			return false;
		}
		for (int i = OUTPUT_START; i < CONTAINER_SIZE; i++) {
			ItemStack stack = this.items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			for (int t = 0; t < targetContainer.getContainerSize() && !stack.isEmpty(); t++) {
				ItemStack existing = targetContainer.getItem(t);
				if (existing.isEmpty()) {
					targetContainer.setItem(t, stack.copy());
					stack.setCount(0);
				} else if (ItemStack.isSameItemSameComponents(existing, stack)) {
					int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
					existing.grow(moved);
					stack.shrink(moved);
				}
			}
			this.items.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
			targetContainer.setChanged();
			return true; // one slot per push interval
		}
		return false;
	}

	// ---------- container plumbing ----------
	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.vibranium.extractor");
	}

	@Override
	protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
		return new ExtractorMenu(containerId, inventory, this, this.dataAccess);
	}

	@Override
	public int getContainerSize() {
		return CONTAINER_SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(final NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public boolean canPlaceItem(final int slot, final ItemStack stack) {
		if (slot != SLOT_FUEL) {
			return false; // outputs are machine-filled only
		}
		return stack.is(VibraniumItems.VIBRANIUM_INGOT) || (this.level != null && this.level.fuelValues().isFuel(stack));
	}

	@Override
	public int[] getSlotsForFace(final Direction direction) {
		if (direction == Direction.UP) {
			return SLOTS_FOR_UP;
		}
		return direction == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
	}

	@Override
	public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final @Nullable Direction direction) {
		return this.canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction direction) {
		return slot != SLOT_FUEL;
	}
}
