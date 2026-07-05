package com.xiaoshi2022.crpchessoflives.clone.items;

import com.xiaoshi2022.crpchessoflives.clone.registry.ModDataComponentTypes;
import com.xiaoshi2022.crpchessoflives.clone.utils.OwnerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemWithOwner extends Item {
    public ItemWithOwner(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> components, final TooltipFlag tooltipFlag) {
        if (stack.has(ModDataComponentTypes.OWNER_PLAYER.get())) {
            final OwnerData owner = stack.get(ModDataComponentTypes.OWNER_PLAYER.get());
            if (owner != null) {
                components.add(Component.translatable("tooltip.crpchessoflives.dna_owner", owner.playerName()).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
