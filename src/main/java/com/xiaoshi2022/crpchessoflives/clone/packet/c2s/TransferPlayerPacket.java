package com.xiaoshi2022.crpchessoflives.clone.packet.c2s;

import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity.ShellStates;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.findTargetLevel;

public record TransferPlayerPacket(PositionReference oldShellForgePos,
                                   PositionReference newShellForgePos) implements CustomPacketPayload {
    public static final Type<TransferPlayerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "transfer_player_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPlayerPacket> STREAM_CODEC = StreamCodec.composite(
            PositionReference.STREAM_CODEC, TransferPlayerPacket::oldShellForgePos,
            PositionReference.STREAM_CODEC, TransferPlayerPacket::newShellForgePos,
            TransferPlayerPacket::new
    );

    public static void handle(final TransferPlayerPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Level level = context.player().level();
            if (level.getServer() == null) {
                return;
            }
            transfer(level.getServer(), context.player(), data.oldShellForgePos(), data.newShellForgePos());
        }).exceptionally(e -> null);
    }

    public static void transfer(final MinecraftServer server,
                                final Player player,
                                @Nullable final PositionReference oldShellForgePos,
                                final PositionReference newShellForgePos) {
        final ServerLevel oldTargetLevel = findTargetLevel(server, oldShellForgePos);
        final ServerLevel newTargetLevel = findTargetLevel(server, newShellForgePos);

        if (oldTargetLevel != null && oldTargetLevel.getBlockEntity(oldShellForgePos.pos()) instanceof ShellForgeBlockEntity shellForge) {
            shellForge.transferPlayerFrom(player);
        }
        if (newTargetLevel != null && newTargetLevel.getBlockEntity(newShellForgePos.pos()) instanceof ShellForgeBlockEntity shellForge
                && shellForge.getShellState() == ShellStates.EXTERMINATE) {
            shellForge.transferPlayerTo(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
