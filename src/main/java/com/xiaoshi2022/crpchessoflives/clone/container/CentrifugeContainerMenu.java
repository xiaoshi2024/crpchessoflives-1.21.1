package com.xiaoshi2022.crpchessoflives.clone.container;

import com.xiaoshi2022.crpchessoflives.clone.blockentities.CentrifugeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModBlocks;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModDataComponentTypes;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModItems;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.Objects;

import static com.xiaoshi2022.crpchessoflives.clone.blockentities.CentrifugeBlockEntity.PROCESSING_TOTAL_TIME;

public class CentrifugeContainerMenu extends AbstractContainerMenu {
    private final CentrifugeBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public CentrifugeContainerMenu(final int containerId, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, data), ContainerLevelAccess.NULL);
    }

    public CentrifugeContainerMenu(final int containerId,
                                   final Inventory playerInventory,
                                   final CentrifugeBlockEntity blockEntity,
                                   final ContainerLevelAccess access) {
        super(ModMenuTypes.CENTRIFUGE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        this.addSlot(new SlotItemHandler(blockEntity.inventoryHandler, 0, 56, 34) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(ModItems.BLOOD_SYRINGE.get()) && stack.has(ModDataComponentTypes.OWNER_PLAYER.get());
            }
        });
        this.addSlot(new SlotItemHandler(blockEntity.inventoryHandler, 1, 116, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return false;
            }
        });

        final IItemHandler itemHandler = new InvWrapper(playerInventory);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new SlotItemHandler(itemHandler, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, i, 8 + i * 18, 142));
        }
    }

    private static CentrifugeBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof CentrifugeBlockEntity block) {
            return block;
        }
        throw new IllegalStateException("Block entityType is not correct! " + blockEntity);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            final ItemStack rawStack = slot.getItem();
            quickMovedStack = rawStack.copy();

            if (index == 1) {
                if (!this.moveItemStackTo(rawStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(rawStack, quickMovedStack);
            } else if (index >= 2 && index < 38) {
                if (!this.moveItemStackTo(rawStack, 0, 1, false)) {
                    if (index < 29) {
                        if (!this.moveItemStackTo(rawStack, 29, 38, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(rawStack, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(rawStack, 2, 38, false)) {
                return ItemStack.EMPTY;
            }

            if (rawStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (rawStack.getCount() == quickMovedStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    @Override
    public boolean stillValid(final Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.CENTRIFUGE.get());
    }

    public CentrifugeBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public float getProcessingProgress() {
        final int progress = this.blockEntity.getProcessingProgress();
        return progress != 0 ? Mth.clamp((float) progress / (float) PROCESSING_TOTAL_TIME, 0.0F, 1.0F) : 0.0F;
    }
}
