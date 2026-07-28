package com.overyourhead.merchant_orders.common.sale;

import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TradeCrateValuation {
    public static final int MAX_REWARDS = 8;

    private TradeCrateValuation() {
    }

    public static Offer evaluate(ServerLevel level, NonNullList<ItemStack> items, long day, long playerSeed) {
        int points = 0;
        int wood = 0;
        int food = 0;
        int mineral = 0;
        int plant = 0;
        long contentHash = 1125899906842597L;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            int unit = baseUnitValue(stack);
            points += Math.max(1, unit * count);
            if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) wood += count;
            if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.SMALL_FLOWERS)) plant += count;
            if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) food += count;
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.IRON_INGOT)
                    || stack.is(Items.GOLD_INGOT) || stack.is(Items.COPPER_INGOT)
                    || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                    || stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI)) mineral += count;
            contentHash = 31L * contentHash + BuiltInRegistries.ITEM.getId(stack.getItem());
            contentHash = 31L * contentHash + count;
            contentHash = 31L * contentHash + stack.getComponentsPatch().hashCode();
        }

        if (points <= 0) {
            return Offer.empty(day, contentHash);
        }

        long seed = level.getSeed() ^ Long.rotateLeft(day, 13) ^ playerSeed ^ contentHash;
        RandomSource random = RandomSource.create(seed);
        double market = 0.90D + random.nextDouble() * 0.20D;
        int adjusted = Math.max(1, (int) Math.floor(points * market));
        int payoutBudget = Math.max(1, (int) Math.floor(adjusted * (0.62D + random.nextDouble() * 0.16D)));

        List<ItemStack> rewards = new ArrayList<>();
        int emeralds = Math.max(1, payoutBudget / 32);
        rewards.add(new ItemStack(Items.EMERALD, Mth.clamp(emeralds, 1, 64)));

        int remainder = Math.max(0, payoutBudget - emeralds * 24);
        TagKey<Item> preferredTag = dominantTag(wood, food, mineral, plant);
        if (remainder >= 8) {
            Item bonus = randomTaggedItem(level, preferredTag, random).orElse(fallbackReward(wood, food, mineral, plant));
            if (bonus != Items.AIR && bonus != MOBlocks.ORDER_SACK_ITEM.get() && bonus != MOBlocks.TRADE_CRATE_ITEM.get()) {
                rewards.add(new ItemStack(bonus, Mth.clamp(1 + remainder / 12, 1, 16)));
            }
        }
        if (remainder >= 20 && rewards.size() < MAX_REWARDS) {
            rewards.add(new ItemStack(Items.BREAD, Mth.clamp(remainder / 10, 1, 16)));
        }
        if (remainder >= 32 && rewards.size() < MAX_REWARDS) {
            Item second = randomTaggedItem(level, ItemTags.SAPLINGS, random).orElse(Items.BONE_MEAL);
            if (second != Items.AIR && second != MOBlocks.ORDER_SACK_ITEM.get() && second != MOBlocks.TRADE_CRATE_ITEM.get()) {
                rewards.add(new ItemStack(second, Mth.clamp(1 + remainder / 32, 1, 8)));
            }
        }
        if (remainder >= 48 && rewards.size() < MAX_REWARDS) {
            rewards.add(new ItemStack(Items.COAL, Mth.clamp(remainder / 16, 1, 16)));
        }

        boolean rare = random.nextFloat() < 0.0125F;
        if (rare && rewards.size() < MAX_REWARDS && adjusted >= 64) {
            Item rareItem = randomTaggedItem(level, ItemTags.SAPLINGS, random).orElse(Items.EXPERIENCE_BOTTLE);
            rewards.add(new ItemStack(rareItem, 1));
        }

        while (rewards.size() < MAX_REWARDS) {
            rewards.add(ItemStack.EMPTY);
        }
        return new Offer(adjusted, rewards, rare, day, contentHash);
    }

    private static int baseUnitValue(ItemStack stack) {
        if (stack.is(Items.NETHERITE_INGOT)) return 128;
        if (stack.is(Items.DIAMOND)) return 48;
        if (stack.is(Items.EMERALD)) return 32;
        if (stack.is(Items.GOLD_INGOT)) return 10;
        if (stack.is(Items.IRON_INGOT)) return 8;
        if (stack.is(Items.COPPER_INGOT)) return 3;
        if (stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.QUARTZ)) return 2;
        if (stack.is(ItemTags.LOGS)) return 2;
        if (stack.is(ItemTags.PLANKS)) return 1;
        if (stack.is(ItemTags.WOOL)) return 2;
        if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.SMALL_FLOWERS)) return 2;
        if (stack.is(Items.DIRT) || stack.is(Items.COBBLESTONE) || stack.is(Items.GRAVEL)
                || stack.is(Items.NETHERRACK) || stack.is(Items.SAND)) return 1;
        if (stack.isDamageableItem()) {
            float remaining = 1.0F - (float) stack.getDamageValue() / Math.max(1, stack.getMaxDamage());
            return Math.max(1, Math.round(6.0F * remaining));
        }
        return Math.max(1, 1 + stack.getCount() / 32);
    }

    private static TagKey<Item> dominantTag(int wood, int food, int mineral, int plant) {
        if (plant >= wood && plant >= food && plant >= mineral) return ItemTags.SAPLINGS;
        if (wood >= food && wood >= mineral) return ItemTags.PLANKS;
        if (food >= mineral) return ItemTags.PIGLIN_FOOD;
        return ItemTags.COALS;
    }

    private static Item fallbackReward(int wood, int food, int mineral, int plant) {
        if (plant >= wood && plant >= food && plant >= mineral) return Items.BONE_MEAL;
        if (wood >= food && wood >= mineral) return Items.OAK_PLANKS;
        if (food >= mineral) return Items.BREAD;
        return Items.COAL;
    }

    private static Optional<Item> randomTaggedItem(ServerLevel level, TagKey<Item> tag, RandomSource random) {
        return level.registryAccess().registryOrThrow(Registries.ITEM).getTag(tag)
                .flatMap(named -> named.getRandomElement(random))
                .map(Holder::value);
    }

    public record Offer(int value, List<ItemStack> rewards, boolean rare, long day, long contentHash) {
        public static Offer empty(long day, long contentHash) {
            return new Offer(0, java.util.Collections.nCopies(MAX_REWARDS, ItemStack.EMPTY), false, day, contentHash);
        }

        public boolean isEmpty() {
            return value <= 0 || rewards.stream().allMatch(ItemStack::isEmpty);
        }
    }
}
