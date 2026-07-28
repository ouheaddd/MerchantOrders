package com.overyourhead.merchant_orders.common.event;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.player.PlayerOrderData;
import com.overyourhead.merchant_orders.common.trade.TradePoolRegistry;
import com.overyourhead.merchant_orders.common.sale.PlayerSaleData;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

@EventBusSubscriber(modid = MerchantOrdersMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        TradePoolRegistry.capture(event);
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(
                MOConfig.TERMINAL_PRICE.get(),
                new ItemStack(MOBlocks.ORDER_TERMINAL.get()),
                MOConfig.TERMINAL_MAX_USES.get(),
                10
        ));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer clone) {
            PlayerOrderData.copyPersistentData(original, clone);
            PlayerSaleData.copy(original, clone);
        }
    }
}
