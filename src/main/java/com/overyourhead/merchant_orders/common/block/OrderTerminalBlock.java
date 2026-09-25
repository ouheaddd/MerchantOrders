package com.overyourhead.merchant_orders.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.merchant_orders.common.block.entity.OrderTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class OrderTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<OrderTerminalBlock> CODEC = simpleCodec(OrderTerminalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    /*
     * The board is a 2x2 multiblock, but the visible model is rendered only by
     * the lower-left anchor. Each occupied cell gets a thin 3px collision /
     * selection slice so the entire 32x32 board can be clicked normally.
     */
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static final ThreadLocal<Boolean> REMOVING_STRUCTURE = ThreadLocal.withInitial(() -> false);

    public OrderTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isAnchor(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos anchor = context.getClickedPos();
        Direction rightDirection = rightDirection(facing);
        BlockPos right = anchor.relative(rightDirection);
        BlockPos upperLeft = anchor.above();
        BlockPos upperRight = right.above();
        Level level = context.getLevel();

        if (!level.getBlockState(right).canBeReplaced()
                || !level.getBlockState(upperLeft).canBeReplaced()
                || !level.getBlockState(upperRight).canBeReplaced()) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(RIGHT, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        Direction facing = state.getValue(FACING);
        BlockPos right = pos.relative(rightDirection(facing));

        level.setBlock(right,
                state.setValue(HALF, DoubleBlockHalf.LOWER).setValue(RIGHT, true),
                Block.UPDATE_ALL);
        level.setBlock(pos.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(RIGHT, false),
                Block.UPDATE_ALL);
        level.setBlock(right.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(RIGHT, true),
                Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, RIGHT);
    }

    /**
     * Structure templates/jigsaw pieces rotate block positions and then ask each
     * block to rotate its own state. Because this block owns a custom FACING
     * property (it does not extend HorizontalDirectionalBlock), the default
     * Block implementation would leave FACING unchanged. That breaks the 2x2
     * layout after a village piece is rotated: RIGHT/anchor calculations point
     * to different cells than the cells that were actually rotated.
     */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * Mirrors reverse handedness, so in addition to mirroring FACING the logical
     * left/right half must be swapped. Jigsaw normally rotates our village piece,
     * but keeping mirror support correct makes the template safe for other uses.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state
                .setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(RIGHT, !state.getValue(RIGHT));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isAnchor(state) ? new OrderTerminalBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockPos anchor = anchorPos(pos, state);
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        return blockEntity instanceof OrderTerminalBlockEntity terminal ? terminal : null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !REMOVING_STRUCTURE.get()) {
            BlockPos anchor = anchorPos(pos, state);

            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(anchor);
                if (blockEntity instanceof OrderTerminalBlockEntity terminal) {
                    terminal.dropPendingDelivery();
                }

                REMOVING_STRUCTURE.set(true);
                try {
                    for (BlockPos partPos : structurePositions(anchor, state.getValue(FACING))) {
                        if (partPos.equals(pos)) {
                            continue;
                        }
                        BlockState partState = level.getBlockState(partPos);
                        if (partState.is(this)) {
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                } finally {
                    REMOVING_STRUCTURE.remove();
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !isAnchor(state)) {
            return null;
        }
        return createTickerHelper(type, com.overyourhead.merchant_orders.core.registry.MOBlockEntities.ORDER_TERMINAL.get(),
                OrderTerminalBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = getMenuProvider(state, level, pos);
            if (provider != null) {
                serverPlayer.openMenu(provider);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean isAnchor(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !state.getValue(RIGHT);
    }

    /** Right-hand cell when looking at the front of the board. */
    private static Direction rightDirection(Direction facing) {
        return facing.getCounterClockWise();
    }

    private static BlockPos anchorPos(BlockPos pos, BlockState state) {
        BlockPos anchor = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (state.getValue(RIGHT)) {
            anchor = anchor.relative(rightDirection(state.getValue(FACING)).getOpposite());
        }
        return anchor;
    }

    private static BlockPos[] structurePositions(BlockPos anchor, Direction facing) {
        BlockPos right = anchor.relative(rightDirection(facing));
        return new BlockPos[]{anchor, right, anchor.above(), right.above()};
    }
}
