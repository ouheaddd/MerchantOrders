package com.overyourhead.merchant_orders;

import com.mojang.logging.LogUtils;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import com.overyourhead.merchant_orders.core.registry.MOCreativeTabs;
import com.overyourhead.merchant_orders.core.registry.MOItems;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import com.overyourhead.merchant_orders.core.registry.MOSoundEvents;
import com.overyourhead.merchant_orders.common.worldgen.RepurposedStructuresIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(MerchantOrdersMod.MOD_ID)
public final class MerchantOrdersMod {
    public static final String MOD_ID = "merchant_orders";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MerchantOrdersMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        MOBlocks.register(modEventBus);
        MOItems.register(modEventBus);
        MOBlockEntities.register(modEventBus);
        MOMenuTypes.register(modEventBus);
        MOSoundEvents.register(modEventBus);
        MOCreativeTabs.register(modEventBus);

        // Repurposed Structures exposes a custom registry for datapack conditions.
        // Register into it only when RS is installed, keeping the integration optional.
        if (ModList.get().isLoaded("repurposed_structures")) {
            RepurposedStructuresIntegration.register(modEventBus);
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, MOConfig.SPEC);
    }


    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Merchant Orders common setup complete. Developer: overyourhead");
    }
}
