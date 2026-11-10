package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IronPipeBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    private int cooldown;

    public IronPipeBlockEntity(BlockPos pos, BlockState state) {
        super(IronPipes.PIPE_ENTITY.get(), pos, state);
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { this.items = stacks; }
    @Override public int getContainerSize() { return 5; }
    @Override protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new HopperMenu(id, inv, this);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
        cooldown = tag.getInt("cooldown");
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);
        tag.putInt("cooldown", cooldown);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                               @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronPipeBlockEntity entity) {
        if (entity.cooldown > 0) { entity.cooldown--; return; }
        if (!(state.getBlock() instanceof IronPipeBlock block)) return;
        boolean moved;
        if (block.isFitting()) moved = entity.tickFitting(level, pos, state);
        else moved = entity.tickPipe(level, pos, state);
        if (moved) { entity.cooldown = 8; entity.setChanged(); }
    }
    private boolean tickPipe(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(IronPipeBlock.FACING);
        boolean moved = moveTo(level, pos.relative(facing), facing);
        if (!moved) moved = dispenseToAir(level, pos, facing);
        if (!state.getValue(IronPipeBlock.POWERED))
            moved |= moveFrom(level, pos.relative(facing.getOpposite()), facing);
        return moved;
    }
    private boolean tickFitting(Level level, BlockPos pos, BlockState state) {
        boolean moved = false;
        Direction[] sides = Direction.values();
        // Rotate the first side each tick to avoid permanently favoring one connection.
        int start = level.random.nextInt(sides.length);
        for (int i = 0; i < sides.length; i++) {
            Direction side = sides[(start + i) % sides.length];
            BlockState neighbor = level.getBlockState(pos.relative(side));
            if (!(neighbor.getBlock() instanceof IronPipeBlock p) || p.isFitting()) continue;
            if (neighbor.getValue(IronPipeBlock.FACING) == side.getOpposite()) {
                moved |= moveFrom(level, pos.relative(side), side.getOpposite());
            } else if (!state.getValue(IronPipeBlock.POWERED)
                    && neighbor.getValue(IronPipeBlock.FACING) == side) {
                moved |= moveTo(level, pos.relative(side), side);
            }
        }
        return moved;
    }
    private boolean moveTo(Level level, BlockPos targetPos, Direction outgoing) {
        if (!level.hasChunkAt(targetPos)) return false;
        var target = level.getBlockEntity(targetPos);
        if (target == null) return false;
        IItemHandler destination = target.getCapability(ForgeCapabilities.ITEM_HANDLER,
                outgoing.getOpposite()).orElse(null);
        if (destination == null) return false;
        return moveOne(level, worldPosition, itemHandler.orElse(null), destination, false);
    }
    private boolean moveFrom(Level level, BlockPos sourcePos, Direction forward) {
        if (!level.hasChunkAt(sourcePos)) return false;
        var source = level.getBlockEntity(sourcePos);
        if (source == null) return false;
        IItemHandler from = source.getCapability(ForgeCapabilities.ITEM_HANDLER, forward).orElse(null);
        if (from == null) return false;
        return moveOne(level, sourcePos, from, itemHandler.orElse(null), true);
    }
    private boolean moveOne(Level level, BlockPos sourcePos, IItemHandler source,
                            IItemHandler target, boolean intake) {
        if (source == null || target == null || source == target) return false;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack preview = source.extractItem(slot, 1, true);
            if (preview.isEmpty()) continue;
            if (!ItemHandlerHelper.insertItem(target, preview, true).isEmpty()) continue;
            ItemStack item = source.extractItem(slot, 1, false);
            if (item.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItem(target, item, false);
            if (!remainder.isEmpty()) {
                ItemStack returned = ItemHandlerHelper.insertItem(source, remainder, false);
                if (!returned.isEmpty()) level.addFreshEntity(new ItemEntity(level,
                        sourcePos.getX() + .5, sourcePos.getY() + .5, sourcePos.getZ() + .5, returned));
            }
            level.playSound(null, worldPosition, intake ? IronPipes.ITEM_IN_SOUND.get() : IronPipes.ITEM_OUT_SOUND.get(),
                    SoundSource.BLOCKS, .35F, .95F + level.random.nextFloat() * .1F);
            return true;
        }
        return false;
    }
    private boolean dispenseToAir(Level level, BlockPos pos, Direction facing) {
        BlockPos output = pos.relative(facing);
        if (!level.getBlockState(output).isAir() && !level.getBlockState(output).getFluidState().isSource()) return false;
        for (int slot = 0; slot < 5; slot++) {
            ItemStack item = getItem(slot);
            if (item.isEmpty()) continue;
            ItemStack released = removeItem(slot, 1);
            ItemEntity dropped = new ItemEntity(level,
                    pos.getX() + .5 + facing.getStepX() * .6,
                    pos.getY() + .5 + facing.getStepY() * .6,
                    pos.getZ() + .5 + facing.getStepZ() * .6,
                    released);
            dropped.setDeltaMovement(facing.getStepX() * .2,
                    facing.getStepY() * .2, facing.getStepZ() * .2);
            if (level.addFreshEntity(dropped)) {
                level.playSound(null, pos, IronPipes.ITEM_LAUNCH_SOUND.get(), SoundSource.BLOCKS, .35F, 1F);
                return true;
            }
            setItem(slot, released);
            return false;
        }
        return false;
    }
}
