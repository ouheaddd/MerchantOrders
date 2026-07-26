package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MerchantOrdersMod.MOD_ID);

    private MOItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
