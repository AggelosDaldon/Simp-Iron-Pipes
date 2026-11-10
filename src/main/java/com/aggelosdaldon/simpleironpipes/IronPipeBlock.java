package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class IronPipeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FRONT_CONNECTED = BooleanProperty.create("front_connected");
    public static final BooleanProperty BACK_CONNECTED = BooleanProperty.create("back_connected");
    public static final BooleanProperty SMOOTH = BooleanProperty.create("smooth");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape FITTING_SHAPE = Block.box(2.5, 2.5, 2.5, 13.5, 13.5, 13.5);
    private final boolean fitting;

    public IronPipeBlock(boolean fitting, Properties properties) {
        super(properties);
        this.fitting = fitting;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FRONT_CONNECTED, false)
                .setValue(BACK_CONNECTED, false)
                .setValue(SMOOTH, false)
                .setValue(POWERED, false));
    }

    public boolean isFitting() { return fitting; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, FRONT_CONNECTED, BACK_CONNECTED, SMOOTH, POWERED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return updateConnections(defaultBlockState().setValue(FACING, ctx.getClickedFace())
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos())),
                ctx.getLevel(), ctx.getClickedPos());
    }
    private static boolean isPipe(BlockState neighbor) {
        return neighbor.getBlock() instanceof IronPipeBlock b && !b.isFitting();
    }
    private static boolean isFittingNeighbor(BlockState neighbor) {
        return neighbor.getBlock() instanceof IronPipeBlock b && b.isFitting();
    }
    private BlockState updateConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        if (fitting) return state;
        Direction facing = state.getValue(FACING);
        BlockState front = level.getBlockState(pos.relative(facing));
        BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
        boolean frontExtension = isFittingNeighbor(front) ||
                (isPipe(front) && front.getValue(FACING).getAxis() != facing.getAxis());
        boolean backExtension = isFittingNeighbor(back) ||
                (isPipe(back) && back.getValue(FACING).getAxis() != facing.getAxis());
        boolean smooth = isPipe(front) && front.getValue(FACING) == facing;
        return state.setValue(FRONT_CONNECTED, frontExtension)
                .setValue(BACK_CONNECTED, backExtension)
                .setValue(SMOOTH, smooth);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState other,
                                              LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return updateConnections(state, level, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                           Block changed, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED))
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (fitting) return FITTING_SHAPE;
        return switch (state.getValue(FACING).getAxis()) {
            case X -> Block.box(0, 4, 4, 16, 12, 12);
            case Y -> Block.box(4, 0, 4, 12, 16, 12);
            case Z -> Block.box(4, 4, 0, 12, 12, 16);
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronPipeBlockEntity(pos, state);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, IronPipes.PIPE_ENTITY.get(),
                IronPipeBlockEntity::serverTick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IronPipeBlockEntity inventory) {
            if (!level.isClientSide) player.openMenu(inventory);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
    @Override public void onRemove(BlockState oldState, Level level, BlockPos pos,
                                   BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof IronPipeBlockEntity inv)
                Containers.dropContents(level, pos, inv);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }
}
