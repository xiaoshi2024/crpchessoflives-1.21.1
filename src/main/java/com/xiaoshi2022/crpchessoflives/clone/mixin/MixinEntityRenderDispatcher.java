package com.xiaoshi2022.crpchessoflives.clone.mixin;

import com.google.common.collect.ImmutableMap;
import com.xiaoshi2022.crpchessoflives.clone.entities.ShellEntity;
import com.xiaoshi2022.crpchessoflives.clone.entities.renderer.ShellRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Unique
    private static final Map<Model, EntityRendererProvider<ShellEntity>> SHELL_PROVIDERS = Map.of(
            Model.WIDE, (EntityRendererProvider) (context) -> new ShellRenderer(context, false),
            Model.SLIM, (EntityRendererProvider) (context) -> new ShellRenderer(context, true));

    @Shadow
    @Final
    private ItemRenderer itemRenderer;
    @Shadow
    @Final
    private BlockRenderDispatcher blockRenderDispatcher;
    @Shadow
    @Final
    private ItemInHandRenderer itemInHandRenderer;
    @Shadow
    @Final
    private EntityModelSet entityModels;
    @Shadow
    @Final
    private Font font;

    @Unique
    private Map<Model, EntityRenderer<? extends ShellEntity>> playershells$shellRenderers = Map.of();

    @Unique
    private static Map<Model, EntityRenderer<? extends ShellEntity>> playershells$createShellRenderers(final EntityRendererProvider.Context context) {
        final ImmutableMap.Builder<Model, EntityRenderer<? extends ShellEntity>> builder = ImmutableMap.builder();
        SHELL_PROVIDERS.forEach((model, provider) -> {
            try {
                builder.put(model, provider.create(context));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create shell model for " + model, exception);
            }
        });
        return builder.build();
    }

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void playerShells$getRenderer(final T entity, final CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity instanceof ShellEntity shell) {
            final Model skinModel = shell.getSkin().model();
            final EntityRenderer<? extends ShellEntity> entityrenderer = this.playershells$shellRenderers.get(skinModel);
            cir.setReturnValue(entityrenderer != null ? (EntityRenderer) entityrenderer : (EntityRenderer) this.playershells$shellRenderers.get(Model.WIDE));
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void playershells$reload(final ResourceManager resourceManager, final CallbackInfo ci) {
        final EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher) (Object) this,
                this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, resourceManager, this.entityModels, this.font);
        this.playershells$shellRenderers = playershells$createShellRenderers(context);
    }
}
