package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.clone.container.CentrifugeContainerMenu;
import com.xiaoshi2022.crpchessoflives.clone.container.ShellForgeContainerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MODID);

    // 使用 IContainerFactory 来处理需要额外数据的容器
    public static final Supplier<MenuType<ShellForgeContainerMenu>> SHELL_FORGE =
            MENU_TYPES.register("shell_forge",
                    () -> IMenuTypeExtension.create((IContainerFactory<ShellForgeContainerMenu>)
                            ShellForgeContainerMenu::new));

    public static final Supplier<MenuType<CentrifugeContainerMenu>> CENTRIFUGE =
            MENU_TYPES.register("centrifuge",
                    () -> IMenuTypeExtension.create((IContainerFactory<CentrifugeContainerMenu>)
                            CentrifugeContainerMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}