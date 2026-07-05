package com.xiaoshi2022.crpchessoflives.clone.shaders;

import com.xiaoshi2022.crpchessoflives.clone.registry.ModRenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class CreateTextureStateShard extends RenderStateShard.TextureStateShard {
    private final float entityPositionY;
    private final float entityHeight;
    private final double percentage;
    private final int guiScale;
    private final int color;

    public CreateTextureStateShard(final ResourceLocation texture, final LivingEntity entity, final int percentage, final int guiScale, final int color) {
        super(texture, false, false);
        this.entityPositionY = (float) entity.getY();
        this.entityHeight = entity.getBbHeight();
        this.percentage = percentage;
        this.guiScale = guiScale;
        this.color = color;
    }

    @Override
    public void setupRenderState() {
        super.setupRenderState();

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        ModRenderTypes.createShaderInstance.safeGetUniform("CameraPos").set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        ModRenderTypes.createShaderInstance.safeGetUniform("EntityPositionY").set(this.entityPositionY);
        ModRenderTypes.createShaderInstance.safeGetUniform("EntityHeight").set(this.entityHeight * this.guiScale);
        ModRenderTypes.createShaderInstance.safeGetUniform("Percentage").set((float) this.percentage);
        ModRenderTypes.createShaderInstance.safeGetUniform("GuiScale").set((float) this.guiScale);
        ModRenderTypes.createShaderInstance.safeGetUniform("PackedRGBColor").set(this.color);
    }
}
