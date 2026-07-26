package com.overyourhead.merchant_orders.common.inventory;

import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Central insertion rules for portable order sacks.
 *
 * Sacks are general-purpose 36-slot containers, but nested sacks and shulker
 * boxes are rejected to avoid recursive container trees and duplication bugs.
 */
public final class SackRules {
    private SackRules() {
    }

    public static boolean canStore(ItemStack stack) {
        if (stack.isEmpty() || stack.is(MOBlocks.ORDER_SACK_ITEM.get())) {
            return false;
        }
        return !(stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock);
    }
}
