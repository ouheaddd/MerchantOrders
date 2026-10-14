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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class TradePoolRegistry {
    private static final List<List<TradeSource>> SOURCES = createTierLists();
    private static final int MAX_OFFERS_PER_PROFESSION = 4;
    private static final int CATALOG_SCAN_WINDOW = 160;
    private static final int CATALOG_CACHE_LIMIT = 512;
    private static final long SLOW_CANDIDATE_LOG_NANOS = 50_000_000L;
    private static final Set<TradeSource> SLOW_SOURCES = new HashSet<>();
    private static final Map<CatalogKey, List<StoredTrade>> CATALOG_CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CatalogKey, List<StoredTrade>> eldest) {
            return size() > CATALOG_CACHE_LIMIT;
        }
    };
    private static int sourceVersion;

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
        sourceVersion++;
        CATALOG_CACHE.clear();
        SLOW_SOURCES.clear();
    }

    public static List<StoredTrade> createCatalog(ServerLevel level, ServerPlayer player, int tier, long cycle) {
        CatalogBuild build = beginCatalogBuild(level, tier, cycle);
        while (!build.step(Integer.MAX_VALUE)) {
            // Complete the synchronous request in one call. Background warmup
            // uses the same builder with a small per-tick budget.
        }
        return build.catalog();
    }

    public static CatalogBuild beginCatalogBuild(ServerLevel level, ServerPlayer player, int tier, long cycle) {
        return beginCatalogBuild(level, tier, cycle);
    }

    public static CatalogBuild beginCatalogBuild(ServerLevel level, int tier, long cycle) {
        if (tier < 0 || tier >= MOConstants.TIER_COUNT) {
            return CatalogBuild.completed(List.of());
        }

        int targetSize = MOConfig.OFFERS_PER_TIER.get();
        long seed = level.getSeed()
                ^ (cycle * 0x9E3779B97F4A7C15L)
                ^ ((long) tier * 0xD1B54A32D192ED03L);

        List<TradeSource> shuffled;
        int capturedVersion;
        synchronized (TradePoolRegistry.class) {
            capturedVersion = sourceVersion;
            CatalogKey key = new CatalogKey(level.getSeed(), tier, cycle, targetSize, capturedVersion);
            List<StoredTrade> cached = CATALOG_CACHE.get(key);
            if (cached != null) {
                return CatalogBuild.completed(cached);
            }
            shuffled = new ArrayList<>(SOURCES.get(tier));
        }

        if (shuffled.isEmpty()) {
            MerchantOrdersMod.LOGGER.warn(
                    "No villager trades were captured for terminal tier {}. "
                            + "Check that TradePoolRegistry.capture is subscribed to VillagerTradesEvent.",
                    tier + 1
            );
            return CatalogBuild.completed(List.of());
        }

        Collections.shuffle(shuffled, new Random(seed));
        CatalogKey key = new CatalogKey(level.getSeed(), tier, cycle, targetSize, capturedVersion);
        return new CatalogBuild(key, level, tier, seed, shuffled, targetSize);
    }

    public static boolean isCatalogCached(ServerLevel level, ServerPlayer player, int tier, long cycle) {
        return isCatalogCached(level, tier, cycle);
    }

    public static boolean isCatalogCached(ServerLevel level, int tier, long cycle) {
        if (tier < 0 || tier >= MOConstants.TIER_COUNT) {
            return true;
        }

        int targetSize = MOConfig.OFFERS_PER_TIER.get();
        synchronized (TradePoolRegistry.class) {
            CatalogKey key = new CatalogKey(level.getSeed(), tier, cycle, targetSize, sourceVersion);
            return CATALOG_CACHE.containsKey(key);
        }
    }

    private static Candidate getCandidate(
            List<TradeSource> sources,
            Villager dummy,
            int tier,
            long seed,
            int index,
            Map<Integer, Candidate> candidateCache,
            Set<Integer> invalidCandidates
    ) {
        if (invalidCandidates.contains(index)) {
            return null;
        }

        Candidate cached = candidateCache.get(index);
        if (cached != null) {
            return cached;
        }

        TradeSource source = sources.get(index);
        synchronized (TradePoolRegistry.class) {
            if (SLOW_SOURCES.contains(source)) {
                invalidCandidates.add(index);
                return null;
            }
        }
        if (isKnownSlowListing(source)) {
            invalidCandidates.add(index);
            return null;
        }

        long started = System.nanoTime();
        try {
            dummy.setVillagerData(
                    dummy.getVillagerData()
                            .setProfession(source.profession())
                            .setLevel(tier + 1)
            );

            MerchantOffer offer = source.listing().getOffer(
                    dummy,
                    RandomSource.create(seed + ((long) (index + 1) * 341873128712L))
            );

            if (offer == null) {
                invalidCandidates.add(index);
                return null;
            }

            StoredTrade stored = new StoredTrade(
                    offer.getCostA(),
                    offer.getCostB(),
                    offer.getResult(),
                    offer.getMaxUses(),
                    offer.getXp()
            );

            if (!stored.isValid()) {
                invalidCandidates.add(index);
                return null;
            }

            Candidate candidate = new Candidate(source.profession(), stored, colorFamilyKey(stored));
            candidateCache.put(index, candidate);
            return candidate;
        } catch (Throwable throwable) {
            invalidCandidates.add(index);
            MerchantOrdersMod.LOGGER.debug(
                    "Skipped incompatible trade from villager profession {}",
                    source.profession().name(),
                    throwable
            );
            return null;
        } finally {
            long elapsed = System.nanoTime() - started;
            if (elapsed >= SLOW_CANDIDATE_LOG_NANOS) {
                synchronized (TradePoolRegistry.class) {
                    SLOW_SOURCES.add(source);
                }
                MerchantOrdersMod.LOGGER.warn(
                        "Slow order catalog candidate disabled: tier={}, profession={}, index={}, listing={}, elapsed={}ms",
                        tier + 1,
                        source.profession().name(),
                        index,
                        source.listing().getClass().getName(),
                        elapsed / 1_000_000L
                );
            }
        }
    }

    private static boolean isKnownSlowListing(TradeSource source) {
        String listingClass = source.listing().getClass().getName();
        return listingClass.contains("TreasureMapForEmeralds")
                || listingClass.contains("ExplorerMap");
    }

    private static void selectCandidates(
            List<TradeSource> sources,
            Villager dummy,
            int tier,
            long seed,
            List<StoredTrade> result,
            Set<String> usedFamilies,
            Map<VillagerProfession, Integer> professionCounts,
            Set<Integer> selectedCandidates,
            Map<Integer, Candidate> candidateCache,
            Set<Integer> invalidCandidates,
            int targetSize,
            boolean enforceFamilyLimit,
            boolean enforceProfessionLimit
    ) {
        for (int i = 0; i < sources.size() && result.size() < targetSize; i++) {
            if (selectedCandidates.contains(i)) {
                continue;
            }

            Candidate candidate = getCandidate(sources, dummy, tier, seed, i, candidateCache, invalidCandidates);
            if (candidate == null) {
                continue;
            }

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

    public static final class CatalogBuild {
        private static final int STRICT_PASS = 0;
        private static final int RELAX_PROFESSION_PASS = 1;
        private static final int RELAX_ALL_PASS = 2;

        private final CatalogKey key;
        private final List<TradeSource> sources;
        private final Villager dummy;
        private final int tier;
        private final long seed;
        private final int targetSize;
        private final List<StoredTrade> result;
        private final Set<String> usedFamilies = new HashSet<>();
        private final Map<VillagerProfession, Integer> professionCounts = new HashMap<>();
        private final Set<Integer> selectedCandidates = new HashSet<>();
        private final Map<Integer, Candidate> candidateCache = new HashMap<>();
        private final Set<Integer> invalidCandidates = new HashSet<>();
        private int pass;
        private int index;
        private int scanLimit;
        private boolean done;
        private List<StoredTrade> catalog = List.of();

        private CatalogBuild(
                CatalogKey key,
                ServerLevel level,
                int tier,
                long seed,
                List<TradeSource> sources,
                int targetSize
        ) {
            this.key = key;
            this.sources = sources;
            this.dummy = new Villager(EntityType.VILLAGER, level);
            this.tier = tier;
            this.seed = seed;
            this.targetSize = targetSize;
            this.result = new ArrayList<>(Math.min(targetSize, sources.size()));
            this.scanLimit = Math.min(sources.size(), Math.max(targetSize * 8, CATALOG_SCAN_WINDOW));
        }

        private CatalogBuild(List<StoredTrade> catalog) {
            this.key = null;
            this.sources = List.of();
            this.dummy = null;
            this.tier = 0;
            this.seed = 0L;
            this.targetSize = 0;
            this.result = new ArrayList<>();
            this.scanLimit = 0;
            this.done = true;
            this.catalog = catalog;
        }

        private static CatalogBuild completed(List<StoredTrade> catalog) {
            return new CatalogBuild(catalog);
        }

        public boolean step(int maxCandidates) {
            return step(maxCandidates, Long.MAX_VALUE);
        }

        public boolean step(int maxCandidates, long maxNanos) {
            if (done) {
                return true;
            }

            long deadline = maxNanos == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + maxNanos;
            int processed = 0;
            while (processed < maxCandidates && !done && System.nanoTime() < deadline) {
                if (result.size() >= targetSize) {
                    finish();
                    break;
                }

                if (index >= scanLimit) {
                    pass++;
                    if (pass > RELAX_ALL_PASS) {
                        if (scanLimit >= sources.size()) {
                            finish();
                            break;
                        }
                        int previousLimit = scanLimit;
                        this.scanLimit = Math.min(sources.size(), scanLimit + CATALOG_SCAN_WINDOW);
                        index = previousLimit;
                        pass = STRICT_PASS;
                    } else {
                        index = 0;
                    }
                    continue;
                }

                int candidateIndex = index++;
                processed++;
                if (selectedCandidates.contains(candidateIndex)) {
                    continue;
                }

                Candidate candidate = getCandidate(
                        sources,
                        dummy,
                        tier,
                        seed,
                        candidateIndex,
                        candidateCache,
                        invalidCandidates
                );
                if (candidate == null) {
                    continue;
                }

                StoredTrade stored = candidate.trade();
                if (result.stream().anyMatch(stored::sameDefinition)) {
                    selectedCandidates.add(candidateIndex);
                    continue;
                }

                boolean enforceFamilyLimit = pass == STRICT_PASS || pass == RELAX_PROFESSION_PASS;
                if (enforceFamilyLimit
                        && candidate.familyKey() != null
                        && usedFamilies.contains(candidate.familyKey())) {
                    continue;
                }

                int professionCount = professionCounts.getOrDefault(candidate.profession(), 0);
                if (pass == STRICT_PASS && professionCount >= MAX_OFFERS_PER_PROFESSION) {
                    continue;
                }

                result.add(stored);
                selectedCandidates.add(candidateIndex);
                professionCounts.put(candidate.profession(), professionCount + 1);
                if (candidate.familyKey() != null) {
                    usedFamilies.add(candidate.familyKey());
                }
            }

            return done;
        }

        public List<StoredTrade> catalog() {
            return catalog;
        }

        private void finish() {
            List<StoredTrade> completed = List.copyOf(result);
            synchronized (TradePoolRegistry.class) {
                List<StoredTrade> cached = CATALOG_CACHE.get(key);
                if (cached != null) {
                    catalog = cached;
                } else {
                    CATALOG_CACHE.put(key, completed);
                    catalog = completed;
                }
            }
            done = true;
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

    private record CatalogKey(
            long levelSeed,
            int tier,
            long cycle,
            int targetSize,
            int sourceVersion
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
