package com.xiaoshi2022.crpchessoflives.clone.events;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.renderer.ShellForgeBlockEntityRenderer;
import com.xiaoshi2022.crpchessoflives.clone.gui.CentrifugeScreen;
import com.xiaoshi2022.crpchessoflives.clone.gui.RadialMenuRenderer;
import com.xiaoshi2022.crpchessoflives.clone.gui.ShellForgeScreen;
import com.xiaoshi2022.crpchessoflives.clone.gui.ShellSelectionOverlay;
import com.xiaoshi2022.crpchessoflives.clone.gui.layer.BloodLayer;
import com.xiaoshi2022.crpchessoflives.clone.items.extensions.SyringeItemExtension;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModBlockEntityTypes;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModItems;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModMenuTypes;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModRenderTypes;
import com.xiaoshi2022.crpchessoflives.clone.utils.CameraHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.io.IOException;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;
import static com.xiaoshi2022.crpchessoflives.clone.items.SyringeItem.MAX_EXTRACT_DURATION;
import static net.minecraft.client.resources.model.ModelResourceLocation.STANDALONE_VARIANT;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientEvents {
    private static final ModelResourceLocation GLASS_LEFT_LOC =
            new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MODID, "block/shell_forge_glass_left"), STANDALONE_VARIANT);
    private static final ModelResourceLocation GLASS_RIGHT_LOC =
            new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MODID, "block/shell_forge_glass_right"), STANDALONE_VARIANT);
    public static BakedModel shellForgeGlassLeft;
    public static BakedModel shellForgeGlassRight;

    @SubscribeEvent
    public static void onMouseInput(final InputEvent.MouseButton.Pre event) {
        final Minecraft mc = Minecraft.getInstance();
        if (ShellSelectionOverlay.INSTANCE.getDisplayedShell() != null && mc.screen == null) {
            final RadialMenuRenderer.MousePos mousePos = RadialMenuRenderer.getMousePos();

            ShellSelectionOverlay.INSTANCE.mouseClick(mousePos.x(), mousePos.y(), event.getButton(), event.getAction());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(final InputEvent.Key event) {
        final Minecraft mc = Minecraft.getInstance();
        if (ShellSelectionOverlay.INSTANCE.getDisplayedShell() != null && mc.screen == null) {
            ShellSelectionOverlay.INSTANCE.keyPressed(event.getKey());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.EMPTY_SYRINGE.get(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "extracting"),
                    (stack, level, player, seed) -> player != null && player.isUsingItem() && player.getUseItem() == stack ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    ModItems.EMPTY_SYRINGE.get(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "extracting_progress"),
                    (stack, level, player, seed) -> {
                        if (player == null) {
                            return 0.0F;
                        } else {
                            return player.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration(player) - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
                        }
                    }
            );
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(final ModelEvent.RegisterAdditional event) {
        event.register(GLASS_LEFT_LOC);
        event.register(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void onBakingCompleted(final ModelEvent.BakingCompleted event) {
        ClientEvents.shellForgeGlassLeft = event.getModels().get(GLASS_LEFT_LOC);
        ClientEvents.shellForgeGlassRight = event.getModels().get(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(new SyringeItemExtension(), ModItems.EMPTY_SYRINGE.get());
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHELL_FORGE.get(), ShellForgeScreen::new);
        event.register(ModMenuTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MODID, "shells_selection"), ShellSelectionOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MODID, "blood"), new BloodLayer());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "create_shader"),
                        DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> ModRenderTypes.createShaderInstance = shaderInstance);
    }

    @SubscribeEvent
    public static void cancelRenderingGuiLayers(final RenderGuiLayerEvent.Pre event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancelRenderingHand(final RenderHandEvent event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }
}
