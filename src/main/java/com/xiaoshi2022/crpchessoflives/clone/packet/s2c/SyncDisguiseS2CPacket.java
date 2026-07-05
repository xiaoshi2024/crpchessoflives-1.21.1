package com.xiaoshi2022.crpchessoflives.clone.packet.s2c;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;

public record SyncDisguiseS2CPacket(
        boolean restore,
        @Nullable String ownerName,
        @Nullable String texturesValue,
        @Nullable String texturesSignature
) implements CustomPacketPayload {

    public static final Type<SyncDisguiseS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "sync_disguise_packet"));

    public static final StreamCodec<FriendlyByteBuf, SyncDisguiseS2CPacket> STREAM_CODEC = StreamCodec.of(
            SyncDisguiseS2CPacket::encode,
            SyncDisguiseS2CPacket::decode
    );

    // ---- 客户端缓存：备份玩家真正的 originalProfile（第一次伪装时记录，restore 时用）----
    @Nullable
    private static GameProfile CLIENT_ORIGINAL_PROFILE = null;
    @Nullable
    private static final Unsafe CLIENT_UNSAFE;
    private static final long CLIENT_GAME_PROFILE_OFFSET;

    static {
        Unsafe unsafe = null;
        long offset = -1L;
        try {
            final Field uf = Unsafe.class.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            unsafe = (Unsafe) uf.get(null);
            final Field f = net.minecraft.world.entity.player.Player.class.getDeclaredField("gameProfile");
            offset = unsafe.objectFieldOffset(f);
        } catch (final Throwable ignore) {
            unsafe = null;
            offset = -1L;
        }
        CLIENT_UNSAFE = unsafe;
        CLIENT_GAME_PROFILE_OFFSET = offset;
    }

    private static void encode(final FriendlyByteBuf buf, final SyncDisguiseS2CPacket pkt) {
        buf.writeBoolean(pkt.restore());
        if (!pkt.restore()) {
            buf.writeUtf(pkt.ownerName() == null ? "" : pkt.ownerName());
            buf.writeBoolean(pkt.texturesValue() != null);
            if (pkt.texturesValue() != null) {
                buf.writeUtf(pkt.texturesValue());
                buf.writeBoolean(pkt.texturesSignature() != null);
                if (pkt.texturesSignature() != null) buf.writeUtf(pkt.texturesSignature());
            }
        }
    }

    private static SyncDisguiseS2CPacket decode(final FriendlyByteBuf buf) {
        final boolean restore = buf.readBoolean();
        if (restore) return new SyncDisguiseS2CPacket(true, null, null, null);
        final String ownerName = buf.readUtf();
        String texturesValue = null;
        String texturesSignature = null;
        if (buf.readBoolean()) {
            texturesValue = buf.readUtf();
            if (buf.readBoolean()) texturesSignature = buf.readUtf();
        }
        return new SyncDisguiseS2CPacket(false, ownerName.isEmpty() ? null : ownerName, texturesValue, texturesSignature);
    }

    public static void handle(final SyncDisguiseS2CPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            final LocalPlayer self = mc.player;
            if (self == null) return;
            if (CLIENT_UNSAFE == null || CLIENT_GAME_PROFILE_OFFSET < 0L) return;

            if (data.restore()) {
                // ---- 还原真实皮肤 ----
                if (CLIENT_ORIGINAL_PROFILE != null) {
                    CLIENT_UNSAFE.putObject(self, CLIENT_GAME_PROFILE_OFFSET, CLIENT_ORIGINAL_PROFILE);
                    CLIENT_ORIGINAL_PROFILE = null;
                }
            } else {
                // ---- 伪装：客户端把 LocalPlayer.gameProfile 改成目标玩家皮肤 ----
                if (CLIENT_ORIGINAL_PROFILE == null) {
                    CLIENT_ORIGINAL_PROFILE = self.getGameProfile(); // 备份原始
                }
                final GameProfile original = CLIENT_ORIGINAL_PROFILE;
                final String newName = data.ownerName() != null ? data.ownerName() : original.getName();
                final UUID selfId = original.getId(); // 保留自己 UUID（服务端也保留，保证两边一致）
                final GameProfile disguised = new GameProfile(selfId, newName);
                if (data.texturesValue() != null) {
                    disguised.getProperties().put("textures", new Property("textures", data.texturesValue(),
                            data.texturesSignature() == null ? "" : data.texturesSignature()));
                }
                CLIENT_UNSAFE.putObject(self, CLIENT_GAME_PROFILE_OFFSET, disguised);
            }
            // 客户端不需要再发 PlayerInfo/实体相关包
            // 因为第一人称手/第三人称身体都是直接读 Minecraft.this.player.getGameProfile().getProperties("textures")
            // 字段改完下一帧重绘就会读到新纹理
            // （但 CustomSkinLoader / Minecraft 自带 SkinManager 可能有缓存，若下次还没刷新，需要额外清缓存）
        }).exceptionally(e -> null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
