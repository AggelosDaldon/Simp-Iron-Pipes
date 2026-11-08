package com.aggelosdaldon.simpleironpipes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.Direction;

public final class RoutingScreen extends AbstractContainerScreen<RoutingMenu> {
    public RoutingScreen(RoutingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 210;
        inventoryLabelY = 114;
        titleLabelX = 8;
        titleLabelY = 6;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int left = leftPos, top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF27333E);
        graphics.fill(left + 2, top + 2, left + imageWidth - 2, top + imageHeight - 2, 0xFFBBC6CB);
        graphics.fill(left + 4, top + 19, left + 172, top + 111, 0xFF7C8A92);
        graphics.fill(left + 4, top + 123, left + 172, top + 207, 0xFF9EAAB0);
        for (int i = 0; i < 6; i++) {
            int x = left + 43 + (i % 3) * 36;
            int y = top + 32 + (i / 3) * 31;
            graphics.fill(x, y, x + 18, y + 18, 0xFF283F50);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFCFD9DD);
        }
        for (int i = 0; i < 36; i++) {
            int row = i < 27 ? i / 9 : 3;
            int col = i % 9;
            int x = left + 7 + col * 18;
            int y = top + (i < 27 ? 125 + row * 18 : 183);
            graphics.fill(x, y, x + 18, y + 18, 0xFF354B56);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB3C1C8);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x26323B, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x26323B, false);
        String[] names = { "North", "South", "East", "West", "Up", "Down" };
        for (int i = 0; i < 6; i++) {
            int x = 44 + (i % 3) * 36;
            int y = 22 + (i / 3) * 31;
            graphics.drawString(font, names[i], x, y, 0xFFFFFF, false);
        }
        graphics.drawString(font, "Click with item to set - RMB to clear", 9, 100, 0xEAF1F2, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
