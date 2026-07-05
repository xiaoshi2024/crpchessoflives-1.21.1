package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.google.common.collect.*;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class MathUtils {
    public static final UUID EMPTY_UUID = new UUID(0, 0);

    public static float yawForDirection(final Direction direction) {
        return switch (direction) {
            case NORTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f; //SOUTH
        };
    }

    public static double getMinVelocity(final double velocity, final double absLimit) {
        return Math.abs(velocity) < absLimit ? velocity : absLimit * Math.signum(velocity);
    }

    public static boolean near(final float a, final float b) {
        return Math.abs(a - b) < 1e-4f;
    }

    public static float smoothstep01(final float x) {
        final float smooth = clamp01(x);
        return smooth * smooth * (3f - 2f * smooth);
    }

    public static float clamp01(final float x) {
        return x < 0f ? 0f : Math.min(x, 1f);
    }

    public static int[] interpolateGradient(final float t, final int[] colorA, final int[] colorB, final int[] colorC) {
        if (t < 0.5f) {
            return lerpColor(t / 0.5f, colorA, colorB);
        } else {
            return lerpColor((t - 0.5f) / 0.5f, colorB, colorC);
        }
    }

    private static int[] lerpColor(final float t, final int[] from, final int[] to) {
        return new int[]{
                (int) (from[0] + (to[0] - from[0]) * t),
                (int) (from[1] + (to[1] - from[1]) * t),
                (int) (from[2] + (to[2] - from[2]) * t)
        };
    }

    public static Vec3 lerpVec3(final Vec3 a, final Vec3 b, final float t) {
        final float clampedT = Mth.clamp(t, 0.0F, 1.0F);
        return new Vec3(
                Mth.lerp(clampedT, (float) a.x, (float) b.x),
                Mth.lerp(clampedT, (float) a.y, (float) b.y),
                Mth.lerp(clampedT, (float) a.z, (float) b.z)
        );
    }

    public static float lerpAngle(final float a, final float b, final float t) {
        final float clampedT = Mth.clamp(t, 0.0F, 1.0F);
        final float delta = Mth.wrapDegrees(b - a);
        return a + delta * clampedT;
    }

    public static float wrapDegrees(final float angle) {
        float newAngle = angle % 360;
        if (newAngle < 0) {
            newAngle += 360;
        }
        return newAngle;
    }

    public static int packRGB8(final int r, final int g, final int b) {
        final int ri = Math.min(255, Math.max(0, r));
        final int gi = Math.min(255, Math.max(0, g));
        final int bi = Math.min(255, Math.max(0, b));
        return (ri << 16) | (gi << 8) | bi;
    }

    @Nullable
    public static ServerLevel findTargetLevel(final MinecraftServer server, @Nullable final PositionReference positionReference) {
        if (positionReference == null) {
            return null;
        }
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .filter(level -> level.dimension().location().equals(positionReference.dimension()))
                .findAny()
                .orElse(null);
    }

    public static boolean hasPlayerInside(final BlockPos pos, final Level level) {
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        return level.getNearestPlayer(x, y, z, 1, false) != null;
    }

    public static boolean isPlayerInFront(final BlockPos pos, final Level level, final UUID playerUuid, final Direction facing) {
        final Player player = level.getPlayerByUUID(playerUuid);
        if (player == null) {
            return false;
        }

        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;

        final double dx = player.getX() - x;
        final double dy = (player.getEyeY()) - y;
        final double dz = player.getZ() - z;

        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) {
            return false;
        }
        final double ndx = dx / length;
        final double ndz = dz / length;

        final Vec3i dir = facing.getNormal();
        final double fx = dir.getX();
        final double fz = dir.getZ();

        final double dot = ndx * fx + ndz * fz;
        return dot > 0.5;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(final BlockEntityType<A> serverType,
                                                                                                         final BlockEntityType<E> clientType,
                                                                                                         final BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }

    public static <K, V> Codec<Multimap<K, V>> multiMapCodec(final Codec<K> keyCodec, final Codec<V> valueCodec) {
        return Codec.unboundedMap(keyCodec, valueCodec.listOf()).xmap(map -> {
            if (map != null) {
                final Multimap<K, V> multiMap = HashMultimap.create();

                map.forEach(multiMap::putAll);
                return multiMap;
            }
            return null;
        }, multiMap -> multiMap != null ? multiMap.asMap().entrySet()
                                          .stream()
                                          .collect(Collectors.toMap(Entry::getKey, e -> Lists.newArrayList(e.getValue()))) : null);
    }


    public static <K, V> StreamCodec<RegistryFriendlyByteBuf, Multimap<K, V>> multiMapStreamCodec(final StreamCodec<ByteBuf, K> keyCodec,
                                                                                                  final StreamCodec<ByteBuf, V> valueCodec) {
        final StreamCodec<ByteBuf, List<V>> listOfV = valueCodec.apply(ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, Map<K, List<V>>> mapCodec = ByteBufCodecs.map(HashMap::new, keyCodec, listOfV);

        return mapCodec.map(
                map -> {
                    final ListMultimap<K, V> multimap = ArrayListMultimap.create();
                    map.forEach((k, list) -> {
                        if (list != null && !list.isEmpty()) {
                            multimap.putAll(k, list);
                        }
                    });
                    return multimap;
                },
                mm -> {
                    final Map<K, List<V>> out = new HashMap<>();
                    mm.asMap().forEach((k, coll) -> out.put(k, new ArrayList<>(coll)));
                    return out;
                }
        );
    }
}
