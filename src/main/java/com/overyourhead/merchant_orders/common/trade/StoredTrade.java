package com.overyourhead.merchant_orders.common.trade;

import net.minecraft.world.item.ItemStack;

public record StoredTrade(ItemStack costA, ItemStack costB, ItemStack result, int maxUses, int xp) {
    public StoredTrade {
        costA = costA.copy();
        costB = costB.copy();
        result = result.copy();
        maxUses = Math.max(1, maxUses);
        xp = Math.max(1, Math.min(20, xp));
    }

    public boolean isValid() {
        return !costA.isEmpty() && !result.isEmpty();
    }

    public boolean satisfiedBy(ItemStack first, ItemStack second) {
        return matchesCost(first, costA) && (costB.isEmpty() || matchesCost(second, costB));
    }

    private static boolean matchesCost(ItemStack offered, ItemStack required) {
        return !offered.isEmpty()
                && offered.getCount() >= required.getCount()
                && ItemStack.isSameItemSameComponents(offered, required);
    }

    public boolean sameDefinition(StoredTrade other) {
        return sameStack(costA, other.costA)
                && sameStack(costB, other.costB)
                && sameStack(result, other.result);
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        }
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }
}
