package com.overyourhead.merchant_orders.common.trade;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TradePoolRegistry {
    private static final List<List<TradeSource>> SOURCES = createTierLists();

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

        List<StoredTrade> result = new ArrayList<>();
        Villager dummy = new Villager(EntityType.VILLAGER, level);
        int attempt = 0;

        for (TradeSource source : shuffled) {
            if (result.size() >= MOConfig.OFFERS_PER_TIER.get()) {
                break;
            }

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

                if (stored.isValid() && result.stream().noneMatch(stored::sameDefinition)) {
                    result.add(stored);
                }
            } catch (Throwable throwable) {
                MerchantOrdersMod.LOGGER.debug(
                        "Skipped incompatible trade from villager profession {}",
                        source.profession().name(),
                        throwable
                );
            }
        }

        return List.copyOf(result);
    }

    private record TradeSource(
            VillagerProfession profession,
            VillagerTrades.ItemListing listing
    ) {
    }
}
