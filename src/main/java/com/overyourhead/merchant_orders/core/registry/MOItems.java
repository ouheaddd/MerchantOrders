package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.item.OrderBoardDebugStickItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MerchantOrdersMod.MOD_ID);

    public static final DeferredItem<OrderBoardDebugStickItem> ORDER_BOARD_DEBUG_STICK = ITEMS.register(
            "order_board_debug_stick",
            () -> new OrderBoardDebugStickItem(new Item.Properties().stacksTo(1))
    );

    private MOItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
