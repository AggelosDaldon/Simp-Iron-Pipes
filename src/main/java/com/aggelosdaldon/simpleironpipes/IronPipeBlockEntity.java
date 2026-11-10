package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IronPipeBlockEntity extends BlockEntity {
    private final ItemStackHandler contents = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> contents);
    private final Map<Direction, ItemStack> filters = new EnumMap<>(Direction.class);
    @Nullable private Direction incoming;
    private int timer;
    private int cursor;

    public IronPipeBlockEntity(BlockPos pos, BlockState state) {
        super(IronPipes.PIPE_ENTITY.get(), pos, state);
    }
    public Container drops() {
        SimpleContainer result = new SimpleContainer(1);
        result.setItem(0, contents.getStackInSlot(0).copy());
        return result;
    }
    public void setFilter(Direction direction, ItemStack stack) {
        if (!isRouting()) return;
        if (stack.isEmpty()) filters.remove(direction);
        else filters.put(direction, stack.copyWithCount(1));
        setChanged();
    }
    public String describeFilters() {
        if (filters.isEmpty()) return "No routes configured. Sneak-use an item on a junction face to assign it.";
        StringBuilder s = new StringBuilder("Iron routing junction: ");
        for (Direction d : Direction.values()) {
            ItemStack item = filters.get(d);
            if (item != null && !item.isEmpty()) s.append(d.getName()).append("=").append(item.getHoverName().getString()).append("; ");
        }
        return s.toString();
    }
    public ItemStack getFilter(Direction side) {
        ItemStack filter = filters.get(side);
        return filter == null ? ItemStack.EMPTY : filter.copy();
    }
    public boolean isExtractor() {
        return getBlockState().getBlock() instanceof IronPipeBlock b && b.isExtractor();
    }
    public boolean isOutlet() {
        return getBlockState().getBlock() instanceof IronPipeBlock b && b.isOutlet();
    }
    public Direction configuredFacing() { return getBlockState().getValue(IronPipeBlock.FACING); }
    public boolean isRouting() {
        return getBlockState().getBlock() instanceof IronPipeBlock block && block.isJunction();
    }
    public boolean receive(ItemStack item, Direction from) {
        if (!contents.getStackInSlot(0).isEmpty() || item.isEmpty()) return false;
        contents.setStackInSlot(0, item.copyWithCount(1));
        incoming = from;
        setChanged();
        return true;
    }
    private void playTransportSound(SoundEventKind kind) {
        if (level == null || level.isClientSide) return;
        switch (kind) {
            case IN -> level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.26F, 0.78F + level.random.nextFloat() * 0.18F);
            case OUT -> level.playSound(null, worldPosition, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 0.22F, 1.35F + level.random.nextFloat() * 0.16F);
            case TURN -> level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.14F, 1.7F + level.random.nextFloat() * 0.1F);
        }
    }
    private enum SoundEventKind { IN, OUT, TURN }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronPipeBlockEntity be) {
        if (level.isClientSide || level.hasNeighborSignal(pos) || ++be.timer % 8 != 0) return;
        if (be.contents.getStackInSlot(0).isEmpty()) {
            if (be.isExtractor()) be.tryPull(level, pos);
        } else if (be.isOutlet()) {
            be.eject(level, pos);
        } else {
            be.tryPush(level, pos);
        }
    }
    private boolean allows(Direction direction, ItemStack item) {
        if (!isRouting() || filters.isEmpty()) return true;
        ItemStack assigned = filters.get(direction);
        if (assigned != null) return ItemStack.isSameItemSameTags(assigned, item);
        for (ItemStack filter : filters.values())
            if (ItemStack.isSameItemSameTags(filter, item)) return false;
        return true;
    }

    /*
     * Look ahead through the pipe graph rather than blindly forwarding to the
     * next pipe. This avoids circulating items around closed loops and gives
     * junction filters an opportunity to choose a destination.
     * Bounded BFS prevents unbounded work on very large networks.
     */
    private record RouteStep(BlockPos pos, Direction first, int depth) { }

    @Nullable
    private Direction routeToDestination(Level level, BlockPos start, ItemStack stack, @Nullable Direction blocked) {
        ArrayDeque<RouteStep> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(new RouteStep(start, null, 0));
        visited.add(start);
        while (!queue.isEmpty() && visited.size() <= 128) {
            RouteStep step = queue.poll();
            BlockEntity node = level.getBlockEntity(step.pos());
            if (!(node instanceof IronPipeBlockEntity pipe)) continue;
            for (Direction side : Direction.values()) {
                if (step.depth() == 0 && side == blocked) continue;
                if (!pipe.allows(side, stack)) continue;
                BlockPos next = step.pos().relative(side);
                if (visited.contains(next)) continue;
                BlockEntity target = level.getBlockEntity(next);
                Direction first = step.first() == null ? side : step.first();
                if (target instanceof IronPipeBlockEntity other) {
                    if (!other.contents.getStackInSlot(0).isEmpty()) continue;
                    if (other.isOutlet()) return first;
                    if (visited.size() < 128) {
                        visited.add(next);
                        queue.add(new RouteStep(next, first, step.depth() + 1));
                    }
                } else if (target != null) {
                    IItemHandler handler = target.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
                    if (handler == null) continue;
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        if (handler.insertItem(slot, stack.copyWithCount(1), true).isEmpty()) return first;
                    }
                }
            }
        }
        return null;
    }

    private void tryPush(Level level, BlockPos pos) {
        ItemStack item = contents.getStackInSlot(0);
        if (item.isEmpty()) return;
        Direction next = routeToDestination(level, pos, item, incoming);
        if (next == null) return;
        BlockEntity target = level.getBlockEntity(pos.relative(next));
        if (target instanceof IronPipeBlockEntity pipe) {
            if (pipe.receive(item, next.getOpposite())) {
                playTransportSound(SoundEventKind.TURN);
                completeMove();
            }
        } else if (target != null) {
            IItemHandler handler = target.getCapability(ForgeCapabilities.ITEM_HANDLER, next.getOpposite()).orElse(null);
            if (handler == null) return;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (handler.insertItem(slot, item.copyWithCount(1), false).isEmpty()) {
                    playTransportSound(SoundEventKind.OUT);
                    completeMove();
                    return;
                }
            }
        }
    }

    private void completeMove() {
        contents.setStackInSlot(0, ItemStack.EMPTY);
        incoming = null;
        setChanged();
    }

    private void tryPull(Level level, BlockPos pos) {
        Direction intake = configuredFacing();
        BlockEntity source = level.getBlockEntity(pos.relative(intake));
        if (source == null || source instanceof IronPipeBlockEntity) return;
        IItemHandler handler = source.getCapability(ForgeCapabilities.ITEM_HANDLER, intake.getOpposite()).orElse(null);
        if (handler == null) return;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack test = handler.extractItem(slot, 1, true);
            if (test.isEmpty() || routeToDestination(level, pos, test, intake) == null) continue;
            ItemStack extracted = handler.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                receive(extracted, intake);
                playTransportSound(SoundEventKind.IN);
                return;
            }
        }
    }

    private void eject(Level level, BlockPos pos) {
        ItemStack item = contents.getStackInSlot(0);
        if (item.isEmpty()) return;
        Direction side = configuredFacing();
        double x = pos.getX() + .5 + side.getStepX() * .7;
        double y = pos.getY() + .5 + side.getStepY() * .7;
        double z = pos.getZ() + .5 + side.getStepZ() * .7;
        ItemEntity dropped = new ItemEntity(level, x, y, z, item.copy());
        dropped.setDeltaMovement(side.getStepX() * .14, side.getStepY() * .14 + .06, side.getStepZ() * .14);
        if (level.addFreshEntity(dropped)) {
            playTransportSound(SoundEventKind.OUT);
            completeMove();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", contents.serializeNBT());
        tag.putInt("cursor", cursor);
        if (incoming != null) tag.putInt("incoming", incoming.get3DDataValue());
        ListTag stored = new ListTag();
        for (Map.Entry<Direction, ItemStack> entry : filters.entrySet()) {
            CompoundTag filter = new CompoundTag();
            filter.putInt("side", entry.getKey().get3DDataValue());
            filter.put("stack", entry.getValue().save(new CompoundTag()));
            stored.add(filter);
        }
        tag.put("routes", stored);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        contents.deserializeNBT(tag.getCompound("inventory"));
        cursor = tag.getInt("cursor");
        incoming = tag.contains("incoming", Tag.TAG_INT) ? Direction.from3DDataValue(tag.getInt("incoming")) : null;
        filters.clear();
        ListTag stored = tag.getList("routes", Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag f = stored.getCompound(i);
            ItemStack item = ItemStack.of(f.getCompound("stack"));
            if (!item.isEmpty()) filters.put(Direction.from3DDataValue(f.getInt("side")), item.copyWithCount(1));
        }
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); itemCapability = LazyOptional.of(() -> contents); }
}
