package com.xiaoshi2022.crpchessoflives.clone.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class SoundHandler {
    private static final Multimap<BlockPos, SoundInstance> SOUND_MAP = ArrayListMultimap.create();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaySound(final PlaySoundEvent event) {
        final SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        if (!event.getOriginalSound().getLocation().getNamespace().startsWith(MODID)) {
            return;
        }

        final BlockPos pos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
        SOUND_MAP.put(pos, sound);
    }

    public static void startBlockSound(final SoundEvent soundEvent,
                                       final SoundSource source,
                                       final float volume,
                                       final float pitch,
                                       final RandomSource random,
                                       final BlockPos pos) {
        final SoundInstance sound = new SimpleSoundInstance(soundEvent, source, volume, pitch, random, pos.getX(), pos.getY(), pos.getZ());
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void stopAllBlockSounds(final BlockPos pos) {
        for (final SoundInstance sound : SOUND_MAP.get(pos)) {
            if (sound != null) {
                Minecraft.getInstance().getSoundManager().stop(sound);
            }
        }
        SOUND_MAP.removeAll(pos);
    }
}
