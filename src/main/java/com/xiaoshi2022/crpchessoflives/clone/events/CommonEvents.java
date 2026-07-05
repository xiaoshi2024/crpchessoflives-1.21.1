package com.xiaoshi2022.crpchessoflives.clone.events;

import com.xiaoshi2022.crpchessoflives.clone.gui.ShellSelectionOverlay;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModBlockEntityTypes;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellSavedData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber
public final class CommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntityTypes.SHELL_FORGE.get(),
                (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntityTypes.CENTRIFUGE.get(),
                (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.CENTRIFUGE.get(),
                (blockEntity, side) -> blockEntity.inventoryHandler
        );
    }

    @SubscribeEvent
    public static void onPlayerJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).syncToClient();
        }
    }

    @SubscribeEvent
    public static void onPlayerLeaveLevel(final EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player && ShellSelectionOverlay.INSTANCE.isOpened()) {
            ShellSelectionOverlay.INSTANCE.close(false);
        }
    }
}
