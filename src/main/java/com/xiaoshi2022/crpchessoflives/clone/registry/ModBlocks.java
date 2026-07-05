package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import com.xiaoshi2022.crpchessoflives.clone.blocks.CentrifugeBlock;
import com.xiaoshi2022.crpchessoflives.clone.blocks.ShellForgeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CRPChessOfLives.MODID);

    public static final DeferredBlock<Block> SHELL_FORGE = BLOCKS.register("shell_forge", () ->
            new ShellForgeBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()));
    public static final DeferredBlock<Block> CENTRIFUGE = BLOCKS.register("centrifuge", () ->
            new CentrifugeBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()));
}