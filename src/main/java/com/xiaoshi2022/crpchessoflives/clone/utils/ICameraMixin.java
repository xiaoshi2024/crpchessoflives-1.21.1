// 文件: mixin/ICameraMixin.java
package com.xiaoshi2022.crpchessoflives.clone.utils;

import net.minecraft.world.phys.Vec3;

public interface ICameraMixin {
    void setPositionPublic(Vec3 pos);
    void setRotationPublic(float yaw, float pitch, float roll);
    void setDetachedPublic(boolean detached);
}