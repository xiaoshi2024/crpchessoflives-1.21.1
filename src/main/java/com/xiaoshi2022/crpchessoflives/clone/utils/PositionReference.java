package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record PositionReference(BlockPos pos, Direction facing, ResourceLocation dimension) {
    public static final StreamCodec<ByteBuf, PositionReference> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PositionReference::pos,
            Direction.STREAM_CODEC, PositionReference::facing,
            ResourceLocation.STREAM_CODEC, PositionReference::dimension,
            PositionReference::new
    );

    public static final Codec<PositionReference> CODEC = RecordCodecBuilder.create(in -> in.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PositionReference::pos),
            Direction.CODEC.fieldOf("facing").forGetter(PositionReference::facing),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(PositionReference::dimension)
    ).apply(in, PositionReference::new));
}
