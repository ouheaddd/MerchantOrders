package com.overyourhead.merchant_orders.common.config;

import com.overyourhead.merchant_orders.common.MOConstants;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-authoritative settings for the prototype. Values that change the fixed
 * network layout (basket size and the hard 20-offer sync capacity) intentionally
 * remain constants for v1.0.0.
 */
public final class MOConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TERMINAL_PRICE = BUILDER
            .comment("Emerald cost of the order terminal from a wandering trader.")
            .defineInRange("terminalPrice", 24, 1, 64);

    public static final ModConfigSpec.IntValue TERMINAL_MAX_USES = BUILDER
            .comment("Maximum purchases of the terminal offer per wandering trader.")
            .defineInRange("terminalMaxUses", 1, 1, 16);

    public static final ModConfigSpec.IntValue OFFERS_PER_TIER = BUILDER
            .comment("Random offers shown per unlocked tier. The prototype supports up to 20.")
            .defineInRange("offersPerTier", 20, 1, MOConstants.OFFERS_PER_TIER);

    public static final ModConfigSpec.IntValue CATALOG_REFRESH_DAYS = BUILDER
            .comment("Minecraft days before a player's deterministic catalog changes.")
            .defineInRange("catalogRefreshDays", 7, 1, 365);

    public static final ModConfigSpec.IntValue RESTOCK_DAYS = BUILDER
            .comment("Minecraft days before used offers restock.")
            .defineInRange("restockDays", 3, 1, 365);

    public static final ModConfigSpec.IntValue MAX_SHIFT_TRADES = BUILDER
            .comment("Safety cap for one shift-click batch purchase.")
            .defineInRange("maximumShiftClickTrades", 64, 1, 256);

    public static final ModConfigSpec.IntValue PURCHASE_DELIVERY_DELAY_TICKS = BUILDER
            .comment("Delay before a purchase sack appears beside the terminal.")
            .defineInRange("purchaseDeliveryDelayTicks", 200, 1, 72_000);

    public static final ModConfigSpec.IntValue SALE_DELIVERY_DELAY_TICKS = BUILDER
            .comment("Delay before a payment sack appears beside the trade crate.")
            .defineInRange("saleDeliveryDelayTicks", 200, 1, 72_000);

    public static final ModConfigSpec.IntValue SALE_COOLDOWN_DAYS = BUILDER
            .comment("Personal cooldown in Minecraft days between trade-crate sales.")
            .defineInRange("saleCooldownDays", 3, 1, 365);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MOConfig() {
    }

    public static long catalogRefreshTicks() {
        return CATALOG_REFRESH_DAYS.get().longValue() * 24_000L;
    }

    public static long restockTicks() {
        return RESTOCK_DAYS.get().longValue() * 24_000L;
    }

    public static int purchaseDeliveryDelayTicks() {
        return PURCHASE_DELIVERY_DELAY_TICKS.get();
    }

    public static int saleDeliveryDelayTicks() {
        return SALE_DELIVERY_DELAY_TICKS.get();
    }

    public static long saleCooldownTicks() {
        return SALE_COOLDOWN_DAYS.get().longValue() * 24_000L;
    }
}
