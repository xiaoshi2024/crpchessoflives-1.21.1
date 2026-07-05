package com.xiaoshi2022.crpchessoflives.clone.blockentities.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity.ShellStates;
import com.xiaoshi2022.crpchessoflives.clone.entities.ShellEntity;
import com.xiaoshi2022.crpchessoflives.clone.events.ClientEvents;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModRenderTypes;
import com.xiaoshi2022.crpchessoflives.clone.storage.ClientShellData;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.clone.blocks.AbstractMultiblockBlock.FACING;
import static com.xiaoshi2022.crpchessoflives.clone.blocks.AbstractMultiblockBlock.isBottomHalf;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.*;

public class ShellForgeBlockEntityRenderer implements BlockEntityRenderer<ShellForgeBlockEntity> {
    private static final Map<UUID, ShellEntity> SHELL_CACHE = new HashMap<>();

    private static final int FLAME_STRAY_COUNT = 50;
    private static final float FLAME_STRAY_WIDTH = 0.03f;
    private static final float FLAME_RADIUS = 0.25f;

    private static final int[] YELLOW = {247, 223, 37};
    private static final int[] ORANGE = {255, 140, 0};
    private static final int[] DARK_ORANGE = {153, 84, 1};

    private final BlockRenderDispatcher blockRenderer;

    public ShellForgeBlockEntityRenderer(final BlockEntityRendererProvider.Context ctx) {
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    public static ShellEntity getPlayerShellFromCache(final ClientLevel level, final UUID playerUuid, final UUID shellUuid, final Map<UUID, ShellEntity> cache) {
        return cache.computeIfAbsent(shellUuid, ignored -> {
            final ShellEntity player = new ShellEntity(level, new GameProfile(playerUuid, ""));
            final ShellState shellState = ClientShellData.INSTANCE.get(playerUuid, shellUuid);
            if (shellState != null) {
                player.load(shellState.playerData());
            }

            player.setPose(Pose.STANDING);
            player.setXRot(0f);

            return player;
        });
    }

    @Override
    public void render(final ShellForgeBlockEntity blockEntity,
                       final float partialTick,
                       final PoseStack poseStack,
                       final MultiBufferSource buffers,
                       final int packedLight,
                       final int packedOverlay) {
        if (!isBottomHalf(blockEntity.getBlockState())) {
            return;
        }

        this.renderGlassPanels(blockEntity, partialTick, poseStack, buffers, packedLight, packedOverlay);

        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        final UUID playerUuid = blockEntity.getPlayerUuid();
        final UUID shellUuid = blockEntity.getShellUuid();
        if (playerUuid.equals(EMPTY_UUID) || shellUuid.equals(EMPTY_UUID)) {
            return;
        }

        if (blockEntity.getShellState() == ShellStates.EXTERMINATING && blockEntity.getLevel() != null) {
            this.renderSmallFlame(poseStack, buffers, blockEntity.getLevel().random, packedLight);
            // TODO: also add black spots onto the shell
        }

        final ShellEntity shellPlayer = getPlayerShellFromCache(mc.level, playerUuid, shellUuid, SHELL_CACHE);
        shellPlayer.setPos(blockEntity.getBlockPos().getCenter().subtract(0, 0.5 - 0.1, 0));

        final float yaw = yawForDirection(blockEntity.getBlockState().getValue(FACING));
        shellPlayer.yBodyRotO = yaw;
        shellPlayer.yBodyRot = yaw;
        shellPlayer.setYRot(yaw);
        shellPlayer.yHeadRotO = yaw;
        shellPlayer.yHeadRot = yaw;
        shellPlayer.setXRot(0f);

        final EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        final ResourceLocation skinTexture = dispatcher.getRenderer(shellPlayer).getTextureLocation(shellPlayer);
        final int color = blockEntity.getShellState() == ShellStates.CREATING ? packRGB8(54, 188, 184) : packRGB8(53, 53, 53);
        final RenderType renderType = ModRenderTypes.CREATE_SHADER_TYPE.apply(skinTexture, shellPlayer, blockEntity.getShellPercentage(), 1, color);

        final MultiBufferSource remappedBuffer = new ShaderForcingBuffer(buffers, renderType,
                blockEntity.getShellState() == ShellStates.CREATING
                        || blockEntity.getShellState() == ShellStates.DECAYING
                        || blockEntity.getShellState() == ShellStates.EXTERMINATING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.06250, 0.5);
        dispatcher.setRenderShadow(false);
        dispatcher.render(shellPlayer, 0, 0, 0, yaw, partialTick, poseStack, remappedBuffer, packedLight);
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
    }

    private void renderSmallFlame(final PoseStack poseStack, final MultiBufferSource buffers, final RandomSource random, final int packedLight) {
        final VertexConsumer vertexConsumer = buffers.getBuffer(RenderType.debugQuads());

        for (int i = 0; i < FLAME_STRAY_COUNT; i++) {
            final float height = 1.6f + (random.nextFloat() - 0.5f) * 0.7f;
            final float startY = -height + 2.0F;

            final float angleRad = (float) (((float) i / FLAME_STRAY_COUNT) * (2 * Math.PI));
            final float baseX = 0.5f + (float) Math.cos(angleRad) * FLAME_RADIUS;
            final float baseZ = 0.5f + (float) Math.sin(angleRad) * FLAME_RADIUS;

            final float angle = random.nextFloat() * 360f;

            poseStack.pushPose();

            // Rotate around center of quad
            poseStack.translate(baseX, startY + height / 2f, baseZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(-baseX, -(startY + height / 2f), -baseZ);

            final Matrix4f matrix = poseStack.last().pose();

            final int steps = 10;
            for (int j = 0; j < steps; j++) {
                final float y1 = startY + (j / (float) steps) * height;
                final float y2 = startY + ((j + 1) / (float) steps) * height;

                final float t1 = (y1 - startY) / height;
                final float t2 = (y2 - startY) / height;

                final float widthFactor1 = (float) Math.sin(t1 * Math.PI);
                final float widthFactor2 = (float) Math.sin(t2 * Math.PI);

                final float w1 = FLAME_STRAY_WIDTH + 0.25f * widthFactor1;
                final float w2 = FLAME_STRAY_WIDTH + 0.25f * widthFactor2;

                // Calculate colors
                final float midT = j / (float) steps;
                final float midT2 = (j + 1) / (float) steps;

                final int[] color1 = interpolateGradient(midT, DARK_ORANGE, ORANGE, YELLOW);
                final int[] color2 = interpolateGradient(midT2, DARK_ORANGE, ORANGE, YELLOW);

                // Build vertices
                vertexConsumer.addVertex(matrix, baseX - w1 / 2, y1, baseZ)
                        .setColor(color1[0], color1[1], color1[2], 200)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight);
                vertexConsumer.addVertex(matrix, baseX - w2 / 2, y2, baseZ)
                        .setColor(color2[0], color2[1], color2[2], 200)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight);
                vertexConsumer.addVertex(matrix, baseX + w2 / 2, y2, baseZ)
                        .setColor(color2[0], color2[1], color2[2], 200)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight);
                vertexConsumer.addVertex(matrix, baseX + w1 / 2, y1, baseZ)
                        .setColor(color1[0], color1[1], color1[2], 200)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight);
            }

