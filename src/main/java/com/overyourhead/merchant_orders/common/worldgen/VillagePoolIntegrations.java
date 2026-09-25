package com.overyourhead.merchant_orders.common.worldgen;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry for vanilla-style village pool compatibility.
 *
 * <p>Vanilla villages use the five Minecraft house pools. Towns & Towers
 * uses its own kaisyn namespace. Repurposed Structures
 * is intentionally NOT handled here; it has a native pool-addition API and is
 * integrated through data/merchant_orders/rs_pool_additions instead.</p>
 */
public final class VillagePoolIntegrations {
    private static final ResourceLocation COMPAT_ORDER_BOARD_POOL =
            ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, "village/compat/order_board");

    private static final Map<ResourceLocation, ResourceLocation> EXACT_REPLACEMENTS = new LinkedHashMap<>();

    static {
        registerVanilla("plains");
        registerVanilla("desert");
        registerVanilla("savanna");
        registerVanilla("snowy");
        registerVanilla("taiga");
    }

    private VillagePoolIntegrations() {
    }

    private static void registerVanilla(String villageType) {
        register(
                ResourceLocation.withDefaultNamespace("village/" + villageType + "/houses"),
                ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, "village/" + villageType + "/order_board")
        );
    }

    public static void register(ResourceLocation sourceHousePool, ResourceLocation replacementPool) {
        EXACT_REPLACEMENTS.put(sourceHousePool, replacementPool);
    }

    public static ResourceLocation replacementFor(ResourceLocation sourceHousePool) {
        ResourceLocation exact = EXACT_REPLACEMENTS.get(sourceHousePool);
        if (exact != null) {
            return exact;
        }

        String namespace = sourceHousePool.getNamespace();
        String path = sourceHousePool.getPath();

        /*
         * Towns & Towers 1.13.11 uses the data namespace "kaisyn" for its
         * actual template pools (the mod id itself is towns_and_towers).
         */
        if (namespace.equals("kaisyn")
                && MOConfig.TOWNS_AND_TOWERS_INTEGRATION.get()
                && isTownsAndTowersVillageBuildingPool(path)) {
            return COMPAT_ORDER_BOARD_POOL;
        }

        return null;
    }

    private static boolean isTownsAndTowersVillageBuildingPool(String path) {
        if (!path.startsWith("village/")) {
            return false;
        }

        if (path.endsWith("/houses") || path.contains("/houses/")) {
            return true;
        }

        return path.endsWith("/side/house")
                || path.endsWith("/side/farms")
                || path.endsWith("/side/windmill");
    }
}
