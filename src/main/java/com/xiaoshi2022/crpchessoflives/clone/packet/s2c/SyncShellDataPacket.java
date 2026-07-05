package com.xiaoshi2022.crpchessoflives.clone.packet.s2c;

import com.google.common.collect.Multimap;
import com.xiaoshi2022.crpchessoflives.clone.storage.ClientShellData;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.multiMapStreamCodec;

public record SyncShellDataPacket(Multimap<UUID, ShellState> entries) implements CustomPacketPayload {
    public static final Type<SyncShellDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "sync_shell_data_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShellDataPacket> STREAM_CODEC = StreamCodec.composite(
            multiMapStreamCodec(UUIDUtil.STREAM_CODEC, ShellState.STREAM_CODEC), SyncShellDataPacket::entries,
            SyncShellDataPacket::new
    );

    public static void handle(final SyncShellDataPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> ClientShellData.INSTANCE.set(data.entries()))
                .exceptionally(e -> null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
