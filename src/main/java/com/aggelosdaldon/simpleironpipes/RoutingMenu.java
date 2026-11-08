package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RoutingMenu extends AbstractContainerMenu {
    public static final Direction[] SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.UP, Direction.DOWN
    };
    private final Container filters = new SimpleContainer(6);
    private final BlockPos position;
    private final Inventory inventory;

    public RoutingMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, data.readBlockPos());
    }

    public RoutingMenu(int id, Inventory inv, BlockPos pos) {
        super(IronPipes.ROUTING_MENU.get(), id);
        this.position = pos;
        this.inventory = inv;
        BlockEntity blockEntity = inv.player.level().getBlockEntity(pos);
        if (blockEntity instanceof IronPipeBlockEntity pipe && pipe.isRouting()) {
            for (int i = 0; i < SIDES.length; i++) filters.setItem(i, pipe.getFilter(SIDES[i]));
        }
        for (int i = 0; i < 6; i++) {
            // Ghost slots: placing a filter never consumes the item.
            addSlot(new Slot(filters, i, 44 + (i % 3) * 36, 33 + (i / 3) * 31) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + 18 * col, 126 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + 18 * col, 184));
    }

    public BlockPos getPosition() { return position; }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < 6) {
            // Right-click removes a filter; left-click copies the cursor item.
            ItemStack newFilter = button == 1 ? ItemStack.EMPTY : getCarried();
            if (!player.level().isClientSide) {
                BlockEntity be = player.level().getBlockEntity(position);
                if (be instanceof IronPipeBlockEntity pipe && pipe.isRouting()) {
                    ItemStack value = newFilter.isEmpty() ? ItemStack.EMPTY : newFilter.copyWithCount(1);
                    pipe.setFilter(SIDES[slotId], value);
                    filters.setItem(slotId, value);
                    broadcastChanges();
                }
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 6 || slotId >= slots.size()) return ItemStack.EMPTY;
        // Shift transfer is intentionally disabled to avoid putting real inventory items into ghost slots.
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5) <= 64
                && player.level().getBlockState(position).is(IronPipes.ROUTING_JUNCTION.get());
    }
}
