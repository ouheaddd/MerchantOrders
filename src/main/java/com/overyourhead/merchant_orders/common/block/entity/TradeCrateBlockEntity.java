package com.overyourhead.merchant_orders.common.block.entity;

import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.delivery.DeliveryUtil;
import com.overyourhead.merchant_orders.common.menu.TradeCrateMenu;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TradeCrateBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SIZE = 36;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ItemStack pendingDelivery = ItemStack.EMPTY;
    private int deliveryTicks;

    public TradeCrateBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.TRADE_CRATE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.merchant_orders.trade_crate");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    public NonNullList<ItemStack> getStoredItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        NonNullList<ItemStack> resized = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), resized.size()); i++) {
            resized.set(i, items.get(i));
        }
        this.items = resized;
    }

    public boolean isEmptyBatch() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public void clearBatch() {
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        setChanged();
    }

    public boolean hasPendingDelivery() {
        return !pendingDelivery.isEmpty();
    }

    public int deliveryTicks() {
        return deliveryTicks;
    }

    public boolean scheduleDelivery(ItemStack sack) {
        if (sack.isEmpty() || hasPendingDelivery()) {
            return false;
        }
        pendingDelivery = sack.copy();
        deliveryTicks = MOConfig.saleDeliveryDelayTicks();
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TradeCrateBlockEntity blockEntity) {
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
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TradeCrateMenu(containerId, inventory, this);
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

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }
}
