package com.xiaoshi2022.crpchessoflives.clone.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity.ShellStates;
import com.xiaoshi2022.crpchessoflives.clone.container.ShellForgeContainerMenu;
import com.xiaoshi2022.crpchessoflives.clone.gui.widgets.ProgressBarWidget;
import com.xiaoshi2022.crpchessoflives.clone.gui.widgets.ShellButton;
import com.xiaoshi2022.crpchessoflives.clone.packet.c2s.ShellButtonPressedPacket;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModSoundEvents;
import com.xiaoshi2022.crpchessoflives.clone.utils.SoundHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;


public class ShellForgeScreen extends AbstractContainerScreen<ShellForgeContainerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/shell_forge.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final float STATUS_TEXT_SCALE = 0.5F;

    @Nullable
    private ShellButton shellButton;
    @Nullable
    private ProgressBarWidget progressBar;

    @Nullable
    private Component randomStatusSubText;
    @Nullable
    private ShellStates lastShellState;

    public ShellForgeScreen(final ShellForgeContainerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        this.randomStatusSubText = this.getRandomStatusSubText();
    }

    @Override
    protected void init() {
        super.init();

        this.shellButton = new ShellButton(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 55, 80, 16, (button) -> {
            if (this.getMenu().getBlockEntity().getShellState() == ShellStates.EXTERMINATE && this.getMenu().getBlockEntity().getLevel() != null) {
                SoundHandler.startBlockSound(ModSoundEvents.FLAMETHROWER.get(), SoundSource.BLOCKS, 1.5F, 1.0F,
                        this.getMenu().getBlockEntity().getLevel().random, this.getMenu().getBlockEntity().getBlockPos());
            }

            PacketDistributor.sendToServer(new ShellButtonPressedPacket(this.getMenu().getBlockEntity().getBlockPos()));
        }, this.getMenu().getBlockEntity());
        this.addRenderableWidget(this.shellButton);

        this.progressBar = new ProgressBarWidget(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 25, 80, 16, this.getMenu().getBlockEntity(),
                () -> this.getMenu().getBlockEntity().getShellPercentage());
        this.addRenderableWidget(this.progressBar);

        this.containerTick();
    }

    @Override
    protected void containerTick() {
        if (this.shellButton != null) {
            this.shellButton.tick();
        }
        if (this.progressBar != null) {
            this.progressBar.tick();
        }
        // Check if shell state has updated
        if (this.lastShellState != this.getMenu().getBlockEntity().getShellState()) {
            this.randomStatusSubText = this.getRandomStatusSubText();
        }
        this.lastShellState = this.getMenu().getBlockEntity().getShellState();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getXSize(), this.getYSize());
        if (this.getMenu().getBlockEntity().getShellState() == ShellStates.CREATE) {
            graphics.blitSprite(SLOT_SPRITE, this.leftPos + 79, this.topPos + 16, 0, 18, 18);
        }

        final PoseStack poseStack = graphics.pose();

        if (this.getMenu().getBlockEntity().getShellState() != ShellStates.CREATE) {
            if (this.randomStatusSubText != null) {
                final Component status = Component.translatable("gui.crpchessoflives.shell_forge.status", this.randomStatusSubText);
                final int x = (int) (this.leftPos + (this.imageWidth - this.font.width(status.getString()) * STATUS_TEXT_SCALE) / 2);
                final int y = this.topPos + 47;

                poseStack.pushPose();
                poseStack.translate(x, y, 0);
                poseStack.scale(STATUS_TEXT_SCALE, STATUS_TEXT_SCALE, STATUS_TEXT_SCALE);
                graphics.drawString(this.font, status.getString(), 0, 0, 0xFFFFFFFF);
                poseStack.popPose();
            }
        }

        // Draw energy bar
        graphics.blit(BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT);
        final int energyAmount = this.getMenu().getBlockEntity().energyStorage.getEnergyStored();
        final int energyMaxStored = this.getMenu().getBlockEntity().energyStorage.getMaxEnergyStored();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) energyMaxStored));
        graphics.blit(BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.renderTooltip(this.font, List.of(Component.translatable("gui.crpchessoflives.energy",
                    this.getMenu().getBlockEntity().energyStorage.getEnergyStored()).getVisualOrderText()), mouseX, mouseY);
        }
    }

    @Nullable
    private Component getRandomStatusSubText() {
        final Level level = this.getMenu().getBlockEntity().getLevel();
        if (level == null) {
            return null;
        }
        final RandomSource random = level.random;

        // 70% chance to show the main/correct status
        final boolean useMainText = random.nextDouble() < 0.7;
        return switch (this.getMenu().getBlockEntity().getShellState()) {
            case CREATE -> null;
            case CREATING -> useMainText
                    ? Component.translatable("gui.crpchessoflives.shell_forge.status.creating")
                    : Component.translatable("gui.crpchessoflives.shell_forge.status.creating_" + random.nextInt(0, 5));
            case EXTERMINATE -> useMainText
                    ? Component.translatable("gui.crpchessoflives.shell_forge.status.alive")
                    : Component.translatable("gui.crpchessoflives.shell_forge.status.alive_" + random.nextInt(0, 7));
            case EXTERMINATING -> useMainText
                    ? Component.translatable("gui.crpchessoflives.shell_forge.status.exterminating")
                    : Component.translatable("gui.crpchessoflives.shell_forge.status.exterminating_" + random.nextInt(0, 2));
            case DECAYING -> useMainText
                    ? Component.translatable("gui.crpchessoflives.shell_forge.status.decaying")
                    : Component.translatable("gui.crpchessoflives.shell_forge.status.decaying_" + random.nextInt(0, 3));
        };
    }
}
