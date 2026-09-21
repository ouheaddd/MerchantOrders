package com.overyourhead.merchant_orders.common.menu;

import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.block.entity.OrderTerminalBlockEntity;
import com.overyourhead.merchant_orders.common.player.PlayerOrderData;
import com.overyourhead.merchant_orders.common.trade.StoredTrade;
import com.overyourhead.merchant_orders.common.trade.TradePoolRegistry;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import com.overyourhead.merchant_orders.core.registry.MOSoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class OrderTerminalMenu extends AbstractContainerMenu {
    public static final int BUTTON_TIER_BASE = 0;
    public static final int BUTTON_OFFER_BASE = 100;
    public static final int BUTTON_EXECUTE_ONE = 200;
    public static final int BUTTON_EXECUTE_MAX = 201;
    public static final int BUTTON_CLAIM = 202;

    public static final int DATA_SELECTED_TIER = 0;
    public static final int DATA_XP = 1;
    public static final int DATA_UNLOCKED_TIER = 2;
    public static final int DATA_BASKET_SLOTS = 3;
    public static final int DATA_BASKET_ITEMS = 4;
    public static final int DATA_CATALOG_SIZE = 5;
    public static final int DATA_SELECTED_OFFER = 6;
    public static final int DATA_USES_BASE = 7;
    public static final int DATA_MAX_USES_BASE = DATA_USES_BASE + MOConstants.OFFERS_PER_TIER;
    public static final int DATA_COUNT = DATA_MAX_USES_BASE + MOConstants.OFFERS_PER_TIER;

    public static final int PAYMENT_A_SLOT = 0;
    public static final int PAYMENT_B_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int CATALOG_SLOT_BASE = 3;
    public static final int CATALOG_SLOT_COUNT = MOConstants.OFFERS_PER_TIER * 3;
    public static final int BASKET_SLOT_BASE = CATALOG_SLOT_BASE + CATALOG_SLOT_COUNT;
    public static final int BASKET_SLOT_COUNT = MOConstants.BASKET_SIZE;
    public static final int PLAYER_SLOT_BASE = BASKET_SLOT_BASE + BASKET_SLOT_COUNT;

    private final Inventory playerInventory;
    private final ContainerLevelAccess access;
    private final SimpleContainer payment = new SimpleContainer(2);
    private final SimpleContainer result = new SimpleContainer(1);
    private final SimpleContainer catalogSync = new SimpleContainer(CATALOG_SLOT_COUNT);
    private final SimpleContainer basketSync = new SimpleContainer(BASKET_SLOT_COUNT);
    private final SimpleContainerData trackedData = new SimpleContainerData(DATA_COUNT);
    private final @Nullable ServerPlayer serverPlayer;
    private final @Nullable PlayerOrderData orderData;

    private List<StoredTrade> catalog = List.of();
    private int selectedTier;
    private int selectedOffer = -1;

    public OrderTerminalMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public OrderTerminalMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(MOMenuTypes.ORDER_TERMINAL.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access;
        this.serverPlayer = playerInventory.player instanceof ServerPlayer server ? server : null;

        if (serverPlayer != null && serverPlayer.level() instanceof ServerLevel serverLevel) {
            long cycle = serverLevel.getGameTime() / MOConfig.catalogRefreshTicks();
            this.orderData = PlayerOrderData.load(serverPlayer, cycle);
            this.orderData.restockIfNeeded(serverLevel.getGameTime());
            this.selectedTier = Math.min(orderData.selectedTier(), MOConstants.unlockedTier(orderData.xp()));
            refreshCatalog();
        } else {
            this.orderData = null;
            this.selectedTier = 0;
        }

        addSlot(new PaymentSlot(payment, 0, 204, 62, true));
        addSlot(new PaymentSlot(payment, 1, 224, 62, false));
        addSlot(new ReadOnlySlot(result, 0, 274, 62));

        for (int i = 0; i < CATALOG_SLOT_COUNT; i++) {
            addSlot(new ReadOnlySlot(catalogSync, i, -1000, -1000));
        }
        for (int i = 0; i < BASKET_SLOT_COUNT; i++) {
            addSlot(new ReadOnlySlot(basketSync, i, -1000, -1000));
        }

        addPlayerInventory(playerInventory);
        addDataSlots(trackedData);
        updateTrackedData();
        updateResultPreview();
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 202 + column * 18, 143 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 202 + column * 18, 201));
        }
    }

    private void refreshCatalog() {
        if (serverPlayer == null || orderData == null || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        catalog = TradePoolRegistry.createCatalog(level, serverPlayer, selectedTier, orderData.catalogCycle());
        for (int i = 0; i < MOConstants.OFFERS_PER_TIER; i++) {
            StoredTrade trade = i < catalog.size() ? catalog.get(i) : null;
            catalogSync.setItem(i, trade == null ? ItemStack.EMPTY : trade.costA().copy());
            catalogSync.setItem(MOConstants.OFFERS_PER_TIER + i, trade == null ? ItemStack.EMPTY : trade.costB().copy());
            catalogSync.setItem(MOConstants.OFFERS_PER_TIER * 2 + i, trade == null ? ItemStack.EMPTY : trade.result().copy());
        }
        selectedOffer = -1;
        updateTrackedData();
        broadcastChanges();
    }

    private void updateTrackedData() {
        trackedData.set(DATA_SELECTED_TIER, selectedTier);
        trackedData.set(DATA_SELECTED_OFFER, selectedOffer);
        trackedData.set(DATA_CATALOG_SIZE, catalog.size());
        if (orderData != null) {
            syncBasketContents();
            trackedData.set(DATA_XP, orderData.xp());
            trackedData.set(DATA_UNLOCKED_TIER, MOConstants.unlockedTier(orderData.xp()));
            trackedData.set(DATA_BASKET_SLOTS, orderData.occupiedBasketSlots());
            trackedData.set(DATA_BASKET_ITEMS, orderData.basketItemCount());
            for (int i = 0; i < MOConstants.OFFERS_PER_TIER; i++) {
                int uses = orderData.uses(selectedTier, i);
                int maxUses = i < catalog.size() ? catalog.get(i).maxUses() : 0;
                trackedData.set(DATA_USES_BASE + i, uses);
                trackedData.set(DATA_MAX_USES_BASE + i, maxUses);
            }
        }
    }

    private void syncBasketContents() {
        if (orderData == null) {
            return;
        }
        for (int i = 0; i < BASKET_SLOT_COUNT; i++) {
            basketSync.setItem(i, orderData.basket().get(i).copy());
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (serverPlayer == null || orderData == null || player != serverPlayer) {
            return false;
        }

        if (id >= BUTTON_TIER_BASE && id < BUTTON_TIER_BASE + MOConstants.TIER_COUNT) {
            int tier = id - BUTTON_TIER_BASE;
            if (tier <= MOConstants.unlockedTier(orderData.xp())) {
                returnPaymentsToPlayer();
                selectedTier = tier;
                orderData.setSelectedTier(tier);
                orderData.save();
                refreshCatalog();
                return true;
            }
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
            return false;
        }

        if (id >= BUTTON_OFFER_BASE && id < BUTTON_OFFER_BASE + MOConstants.OFFERS_PER_TIER) {
            int offer = id - BUTTON_OFFER_BASE;
            if (offer < catalog.size()) {
                selectOffer(offer);
                return true;
            }
            return false;
        }

        return switch (id) {
            case BUTTON_EXECUTE_ONE -> executeTrades(1) > 0;
            case BUTTON_EXECUTE_MAX -> executeTrades(MOConfig.MAX_SHIFT_TRADES.get()) > 0;
            case BUTTON_CLAIM -> claimBasket();
            default -> false;
        };
    }

    private void selectOffer(int offer) {
        returnPaymentsToPlayer();
        selectedOffer = offer;
        refillPayment();
        updateResultPreview();
        updateTrackedData();
        broadcastChanges();
    }

    private int executeTrades(int limit) {
        StoredTrade trade = getSelectedTrade();
        if (trade == null || orderData == null || serverPlayer == null) {
            return 0;
        }

        int executed = 0;
        while (executed < limit) {
            int uses = orderData.uses(selectedTier, selectedOffer);
            if (uses >= trade.maxUses()) {
                break;
            }
            if (!trade.satisfiedBy(payment.getItem(0), payment.getItem(1))) {
                refillPayment();
            }
            if (!trade.satisfiedBy(payment.getItem(0), payment.getItem(1))) {
                break;
            }
            if (!orderData.canFit(trade.result())) {
                break;
            }

            payment.getItem(0).shrink(trade.costA().getCount());
            if (!trade.costB().isEmpty()) {
                payment.getItem(1).shrink(trade.costB().getCount());
            }
            if (!orderData.addToBasket(trade.result())) {
                break;
            }

            orderData.incrementUses(selectedTier, selectedOffer);
            orderData.addXp(trade.xp());
            executed++;
            refillPayment();
        }

        if (executed > 0) {
            orderData.save();
            serverPlayer.playSound(MOSoundEvents.ORDER_ADD.get(), 0.9F, 0.95F + serverPlayer.getRandom().nextFloat() * 0.1F);
        } else {
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
        }
        updateResultPreview();
        updateTrackedData();
        broadcastChanges();
        return executed;
    }

    private boolean claimBasket() {
        if (serverPlayer == null || orderData == null || orderData.occupiedBasketSlots() == 0) {
            return false;
        }

        ItemStack sack = orderData.createSackStack(new ItemStack(MOBlocks.ORDER_SACK_ITEM.get()));
        final boolean[] scheduled = {false};
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof OrderTerminalBlockEntity terminal) {
                scheduled[0] = terminal.scheduleDelivery(sack, MOConfig.purchaseDeliveryDelayTicks());
            }
        });
        if (!scheduled[0]) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.merchant_orders.delivery_busy"), true);
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }

        orderData.clearBasket();
        orderData.save();
        serverPlayer.playSound(MOSoundEvents.ORDER_CLAIM.get(), 1.0F, 1.0F);
        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.merchant_orders.delivery_scheduled"), true);
        updateTrackedData();
        broadcastChanges();
        return true;
    }

    private void refillPayment() {
        StoredTrade trade = getSelectedTrade();
        if (trade == null || serverPlayer == null) {
            return;
        }
        fillSlotToRequired(payment, 0, trade.costA());
        if (!trade.costB().isEmpty()) {
            fillSlotToRequired(payment, 1, trade.costB());
        }
        payment.setChanged();
        updateResultPreview();
    }

    private void fillSlotToRequired(Container target, int targetSlot, ItemStack required) {
        ItemStack present = target.getItem(targetSlot);
        int presentCount = ItemStack.isSameItemSameComponents(present, required) ? present.getCount() : 0;
        int needed = required.getCount() - presentCount;
        if (needed <= 0) {
            return;
        }
        for (int i = 0; i < playerInventory.items.size() && needed > 0; i++) {
            ItemStack inventoryStack = playerInventory.items.get(i);
            if (!inventoryStack.isEmpty() && ItemStack.isSameItemSameComponents(inventoryStack, required)) {
                int moved = Math.min(needed, inventoryStack.getCount());
                if (present.isEmpty()) {
                    present = inventoryStack.copyWithCount(moved);
                    target.setItem(targetSlot, present);
                } else {
                    present.grow(moved);
                }
                inventoryStack.shrink(moved);
                needed -= moved;
            }
        }
    }

    private void updateResultPreview() {
        StoredTrade trade = getSelectedTrade();
        boolean valid = trade != null
                && orderData != null
                && orderData.uses(selectedTier, selectedOffer) < trade.maxUses()
                && trade.satisfiedBy(payment.getItem(0), payment.getItem(1))
                && orderData.canFit(trade.result());
        result.setItem(0, valid ? trade.result().copy() : ItemStack.EMPTY);
    }

    private @Nullable StoredTrade getSelectedTrade() {
        return selectedOffer >= 0 && selectedOffer < catalog.size() ? catalog.get(selectedOffer) : null;
    }

    private void returnPaymentsToPlayer() {
        if (serverPlayer == null) {
            return;
        }
        for (int i = 0; i < payment.getContainerSize(); i++) {
            ItemStack stack = payment.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !playerInventory.add(stack)) {
                serverPlayer.drop(stack, false);
            }
        }
        result.setItem(0, ItemStack.EMPTY);
    }

    public ItemStack getCatalogCostA(int offer) {
        return catalogSync.getItem(offer);
    }

    public ItemStack getCatalogCostB(int offer) {
        return catalogSync.getItem(MOConstants.OFFERS_PER_TIER + offer);
    }

    public ItemStack getCatalogResult(int offer) {
        return catalogSync.getItem(MOConstants.OFFERS_PER_TIER * 2 + offer);
    }

    public int selectedTier() {
        return trackedData.get(DATA_SELECTED_TIER);
    }

    public int xp() {
        return trackedData.get(DATA_XP);
    }

    public int unlockedTier() {
        return trackedData.get(DATA_UNLOCKED_TIER);
    }

    public int basketSlots() {
        return trackedData.get(DATA_BASKET_SLOTS);
    }

    public int basketItems() {
        return trackedData.get(DATA_BASKET_ITEMS);
    }

    public ItemStack getBasketItem(int slot) {
        if (slot < 0 || slot >= BASKET_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return basketSync.getItem(slot);
    }

    public int catalogSize() {
        return trackedData.get(DATA_CATALOG_SIZE);
    }

    public int selectedOffer() {
        return trackedData.get(DATA_SELECTED_OFFER);
    }

    public int uses(int offer) {
        return trackedData.get(DATA_USES_BASE + offer);
    }

    public int maxUses(int offer) {
        return trackedData.get(DATA_MAX_USES_BASE + offer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        if (slotIndex == PAYMENT_A_SLOT || slotIndex == PAYMENT_B_SLOT) {
            if (!moveItemStackTo(source, PLAYER_SLOT_BASE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_SLOT_BASE) {
            if (!moveItemStackTo(source, PAYMENT_A_SLOT, PAYMENT_B_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        updateResultPreview();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, MOBlocks.ORDER_TERMINAL.get());
    }

    @Override
    public void removed(Player player) {
        returnPaymentsToPlayer();
        if (orderData != null) {
            orderData.save();
        }
        super.removed(player);
    }

    private final class PaymentSlot extends Slot {
        private final boolean first;

        private PaymentSlot(Container container, int slot, int x, int y, boolean first) {
            super(container, slot, x, y);
            this.first = first;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            StoredTrade trade = getSelectedTrade();
            if (trade == null) {
                return false;
            }
            ItemStack required = first ? trade.costA() : trade.costB();
            return !required.isEmpty() && ItemStack.isSameItemSameComponents(stack, required);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            updateResultPreview();
        }
    }

    private static final class ReadOnlySlot extends Slot {
        private ReadOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
