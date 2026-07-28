package com.overyourhead.merchant_orders.common.sale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerSaleData {
    private static final String ROOT = "MerchantOrdersSale";

    private PlayerSaleData() {
    }

    public static long lastSale(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT).getLong("LastSale");
    }

    public static void setLastSale(ServerPlayer player, long gameTime) {
        CompoundTag tag = player.getPersistentData().getCompound(ROOT);
        tag.putLong("LastSale", gameTime);
        player.getPersistentData().put(ROOT, tag);
    }

    public static long remainingCooldown(ServerPlayer player, long gameTime, long cooldownTicks) {
        long last = lastSale(player);
        if (last <= 0L) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (gameTime - last));
    }

    public static void copy(ServerPlayer original, ServerPlayer clone) {
        if (original.getPersistentData().contains(ROOT)) {
            clone.getPersistentData().put(ROOT, original.getPersistentData().getCompound(ROOT).copy());
        }
    }
}
