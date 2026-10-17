package com.overyourhead.merchant_orders.common.delivery;

import com.overyourhead.merchant_orders.common.block.entity.OrderSackBlockEntity;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import com.overyourhead.merchant_orders.core.registry.MOSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DeliveryUtil {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private DeliveryUtil() {
    }

    public static void playIncomingSound(ServerLevel level, BlockPos origin) {
        level.playSound(null, origin, MOSoundEvents.DELIVERY_INCOMING.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 1.0F);
    }

    public static boolean placeSack(ServerLevel level, BlockPos origin, ItemStack sack) {
        BlockPos target = findPosition(level, origin, 3);
        if (target == null) {
            return level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level,
                    origin.getX() + 0.5,
                    origin.getY() + 1.1,
                    origin.getZ() + 0.5,
                    sack.copy()
            ));
        }

        BlockState state = MOBlocks.ORDER_SACK.get().defaultBlockState();
        if (!level.setBlock(target, state, 3)) {
            return false;
        }
        if (!(level.getBlockEntity(target) instanceof OrderSackBlockEntity blockEntity)) {
            // Do not consume a delivery unless the placed sack can actually store it.
            level.removeBlock(target, false);
            return false;
        }
        blockEntity.applyComponentsFromItemStack(sack);
        blockEntity.setChanged();
        level.playSound(null, target, SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.9F, 1.0F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                target.getX() + 0.5, target.getY() + 0.8, target.getZ() + 0.5,
                8, 0.25, 0.25, 0.25, 0.0);
        return true;
    }

    private static BlockPos findPosition(ServerLevel level, BlockPos origin, int radius) {
        // Prefer the board's base level; only try one block higher when it is full.
        for (int dy = 0; dy <= 1; dy++) {
            for (int r = 1; r <= radius; r++) {
                for (Direction direction : HORIZONTAL) {
                    BlockPos candidate = origin.relative(direction, r).above(dy);
                    if (isValid(level, candidate)) {
                        return candidate;
                    }
                }
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) {
                            continue;
                        }
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (isValid(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isValid(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())
                || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) {
            return false;
        }
        return level.getBlockState(pos).canBeReplaced()
                && level.getBlockState(pos.above()).canBeReplaced()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
