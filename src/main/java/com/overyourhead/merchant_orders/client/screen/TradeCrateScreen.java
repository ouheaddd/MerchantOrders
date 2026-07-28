package com.overyourhead.merchant_orders.client.screen;

import com.overyourhead.merchant_orders.MerchantOrdersMod;
import com.overyourhead.merchant_orders.common.menu.TradeCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TradeCrateScreen extends AbstractContainerScreen<TradeCrateMenu> {
    private static final ResourceLocation BACKGROUND = gui("trade_crate/background.png");
    private static final ResourceLocation LEFT_PANEL = gui("trade_crate/left_panel.png");
    private static final ResourceLocation VALUE_PANEL = gui("trade_crate/value_panel.png");
    private static final ResourceLocation REWARD_PANEL = gui("trade_crate/reward_panel.png");
    private static final ResourceLocation STATUS_PANEL = gui("trade_crate/status_panel.png");
    private static final ResourceLocation SLOT = gui("trade_crate/slot.png");
    private static final ResourceLocation REWARD_SLOT = gui("trade_crate/reward_slot.png");
    private static final ResourceLocation BUTTON = gui("trade_crate/sell_button.png");
    private static final ResourceLocation BUTTON_HOVER = gui("trade_crate/sell_button_hover.png");
    private static final ResourceLocation BUTTON_DISABLED = gui("trade_crate/sell_button_disabled.png");
    private static final ResourceLocation PROGRESS_BG = gui("trade_crate/progress_background.png");
    private static final ResourceLocation PROGRESS_FILL = gui("trade_crate/progress_fill.png");
    private static final ResourceLocation CLOCK = gui("trade_crate/clock.png");

    private static final int WIDTH = 380;
    private static final int HEIGHT = 240;

    private static final int CRATE_X = 16;
    private static final int CRATE_Y = 34;
    private static final int PLAYER_X = 16;
    private static final int PLAYER_Y = 160;
    private static final int HOTBAR_Y = 218;
    private static final int REWARD_X = 204;
    private static final int REWARD_Y = 120;
    private static final int REWARD_GAP_X = 37;
    private static final int REWARD_GAP_Y = 22;

    private static ResourceLocation gui(String path) {
        return ResourceLocation.fromNamespaceAndPath(MerchantOrdersMod.MOD_ID, "textures/gui/" + path);
    }

    public TradeCrateScreen(TradeCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        blit(graphics, BACKGROUND, 0, 0, WIDTH, HEIGHT);
        blit(graphics, LEFT_PANEL, 6, 17, 178, 219);
        blit(graphics, VALUE_PANEL, 190, 20, 182, 64);
        blit(graphics, REWARD_PANEL, 190, 88, 182, 91);
        blit(graphics, STATUS_PANEL, 190, 209, 182, 27);

        renderSlots(graphics);
        renderProgress(graphics);
        renderButton(graphics, mouseX, mouseY);
    }

    private void renderSlots(GuiGraphics graphics) {
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                blit(graphics, SLOT, CRATE_X + column * 18, CRATE_Y + row * 18, 18, 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                blit(graphics, SLOT, PLAYER_X + column * 18, PLAYER_Y + row * 18, 18, 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            blit(graphics, SLOT, PLAYER_X + column * 18, HOTBAR_Y, 18, 18);
        }
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 4; column++) {
                blit(graphics, REWARD_SLOT,
                        REWARD_X + column * REWARD_GAP_X,
                        REWARD_Y + row * REWARD_GAP_Y,
                        18, 18);
            }
        }
    }

    private void renderProgress(GuiGraphics graphics) {
        blit(graphics, PROGRESS_BG, 204, 51, 100, 8);
        int segments = Math.min(7, Math.max(0, menu.value() / 40));
        if (segments > 0) {
            graphics.blit(PROGRESS_FILL, leftPos + 206, topPos + 53,
                    0, 0, segments * 13, 4, 91, 4);
        }
    }

    private void renderButton(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean enabled = menu.value() > 0 && menu.cooldownTicks() <= 0 && menu.pendingTicks() <= 0;
        int bx = leftPos + 190;
        int by = topPos + 183;
        boolean hovered = mouseX >= bx && mouseX < bx + 182 && mouseY >= by && mouseY < by + 22;
        ResourceLocation texture = !enabled ? BUTTON_DISABLED : hovered ? BUTTON_HOVER : BUTTON;
        blit(graphics, texture, 190, 183, 182, 22);

        Component sell = Component.translatable("screen.merchant_orders.sell_contents");
        graphics.drawString(font, sell, bx + (182 - font.width(sell)) / 2, by + 7,
                enabled ? 0xFFF3E8CD : 0xFFB8B2A8, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("container.merchant_orders.trade_crate");
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 7, 0xFF3E2E22, false);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.crate_contents"), 16, 22, 0xFF49372A, false);
        graphics.drawString(font, playerInventoryTitle, 16, 148, 0xFF49372A, false);

        graphics.drawString(font, Component.translatable("screen.merchant_orders.batch_value"), 204, 27, 0xFF49372A, false);
        Component grade = gradeComponent(menu.value());
        graphics.drawString(font, grade, 204, 40, gradeColor(menu.value()), false);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.approximate_value"), 204, 64, 0xFF49372A, false);

        ItemStack emerald = new ItemStack(Items.EMERALD);
        graphics.renderItem(emerald, 204, 70);
        graphics.drawString(font, Component.literal(menu.approxEmeraldMin() + " - " + menu.approxEmeraldMax()),
                225, 75, 0xFF49372A, false);

        graphics.drawString(font, Component.translatable("screen.merchant_orders.offered_reward"), 204, 95, 0xFF49372A, false);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.reward_explanation"), 204, 107, 0xFF5A4635, false);

        blitLocal(graphics, CLOCK, 204, 161, 16, 16);
        graphics.drawString(font, Component.translatable("screen.merchant_orders.offer_refreshes_in"), 224, 162, 0xFF49372A, false);
        graphics.drawString(font, formatOfferRefresh(), 224, 171, 0xFFC83D32, false);

        graphics.drawString(font, Component.translatable("screen.merchant_orders.next_batch"), 204, 215, 0xFF49372A, false);
        graphics.drawString(font, statusText(), 204, 225, statusColor(), false);
    }

    private Component gradeComponent(int value) {
        String key;
        if (value <= 0) key = "screen.merchant_orders.grade.empty";
        else if (value < 64) key = "screen.merchant_orders.grade.small";
        else if (value < 192) key = "screen.merchant_orders.grade.normal";
        else if (value < 512) key = "screen.merchant_orders.grade.valuable";
        else key = "screen.merchant_orders.grade.very_valuable";
        return Component.translatable(key);
    }

    private int gradeColor(int value) {
        if (value <= 0) return 0xFF777067;
        if (value < 64) return 0xFF6B9A43;
        if (value < 192) return 0xFF3C9B38;
        if (value < 512) return 0xFFD28B26;
        return 0xFFC94A35;
    }

    private Component formatOfferRefresh() {
        long remaining = Math.max(0L, menu.offerRefreshTicks());
        long hours = remaining / 1000L;
        long minutes = (remaining % 1000L) * 60L / 1000L;
        return Component.translatable("screen.merchant_orders.time_hours_minutes", hours, minutes);
    }

    private Component statusText() {
        if (menu.pendingTicks() > 0) {
            return Component.translatable("screen.merchant_orders.delivery_in", Math.max(1, menu.pendingTicks() / 20));
        }
        if (menu.cooldownTicks() > 0) {
            long ticks = menu.cooldownTicks();
            long days = ticks / 24000L;
            long hours = (ticks % 24000L) / 1000L;
            return Component.translatable("screen.merchant_orders.cooldown", days, hours);
        }
        return Component.translatable("screen.merchant_orders.ready_to_sell");
    }

    private int statusColor() {
        return menu.cooldownTicks() > 0 || menu.pendingTicks() > 0 ? 0xFFC83D32 : 0xFF4C963F;
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, leftPos + x, topPos + y, 0, 0, width, height, width, height);
    }

    private void blitLocal(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (x >= 190 && x < 372 && y >= 183 && y < 205) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TradeCrateMenu.BUTTON_SELL);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
