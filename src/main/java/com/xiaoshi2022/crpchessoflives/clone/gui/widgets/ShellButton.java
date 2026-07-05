package com.xiaoshi2022.crpchessoflives.clone.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity.ShellStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public class ShellButton extends Button {
    private final ShellForgeBlockEntity shellForge;

    private int dotTick = 0;
    private int dotCount = 0;

    public ShellButton(final int x, final int y, final int width, final int height, final OnPress onPress, final ShellForgeBlockEntity shellForge) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.shellForge = shellForge;
        this.tick();
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        // TODO: improve visual
        final Minecraft mc = Minecraft.getInstance();
        graphics.setColor(1.0F, 0.0F, 0.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        graphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());

        // Render label
        final String text = this.getMessage().getString();
        final int color = this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24;

        if (this.shellForge.getShellState() == ShellStates.CREATING || this.shellForge.getShellState() == ShellStates.EXTERMINATING) {
            final int x = this.getX() + (this.getWidth() - mc.font.width(text.replace(".", ""))) / 2;
            final int y = this.getY() + (this.getHeight() - mc.font.lineHeight) / 2 + 1;
            graphics.drawString(mc.font, text, x, y, color, true);
        } else {
            super.renderString(graphics, mc.font, color);
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void setMessage(final ShellStates state) {
        final String additional = (state == ShellStates.CREATING || state == ShellStates.EXTERMINATING) ? ".".repeat(this.dotCount) : "";
        super.setMessage(Component.translatable("gui.crpchessoflives.shell_forge." + state.name().toLowerCase(Locale.ROOT)).append(additional));
    }

    public void tick() {
        final ShellStates state = this.shellForge.getShellState();
        if (state == ShellStates.CREATE) {
            this.active = this.shellForge.canCreateShell();
            this.resetDots();
        } else if (state == ShellStates.CREATING || state == ShellStates.EXTERMINATING) {
            this.active = false;
            // Tick dots
            if (++this.dotTick >= 10) {
                this.dotTick = 0;
                this.dotCount = (this.dotCount + 1) % 4;
            }
        } else {
            this.active = true;
            this.resetDots();
        }
        this.setMessage(state);
    }

    private void resetDots() {
        this.dotTick = 0;
        this.dotCount = 0;
    }
}
