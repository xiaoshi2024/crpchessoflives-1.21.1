package com.xiaoshi2022.crpchessoflives.clone.storage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

public class ClientShellData {
    public static final ClientShellData INSTANCE = new ClientShellData();
    public static final double NEARBY_RANGE_SQ = 32.0 * 32.0;

    private Multimap<UUID, ShellState> entries = ArrayListMultimap.create();

    public void set(final Multimap<UUID, ShellState> entries) {
        this.entries = entries;
    }

    public Collection<ShellState> getAll(final UUID uuid) {
        return this.entries.get(uuid);
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
    public UUID findOwnerUuidByShellUuid(final UUID shellUuid) {
        for (final Entry<UUID, ShellState> entry : this.entries.entries()) {
            if (entry.getValue() != null && entry.getValue().shellUUID().equals(shellUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<Entry<UUID, ShellState>> getNearbyCompletedAllOwners(final PositionReference viewerPos) {
        final List<Entry<UUID, ShellState>> result = new ArrayList<>();
        if (viewerPos == null) {
            return result;
        }
        for (final Entry<UUID, ShellState> entry : this.entries.entries()) {
            final ShellState shell = entry.getValue();
            if (shell == null || shell.shellCreationProgress() < 100) {
                continue;
            }
            final PositionReference ref = shell.shellForgePos();
            if (ref == null || !ref.dimension().equals(viewerPos.dimension())) {
                continue;
            }
            if (ref.pos().distSqr(viewerPos.pos()) > NEARBY_RANGE_SQ) {
                continue;
            }
            result.add(entry);
        }
        result.sort(Comparator.comparingDouble(e -> e.getValue().shellForgePos().pos().distSqr(viewerPos.pos())));
        return result;
    }
}
