package com.overyourhead.merchant_orders.common.event;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.player.PlayerOrderData;
import com.overyourhead.merchant_orders.common.sale.PlayerSaleData;
import com.overyourhead.merchant_orders.common.trade.TradePoolRegistry;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = MerchantOrdersMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class CommonEvents {
    private static final int CATALOG_WARMUP_INTERVAL_TICKS = 1;
    private static final int CATALOG_BACKGROUND_CANDIDATES_PER_TICK = 2;
    private static final int CATALOG_URGENT_CANDIDATES_PER_TICK = 4;
    private static final long CATALOG_BACKGROUND_NANOS_PER_TICK = 150_000L;
    private static final long CATALOG_URGENT_NANOS_PER_TICK = 300_000L;
    private static final int CATALOG_BACKGROUND_STEP_INTERVAL_TICKS = 6;
    private static final int CATALOG_URGENT_STEP_INTERVAL_TICKS = 2;
    private static final int CATALOG_WARMUP_DELAY_TICKS = 100;
    private static final int CATALOG_LOGIN_WARMUP_DELAY_TICKS = 800;
    private static final Map<WarmupKey, CatalogWarmup> CATALOG_WARMUPS = new HashMap<>();

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        TradePoolRegistry.capture(event);
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(
                MOConfig.TERMINAL_PRICE.get(),
                new ItemStack(MOBlocks.ORDER_TERMINAL.get()),
                MOConfig.TERMINAL_MAX_USES.get(),
                10
        ));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            queueCatalogWarmupDelayed(player, CATALOG_LOGIN_WARMUP_DELAY_TICKS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        long cycle = gameTime / MOConfig.catalogRefreshTicks();
        WarmupKey key = new WarmupKey(player.level().dimension(), cycle);
        removeOldWarmups(player.level().dimension(), cycle);
        CatalogWarmup warmup = CATALOG_WARMUPS.get(key);
        if (warmup == null) {
            return;
        }
        if (player.tickCount % CATALOG_WARMUP_INTERVAL_TICKS != 0) {
            return;
        }
        if (warmup.lastProcessedGameTime == gameTime) {
            return;
        }
        warmup.lastProcessedGameTime = gameTime;
        boolean urgentWork = warmup.currentBuild != null
                ? warmup.currentUrgent
                : !warmup.urgentTiers.isEmpty();
        int interval = urgentWork
                ? CATALOG_URGENT_STEP_INTERVAL_TICKS
                : CATALOG_BACKGROUND_STEP_INTERVAL_TICKS;
        if (gameTime % interval != 0) {
            return;
        }
        try {
            if (warmup.currentBuild == null) {
                warmup.startNextBuild(player);
            }
            if (warmup.currentBuild != null
                    && warmup.currentBuild.step(
                            warmup.currentUrgent
                            ? CATALOG_URGENT_CANDIDATES_PER_TICK
                            : CATALOG_BACKGROUND_CANDIDATES_PER_TICK,
                            warmup.currentUrgent
                                    ? CATALOG_URGENT_NANOS_PER_TICK
                                    : CATALOG_BACKGROUND_NANOS_PER_TICK
                    )) {
                warmup.currentBuild = null;
                warmup.currentTier = -1;
            }
        } catch (RuntimeException exception) {
            CATALOG_WARMUPS.remove(key);
            MerchantOrdersMod.LOGGER.debug("Stopped order catalog warmup for {}", player.getGameProfile().getName(), exception);
            return;
        }

        if (warmup.currentBuild == null && warmup.urgentTiers.isEmpty() && warmup.backgroundTiers.isEmpty()) {
            CATALOG_WARMUPS.remove(key);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer clone) {
            PlayerOrderData.copyPersistentData(original, clone);
            PlayerSaleData.copy(original, clone);
        }
    }

    public static void queueCatalogWarmup(ServerPlayer player) {
        queueCatalogWarmupDelayed(player, CATALOG_WARMUP_DELAY_TICKS);
    }

    private static void queueCatalogWarmupDelayed(ServerPlayer player, int delayTicks) {
        long cycle = player.serverLevel().getGameTime() / MOConfig.catalogRefreshTicks();
        WarmupKey key = new WarmupKey(player.level().dimension(), cycle);
        removeOldWarmups(player.level().dimension(), cycle);
        ArrayDeque<Integer> tiers = buildWarmupTiers();

        CatalogWarmup warmup = CATALOG_WARMUPS.get(key);
        if (warmup == null) {
            CATALOG_WARMUPS.put(key, new CatalogWarmup(cycle, tiers, delayTicks));
            return;
        }

        while (!tiers.isEmpty()) {
            int tier = tiers.pollFirst();
            if (tier != warmup.currentTier
                    && !warmup.urgentTiers.contains(tier)
                    && !warmup.backgroundTiers.contains(tier)
                    && !TradePoolRegistry.isCatalogCached(player.serverLevel(), tier, cycle)) {
                warmup.backgroundTiers.addLast(tier);
            }
        }
        warmup.delayTicks = Math.min(warmup.delayTicks, delayTicks);
    }

    public static void queueCatalogWarmup(ServerPlayer player, int priorityTier) {
        long cycle = player.serverLevel().getGameTime() / MOConfig.catalogRefreshTicks();
        WarmupKey key = new WarmupKey(player.level().dimension(), cycle);
        removeOldWarmups(player.level().dimension(), cycle);
        if (priorityTier < 0 || priorityTier >= MOConstants.TIER_COUNT) {
            return;
        }

        CatalogWarmup warmup = CATALOG_WARMUPS.get(key);
        if (warmup == null) {
            warmup = new CatalogWarmup(cycle, buildWarmupTiers(), CATALOG_WARMUP_DELAY_TICKS);
            CATALOG_WARMUPS.put(key, warmup);
        }

        warmup.backgroundTiers.remove(priorityTier);
        warmup.urgentTiers.remove(priorityTier);
        if (!TradePoolRegistry.isCatalogCached(player.serverLevel(), priorityTier, cycle)) {
            warmup.pauseBackgroundBuild();
            warmup.delayTicks = CATALOG_WARMUP_DELAY_TICKS;
            warmup.urgentTiers.addFirst(priorityTier);
        }
    }

    public static boolean isCatalogMarketReady(ServerLevel level) {
        long cycle = level.getGameTime() / MOConfig.catalogRefreshTicks();
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            if (!TradePoolRegistry.isCatalogCached(level, tier, cycle)) {
                return false;
            }
        }
        return true;
    }

    private static ArrayDeque<Integer> buildWarmupTiers() {
        ArrayDeque<Integer> tiers = new ArrayDeque<>();
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            tiers.add(tier);
        }
        return tiers;
    }

    private static void removeOldWarmups(ResourceKey<Level> dimension, long currentCycle) {
        CATALOG_WARMUPS.keySet().removeIf(key -> key.dimension().equals(dimension) && key.catalogCycle() != currentCycle);
    }

    private static final class CatalogWarmup {
        private final long catalogCycle;
        private final ArrayDeque<Integer> urgentTiers = new ArrayDeque<>();
        private final ArrayDeque<Integer> backgroundTiers;
        private int delayTicks;
        private TradePoolRegistry.CatalogBuild currentBuild;
        private boolean currentUrgent;
        private int currentTier = -1;
        private long lastProcessedGameTime = -1L;

        private CatalogWarmup(long catalogCycle, ArrayDeque<Integer> backgroundTiers, int delayTicks) {
            this.catalogCycle = catalogCycle;
            this.backgroundTiers = backgroundTiers;
            this.delayTicks = delayTicks;
        }

        private void startNextBuild(ServerPlayer player) {
            if (startNextBuild(player, urgentTiers, true)) {
                return;
            }
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }
            startNextBuild(player, backgroundTiers, false);
        }

        private void pauseBackgroundBuild() {
            if (currentBuild == null || currentUrgent || currentTier < 0) {
                return;
            }

            backgroundTiers.remove(currentTier);
            backgroundTiers.addLast(currentTier);
            currentBuild = null;
            currentTier = -1;
        }

        private boolean startNextBuild(ServerPlayer player, ArrayDeque<Integer> tiers, boolean urgent) {
            while (!tiers.isEmpty()) {
                int tier = tiers.pollFirst();
                if (TradePoolRegistry.isCatalogCached(player.serverLevel(), tier, catalogCycle)) {
                    continue;
                }

                currentBuild = TradePoolRegistry.beginCatalogBuild(player.serverLevel(), tier, catalogCycle);
                currentUrgent = urgent;
                currentTier = tier;
                if (!currentBuild.step(0)) {
                    return true;
                }
                currentBuild = null;
                currentTier = -1;
            }
            return false;
        }
    }

    private record WarmupKey(ResourceKey<Level> dimension, long catalogCycle) {
    }
}
