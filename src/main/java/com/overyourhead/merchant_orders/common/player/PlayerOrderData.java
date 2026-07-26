package com.overyourhead.merchant_orders.common.player;

import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class PlayerOrderData {
    public static final String ROOT_KEY = "MerchantOrders";

    private final ServerPlayer player;
    private final int[][] uses = new int[MOConstants.TIER_COUNT][MOConstants.OFFERS_PER_TIER];
    private NonNullList<ItemStack> basket = NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);
    private int xp;
    private int selectedTier;
    private long catalogCycle;
    private long lastRestock;

    private PlayerOrderData(ServerPlayer player) {
        this.player = player;
    }

    public static PlayerOrderData load(ServerPlayer player, long currentCycle) {
        PlayerOrderData data = new PlayerOrderData(player);
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        data.xp = Math.max(0, root.getInt("Xp"));
        data.selectedTier = Math.max(0, Math.min(MOConstants.TIER_COUNT - 1, root.getInt("SelectedTier")));
        data.catalogCycle = root.getLong("CatalogCycle");
        data.lastRestock = root.getLong("LastRestock");

        if (data.catalogCycle == currentCycle) {
            for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
                int[] stored = root.getIntArray("Uses" + tier);
                System.arraycopy(stored, 0, data.uses[tier], 0, Math.min(stored.length, MOConstants.OFFERS_PER_TIER));
            }
        } else {
            data.catalogCycle = currentCycle;
        }

        if (root.contains("Basket")) {
            data.basket = NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(root.getCompound("Basket"), data.basket, player.registryAccess());
        }
        return data;
    }

    public void save() {
        CompoundTag root = new CompoundTag();
        root.putInt("Xp", xp);
        root.putInt("SelectedTier", selectedTier);
        root.putLong("CatalogCycle", catalogCycle);
        root.putLong("LastRestock", lastRestock);
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            root.putIntArray("Uses" + tier, uses[tier]);
        }
        CompoundTag basketTag = new CompoundTag();
        ContainerHelper.saveAllItems(basketTag, basket, player.registryAccess());
        root.put("Basket", basketTag);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    public void restockIfNeeded(long gameTime) {
        if (lastRestock == 0L) {
            lastRestock = gameTime;
            return;
        }
        if (gameTime - lastRestock >= MOConfig.restockTicks()) {
            for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
                java.util.Arrays.fill(uses[tier], 0);
            }
            lastRestock = gameTime;
            save();
        }
    }

    public int xp() {
        return xp;
    }

    public void addXp(int amount) {
        xp = Math.max(0, xp + amount);
    }

    public int selectedTier() {
        return selectedTier;
    }

    public void setSelectedTier(int selectedTier) {
        this.selectedTier = Math.max(0, Math.min(MOConstants.TIER_COUNT - 1, selectedTier));
    }

    public long catalogCycle() {
        return catalogCycle;
    }

    public int uses(int tier, int offer) {
        if (tier < 0 || tier >= uses.length || offer < 0 || offer >= uses[tier].length) {
            return 0;
        }
        return uses[tier][offer];
    }

    public void incrementUses(int tier, int offer) {
        if (tier >= 0 && tier < uses.length && offer >= 0 && offer < uses[tier].length) {
            uses[tier][offer]++;
        }
    }

    public NonNullList<ItemStack> basket() {
        return basket;
    }

    public int occupiedBasketSlots() {
        int count = 0;
        for (ItemStack stack : basket) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int basketItemCount() {
        int count = 0;
        for (ItemStack stack : basket) {
            count += stack.getCount();
        }
        return count;
    }

    public boolean canFit(ItemStack incoming) {
        ItemStack remaining = incoming.copy();
        for (ItemStack stack : basket) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, remaining)) {
                int free = Math.max(0, stack.getMaxStackSize() - stack.getCount());
                remaining.shrink(Math.min(free, remaining.getCount()));
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        for (ItemStack stack : basket) {
            if (stack.isEmpty()) {
                remaining.shrink(Math.min(remaining.getMaxStackSize(), remaining.getCount()));
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        return remaining.isEmpty();
    }

    public boolean addToBasket(ItemStack incoming) {
        if (!canFit(incoming)) {
            return false;
        }
        ItemStack remaining = incoming.copy();
        for (int i = 0; i < basket.size(); i++) {
            ItemStack stack = basket.get(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, remaining)) {
                int move = Math.min(stack.getMaxStackSize() - stack.getCount(), remaining.getCount());
                stack.grow(move);
                remaining.shrink(move);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        for (int i = 0; i < basket.size(); i++) {
            if (basket.get(i).isEmpty()) {
                int move = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                ItemStack placed = remaining.copyWithCount(move);
                basket.set(i, placed);
                remaining.shrink(move);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        return remaining.isEmpty();
    }

    public ItemStack createSackStack(ItemStack sackTemplate) {
        ItemStack sack = sackTemplate.copy();
        sack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(basket));
        return sack;
    }

    public void clearBasket() {
        basket = NonNullList.withSize(MOConstants.BASKET_SIZE, ItemStack.EMPTY);
    }

    public static void copyPersistentData(ServerPlayer original, ServerPlayer clone) {
        if (original.getPersistentData().contains(ROOT_KEY)) {
            clone.getPersistentData().put(ROOT_KEY, original.getPersistentData().getCompound(ROOT_KEY).copy());
        }
    }
}
