package com.overyourhead.merchant_orders.common.block.entity;

import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.menu.OrderSackMenu;
import com.overyourhead.merchant_orders.core.registry.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OrderSackBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);

    public OrderSackBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.ORDER_SACK.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return MOConstants.BASKET_SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.merchant_orders.order_sack");
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
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    public boolean hasAnyItem() {
        return items.stream().anyMatch(stack -> !stack.isEmpty());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        Level currentLevel = getLevel();
        ContainerLevelAccess access = currentLevel == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(currentLevel, getBlockPos());
        return new OrderSackMenu(containerId, inventory, this, access, false);
    }
}
