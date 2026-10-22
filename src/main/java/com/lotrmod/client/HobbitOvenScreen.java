package com.lotrmod.client;

import com.lotrmod.LOTRMod;
import com.lotrmod.block.HobbitOvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI for {@link HobbitOvenMenu}, using the ported original {@code gui/oven.png} background. */
public class HobbitOvenScreen extends AbstractContainerScreen<HobbitOvenMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "textures/gui/hobbit_oven.png");

    public HobbitOvenScreen(HobbitOvenMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        // TODO: fuel-flame and per-slot cook-progress overlay sprites need their UV rects measured
        // against the ported hobbit_oven.png sheet before they can be drawn accurately; background only for now.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
