package com.overyourhead.merchant_orders.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the backing structure id of vanilla single/legacy-single pool elements.
 * LegacySinglePoolElement extends SinglePoolElement, so the same accessor works
 * for the order-board pool used by village generation.
 */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
    @Accessor("template")
    Either<ResourceLocation, StructureTemplate> merchantOrders$getTemplate();
}