            poseStack.popPose();
        }
    }

    private void renderGlassPanels(final ShellForgeBlockEntity blockEntity,
                                   final float partialTick,
                                   final PoseStack poseStack,
                                   final MultiBufferSource buffers,
                                   final int packedLight,
                                   final int packedOverlay) {
        poseStack.pushPose();

        final float leftAngleDeg = this.getAngle(partialTick, blockEntity);
        final float rightAngleDeg = -leftAngleDeg;

        final float yaw = switch (blockEntity.getBlockState().getValue(FACING)) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f; //NORTH
        };
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, 0, -0.5);

        final VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        // --- RIGHT PANEL ---
        poseStack.pushPose();
        float px = 14.8f / 16f;
        float py = 0.0f;
        float pz = 1.6f / 16f;
        poseStack.translate(px, py, pz);
        poseStack.mulPose(Axis.YP.rotationDegrees(rightAngleDeg));
        poseStack.translate(-px, -py, -pz);
        this.blockRenderer.getModelRenderer().renderModel(
                poseStack.last(), consumer, null, ClientEvents.shellForgeGlassRight, 1f, 1f, 1f,
                packedLight, packedOverlay, ModelData.EMPTY, null
        );
        poseStack.popPose();

        // --- LEFT PANEL ---
        poseStack.pushPose();
        px = 1.1f / 16f;
        py = 0.0f;
        pz = 1.6f / 16f;

        poseStack.translate(px, py, pz);
        poseStack.mulPose(Axis.YP.rotationDegrees(leftAngleDeg));
        poseStack.translate(-px, -py, -pz);

        this.blockRenderer.getModelRenderer().renderModel(
                poseStack.last(), consumer, null, ClientEvents.shellForgeGlassLeft, 1f, 1f, 1f,
                packedLight, packedOverlay, ModelData.EMPTY, null
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    public float getAngle(final float partialTick, final ShellForgeBlockEntity blockEntity) {
        final float t = Mth.lerp(partialTick, blockEntity.getAnimPrevProgress(), blockEntity.getAnimProgress());
        return t * 60.0f;
    }

    @Override
    public AABB getRenderBoundingBox(final ShellForgeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, isBottomHalf(blockEntity.getBlockState()) ? 1 : -1, 0);
    }

    public static class ShaderForcingBuffer implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final RenderType forced;
        private final boolean replace;

        public ShaderForcingBuffer(final MultiBufferSource delegate, final RenderType forced, final boolean replace) {
            this.delegate = delegate;
            this.forced = forced;
            this.replace = replace;
        }

        @Override
        public VertexConsumer getBuffer(final RenderType requested) {
            if (this.replace
                    && requested.format().equals(this.forced.format())
                    && requested.mode() == this.forced.mode()) {
                return this.delegate.getBuffer(this.forced);
            }

            return this.delegate.getBuffer(requested);
        }
    }
}
