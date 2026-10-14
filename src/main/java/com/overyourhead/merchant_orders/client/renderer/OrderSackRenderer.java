package com.overyourhead.merchant_orders.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.overyourhead.merchant_orders.common.block.entity.OrderSackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public final class OrderSackRenderer implements BlockEntityRenderer<OrderSackBlockEntity> {
    private static final float NAME_TAG_SCALE = 0.025F;
    private static final double MAX_NAME_TAG_DISTANCE_SQR = 64.0D * 64.0D;
    private static final double NAME_TAG_X = 0.5D;
    private static final double NAME_TAG_Y = 1.35D;
    private static final double NAME_TAG_Z = 0.5D;

    private final Font font;
    private final EntityRenderDispatcher entityRenderer;

    public OrderSackRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(
            OrderSackBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Component name = blockEntity.getCustomName();
        if (name == null || name.getString().isBlank()) {
            return;
        }
        if (entityRenderer.distanceToSqr(
                blockEntity.getBlockPos().getX() + NAME_TAG_X,
                blockEntity.getBlockPos().getY() + NAME_TAG_Y,
                blockEntity.getBlockPos().getZ() + NAME_TAG_Z
        ) > MAX_NAME_TAG_DISTANCE_SQR) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(NAME_TAG_X, NAME_TAG_Y, NAME_TAG_Z);
        poseStack.mulPose(entityRenderer.cameraOrientation());
        poseStack.scale(NAME_TAG_SCALE, -NAME_TAG_SCALE, NAME_TAG_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(name) / 2.0F;
        int background = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(
                name,
                x,
                0.0F,
                553648127,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                background,
                packedLight
        );
        font.drawInBatch(
                name,
                x,
                0.0F,
                -1,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                packedLight
        );
        poseStack.popPose();
    }
}
