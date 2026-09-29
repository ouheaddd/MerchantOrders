package com.overyourhead.merchant_orders.common.trade;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class TradePoolRegistry {
    private static final List<List<TradeSource>> SOURCES = createTierLists();
    private static final int MAX_OFFERS_PER_PROFESSION = 4;

    private static final List<String> COLOR_PREFIXES = List.of(
            "light_blue_",
            "light_gray_",
            "white_",
            "orange_",
            "magenta_",
            "yellow_",
            "lime_",
            "pink_",
            "gray_",
            "cyan_",
            "purple_",
            "blue_",
            "brown_",
            "green_",
            "red_",
            "black_"
    );

    private TradePoolRegistry() {
    }

    private static List<List<TradeSource>> createTierLists() {
        List<List<TradeSource>> result = new ArrayList<>();
        for (int i = 0; i < MOConstants.TIER_COUNT; i++) {
            result.add(new ArrayList<>());
        }
        return result;
    }

    /**
     * Captures the final vanilla/NeoForge trade lists for each registered
     * villager profession. No trades are hardcoded in this class.
     */
    public static synchronized void capture(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();

        // The event is fired again on data reload, so replace the old entries
        // for this profession instead of duplicating them.
        for (List<TradeSource> tier : SOURCES) {
            tier.removeIf(source -> source.profession().equals(profession));
        }

        for (int level = 1; level <= MOConstants.TIER_COUNT; level++) {
            List<VillagerTrades.ItemListing> listings = event.getTrades().get(level);
            if (listings == null || listings.isEmpty()) {
                continue;
            }

            for (VillagerTrades.ItemListing listing : listings) {
                SOURCES.get(level - 1).add(new TradeSource(profession, listing));
            }
        }
    }

    public static List<StoredTrade> createCatalog(ServerLevel level, ServerPlayer player, int tier, long cycle) {
        if (tier < 0 || tier >= MOConstants.TIER_COUNT) {
            return List.of();
        }

        List<TradeSource> shuffled;
        synchronized (TradePoolRegistry.class) {
            shuffled = new ArrayList<>(SOURCES.get(tier));
        }

        if (shuffled.isEmpty()) {
            MerchantOrdersMod.LOGGER.warn(
                    "No villager trades were captured for terminal tier {}. "
                            + "Check that TradePoolRegistry.capture is subscribed to VillagerTradesEvent.",
                    tier + 1
            );
            return List.of();
        }

        long seed = level.getSeed()
                ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17)
                ^ (cycle * 0x9E3779B97F4A7C15L)
                ^ ((long) tier * 0xD1B54A32D192ED03L);

        Collections.shuffle(shuffled, new Random(seed));

        int targetSize = MOConfig.OFFERS_PER_TIER.get();
        List<Candidate> candidates = buildCandidates(level, tier, seed, shuffled);
        List<StoredTrade> result = new ArrayList<>(Math.min(targetSize, candidates.size()));
        Set<String> usedFamilies = new HashSet<>();
        Map<VillagerProfession, Integer> professionCounts = new HashMap<>();
        Set<Integer> selectedCandidates = new HashSet<>();

        // Pass 1: keep both diversity rules strict.
        selectCandidates(
                candidates,
                result,
                usedFamilies,
                professionCounts,
                selectedCandidates,
                targetSize,
                true,
                true
        );

        // Pass 2: if the pool is small, relax only the profession cap while
        // still guaranteeing one representative per color family.
        if (result.size() < targetSize) {
            selectCandidates(
                    candidates,
                    result,
                    usedFamilies,
                    professionCounts,
                    selectedCandidates,
                    targetSize,
                    true,
                    false
            );
        }

        // Pass 3: last-resort fill. This keeps small/modded trade pools from
        // producing a half-empty board. Exact duplicate definitions are still
        // rejected, but family duplicates may appear only when necessary.
        if (result.size() < targetSize) {
            selectCandidates(
                    candidates,
                    result,
                    usedFamilies,
                    professionCounts,
                    selectedCandidates,
                    targetSize,
                    false,
                    false
            );
        }

        return List.copyOf(result);
    }

    private static List<Candidate> buildCandidates(
            ServerLevel level,
            int tier,
            long seed,
            List<TradeSource> shuffled
    ) {
        List<Candidate> candidates = new ArrayList<>(shuffled.size());
        Villager dummy = new Villager(EntityType.VILLAGER, level);
        int attempt = 0;

        for (TradeSource source : shuffled) {
            try {
                dummy.setVillagerData(
                        dummy.getVillagerData()
                                .setProfession(source.profession())
                                .setLevel(tier + 1)
                );

                MerchantOffer offer = source.listing().getOffer(
                        dummy,
                        RandomSource.create(seed + (++attempt * 341873128712L))
                );

                if (offer == null) {
                    continue;
                }

                StoredTrade stored = new StoredTrade(
                        offer.getCostA(),
                        offer.getCostB(),
                        offer.getResult(),
                        offer.getMaxUses(),
                        offer.getXp()
                );

                if (!stored.isValid()) {
                    continue;
                }

                String familyKey = colorFamilyKey(stored);
                candidates.add(new Candidate(source.profession(), stored, familyKey));
            } catch (Throwable throwable) {
                MerchantOrdersMod.LOGGER.debug(
                        "Skipped incompatible trade from villager profession {}",
                        source.profession().name(),
                        throwable
                );
            }
        }

        return candidates;
    }

    private static void selectCandidates(
            List<Candidate> candidates,
            List<StoredTrade> result,
            Set<String> usedFamilies,
            Map<VillagerProfession, Integer> professionCounts,
            Set<Integer> selectedCandidates,
            int targetSize,
            boolean enforceFamilyLimit,
            boolean enforceProfessionLimit
    ) {
        for (int i = 0; i < candidates.size() && result.size() < targetSize; i++) {
            if (selectedCandidates.contains(i)) {
                continue;
            }

            Candidate candidate = candidates.get(i);
            StoredTrade stored = candidate.trade();

            if (result.stream().anyMatch(stored::sameDefinition)) {
                selectedCandidates.add(i);
                continue;
            }

            if (enforceFamilyLimit
                    && candidate.familyKey() != null
                    && usedFamilies.contains(candidate.familyKey())) {
                continue;
            }

            int professionCount = professionCounts.getOrDefault(candidate.profession(), 0);
            if (enforceProfessionLimit && professionCount >= MAX_OFFERS_PER_PROFESSION) {
                continue;
            }

            result.add(stored);
            selectedCandidates.add(i);
            professionCounts.put(candidate.profession(), professionCount + 1);
            if (candidate.familyKey() != null) {
                usedFamilies.add(candidate.familyKey());
            }
        }
    }

    /**
     * Returns a stable key when this trade is just a color variation of another
     * trade. The 16 vanilla dye prefixes are intentionally recognized for every
     * namespace, so modded items such as "example:red_marble" and
     * "example:blue_marble" are grouped automatically as well.
     *
     * The rest of the trade signature (cost/result counts and component patches)
     * stays in the key, so different prices, quantities or component-bearing
     * variants remain separate trade families.
     */
    private static String colorFamilyKey(StoredTrade trade) {
        NormalizedStack costA = normalizeStack(trade.costA());
        NormalizedStack costB = normalizeStack(trade.costB());
        NormalizedStack result = normalizeStack(trade.result());

        if (!costA.colorVariant() && !costB.colorVariant() && !result.colorVariant()) {
            return null;
        }

        return costA.signature() + "|" + costB.signature() + "|" + result.signature();
    }

    private static NormalizedStack normalizeStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return new NormalizedStack("empty", false);
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        String normalizedPath = path;
        boolean colorVariant = false;

        for (String prefix : COLOR_PREFIXES) {
            if (path.startsWith(prefix) && path.length() > prefix.length()) {
                normalizedPath = "{color}_" + path.substring(prefix.length());
                colorVariant = true;
                break;
            }
        }

        String signature = id.getNamespace()
                + ":"
                + normalizedPath
                + "x"
                + stack.getCount()
                + "#"
                + stack.getComponentsPatch().hashCode();

        return new NormalizedStack(signature, colorVariant);
    }

    private record TradeSource(
            VillagerProfession profession,
            VillagerTrades.ItemListing listing
    ) {
    }

    private record Candidate(
            VillagerProfession profession,
            StoredTrade trade,
            String familyKey
    ) {
    }

    private record NormalizedStack(
            String signature,
            boolean colorVariant
    ) {
    }
}
