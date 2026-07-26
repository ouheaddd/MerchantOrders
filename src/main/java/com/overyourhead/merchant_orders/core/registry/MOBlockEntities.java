package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.block.entity.OrderSackBlockEntity;
import com.overyourhead.merchant_orders.common.block.entity.OrderTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MerchantOrdersMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrderTerminalBlockEntity>> ORDER_TERMINAL =
            BLOCK_ENTITY_TYPES.register("order_terminal", () -> BlockEntityType.Builder
                    .of(OrderTerminalBlockEntity::new, MOBlocks.ORDER_TERMINAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrderSackBlockEntity>> ORDER_SACK =
            BLOCK_ENTITY_TYPES.register("order_sack", () -> BlockEntityType.Builder
                    .of(OrderSackBlockEntity::new, MOBlocks.ORDER_SACK.get())
                    .build(null));

    private MOBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
