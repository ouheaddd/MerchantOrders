package com.overyourhead.merchant_orders.common;

public final class MOConstants {
    public static final int TIER_COUNT = 5;
    public static final int OFFERS_PER_TIER = 20;
    public static final int BASKET_SIZE = 36;
    public static final int[] XP_THRESHOLDS = {0, 100, 700, 1500, 2500};

    private MOConstants() {
    }

    public static int unlockedTier(int xp) {
        int result = 0;
        for (int i = 1; i < XP_THRESHOLDS.length; i++) {
            if (xp >= XP_THRESHOLDS[i]) {
                result = i;
            }
        }
        return result;
    }
}
