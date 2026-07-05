package com.xiaoshi2022.crpchessoflives;

import com.mojang.logging.LogUtils;
import com.xiaoshi2022.crpchessoflives.clone.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(CRPChessOfLives.MODID)
public class CRPChessOfLives {
    public static final String MODID = "crpchessoflives";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CRPChessOfLives(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册所有内容
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModDataComponentTypes.DATA_COMPONENT_TYPES.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
    }
}