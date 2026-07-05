package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record OwnerData(UUID playerUUID, String playerName) {
    public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(in -> in.group(
            UUIDUtil.CODEC.fieldOf("playerUUID").forGetter(OwnerData::playerUUID),
            Codec.STRING.fieldOf("playerName").forGetter(OwnerData::playerName)
    ).apply(in, OwnerData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OwnerData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OwnerData::playerUUID,
            ByteBufCodecs.STRING_UTF8, OwnerData::playerName,
            OwnerData::new
    );
}
