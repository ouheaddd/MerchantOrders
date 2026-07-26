package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MerchantOrdersMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.merchant_orders.main"))
                    .icon(() -> new ItemStack(MOBlocks.ORDER_TERMINAL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MOBlocks.ORDER_TERMINAL_ITEM.get());
                        output.accept(MOBlocks.ORDER_SACK_ITEM.get());
                    })
                    .build()
    );

    private MOCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
