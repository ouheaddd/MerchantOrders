package com.overyourhead.merchant_orders.common.block.entity;

import com.overyourhead.merchant_orders.common.menu.OrderTerminalMenu;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class OrderTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public OrderTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.ORDER_TERMINAL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.merchant_orders.order_terminal");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, worldPosition);
        return new OrderTerminalMenu(containerId, inventory, access);
    }
}
