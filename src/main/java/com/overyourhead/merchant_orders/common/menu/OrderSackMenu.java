package com.overyourhead.merchant_orders.common.menu;

import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.inventory.SackItemContainer;
import com.overyourhead.merchant_orders.common.inventory.SackRules;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class OrderSackMenu extends AbstractContainerMenu {
    private static final int SACK_ROWS = 4;
    private static final int PLAYER_SLOT_BASE = MOConstants.BASKET_SIZE;
    private final Container container;
    private final ContainerLevelAccess access;
    private final boolean itemBacked;
    private final @Nullable ItemStack lockedSackStack;

    /** Client-side constructor used by MenuType. */
    public OrderSackMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MOConstants.BASKET_SIZE), ContainerLevelAccess.NULL, true, null);
    }

    public OrderSackMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            ContainerLevelAccess access,
            boolean itemBacked
    ) {
        this(containerId, playerInventory, container, access, itemBacked, null);
    }

    private OrderSackMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            ContainerLevelAccess access,
            boolean itemBacked,
            @Nullable ItemStack lockedSackStack
    ) {
        super(MOMenuTypes.ORDER_SACK.get(), containerId);
        this.container = container;
        this.access = access;
        this.itemBacked = itemBacked;
        this.lockedSackStack = lockedSackStack;
        checkContainerSize(container, MOConstants.BASKET_SIZE);
        container.startOpen(playerInventory.player);

        for (int row = 0; row < SACK_ROWS; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SackSlot(container, column + row * 9, 9 + column * 18, 19 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new LockedPlayerSlot(playerInventory, column + row * 9 + 9, 10 + column * 18, 104 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new LockedPlayerSlot(playerInventory, column, 10 + column * 18, 162));
        }
    }

    public static OrderSackMenu forItem(int containerId, Inventory inventory, ItemStack sackStack) {
        return new OrderSackMenu(
                containerId,
                inventory,
                new SackItemContainer(sackStack),
                ContainerLevelAccess.NULL,
                true,
                sackStack
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (slotIndex < PLAYER_SLOT_BASE) {
            if (!moveItemStackTo(source, PLAYER_SLOT_BASE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!SackRules.canStore(source)
                    || !moveItemStackTo(source, 0, PLAYER_SLOT_BASE, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!itemBacked) {
            return stillValid(access, player, MOBlocks.ORDER_SACK.get());
        }
        return lockedSackStack == null
                || (!lockedSackStack.isEmpty() && lockedSackStack.is(MOBlocks.ORDER_SACK_ITEM.get()));
    }

    @Override
    public void removed(Player player) {
        container.stopOpen(player);
        super.removed(player);
    }

    private final class LockedPlayerSlot extends Slot {
        private LockedPlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return lockedSackStack == null || getItem() != lockedSackStack;
        }
    }

    private static final class SackSlot extends Slot {
        private SackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SackRules.canStore(stack) && container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
