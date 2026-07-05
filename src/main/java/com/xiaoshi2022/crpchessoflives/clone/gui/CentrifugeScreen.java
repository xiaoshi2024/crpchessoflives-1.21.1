package com.xiaoshi2022.crpchessoflives.clone.gui;

import com.xiaoshi2022.crpchessoflives.clone.container.CentrifugeContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;


public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeContainerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/centrifuge.png");
    private static final ResourceLocation PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final int ENERGY_BAR_HEIGHT = 52;

    public CentrifugeScreen(final CentrifugeContainerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getXSize(), this.getYSize());

        // Draw energy bar
        graphics.blit(BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT);
        final int energyAmount = this.getMenu().getBlockEntity().energyStorage.getEnergyStored();
        final int energyMaxStored = this.getMenu().getBlockEntity().energyStorage.getMaxEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) energyMaxStored));
        graphics.blit(BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel);

        // Draw progress
        final int width = Mth.ceil((this.menu.getProcessingProgress() * 24.0F));
        graphics.blitSprite(PROGRESS_SPRITE, 24, 16, 0, 0, this.leftPos + 79, this.topPos + 34, width, 16);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.renderTooltip(this.font, List.of(Component.translatable("gui.crpchessoflives.energy",
                    this.getMenu().getBlockEntity().energyStorage.getEnergyStored()).getVisualOrderText()), mouseX, mouseY);
        }
    }
}
