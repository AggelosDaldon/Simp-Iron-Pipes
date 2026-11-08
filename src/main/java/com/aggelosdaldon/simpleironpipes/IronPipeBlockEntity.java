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
    public static void serverTick(Level level, BlockPos pos, BlockState state, IronPipeBlockEntity be) {
        if (level.isClientSide || ++be.timer % 8 != 0) return;
        if (be.contents.getStackInSlot(0).isEmpty()) be.tryPull(level, pos);
        else be.tryPush(level, pos);
    }
    private boolean allows(Direction direction, ItemStack item) {
        if (!isRouting() || filters.isEmpty()) return true;
        ItemStack assigned = filters.get(direction);
        if (assigned != null) return ItemStack.isSameItemSameTags(assigned, item);
        // Unassigned sides are fallback routes only if no specifically matched filter exists.
        for (ItemStack filter : filters.values())
            if (ItemStack.isSameItemSameTags(filter, item)) return false;
        return true;
    }
    private void tryPush(Level level, BlockPos pos) {
        ItemStack item = contents.getStackInSlot(0);
        if (item.isEmpty()) return;
        Direction[] sides = Direction.values();
        for (int offset = 0; offset < sides.length; offset++) {
            Direction direction = sides[(cursor + offset) % sides.length];
            if (direction == incoming || !allows(direction, item)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) continue;
            if (neighbor instanceof IronPipeBlockEntity pipe) {
                if (pipe.receive(item, direction.getOpposite())) { completeMove((cursor + offset + 1) % sides.length); return; }
                continue;
            }
            LazyOptional<IItemHandler> optional = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            IItemHandler handler = optional.orElse(null);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack remainder = handler.insertItem(slot, item.copyWithCount(1), false);
                if (remainder.isEmpty()) { completeMove((cursor + offset + 1) % sides.length); return; }
            }
        }
    }
    private void completeMove(int nextCursor) {
        contents.setStackInSlot(0, ItemStack.EMPTY);
        incoming = null;
        cursor = nextCursor;
        setChanged();
    }
    private void tryPull(Level level, BlockPos pos) {
        Direction[] sides = Direction.values();
        for (int offset = 0; offset < sides.length; offset++) {
            Direction side = sides[(cursor + offset) % sides.length];
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            if (neighbor == null || neighbor instanceof IronPipeBlockEntity) continue;
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack test = handler.extractItem(slot, 1, true);
                if (test.isEmpty()) continue;
                // Pull only if at least one forward direction can accept this item.
                if (!hasPotentialOutput(level, pos, side, test)) continue;
                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (!extracted.isEmpty()) {
                    receive(extracted, side);
                    cursor = (cursor + offset + 1) % sides.length;
                    return;
                }
            }
        }
    }
    private boolean hasPotentialOutput(Level level, BlockPos pos, Direction source, ItemStack item) {
        for (Direction target : Direction.values()) {
            if (target != source && allows(target, item) && level.getBlockEntity(pos.relative(target)) != null) return true;
        }
        return false;
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
