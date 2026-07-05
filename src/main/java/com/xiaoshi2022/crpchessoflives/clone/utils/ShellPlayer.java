package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

public interface ShellPlayer {
    void playershells$applyData(CompoundTag tag, PositionReference posReference);

    CompoundTag playershells$getData();

    void playershells$applyGameProfile(@Nullable GameProfile targetOwnerProfile);

    void playershells$restoreGameProfile();

    @Nullable
    UUID playershells$getDisguiseOwnerUuid();
}
