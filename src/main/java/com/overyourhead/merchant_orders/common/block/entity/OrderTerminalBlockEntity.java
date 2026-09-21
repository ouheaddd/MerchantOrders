package com.overyourhead.merchant_orders.common.block.entity;

import com.overyourhead.merchant_orders.common.delivery.DeliveryUtil;
import com.overyourhead.merchant_orders.common.menu.OrderTerminalMenu;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class OrderTerminalBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack pendingDelivery = ItemStack.EMPTY;
    private int deliveryTicks;

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

    public boolean hasPendingDelivery() {
        return !pendingDelivery.isEmpty();
    }

    public boolean scheduleDelivery(ItemStack sack, int delayTicks) {
        if (sack.isEmpty() || hasPendingDelivery()) {
            return false;
        }
        pendingDelivery = sack.copy();
        deliveryTicks = Math.max(1, delayTicks);
        setChanged();
        return true;
    }

    public void dropPendingDelivery() {
        if (pendingDelivery.isEmpty() || level == null || level.isClientSide) {
            return;
        }

        Containers.dropItemStack(
                level,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                pendingDelivery.copy()
        );
        pendingDelivery = ItemStack.EMPTY;
        deliveryTicks = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrderTerminalBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!blockEntity.hasPendingDelivery()) {
            return;
        }
        if (blockEntity.deliveryTicks > 0) {
            blockEntity.deliveryTicks--;
            blockEntity.setChanged();
            return;
        }
        if (DeliveryUtil.placeSack(serverLevel, pos, blockEntity.pendingDelivery)) {
            blockEntity.pendingDelivery = ItemStack.EMPTY;
            blockEntity.deliveryTicks = 0;
            blockEntity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!pendingDelivery.isEmpty()) {
            tag.put("PendingDelivery", pendingDelivery.save(registries));
            tag.putInt("DeliveryTicks", deliveryTicks);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pendingDelivery = tag.contains("PendingDelivery")
                ? ItemStack.parseOptional(registries, tag.getCompound("PendingDelivery"))
                : ItemStack.EMPTY;
        deliveryTicks = tag.getInt("DeliveryTicks");
    }
}
