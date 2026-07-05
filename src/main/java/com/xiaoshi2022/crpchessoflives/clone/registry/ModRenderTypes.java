package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xiaoshi2022.crpchessoflives.clone.shaders.CreateTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.data.models.blockstates.PropertyDispatch.PentaFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;


@OnlyIn(Dist.CLIENT)
public class ModRenderTypes {
    public static ShaderInstance createShaderInstance;
    public static final ShaderStateShard CREATE_SHADER = new ShaderStateShard(() -> createShaderInstance);

    public static final PentaFunction<ResourceLocation, LivingEntity, Integer, Integer, Integer, RenderType> CREATE_SHADER_TYPE =
            (location, entity, percentage, guiScale, color) -> RenderType.create(
                    MODID + "_create_shader_type",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    1536,
                    true,
                    false,
                    CompositeState.builder()
                            .setShaderState(CREATE_SHADER)
                            .setTextureState(new CreateTextureStateShard(location, entity, percentage, guiScale, color))
                            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setLightmapState(RenderStateShard.LIGHTMAP)
                            .setOverlayState(RenderStateShard.OVERLAY)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .createCompositeState(true)
            );
}
