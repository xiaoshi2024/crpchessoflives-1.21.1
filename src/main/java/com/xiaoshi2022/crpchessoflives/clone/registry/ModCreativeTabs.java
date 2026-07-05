package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CRPChessOfLives.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CLONE_TAB = CREATIVE_TABS.register(CRPChessOfLives.MODID + "_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CRPChessOfLives.MODID))
                    .icon(ModItems.DNA.get()::getDefaultInstance)
                    .displayItems((parameters, output) -> {
                        for (final DeferredHolder<Item, ?> item : ModItems.ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    }).build());
}