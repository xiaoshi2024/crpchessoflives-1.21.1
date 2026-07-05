package com.xiaoshi2022.crpchessoflives.clone.storage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.packet.s2c.SyncShellDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.findTargetLevel;
import static java.util.Objects.requireNonNull;

public class ShellSavedData extends SavedData {
    private final Multimap<UUID, ShellState> entries;

    public ShellSavedData() {
        this.entries = ArrayListMultimap.create();
    }

    public ShellSavedData(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.entries = ArrayListMultimap.create();
        this.entries.putAll(ShellState.MULTIMAP_CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .getOrThrow()
                .getFirst());
    }

    public static ShellSavedData getShellData(final ServerLevel level) {
        final ServerLevel serverLevel = requireNonNull(level.getServer().getLevel(Level.OVERWORLD));
        return serverLevel.getDataStorage().computeIfAbsent(new Factory<>(ShellSavedData::new, ShellSavedData::new), "player_shells");
    }

    public void add(final UUID playerUuid, final ShellState shellState) {
        this.entries.put(playerUuid, shellState);
        this.setDirty();
    }

    public void updateShellCreationProgress(final UUID playerUuid, final UUID shellUuid, final int newShellCreationProgress) {
        final ShellState oldShellState = this.get(playerUuid, shellUuid);
        if (oldShellState != null) {
            final ShellState newShellState = new ShellState(
                    oldShellState.shellUUID(),
                    oldShellState.ownerUuid(),
                    oldShellState.shellForgePos(),
                    oldShellState.playerData(),
                    newShellCreationProgress
            );

            final Collection<ShellState> states = this.entries.get(playerUuid);
            states.remove(oldShellState);
            states.add(newShellState);

            this.setDirty();
        }
    }

    public Collection<ShellState> getAll(final UUID playerUuid) {
        return this.entries.get(playerUuid);
    }

    @Nullable
    public ShellState get(final UUID playerUuid, final UUID shellUuid) {
        for (final ShellState state : this.entries.get(playerUuid)) {
            if (state != null && state.shellUUID().equals(shellUuid)) {
                return state;
            }
        }

        return null;
    }

    @Nullable
    public ShellState getNearestActive(final UUID playerUuid, final ResourceLocation location, final BlockPos playerPos) {
        ShellState nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        ShellState fallback = null;

        for (final ShellState shellState : this.getAll(playerUuid)) {
            if (shellState.shellCreationProgress() == 100) {
                if (shellState.shellForgePos().dimension().equals(location)) {
                    final double distSqr = playerPos.distSqr(shellState.shellForgePos().pos());
                    if (distSqr < bestDistSq) {
                        bestDistSq = distSqr;
                        nearest = shellState;
                    }
                } else if (fallback == null) {
                    fallback = shellState;
                }
            }
        }

        return nearest != null ? nearest : fallback;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider provider) {
        return (CompoundTag) ShellState.MULTIMAP_CODEC.encode(this.entries, provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    @Override
    public void setDirty() {
        super.setDirty();
        this.syncToClient();
    }

    public void validateShellData(final Level level) {
        final Iterator<Entry<UUID, ShellState>> iterator = this.entries.entries().iterator();
        while (iterator.hasNext()) {
            final Entry<UUID, ShellState> entry = iterator.next();
            final ShellState shellState = entry.getValue();

            if (shellState == null || level.getServer() == null) {
                iterator.remove();
                continue;
            }

            final ServerLevel targetLevel = findTargetLevel(level.getServer(), shellState.shellForgePos());
            if (targetLevel == null) {
                iterator.remove();
                continue;
            }

            final BlockEntity blockEntity = targetLevel.getBlockEntity(shellState.shellForgePos().pos());
            if (!(blockEntity instanceof ShellForgeBlockEntity shellForge)) {
                iterator.remove();
                continue;
            }

            if (!shellForge.getShellUuid().equals(shellState.shellUUID())) {
                iterator.remove();
            }
        }
        this.setDirty();
    }

    public void syncToClient() {
        PacketDistributor.sendToAllPlayers(new SyncShellDataPacket(ImmutableListMultimap.copyOf(this.entries)));
    }
}
