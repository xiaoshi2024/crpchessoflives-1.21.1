package com.xiaoshi2022.crpchessoflives.clone.entities.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;

public class ShellRenderer extends PlayerRenderer {
    public ShellRenderer(final Context context, final boolean useSlimModel) {
        super(context, useSlimModel);
    }

    @Override
    protected void renderNameTag(final AbstractClientPlayer entity,
                                 final Component displayName,
                                 final PoseStack poseStack,
                                 final MultiBufferSource bufferSource,
                                 final int packedLight,
                                 final float partialTick) {
    }
}
