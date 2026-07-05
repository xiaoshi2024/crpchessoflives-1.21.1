package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import com.xiaoshi2022.crpchessoflives.clone.utils.OwnerData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CRPChessOfLives.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OwnerData>> OWNER_PLAYER = register("owner_player",
            builder -> builder.persistent(OwnerData.CODEC).networkSynchronized(OwnerData.STREAM_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> customizer) {
        return DATA_COMPONENT_TYPES.register(name, () -> customizer.apply(DataComponentType.builder()).build());
    }
}