package com.overyourhead.merchant_orders.common.item;

import com.overyourhead.merchant_orders.common.menu.OrderSackMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class OrderSackItem extends BlockItem {
    public OrderSackItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> OrderSackMenu.forItem(containerId, inventory, stack),
                    Component.translatable("container.merchant_orders.order_sack")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
