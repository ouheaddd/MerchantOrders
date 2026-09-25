package com.overyourhead.merchant_orders.common.worldgen;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Optional bridge to Repurposed Structures' custom json_conditions registry.
 *
 * <p>This class only uses NeoForge registry APIs and the registry id exposed by
 * RS, so Merchant Orders does not have a compile/runtime hard dependency on RS.</p>
 */
public final class RepurposedStructuresIntegration {
    private static final DeferredRegister<Supplier<Boolean>> RS_CONDITIONS = DeferredRegister.create(
            ResourceLocation.fromNamespaceAndPath("repurposed_structures", "json_conditions"),
            MerchantOrdersMod.MOD_ID
    );

    @SuppressWarnings("unused")
    private static final DeferredHolder<Supplier<Boolean>, Supplier<Boolean>> CONFIG_CONDITION = RS_CONDITIONS.register(
            "config",
            () -> () -> MOConfig.SPAWN_IN_VILLAGES.get() && MOConfig.REPURPOSED_STRUCTURES_INTEGRATION.get()
    );

    private RepurposedStructuresIntegration() {
    }

    public static void register(IEventBus modEventBus) {
        RS_CONDITIONS.register(modEventBus);
    }
}
