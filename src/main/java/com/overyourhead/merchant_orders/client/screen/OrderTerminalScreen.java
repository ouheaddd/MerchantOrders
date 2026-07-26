package com.overyourhead.merchant_orders.client.screen;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.menu.OrderTerminalMenu;
import com.overyourhead.merchant_orders.core.registry.MOBlocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class OrderTerminalScreen extends AbstractContainerScreen<OrderTerminalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MerchantOrdersMod.MOD_ID,
            "textures/gui/order_terminal.png"
    );
    private static final ResourceLocation[] TIER_TEXTURES = {
            tierTexture(0), tierTexture(1), tierTexture(2), tierTexture(3), tierTexture(4)
    };
    private static final int VISIBLE_ROWS = 6;
    private static final int TRADE_X = 45;
    private static final int TRADE_Y = 23;
    private static final int TRADE_W = 128;
    private static final int TRADE_H = 18;
    private int scrollOffset;

    private static ResourceLocation tierTexture(int tier) {
        return ResourceLocation.fromNamespaceAndPath(
                MerchantOrdersMod.MOD_ID,
                "textures/gui/tier_" + tier + ".png"
        );
    }

    public OrderTerminalScreen(OrderTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 240;
        inventoryLabelX = 82;
        inventoryLabelY = 146;
        titleLabelX = 45;
        titleLabelY = 7;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.blit(TEXTURE, left, top, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        int unlocked = menu.unlockedTier();
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int y = top + 22 + tier * 25;
            int color = tier == menu.selectedTier() ? 0xFFB6D37A : (tier <= unlocked ? 0xFF8D7650 : 0xFF494949);
            graphics.fill(left + 8, y, left + 37, y + 21, color);
            graphics.blit(TIER_TEXTURES[tier], left + 14, y + 3, 0, 0, 16, 16, 16, 16);
            if (tier > unlocked) {
                graphics.fill(left + 8, y, left + 37, y + 21, 0x99000000);
                graphics.drawCenteredString(font, "x", left + 22, y + 7, 0xFFBBBBBB);
            }
        }

        int nextThreshold = menu.selectedTier() >= MOConstants.TIER_COUNT - 1
                ? MOConstants.XP_THRESHOLDS[MOConstants.TIER_COUNT - 1]
                : MOConstants.XP_THRESHOLDS[menu.selectedTier() + 1];
        int previousThreshold = MOConstants.XP_THRESHOLDS[menu.selectedTier()];
        int span = Math.max(1, nextThreshold - previousThreshold);
        int progress = Math.max(0, Math.min(100, (menu.xp() - previousThreshold) * 100 / span));
        graphics.fill(left + 184, top + 16, left + 302, top + 23, 0xFF3B342B);
        graphics.fill(left + 185, top + 17, left + 185 + (116 * progress / 100), top + 22, 0xFF63A64B);

        renderTradeRows(graphics, mouseX, mouseY);

        int sackColor = menu.basketSlots() > 0 ? 0xFFB98543 : 0xFF5A5148;
        graphics.fill(left + 271, top + 42, left + 309, top + 82, sackColor);
        ItemStack sackPreview = new ItemStack(MOBlocks.ORDER_SACK_ITEM.get());
        graphics.renderItem(sackPreview, left + 282, top + 48);
        if (menu.basketSlots() > 0) {
            graphics.drawCenteredString(font, Integer.toString(menu.basketItems()), left + 290, top + 68, 0xFFFFFF66);
        } else {
            graphics.fill(left + 271, top + 42, left + 309, top + 82, 0x66000000);
        }
    }

    private void renderTradeRows(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = leftPos + TRADE_X;
        int top = topPos + TRADE_Y;
        int maxOffset = Math.max(0, menu.catalogSize() - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxOffset);

        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int offer = scrollOffset + visible;
            int y = top + visible * TRADE_H;
            boolean selected = offer == menu.selectedOffer();
            int bg = selected ? 0xFF8DAA69 : 0xFF6B6255;
            graphics.fill(left, y, left + TRADE_W, y + 16, bg);
            if (offer >= menu.catalogSize()) {
                continue;
            }

            ItemStack costA = menu.getCatalogCostA(offer);
            ItemStack costB = menu.getCatalogCostB(offer);
            ItemStack result = menu.getCatalogResult(offer);
            graphics.renderItem(costA, left + 3, y);
            graphics.renderItemDecorations(font, costA, left + 3, y);
            if (!costB.isEmpty()) {
                graphics.renderItem(costB, left + 27, y);
                graphics.renderItemDecorations(font, costB, left + 27, y);
            }
            graphics.drawString(font, ">", left + 52, y + 4, 0xFFFFFFFF, false);
            graphics.renderItem(result, left + 66, y);
            graphics.renderItemDecorations(font, result, left + 66, y);

            int uses = menu.uses(offer);
            int max = menu.maxUses(offer);
            graphics.drawString(font, uses + "/" + max, left + 90, y + 4, uses >= max ? 0xFFFF6666 : 0xFFE8E0D0, false);
            if (uses >= max && max > 0) {
                graphics.fill(left, y, left + TRADE_W, y + 16, 0x66000000);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF3A2A1C, false);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.tier." + menu.selectedTier()), 184, 7, 0xFF3A2A1C, false);
        graphics.drawString(font, menu.xp() + " XP", 224, 7, 0xFF3A2A1C, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3A2A1C, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoveredCatalogTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderHoveredCatalogTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int tabY = 22 + tier * 25;
            if (localX >= 8 && localX < 37 && localY >= tabY && localY < tabY + 21) {
                Component tierName = Component.translatable("screen.merchant_orders.tier." + tier);
                Component tooltip = tier <= menu.unlockedTier()
                        ? Component.translatable("tooltip.merchant_orders.tier_open", tierName)
                        : Component.translatable("tooltip.merchant_orders.tier_locked", MOConstants.XP_THRESHOLDS[tier]);
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
        if (localX < TRADE_X || localX >= TRADE_X + TRADE_W || localY < TRADE_Y || localY >= TRADE_Y + VISIBLE_ROWS * TRADE_H) {
            if (localX >= 271 && localX < 309 && localY >= 42 && localY < 82) {
                graphics.renderTooltip(font, Component.translatable(
                        menu.basketSlots() > 0 ? "tooltip.merchant_orders.claim" : "tooltip.merchant_orders.empty_basket",
                        menu.basketItems()
                ), mouseX, mouseY);
            }
            return;
        }
        int offer = scrollOffset + (localY - TRADE_Y) / TRADE_H;
        if (offer >= menu.catalogSize()) {
            return;
        }
        int xInRow = localX - TRADE_X;
        ItemStack hovered = xInRow < 23
                ? menu.getCatalogCostA(offer)
                : (xInRow < 48 ? menu.getCatalogCostB(offer) : menu.getCatalogResult(offer));
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;

        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int tabY = 22 + tier * 25;
            if (x >= 8 && x < 37 && y >= tabY && y < tabY + 21) {
                sendButton(OrderTerminalMenu.BUTTON_TIER_BASE + tier);
                return true;
            }
        }

        if (x >= TRADE_X && x < TRADE_X + TRADE_W && y >= TRADE_Y && y < TRADE_Y + VISIBLE_ROWS * TRADE_H) {
            int offer = scrollOffset + (y - TRADE_Y) / TRADE_H;
            if (offer < menu.catalogSize()) {
                sendButton(OrderTerminalMenu.BUTTON_OFFER_BASE + offer);
            }
            return true;
        }

        if (x >= 240 && x < 267 && y >= 40 && y < 68) {
            sendButton(Screen.hasShiftDown() ? OrderTerminalMenu.BUTTON_EXECUTE_MAX : OrderTerminalMenu.BUTTON_EXECUTE_ONE);
            return true;
        }

        if (x >= 271 && x < 309 && y >= 42 && y < 82) {
            sendButton(OrderTerminalMenu.BUTTON_CLAIM);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, menu.catalogSize() - VISIBLE_ROWS);
        if (maxOffset > 0) {
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
