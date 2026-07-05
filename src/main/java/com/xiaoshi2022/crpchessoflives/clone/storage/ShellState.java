package com.xiaoshi2022.crpchessoflives.clone.storage;

import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.EMPTY_UUID;

public record ShellState(UUID shellUUID, UUID ownerUuid, PositionReference shellForgePos, CompoundTag playerData,
                         int shellCreationProgress) {
    public static final StreamCodec<ByteBuf, ShellState> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShellState::shellUUID,
            UUIDUtil.STREAM_CODEC, ShellState::ownerUuid,
            PositionReference.STREAM_CODEC, ShellState::shellForgePos,
            ByteBufCodecs.COMPOUND_TAG, ShellState::playerData,
            ByteBufCodecs.INT, ShellState::shellCreationProgress,
            ShellState::new
    );

    public static final Codec<ShellState> CODEC = RecordCodecBuilder.create(in -> in.group(
            UUIDUtil.CODEC.fieldOf("shellUUID").forGetter(ShellState::shellUUID),
            UUIDUtil.CODEC.optionalFieldOf("ownerUuid", EMPTY_UUID).forGetter(ShellState::ownerUuid),
            PositionReference.CODEC.fieldOf("oldShellForgePos").forGetter(ShellState::shellForgePos),
            CompoundTag.CODEC.fieldOf("playerData").forGetter(ShellState::playerData),
            Codec.INT.fieldOf("shellCreationProgress").forGetter(ShellState::shellCreationProgress)
    ).apply(in, ShellState::new));

    public static final Codec<Multimap<UUID, ShellState>> MULTIMAP_CODEC = MathUtils.multiMapCodec(Codec.STRING.xmap(UUID::fromString, UUID::toString), ShellState.CODEC);
}
