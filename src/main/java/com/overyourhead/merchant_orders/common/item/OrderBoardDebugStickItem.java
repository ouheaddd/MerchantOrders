package com.overyourhead.merchant_orders.common.item;

import com.overyourhead.merchant_orders.common.block.entity.OrderTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Developer-only helper for checking village worldgen in-game.
 * Right click to find generated Order Board anchors within 64 blocks.
 */
public final class OrderBoardDebugStickItem extends Item {
    private static final int SEARCH_RADIUS = 64;
    private static final int SEARCH_RADIUS_SQR = SEARCH_RADIUS * SEARCH_RADIUS;

    public OrderBoardDebugStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos origin = serverPlayer.blockPosition();
        List<BlockPos> found = findOrderBoards(serverLevel, origin);
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        if (found.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.merchant_orders.debug_board.none", SEARCH_RADIUS));
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("message.merchant_orders.debug_board.found", found.size(), SEARCH_RADIUS));
            for (BlockPos pos : found) {
                double distance = Math.sqrt(pos.distSqr(origin));
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.merchant_orders.debug_board.position",
                        pos.getX(), pos.getY(), pos.getZ(), String.format(Locale.ROOT, "%.1f", distance)
                ));
                spawnMarker(serverLevel, serverPlayer, pos);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private static List<BlockPos> findOrderBoards(ServerLevel level, BlockPos origin) {
        List<BlockPos> found = new ArrayList<>();
        int minChunkX = (origin.getX() - SEARCH_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + SEARCH_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - SEARCH_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + SEARCH_RADIUS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof OrderTerminalBlockEntity)) {
                        continue;
                    }

                    BlockPos pos = entry.getKey();
                    if (pos.distSqr(origin) <= SEARCH_RADIUS_SQR) {
                        found.add(pos.immutable());
                    }
                }
            }
        }

        return found;
    }

    private static void spawnMarker(ServerLevel level, ServerPlayer player, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        // Local burst at the exact anchor.
        level.sendParticles(player, ParticleTypes.END_ROD, true,
                x, y + 0.75D, z,
                40, 0.7D, 1.0D, 0.7D, 0.015D);

        // Long-distance forced beam. Particles themselves still obey normal
        // depth rendering, but the beam rises well above village roofs so a
        // buried/indoor board remains easy to locate while flying around.
        for (double dy = 0.0D; dy <= 22.0D; dy += 0.75D) {
            level.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, true,
                    x, y + dy, z,
                    2, 0.08D, 0.08D, 0.08D, 0.0D);
        }
    }
}
