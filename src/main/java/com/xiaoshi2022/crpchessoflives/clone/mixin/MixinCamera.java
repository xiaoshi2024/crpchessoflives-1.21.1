package com.xiaoshi2022.crpchessoflives.clone.mixin;

import com.xiaoshi2022.crpchessoflives.clone.utils.CameraHandler;
import com.xiaoshi2022.crpchessoflives.clone.utils.ICameraMixin;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera implements ICameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch, float roll);

    @Shadow
    public boolean detached;

    @Override
    public void setPositionPublic(Vec3 pos) {
        setPosition(pos);
    }

    @Override
    public void setRotationPublic(float yaw, float pitch, float roll) {
        setRotation(yaw, pitch, roll);
    }

    @Override
    public void setDetachedPublic(boolean detached) {
        this.detached = detached;
    }

    @Inject(method = "setup", at = @At("RETURN"))
    public void setup(final BlockGetter level,
                      final Entity entity,
                      final boolean detached,
                      final boolean thirdPersonReverse,
                      final float partialTick,
                      final CallbackInfo ci) {
        CameraHandler.cameraTick((Camera) (Object) this);
    }
}