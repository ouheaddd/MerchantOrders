package com.overyourhead.merchant_orders.client;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.client.screen.OrderSackScreen;
import com.overyourhead.merchant_orders.client.screen.OrderTerminalScreen;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MerchantOrdersMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MOClientEvents {
    private MOClientEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MOMenuTypes.ORDER_TERMINAL.get(), OrderTerminalScreen::new);
        event.register(MOMenuTypes.ORDER_SACK.get(), OrderSackScreen::new);
    }
}
