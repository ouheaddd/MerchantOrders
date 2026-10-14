package com.overyourhead.merchant_orders.client;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.client.renderer.OrderSackRenderer;
import com.overyourhead.merchant_orders.client.screen.OrderSackScreen;
import com.overyourhead.merchant_orders.client.screen.OrderTerminalScreen;
import com.overyourhead.merchant_orders.client.screen.TradeCrateScreen;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MerchantOrdersMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MOClientEvents {
    private MOClientEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MOMenuTypes.ORDER_TERMINAL.get(), OrderTerminalScreen::new);
        event.register(MOMenuTypes.TRADE_CRATE.get(), TradeCrateScreen::new);
        event.register(MOMenuTypes.ORDER_SACK.get(), OrderSackScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MOBlockEntities.ORDER_SACK.get(), OrderSackRenderer::new);
    }
}
