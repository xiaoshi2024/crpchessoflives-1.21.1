package com.xiaoshi2022.crpchessoflives.clone.packet.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;

public record FinishedSyncPacket() implements CustomPacketPayload {
    public static final Type<FinishedSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "finished_sync_packet"));
    public static final StreamCodec<FriendlyByteBuf, FinishedSyncPacket> STREAM_CODEC = StreamCodec.unit(new FinishedSyncPacket());

    public static void handle(final FinishedSyncPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof DeathScreen) {
                mc.setScreen(null);
            }
        }).exceptionally(e -> null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
