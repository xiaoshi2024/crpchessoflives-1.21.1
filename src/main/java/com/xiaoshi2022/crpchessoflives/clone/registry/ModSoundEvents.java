package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CRPChessOfLives.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER = SOUND_EVENTS.register("flamethrower", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CRPChessOfLives.MODID, "flamethrower")));
}