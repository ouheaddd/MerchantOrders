package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MerchantOrdersMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_ADD = register("ui.order_add");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_CLAIM = register("ui.order_claim");

    private MOSoundEvents() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, name)
        ));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
