package com.xiaoshi2022.crpchessoflives.clone.items.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class SyringeItemExtension implements IClientItemExtensions {
    @Override
    public boolean applyForgeHandTransform(final PoseStack poseStack,
                                           final LocalPlayer player,
                                           final HumanoidArm arm,
                                           final ItemStack stackInHand,
                                           final float partialTicks,
                                           final float equippedProgress,
                                           final float swingProgress) {
        if (player.getOffhandItem().is(stackInHand.getItem())) {
            return false;
        }

        final int useDuration = stackInHand.getUseDuration(player);
        final int remainingUseTicks = player.getUseItemRemainingTicks();
        final int useTicks = useDuration - remainingUseTicks;

        final Minecraft mc = Minecraft.getInstance();
        final MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        final int packedLight = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTicks);

        float equipProgress = equippedProgress;
        if (player.isUsingItem()) {
            equipProgress = 0;

            if (remainingUseTicks > 0) {
                this.renderLeftPlayerArm(mc, player, poseStack, bufferSource, packedLight, partialTicks, useTicks);
            }
        }
        this.renderRightPlayerArm(mc, player, poseStack, bufferSource, packedLight, partialTicks, equipProgress, useTicks, useDuration);

        return true;
    }

    private void renderLeftPlayerArm(final Minecraft mc,
                                     final LocalPlayer localPlayer,
                                     final PoseStack poseStack,
                                     final MultiBufferSource buffer,
                                     final int packedLight,
                                     final float partialTicks,
                                     final int useTicks) {
        final float t = Mth.clamp((useTicks + partialTicks) / 6.0F, 0.0F, 1.0F);
        final float moveFactor = Mth.lerp(t, -1.6F, 0.0F);

        poseStack.pushPose();

        poseStack.translate(moveFactor, 0.0F, 0.0F);

        poseStack.translate(0.14F, 0.0F, -0.50F);

        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.translate(-1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-130.0F));
        poseStack.translate(5.8F, 0F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(80.0F));
        poseStack.translate(0.1F, -0.1F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));

        final PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(localPlayer);
        playerRenderer.renderLeftHand(poseStack, buffer, packedLight, localPlayer);

        poseStack.popPose();
    }

    private void renderRightPlayerArm(final Minecraft mc,
                                      final LocalPlayer localPlayer,
                                      final PoseStack poseStack,
                                      final MultiBufferSource buffer,
                                      final int packedLight,
                                      final float partialTicks,
                                      final float equippedProgress,
                                      final int useTicks,
                                      final int useDuration) {
        final float t = Mth.clamp((useTicks + partialTicks) / 6.0F, 0.0F, 1.0F);
        final float moveFactor = (useTicks != useDuration) ? Mth.lerp(t, 0.0F, -1.0F) : 0.0F;

        poseStack.pushPose();

        poseStack.translate(0.0F, 0.04F + equippedProgress * -1.2F, 0.0F);

        poseStack.translate(0.64F, -0.6F, -0.72F);

        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.translate(-1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        poseStack.translate(5.6F, 0.0F, 0.0F);

        poseStack.mulPose(Axis.XP.rotationDegrees(75 * moveFactor));
        poseStack.translate(0.0F, -0.2F * moveFactor + 0.2F, 0.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(10));

        final PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(localPlayer);
        playerRenderer.renderRightHand(poseStack, buffer, packedLight, localPlayer);

        poseStack.popPose();

        // Syringe
        poseStack.translate(0.57F, -0.32F + equippedProgress * -0.6F, -1.22F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-162F));
        poseStack.mulPose(Axis.YP.rotationDegrees(10F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10F));

        poseStack.translate(moveFactor, 0.25F * moveFactor, 0.2F * moveFactor);

        poseStack.mulPose(Axis.YP.rotationDegrees(110F * moveFactor));
        poseStack.mulPose(Axis.XP.rotationDegrees(20F));
    }
}
