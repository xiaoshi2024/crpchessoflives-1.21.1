package com.xiaoshi2022.crpchessoflives.clone.container;

import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity;
import com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity.ShellStates;
import com.xiaoshi2022.crpchessoflives.clone.registry.ModItems;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ShellSlotItemHandler extends SlotItemHandler {
    private final ShellForgeBlockEntity shellForge;

    public ShellSlotItemHandler(final IItemHandler itemHandler, final int index, final int x, final int y, final ShellForgeBlockEntity shellForge) {
        super(itemHandler, index, x, y);
        this.shellForge = shellForge;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return super.mayPlace(stack) && stack.getItem() == ModItems.DNA.get();
    }

    @Override
    public boolean isActive() {
        return this.shellForge.getShellState() == ShellStates.CREATE;
    }
}
