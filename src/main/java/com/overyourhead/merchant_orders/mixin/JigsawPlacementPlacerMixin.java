package com.overyourhead.merchant_orders.mixin;

import com.mojang.datafixers.util.Either;
import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.config.MOConfig;
import com.overyourhead.merchant_orders.common.worldgen.VillagePoolIntegrations;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public class JigsawPlacementPlacerMixin {
    @Unique
    private static final ResourceLocation MERCHANT_ORDERS$ORDER_BOARD_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, "village/order_board");

    @Final @Shadow private Registry<StructureTemplatePool> pools;
    @Final @Shadow private RandomSource random;
    @Final @Shadow private List<? super PoolElementStructurePiece> pieces;

    @Unique private boolean merchantOrders$spawnDecisionMade;
    @Unique private boolean merchantOrders$shouldSpawn;

    @ModifyArg(
            method = "tryPlacingChildren(Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IZLnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;")
    )
    private ResourceKey<StructureTemplatePool> merchantOrders$selectOrderBoardPool(ResourceKey<StructureTemplatePool> sourcePoolKey) {
        if (!MOConfig.SPAWN_IN_VILLAGES.get() || merchantOrders$hasPlacedOrderBoard()) {
            return sourcePoolKey;
        }

        ResourceLocation replacement = VillagePoolIntegrations.replacementFor(sourcePoolKey.location());
        if (replacement == null) {
            return sourcePoolKey;
        }

        if (!merchantOrders$spawnDecisionMade) {
            merchantOrders$makeSpawnDecision();
        }
        if (!merchantOrders$shouldSpawn) {
            return sourcePoolKey;
        }

        ResourceKey<StructureTemplatePool> replacementKey = ResourceKey.create(Registries.TEMPLATE_POOL, replacement);
        return pools.getHolder(replacementKey).isPresent() ? replacementKey : sourcePoolKey;
    }

    @Unique
    private void merchantOrders$makeSpawnDecision() {
        merchantOrders$spawnDecisionMade = true;
        double chance = MOConfig.VILLAGE_SPAWN_CHANCE_PERCENT.get() / 100.0D;
        merchantOrders$shouldSpawn = random.nextDouble() < chance;
    }

    @Unique
    private boolean merchantOrders$isOrderBoardElement(StructurePoolElement element) {
        if (!(element instanceof SinglePoolElement singlePoolElement)) {
            return false;
        }
        Either<ResourceLocation, StructureTemplate> template =
                ((SinglePoolElementAccessor) singlePoolElement).merchantOrders$getTemplate();
        return template.left().filter(MERCHANT_ORDERS$ORDER_BOARD_STRUCTURE::equals).isPresent();
    }

    @Unique
    private boolean merchantOrders$hasPlacedOrderBoard() {
        for (Object candidate : pieces) {
            if (candidate instanceof PoolElementStructurePiece piece
                    && merchantOrders$isOrderBoardElement(piece.getElement())) {
                return true;
            }
        }
        return false;
    }
}
