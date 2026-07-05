package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.CentrifugeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CRPChessOfLives.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShellForgeBlockEntity>> SHELL_FORGE = BLOCK_ENTITY_TYPES.register("shell_forge",
            () -> BlockEntityType.Builder.of(ShellForgeBlockEntity::new, ModBlocks.SHELL_FORGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE = BLOCK_ENTITY_TYPES.register("centrifuge",
            () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get()).build(null));
}