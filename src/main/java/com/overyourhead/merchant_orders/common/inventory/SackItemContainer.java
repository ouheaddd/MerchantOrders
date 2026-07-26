package com.overyourhead.merchant_orders.common.inventory;

import com.overyourhead.merchant_orders.common.MOConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class SackItemContainer extends SimpleContainer {
    private final ItemStack sackStack;
    private boolean loading;

    public SackItemContainer(ItemStack sackStack) {
        super(MOConstants.BASKET_SIZE);
        this.sackStack = sackStack;
        this.loading = true;
        ItemContainerContents contents = sackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        net.minecraft.core.NonNullList<ItemStack> loaded = net.minecraft.core.NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);
        contents.copyInto(loaded);
        for (int i = 0; i < MOConstants.BASKET_SIZE; i++) {
            setItem(i, loaded.get(i).copy());
        }
        this.loading = false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            net.minecraft.core.NonNullList<ItemStack> saved = net.minecraft.core.NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < MOConstants.BASKET_SIZE; i++) {
                saved.set(i, getItem(i).copy());
            }
            sackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(saved));
        }
    }
}
