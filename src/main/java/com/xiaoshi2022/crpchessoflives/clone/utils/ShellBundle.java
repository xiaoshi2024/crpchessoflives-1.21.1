package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ShellBundle {
    public static final Codec<ShellBundle> CODEC = RecordCodecBuilder.create(in -> in.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(k -> k.id),
            ShellEntry.CODEC.listOf().fieldOf("entries").forGetter(k -> k.entries)
    ).apply(in, ShellBundle::new));

    public static final Codec<List<ShellBundle>> LIST_CODEC = CODEC.listOf();

    private final UUID id;
    private final List<ShellEntry> entries;

    public ShellBundle(final UUID id, final List<ShellEntry> entries) {
        this.id = id;
        this.entries = new ArrayList<>(entries);
    }

    public List<ShellEntry> getEntries() {
        return this.entries;
    }

    public record ShellEntry(String title,
                             Optional<PositionReference> shellForgePos,
                             Optional<InventoryEntry> inventory,
                             Optional<StatEntry> stats,
                             Optional<UUID> shellUuid,
                             Optional<UUID> ownerUuid) {
        public static final Codec<ShellEntry> CODEC = RecordCodecBuilder.create(in -> in.group(
                Codec.STRING.fieldOf("title").forGetter(ShellEntry::title),
                PositionReference.CODEC.optionalFieldOf("oldShellForgePos").forGetter(ShellEntry::shellForgePos),
                InventoryEntry.CODEC.optionalFieldOf("inventory").forGetter(ShellEntry::inventory),
                StatEntry.CODEC.optionalFieldOf("stats").forGetter(ShellEntry::stats),
                UUIDUtil.CODEC.optionalFieldOf("shellUuid").forGetter(ShellEntry::shellUuid),
                UUIDUtil.CODEC.optionalFieldOf("ownerUuid").forGetter(ShellEntry::ownerUuid)
        ).apply(in, ShellEntry::new));
    }

    public record InventoryEntry(NonNullList<ItemStack> items, NonNullList<ItemStack> armor,
                                 NonNullList<ItemStack> offhand) {
        public static final Codec<InventoryEntry> CODEC = RecordCodecBuilder.create(in -> in.group(
                NonNullList.codecOf(ItemStack.CODEC).fieldOf("items").forGetter(InventoryEntry::items),
                NonNullList.codecOf(ItemStack.CODEC).fieldOf("armorValue").forGetter(InventoryEntry::armor),
                NonNullList.codecOf(ItemStack.CODEC).fieldOf("offhand").forGetter(InventoryEntry::offhand)
        ).apply(in, InventoryEntry::new));
    }

    public record StatEntry(float health, int armorValue, int foodLevel,
                            Map<Holder<MobEffect>, MobEffectInstance> activeEffects) {
        public static final Codec<StatEntry> CODEC = RecordCodecBuilder.create(in -> in.group(
                Codec.FLOAT.fieldOf("health").forGetter(StatEntry::health),
                Codec.INT.fieldOf("armorValue").forGetter(StatEntry::armorValue),
                Codec.INT.fieldOf("foodLevel").forGetter(StatEntry::foodLevel),
                Codec.unboundedMap(MobEffect.CODEC, MobEffectInstance.CODEC).fieldOf("activeEffects").forGetter(StatEntry::activeEffects)
        ).apply(in, StatEntry::new));
    }
}
