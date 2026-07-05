package com.xiaoshi2022.crpchessoflives.clone.utils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.lerpAngle;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.lerpVec3;

public final class CameraHandler {
    private static final long PHASE1_MS = 700L;
    private static final long PHASE2_MS = 2_000L;
    private static final long PITCH_MS = 400L;
    private static final double BLOCKS_UP = 400.0;

    @Nullable
    private static Vec3 startPosition;
    @Nullable
    private static Direction startFacing;
    @Nullable
    private static Vec3 endPosition;
    @Nullable
    private static Direction endFacing;

    @Nullable
    private static Runnable onFinished;

    private static boolean animating = false;
    private static boolean reversing = false;
    private static long animClockMs = 0L;
    private static long lastNowMs = -1L;

    @Nullable
    private static Vec3 p0Start;
    @Nullable
    private static Vec3 p1Forward;
    @Nullable
    private static Vec3 p2Up;
    @Nullable
    private static Vec3 endStepDir;

    private static float startYaw;
    private static float startMidYaw;
    private static float endYaw;
    private static float endMidYaw;

    public static void cameraTick(final Camera camera) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ReceivingLevelScreen) {
            return;
        }

        final long now = System.currentTimeMillis();

        if (!animating) {
            beginAnimation(camera, now);
        }
        if (animating) {
            if (lastNowMs < 0L) {
                lastNowMs = now;
            }

            if (!mc.isPaused()) {
                animClockMs += Math.max(0L, now - lastNowMs);
            }
            lastNowMs = now;

            // 使用接口
            ((ICameraMixin) camera).setDetachedPublic(true);
            applyAnimation(camera, animClockMs);
        }
    }

    public static void setMovingAnimation(@Nullable final BlockPos startPosition,
                                          @Nullable final Direction startFacing,
                                          final BlockPos endPosition,
                                          final Direction endFacing,
                                          final Runnable onFinished) {
        CameraHandler.startPosition = startPosition != null ? startPosition.getCenter().add(0, 0.2, 0) : null;
        CameraHandler.startFacing = startFacing;
        CameraHandler.endPosition = endPosition.getCenter().add(0, 0.2, 0);
        CameraHandler.endFacing = endFacing;
        CameraHandler.onFinished = onFinished;
    }

    public static void resetPosition() {
        startPosition = null;
        startFacing = null;
        endPosition = null;
        endFacing = null;
        onFinished = null;
        animating = false;
        reversing = false;
        animClockMs = 0L;
        lastNowMs = -1L;
        p0Start = null;
        p1Forward = null;
        p2Up = null;
        endStepDir = null;
    }

    public static boolean isCameraOutsideOfPlayer() {
        return (startPosition != null && startFacing != null) || (endPosition != null && endFacing != null);
    }

    private static void beginAnimation(final Camera camera, final long now) {
        if (endPosition == null || endFacing == null) {
            return;
        }
        animating = true;
        reversing = false;
        animClockMs = 0L;
        lastNowMs = now;

        if (startFacing != null && startPosition != null) {
            p0Start = startPosition;
            final Vec3 startStepDir = new Vec3(startFacing.getStepX(), startFacing.getStepY(), startFacing.getStepZ());
            p1Forward = p0Start.add(startStepDir);
            p2Up = p1Forward.add(0.0, BLOCKS_UP, 0.0);

            startYaw = startFacing.toYRot();
            startMidYaw = Mth.wrapDegrees(startYaw + 180.0f);
        } else {
            p1Forward = camera.getPosition();
            p2Up = p1Forward.add(0.0, BLOCKS_UP, 0.0);
        }

        endStepDir = new Vec3(endFacing.getStepX(), endFacing.getStepY(), endFacing.getStepZ());

        endYaw = endFacing.toYRot();
        endMidYaw = Mth.wrapDegrees(endYaw + 180.0f);
    }

    private static void applyAnimation(final Camera camera, final long elapsedMs) {
        if (p2Up == null) {
            return;
        }
        final long elapsed = Math.max(0L, elapsedMs);
        final long forwardTotal = PHASE1_MS + PHASE2_MS;

        final ICameraMixin cameraMixin = (ICameraMixin) camera;

        if (!reversing && p0Start != null && p1Forward != null) {
            if (elapsed <= PHASE1_MS) {
                final float t = (float) elapsed / (float) PHASE1_MS;
                final Vec3 pos = lerpVec3(p0Start, p1Forward, t);
                final float yaw = lerpAngle(startYaw, startMidYaw, t);
                cameraMixin.setPositionPublic(pos);
                cameraMixin.setRotationPublic(yaw, 0.0f, 0.0F);
            } else if (elapsed <= forwardTotal) {
                final long phase2Elapsed = elapsed - PHASE1_MS;
                final float tRise = (float) phase2Elapsed / (float) PHASE2_MS;
                final Vec3 pos = lerpVec3(p1Forward, p2Up, tRise);

                final float pitch;
                if (phase2Elapsed <= PITCH_MS) {
                    final float tPitch = (float) phase2Elapsed / (float) PITCH_MS;
                    pitch = Mth.lerp(tPitch, 0.0f, 90.0f);
                } else {
                    pitch = 90.0f;
                }

                cameraMixin.setPositionPublic(pos);
                cameraMixin.setRotationPublic(startMidYaw, pitch, 0.0F);
            } else {
                cameraMixin.setPositionPublic(p2Up);
                cameraMixin.setRotationPublic(startMidYaw, 90.0f, 0.0F);
                reversing = true;
                animClockMs = 0L;
                if (onFinished != null) {
                    onFinished.run();
                }
            }
            return;
        }

        if (endPosition == null || endFacing == null || endStepDir == null) {
            resetPosition();
            return;
        }

        final Vec3 e0End = endPosition;
        final Vec3 e1Forward = e0End.add(endStepDir);

        if (elapsed <= PHASE2_MS) {
            final float tDown = (float) elapsed / (float) PHASE2_MS;
            final Vec3 pos = lerpVec3(p2Up, e1Forward, tDown);
            cameraMixin.setPositionPublic(pos);
            cameraMixin.setRotationPublic(endMidYaw, 90.0F, 0.0F);
        } else if (elapsed <= PHASE2_MS + PHASE1_MS) {
            final long phase1BackElapsed = elapsed - PHASE2_MS;
            final float t = (float) phase1BackElapsed / (float) PHASE1_MS;

            final Vec3 pos = lerpVec3(e1Forward, e0End, t);
            final float yaw = lerpAngle(endMidYaw, endYaw, t);

            final float pitch;
            if (phase1BackElapsed <= PITCH_MS) {
                final float tPitch = (float) phase1BackElapsed / (float) PITCH_MS;
                pitch = Mth.lerp(tPitch, 90.0f, 0.0f);
            } else {
                pitch = 0.0f;
            }

            cameraMixin.setPositionPublic(pos);
            cameraMixin.setRotationPublic(yaw, pitch, 0.0F);
        } else {
            cameraMixin.setPositionPublic(e0End);
            cameraMixin.setRotationPublic(endYaw, 0.0f, 0.0F);
            resetPosition();
        }
    }
}