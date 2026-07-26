package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.menu.OrderSackMenu;
import com.overyourhead.merchant_orders.common.menu.OrderTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MerchantOrdersMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<OrderTerminalMenu>> ORDER_TERMINAL =
            MENU_TYPES.register("order_terminal", () -> new MenuType<>(OrderTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<OrderSackMenu>> ORDER_SACK =
            MENU_TYPES.register("order_sack", () -> new MenuType<>(OrderSackMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private MOMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
