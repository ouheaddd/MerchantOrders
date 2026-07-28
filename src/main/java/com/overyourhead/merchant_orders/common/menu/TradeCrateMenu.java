package com.overyourhead.merchant_orders.common.menu;

import com.overyourhead.merchant_orders.common.block.entity.TradeCrateBlockEntity;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.sale.PlayerSaleData;
import com.overyourhead.merchant_orders.common.sale.TradeCrateValuation;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import com.overyourhead.merchant_orders.core.registry.MOMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

public final class TradeCrateMenu extends AbstractContainerMenu {
    public static final int BUTTON_SELL = 0;
    public static final int CRATE_SLOTS = TradeCrateBlockEntity.SIZE;
    public static final int REWARD_SLOTS = TradeCrateValuation.MAX_REWARDS;
    public static final int REWARD_SLOT_BASE = CRATE_SLOTS;
    public static final int PLAYER_SLOT_BASE = CRATE_SLOTS + REWARD_SLOTS;

    private static final int DATA_VALUE = 0;
    private static final int DATA_COOLDOWN = 1;
    private static final int DATA_PENDING = 2;
    private static final int DATA_RARE = 3;
    private static final int DATA_OFFER_REFRESH = 4;
    private static final int DATA_COUNT = 5;

    private final Container crate;
    private final SimpleContainer reward = new SimpleContainer(REWARD_SLOTS);
    private final ContainerData data = new SimpleContainerData(DATA_COUNT);
    private final Inventory playerInventory;
    private final @Nullable TradeCrateBlockEntity blockEntity;
    private final @Nullable ServerPlayer serverPlayer;
    private TradeCrateValuation.Offer currentOffer = TradeCrateValuation.Offer.empty(0L, 0L);

