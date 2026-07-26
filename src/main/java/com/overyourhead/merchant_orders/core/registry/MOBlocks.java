package com.overyourhead.merchant_orders.core.registry;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.block.OrderSackBlock;
import com.overyourhead.merchant_orders.common.block.OrderTerminalBlock;
import com.overyourhead.merchant_orders.common.item.OrderSackItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MerchantOrdersMod.MOD_ID);

    public static final DeferredBlock<OrderTerminalBlock> ORDER_TERMINAL = BLOCKS.register(
            "order_terminal",
            () -> new OrderTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
    );

    public static final DeferredBlock<OrderSackBlock> ORDER_SACK = BLOCKS.register(
            "order_sack",
            () -> new OrderSackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .noOcclusion())
    );

    public static final DeferredItem<Item> ORDER_TERMINAL_ITEM = MOItems.ITEMS.register(
            "order_terminal",
            () -> new BlockItem(ORDER_TERMINAL.get(), new Item.Properties())
    );

    public static final DeferredItem<OrderSackItem> ORDER_SACK_ITEM = MOItems.ITEMS.register(
            "order_sack",
            () -> new OrderSackItem(ORDER_SACK.get(), new Item.Properties().stacksTo(1))
    );

    private MOBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
