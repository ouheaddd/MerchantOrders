package com.overyourhead.merchant_orders.client.screen;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.MOConstants;
import com.overyourhead.merchant_orders.common.menu.OrderTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class OrderTerminalScreen extends AbstractContainerScreen<OrderTerminalMenu> {
    private static final ResourceLocation BACKGROUND = gui("order_terminal.png");
    private static final ResourceLocation CURRENT_TRADE_PANEL = gui("current_trade_panel.png");
    private static final ResourceLocation BASKET_PANEL = gui("basket_panel.png");
    private static final ResourceLocation BASKET_PANEL_HOVER = gui("basket_panel_hover.png");
    private static final ResourceLocation ORDER_BASKET = gui("order_basket.png");
    private static final ResourceLocation XP_BAR_BACKGROUND = gui("xp_bar_background.png");
    private static final ResourceLocation XP_BAR_FILL = gui("xp_bar_fill.png");
    private static final ResourceLocation TRADE_ROW = gui("trade_row.png");
    private static final ResourceLocation TRADE_ROW_SELECTED = gui("trade_row_selected.png");
    private static final ResourceLocation TIER_SELECTED_OVERLAY = gui("tier_selected_overlay.png");
    private static final ResourceLocation TIER_LOCKED_OVERLAY = gui("tier_locked_overlay.png");
    private static final ResourceLocation SEND_ORDER = gui("send_order_button.png");
    private static final ResourceLocation SEND_ORDER_HOVER = gui("send_order_button_hover.png");
    private static final ResourceLocation SEND_ORDER_DISABLED = gui("send_order_button_disabled.png");
    private static final ResourceLocation SCROLL_TRACK = gui("scrollbar_track.png");
    private static final ResourceLocation SCROLL_THUMB = gui("scrollbar_thumb.png");

    private static final ResourceLocation[] TIER_TEXTURES = {
            gui("tier_0.png"), gui("tier_1.png"), gui("tier_2.png"), gui("tier_3.png"), gui("tier_4.png")
    };

    private static final int TIER_X = 8;
    private static final int TIER_Y = 26;
    private static final int TIER_W = 38;
    private static final int TIER_H = 36;
    private static final int TIER_GAP = 3;

    private static final int VISIBLE_ROWS = 8;
    private static final int TRADE_X = 52;
    private static final int TRADE_Y = 26;
    private static final int TRADE_W = 128;
    private static final int TRADE_H = 24;

    private static final int SCROLL_X = 180;
    private static final int SCROLL_Y = 26;
    private static final int SCROLL_W = 4;
    private static final int SCROLL_H = 192;
    private static final int SCROLL_THUMB_H = 24;

    private static final int CURRENT_X = 196;
    private static final int CURRENT_Y = 29;
    private static final int CURRENT_W = 103;
    private static final int CURRENT_H = 81;

    private static final int BASKET_X = 301;
    private static final int BASKET_Y = 29;
    private static final int BASKET_W = 71;
    private static final int BASKET_H = 81;

    private static final int BASKET_PREVIEW_W = 176;
    private static final int BASKET_PREVIEW_H = 96;
    private static final int BASKET_PREVIEW_SLOT_X = 8;
    private static final int BASKET_PREVIEW_SLOT_Y = 18;

    private static final int BUTTON_X = 196;
    private static final int BUTTON_Y = 113;
    private static final int BUTTON_W = 176;
    private static final int BUTTON_H = 22;

    private static final int XP_X = 196;
    private static final int XP_Y = 19;
    private static final int XP_W = 176;
    private static final int XP_H = 7;
    private static final int XP_FILL_W = 174;
    private static final int XP_FILL_H = 5;

    private int scrollOffset;
    private boolean basketOpen;

    private static ResourceLocation gui(String file) {
        return ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, "textures/gui/" + file);
    }

    public OrderTerminalScreen(OrderTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 380;
        imageHeight = 240;
        titleLabelX = 86;
        titleLabelY = 7;
        inventoryLabelX = -1000;
        inventoryLabelY = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        blit(graphics, CURRENT_TRADE_PANEL, CURRENT_X, CURRENT_Y, CURRENT_W, CURRENT_H);
        boolean basketHovered = !basketOpen
                && mouseX >= leftPos + BASKET_X && mouseX < leftPos + BASKET_X + BASKET_W
                && mouseY >= topPos + BASKET_Y && mouseY < topPos + BASKET_Y + BASKET_H;
        blit(graphics, basketHovered ? BASKET_PANEL_HOVER : BASKET_PANEL,
                BASKET_X, BASKET_Y, BASKET_W, BASKET_H);

        renderTierTabs(graphics);
        renderProgressBar(graphics);
        renderTradeRows(graphics);
        renderBasketCounters(graphics);
        renderOrderButton(graphics, mouseX, mouseY);
        renderScrollBar(graphics);
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, leftPos + x, topPos + y, 0, 0, width, height, width, height);
    }

    private void renderTierTabs(GuiGraphics graphics) {
        int unlocked = menu.unlockedTier();
        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int x = TIER_X;
            int y = TIER_Y + tier * (TIER_H + TIER_GAP);
            blit(graphics, TIER_TEXTURES[tier], x, y, TIER_W, TIER_H);

            if (tier == menu.selectedTier()) {
                blit(graphics, TIER_SELECTED_OVERLAY, x, y, TIER_W, TIER_H);
            }
            if (tier > unlocked) {
                blit(graphics, TIER_LOCKED_OVERLAY, x, y, TIER_W, TIER_H);
            }
        }
    }

    private void renderProgressBar(GuiGraphics graphics) {
        int selectedTier = menu.selectedTier();
        int previousThreshold = MOConstants.XP_THRESHOLDS[selectedTier];
        int nextThreshold = selectedTier >= MOConstants.TIER_COUNT - 1
                ? previousThreshold
                : MOConstants.XP_THRESHOLDS[selectedTier + 1];
        int span = Math.max(1, nextThreshold - previousThreshold);
        int progress = selectedTier >= MOConstants.TIER_COUNT - 1
                ? 100
                : Math.max(0, Math.min(100, (menu.xp() - previousThreshold) * 100 / span));

        blit(graphics, XP_BAR_BACKGROUND, XP_X, XP_Y, XP_W, XP_H);
        int fillWidth = XP_FILL_W * progress / 100;
        if (fillWidth > 0) {
            graphics.blit(XP_BAR_FILL, leftPos + XP_X + 1, topPos + XP_Y + 1,
                    0, 0, fillWidth, XP_FILL_H, XP_FILL_W, XP_FILL_H);
        }
    }

    private void renderTradeRows(GuiGraphics graphics) {
        int maxOffset = Math.max(0, menu.catalogSize() - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxOffset);

        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int offer = scrollOffset + visible;
            int y = TRADE_Y + visible * TRADE_H;
            blit(graphics, offer == menu.selectedOffer() ? TRADE_ROW_SELECTED : TRADE_ROW,
                    TRADE_X, y, TRADE_W, TRADE_H);

            if (offer >= menu.catalogSize()) {
                continue;
            }

            int left = leftPos + TRADE_X;
            int top = topPos + y;
            ItemStack costA = menu.getCatalogCostA(offer);
            ItemStack costB = menu.getCatalogCostB(offer);
            ItemStack result = menu.getCatalogResult(offer);

            graphics.renderItem(costA, left + 5, top + 3);
            graphics.renderItemDecorations(font, costA, left + 5, top + 3);
            if (!costB.isEmpty()) {
                graphics.renderItem(costB, left + 32, top + 3);
                graphics.renderItemDecorations(font, costB, left + 32, top + 3);
            }
            graphics.drawString(font, ">", left + 58, top + 7, 0xFFE1D7C5, false);
            graphics.renderItem(result, left + 72, top + 3);
            graphics.renderItemDecorations(font, result, left + 72, top + 3);

            int uses = menu.uses(offer);
            int max = menu.maxUses(offer);
            int usesColor = uses >= max && max > 0 ? 0xFFFF6767 : 0xFFF0E7D8;
            graphics.drawString(font, uses + "/" + max, left + 93, top + 8, usesColor, false);
            if (uses >= max && max > 0) {
                graphics.fill(left + 2, top + 2, left + TRADE_W - 2, top + TRADE_H - 2, 0x66000000);
            }
        }
    }

    private void renderBasketCounters(GuiGraphics graphics) {
        int x = leftPos + BASKET_X;
        int y = topPos + BASKET_Y;
        boolean hasOrder = menu.basketSlots() > 0;
        int color = hasOrder ? 0xFF4A3828 : 0xFF8A8176;

        String slots = menu.basketSlots() + "/" + MOConstants.BASKET_SIZE;
        graphics.drawString(font, slots, x + 6, y + 68, color, false);

        String itemCount = Integer.toString(menu.basketItems());
        graphics.drawString(font, itemCount, x + BASKET_W - 6 - font.width(itemCount), y + 68, color, false);
    }

    private void renderOrderButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + BUTTON_X;
        int y = topPos + BUTTON_Y;
        boolean enabled = menu.basketSlots() > 0;
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
        ResourceLocation texture = !enabled ? SEND_ORDER_DISABLED : (hovered ? SEND_ORDER_HOVER : SEND_ORDER);
        blit(graphics, texture, BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        Component label = Component.translatable("screen.merchant_orders.send_order");
        graphics.drawString(font, label,
                x + (BUTTON_W - font.width(label)) / 2,
                y + 7,
                enabled ? 0xFFFFFFFF : 0xFFB6B2AA,
                false);
    }

    private void renderScrollBar(GuiGraphics graphics) {
        int maxOffset = Math.max(0, menu.catalogSize() - VISIBLE_ROWS);
        blit(graphics, SCROLL_TRACK, SCROLL_X, SCROLL_Y, SCROLL_W, SCROLL_H);
        int thumbY = maxOffset == 0
                ? SCROLL_Y
                : SCROLL_Y + (SCROLL_H - SCROLL_THUMB_H) * scrollOffset / maxOffset;
        blit(graphics, SCROLL_THUMB, SCROLL_X, thumbY, SCROLL_W, SCROLL_THUMB_H);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 10, 0xFF33271D, false);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.tier." + menu.selectedTier()),
                196, 10, 0xFF33271D, false);
        String xpText = menu.xp() + " XP";
        graphics.drawString(font, xpText,
                XP_X + XP_W - font.width(xpText),
                10,
                0xFF33271D,
                false);

        Component currentTrade = Component.translatable("screen.merchant_orders.current_trade");
        graphics.drawString(font, currentTrade,
                CURRENT_X + (CURRENT_W - font.width(currentTrade)) / 2,
                CURRENT_Y + 4,
                0xFF443427,
                false);

        Component basket = Component.translatable("screen.merchant_orders.order_basket");
        graphics.drawString(font, basket,
                BASKET_X + (BASKET_W - font.width(basket)) / 2,
                BASKET_Y + 4,
                0xFF443427,
                false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // The basket is a modal preview. While it is open, render the terminal
        // with an off-screen mouse position so underlying slots/buttons never
        // receive hover state or appear interactive through the overlay.
        int terminalMouseX = basketOpen ? -10_000 : mouseX;
        int terminalMouseY = basketOpen ? -10_000 : mouseY;
        super.render(graphics, terminalMouseX, terminalMouseY, partialTick);

        if (basketOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 500.0F);
            graphics.fill(0, 0, width, height, 0x99000000);
            graphics.pose().translate(0.0F, 0.0F, 10.0F);
            renderBasketPreview(graphics);
            renderBasketPreviewTooltip(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            return;
        }

        renderHoveredCatalogTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderBasketPreview(GuiGraphics graphics) {
        int x = basketPreviewX();
        int y = basketPreviewY();
        graphics.blit(ORDER_BASKET, x, y, 0, 0,
                BASKET_PREVIEW_W, BASKET_PREVIEW_H, BASKET_PREVIEW_W, BASKET_PREVIEW_H);

        for (int slot = 0; slot < MOConstants.BASKET_SIZE; slot++) {
            ItemStack stack = menu.getBasketItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int column = slot % 9;
            int row = slot / 9;
            int slotX = x + BASKET_PREVIEW_SLOT_X + column * 18;
            int slotY = y + BASKET_PREVIEW_SLOT_Y + row * 18;
            graphics.renderItem(stack, slotX, slotY);
            graphics.renderItemDecorations(font, stack, slotX, slotY);
        }
    }

    private void renderBasketPreviewTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int relativeX = mouseX - basketPreviewX() - BASKET_PREVIEW_SLOT_X;
        int relativeY = mouseY - basketPreviewY() - BASKET_PREVIEW_SLOT_Y;
        if (relativeX < 0 || relativeY < 0 || relativeX >= 9 * 18 || relativeY >= 4 * 18) {
            return;
        }

        int column = relativeX / 18;
        int row = relativeY / 18;
        ItemStack hovered = menu.getBasketItem(row * 9 + column);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private int basketPreviewX() {
        return leftPos + (imageWidth - BASKET_PREVIEW_W) / 2;
    }

    private int basketPreviewY() {
        return topPos + (imageHeight - BASKET_PREVIEW_H) / 2;
    }

    private void renderHoveredCatalogTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;

        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int tabY = TIER_Y + tier * (TIER_H + TIER_GAP);
            if (localX >= TIER_X && localX < TIER_X + TIER_W
                    && localY >= tabY && localY < tabY + TIER_H) {
                Component tierName = Component.translatable("screen.merchant_orders.tier." + tier);
                Component tooltip = tier <= menu.unlockedTier()
                        ? Component.translatable("tooltip.merchant_orders.tier_open", tierName)
                        : Component.translatable("tooltip.merchant_orders.tier_locked", MOConstants.XP_THRESHOLDS[tier]);
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }

        if (localX >= BUTTON_X && localX < BUTTON_X + BUTTON_W
                && localY >= BUTTON_Y && localY < BUTTON_Y + BUTTON_H) {
            graphics.renderTooltip(font,
                    Component.translatable(menu.basketSlots() > 0
                            ? "tooltip.merchant_orders.send_order"
                            : "tooltip.merchant_orders.empty_basket"),
                    mouseX, mouseY);
            return;
        }

        if (localX < TRADE_X || localX >= TRADE_X + TRADE_W
                || localY < TRADE_Y || localY >= TRADE_Y + VISIBLE_ROWS * TRADE_H) {
            return;
        }

        int offer = scrollOffset + (localY - TRADE_Y) / TRADE_H;
        if (offer >= menu.catalogSize()) {
            return;
        }

        int xInRow = localX - TRADE_X;
        ItemStack hovered = xInRow < 30
                ? menu.getCatalogCostA(offer)
                : (xInRow < 57 ? menu.getCatalogCostB(offer) : menu.getCatalogResult(offer));
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (basketOpen) {
            int previewX = basketPreviewX();
            int previewY = basketPreviewY();
            if (mouseX < previewX || mouseX >= previewX + BASKET_PREVIEW_W
                    || mouseY < previewY || mouseY >= previewY + BASKET_PREVIEW_H) {
                basketOpen = false;
            }
            return true;
        }

        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;

        if (x >= BASKET_X && x < BASKET_X + BASKET_W
                && y >= BASKET_Y && y < BASKET_Y + BASKET_H) {
            basketOpen = true;
            return true;
        }

        if (x >= 274 && x < 292 && y >= 62 && y < 80) {
            sendButton(Screen.hasShiftDown()
                    ? OrderTerminalMenu.BUTTON_EXECUTE_MAX
                    : OrderTerminalMenu.BUTTON_EXECUTE_ONE);
            return true;
        }

        for (int tier = 0; tier < MOConstants.TIER_COUNT; tier++) {
            int tabY = TIER_Y + tier * (TIER_H + TIER_GAP);
            if (x >= TIER_X && x < TIER_X + TIER_W && y >= tabY && y < tabY + TIER_H) {
                sendButton(OrderTerminalMenu.BUTTON_TIER_BASE + tier);
                return true;
            }
        }

        if (x >= TRADE_X && x < TRADE_X + TRADE_W
                && y >= TRADE_Y && y < TRADE_Y + VISIBLE_ROWS * TRADE_H) {
            int offer = scrollOffset + (y - TRADE_Y) / TRADE_H;
            if (offer < menu.catalogSize()) {
                sendButton(OrderTerminalMenu.BUTTON_OFFER_BASE + offer);
            }
            return true;
        }

        if (x >= BUTTON_X && x < BUTTON_X + BUTTON_W && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_H) {
            sendButton(OrderTerminalMenu.BUTTON_CLAIM);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (basketOpen) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (basketOpen) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (basketOpen) {
            return true;
        }

        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        int maxOffset = Math.max(0, menu.catalogSize() - VISIBLE_ROWS);
        if (maxOffset > 0
                && localX >= TRADE_X && localX < SCROLL_X + SCROLL_W
                && localY >= TRADE_Y && localY < TRADE_Y + VISIBLE_ROWS * TRADE_H) {
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (basketOpen) {
            if (keyCode == 256
                    || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                basketOpen = false;
            }
            // Consume every other key as well (hotbar swaps, drop key, etc.) so
            // the terminal underneath cannot be operated while the basket is open.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