    public TradeCrateMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CRATE_SLOTS), null);
    }

    public TradeCrateMenu(int containerId, Inventory inventory, TradeCrateBlockEntity crate) {
        this(containerId, inventory, crate, crate);
    }

    private TradeCrateMenu(int containerId, Inventory inventory, Container crate, @Nullable TradeCrateBlockEntity blockEntity) {
        super(MOMenuTypes.TRADE_CRATE.get(), containerId);
        this.playerInventory = inventory;
        this.crate = crate;
        this.blockEntity = blockEntity;
        this.serverPlayer = inventory.player instanceof ServerPlayer server ? server : null;
        checkContainerSize(crate, CRATE_SLOTS);
        crate.startOpen(inventory.player);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                addSlot(new Slot(crate, column + row * 6, 16 + column * 18, 34 + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 4; column++) {
                int index = column + row * 4;
                addSlot(new RewardSlot(reward, index, 204 + column * 37, 120 + row * 22));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 160 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 16 + column * 18, 218));
        }

        addDataSlots(data);
        refreshOffer();
    }

    private void refreshOffer() {
        if (serverPlayer == null || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        NonNullList<ItemStack> contents = NonNullList.withSize(CRATE_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < CRATE_SLOTS; i++) {
            contents.set(i, crate.getItem(i).copy());
        }
        long day = level.getDayTime() / 24_000L;
        long playerSeed = serverPlayer.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(serverPlayer.getUUID().getLeastSignificantBits(), 19);
        currentOffer = TradeCrateValuation.evaluate(level, contents, day, playerSeed);
        for (int i = 0; i < REWARD_SLOTS; i++) {
            reward.setItem(i, currentOffer.rewards().get(i).copy());
        }
        long remaining = PlayerSaleData.remainingCooldown(serverPlayer, level.getGameTime(), MOConfig.saleCooldownTicks());
        data.set(DATA_VALUE, currentOffer.value());
        data.set(DATA_COOLDOWN, (int) Math.min(Integer.MAX_VALUE, remaining));
        data.set(DATA_PENDING, blockEntity == null ? 0 : blockEntity.deliveryTicks());
        data.set(DATA_RARE, currentOffer.rare() ? 1 : 0);
        data.set(DATA_OFFER_REFRESH, (int) Math.min(Integer.MAX_VALUE, 24000L - (level.getDayTime() % 24000L)));
        broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == crate) {
            crate.setChanged();
            refreshOffer();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_SELL || player != serverPlayer || serverPlayer == null
                || blockEntity == null || !(serverPlayer.level() instanceof ServerLevel level)) {
            return false;
        }
        refreshOffer();
        long remaining = PlayerSaleData.remainingCooldown(serverPlayer, level.getGameTime(), MOConfig.saleCooldownTicks());
        if (remaining > 0L) {
            serverPlayer.displayClientMessage(Component.translatable("message.merchant_orders.sale_cooldown"), true);
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }
        if (blockEntity.hasPendingDelivery()) {
            serverPlayer.displayClientMessage(Component.translatable("message.merchant_orders.delivery_busy"), true);
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }
        if (currentOffer.isEmpty()) {
            serverPlayer.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }

        NonNullList<ItemStack> sackContents = NonNullList.withSize(36, ItemStack.EMPTY);
        int index = 0;
        for (ItemStack stack : currentOffer.rewards()) {
            if (!stack.isEmpty() && index < sackContents.size()) {
                sackContents.set(index++, stack.copy());
            }
        }
        ItemStack sack = new ItemStack(MOBlocks.ORDER_SACK_ITEM.get());
        sack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(sackContents));
        if (!blockEntity.scheduleDelivery(sack)) {
            return false;
        }

        blockEntity.clearBatch();
        PlayerSaleData.setLastSale(serverPlayer, level.getGameTime());
        serverPlayer.playSound(SoundEvents.VILLAGER_YES, 0.9F, 1.0F);
        serverPlayer.displayClientMessage(Component.translatable("message.merchant_orders.sale_accepted"), true);
        refreshOffer();
        return true;
    }

    public int value() {
        return data.get(DATA_VALUE);
    }

    public int cooldownTicks() {
        return data.get(DATA_COOLDOWN);
    }

    public int pendingTicks() {
        return data.get(DATA_PENDING);
    }

    public boolean rareOffer() {
        return data.get(DATA_RARE) != 0;
    }

    public int approxEmeraldMin() {
        if (value() <= 0) return 0;
        return Math.max(1, value() / 40);
    }

    public int approxEmeraldMax() {
        if (value() <= 0) return 0;
        return Math.max(approxEmeraldMin(), (value() + 23) / 24);
    }

    public int offerRefreshTicks() {
        return data.get(DATA_OFFER_REFRESH);
    }

    @Override
    public void broadcastChanges() {
        if (serverPlayer != null && serverPlayer.level() instanceof ServerLevel level) {
            long day = level.getDayTime() / 24000L;
            if (currentOffer.day() != day) {
                refreshOffer();
                return;
            }
            long remaining = PlayerSaleData.remainingCooldown(serverPlayer, level.getGameTime(), MOConfig.saleCooldownTicks());
            data.set(DATA_COOLDOWN, (int) Math.min(Integer.MAX_VALUE, remaining));
            data.set(DATA_PENDING, blockEntity == null ? 0 : blockEntity.deliveryTicks());
            data.set(DATA_OFFER_REFRESH, (int) Math.min(Integer.MAX_VALUE, 24000L - (level.getDayTime() % 24000L)));
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (slotIndex < CRATE_SLOTS) {
            if (!moveItemStackTo(source, PLAYER_SLOT_BASE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_SLOT_BASE) {
            if (!moveItemStackTo(source, 0, CRATE_SLOTS, false)) {
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
        slotsChanged(crate);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return crate.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        crate.stopOpen(player);
        super.removed(player);
    }

    private static final class RewardSlot extends Slot {
        private RewardSlot(Container container, int slot, int x, int y) {
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
