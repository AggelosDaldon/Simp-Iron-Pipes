package com.aggelosdaldon.simpleironpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class IronPipeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public enum Mode { PIPE, JUNCTION, EXTRACTOR, OUTLET }
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_N = Block.box(5, 5, 0, 11, 11, 8);
    private static final VoxelShape ARM_S = Block.box(5, 5, 8, 11, 11, 16);
    private static final VoxelShape ARM_E = Block.box(8, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_W = Block.box(0, 5, 5, 8, 11, 11);
    private static final VoxelShape ARM_U = Block.box(5, 8, 5, 11, 16, 11);
    private static final VoxelShape ARM_D = Block.box(5, 0, 5, 11, 8, 11);
    private final Mode mode;

    public IronPipeBlock(boolean junction, Properties properties) {
        this(junction ? Mode.JUNCTION : Mode.PIPE, properties);
    }
    public IronPipeBlock(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }
    public boolean isJunction() { return mode == Mode.JUNCTION; }
    public boolean isExtractor() { return mode == Mode.EXTRACTOR; }
    public boolean isOutlet() { return mode == Mode.OUTLET; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }
    private static BooleanProperty property(Direction direction) {
        return switch(direction) {
            case NORTH -> NORTH; case SOUTH -> SOUTH; case EAST -> EAST;
            case WEST -> WEST; case UP -> UP; case DOWN -> DOWN;
        };
    }
    private static boolean connects(LevelAccessor level, BlockPos pos, Direction side) {
        BlockPos other = pos.relative(side);
        BlockState neighbor = level.getBlockState(other);
        if (neighbor.getBlock() instanceof IronPipeBlock) return true;
        // Vanilla containers and other block entities can be network endpoints.
        return level.getBlockEntity(other) != null;
    }
    private static BlockState refresh(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), connects(level, pos, direction));
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return refresh(defaultBlockState().setValue(FACING,
                mode == Mode.EXTRACTOR ? context.getClickedFace().getOpposite() : context.getClickedFace()),
                context.getLevel(), context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState otherState,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return state.setValue(property(side), connects(level, pos, side));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape s = isJunction() ? Block.box(4,4,4,12,12,12) : CORE;
        if (state.getValue(NORTH)) s = Shapes.or(s, ARM_N);
        if (state.getValue(SOUTH)) s = Shapes.or(s, ARM_S);
        if (state.getValue(EAST)) s = Shapes.or(s, ARM_E);
        if (state.getValue(WEST)) s = Shapes.or(s, ARM_W);
        if (state.getValue(UP)) s = Shapes.or(s, ARM_U);
        if (state.getValue(DOWN)) s = Shapes.or(s, ARM_D);
        return s;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new IronPipeBlockEntity(pos, state); }
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, IronPipes.PIPE_ENTITY.get(), IronPipeBlockEntity::serverTick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        if (!isJunction()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (windowId, inventory, ignored) -> new RoutingMenu(windowId, inventory, pos),
                    Component.translatable("screen.simple_iron_pipes.routing")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IronPipeBlockEntity pipe) {
                net.minecraft.world.Containers.dropContents(level, pos, pipe.drops());
            }
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }
}
