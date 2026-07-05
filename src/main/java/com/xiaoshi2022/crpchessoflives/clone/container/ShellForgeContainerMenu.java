package com.xiaoshi2022.crpchessoflives.clone.container;

import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModBlocks;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
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

public class ShellForgeContainerMenu extends AbstractContainerMenu {
    private final ShellForgeBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public ShellForgeContainerMenu(final int containerId, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, data), ContainerLevelAccess.NULL);
    }

    public ShellForgeContainerMenu(final int containerId,
                                   final Inventory playerInventory,
                                   final ShellForgeBlockEntity blockEntity,
                                   final ContainerLevelAccess access) {
        super(ModMenuTypes.SHELL_FORGE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        this.addSlot(new ShellSlotItemHandler(this.blockEntity.inventoryHandler, 0, 80, 17, this.blockEntity));

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

    private static ShellForgeBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof ShellForgeBlockEntity block) {
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

            if (index == 0) {
                if (!this.moveItemStackTo(rawStack, 1, 36, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(rawStack, quickMovedStack);
            } else if (index >= 1 && index < 37) {
                if (!this.moveItemStackTo(rawStack, 0, 1, false)) {
                    if (index < 28) {
                        if (!this.moveItemStackTo(rawStack, 28, 37, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(rawStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(rawStack, 1, 37, false)) {
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
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.SHELL_FORGE.get());
    }

    public ShellForgeBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
