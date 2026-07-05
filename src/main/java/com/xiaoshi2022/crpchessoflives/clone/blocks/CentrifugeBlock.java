package com.xiaoshi2022.crpchessoflives.clone.blocks;

import com.xiaoshi2022.crpchessoflives.clone.blockentities.CentrifugeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.createTickerHelper;

public class CentrifugeBlock extends Block implements EntityBlock {
    public CentrifugeBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void neighborChanged(final BlockState state,
                                final Level level,
                                final BlockPos pos,
                                final Block neighborBlock,
                                final BlockPos neighborPos,
                                final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity centrifuge) {
                centrifuge.refreshRedstoneSignal();
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity centrifuge) {
            final int progress = centrifuge.getProcessingProgress();
            if (progress > 0) {
                return Math.max(1, (progress * 15) / CentrifugeBlockEntity.PROCESSING_TOTAL_TIME);
            }
            return centrifuge.getRedstoneLevel() > 0 ? Math.max(1, centrifuge.getRedstoneLevel()) : 0;
        }
        return 0;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos blockPos, final BlockState state) {
        return new CentrifugeBlockEntity(blockPos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return !level.isClientSide()
                ? createTickerHelper(blockEntityType, ModBlockEntityTypes.CENTRIFUGE.get(), CentrifugeBlockEntity::serverTick)
                : null;
    }
}
