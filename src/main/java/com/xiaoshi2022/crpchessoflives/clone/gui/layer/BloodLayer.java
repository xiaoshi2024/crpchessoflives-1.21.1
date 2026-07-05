package com.xiaoshi2022.crpchessoflives.clone.gui.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;
import static com.xiaoshi2022.crpchessoflives.clone.items.SyringeItem.MAX_EXTRACT_DURATION;
import static com.xiaoshi2022.crpchessoflives.clone.items.SyringeItem.PULSE_POINTS;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.smoothstep01;

public class BloodLayer implements LayeredDraw.Layer {
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/blood_overlay.png");

    private static final float PULSE_WIDTH = 0.02f;
    private static final float MAX_ALPHA = 0.4f;

    private static float pulseAlpha(final float usePos) {
        float alphaFactor = 0f;
        for (final float point : PULSE_POINTS) {
            final float d = Math.abs(usePos - point);
            if (d < PULSE_WIDTH) {
                final float t = 1f - (d / PULSE_WIDTH);
                final float smooth = smoothstep01(t);
                alphaFactor = Math.max(alphaFactor, smooth);
            }
        }
        return alphaFactor * MAX_ALPHA;
    }

    @Override
    public void render(final GuiGraphics graphics, final DeltaTracker deltaTracker) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;

        if (player == null) {
            return;
        }

        if (!player.isHolding(ModItems.EMPTY_SYRINGE.get())) {
            return;
        }

        final float usePos = (float) (player.getMainHandItem().getUseDuration(player) - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
        final float alpha = pulseAlpha(usePos);
        if (alpha > 0.01f) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(UNDERWATER_LOCATION, 0, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
